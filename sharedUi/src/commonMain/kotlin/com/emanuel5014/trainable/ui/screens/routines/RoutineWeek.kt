package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.data.local.relation.PlanWithDetails

/** Weekday indexes (0 = Monday … 6 = Sunday) of a routine's `giorni_settimana` (ISO numbers 1..7, comma separated). */
fun scheduledDayIndexes(giorniSettimana: String?): Set<Int> =
    giorniSettimana.orEmpty().split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..7 }
        .map { it - 1 }
        .toSet()

/** The routines of each weekday (index 0 = Monday … 6 = Sunday), each list in the order of [plans]. */
fun routineWeek(plans: List<PlanWithDetails>): List<List<PlanWithDetails>> {
    val days = plans.map { scheduledDayIndexes(it.plan.giorniSettimana) }
    return (0..6).map { day -> plans.filterIndexed { index, _ -> day in days[index] } }
}

/** True when at least one routine has a valid weekday: only then the week card is shown. */
fun hasAssignedDays(plans: List<PlanWithDetails>): Boolean =
    plans.any { scheduledDayIndexes(it.plan.giorniSettimana).isNotEmpty() }

/** Corner radii (dp) of a routine row: round at the ends of the group, tight where two rows touch. */
data class RowCorners(val top: Float, val bottom: Float) {
    /** The corners of something inset by [by] dp inside the row, so both curves stay concentric. */
    fun inset(by: Float): RowCorners = RowCorners((top - by).coerceAtLeast(0f), (bottom - by).coerceAtLeast(0f))
}

const val ROUTINE_GROUP_END_RADIUS = 32f
const val ROUTINE_GROUP_JOIN_RADIUS = 12f

/**
 * Rows of the list are joined into one group (Material 3 Expressive list): only the ends are round. With [hasHead]
 * the week card is the top of the group, so the first row joins it.
 */
fun routineRowCorners(index: Int, count: Int, hasHead: Boolean = false): RowCorners = RowCorners(
    top = if (index == 0 && !hasHead) ROUTINE_GROUP_END_RADIUS else ROUTINE_GROUP_JOIN_RADIUS,
    bottom = if (index == count - 1) ROUTINE_GROUP_END_RADIUS else ROUTINE_GROUP_JOIN_RADIUS
)

/** The week card as the head of the group: round on top, joined to the first routine below. */
fun routineWeekCorners(): RowCorners = RowCorners(top = ROUTINE_GROUP_END_RADIUS, bottom = ROUTINE_GROUP_JOIN_RADIUS)

/** Which theme container colours a routine's cover uses: 0 = primary, 1 = secondary, 2 = tertiary, by position. */
fun routineCoverRole(index: Int): Int = index.mod(3)
