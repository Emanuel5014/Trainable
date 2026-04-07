package com.example.gymtracking.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymtracking.data.local.entity.ExerciseEntity
import com.example.gymtracking.data.local.entity.SetLogEntity
import com.example.gymtracking.data.local.entity.WorkoutPlanEntity
import com.example.gymtracking.data.local.entity.WorkoutSessionEntity

data class SetWithExercise(
    @Embedded val setLog: SetLogEntity,
    @Relation(
        parentColumn = "exercise_id",
        entityColumn = "id"
    )
    val exercise: ExerciseEntity
)

data class SessionWithDetails(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "plan_id",
        entityColumn = "id"
    )
    val plan: WorkoutPlanEntity,
    @Relation(
        entity = SetLogEntity::class,
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val sets: List<SetWithExercise>
)
