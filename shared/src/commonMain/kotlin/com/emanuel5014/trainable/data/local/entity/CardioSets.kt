package com.emanuel5014.trainable.data.local.entity

import com.emanuel5014.trainable.domain.csvimport.HistoryCsvColumns

/**
 * One cardio exercise of a session: its sets are the cardio rows of the same exercise and the same order (rows
 * written before cardio had sets are each a one-set exercise). The exercise is its id; only a row not linked yet
 * falls back to the name written on it.
 */
data class CardioExerciseLogs(
    /** The linked exercise, null only for rows not linked yet. */
    val exerciseId: Int?,
    /** The name written on the first set: only a fallback for display when [exerciseId] is null. */
    val categoria: String,
    val order: Int,
    /** In set order. */
    val sets: List<CardioLogEntity>
) {
    val first: CardioLogEntity get() = sets.first()
    val totalSeconds: Int get() = sets.sumOf { it.durataSecondi }
    val totalKm: Float get() = sets.sumOf { it.distanza.toDouble() }.toFloat()
    val isSingleSet: Boolean get() = sets.size == 1
}

/** Which exercise a cardio row is: its id, or its folded name while it has no link. The two kinds never meet. */
sealed interface CardioExerciseKey {
    data class Linked(val exerciseId: Int) : CardioExerciseKey
    data class Unlinked(val foldedName: String) : CardioExerciseKey
}

/** The exercise this row is a set of, for grouping: rows with equal keys (and order) are one exercise. */
fun CardioLogEntity.exerciseKey(): CardioExerciseKey =
    exerciseId?.let { CardioExerciseKey.Linked(it) } ?: CardioExerciseKey.Unlinked(HistoryCsvColumns.fold(categoria))

/** This row is a set of [exercise]: by id when it is linked, by the name written on it only when it is not. */
fun CardioLogEntity.belongsTo(exercise: ExerciseEntity): Boolean =
    exerciseId?.let { it == exercise.id } ?: (HistoryCsvColumns.fold(categoria) == HistoryCsvColumns.fold(exercise.nome))

/** The exercise of this row among [exercises]: by id when it is linked, by the name written on it otherwise. */
fun CardioLogEntity.exerciseIn(exercises: List<ExerciseEntity>): ExerciseEntity? =
    exercises.firstOrNull { belongsTo(it) }

/** Groups cardio rows into exercises, in the order they were done. */
fun List<CardioLogEntity>.groupedByExercise(): List<CardioExerciseLogs> =
    groupBy { it.exerciseKey() to it.ordineEsercizio }
        .values
        .map { rows ->
            val sorted = rows.sortedWith(compareBy({ it.numeroSerie }, { it.id }))
            CardioExerciseLogs(sorted.first().exerciseId, sorted.first().categoria, sorted.first().ordineEsercizio, sorted)
        }
        .sortedWith(compareBy({ it.order }, { it.first.id }))

/** The rows of the same cardio exercise as [log], in set order. */
fun List<CardioLogEntity>.setsOf(log: CardioLogEntity): List<CardioLogEntity> {
    val key = log.exerciseKey()
    return filter { it.ordineEsercizio == log.ordineEsercizio && it.exerciseKey() == key }
        .sortedWith(compareBy({ it.numeroSerie }, { it.id }))
}

/** A new set of the same cardio exercise as this one: it starts from the same time and distance, not from its note or RPE. */
fun CardioLogEntity.nextSet(number: Int): CardioLogEntity =
    copy(id = 0, numeroSerie = number, note = null, rpe = null, isCompleted = true)
