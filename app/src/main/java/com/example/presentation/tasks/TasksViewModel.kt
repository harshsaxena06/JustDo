package com.example.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.CategoryItem
import com.example.domain.model.ReminderOption
import com.example.domain.model.StatusFilter
import com.example.domain.model.TaskFilter
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskSort
import com.example.domain.repository.TaskRepository
import com.example.domain.usecase.FilterAndSortTasksUseCase
import com.example.utils.RecurrenceRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val tasks: List<TaskItem> = emptyList(),
    val categories: List<CategoryItem> = emptyList(),
    val filter: TaskFilter = TaskFilter(),
    val totalTaskCount: Int = 0
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val filterAndSortTasksUseCase: FilterAndSortTasksUseCase
) : ViewModel() {

    private val filterState = MutableStateFlow(TaskFilter())

    val uiState: StateFlow<TasksUiState> = combine(
        taskRepository.observeAllTasks(),
        taskRepository.observeCategories(),
        filterState
    ) { allTasks, categories, filter ->
        TasksUiState(
            tasks = filterAndSortTasksUseCase(allTasks, filter),
            categories = categories,
            filter = filter,
            totalTaskCount = allTasks.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState()
    )

    fun updateQuery(query: String) {
        filterState.update { it.copy(query = query) }
    }

    fun updateStatus(status: StatusFilter) {
        filterState.update { it.copy(status = status) }
    }

    fun updateCategory(categoryId: Long?) {
        filterState.update { it.copy(categoryId = categoryId) }
    }

    fun updatePriority(priority: TaskPriority?) {
        filterState.update { it.copy(priority = priority) }
    }

    fun updateDateFilter(epochDay: Long?) {
        filterState.update { it.copy(dateEpochDay = epochDay) }
    }

    fun updateSort(sort: TaskSort) {
        filterState.update { it.copy(sort = sort) }
    }

    fun clearFilters() {
        filterState.value = TaskFilter()
    }

    fun toggleTaskCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompleted(taskId, completed)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun saveTask(
        taskId: Long?,
        title: String,
        description: String,
        notes: String,
        categoryId: Long,
        priority: TaskPriority,
        scheduledDate: Long,
        startTime: Long?,
        dueTime: Long?,
        isRecurring: Boolean,
        recurrenceRule: RecurrenceRule,
        subtasks: List<Pair<Long, String>>,
        reminderOptions: List<ReminderOption>,
        onSaved: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val savedId = taskRepository.saveTask(
                taskId = taskId,
                title = title,
                description = description,
                notes = notes,
                categoryId = categoryId,
                priority = priority,
                scheduledDate = scheduledDate,
                startTime = startTime,
                dueTime = dueTime,
                isRecurring = isRecurring,
                recurrenceRule = recurrenceRule,
                subtasks = subtasks,
                reminderOptions = reminderOptions
            )
            onSaved(savedId)
        }
    }
}
