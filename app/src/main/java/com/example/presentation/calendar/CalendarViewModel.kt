package com.example.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.CategoryItem
import com.example.domain.model.TaskItem
import com.example.domain.repository.TaskRepository
import com.example.utils.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class CalendarViewMode(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

enum class CalendarStatusFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed")
}

data class CalendarUiState(
    val viewMode: CalendarViewMode = CalendarViewMode.MONTHLY,
    val statusFilter: CalendarStatusFilter = CalendarStatusFilter.ALL,
    val selectedDate: LocalDate = LocalDate.now(),
    val displayedMonth: YearMonth = YearMonth.now(),
    val tasksByEpochDay: Map<Long, List<TaskItem>> = emptyMap(),
    val selectedDateTasks: List<TaskItem> = emptyList(),
    val categories: List<CategoryItem> = emptyList()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val viewMode = MutableStateFlow(CalendarViewMode.MONTHLY)
    private val statusFilter = MutableStateFlow(CalendarStatusFilter.ALL)
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val displayedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<CalendarUiState> = combine(
        taskRepository.observeAllTasks(),
        taskRepository.observeCategories(),
        viewMode,
        statusFilter,
        selectedDate,
        displayedMonth
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allTasks = values[0] as List<TaskItem>
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<CategoryItem>
        val mode = values[2] as CalendarViewMode
        val filter = values[3] as CalendarStatusFilter
        val selDate = values[4] as LocalDate
        val month = values[5] as YearMonth

        val filtered = when (filter) {
            CalendarStatusFilter.ALL -> allTasks
            CalendarStatusFilter.PENDING -> allTasks.filter { !it.isCompleted }
            CalendarStatusFilter.COMPLETED -> allTasks.filter { it.isCompleted }
        }

        val grouped = filtered.groupBy { it.scheduledDate }
        val selEpoch = DateTimeUtils.localDateToEpochDay(selDate)
        val selTasks = (grouped[selEpoch] ?: emptyList()).sortedWith(
            compareBy<TaskItem> { it.isCompleted }
                .thenBy { it.startTime ?: it.dueTime ?: Long.MAX_VALUE }
                .thenByDescending { it.priority.level }
        )

        CalendarUiState(
            viewMode = mode,
            statusFilter = filter,
            selectedDate = selDate,
            displayedMonth = month,
            tasksByEpochDay = grouped,
            selectedDateTasks = selTasks,
            categories = categories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun setViewMode(mode: CalendarViewMode) {
        viewMode.value = mode
    }

    fun setStatusFilter(filter: CalendarStatusFilter) {
        statusFilter.value = filter
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        displayedMonth.value = YearMonth.from(date)
    }

    fun previousMonth() {
        displayedMonth.value = displayedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        displayedMonth.value = displayedMonth.value.plusMonths(1)
    }

    fun jumpToToday() {
        val today = LocalDate.now()
        selectedDate.value = today
        displayedMonth.value = YearMonth.from(today)
    }

    fun toggleTaskCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompleted(taskId, completed)
        }
    }
}
