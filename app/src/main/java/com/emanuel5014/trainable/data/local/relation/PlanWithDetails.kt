package com.emanuel5014.trainable.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseBlockEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.byWeek
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionResolver
import com.emanuel5014.trainable.domain.prescription.ResolvedPrescription
import com.emanuel5014.trainable.domain.prescription.WeekSetCodec
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity

data class PlanExerciseWithDetails(
    @Embedded val planExercise: PlanExerciseEntity,
    @Relation(
        parentColumn = "exercise_id",
        entityColumn = "id"
    )
    val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "plan_exercise_id"
    )
    val blocks: List<PlanExerciseBlockEntity> = emptyList()
) {
    val blocksByWeek: Map<Int, List<PrescriptionBlock>> get() = blocks.byWeek()

    val excludedWeeks: Set<Int> get() = WeekSetCodec.decode(planExercise.excludedWeeks)

    /** True when the exercise uses %1RM / technique blocks rather than plain sets × reps. */
    val isAdvanced: Boolean get() = blocks.isNotEmpty()

    fun resolve(week: Int): ResolvedPrescription = PrescriptionResolver.resolve(blocksByWeek, excludedWeeks, week)
}

data class PlanWithDetails(
    @Embedded val plan: WorkoutPlanEntity,
    @Relation(
        entity = PlanExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "plan_id"
    )
    val exercises: List<PlanExerciseWithDetails>,
    @Relation(
        parentColumn = "id",
        entityColumn = "plan_id"
    )
    val images: List<WorkoutPlanImageEntity> = emptyList()
)
