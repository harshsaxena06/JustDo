package com.example.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val clockSecondsFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault())

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun localDateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    fun minutesToLocalTime(minutes: Long): LocalTime {
        val safe = minutes.coerceIn(0L, 1439L)
        return LocalTime.of((safe / 60).toInt(), (safe % 60).toInt())
    }

    fun localTimeToMinutes(time: LocalTime): Long = (time.hour * 60 + time.minute).toLong()

    fun toEpochMillis(epochDay: Long, timeMinutes: Long?): Long {
        val date = LocalDate.ofEpochDay(epochDay)
        val time = if (timeMinutes != null) minutesToLocalTime(timeMinutes) else LocalTime.of(23, 59)
        return LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun formatMinutes(minutes: Long?): String {
        if (minutes == null) return "Anytime"
        return minutesToLocalTime(minutes).format(timeFormatter)
    }

    fun formatTimeRange(startMinutes: Long?, dueMinutes: Long?): String {
        return when {
            startMinutes != null && dueMinutes != null ->
                "${formatMinutes(startMinutes)} – ${formatMinutes(dueMinutes)}"
            startMinutes != null -> "Starts ${formatMinutes(startMinutes)}"
            dueMinutes != null -> "Due ${formatMinutes(dueMinutes)}"
            else -> "All Day"
        }
    }

    fun formatShortDate(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(shortDateFormatter)
        }
    }

    fun formatFullDate(date: LocalDate): String = date.format(fullDateFormatter)

    fun formatClockNow(nowMillis: Long = System.currentTimeMillis()): String {
        val ldt = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        return ldt.format(clockSecondsFormatter)
    }

    fun formatEpochMillis(millis: Long): String {
        val ldt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        return ldt.format(dateTimeFormatter)
    }

    fun formatCountdown(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0L)
        val mins = safe / 60
        val secs = safe % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    fun greetingForHour(hour: Int = LocalTime.now().hour): String {
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Late Night Focus"
        }
    }

    fun isTaskOverdue(
        scheduledDate: Long,
        startTime: Long?,
        dueTime: Long?,
        isCompleted: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (isCompleted) return false
        val today = todayEpochDay()
        if (scheduledDate < today) return true
        if (scheduledDate > today) return false
        val targetMinutes = dueTime ?: startTime ?: return false
        val deadlineMillis = toEpochMillis(scheduledDate, targetMinutes)
        return nowMillis > deadlineMillis
    }
}
