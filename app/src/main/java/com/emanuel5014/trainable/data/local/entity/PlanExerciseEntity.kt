package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emanuel5014.trainable.domain.prescription.RepRange

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
    val ordine: Int,
    @ColumnInfo(name = "superset_id")
    val supersetId: String? = null,
    @ColumnInfo(name = "exercise_type")
    val exerciseType: String = "strength",
    @ColumnInfo(name = "durata_target_secondi")
    val durataTargetSecondi: Int? = null,
    @ColumnInfo(name = "distanza_target_km")
    val distanzaTargetKm: Float? = null,
    @ColumnInfo(name = "cardio_categoria")
    val cardioCategoria: String? = null,
    /** Weeks in which this exercise is skipped, CSV ("4,8"). */
    @ColumnInfo(name = "excluded_weeks")
    val excludedWeeks: String? = null,
    /** Top of a rep range ("10–15"), in which case [repsTarget] holds its bottom. Null for fixed reps and per-set lists. */
    @ColumnInfo(name = "reps_max")
    val repsMax: Int? = null,
    /** Weight in kg the sets start from until the routine has a workout to copy it from (only with starting weights on). */
    @ColumnInfo(name = "weight_preset")
    val weightPreset: Float? = null
) {
    /** The rep range of the exercise, null for fixed reps and per-set lists. */
    val repsRange: RepRange? get() = RepRange.of(repsTarget.trim().toIntOrNull(), repsMax)

    /** Reps as shown to the user: "10–15" for a range, otherwise exactly what was typed ("8", "8-6-4-2"). */
    val repsDisplay: String get() = repsRange?.text ?: repsTarget
}
