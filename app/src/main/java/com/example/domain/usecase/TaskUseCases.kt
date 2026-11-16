package com.example.domain.usecase

import com.example.domain.model.AnalyticsRange
import com.example.domain.model.AnalyticsSummary
import com.example.domain.model.CategoryDistributionItem
import com.example.domain.model.CategoryItem
import com.example.domain.model.DailyProductivityPoint
import com.example.domain.model.FocusSessionItem
import com.example.domain.model.StatusFilter
import com.example.domain.model.TaskFilter
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskSort
import com.example.utils.DateTimeUtils
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

class FilterAndSortTasksUseCase @Inject constructor() {
    operator fun invoke(
        tasks: List<TaskItem>,
        filter: TaskFilter,
        nowMillis: Long = System.currentTimeMillis()
    ): List<TaskItem> {
        val today = DateTimeUtils.todayEpochDay()
        val query = filter.query.trim().lowercase()

        val filtered = tasks.filter { task ->
            val matchesQuery = if (query.isEmpty()) true else {
                task.title.lowercase().contains(query) ||
                    task.description.lowercase().contains(query) ||
                    task.notes.lowercase().contains(query) ||
                    (task.category?.name?.lowercase()?.contains(query) == true)
            }
            val matchesCategory = filter.categoryId == null || task.categoryId == filter.categoryId
            val matchesPriority = filter.priority == null || task.priority == filter.priority
            val matchesDate = filter.dateEpochDay == null || task.scheduledDate == filter.dateEpochDay
            val matchesStatus = when (filter.status) {
                StatusFilter.ALL -> true
                StatusFilter.PENDING -> !task.isCompleted
                StatusFilter.COMPLETED -> task.isCompleted
                StatusFilter.OVERDUE -> task.isOverdue(nowMillis)
                StatusFilter.UPCOMING -> !task.isCompleted && task.scheduledDate >= today && !task.isOverdue(nowMillis)
            }
            matchesQuery && matchesCategory && matchesPriority && matchesDate && matchesStatus
        }

        return when (filter.sort) {
            TaskSort.TIME_ASC -> filtered.sortedWith(
                compareBy<TaskItem> { it.isCompleted }
                    .thenBy { it.scheduledDate }
                    .thenBy { it.startTime ?: it.dueTime ?: Long.MAX_VALUE }
                    .thenByDescending { it.priority.level }
            )
            TaskSort.PRIORITY_DESC -> filtered.sortedWith(
                compareBy<TaskItem> { it.isCompleted }
                    .thenByDescending { it.priority.level }
                    .thenBy { it.scheduledDate }
                    .thenBy { it.startTime ?: it.dueTime ?: Long.MAX_VALUE }
            )
            TaskSort.CREATED_DESC -> filtered.sortedWith(
                compareBy<TaskItem> { it.isCompleted }
                    .thenByDescending { it.createdAt }
            )
        }
    }
}

class ComputeAnalyticsUseCase @Inject constructor() {
    operator fun invoke(
        tasks: List<TaskItem>,
        categories: List<CategoryItem>,
        focusSessions: List<FocusSessionItem>,
        range: AnalyticsRange,
        nowMillis: Long = System.currentTimeMillis()
    ): AnalyticsSummary {
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val startEpoch = when (range) {
            AnalyticsRange.DAILY -> todayEpoch
            AnalyticsRange.WEEKLY -> today.minusDays(6).toEpochDay()
            AnalyticsRange.MONTHLY -> today.minusDays(29).toEpochDay()
        }

        val rangeTasks = tasks.filter { it.scheduledDate in startEpoch..todayEpoch }
        val total = rangeTasks.size
        val completed = rangeTasks.count { it.isCompleted }
        val pending = rangeTasks.count { !it.isCompleted }
        val overdue = rangeTasks.count { it.isOverdue(nowMillis) }
        val pct = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f

        // Streaks computed across all completed tasks by completion date (or scheduledDate fallback)
        val completedEpochDays = tasks
            .filter { it.isCompleted }
            .map { task ->
                val completedAt = task.completedAt
                if (completedAt != null) {
                    Instant.ofEpochMilli(completedAt).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                } else {
                    task.scheduledDate
                }
            }
            .toSet()

        val currentStreak = computeCurrentStreak(completedEpochDays, todayEpoch)
        val bestStreak = computeBestStreak(completedEpochDays)

        // Most productive weekday
        val completedByWeekday = tasks.filter { it.isCompleted }.groupBy { task ->
            val dayEpoch = task.completedAt?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
            } ?: task.scheduledDate
            LocalDate.ofEpochDay(dayEpoch).dayOfWeek
        }
        val bestWeekday: DayOfWeek? = completedByWeekday.maxByOrNull { it.value.size }?.key
        val mostProductiveDayLabel = bestWeekday?.getDisplayName(TextStyle.FULL, Locale.getDefault()) ?: "—"

