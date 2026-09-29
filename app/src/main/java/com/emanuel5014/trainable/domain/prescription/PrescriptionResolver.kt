package com.emanuel5014.trainable.domain.prescription

sealed interface ResolvedPrescription {
    /** No blocks at all: use `serieTarget` / `repsTarget`. */
    data object Legacy : ResolvedPrescription
    /** The exercise is skipped this week. */
    data object Excluded : ResolvedPrescription
    /**
     * [sourceWeek] differs from [week] when the week isn't written yet (e.g. W5 on a sheet where
     * the coach has only filled W1–W4): the closest previous week is repeated.
     */
    data class Blocks(val week: Int, val sourceWeek: Int, val blocks: List<PrescriptionBlock>) : ResolvedPrescription {
        val isFallback: Boolean get() = week != sourceWeek
    }
}

object PrescriptionResolver {

    fun resolve(blocksByWeek: Map<Int, List<PrescriptionBlock>>, excludedWeeks: Set<Int>, week: Int): ResolvedPrescription {
        if (week in excludedWeeks) return ResolvedPrescription.Excluded
        val defined = blocksByWeek.filterValues { it.isNotEmpty() }
        if (defined.isEmpty()) return ResolvedPrescription.Legacy
        defined[week]?.let { return ResolvedPrescription.Blocks(week, week, it) }
        val source = defined.keys.filter { it < week }.maxOrNull() ?: defined.keys.min()
        return ResolvedPrescription.Blocks(week, source, defined.getValue(source))
    }
}

/** Removing a week from a periodized plan shifts every later week down by one. */
object WeekShift {
    fun <T> removeWeek(byWeek: Map<Int, T>, week: Int): Map<Int, T> =
        byWeek.filterKeys { it != week }.mapKeys { (w, _) -> if (w > week) w - 1 else w }

    fun removeWeek(weeks: Set<Int>, week: Int): Set<Int> =
        weeks.filter { it != week }.map { if (it > week) it - 1 else it }.toSet()

    /** Where the "current week" pointer ends up after [week] is deleted from a plan of [oldCount] weeks. */
    fun currentAfterRemoval(current: Int, week: Int, oldCount: Int): Int {
        val newCount = (oldCount - 1).coerceAtLeast(1)
        val shifted = if (current > week) current - 1 else current
        return shifted.coerceIn(1, newCount)
    }
}
