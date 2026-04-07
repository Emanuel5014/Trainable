package com.example.gymtracking.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_exercise_swaps",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlanExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["original_exercise_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["replacement_exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("session_id"),
        Index("original_exercise_id"),
        Index("replacement_exercise_id")
    ]
)
data class SessionExerciseSwapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Int,
    @ColumnInfo(name = "original_exercise_id")
    val originalExerciseId: Int,
    @ColumnInfo(name = "replacement_exercise_id")
    val replacementExerciseId: Int
)
