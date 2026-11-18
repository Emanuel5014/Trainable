package com.emanuel5014.trainable.domain.csvimport

import com.emanuel5014.trainable.data.local.entity.SetLogEntity

/**
 * What a Superset cell says about an exercise: it is in a superset, and [label] names the group when the cell does
 * (null for a plain yes). Exercises of one workout with the same label are one superset; the exercises with a plain
 * yes that follow one another are one too.
 */
data class SupersetMark(val label: String?)

/**
 * Supersets in the history CSV: one cell after the others, repeated on every row of the exercise. The export writes the
 * number of the group inside the workout (1, 2, ...) and leaves the cell empty for an exercise on its own, so two
 * supersets one after the other stay two. The import ([parse]) also takes what a file edited by hand tends to hold:
 * true / false, yes / no, a letter or a word naming the group.
 */
object SupersetCsv {
    private val yes = setOf("true", "yes", "y", "x", "si", "oui", "ja", "sim")
    private val no = setOf("false", "no", "n", "0", "non", "nein", "nao", "none")
    private val checkMarks = setOf("✓", "✔", "☑", "✅")

    /** The mark of a cell; null when the exercise is not in a superset. */
    fun parse(raw: String): SupersetMark? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val folded = HistoryCsvColumns.fold(text)
        return when {
            // Nothing but punctuation: a tick says yes, a dash says no
            folded.isEmpty() -> if (text in checkMarks) SupersetMark(label = null) else null
            folded in no -> null
            folded in yes -> SupersetMark(label = null)
            else -> SupersetMark(label = folded)
        }
    }

    /**
     * The number of each superset of a workout, from 1 in the order of its exercises (not of the rows: the sets of a
     * superset are logged alternating). A superset with a single exercise in this workout - the other was never done -
     * has no number, since reading the file back could not tell it from an exercise on its own.
     */
    fun numbering(sets: List<SetLogEntity>): Map<String, Int> {
        val entriesOf = sets.filter { it.supersetId != null }
            .groupBy({ it.supersetId!! }, { it.exerciseId to it.ordineEsercizio })
            .mapValues { (_, runs) -> runs.distinct().size }
        return sets.sortedBy { it.ordineEsercizio }
            .mapNotNull { it.supersetId }
            .distinct()
            .filter { entriesOf.getValue(it) >= 2 }
            .mapIndexed { index, id -> id to index + 1 }
            .toMap()
    }

    /** The cell the export writes: the number of the group, empty for an exercise on its own. */
    fun cell(group: Int?): String = group?.toString().orEmpty()
}
