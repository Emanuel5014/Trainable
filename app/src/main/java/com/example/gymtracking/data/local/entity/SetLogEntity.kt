package com.example.gymtracking.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id"), Index("exercise_id")]
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Int,
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Int,
    @ColumnInfo(name = "peso_sollevato")
    val pesoSollevato: Float,
    @ColumnInfo(name = "reps_effettive")
    val repsEffettive: Int,
    @ColumnInfo(name = "numero_serie")
    val numeroSerie: Int,
    @ColumnInfo(name = "rpe")
    val rpe: Int? = null,
    @ColumnInfo(name = "is_warmup")
    val isWarmup: Boolean = false,
    @ColumnInfo(name = "note")
    val note: String? = null
)
