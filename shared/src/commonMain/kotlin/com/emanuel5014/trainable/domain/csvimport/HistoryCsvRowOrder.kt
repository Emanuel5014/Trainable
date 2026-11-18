package com.emanuel5014.trainable.domain.csvimport

import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.groupedByExercise
import com.emanuel5014.trainable.data.local.relation.SetWithExercise

/** A row of a workout in the history CSV: a set of an exercise, or a cardio set. */
sealed interface HistoryCsvRow {
    data class Strength(val set: SetWithExercise) : HistoryCsvRow
    data class Cardio(val log: CardioLogEntity) : HistoryCsvRow
}

/**
 * The rows of a workout in the order the history shows it: exercise by exercise in their place in the workout, each
 * with its sets in order, and cardio in its own place among them.
 *
 * The database keeps rows in the order they were saved, which is not that order for a workout tracked in the app: the
 * sets of a superset are saved alternating, and an exercise added or moved during the workout has newer rows. Writing
 * them as saved put supersets in the wrong place in the file, and reading the file back rebuilt the workout that way.
 */
object HistoryCsvRowOrder {
    fun of(sets: List<SetWithExercise>, cardio: List<CardioLogEntity>): List<HistoryCsvRow> {
        // As the history groups them: by place, then set; one exercise run is its exercise at its place
        val runs = LinkedHashMap<Pair<Int, Int>, MutableList<SetWithExercise>>()
        sets.sortedWith(compareBy({ it.setLog.ordineEsercizio }, { it.setLog.numeroSerie }))
            .forEach { runs.getOrPut(it.setLog.exerciseId to it.setLog.ordineEsercizio) { mutableListOf() }.add(it) }
        val items = runs.values.map { run -> run.first().setLog.ordineEsercizio to run.map { HistoryCsvRow.Strength(it) } } +
            cardio.groupedByExercise().map { exercise -> exercise.order to exercise.sets.map { HistoryCsvRow.Cardio(it) } }
        // Stable: at the same place the exercise stays before the cardio, as in the history
        return items.sortedBy { it.first }.flatMap { it.second }
    }
}
