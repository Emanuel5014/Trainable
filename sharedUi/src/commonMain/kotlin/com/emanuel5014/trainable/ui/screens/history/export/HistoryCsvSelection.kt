package com.emanuel5014.trainable.ui.screens.history.export

import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvRowFilter
import com.emanuel5014.trainable.ui.screens.history.lastDaysRange
import com.emanuel5014.trainable.ui.screens.history.yearRange
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * What a CSV export is made of: the workouts of some routines, groups of routines and/or of no routine (none of them
 * picked means every workout), in a date range, and of those workouts the rows [rows] keeps. [startDate] and [endDate]
 * are inclusive, like the history's filters.
 */
data class HistoryCsvSelection(
    val planIds: Set<Int> = emptySet(),
    val groupIds: Set<Int> = emptySet(),
    /** The workouts without a routine of their own: cardio, quick and custom workouts, those of deleted routines. */
    val noRoutine: Boolean = false,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val rows: HistoryCsvRowFilter = HistoryCsvRowFilter()
) {
    val anyRoutine: Boolean get() = planIds.isEmpty() && groupIds.isEmpty() && !noRoutine

    fun matchesRoutine(details: SessionWithDetails): Boolean {
        if (anyRoutine) return true
        val plan = details.plan
        if (plan.isSystemPlan) return noRoutine
        return details.session.planId in planIds || (plan.groupId != null && plan.groupId in groupIds)
    }

    fun matchesPeriod(details: SessionWithDetails): Boolean {
        val timestamp = details.session.timestamp
        return (startDate == null || timestamp >= startDate) && (endDate == null || timestamp <= endDate)
    }

    /** The workouts of [sessions] this export takes, before [rows] narrows them. */
    fun workouts(sessions: List<SessionWithDetails>): List<SessionWithDetails> =
        sessions.filter { matchesRoutine(it) && matchesPeriod(it) }

    /** What goes into the file: the workouts this export takes, with only the rows it keeps. */
    fun exported(sessions: List<SessionWithDetails>): List<SessionWithDetails> = rows.apply(workouts(sessions))

    fun withPeriod(range: Pair<Long, Long>?): HistoryCsvSelection = copy(startDate = range?.first, endDate = range?.second)
}

/** The period shortcuts of the export sheet; [CUSTOM] is any other range (a month picked in the history too). */
enum class ExportPeriod { ALL, LAST_30_DAYS, LAST_3_MONTHS, THIS_YEAR, LAST_YEAR, CUSTOM }

object ExportPeriods {
    fun range(period: ExportPeriod, today: LocalDate, zone: TimeZone = TimeZone.currentSystemDefault()): Pair<Long, Long>? = when (period) {
        ExportPeriod.ALL, ExportPeriod.CUSTOM -> null
        ExportPeriod.LAST_30_DAYS -> lastDaysRange(30, today, zone)
        ExportPeriod.LAST_3_MONTHS -> lastDaysRange(90, today, zone)
        ExportPeriod.THIS_YEAR -> yearRange(today.year, zone)
        ExportPeriod.LAST_YEAR -> yearRange(today.year - 1, zone)
    }

    /** Which shortcut [selection]'s range is, [ExportPeriod.CUSTOM] when it is none of them. */
    fun of(selection: HistoryCsvSelection, today: LocalDate, zone: TimeZone = TimeZone.currentSystemDefault()): ExportPeriod {
        if (selection.startDate == null && selection.endDate == null) return ExportPeriod.ALL
        val range = selection.startDate to selection.endDate
        return ExportPeriod.entries.firstOrNull { it != ExportPeriod.ALL && it != ExportPeriod.CUSTOM && range(it, today, zone) == range }
            ?: ExportPeriod.CUSTOM
    }
}
