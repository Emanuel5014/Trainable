package com.emanuel5014.trainable.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SessionPrescriptionEditEntity
import com.emanuel5014.trainable.data.local.entity.SessionTagEntity
import com.emanuel5014.trainable.data.local.entity.SessionTagRefEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.domain.journal.SessionJournal

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
    val cardio: List<CardioLogEntity> = emptyList(),
    /** Changes made to the prescription of an exercise during the workout, oldest first. */
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val prescriptionEdits: List<SessionPrescriptionEditEntity> = emptyList(),
    /** The tags of the workout journal on this workout. */
    @Relation(
        entity = SessionTagEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SessionTagRefEntity::class,
            parentColumn = "session_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<SessionTagEntity> = emptyList()
)

/** The journal of this workout: rating, note and the ids of its tags. */
fun SessionWithDetails.toJournal(): SessionJournal =
    SessionJournal(rating = session.rating, note = session.journalNote, tagIds = tags.map { it.id }.toSet())
