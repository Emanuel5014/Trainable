package com.emanuel5014.trainable.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity

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
    val sets: List<SetWithExercise>,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val cardio: List<CardioLogEntity> = emptyList()
)
