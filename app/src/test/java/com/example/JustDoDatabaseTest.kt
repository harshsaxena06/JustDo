package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.TaskRepositoryImpl
import com.example.domain.model.ReminderOption
import com.example.domain.model.ReminderType
import com.example.domain.model.TaskPriority
import com.example.notifications.manager.NotificationHelper
import com.example.notifications.scheduler.AlarmScheduler
import com.example.utils.DateTimeUtils
import com.example.utils.RecurrenceRule
import com.example.utils.RecurrenceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JustDoDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TaskRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TaskRepositoryImpl(
            database = db,
            taskDao = db.taskDao(),
            categoryDao = db.categoryDao(),
            reminderDao = db.reminderDao(),
            subtaskDao = db.subtaskDao(),
            focusSessionDao = db.focusSessionDao(),
            alarmScheduler = AlarmScheduler(context),
            notificationHelper = NotificationHelper(context),
            preferencesRepository = UserPreferencesRepository(context)
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `seed categories and save task with subtasks and reminders`() = runTest {
        repository.ensureSeeded()
        val categories = repository.observeCategories().first()
        assertEquals(7, categories.size)

        val futureDate = DateTimeUtils.todayEpochDay() + 1L
        val taskId = repository.saveTask(
            title = "Complete Java DSA Practice",
            description = "Trees and Graphs",
            notes = "Focus on BFS & DFS",
            categoryId = 1L,
            priority = TaskPriority.HIGH,
            scheduledDate = futureDate,
            startTime = 17 * 60L,
            dueTime = 18 * 60L,
            isRecurring = false,
            recurrenceRule = RecurrenceRule(),
            subtasks = listOf(0L to "Solve 3 tree problems", 0L to "Review graph templates"),
            reminderOptions = listOf(
                ReminderOption(ReminderType.BEFORE_15M, 15),
                ReminderOption(ReminderType.AT_TIME, 0)
            )
        )

        val saved = repository.observeTask(taskId).first()
        assertNotNull(saved)
        assertEquals(2, saved!!.subtasks.size)
        // 2 reminder offsets + 1 single overdue reminder
        assertEquals(3, saved.reminders.size)
    }

    @Test
    fun `completing recurring task schedules single next occurrence without duplicates`() = runTest {
        repository.ensureSeeded()
        val today = DateTimeUtils.todayEpochDay()
        val taskId = repository.saveTask(
            title = "Morning Workout",
            description = "",
            notes = "",
            categoryId = 6L,
            priority = TaskPriority.MEDIUM,
            scheduledDate = today,
            startTime = 7 * 60L,
            dueTime = 8 * 60L,
            isRecurring = true,
            recurrenceRule = RecurrenceRule(type = RecurrenceType.DAILY),
            subtasks = listOf(0L to "Warmup"),
            reminderOptions = listOf(ReminderOption(ReminderType.BEFORE_15M, 15))
        )

        repository.toggleTaskCompleted(taskId, completed = true)
        val allTasks = repository.observeAllTasks().first()
        assertEquals(2, allTasks.size)

        // Toggle false then true again should NOT duplicate tomorrow's occurrence
        repository.toggleTaskCompleted(taskId, completed = false)
        repository.toggleTaskCompleted(taskId, completed = true)
        val afterRepeatToggle = repository.observeAllTasks().first()
        assertEquals(2, afterRepeatToggle.size)
    }

    @Test
    fun `deleting custom category reassigns tasks to Other`() = runTest {
        repository.ensureSeeded()
        val customCatId = repository.createCategory("Side Project", 0xFF7C3AED.toInt())
        val taskId = repository.saveTask(
            title = "Build JustDo",
            description = "",
            notes = "",
            categoryId = customCatId,
            priority = TaskPriority.URGENT,
            scheduledDate = DateTimeUtils.todayEpochDay(),
            startTime = null,
            dueTime = null,
            isRecurring = false,
            recurrenceRule = RecurrenceRule(),
            subtasks = emptyList(),
            reminderOptions = emptyList()
        )

        repository.deleteCustomCategory(customCatId)
        val updatedTask = repository.observeTask(taskId).first()
        assertNotNull(updatedTask)
        assertEquals(AppDatabase.DEFAULT_OTHER_CATEGORY_ID, updatedTask!!.categoryId)
        assertTrue(repository.observeCategories().first().none { it.id == customCatId })
    }
}
