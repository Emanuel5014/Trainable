package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.data.local.entity.RoutineGroupEntity
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * One block of the routines list: the routines of [group] in their order, or the routines in no group when [group]
 * is null.
 */
data class RoutineSection(val group: RoutineGroupEntity?, val plans: List<PlanWithDetails>)

/**
 * The routines of one tab split by group: the groups in their order (a group with no routine in this tab is left
 * out), then the routines in no group. A routine pointing to a group that is gone counts as in no group. With no
 * group at all the result is one section with every routine, so the list looks as it did before groups.
 */
fun routineSections(plans: List<PlanWithDetails>, groups: List<RoutineGroupEntity>): List<RoutineSection> {
    val known = groups.map { it.id }.toSet()
    val byGroup = plans.groupBy { plan -> plan.plan.groupId?.takeIf { it in known } }
    val sections = groups
        .sortedWith(compareBy({ it.ordine }, { it.id }))
        .mapNotNull { group -> byGroup[group.id]?.let { RoutineSection(group, it) } }
    val loose = byGroup[null].orEmpty()
    return when {
        sections.isEmpty() -> listOf(RoutineSection(null, plans))
        loose.isEmpty() -> sections
        else -> sections + RoutineSection(null, loose)
    }
}

/** True when the list is split into groups (at least one section belongs to a group). */
fun List<RoutineSection>.isGrouped(): Boolean = any { it.group != null }

/** The theme colour a new group gets: the next one after the groups that exist, so two new groups differ. */
fun nextGroupColorRole(groups: List<RoutineGroupEntity>): Int = groups.size.mod(RoutineGroupColor.THEME_ROLES)

/** The theme colours a group can take; a colour picked freely is stored apart (`custom_color`). */
object RoutineGroupColor {
    const val PRIMARY = 0
    const val SECONDARY = 1
    const val TERTIARY = 2
    const val THEME_ROLES = 3
}

/**
 * From when to when the routines of a group were used: the earliest start date of its routines, to the last workout
 * of any of them ([lastSessions]: plan id → time), or to that start when none was trained after it.
 */
fun routineGroupPeriod(plans: List<PlanWithDetails>, lastSessions: Map<Int, Long>): Pair<Long, Long>? {
    if (plans.isEmpty()) return null
    val start = plans.minOf { it.plan.dataInizio }
    val end = plans.mapNotNull { lastSessions[it.plan.id] }.maxOrNull()?.coerceAtLeast(start) ?: start
    return start to end
}

/**
 * A period as months: "Nov 2026", "Apr – Jun 2026" or "Nov 2025 – Mar 2026". [monthNames] are the twelve short
 * month names in the app's language.
 */
fun formatMonthRange(
    startMillis: Long,
    endMillis: Long,
    monthNames: List<String>,
    zone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val start = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(zone)
    val end = Instant.fromEpochMilliseconds(endMillis).toLocalDateTime(zone)
    val startMonth = start.month.ordinal
    val endMonth = end.month.ordinal
    fun name(monthIndex: Int) = monthNames.getOrElse(monthIndex) { (monthIndex + 1).toString() }
    return when {
        start.year == end.year && startMonth == endMonth -> "${name(startMonth)} ${start.year}"
        start.year == end.year -> "${name(startMonth)} – ${name(endMonth)} ${end.year}"
        else -> "${name(startMonth)} ${start.year} – ${name(endMonth)} ${end.year}"
    }
}

/** "Nov 2026": the month of [millis]. */
fun formatMonthYear(millis: Long, monthNames: List<String>, zone: TimeZone = TimeZone.currentSystemDefault()): String =
    formatMonthRange(millis, millis, monthNames, zone)

/**
 * [ids] (the order of every routine) with [first] and [second] swapping places: how a routine moves up or down
 * next to its neighbour in a group without disturbing the other routines.
 */
fun <T> swapInOrder(ids: List<T>, first: T, second: T): List<T> {
    val a = ids.indexOf(first)
    val b = ids.indexOf(second)
    if (a < 0 || b < 0 || a == b) return ids
    return ids.toMutableList().apply {
        this[a] = second
        this[b] = first
    }
}
