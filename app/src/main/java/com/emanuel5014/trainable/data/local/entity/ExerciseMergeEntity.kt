package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One exercise merged into another: everything of the loser (sets, routine entries, maxes, swaps, program changes) now
 * belongs to the winner and the loser is hidden. The row stays so that the merge can be undone and so that imports
 * recognise the loser's name as the winner.
 */
@Entity(
    tableName = "exercise_merges",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["winner_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["loser_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("winner_id"), Index("loser_id")]
)
data class ExerciseMergeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "winner_id")
    val winnerId: Int,
    @ColumnInfo(name = "loser_id")
    val loserId: Int,
    /** The loser's name when it was merged: a file written before the merge still calls the exercise that. */
    @ColumnInfo(name = "loser_name")
    val loserName: String,
    /** The loser was already hidden before the merge, so undoing it leaves it hidden. */
    @ColumnInfo(name = "loser_was_hidden")
    val loserWasHidden: Boolean,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    /** The loser was a favorite and the winner was not: the merge starred the winner, and undoing it takes the star back. */
    @ColumnInfo(name = "favorite_moved", defaultValue = "0")
    val favoriteMoved: Boolean = false
)

/** A row that a merge moved from the loser to the winner: [source] says which table, [rowId] which row. */
@Entity(
    tableName = "exercise_merge_moves",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseMergeEntity::class,
            parentColumns = ["id"],
            childColumns = ["merge_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("merge_id")]
)
data class ExerciseMergeMoveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "merge_id")
    val mergeId: Int,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "row_id")
    val rowId: Int
)

/** The tables whose rows a merge moves, as stored in [ExerciseMergeMoveEntity.source]. */
object MergeSource {
    const val SET_LOG = "SET_LOG"
    const val PLAN_EXERCISE = "PLAN_EXERCISE"
    const val ONE_REP_MAX = "ONE_REP_MAX"
    const val SWAP_REPLACEMENT = "SWAP_REPLACEMENT"
    const val PRESCRIPTION_EDIT = "PRESCRIPTION_EDIT"
}
