package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.TaskEntity
import com.example.data.entity.TaskWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY scheduledDate ASC, COALESCE(startTime, dueTime, 9999) ASC, priority DESC")
    fun observeAllTasksWithDetails(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskWithDetails(taskId: Long): Flow<TaskWithDetails?>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskWithDetailsById(taskId: Long): TaskWithDetails?

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    suspend fun getPendingTasksSync(): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE title = :title AND scheduledDate = :scheduledDate AND isRecurring = 1")
    suspend fun countRecurringOccurrence(title: String, scheduledDate: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: Long, completed: Boolean, completedAt: Long?, updatedAt: Long)

    @Query("UPDATE tasks SET overdueNotified = :notified, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun setOverdueNotified(taskId: Long, notified: Boolean, updatedAt: Long)

    @Query("UPDATE tasks SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
