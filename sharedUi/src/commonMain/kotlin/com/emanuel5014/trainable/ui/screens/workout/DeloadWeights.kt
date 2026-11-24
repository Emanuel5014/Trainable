package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.domain.session.DeloadRules
import kotlin.math.abs

/**
 * The weights of a running workout when it is marked as a deload, or back to a normal one. A set still at what the
 * last normal workout did ([WorkoutSetState.previousWeight]) goes to the deload share of it, and a set at that share
 * goes back; a weight typed by hand, a completed set, a warm-up, cardio and %1RM exercises (their prescription says
 * the load) are left alone.
 */
object DeloadWeights {
    data class Result(val exercises: List<WorkoutExerciseState>, val changedIndices: List<Int>)

    fun apply(
        exercises: List<WorkoutExerciseState>,
        deload: Boolean,
        percent: Int,
        unit: String,
        increment: Float,
        onlyIndex: Int? = null
    ): Result {
        val changed = mutableListOf<Int>()
        val updated = exercises.mapIndexed { index, exercise ->
            if (onlyIndex != null && index != onlyIndex) return@mapIndexed exercise
            if (exercise.isCardio || exercise.isAdvanced) return@mapIndexed exercise
            var any = false
            val sets = exercise.sets.map { set ->
                val next = rescaled(set, deload, percent, unit, increment)
                if (next !== set) any = true
                next
            }
            if (any) {
                changed += index
                exercise.copy(sets = sets)
            } else exercise
        }
        return Result(updated, changed)
    }

    private fun rescaled(set: WorkoutSetState, deload: Boolean, percent: Int, unit: String, increment: Float): WorkoutSetState {
        val previous = set.previousWeight ?: return set
        if (set.isCompleted || set.isWarmup || set.isTimeAndReps || previous <= 0f) return set
        val scaled = DeloadRules.scaledWeightKg(previous, percent, unit, increment)
        if (same(scaled, previous)) return set
        return when {
            deload && same(set.weight, previous) -> set.copy(weight = scaled)
            !deload && same(set.weight, scaled) -> set.copy(weight = previous)
            else -> set
        }
    }

    private fun same(a: Float, b: Float): Boolean = abs(a - b) < 0.001f
}
