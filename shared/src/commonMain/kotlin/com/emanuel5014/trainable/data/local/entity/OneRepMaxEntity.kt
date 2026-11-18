package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A 1RM (massimale) entry. The current 1RM of an exercise is its most recent entry. */
@Entity(
    tableName = "one_rep_maxes",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exercise_id")]
)
data class OneRepMaxEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Int,
    @ColumnInfo(name = "weight_kg")
    val weightKg: Float,
    @ColumnInfo(name = "date")
    val date: Long,
    @ColumnInfo(name = "source")
    val source: String = SOURCE_MANUAL,
    @ColumnInfo(name = "note")
    val note: String? = null
) {
    companion object {
        const val SOURCE_MANUAL = "manual"
        const val SOURCE_TESTED = "tested"
        const val SOURCE_ESTIMATED = "estimated"
        /** Came with a shared .trainableplan file; the receiver can overwrite it with their own. */
        const val SOURCE_IMPORTED = "imported"
    }
}
