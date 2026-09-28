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
