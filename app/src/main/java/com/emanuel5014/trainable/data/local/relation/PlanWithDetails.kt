package com.emanuel5014.trainable.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity

data class PlanExerciseWithDetails(
    @Embedded val planExercise: PlanExerciseEntity,
    @Relation(
        parentColumn = "exercise_id",
        entityColumn = "id"
    )
    val exercise: ExerciseEntity
)

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
