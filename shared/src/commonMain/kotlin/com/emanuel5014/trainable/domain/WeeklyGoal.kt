package com.emanuel5014.trainable.domain

/** Workouts per week the user aims for: a count, not weekdays, so it can go past 7 (two a day, for example). */
object WeeklyGoal {
    const val DEFAULT = 3
    const val MAX = 50

    /** One tap of − or + from [current]. */
    fun step(current: Int, delta: Int): Int = (current + delta).coerceIn(1, MAX)

    /** A typed value; empty or below 1 means the default. */
    fun parse(text: String): Int = text.trim().toIntOrNull()?.takeIf { it >= 1 }?.coerceAtMost(MAX) ?: DEFAULT
}
