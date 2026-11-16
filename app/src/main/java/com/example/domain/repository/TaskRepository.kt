package com.example.domain.repository

import com.example.domain.model.CategoryItem
import com.example.domain.model.FocusSessionItem
import com.example.domain.model.ReminderItem
import com.example.domain.model.ReminderOption
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.utils.RecurrenceRule
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeAllTasks(): Flow<List<TaskItem>>
    fun observeTask(taskId: Long): Flow<TaskItem?>
    fun observeCategories(): Flow<List<CategoryItem>>
    fun observeUpcomingReminders(): Flow<List<ReminderItem>>
    fun observeFocusSessions(): Flow<List<FocusSessionItem>>

    suspend fun ensureSeeded()

    suspend fun saveTask(
        taskId: Long? = null,
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
        reminderOptions: List<ReminderOption>
    ): Long

    suspend fun toggleTaskCompleted(taskId: Long, completed: Boolean)
    suspend fun rescheduleTaskToToday(taskId: Long)
    suspend fun deleteTask(taskId: Long)

    suspend fun toggleSubtask(subtaskId: Long, completed: Boolean)
    suspend fun addSubtask(taskId: Long, title: String)
    suspend fun setReminderEnabled(reminderId: Long, enabled: Boolean)

    suspend fun snoozeTaskReminder(taskId: Long, snoozeMinutes: Int, dismissedNotificationId: Int)
    suspend fun rescheduleAllPendingReminders()

    suspend fun createCategory(name: String, color: Int): Long
    suspend fun updateCategory(id: Long, name: String, color: Int)
    suspend fun deleteCustomCategory(id: Long)

    suspend fun logFocusSession(
        taskId: Long?,
        startTime: Long,
        endTime: Long,
        durationSeconds: Long,
        sessionType: String
    )

    suspend fun clearAllLocalData()
}
