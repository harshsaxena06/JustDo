package com.example.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AnalyticsRange
import com.example.domain.model.AnalyticsSummary
import com.example.domain.model.FocusSessionItem
import com.example.domain.repository.TaskRepository
import com.example.domain.usecase.ComputeAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FocusHistoryRow(
    val session: FocusSessionItem,
    val taskTitle: String
)

data class AnalyticsUiState(
    val summary: AnalyticsSummary = AnalyticsSummary(),
    val recentFocusSessions: List<FocusHistoryRow> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    taskRepository: TaskRepository,
    private val computeAnalyticsUseCase: ComputeAnalyticsUseCase
) : ViewModel() {

    private val rangeFlow = MutableStateFlow(AnalyticsRange.WEEKLY)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        taskRepository.observeAllTasks(),
        taskRepository.observeCategories(),
        taskRepository.observeFocusSessions(),
        rangeFlow
    ) { tasks, categories, sessions, range ->
        val summary = computeAnalyticsUseCase(
            tasks = tasks,
            categories = categories,
            focusSessions = sessions,
            range = range
        )
        val taskTitles = tasks.associate { it.id to it.title }
        val recent = sessions.take(8).map { session ->
            FocusHistoryRow(
                session = session,
                taskTitle = session.taskId?.let { taskTitles[it] } ?: "General Focus"
            )
        }
        AnalyticsUiState(summary = summary, recentFocusSessions = recent)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun setRange(range: AnalyticsRange) {
        rangeFlow.value = range
    }
}
