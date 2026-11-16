package com.example.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

@Serializable
enum class RecurrenceType(val label: String) {
    NONE("Does not repeat"),
    DAILY("Daily"),
    WEEKDAYS("Weekdays (Mon–Fri)"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom")
}

@Serializable
data class RecurrenceRule(
    val type: RecurrenceType = RecurrenceType.NONE,
    val intervalDays: Int = 1,
    val daysOfWeek: List<Int> = emptyList() // ISO-8601: 1 = Monday .. 7 = Sunday
) {
    fun toJson(): String = Json.encodeToString(this)

    fun displaySummary(): String {
        return when (type) {
            RecurrenceType.NONE -> "One-time"
            RecurrenceType.DAILY -> "Every day"
            RecurrenceType.WEEKDAYS -> "Weekdays (Mon–Fri)"
            RecurrenceType.WEEKLY -> "Every week"
            RecurrenceType.MONTHLY -> "Every month"
            RecurrenceType.CUSTOM -> {
                if (daysOfWeek.isNotEmpty()) {
                    val names = daysOfWeek.sorted().map { DayOfWeek.of(it).name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
                    "Custom: ${names.joinToString(", ")}"
                } else {
                    val safeInterval = intervalDays.coerceAtLeast(1)
                    if (safeInterval == 1) "Every day" else "Every $safeInterval days"
                }
            }
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(raw: String?): RecurrenceRule {
            if (raw.isNullOrBlank()) return RecurrenceRule()
            return runCatching { json.decodeFromString<RecurrenceRule>(raw) }
                .getOrElse { RecurrenceRule() }
        }
    }
}

object RecurrenceUtils {
    fun computeNextOccurrenceDate(currentEpochDay: Long, rule: RecurrenceRule): Long? {
        val current = LocalDate.ofEpochDay(currentEpochDay)
        val next = when (rule.type) {
            RecurrenceType.NONE -> return null
            RecurrenceType.DAILY -> current.plusDays(1)
            RecurrenceType.WEEKDAYS -> {
                var candidate = current.plusDays(1)
                while (candidate.dayOfWeek == DayOfWeek.SATURDAY || candidate.dayOfWeek == DayOfWeek.SUNDAY) {
                    candidate = candidate.plusDays(1)
                }
                candidate
            }
            RecurrenceType.WEEKLY -> current.plusWeeks(1)
            RecurrenceType.MONTHLY -> current.plusMonths(1)
            RecurrenceType.CUSTOM -> {
                if (rule.daysOfWeek.isNotEmpty()) {
                    val validDays = rule.daysOfWeek.filter { it in 1..7 }.toSet()
                    if (validDays.isEmpty()) {
                        current.plusDays(rule.intervalDays.coerceAtLeast(1).toLong())
                    } else {
                        var candidate = current.plusDays(1)
                        var guard = 0
                        while (candidate.dayOfWeek.value !in validDays && guard < 14) {
                            candidate = candidate.plusDays(1)
                            guard++
                        }
                        candidate
                    }
                } else {
                    current.plusDays(rule.intervalDays.coerceAtLeast(1).toLong())
                }
            }
        }
        return next.toEpochDay()
    }
}
