package com.example.gymtracking.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plan_id"), Index("exercise_id")]
)
data class PlanExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "plan_id")
    val planId: Int,
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Int,
    @ColumnInfo(name = "serie_target")
    val serieTarget: Int,
    @ColumnInfo(name = "reps_target")
    val repsTarget: String,
    @ColumnInfo(name = "recupero_target")
    val recuperoTarget: Int,
    @ColumnInfo(name = "ordine")
    val ordine: Int
)
