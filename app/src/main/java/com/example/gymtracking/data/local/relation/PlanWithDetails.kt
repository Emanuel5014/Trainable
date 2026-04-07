package com.example.gymtracking.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymtracking.data.local.entity.ExerciseEntity
import com.example.gymtracking.data.local.entity.PlanExerciseEntity
import com.example.gymtracking.data.local.entity.WorkoutPlanEntity

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
    val exercises: List<PlanExerciseWithDetails>
)
