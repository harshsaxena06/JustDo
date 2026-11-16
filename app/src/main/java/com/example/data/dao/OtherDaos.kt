package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CategoryEntity
import com.example.data.entity.FocusSessionEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, id ASC")
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, id ASC")
    suspend fun getAllCategoriesSync(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id AND isDefault = 0")
    suspend fun deleteCustomCategory(id: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isFired = 0 ORDER BY reminderDateTime ASC")
    fun observeUpcomingReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersSync(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    suspend fun getRemindersForTask(taskId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isFired = 0")
    suspend fun getActiveUnfiredReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>): List<Long>

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isFired = 1 WHERE id = :reminderId")
    suspend fun markFired(reminderId: Long)

    @Query("UPDATE reminders SET isEnabled = :enabled WHERE id = :reminderId")
    suspend fun setReminderEnabled(reminderId: Long, enabled: Boolean)

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun deleteRemindersForTask(taskId: Long)

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks")
    suspend fun getAllSubtasksSync(): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY id ASC")
    suspend fun getSubtasksForTask(taskId: Long): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Query("UPDATE subtasks SET isCompleted = :completed WHERE id = :subtaskId")
    suspend fun setSubtaskCompleted(subtaskId: Long, completed: Boolean)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: Long)

    @Query("DELETE FROM subtasks")
    suspend fun deleteAllSubtasks()
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun observeAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsSync(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<FocusSessionEntity>)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllSessions()
}
