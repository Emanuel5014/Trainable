package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cardio_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id")]
)
data class CardioLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Int,
    @ColumnInfo(name = "categoria")
    val categoria: String,
    @ColumnInfo(name = "distanza")
    val distanza: Float, // in km
    @ColumnInfo(name = "durata_secondi")
    val durataSecondi: Int,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "ordine_esercizio")
    val ordineEsercizio: Int = 0,
    @ColumnInfo(name = "durata_target_secondi")
    val durataTargetSecondi: Int? = null,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = true,
    /** Set of the cardio exercise this row is (1, 2, ...): the rows with the same activity and order are one exercise. */
    @ColumnInfo(name = "numero_serie")
    val numeroSerie: Int = 1,
    @ColumnInfo(name = "note")
    val note: String? = null,
    /** How hard the set felt (RPE), on whichever scale it was asked in; null when it was not asked. */
    @ColumnInfo(name = "rpe")
    val rpe: Float? = null
)
