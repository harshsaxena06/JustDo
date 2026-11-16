package com.example.presentation.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.TaskItem
import com.example.domain.repository.TaskRepository
import com.example.notifications.manager.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FocusPhase(val label: String) {
    FOCUS("Focus"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

data class FocusUiState(
    val phase: FocusPhase = FocusPhase.FOCUS,
    val selectedTaskId: Long? = null,
    val pendingTasks: List<TaskItem> = emptyList(),
    val focusMinutesSetting: Int = 25,
    val breakMinutesSetting: Int = 5,
    val totalSeconds: Long = 25 * 60L,
    val remainingSeconds: Long = 25 * 60L,
    val isRunning: Boolean = false,
    val completedFocusCycles: Int = 0,
    val sessionSavedNotice: String? = null
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    preferencesRepository: UserPreferencesRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val internalState = MutableStateFlow(FocusUiState())
    private var timerJob: Job? = null
    private var currentSessionStartMillis: Long = 0L

    val uiState: StateFlow<FocusUiState> = combine(
        internalState,
        taskRepository.observeAllTasks(),
        preferencesRepository.preferencesFlow
    ) { state, allTasks, prefs ->
        val pending = allTasks.filter { !it.isCompleted }
        val updatedTotal = if (!state.isRunning && state.remainingSeconds == state.totalSeconds) {
            durationSecondsFor(state.phase, prefs.pomodoroFocusMinutes, prefs.pomodoroBreakMinutes)
        } else {
            state.totalSeconds
        }
        val updatedRemaining = if (!state.isRunning && state.remainingSeconds == state.totalSeconds) {
            updatedTotal
        } else {
            state.remainingSeconds
        }
        state.copy(
            pendingTasks = pending,
            focusMinutesSetting = prefs.pomodoroFocusMinutes,
            breakMinutesSetting = prefs.pomodoroBreakMinutes,
            totalSeconds = updatedTotal,
            remainingSeconds = updatedRemaining
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FocusUiState()
    )

    fun selectTask(taskId: Long?) {
        internalState.update { it.copy(selectedTaskId = taskId) }
    }

    fun selectPhase(phase: FocusPhase) {
        timerJob?.cancel()
        val current = uiState.value
        val secs = durationSecondsFor(phase, current.focusMinutesSetting, current.breakMinutesSetting)
        internalState.update {
            it.copy(
                phase = phase,
                totalSeconds = secs,
                remainingSeconds = secs,
                isRunning = false
            )
        }
    }

    fun toggleStartPause() {
        val current = uiState.value
        if (current.isRunning) {
            timerJob?.cancel()
            internalState.update { it.copy(isRunning = false) }
        } else {
            if (currentSessionStartMillis == 0L || current.remainingSeconds == current.totalSeconds) {
                currentSessionStartMillis = System.currentTimeMillis()
            }
            internalState.update {
                it.copy(
                    isRunning = true,
                    totalSeconds = current.totalSeconds,
                    remainingSeconds = current.remainingSeconds,
                    sessionSavedNotice = null
                )
            }
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1000L)
                    val s = internalState.value
                    if (!s.isRunning) break
                    if (s.remainingSeconds <= 1L) {
                        onTimerCompleted()
                        break
                    } else {
                        internalState.update { it.copy(remainingSeconds = it.remainingSeconds - 1L) }
                    }
                }
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        currentSessionStartMillis = 0L
        val current = uiState.value
        val secs = durationSecondsFor(current.phase, current.focusMinutesSetting, current.breakMinutesSetting)
        internalState.update {
            it.copy(
                isRunning = false,
                totalSeconds = secs,
                remainingSeconds = secs
            )
        }
    }

    fun completeEarlyAndLog() {
        val current = uiState.value
        val elapsed = (current.totalSeconds - current.remainingSeconds).coerceAtLeast(0L)
        if (elapsed < 10L) {
            resetTimer()
            return
        }
        timerJob?.cancel()
        viewModelScope.launch {
            val end = System.currentTimeMillis()
            val start = if (currentSessionStartMillis > 0L) currentSessionStartMillis else end - elapsed * 1000L
            taskRepository.logFocusSession(
                taskId = current.selectedTaskId,
                startTime = start,
                endTime = end,
                durationSeconds = elapsed,
                sessionType = if (current.phase == FocusPhase.FOCUS) "FOCUS" else "BREAK"
            )
            currentSessionStartMillis = 0L
            val secs = durationSecondsFor(current.phase, current.focusMinutesSetting, current.breakMinutesSetting)
            internalState.update {
                it.copy(
                    isRunning = false,
                    totalSeconds = secs,
                    remainingSeconds = secs,
                    sessionSavedNotice = "Logged ${elapsed / 60}m ${elapsed % 60}s session!"
                )
            }
        }
    }

    fun clearNotice() {
        internalState.update { it.copy(sessionSavedNotice = null) }
    }

    private fun onTimerCompleted() {
        val current = uiState.value
        val end = System.currentTimeMillis()
        val start = if (currentSessionStartMillis > 0L) currentSessionStartMillis else end - current.totalSeconds * 1000L
        val taskTitle = uiState.value.pendingTasks.firstOrNull { it.id == current.selectedTaskId }?.title

        viewModelScope.launch {
            taskRepository.logFocusSession(
                taskId = current.selectedTaskId,
                startTime = start,
                endTime = end,
                durationSeconds = current.totalSeconds,
                sessionType = if (current.phase == FocusPhase.FOCUS) "FOCUS" else "BREAK"
            )
            notificationHelper.showFocusCompleteNotification(
                isBreak = current.phase != FocusPhase.FOCUS,
                taskTitle = taskTitle
            )
        }

        currentSessionStartMillis = 0L
        val nextPhase = if (current.phase == FocusPhase.FOCUS) {
            if ((current.completedFocusCycles + 1) % 4 == 0) FocusPhase.LONG_BREAK else FocusPhase.SHORT_BREAK
        } else {
            FocusPhase.FOCUS
        }
        val nextTotal = durationSecondsFor(nextPhase, uiState.value.focusMinutesSetting, uiState.value.breakMinutesSetting)
        internalState.update {
            it.copy(
                phase = nextPhase,
                totalSeconds = nextTotal,
                remainingSeconds = nextTotal,
                isRunning = false,
                completedFocusCycles = if (current.phase == FocusPhase.FOCUS) it.completedFocusCycles + 1 else it.completedFocusCycles,
                sessionSavedNotice = if (current.phase == FocusPhase.FOCUS) "Focus complete! Ready for a break." else "Break complete! Ready to focus."
            )
        }
    }

    private fun durationSecondsFor(phase: FocusPhase, focusMins: Int, breakMins: Int): Long {
        return when (phase) {
            FocusPhase.FOCUS -> focusMins * 60L
            FocusPhase.SHORT_BREAK -> breakMins * 60L
            FocusPhase.LONG_BREAK -> (breakMins * 3).coerceAtLeast(15) * 60L
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
