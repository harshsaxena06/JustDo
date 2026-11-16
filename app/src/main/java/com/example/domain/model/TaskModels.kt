package com.example.domain.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import com.example.ui.theme.PriorityUrgentColor
import com.example.utils.DateTimeUtils
import com.example.utils.RecurrenceRule

enum class TaskPriority(val level: Int, val label: String, val color: Color) {
    LOW(0, "Low", PriorityLowColor),
    MEDIUM(1, "Medium", PriorityMediumColor),
    HIGH(2, "High", PriorityHighColor),
    URGENT(3, "Urgent", PriorityUrgentColor);

    companion object {
        fun fromLevel(level: Int): TaskPriority =
            entries.firstOrNull { it.level == level } ?: MEDIUM
    }
}

enum class ReminderType(val label: String, val defaultOffsetMinutes: Int) {
    AT_TIME("At scheduled time", 0),
    BEFORE_5M("5 min before", 5),
    BEFORE_10M("10 min before", 10),
    BEFORE_15M("15 min before", 15),
    BEFORE_30M("30 min before", 30),
    BEFORE_60M("1 hour before", 60),
    CUSTOM("Custom", 0),
    OVERDUE("Overdue alert", 0),
    SNOOZED("Snoozed", 0);

    companion object {
        fun fromString(value: String): ReminderType =
            entries.firstOrNull { it.name == value } ?: AT_TIME
    }
}

data class ReminderOption(
    val type: ReminderType,
    val offsetMinutes: Int
) {
    fun displayLabel(): String {
        return when (type) {
            ReminderType.AT_TIME -> "At time"
            ReminderType.BEFORE_5M -> "5m before"
            ReminderType.BEFORE_10M -> "10m before"
            ReminderType.BEFORE_15M -> "15m before"
            ReminderType.BEFORE_30M -> "30m before"
            ReminderType.BEFORE_60M -> "1h before"
            ReminderType.CUSTOM -> "${offsetMinutes}m before"
            ReminderType.OVERDUE -> "Overdue alert"
            ReminderType.SNOOZED -> "Snoozed"
        }
    }
}

data class CategoryItem(
    val id: Long,
    val name: String,
    val color: Int,
    val isDefault: Boolean
)

data class SubtaskItem(
    val id: Long = 0L,
    val taskId: Long = 0L,
    val title: String,
    val isCompleted: Boolean = false
)

data class ReminderItem(
    val id: Long = 0L,
    val taskId: Long = 0L,
    val reminderDateTime: Long,
    val reminderType: ReminderType,
    val offsetMinutes: Int,
    val isEnabled: Boolean,
    val isFired: Boolean,
    val notificationId: Int
)

data class TaskItem(
    val id: Long,
    val title: String,
    val description: String,
    val notes: String,
    val categoryId: Long,
    val category: CategoryItem?,
    val priority: TaskPriority,
    val scheduledDate: Long,
    val startTime: Long?,
    val dueTime: Long?,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val isRecurring: Boolean,
    val recurrenceRule: RecurrenceRule,
    val overdueNotified: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val subtasks: List<SubtaskItem>,
    val reminders: List<ReminderItem>
) {
    fun isOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return DateTimeUtils.isTaskOverdue(
            scheduledDate = scheduledDate,
            startTime = startTime,
            dueTime = dueTime,
            isCompleted = isCompleted,
            nowMillis = nowMillis
        )
    }

    val completedSubtasksCount: Int get() = subtasks.count { it.isCompleted }
}

data class FocusSessionItem(
    val id: Long,
    val taskId: Long?,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val sessionType: String
)

enum class StatusFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    OVERDUE("Overdue"),
    UPCOMING("Upcoming")
}

enum class TaskSort(val label: String) {
    TIME_ASC("Time"),
    PRIORITY_DESC("Priority"),
    CREATED_DESC("Newest")
}

data class TaskFilter(
    val query: String = "",
    val status: StatusFilter = StatusFilter.ALL,
    val categoryId: Long? = null,
    val priority: TaskPriority? = null,
    val dateEpochDay: Long? = null,
    val sort: TaskSort = TaskSort.TIME_ASC
)

enum class AnalyticsRange(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

data class DailyProductivityPoint(
    val label: String,
    val epochDay: Long,
    val completedCount: Int,
    val totalCount: Int
)

data class CategoryDistributionItem(
    val category: CategoryItem,
    val totalCount: Int,
    val completedCount: Int,
    val percentage: Float
)

data class AnalyticsSummary(
    val range: AnalyticsRange = AnalyticsRange.WEEKLY,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val overdueTasks: Int = 0,
    val completionPercentage: Float = 0f,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val mostProductiveDay: String = "—",
    val totalFocusMinutes: Long = 0L,
    val chartPoints: List<DailyProductivityPoint> = emptyList(),
    val categoryDistribution: List<CategoryDistributionItem> = emptyList()
)
