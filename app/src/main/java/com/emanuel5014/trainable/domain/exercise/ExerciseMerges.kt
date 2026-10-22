package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseMergeEntity
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvColumns
import com.emanuel5014.trainable.domain.csvimport.ResolvedExercise

/**
 * Why two exercises can or cannot be merged. [CARDIO_EXERCISE]: the cardio history is linked to an exercise by its name,
 * which a merge does not move. [WINNER_MERGED_AWAY]: the target was itself merged into another exercise, which is where
 * its history is. [LOSER_IN_ACTIVE_WORKOUT] is decided by the database (it needs the workouts in progress).
 */
enum class MergeCheck {
    OK, SAME_EXERCISE, NOT_FOUND, WINNER_HIDDEN, WINNER_MERGED_AWAY, LOSER_ALREADY_MERGED, CARDIO_EXERCISE, LOSER_IN_ACTIVE_WORKOUT
}

/** One merge as an exercise sees it: the exercise on the other side, and whether the merge can be undone now. */
data class ExerciseMergeInfo(
    val mergeId: Int,
    val otherId: Int,
    /** Name as stored (English for presets): translate it for display. */
    val otherName: String,
    val timestamp: Long,
    val canUndo: Boolean
)

/** The merges an exercise took part in: the ones it absorbed ([asWinner]) and the one that absorbed it ([asLoser]). */
data class ExerciseMergeState(
    val asWinner: List<ExerciseMergeInfo> = emptyList(),
    val asLoser: ExerciseMergeInfo? = null
)

/** The rules of merging exercises, on plain data: the database and the screens ask here. */
object ExerciseMerges {

    /** The exercise [id] ended up in, following merges of merges; [id] itself when it was not merged away. */
    fun winnerOf(id: Int, merges: List<ExerciseMergeEntity>): Int {
        var current = id
        val seen = hashSetOf(id)
        while (true) {
            val next = merges.firstOrNull { it.loserId == current }?.winnerId ?: return current
            if (!seen.add(next)) return current
            current = next
        }
    }

    fun check(winner: ExerciseEntity?, loser: ExerciseEntity?, merges: List<ExerciseMergeEntity>): MergeCheck {
        if (winner == null || loser == null) return MergeCheck.NOT_FOUND
        if (winner.id == loser.id) return MergeCheck.SAME_EXERCISE
        if (merges.any { it.loserId == loser.id }) return MergeCheck.LOSER_ALREADY_MERGED
        if (merges.any { it.loserId == winner.id }) return MergeCheck.WINNER_MERGED_AWAY
        if (winner.isDeleted) return MergeCheck.WINNER_HIDDEN
        if (isCardio(winner) || isCardio(loser)) return MergeCheck.CARDIO_EXERCISE
        return MergeCheck.OK
    }

    /** A merge is undone only while its winner is not itself merged away: that later merge goes first. */
    fun canUndo(mergeId: Int, merges: List<ExerciseMergeEntity>): Boolean {
        val merge = merges.firstOrNull { it.id == mergeId } ?: return false
        return merges.none { it.loserId == merge.winnerId }
    }

    /**
     * The exercises to match an imported name against: the merged-away ones are replaced by their final winner under the
     * name they had (when merged, and now if renamed), so a file written before the merge finds the winner. A visible
     * exercise that really has such a name keeps it.
     */
    fun aliasView(rows: List<ExerciseEntity>, merges: List<ExerciseMergeEntity>): List<ExerciseEntity> {
        if (merges.isEmpty()) return rows
        val loserIds = merges.mapTo(hashSetOf()) { it.loserId }
        val real = rows.filter { it.id !in loserIds }
        val byId = rows.associateBy { it.id }
        val taken = real.mapTo(hashSetOf()) { HistoryCsvColumns.fold(it.nome) }
        val aliases = buildList {
            merges.forEach { merge ->
                val winner = byId[winnerOf(merge.winnerId, merges)] ?: return@forEach
                listOfNotNull(merge.loserName, byId[merge.loserId]?.nome).forEach { name ->
                    if (taken.add(HistoryCsvColumns.fold(name))) add(winner.copy(nome = name))
                }
            }
        }
        return real + aliases
    }

    fun stateFor(exerciseId: Int, merges: List<ExerciseMergeEntity>, exercises: List<ExerciseEntity>): ExerciseMergeState {
        val names = exercises.associate { it.id to it.nome }
        return ExerciseMergeState(
            asWinner = merges.filter { it.winnerId == exerciseId }.map {
                ExerciseMergeInfo(it.id, it.loserId, it.loserName, it.timestamp, canUndo(it.id, merges))
            },
            asLoser = merges.firstOrNull { it.loserId == exerciseId }?.let {
                ExerciseMergeInfo(it.id, it.winnerId, names[it.winnerId].orEmpty(), it.timestamp, canUndo(it.id, merges))
            }
        )
    }

    /**
     * The exercise a pinned Analytics chart ([widgetId] like `exercise_<id>_<range>_<n>`) shows now: the exercise it was
     * pinned for, or the one that took its history in a merge. Null when [widgetId] is not an exercise chart.
     */
    fun widgetExerciseId(widgetId: String, merges: List<ExerciseMergeEntity>): Int? {
        if (!widgetId.startsWith("exercise_")) return null
        val pinned = widgetId.split("_").getOrNull(1)?.toIntOrNull() ?: return null
        return winnerOf(pinned, merges)
    }

    /** Analytics keeps a set of selected exercises: the winner joins it when the loser was in it. */
    fun selectionAfterMerge(selected: Set<Int>, loserId: Int, winnerId: Int): Set<Int> =
        if (loserId in selected) selected + winnerId else selected

    fun isCardio(exercise: ExerciseEntity): Boolean = exercise.categoria.equals(ResolvedExercise.CARDIO, ignoreCase = true)
}
