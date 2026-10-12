package com.emanuel5014.trainable.data.local.entity

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
    val rpe: Float? = null,
    @ColumnInfo(name = "is_warmup")
    val isWarmup: Boolean = false,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "ordine_esercizio")
    val ordineEsercizio: Int = 0,
    @ColumnInfo(name = "superset_id")
    val supersetId: String? = null,
    @ColumnInfo(name = "rest_timer_seconds")
    val restTimerSeconds: Int? = null,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = true,
    @ColumnInfo(name = "durata_secondi")
    val durataSecondi: Int? = null,
    // Snapshot of the advanced prescription this set was performed against
    @ColumnInfo(name = "target_percent")
    val targetPercent: Float? = null,
    @ColumnInfo(name = "target_rpe")
    val targetRpe: Float? = null,
    /** Planned reps ("5", or "MAX" for AMRAP). */
    @ColumnInfo(name = "target_reps")
    val targetReps: String? = null,
    @ColumnInfo(name = "rep_mode")
    val repMode: String? = null,
    @ColumnInfo(name = "block_index")
    val blockIndex: Int? = null,
    @ColumnInfo(name = "techniques")
    val techniques: String? = null,
    /** Extra set added on top of the plan (e.g. top singles). */
    @ColumnInfo(name = "is_extra")
    val isExtra: Boolean = false,
    /** Total-reps target of the block (ALSAP), to rebuild the block progress on resume. */
    @ColumnInfo(name = "target_total_reps")
    val targetTotalReps: Int? = null,
    /**
     * [TIME_AND_REPS] for a set of a Time & Reps exercise, null for every other set: a Time & Weight set is told by its
     * duration alone and a plain one by having none (see [SetLogKind]).
     */
    @ColumnInfo(name = "exercise_type")
    val exerciseType: String? = null,
    /**
     * The parts of a set finished after short pauses (rest-pause / cluster), "2+1+1"; null for a set done in one go.
     * [repsEffettive] is their sum (see [SetPerformance]).
     */
    @ColumnInfo(name = "rep_parts")
    val repParts: String? = null,
    /** Seconds of each pause between the parts, "60,50". */
    @ColumnInfo(name = "intra_rests")
    val intraRests: String? = null,
    /** Reps done with a spotter's help, on top of [repsEffettive]. */
    @ColumnInfo(name = "assisted_reps")
    val assistedReps: Int = 0,
    /** Partial reps after the full ones, on top of [repsEffettive]. */
    @ColumnInfo(name = "partial_reps")
    val partialReps: Int = 0,
    /** The techniques the set was really done with when they differ from the planned ones ([techniques]); null as planned. */
    @ColumnInfo(name = "performed_techniques")
    val performedTechniques: String? = null
)
