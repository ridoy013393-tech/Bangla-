package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pure JVM helper for computing daily consecutive chat streaks (🔥).
 * Uses local calendar date keys ("yyyy-MM-dd") so streaks roll over naturally at midnight.
 */
object StreakCalculator {

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(nowMillis))
    }

    fun previousDayKey(dateKey: String): String? {
        if (dateKey.length != 10) return null
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val parsed = runCatching { fmt.parse(dateKey) }.getOrNull() ?: return null
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return fmt.format(cal.time)
    }

    /**
     * Computes the updated `(streakCount, lastStreakDate)` when a new message is sent today.
     * - Same day as [lastStreakDate]: preserves current streak (minimum 1).
     * - Consecutive day (yesterday): increments streak by 1.
     * - Older or empty: resets streak to 1.
     */
    fun computeNextStreak(
        currentStreak: Int,
        lastStreakDate: String,
        todayDate: String = todayDateKey()
    ): Pair<Int, String> {
        val cleanLast = lastStreakDate.trim()
        if (cleanLast == todayDate) {
            return currentStreak.coerceAtLeast(1) to todayDate
        }
        val yesterday = previousDayKey(todayDate)
        if (yesterday != null && cleanLast == yesterday) {
            return (currentStreak.coerceAtLeast(1) + 1).coerceAtMost(9999) to todayDate
        }
        return 1 to todayDate
    }

    /**
     * Returns the visible active streak count (0 if broken / older than yesterday).
     */
    fun activeStreakCount(
        streakCount: Int,
        lastStreakDate: String,
        todayDate: String = todayDateKey()
    ): Int {
        if (streakCount <= 0 || lastStreakDate.isBlank()) return 0
        val cleanLast = lastStreakDate.trim()
        if (cleanLast == todayDate) return streakCount
        val yesterday = previousDayKey(todayDate)
        return if (yesterday != null && cleanLast == yesterday) streakCount else 0
    }
}
