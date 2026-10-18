package com.emanuel5014.trainable.data.local.entity

import java.util.Locale

/**
 * One cardio exercise of a session: its sets are the cardio rows with the same activity and the same order (rows
 * written before cardio had sets are each a one-set exercise).
 */
data class CardioExerciseLogs(
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

/** Groups cardio rows into exercises, in the order they were done. */
fun List<CardioLogEntity>.groupedByExercise(): List<CardioExerciseLogs> =
    groupBy { it.categoria.trim().lowercase(Locale.ROOT) to it.ordineEsercizio }
        .values
        .map { rows ->
            val sorted = rows.sortedWith(compareBy({ it.numeroSerie }, { it.id }))
            CardioExerciseLogs(sorted.first().categoria, sorted.first().ordineEsercizio, sorted)
        }
        .sortedWith(compareBy({ it.order }, { it.first.id }))

/** The rows of the same cardio exercise as [log], in set order. */
fun List<CardioLogEntity>.setsOf(log: CardioLogEntity): List<CardioLogEntity> =
    filter { it.ordineEsercizio == log.ordineEsercizio && it.categoria.equals(log.categoria, ignoreCase = true) }
        .sortedWith(compareBy({ it.numeroSerie }, { it.id }))

/** A new set of the same cardio exercise as this one: it starts from the same time and distance, not from its note or RPE. */
fun CardioLogEntity.nextSet(number: Int): CardioLogEntity =
    copy(id = 0, numeroSerie = number, note = null, rpe = null, isCompleted = true)
