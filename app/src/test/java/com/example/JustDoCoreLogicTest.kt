package com.example

import com.example.domain.model.AnalyticsRange
import com.example.domain.model.CategoryItem
import com.example.domain.model.FocusSessionItem
import com.example.domain.model.StatusFilter
import com.example.domain.model.TaskFilter
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.usecase.ComputeAnalyticsUseCase
import com.example.domain.usecase.FilterAndSortTasksUseCase
import com.example.utils.DateTimeUtils
import com.example.utils.RecurrenceRule
import com.example.utils.RecurrenceType
import com.example.utils.RecurrenceUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class JustDoCoreLogicTest {

    @Test
    fun `weekdays recurrence skips weekend`() {
        val friday = LocalDate.of(2026, 11, 13) // Friday
        val rule = RecurrenceRule(type = RecurrenceType.WEEKDAYS)
        val nextEpoch = RecurrenceUtils.computeNextOccurrenceDate(friday.toEpochDay(), rule)
        val nextDate = LocalDate.ofEpochDay(nextEpoch!!)
        assertEquals(DayOfWeek.MONDAY, nextDate.dayOfWeek)
    }

    @Test
    fun `custom days of week recurrence picks next matching weekday`() {
        val monday = LocalDate.of(2026, 11, 16) // Monday (1)
        val rule = RecurrenceRule(
            type = RecurrenceType.CUSTOM,
            daysOfWeek = listOf(3, 5) // Wed, Fri
        )
        val nextEpoch = RecurrenceUtils.computeNextOccurrenceDate(monday.toEpochDay(), rule)
        val nextDate = LocalDate.ofEpochDay(nextEpoch!!)
        assertEquals(DayOfWeek.WEDNESDAY, nextDate.dayOfWeek)
    }

    @Test
    fun `filter and sort narrows by search query and priority`() {
        val today = DateTimeUtils.todayEpochDay()
        val tasks = listOf(
            sampleTask(1L, "Java DSA Practice", TaskPriority.HIGH, today, false),
            sampleTask(2L, "Grocery Run", TaskPriority.LOW, today, false),
            sampleTask(3L, "Java Streams Notes", TaskPriority.MEDIUM, today, true)
        )
        val useCase = FilterAndSortTasksUseCase()
        val res = useCase(
            tasks = tasks,
            filter = TaskFilter(query = "java", status = StatusFilter.PENDING)
        )
        assertEquals(1, res.size)
        assertEquals("Java DSA Practice", res.first().title)
    }

    @Test
    fun `analytics computes completion rate and streak accurately`() {
        val today = LocalDate.now().toEpochDay()
        val yesterday = today - 1L
        val tasks = listOf(
            sampleTask(1L, "Task 1", TaskPriority.HIGH, today, true, DateTimeUtils.toEpochMillis(today, 600L)),
            sampleTask(2L, "Task 2", TaskPriority.MEDIUM, today, false, null),
            sampleTask(3L, "Task 3", TaskPriority.LOW, yesterday, true, DateTimeUtils.toEpochMillis(yesterday, 700L))
        )
        val categories = listOf(CategoryItem(1L, "Study", 0xFF4F46E5.toInt(), true))
        val sessions = listOf(FocusSessionItem(1L, 1L, System.currentTimeMillis() - 1500_000L, System.currentTimeMillis(), 1500L, "FOCUS"))

        val analytics = ComputeAnalyticsUseCase()(
            tasks = tasks,
            categories = categories,
            focusSessions = sessions,
            range = AnalyticsRange.WEEKLY
        )
        assertEquals(3, analytics.totalTasks)
        assertEquals(2, analytics.completedTasks)
        assertEquals(2, analytics.currentStreakDays)
        assertTrue(analytics.totalFocusMinutes >= 25L)
        assertFalse(analytics.categoryDistribution.isEmpty())
    }

    private fun sampleTask(
        id: Long,
        title: String,
        priority: TaskPriority,
        date: Long,
        completed: Boolean,
        completedAt: Long? = null
    ) = TaskItem(
        id = id,
        title = title,
        description = "",
        notes = "",
        categoryId = 1L,
        category = CategoryItem(1L, "Study", 0xFF4F46E5.toInt(), true),
        priority = priority,
        scheduledDate = date,
        startTime = 540L,
        dueTime = 600L,
        isCompleted = completed,
        completedAt = completedAt,
        isRecurring = false,
        recurrenceRule = RecurrenceRule(),
        overdueNotified = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        subtasks = emptyList(),
        reminders = emptyList()
    )
}
