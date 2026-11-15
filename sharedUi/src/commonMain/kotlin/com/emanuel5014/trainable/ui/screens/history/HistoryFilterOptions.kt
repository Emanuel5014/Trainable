package com.emanuel5014.trainable.ui.screens.history

import com.emanuel5014.trainable.data.local.entity.RoutineGroupEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.isArchived
import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * How many workouts each choice of the filter sheet would show. Every part counts with the other filters on and its
 * own left out: the routines count within the period picked, the months within the routine picked, so a number is
 * what the list shows after tapping it.
 */
data class HistoryFilterCounts(
    /** Every workout in the history. */
    val total: Int = 0,
    /** The workouts the filters keep. */
    val shown: Int = 0,
    /** Plan id → workouts, whatever routine is picked. */
    val byPlan: Map<Int, Int> = emptyMap(),
    /** Workouts of no routine (cardio, quick, custom, deleted routines), whatever routine is picked. */
    val noRoutine: Int = 0,
    /** (year, month from 0) → workouts, whatever period is picked. */
    val byMonth: Map<Pair<Int, Int>, Int> = emptyMap(),
    /** Plan id → first and last workout of the routine, over the whole history. */
    val periodByPlan: Map<Int, Pair<Long, Long>> = emptyMap(),
    /** The year of the first workout, null with no workout. */
    val firstYear: Int? = null
)

fun historyFilterCounts(
    sessions: List<SessionWithDetails>,
    filters: HistoryFilters,
    journalEnabled: Boolean,
    zone: TimeZone = TimeZone.currentSystemDefault()
): HistoryFilterCounts {
    val anyRoutine = filters.anyRoutine()
    val anyDate = filters.anyDate()
    val byPlan = mutableMapOf<Int, Int>()
    val byMonth = mutableMapOf<Pair<Int, Int>, Int>()
    val periods = mutableMapOf<Int, Pair<Long, Long>>()
    var shown = 0
    var noRoutine = 0
    var firstYear: Int? = null
    for (details in sessions) {
        val session = details.session
        val date = Instant.fromEpochMilliseconds(session.timestamp).toLocalDateTime(zone).date
        firstYear = minOf(firstYear ?: date.year, date.year)
        if (!details.plan.isSystemPlan) {
            val (first, last) = periods[session.planId] ?: (session.timestamp to session.timestamp)
            periods[session.planId] = minOf(first, session.timestamp) to maxOf(last, session.timestamp)
        }
        if (filters.matches(details, journalEnabled)) shown++
        if (anyRoutine.matches(details, journalEnabled)) {
            if (details.plan.isSystemPlan) noRoutine++
            else byPlan[session.planId] = (byPlan[session.planId] ?: 0) + 1
        }
        if (anyDate.matches(details, journalEnabled)) {
            val key = date.year to date.month.ordinal
            byMonth[key] = (byMonth[key] ?: 0) + 1
        }
    }
    return HistoryFilterCounts(sessions.size, shown, byPlan, noRoutine, byMonth, periods, firstYear)
}

/** What a block of the routine picker holds. */
enum class RoutineBlockKind {
    /** A group the lifter made. */
    Group,

    /** With no group at all: the active routines. */
    Active,

    /** With no group at all: the archived routines. */
    Archived,

    /** With groups: the routines in none of them. */
    Loose
}

data class RoutinePickerItem(
    val plan: WorkoutPlanEntity,
    val workouts: Int,
    /** First and last workout of the routine, null when it has none. */
    val period: Pair<Long, Long>?,
    /** Another routine of the same block has this name: its period tells them apart. */
    val showPeriod: Boolean
)

data class RoutinePickerBlock(
    val kind: RoutineBlockKind,
    /** The group, for [RoutineBlockKind.Group] only. */
    val group: RoutineGroupEntity?,
    val routines: List<RoutinePickerItem>,
    /** First workout to last of all its routines, null when none was trained. */
    val period: Pair<Long, Long>?,
    /** Every routine of the block is archived. */
    val archived: Boolean
) {
    val workouts: Int get() = routines.sumOf { it.workouts }
    val key: String get() = group?.let { "group-${it.id}" } ?: kind.name

    /** Opens by default: it has a routine still in use. */
    val openByDefault: Boolean get() = !archived

    fun contains(planId: Int?): Boolean = planId != null && routines.any { it.plan.id == planId }
}

/**
 * The routines of the picker in blocks: the groups in their order and then the routines in no group, or, when the
 * lifter made no group, the active routines and then the archived ones. Active routines come first in every block.
 * Routines of the system plans (cardio, quick, custom) are never listed: they are "no routine".
 */
fun routinePickerBlocks(
    plans: List<WorkoutPlanEntity>,
    groups: List<RoutineGroupEntity>,
    counts: HistoryFilterCounts
): List<RoutinePickerBlock> {
    val routines = plans.filter { !it.isSystemPlan }
    val known = groups.map { it.id }.toSet()
    val byGroup = routines.groupBy { plan -> plan.groupId?.takeIf { it in known } }

    fun block(kind: RoutineBlockKind, group: RoutineGroupEntity?, members: List<WorkoutPlanEntity>): RoutinePickerBlock? {
        if (members.isEmpty()) return null
        val ordered = members.sortedBy { it.isArchived }
        val sameName = ordered.groupingBy { it.nome.trim().lowercase() }.eachCount()
        val items = ordered.map { plan ->
            RoutinePickerItem(
                plan = plan,
                workouts = counts.byPlan[plan.id] ?: 0,
                period = counts.periodByPlan[plan.id],
                showPeriod = (sameName[plan.nome.trim().lowercase()] ?: 0) > 1
            )
        }
        val periods = items.mapNotNull { it.period }
        val period = if (periods.isEmpty()) null else periods.minOf { it.first } to periods.maxOf { it.second }
        return RoutinePickerBlock(kind, group, items, period, archived = ordered.all { it.isArchived })
    }

    val grouped = groups
        .sortedWith(compareBy({ it.ordine }, { it.id }))
        .mapNotNull { group -> block(RoutineBlockKind.Group, group, byGroup[group.id].orEmpty()) }
    val loose = byGroup[null].orEmpty()
    return if (grouped.isEmpty()) {
        listOfNotNull(
            block(RoutineBlockKind.Active, null, loose.filter { !it.isArchived }),
            block(RoutineBlockKind.Archived, null, loose.filter { it.isArchived })
        )
    } else {
        grouped + listOfNotNull(block(RoutineBlockKind.Loose, null, loose))
    }
}

/** A whole year, from its first millisecond to its last, in [zone]. */
fun yearRange(year: Int, zone: TimeZone = TimeZone.currentSystemDefault()): Pair<Long, Long> =
    LocalDate(year, 1, 1).atStartOfDayIn(zone).toEpochMilliseconds() to
        LocalDate(year + 1, 1, 1).atStartOfDayIn(zone).toEpochMilliseconds() - 1

/** The last [days] days up to the end of [today], in [zone]. */
fun lastDaysRange(days: Int, today: LocalDate, zone: TimeZone = TimeZone.currentSystemDefault()): Pair<Long, Long> =
    today.minus(days - 1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds() to
        today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds() - 1
