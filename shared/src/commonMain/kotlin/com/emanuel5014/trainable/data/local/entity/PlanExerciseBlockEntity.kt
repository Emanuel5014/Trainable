package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emanuel5014.trainable.domain.prescription.IntensityType
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.RepMode
import com.emanuel5014.trainable.domain.prescription.TechniqueCodec

/** One block of an advanced prescription (e.g. `75% 3×5 STOP 2"`), for a given week of the plan. */
@Entity(
    tableName = "plan_exercise_blocks",
    foreignKeys = [
        ForeignKey(
            entity = PlanExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plan_exercise_id")]
)
data class PlanExerciseBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "plan_exercise_id")
    val planExerciseId: Int,
    @ColumnInfo(name = "week")
    val week: Int = 1,
    @ColumnInfo(name = "ordine")
    val ordine: Int = 0,
    @ColumnInfo(name = "sets")
    val sets: Int = 1,
    @ColumnInfo(name = "reps")
    val reps: String = "",
    @ColumnInfo(name = "rep_mode")
    val repMode: String = RepMode.FIXED.code,
    @ColumnInfo(name = "total_reps")
    val totalReps: Int? = null,
    @ColumnInfo(name = "intensity_type")
    val intensityType: String = IntensityType.NONE.code,
    @ColumnInfo(name = "intensity_value")
    val intensityValue: Float? = null,
    @ColumnInfo(name = "techniques")
    val techniques: String? = null,
    @ColumnInfo(name = "rest_seconds")
    val restSeconds: Int? = null,
    @ColumnInfo(name = "note")
    val note: String? = null
) {
    fun toDomain() = PrescriptionBlock(
        sets = sets,
        reps = reps,
        repMode = RepMode.fromCode(repMode),
        totalReps = totalReps,
        intensityType = IntensityType.fromCode(intensityType),
        intensityValue = intensityValue,
        techniques = TechniqueCodec.decode(techniques),
        restSeconds = restSeconds,
        note = note
    )

    companion object {
        fun fromDomain(block: PrescriptionBlock, planExerciseId: Int, week: Int, ordine: Int) = PlanExerciseBlockEntity(
            planExerciseId = planExerciseId,
            week = week,
            ordine = ordine,
            sets = block.sets,
            reps = block.reps,
            repMode = block.repMode.code,
            totalReps = block.totalReps,
            intensityType = block.intensityType.code,
            intensityValue = block.intensityValue,
            techniques = TechniqueCodec.encode(block.techniques),
            restSeconds = block.restSeconds,
            note = block.note
        )
    }
}

/** Blocks grouped by week, sorted, ready for [com.emanuel5014.trainable.domain.prescription.PrescriptionResolver]. */
fun List<PlanExerciseBlockEntity>.byWeek(): Map<Int, List<PrescriptionBlock>> =
    groupBy { it.week }.mapValues { (_, blocks) -> blocks.sortedBy { it.ordine }.map { it.toDomain() } }
