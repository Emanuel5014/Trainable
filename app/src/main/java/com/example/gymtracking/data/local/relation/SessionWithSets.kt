package com.example.gymtracking.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymtracking.data.local.entity.SetLogEntity
import com.example.gymtracking.data.local.entity.WorkoutSessionEntity

data class SessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val sets: List<SetLogEntity>
)
