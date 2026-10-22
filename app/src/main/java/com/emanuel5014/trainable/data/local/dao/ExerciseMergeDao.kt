package com.emanuel5014.trainable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseMergeEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseMergeMoveEntity
import com.emanuel5014.trainable.domain.exercise.ExerciseMerges
import com.emanuel5014.trainable.domain.exercise.MergeCheck
import kotlinx.coroutines.flow.Flow

/** What a merge will move from the exercise that goes away to the one that stays. */
data class MergePreview(
    val sets: Int,
    val workouts: Int,
    val routineEntries: Int,
    val routines: Int,
    val maxes: Int,
    val swaps: Int,
    val programEdits: Int
) {
    val isEmpty: Boolean get() = sets + routineEntries + maxes + swaps + programEdits == 0

    /** The figures worth telling the user, in reading order, left out when zero. */
    fun counts(): List<Pair<MergePreviewKind, Int>> = listOf(
        MergePreviewKind.SETS to sets,
        MergePreviewKind.WORKOUTS to workouts,
        MergePreviewKind.ROUTINES to routines,
        MergePreviewKind.MAXES to maxes,
        MergePreviewKind.OTHERS to swaps + programEdits
    ).filter { it.second > 0 }
}

enum class MergePreviewKind { SETS, WORKOUTS, ROUTINES, MAXES, OTHERS }

sealed interface MergeOutcome {
    data class Merged(val mergeId: Int) : MergeOutcome
    data class Rejected(val check: MergeCheck) : MergeOutcome
}

/**
 * The merge log and the SQL that merges two exercises and undoes it. A merge only ever changes the `exercise_id` of
 * rows: nothing is deleted, renumbered or copied, which is what makes undoing it exact.
 */
@Dao
interface ExerciseMergeDao {
    @Insert
    suspend fun insertMerge(merge: ExerciseMergeEntity): Long

    @Query("SELECT * FROM exercise_merges ORDER BY id")
    suspend fun getAllMerges(): List<ExerciseMergeEntity>

    @Query("SELECT * FROM exercise_merges ORDER BY id")
    fun observeAllMerges(): Flow<List<ExerciseMergeEntity>>

    @Query("SELECT * FROM exercise_merge_moves WHERE merge_id = :mergeId ORDER BY id")
    suspend fun getMoves(mergeId: Int): List<ExerciseMergeMoveEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExercise(id: Int): ExerciseEntity?

    @Query("UPDATE exercises SET is_deleted = :hidden WHERE id = :id")
    suspend fun setHidden(id: Int, hidden: Boolean)

    @Query("DELETE FROM exercise_merges WHERE id = :mergeId")
    suspend fun deleteMerge(mergeId: Int)

    // --- what will move (the five places an exercise is referred to) ---

    @Query("SELECT COUNT(*) FROM set_logs WHERE exercise_id = :id")
    suspend fun countSets(id: Int): Int

    @Query("SELECT COUNT(DISTINCT session_id) FROM set_logs WHERE exercise_id = :id")
    suspend fun countWorkouts(id: Int): Int

    @Query("SELECT COUNT(*) FROM plan_exercises WHERE exercise_id = :id")
    suspend fun countRoutineEntries(id: Int): Int

    @Query("SELECT COUNT(DISTINCT plan_id) FROM plan_exercises WHERE exercise_id = :id")
    suspend fun countRoutines(id: Int): Int

    @Query("SELECT COUNT(*) FROM one_rep_maxes WHERE exercise_id = :id")
    suspend fun countMaxes(id: Int): Int

    @Query("SELECT COUNT(*) FROM session_exercise_swaps WHERE replacement_exercise_id = :id")
    suspend fun countSwaps(id: Int): Int

    @Query("SELECT COUNT(*) FROM session_prescription_edits WHERE exercise_id = :id")
    suspend fun countProgramEdits(id: Int): Int

    @Transaction
    suspend fun preview(loserId: Int): MergePreview = MergePreview(
        sets = countSets(loserId),
        workouts = countWorkouts(loserId),
        routineEntries = countRoutineEntries(loserId),
        routines = countRoutines(loserId),
        maxes = countMaxes(loserId),
        swaps = countSwaps(loserId),
        programEdits = countProgramEdits(loserId)
    )

    // --- note which rows move ---

    @Query("INSERT INTO exercise_merge_moves (merge_id, source, row_id) SELECT :mergeId, 'SET_LOG', id FROM set_logs WHERE exercise_id = :exerciseId")
    suspend fun recordSetLogs(mergeId: Int, exerciseId: Int)

    @Query("INSERT INTO exercise_merge_moves (merge_id, source, row_id) SELECT :mergeId, 'PLAN_EXERCISE', id FROM plan_exercises WHERE exercise_id = :exerciseId")
    suspend fun recordPlanExercises(mergeId: Int, exerciseId: Int)

    @Query("INSERT INTO exercise_merge_moves (merge_id, source, row_id) SELECT :mergeId, 'ONE_REP_MAX', id FROM one_rep_maxes WHERE exercise_id = :exerciseId")
    suspend fun recordOneRepMaxes(mergeId: Int, exerciseId: Int)

    @Query("INSERT INTO exercise_merge_moves (merge_id, source, row_id) SELECT :mergeId, 'SWAP_REPLACEMENT', id FROM session_exercise_swaps WHERE replacement_exercise_id = :exerciseId")
    suspend fun recordSwaps(mergeId: Int, exerciseId: Int)