        // Total focus minutes in range
        val rangeStartMillis = DateTimeUtils.toEpochMillis(startEpoch, 0L)
        val totalFocusSeconds = focusSessions
            .filter { it.sessionType == "FOCUS" && it.startTime >= rangeStartMillis }
            .sumOf { it.durationSeconds }
        val totalFocusMinutes = totalFocusSeconds / 60L

        // Chart points
        val chartPoints = when (range) {
            AnalyticsRange.DAILY -> {
                listOf(
                    DailyProductivityPoint("Today", todayEpoch, completed, total)
                )
            }
            AnalyticsRange.WEEKLY -> {
                (0..6).map { offset ->
                    val d = today.minusDays((6 - offset).toLong())
                    val e = d.toEpochDay()
                    val dayTasks = tasks.filter { it.scheduledDate == e }
                    DailyProductivityPoint(
                        label = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        epochDay = e,
                        completedCount = dayTasks.count { it.isCompleted },
                        totalCount = dayTasks.size
                    )
                }
            }
            AnalyticsRange.MONTHLY -> {
                // Group 30 days into 5 6-day buckets for clean readable bars
                (0..4).map { bucket ->
                    val bStart = today.minusDays((29 - bucket * 6).toLong())
                    val bEnd = if (bucket == 4) today else bStart.plusDays(5)
                    val sEpoch = bStart.toEpochDay()
                    val eEpoch = bEnd.toEpochDay()
                    val bucketTasks = tasks.filter { it.scheduledDate in sEpoch..eEpoch }
                    DailyProductivityPoint(
                        label = "${bStart.monthValue}/${bStart.dayOfMonth}",
                        epochDay = sEpoch,
                        completedCount = bucketTasks.count { it.isCompleted },
                        totalCount = bucketTasks.size
                    )
                }
            }
        }

        // Category distribution
        val dist = categories.mapNotNull { cat ->
            val catTasks = rangeTasks.filter { it.categoryId == cat.id }
            if (catTasks.isEmpty()) null else {
                CategoryDistributionItem(
                    category = cat,
                    totalCount = catTasks.size,
                    completedCount = catTasks.count { it.isCompleted },
                    percentage = if (total > 0) (catTasks.size.toFloat() / total.toFloat()) * 100f else 0f
                )
            }
        }.sortedByDescending { it.totalCount }

        return AnalyticsSummary(
            range = range,
            totalTasks = total,
            completedTasks = completed,
            pendingTasks = pending,
            overdueTasks = overdue,
            completionPercentage = pct,
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            mostProductiveDay = mostProductiveDayLabel,
            totalFocusMinutes = totalFocusMinutes,
            chartPoints = chartPoints,
            categoryDistribution = dist
        )
    }

    fun computeCurrentStreak(completedDays: Set<Long>, todayEpoch: Long): Int {
        if (completedDays.isEmpty()) return 0
        var cursor = if (todayEpoch in completedDays) todayEpoch else todayEpoch - 1
        if (cursor !in completedDays) return 0
        var streak = 0
        while (cursor in completedDays) {
            streak++
            cursor--
        }
        return streak
    }

    private fun computeBestStreak(completedDays: Set<Long>): Int {
        if (completedDays.isEmpty()) return 0
        val sorted = completedDays.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1L) {
                current++
                if (current > best) best = current
            } else {
                current = 1
            }
        }
        return best
    }
}
