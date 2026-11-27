package com.emanuel5014.trainable.ui.screens.workout

/** How far along each exercise of the workout is, for the progress strip under the top bar. */
object ExerciseProgress {
    /** Share of [total] sets that are [completed]: 0 when there are none, never above 1. */
    fun fraction(completed: Int, total: Int): Float =
        if (total <= 0) 0f else (completed.toFloat() / total).coerceIn(0f, 1f)

    /** One fraction per exercise, in order: its sets done over its sets (a cardio exercise counts its cardio sets). */
    fun fractions(exercises: List<WorkoutExerciseState>): List<Float> = exercises.map { exercise ->
        if (exercise.isCardio) {
            fraction(exercise.cardioSets.count { it.isCompleted }, exercise.cardioSets.size)
        } else {
            fraction(exercise.sets.count { it.isCompleted }, exercise.sets.size)
        }
    }

    /**
     * Where a tap at [x] lands among segments that start at [starts] and end at [ends] (same order, same unit): the
     * segment under it, or the nearest one when it falls in a gap or past an end.
     */
    fun segmentAt(x: Float, starts: FloatArray, ends: FloatArray): Int {
        if (starts.isEmpty()) return -1
        var best = 0
        var bestDistance = Float.MAX_VALUE
        for (i in starts.indices) {
            val distance = when {
                x < starts[i] -> starts[i] - x
                x > ends[i] -> x - ends[i]
                else -> return i
            }
            if (distance < bestDistance) {
                bestDistance = distance
                best = i
            }
        }
        return best
    }
}
