package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.dao.CategoryDao
import com.example.data.dao.FocusSessionDao
import com.example.data.dao.ReminderDao
import com.example.data.dao.SubtaskDao
import com.example.data.dao.TaskDao
import com.example.data.database.AppDatabase
import com.example.data.entity.CategoryEntity
import com.example.data.entity.FocusSessionEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.SubtaskEntity
import com.example.data.entity.TaskEntity
import com.example.data.entity.TaskWithDetails
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.CategoryItem
import com.example.domain.model.FocusSessionItem
import com.example.domain.model.ReminderItem
import com.example.domain.model.ReminderOption
import com.example.domain.model.ReminderType
import com.example.domain.model.SubtaskItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.repository.TaskRepository
import com.example.notifications.manager.NotificationHelper
import com.example.notifications.scheduler.AlarmScheduler
import com.example.utils.DateTimeUtils
import com.example.utils.RecurrenceRule
import com.example.utils.RecurrenceUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val reminderDao: ReminderDao,
    private val subtaskDao: SubtaskDao,
    private val focusSessionDao: FocusSessionDao,
    private val alarmScheduler: AlarmScheduler,
    private val notificationHelper: NotificationHelper,
    private val preferencesRepository: UserPreferencesRepository
) : TaskRepository {

    override fun observeAllTasks(): Flow<List<TaskItem>> {
        return taskDao.observeAllTasksWithDetails().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeTask(taskId: Long): Flow<TaskItem?> {
        return taskDao.observeTaskWithDetails(taskId).map { it?.toDomain() }
    }

    override fun observeCategories(): Flow<List<CategoryItem>> {
        return categoryDao.observeAllCategories().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeUpcomingReminders(): Flow<List<ReminderItem>> {
        return reminderDao.observeUpcomingReminders().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeFocusSessions(): Flow<List<FocusSessionItem>> {
        return focusSessionDao.observeAllSessions().map { list ->
            list.map {
                FocusSessionItem(
                    id = it.id,
                    taskId = it.taskId,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    durationSeconds = it.duration,
                    sessionType = it.sessionType
                )
            }
        }
    }

    override suspend fun ensureSeeded() {
        val existing = categoryDao.getAllCategoriesSync()
        if (existing.isEmpty()) {
            categoryDao.insertCategories(AppDatabase.DEFAULT_CATEGORIES)
        }
    }

    override suspend fun saveTask(
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
        reminderOptions: List<ReminderOption>
    ): Long {
        ensureSeeded()
        val now = System.currentTimeMillis()
        val existing = if (taskId != null && taskId > 0L) taskDao.getTaskWithDetailsById(taskId) else null

        // Cancel existing alarms & notifications before updating reminders
        if (existing != null) {
            existing.reminders.forEach { rem ->
                alarmScheduler.cancelReminder(rem)
                notificationHelper.cancelNotification(rem.notificationId)
            }
        }

        val effectiveRecurrence = if (isRecurring && recurrenceRule.type.name != "NONE") {
            recurrenceRule.toJson()
        } else null
        val effectiveIsRecurring = effectiveRecurrence != null

        val savedId = database.withTransaction {
            val entity = TaskEntity(
                id = existing?.task?.id ?: 0L,
                title = title.trim(),
                description = description.trim(),
                notes = notes.trim(),
                categoryId = categoryId,
                priority = priority.level,
                scheduledDate = scheduledDate,
                startTime = startTime,
                dueTime = dueTime,
                isCompleted = existing?.task?.isCompleted ?: false,
                completedAt = existing?.task?.completedAt,
                isRecurring = effectiveIsRecurring,
                recurrenceRule = effectiveRecurrence,
                overdueNotified = false,
                createdAt = existing?.task?.createdAt ?: now,
                updatedAt = now
            )
            val id = taskDao.insertTask(entity)

            // Sync subtasks cleanly preserving completion for matching IDs
            val oldCompletedIds = existing?.subtasks
                ?.filter { it.isCompleted }
                ?.map { it.id }
                ?.toSet() ?: emptySet()
            subtaskDao.deleteSubtasksForTask(id)
            val cleanedSubtasks = subtasks
                .map { it.first to it.second.trim() }
                .filter { it.second.isNotEmpty() }
                .map { (subId, subTitle) ->
                    SubtaskEntity(
                        id = 0L,
                        taskId = id,
                        title = subTitle,
                        isCompleted = subId in oldCompletedIds
                    )
                }
            if (cleanedSubtasks.isNotEmpty()) {
                subtaskDao.insertSubtasks(cleanedSubtasks)
            }

            // Rebuild reminders
            reminderDao.deleteRemindersForTask(id)
            val newReminders = buildRemindersForTask(
                taskId = id,
                scheduledDate = scheduledDate,
                startTime = startTime,
                dueTime = dueTime,
                isCompleted = entity.isCompleted,
                reminderOptions = reminderOptions,
                now = now
            )
            if (newReminders.isNotEmpty()) {
                val insertedIds = reminderDao.insertReminders(newReminders)
                newReminders.forEachIndexed { index, rem ->
                    val withId = rem.copy(id = insertedIds[index])
                    alarmScheduler.scheduleReminder(withId)
                }
            }
            id
        }
        return savedId
    }

    override suspend fun toggleTaskCompleted(taskId: Long, completed: Boolean) {
        val details = taskDao.getTaskWithDetailsById(taskId) ?: return
        val now = System.currentTimeMillis()
        taskDao.setTaskCompleted(
            taskId = taskId,
            completed = completed,
            completedAt = if (completed) now else null,
            updatedAt = now
        )

        if (completed) {
            // Cancel all pending alarms and dismiss notifications
            details.reminders.forEach { rem ->
                alarmScheduler.cancelReminder(rem)
                notificationHelper.cancelNotification(rem.notificationId)
            }
            // Advance recurring task cleanly without duplicating
            if (details.task.isRecurring) {
                val rule = RecurrenceRule.fromJson(details.task.recurrenceRule)
                val nextDate = RecurrenceUtils.computeNextOccurrenceDate(details.task.scheduledDate, rule)
                if (nextDate != null) {
                    val existingCount = taskDao.countRecurringOccurrence(details.task.title, nextDate)
                    if (existingCount == 0) {
                        val reminderOptions = details.reminders
                            .filter { it.reminderType != ReminderType.OVERDUE.name && it.reminderType != ReminderType.SNOOZED.name }
                            .map { ReminderOption(ReminderType.fromString(it.reminderType), it.offsetMinutes) }
                            .distinctBy { it.offsetMinutes }
                        saveTask(
                            taskId = null,
                            title = details.task.title,
                            description = details.task.description,
                            notes = details.task.notes,
                            categoryId = details.task.categoryId,
                            priority = TaskPriority.fromLevel(details.task.priority),
                            scheduledDate = nextDate,
                            startTime = details.task.startTime,
                            dueTime = details.task.dueTime,
                            isRecurring = true,
                            recurrenceRule = rule,
                            subtasks = details.subtasks.map { 0L to it.title },
                            reminderOptions = reminderOptions
                        )
                    }
                }
            }
        } else {
            // Reopened: re-arm upcoming alarms
            taskDao.setOverdueNotified(taskId, notified = false, updatedAt = now)
            details.reminders.forEach { rem ->
                if (!rem.isFired && rem.isEnabled && rem.reminderDateTime > now) {
                    alarmScheduler.scheduleReminder(rem)
                }
            }
        }
    }

    override suspend fun rescheduleTaskToToday(taskId: Long) {
        val details = taskDao.getTaskWithDetailsById(taskId) ?: return
        val options = details.reminders
            .filter { it.reminderType != ReminderType.OVERDUE.name && it.reminderType != ReminderType.SNOOZED.name }
            .map { ReminderOption(ReminderType.fromString(it.reminderType), it.offsetMinutes) }
        saveTask(
            taskId = details.task.id,
            title = details.task.title,
            description = details.task.description,
            notes = details.task.notes,
            categoryId = details.task.categoryId,
            priority = TaskPriority.fromLevel(details.task.priority),
            scheduledDate = DateTimeUtils.todayEpochDay(),
            startTime = details.task.startTime,
            dueTime = details.task.dueTime,
            isRecurring = details.task.isRecurring,
            recurrenceRule = RecurrenceRule.fromJson(details.task.recurrenceRule),
            subtasks = details.subtasks.map { it.id to it.title },
            reminderOptions = options
        )
    }

    override suspend fun deleteTask(taskId: Long) {
        val reminders = reminderDao.getRemindersForTask(taskId)
        reminders.forEach { rem ->
            alarmScheduler.cancelReminder(rem)
            notificationHelper.cancelNotification(rem.notificationId)
        }
        taskDao.deleteTaskById(taskId)
    }

    override suspend fun toggleSubtask(subtaskId: Long, completed: Boolean) {
        subtaskDao.setSubtaskCompleted(subtaskId, completed)
    }

    override suspend fun addSubtask(taskId: Long, title: String) {
        val cleaned = title.trim()
        if (cleaned.isNotEmpty()) {
            subtaskDao.insertSubtask(SubtaskEntity(taskId = taskId, title = cleaned, isCompleted = false))
        }
    }

    override suspend fun setReminderEnabled(reminderId: Long, enabled: Boolean) {
        reminderDao.setReminderEnabled(reminderId, enabled)
        val reminder = reminderDao.getReminderById(reminderId) ?: return
        if (enabled) {
            alarmScheduler.scheduleReminder(reminder.copy(isEnabled = true))
        } else {
            alarmScheduler.cancelReminder(reminder)
            notificationHelper.cancelNotification(reminder.notificationId)
        }
    }

    override suspend fun snoozeTaskReminder(
        taskId: Long,
        snoozeMinutes: Int,
        dismissedNotificationId: Int
    ) {
        notificationHelper.cancelNotification(dismissedNotificationId)
        val details = taskDao.getTaskWithDetailsById(taskId) ?: return
        if (details.task.isCompleted) return

        val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
        val notifId = generateUniqueNotificationId(taskId, 900 + snoozeMinutes, triggerAt)
        val snoozeReminder = ReminderEntity(
            taskId = taskId,
            reminderDateTime = triggerAt,
            reminderType = ReminderType.SNOOZED.name,
            offsetMinutes = snoozeMinutes,
            isEnabled = true,
            isFired = false,
            notificationId = notifId
        )
        val id = reminderDao.insertReminder(snoozeReminder)
        alarmScheduler.scheduleReminder(snoozeReminder.copy(id = id))
    }

    override suspend fun rescheduleAllPendingReminders() {
        val now = System.currentTimeMillis()
        val prefs = preferencesRepository.preferencesFlow.first()
        val pendingTasks = taskDao.getPendingTasksSync()

        for (task in pendingTasks) {
            val reminders = reminderDao.getRemindersForTask(task.id)
            for (rem in reminders) {
                if (!rem.isEnabled || rem.isFired) continue

                val type = ReminderType.fromString(rem.reminderType)
                // Recompute wall-clock trigger for non-snoozed reminders in case timezone/clock changed
                val updatedTrigger = if (type == ReminderType.SNOOZED) {
                    rem.reminderDateTime
                } else if (type == ReminderType.OVERDUE) {
                    val targetMin = task.dueTime ?: task.startTime
                    if (targetMin != null) DateTimeUtils.toEpochMillis(task.scheduledDate, targetMin) + 60_000L else rem.reminderDateTime
                } else {
                    val baseMin = task.startTime ?: task.dueTime
                    if (baseMin != null) {
                        DateTimeUtils.toEpochMillis(task.scheduledDate, baseMin) - rem.offsetMinutes * 60_000L
                    } else rem.reminderDateTime
                }

                val updatedRem = if (updatedTrigger != rem.reminderDateTime) {
                    rem.copy(reminderDateTime = updatedTrigger).also { reminderDao.updateReminder(it) }
                } else rem

                if (updatedRem.reminderDateTime > now) {
                    alarmScheduler.scheduleReminder(updatedRem)
                } else if (type == ReminderType.OVERDUE && !task.overdueNotified) {
                    // Task became overdue while device was off/restarting -> fire single overdue alert
                    reminderDao.markFired(updatedRem.id)
                    taskDao.setOverdueNotified(task.id, notified = true, updatedAt = now)
                    notificationHelper.showTaskReminderNotification(
                        taskId = task.id,
                        taskTitle = task.title,
                        scheduledTimeText = DateTimeUtils.formatTimeRange(task.startTime, task.dueTime),
                        offsetMinutes = 0,
                        reminderType = ReminderType.OVERDUE,
                        notificationId = updatedRem.notificationId,
                        soundEnabled = prefs.notificationSoundEnabled,
                        vibrationEnabled = prefs.notificationVibrationEnabled
                    )
                }
            }
        }
    }

    override suspend fun createCategory(name: String, color: Int): Long {
        return categoryDao.insertCategory(
            CategoryEntity(name = name.trim(), color = color, isDefault = false)
        )
    }

    override suspend fun updateCategory(id: Long, name: String, color: Int) {
        val existing = categoryDao.getCategoryById(id) ?: return
        categoryDao.updateCategory(existing.copy(name = name.trim(), color = color))
    }

    override suspend fun deleteCustomCategory(id: Long) {
        ensureSeeded()
        database.withTransaction {
            taskDao.reassignCategory(oldCategoryId = id, newCategoryId = AppDatabase.DEFAULT_OTHER_CATEGORY_ID)
            categoryDao.deleteCustomCategory(id)
        }
    }

    override suspend fun logFocusSession(
        taskId: Long?,
        startTime: Long,
        endTime: Long,
        durationSeconds: Long,
        sessionType: String
    ) {
        focusSessionDao.insertSession(
            FocusSessionEntity(
                taskId = taskId,
                startTime = startTime,
                endTime = endTime,
                duration = durationSeconds,
                sessionType = sessionType
            )
        )
    }

    override suspend fun clearAllLocalData() {
        val allReminders = reminderDao.getAllRemindersSync()
        allReminders.forEach { rem ->
            alarmScheduler.cancelReminder(rem)
            notificationHelper.cancelNotification(rem.notificationId)
        }
        database.withTransaction {
            reminderDao.deleteAllReminders()
            subtaskDao.deleteAllSubtasks()
            focusSessionDao.deleteAllSessions()
            taskDao.deleteAllTasks()
            categoryDao.deleteAllCategories()
            categoryDao.insertCategories(AppDatabase.DEFAULT_CATEGORIES)
        }
    }

    private fun buildRemindersForTask(
        taskId: Long,
        scheduledDate: Long,
        startTime: Long?,
        dueTime: Long?,
        isCompleted: Boolean,
        reminderOptions: List<ReminderOption>,
        now: Long
    ): List<ReminderEntity> {
        if (isCompleted) return emptyList()
        val baseTimeMinutes = startTime ?: dueTime ?: return emptyList()
        val baseMillis = DateTimeUtils.toEpochMillis(scheduledDate, baseTimeMinutes)

        val result = mutableListOf<ReminderEntity>()
        val seenOffsets = mutableSetOf<Int>()

        for (option in reminderOptions) {
            if (!seenOffsets.add(option.offsetMinutes)) continue
            val triggerMillis = baseMillis - (option.offsetMinutes * 60_000L)
            if (triggerMillis > now) {
                result += ReminderEntity(
                    taskId = taskId,
                    reminderDateTime = triggerMillis,
                    reminderType = option.type.name,
                    offsetMinutes = option.offsetMinutes,
                    isEnabled = true,
                    isFired = false,
                    notificationId = generateUniqueNotificationId(taskId, option.offsetMinutes, triggerMillis)
                )
            }
        }

        // Single overdue reminder 1 minute after due time (or start time if no due time)
        val overdueBaseMinutes = dueTime ?: startTime
        if (overdueBaseMinutes != null) {
            val overdueMillis = DateTimeUtils.toEpochMillis(scheduledDate, overdueBaseMinutes) + 60_000L
            if (overdueMillis > now) {
                result += ReminderEntity(
                    taskId = taskId,
                    reminderDateTime = overdueMillis,
                    reminderType = ReminderType.OVERDUE.name,
                    offsetMinutes = 0,
                    isEnabled = true,
                    isFired = false,
                    notificationId = generateUniqueNotificationId(taskId, 7777, overdueMillis)
                )
            }
        }
        return result
    }

    private fun generateUniqueNotificationId(taskId: Long, salt: Int, timestamp: Long): Int {
        val raw = ((taskId * 31L + salt) * 31L + (timestamp / 1000L)).toInt()
        return abs(raw).coerceAtLeast(100)
    }

    private fun TaskWithDetails.toDomain(): TaskItem {
        val rule = RecurrenceRule.fromJson(task.recurrenceRule)
        return TaskItem(
            id = task.id,
            title = task.title,
            description = task.description,
            notes = task.notes,
            categoryId = task.categoryId,
            category = category?.toDomain(),
            priority = TaskPriority.fromLevel(task.priority),
            scheduledDate = task.scheduledDate,
            startTime = task.startTime,
            dueTime = task.dueTime,
            isCompleted = task.isCompleted,
            completedAt = task.completedAt,
            isRecurring = task.isRecurring,
            recurrenceRule = rule,
            overdueNotified = task.overdueNotified,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt,
            subtasks = subtasks.map {
                SubtaskItem(
                    id = it.id,
                    taskId = it.taskId,
                    title = it.title,
                    isCompleted = it.isCompleted
                )
            },
            reminders = reminders.map { it.toDomain() }
        )
    }

    private fun CategoryEntity.toDomain() = CategoryItem(
        id = id,
        name = name,
        color = color,
        isDefault = isDefault
    )

    private fun ReminderEntity.toDomain() = ReminderItem(
        id = id,
        taskId = taskId,
        reminderDateTime = reminderDateTime,
        reminderType = ReminderType.fromString(reminderType),
        offsetMinutes = offsetMinutes,
        isEnabled = isEnabled,
        isFired = isFired,
        notificationId = notificationId
    )
}
