package com.example.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.ReminderItem
import com.example.domain.model.ReminderOption
import com.example.domain.model.ReminderType
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.repository.TaskRepository
import com.example.domain.usecase.ComputeAnalyticsUseCase
import com.example.notifications.manager.NotificationHelper
import com.example.notifications.scheduler.AlarmScheduler
import com.example.utils.DateTimeUtils
import com.example.utils.MotivationalQuote
import com.example.utils.QuotesProvider
import com.example.utils.RecurrenceRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class UpcomingReminderDisplay(
    val reminder: ReminderItem,
    val taskTitle: String
)

data class DashboardUiState(
    val greeting: String = DateTimeUtils.greetingForHour(),
    val liveClock: String = DateTimeUtils.formatClockNow(),
    val fullDate: String = DateTimeUtils.formatFullDate(LocalDate.now()),
    val todayTasks: List<TaskItem> = emptyList(),
    val completedTodayCount: Int = 0,
    val pendingTodayCount: Int = 0,
    val overdueTasks: List<TaskItem> = emptyList(),
    val upcomingReminders: List<UpcomingReminderDisplay> = emptyList(),
    val dailyGoal: Int = 5,
    val completionPercent: Int = 0,
    val completionFraction: Float = 0f,
    val streakDays: Int = 0,
    val quote: MotivationalQuote = QuotesProvider.quoteForToday(),
    val hasNotificationPermission: Boolean = true,
    val canScheduleExactAlarms: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val computeAnalyticsUseCase: ComputeAnalyticsUseCase,
    private val notificationHelper: NotificationHelper,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val quoteOffset = MutableStateFlow(0)
    private val permissionRefreshTrigger = MutableStateFlow(0)

    private val clockFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    init {
        viewModelScope.launch {
            taskRepository.ensureSeeded()
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        taskRepository.observeAllTasks(),
        taskRepository.observeUpcomingReminders(),
        preferencesRepository.preferencesFlow,
        quoteOffset,
        clockFlow,
        permissionRefreshTrigger
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allTasks = values[0] as List<TaskItem>
        @Suppress("UNCHECKED_CAST")
        val reminders = values[1] as List<ReminderItem>
        val prefs = values[2] as com.example.data.preferences.UserPreferences
        val qOffset = values[3] as Int
        val nowMillis = values[4] as Long

        val todayEpoch = DateTimeUtils.todayEpochDay()
        val todayTasks = allTasks.filter { it.scheduledDate == todayEpoch }
        val completedToday = todayTasks.count { it.isCompleted }
        val pendingToday = todayTasks.count { !it.isCompleted }
        val overdue = allTasks.filter { it.isOverdue(nowMillis) }

        val taskTitleById = allTasks.associate { it.id to it.title }
        val upcoming = reminders
            .filter { it.reminderDateTime > nowMillis && it.reminderType != ReminderType.OVERDUE }
            .take(5)
            .mapNotNull { rem ->
                val title = taskTitleById[rem.taskId] ?: return@mapNotNull null
                UpcomingReminderDisplay(rem, title)
            }

        val completedDays = allTasks
            .filter { it.isCompleted }
            .map { t ->
                t.completedAt?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                } ?: t.scheduledDate
            }
            .toSet()

        val streak = computeAnalyticsUseCase.computeCurrentStreak(completedDays, todayEpoch)
        val fraction = if (todayTasks.isNotEmpty()) {
            completedToday.toFloat() / todayTasks.size.toFloat()
        } else 0f

        DashboardUiState(
            greeting = DateTimeUtils.greetingForHour(),
            liveClock = DateTimeUtils.formatClockNow(nowMillis),
            fullDate = DateTimeUtils.formatFullDate(LocalDate.now()),
            todayTasks = todayTasks,
            completedTodayCount = completedToday,
            pendingTodayCount = pendingToday,
            overdueTasks = overdue,
            upcomingReminders = upcoming,
            dailyGoal = prefs.dailyGoal,
            completionPercent = (fraction * 100).toInt(),
            completionFraction = fraction.coerceIn(0f, 1f),
            streakDays = streak,
            quote = QuotesProvider.quoteForToday(qOffset),
            hasNotificationPermission = notificationHelper.hasNotificationPermission(),
            canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(
            hasNotificationPermission = notificationHelper.hasNotificationPermission(),
            canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms()
        )
    )

    fun quickAddTask(title: String) {
        val cleaned = title.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            taskRepository.saveTask(
                title = cleaned,
                description = "",
                notes = "",
                categoryId = AppDatabase.DEFAULT_OTHER_CATEGORY_ID,
                priority = TaskPriority.MEDIUM,
                scheduledDate = DateTimeUtils.todayEpochDay(),
                startTime = null,
                dueTime = null,
                isRecurring = false,
                recurrenceRule = RecurrenceRule(),
                subtasks = emptyList(),
                reminderOptions = emptyList()
            )
        }
    }

    fun toggleTaskCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompleted(taskId, completed)
        }
    }

    fun rescheduleToToday(taskId: Long) {
        viewModelScope.launch {
            taskRepository.rescheduleTaskToToday(taskId)
        }
    }

    fun nextQuote() {
        quoteOffset.update { it + 1 }
    }

    fun refreshPermissions() {
        permissionRefreshTrigger.update { it + 1 }
    }
}
