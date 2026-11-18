package com.emanuel5014.trainable.widget.model

import com.emanuel5014.trainable.domain.weekStartDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

enum class DayMark { TRAINED, TODAY_EMPTY, EMPTY }

data class DaySlot(val day: DayOfWeek, val date: LocalDate, val mark: DayMark, val sessions: Int)

/** The weekly goal: [done] workouts of [goal] (the ring) and the week's [days] (the day row). */
data class WeekProgress(val done: Int, val goal: Int, val cardio: Int, val days: List<DaySlot>) {
    val progress: Float get() = if (goal <= 0) 0f else (done.toFloat() / goal).coerceIn(0f, 1f)
    val met: Boolean get() = goal > 0 && done >= goal
    val remaining: Int get() = (goal - done).coerceAtLeast(0)
}

fun weekProgress(
    finishedSessionTimestamps: List<Long>,
    goal: Int,
    cardio: Int,
    today: LocalDate,
    firstDay: DayOfWeek,
    zone: TimeZone
): WeekProgress {
    val start = weekStartDate(today, firstDay)
    val startMillis = start.atStartOfDayIn(zone).toEpochMilliseconds()
    val perDay = finishedSessionTimestamps
        .filter { it >= startMillis }
        .groupingBy { Instant.fromEpochMilliseconds(it).toLocalDateTime(zone).date }
        .eachCount()
    val days = (0 until 7).map { offset ->
        val date = start.plus(offset, DateTimeUnit.DAY)
        val count = perDay[date] ?: 0
        val mark = when {
            count > 0 -> DayMark.TRAINED
            date == today -> DayMark.TODAY_EMPTY
            else -> DayMark.EMPTY
        }
        DaySlot(date.dayOfWeek, date, mark, count)
    }
    return WeekProgress(done = perDay.values.sum(), goal = goal, cardio = cardio, days = days)
}
