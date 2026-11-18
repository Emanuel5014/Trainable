package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.ui.screens.history.HistoryFilters
import com.emanuel5014.trainable.ui.screens.history.MonthCalendar
import com.emanuel5014.trainable.ui.screens.history.pickedDaysRange
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

/** The date filter of the history (issue #69: the 1st of the next month showed up in a month). */
class HistoryDateFilterTest {
    private val zone = TimeZone.currentSystemDefault()

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0, zone: TimeZone = this.zone) =
        LocalDateTime(year, month, day, hour, minute).toInstant(zone).toEpochMilliseconds()

    private fun session(id: Int, timestamp: Long) = SessionWithDetails(
        session = WorkoutSessionEntity(id = id, planId = 1, timestamp = timestamp),
        plan = WorkoutPlanEntity(id = 1, userId = 1, nome = "Plan", dataInizio = 0L),
        sets = emptyList(),
        cardio = emptyList()
    )

    private val sessions = listOf(
        session(1, at(2026, 9, 30, 23, 59)),
        session(2, at(2026, 10, 1, 0, 0)),
        session(3, at(2026, 10, 31, 23, 59)),
        session(4, at(2026, 11, 1, 0, 0)),
        session(5, at(2026, 11, 1, 18, 30))
    )

    private fun HistoryFilters.ids() = apply(sessions, journalEnabled = false).map { it.session.id }

    @Test
    fun aMonthKeepsOnlyItsOwnDays() {
        val (start, end) = MonthCalendar.monthRange(9, 2026)
        assertEquals(listOf(2, 3), HistoryFilters(startDate = start, endDate = end).ids())
    }

    @Test
    fun theNextMonthStartsOnItsFirst() {
        val (start, end) = MonthCalendar.monthRange(10, 2026)
        assertEquals(listOf(4, 5), HistoryFilters(startDate = start, endDate = end).ids())
    }

    @Test
    fun pickedDaysAreWholeLocalDays() {
        val octFirst = LocalDate(2026, 10, 1).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val octLast = LocalDate(2026, 10, 31).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val (start, end) = pickedDaysRange(octFirst, octLast)
        assertEquals(MonthCalendar.monthRange(9, 2026), start to end)
        assertEquals(listOf(2, 3), HistoryFilters(startDate = start, endDate = end).ids())
    }

    @Test
    fun pickedDaysFollowTheGivenZone() {
        val day = LocalDate(2026, 11, 1).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        for (id in listOf("America/Los_Angeles", "Europe/Rome", "Asia/Tokyo")) {
            val other = TimeZone.of(id)
            val (start, end) = pickedDaysRange(day, day, other)
            assertEquals(at(2026, 11, 1, 0, zone = other), start, id)
            assertEquals(at(2026, 11, 2, 0, zone = other) - 1, end, id)
        }
    }
}
