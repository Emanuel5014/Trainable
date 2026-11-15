package com.emanuel5014.trainable.ui.screens.history

import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.domain.journal.JournalFilters
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * What the history is narrowed to: a routine or a whole group of routines, a date range and, with the workout journal
 * on, tags, rating and notes. [startDate] and [endDate] are both inclusive, in local time: the first and the last
 * millisecond that count.
 */
data class HistoryFilters(
    /** A routine, [NO_ROUTINE_PLAN_ID] for the workouts of no routine, or null for all. */
    val planId: Int? = null,
    /** Every routine of this group (`routine_groups.id`); never set together with [planId]. */
    val groupId: Int? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val journal: JournalFilters = JournalFilters()
) {
    /** Whether anything narrows the list; the journal filters count only while the journal is on. */
    fun isActive(journalEnabled: Boolean): Boolean =
        planId != null || groupId != null || startDate != null || endDate != null || (journalEnabled && journal.isActive)

    /** The same filters on any routine. */
    fun anyRoutine(): HistoryFilters = copy(planId = null, groupId = null)

    /** The same filters on any date. */
    fun anyDate(): HistoryFilters = copy(startDate = null, endDate = null)

    fun apply(sessions: List<SessionWithDetails>, journalEnabled: Boolean): List<SessionWithDetails> =
        sessions.filter { matches(it, journalEnabled) }

    fun matches(details: SessionWithDetails, journalEnabled: Boolean): Boolean {
        val session = details.session
        val matchesPlan = when {
            groupId != null -> !details.plan.isSystemPlan && details.plan.groupId == groupId
            planId == null -> true
            planId == NO_ROUTINE_PLAN_ID -> details.plan.isSystemPlan
            else -> session.planId == planId
        }
        // The end already is the last millisecond of its day: a whole day more let the 1st of the next month into a month (#69)
        val matchesDate = (startDate == null || session.timestamp >= startDate) &&
            (endDate == null || session.timestamp <= endDate)
        val matchesJournal = !journalEnabled ||
            journal.matches(session.rating, session.journalNote, details.tags.map { it.id }.toSet())
        return matchesPlan && matchesDate && matchesJournal
    }
}

/**
 * The days picked in the date range picker, which gives each one as UTC midnight, as a range of the history filter:
 * from the first millisecond of the first day to the last of the last one, in the phone's time zone.
 */
fun pickedDaysRange(startUtcDay: Long, endUtcDay: Long, zone: TimeZone = TimeZone.currentSystemDefault()): Pair<Long, Long> {
    fun day(utcMillis: Long) = Instant.fromEpochMilliseconds(utcMillis).toLocalDateTime(TimeZone.UTC).date
    val start = day(startUtcDay).atStartOfDayIn(zone).toEpochMilliseconds()
    val end = day(endUtcDay).plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds() - 1
    return start to end
}

/**
 * Value of [HistoryFilters.planId] that keeps the workouts without a routine of their own: cardio,
 * custom and quick workouts, and those of routines that were deleted. Plan ids start at 1, so it never
 * collides with a real one.
 */
const val NO_ROUTINE_PLAN_ID = -1
