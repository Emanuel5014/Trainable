package com.emanuel5014.trainable.ui.screens.history

import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.relation.SetWithExercise

/**
 * One exercise of a workout as the edit screen sees it. The same exercise done twice in a workout (warm-ups first, heavy
 * sets at the end) is two of these, told apart by [order]: the rank it was done at, shared by all the rows of that run.
 */
data class ExerciseKey(val exerciseId: Int, val order: Int)

val SetLogEntity.exerciseKey: ExerciseKey get() = ExerciseKey(exerciseId, ordineEsercizio)

val EditExerciseState.key: ExerciseKey get() = ExerciseKey(exercise.id, sets.firstOrNull()?.ordineEsercizio ?: 0)

/** The exercises of a workout in the order they were done: one per exercise and order, each with its sets in set order. */
fun List<SetWithExercise>.toEditExercises(): List<EditExerciseState> {
    val seen = HashMap<Int, Int>()
    return groupBy { it.exercise.id to it.setLog.ordineEsercizio }
        .values
        .map { rows -> EditExerciseState(exercise = rows.first().exercise, sets = rows.map { it.setLog }.sortedBy { it.numeroSerie }) }
        .sortedBy { it.sets.firstOrNull()?.ordineEsercizio ?: 0 }
        .map { state ->
            val instance = seen[state.exercise.id] ?: 0
            seen[state.exercise.id] = instance + 1
            state.copy(instance = instance)
        }
}
