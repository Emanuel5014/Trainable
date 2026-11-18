package com.emanuel5014.trainable.domain.prescription

import com.emanuel5014.trainable.util.WeightUnitConverter

/**
 * Double progression for an exercise with a [RepRange]: keep the weight until every set reaches the top of the
 * range, then add one increment and start again from the bottom of the range.
 */
object DoubleProgression {

    /** A completed working set of the previous workout. */
    data class DoneSet(val setNumber: Int, val weightKg: Float, val reps: Int)

    /** Every set of the previous workout reached the top of the range, so the load can go up. */
    data class Suggestion(
        /** The new weight in kg for each set number of the previous workout. */
        val weightsKg: Map<Int, Float>,
        /** The reps the range restarts from at the heavier weight. */
        val restartReps: Int,
        /** The top of the range that was reached. */
        val reachedReps: Int
    ) {
        val topWeightKg: Float get() = weightsKg.values.max()
    }

    /**
     * @param done the working sets of the last workout of this routine
     * @param plannedSets sets the exercise asks for; a workout that stopped short earns no suggestion
     * @param unit the user's weight unit, the increment is in that unit
     * @param increment how much to add, in [unit] (the plate increment the user picked)
     */
    fun suggest(done: List<DoneSet>, range: RepRange, plannedSets: Int, unit: String, increment: Float): Suggestion? {
        if (increment <= 0f || done.isEmpty() || done.size < plannedSets.coerceAtLeast(1)) return null
        if (done.any { it.weightKg <= 0f || it.reps < range.max }) return null
        val weights = done.associate { set ->
            val display = WeightUnitConverter.convertDisplay(set.weightKg, unit) + increment
            set.setNumber to WeightUnitConverter.convertStorage(LoadCalculator.roundTo(display, DISPLAY_STEP), unit)
        }
        return Suggestion(weights, restartReps = range.min, reachedReps = range.max)
    }

    /** Finest step a suggested weight is rounded to, so 174.99998 lb + 5 reads 180 and not 179.99998. */
    private const val DISPLAY_STEP = 0.25f
}