    @Query("INSERT INTO exercise_merge_moves (merge_id, source, row_id) SELECT :mergeId, 'PRESCRIPTION_EDIT', id FROM session_prescription_edits WHERE exercise_id = :exerciseId")
    suspend fun recordProgramEdits(mergeId: Int, exerciseId: Int)

    // --- move them to the winner ---

    @Query("UPDATE set_logs SET exercise_id = :toId WHERE exercise_id = :fromId")
    suspend fun repointSetLogs(fromId: Int, toId: Int)

    @Query("UPDATE plan_exercises SET exercise_id = :toId WHERE exercise_id = :fromId")
    suspend fun repointPlanExercises(fromId: Int, toId: Int)

    @Query("UPDATE one_rep_maxes SET exercise_id = :toId WHERE exercise_id = :fromId")
    suspend fun repointOneRepMaxes(fromId: Int, toId: Int)

    @Query("UPDATE session_exercise_swaps SET replacement_exercise_id = :toId WHERE replacement_exercise_id = :fromId")
    suspend fun repointSwaps(fromId: Int, toId: Int)

    @Query("UPDATE session_prescription_edits SET exercise_id = :toId WHERE exercise_id = :fromId")
    suspend fun repointProgramEdits(fromId: Int, toId: Int)

    // --- and back: only the rows the merge moved, and only while they are still on the winner ---

    @Query("UPDATE set_logs SET exercise_id = :loserId WHERE exercise_id = :winnerId AND id IN (SELECT row_id FROM exercise_merge_moves WHERE merge_id = :mergeId AND source = 'SET_LOG')")
    suspend fun restoreSetLogs(mergeId: Int, winnerId: Int, loserId: Int)

    @Query("UPDATE plan_exercises SET exercise_id = :loserId WHERE exercise_id = :winnerId AND id IN (SELECT row_id FROM exercise_merge_moves WHERE merge_id = :mergeId AND source = 'PLAN_EXERCISE')")
    suspend fun restorePlanExercises(mergeId: Int, winnerId: Int, loserId: Int)

    @Query("UPDATE one_rep_maxes SET exercise_id = :loserId WHERE exercise_id = :winnerId AND id IN (SELECT row_id FROM exercise_merge_moves WHERE merge_id = :mergeId AND source = 'ONE_REP_MAX')")
    suspend fun restoreOneRepMaxes(mergeId: Int, winnerId: Int, loserId: Int)

    @Query("UPDATE session_exercise_swaps SET replacement_exercise_id = :loserId WHERE replacement_exercise_id = :winnerId AND id IN (SELECT row_id FROM exercise_merge_moves WHERE merge_id = :mergeId AND source = 'SWAP_REPLACEMENT')")
    suspend fun restoreSwaps(mergeId: Int, winnerId: Int, loserId: Int)

    @Query("UPDATE session_prescription_edits SET exercise_id = :loserId WHERE exercise_id = :winnerId AND id IN (SELECT row_id FROM exercise_merge_moves WHERE merge_id = :mergeId AND source = 'PRESCRIPTION_EDIT')")
    suspend fun restoreProgramEdits(mergeId: Int, winnerId: Int, loserId: Int)

    /** Merges [loserId] into [winnerId] (the loser is hidden), or says why it cannot be done and changes nothing. */
    @Transaction
    suspend fun merge(winnerId: Int, loserId: Int, now: Long): MergeOutcome {
        val winner = getExercise(winnerId)
        val loser = getExercise(loserId)
        val check = ExerciseMerges.check(winner, loser, getAllMerges())
        if (check != MergeCheck.OK) return MergeOutcome.Rejected(check)
        requireNotNull(loser)

        val mergeId = insertMerge(
            ExerciseMergeEntity(
                winnerId = winnerId, loserId = loserId, loserName = loser.nome,
                loserWasHidden = loser.isDeleted, timestamp = now
            )
        ).toInt()
        recordSetLogs(mergeId, loserId)
        recordPlanExercises(mergeId, loserId)
        recordOneRepMaxes(mergeId, loserId)
        recordSwaps(mergeId, loserId)
        recordProgramEdits(mergeId, loserId)
        repointSetLogs(loserId, winnerId)
        repointPlanExercises(loserId, winnerId)
        repointOneRepMaxes(loserId, winnerId)
        repointSwaps(loserId, winnerId)
        repointProgramEdits(loserId, winnerId)
        if (!loser.isDeleted) setHidden(loserId, true)
        return MergeOutcome.Merged(mergeId)
    }

    /** Puts back what [mergeId] moved and drops it from the log; false when it is unknown or a later merge builds on it. */
    @Transaction
    suspend fun undo(mergeId: Int): Boolean {
        val merges = getAllMerges()
        if (!ExerciseMerges.canUndo(mergeId, merges)) return false
        val merge = merges.first { it.id == mergeId }

        restoreSetLogs(mergeId, merge.winnerId, merge.loserId)
        restorePlanExercises(mergeId, merge.winnerId, merge.loserId)
        restoreOneRepMaxes(mergeId, merge.winnerId, merge.loserId)
        restoreSwaps(mergeId, merge.winnerId, merge.loserId)
        restoreProgramEdits(mergeId, merge.winnerId, merge.loserId)
        if (!merge.loserWasHidden) setHidden(merge.loserId, false)
        deleteMerge(mergeId)
        return true
    }
}
