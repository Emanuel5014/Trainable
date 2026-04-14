package com.emanuel5014.trainable.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity

data class SessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val sets: List<SetLogEntity>
)
