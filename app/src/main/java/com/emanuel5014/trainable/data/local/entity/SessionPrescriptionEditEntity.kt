package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emanuel5014.trainable.domain.prescription.BlocksCodec
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.ProgramEditKind
import com.emanuel5014.trainable.domain.prescription.ProgramEditScope

/**
 * A change made to the prescription of an exercise while its workout was running (or afterwards, from the history):
 * the blocks it had, the blocks it got and whether the routine's week was updated too. An exercise changed twice has
 * two rows, oldest first, so the whole story can be told in the report.
 */
@Entity(
    tableName = "session_prescription_edits",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id"), Index("exercise_id")]
)
data class SessionPrescriptionEditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Int,
    /** The exercise the prescription belongs to when it was changed. */
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Int,
    /** The exercise of the routine it came from; null for one swapped in, added or done in a quick workout. */
    @ColumnInfo(name = "plan_exercise_id")
    val planExerciseId: Int? = null,
    /** Week of the routine the workout followed. */
    @ColumnInfo(name = "program_week")
    val programWeek: Int? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    /** [ProgramEditKind.code]. */
    @ColumnInfo(name = "kind")
    val kind: String = ProgramEditKind.PROGRAM.code,
    /** [ProgramEditScope.code]. */
    @ColumnInfo(name = "scope")
    val scope: String = ProgramEditScope.WORKOUT.code,
    /** The blocks before the change, see [BlocksCodec]. */
    @ColumnInfo(name = "before_blocks")
    val beforeBlocks: String,
    /** The blocks after the change. */
    @ColumnInfo(name = "after_blocks")
    val afterBlocks: String,
    /**
     * The order the exercise had in the workout when it was changed ([SetLogEntity.ordineEsercizio]): it tells the runs
     * of an exercise done twice apart. Null for an edit saved before runs were told apart.
     */
    @ColumnInfo(name = "ordine_esercizio")
    val order: Int? = null
) {
    val editKind: ProgramEditKind get() = ProgramEditKind.fromCode(kind)
    val editScope: ProgramEditScope get() = ProgramEditScope.fromCode(scope)
    val before: List<PrescriptionBlock> get() = BlocksCodec.decode(beforeBlocks)
    val after: List<PrescriptionBlock> get() = BlocksCodec.decode(afterBlocks)

    companion object {
        fun of(
            sessionId: Int,
            exerciseId: Int,
            planExerciseId: Int?,
            programWeek: Int?,
            kind: ProgramEditKind,
            scope: ProgramEditScope,
            before: List<PrescriptionBlock>,
            after: List<PrescriptionBlock>,
            createdAt: Long = System.currentTimeMillis(),
            order: Int? = null
        ) = SessionPrescriptionEditEntity(
            sessionId = sessionId,
            exerciseId = exerciseId,
            planExerciseId = planExerciseId,
            programWeek = programWeek,
            createdAt = createdAt,
            kind = kind.code,
            scope = scope.code,
            beforeBlocks = BlocksCodec.encode(before),
            afterBlocks = BlocksCodec.encode(after),
            order = order
        )
    }
}

/** The edits of one exercise of a session, oldest first. */
fun List<SessionPrescriptionEditEntity>.forExercise(exerciseId: Int): List<SessionPrescriptionEditEntity> =
    filter { it.exerciseId == exerciseId }.sortedWith(compareBy({ it.createdAt }, { it.id }))

/**
 * The edits of one run of an exercise in a session, oldest first. An exercise done twice in the workout has a story per
 * run: the edits made at that run's [order]. An edit saved before runs were told apart (no order), or whose order no run
 * of the exercise has any more ([runOrders] are the orders of all of them), is shown on every run, as it always was.
 */
fun List<SessionPrescriptionEditEntity>.forRun(exerciseId: Int, order: Int, runOrders: Collection<Int>): List<SessionPrescriptionEditEntity> =
    forExercise(exerciseId).filter { it.order == null || it.order == order || it.order !in runOrders }

/**
 * The prescription an exercise of the session was really done against when it was changed along the way: what the last
 * edit left, null when it was never changed.
 */
fun List<SessionPrescriptionEditEntity>.finalBlocks(exerciseId: Int): List<PrescriptionBlock>? =
    forExercise(exerciseId).lastOrNull()?.after?.takeIf { it.isNotEmpty() }

/** What the exercise's prescription was when the workout began: the first edit's "before". */
fun List<SessionPrescriptionEditEntity>.originalBlocks(exerciseId: Int): List<PrescriptionBlock>? =
    forExercise(exerciseId).firstOrNull()?.before?.takeIf { it.isNotEmpty() }
