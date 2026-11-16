package com.example.utils

import android.content.Context
import android.net.Uri
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
import com.example.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CategoryBackupDto(
    val id: Long,
    val name: String,
    val color: Int,
    val isDefault: Boolean
)

@Serializable
data class TaskBackupDto(
    val id: Long,
    val title: String,
    val description: String,
    val notes: String,
    val categoryId: Long,
    val priority: Int,
    val scheduledDate: Long,
    val startTime: Long?,
    val dueTime: Long?,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val isRecurring: Boolean,
    val recurrenceRule: String?,
    val overdueNotified: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SubtaskBackupDto(
    val id: Long,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean
)

@Serializable
data class ReminderBackupDto(
    val id: Long,
    val taskId: Long,
    val reminderDateTime: Long,
    val reminderType: String,
    val offsetMinutes: Int,
    val isEnabled: Boolean,
    val isFired: Boolean,
    val notificationId: Int
)

@Serializable
data class FocusSessionBackupDto(
    val id: Long,
    val taskId: Long?,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val sessionType: String
)

@Serializable
data class JustDoBackupEnvelope(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val categories: List<CategoryBackupDto> = emptyList(),
    val tasks: List<TaskBackupDto> = emptyList(),
    val subtasks: List<SubtaskBackupDto> = emptyList(),
    val reminders: List<ReminderBackupDto> = emptyList(),
    val focusSessions: List<FocusSessionBackupDto> = emptyList()
)

enum class RestoreMode {
    REPLACE, MERGE
}

data class RestoreResult(
    val tasksRestored: Int,
    val categoriesRestored: Int,
    val focusSessionsRestored: Int
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val subtaskDao: SubtaskDao,
    private val reminderDao: ReminderDao,
    private val focusSessionDao: FocusSessionDao,
    private val taskRepository: TaskRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportJsonToUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val categories = categoryDao.getAllCategoriesSync()
            val tasks = taskDao.getAllTasksSync()
            val subtasks = subtaskDao.getAllSubtasksSync()
            val reminders = reminderDao.getAllRemindersSync()
            val sessions = focusSessionDao.getAllSessionsSync()

            val envelope = JustDoBackupEnvelope(
                schemaVersion = 1,
                exportedAt = System.currentTimeMillis(),
                categories = categories.map {
                    CategoryBackupDto(it.id, it.name, it.color, it.isDefault)
                },
                tasks = tasks.map {
                    TaskBackupDto(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        notes = it.notes,
                        categoryId = it.categoryId,
                        priority = it.priority,
                        scheduledDate = it.scheduledDate,
                        startTime = it.startTime,
                        dueTime = it.dueTime,
                        isCompleted = it.isCompleted,
                        completedAt = it.completedAt,
                        isRecurring = it.isRecurring,
                        recurrenceRule = it.recurrenceRule,
                        overdueNotified = it.overdueNotified,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                },
                subtasks = subtasks.map {
                    SubtaskBackupDto(it.id, it.taskId, it.title, it.isCompleted)
                },
                reminders = reminders.map {
                    ReminderBackupDto(
                        id = it.id,
                        taskId = it.taskId,
                        reminderDateTime = it.reminderDateTime,
                        reminderType = it.reminderType,
                        offsetMinutes = it.offsetMinutes,
                        isEnabled = it.isEnabled,
                        isFired = it.isFired,
                        notificationId = it.notificationId
                    )
                },
                focusSessions = sessions.map {
                    FocusSessionBackupDto(
                        id = it.id,
                        taskId = it.taskId,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        duration = it.duration,
                        sessionType = it.sessionType
                    )
                }
            )

            val content = json.encodeToString(envelope)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(content)
                }
            } ?: error("Unable to open output stream")

            tasks.size
        }
    }

    suspend fun exportCsvToUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val categoriesById = categoryDao.getAllCategoriesSync().associateBy { it.id }
            val tasks = taskDao.getAllTasksSync()
            val subtasksByTask = subtaskDao.getAllSubtasksSync().groupBy { it.taskId }

            val sb = StringBuilder()
            sb.appendLine(
                "id,title,description,notes,category,priority,scheduled_date,start_time,due_time,is_completed,is_recurring,recurrence_rule,subtasks_count,created_at"
            )
            tasks.forEach { t ->
                val catName = categoriesById[t.categoryId]?.name ?: "Other"
                val dateStr = DateTimeUtils.epochDayToLocalDate(t.scheduledDate).toString()
                val startStr = t.startTime?.let { DateTimeUtils.formatMinutes(it) } ?: ""
                val dueStr = t.dueTime?.let { DateTimeUtils.formatMinutes(it) } ?: ""
                val subsCount = subtasksByTask[t.id]?.size ?: 0
                val row = listOf(
                    t.id.toString(),
                    csvEscape(t.title),
                    csvEscape(t.description),
                    csvEscape(t.notes),
                    csvEscape(catName),
                    t.priority.toString(),
                    csvEscape(dateStr),
                    csvEscape(startStr),
                    csvEscape(dueStr),
                    t.isCompleted.toString(),
                    t.isRecurring.toString(),
                    csvEscape(t.recurrenceRule ?: ""),
                    subsCount.toString(),
                    t.createdAt.toString()
                ).joinToString(",")
                sb.appendLine(row)
            }

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(sb.toString())
                }
            } ?: error("Unable to open output stream")

            tasks.size
        }
    }

    suspend fun restoreJsonFromUri(uri: Uri, mode: RestoreMode): Result<RestoreResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } ?: error("Unable to read backup file")

                val envelope = json.decodeFromString<JustDoBackupEnvelope>(raw)

                if (mode == RestoreMode.REPLACE) {
                    taskRepository.clearAllLocalData()
                } else {
                    taskRepository.ensureSeeded()
                }

                database.withTransaction {
                    val existingCategories = categoryDao.getAllCategoriesSync()
                    val catNameMap = existingCategories.associateBy { it.name.lowercase() }.toMutableMap()
                    val categoryIdMap = mutableMapOf<Long, Long>()

                    envelope.categories.forEach { dto ->
                        val existing = catNameMap[dto.name.lowercase()]
                        if (existing != null) {
                            categoryIdMap[dto.id] = existing.id
                        } else {
                            val newId = categoryDao.insertCategory(
                                CategoryEntity(
                                    id = 0L,
                                    name = dto.name,
                                    color = dto.color,
                                    isDefault = dto.isDefault
                                )
                            )
                            val inserted = CategoryEntity(newId, dto.name, dto.color, dto.isDefault)
                            catNameMap[dto.name.lowercase()] = inserted
                            categoryIdMap[dto.id] = newId
                        }
                    }

                    val taskIdMap = mutableMapOf<Long, Long>()
                    envelope.tasks.forEach { dto ->
                        val mappedCategoryId = categoryIdMap[dto.categoryId]
                            ?: AppDatabase.DEFAULT_OTHER_CATEGORY_ID
                        val newTaskId = taskDao.insertTask(
                            TaskEntity(
                                id = 0L,
                                title = dto.title,
                                description = dto.description,
                                notes = dto.notes,
                                categoryId = mappedCategoryId,
                                priority = dto.priority,
                                scheduledDate = dto.scheduledDate,
                                startTime = dto.startTime,
                                dueTime = dto.dueTime,
                                isCompleted = dto.isCompleted,
                                completedAt = dto.completedAt,
                                isRecurring = dto.isRecurring,
                                recurrenceRule = dto.recurrenceRule,
                                overdueNotified = dto.overdueNotified,
                                createdAt = dto.createdAt,
                                updatedAt = dto.updatedAt
                            )
                        )
                        taskIdMap[dto.id] = newTaskId
                    }

                    val subtasksToInsert = envelope.subtasks.mapNotNull { sub ->
                        val mappedTaskId = taskIdMap[sub.taskId] ?: return@mapNotNull null
                        SubtaskEntity(
                            id = 0L,
                            taskId = mappedTaskId,
                            title = sub.title,
                            isCompleted = sub.isCompleted
                        )
                    }
                    if (subtasksToInsert.isNotEmpty()) {
                        subtaskDao.insertSubtasks(subtasksToInsert)
                    }

                    val remindersToInsert = envelope.reminders.mapNotNull { rem ->
                        val mappedTaskId = taskIdMap[rem.taskId] ?: return@mapNotNull null
                        ReminderEntity(
                            id = 0L,
                            taskId = mappedTaskId,
                            reminderDateTime = rem.reminderDateTime,
                            reminderType = rem.reminderType,
                            offsetMinutes = rem.offsetMinutes,
                            isEnabled = rem.isEnabled,
                            isFired = rem.isFired,
                            notificationId = kotlin.math.abs((mappedTaskId * 37L + rem.offsetMinutes + rem.notificationId).toInt()).coerceAtLeast(100)
                        )
                    }
                    if (remindersToInsert.isNotEmpty()) {
                        reminderDao.insertReminders(remindersToInsert)
                    }

                    val sessionsToInsert = envelope.focusSessions.map { s ->
                        FocusSessionEntity(
                            id = 0L,
                            taskId = s.taskId?.let { taskIdMap[it] },
                            startTime = s.startTime,
                            endTime = s.endTime,
                            duration = s.duration,
                            sessionType = s.sessionType
                        )
                    }
                    if (sessionsToInsert.isNotEmpty()) {
                        focusSessionDao.insertSessions(sessionsToInsert)
                    }
                }

                taskRepository.rescheduleAllPendingReminders()

                RestoreResult(
                    tasksRestored = envelope.tasks.size,
                    categoriesRestored = envelope.categories.size,
                    focusSessionsRestored = envelope.focusSessions.size
                )
            }
        }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
