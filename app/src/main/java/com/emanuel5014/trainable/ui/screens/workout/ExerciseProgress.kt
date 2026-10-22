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
}
