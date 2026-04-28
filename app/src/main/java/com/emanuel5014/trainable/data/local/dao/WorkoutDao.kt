package com.emanuel5014.trainable.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SessionExerciseSwapEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.local.relation.SessionWithSets
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // --- Workout Plans ---
    @Query("SELECT * FROM workout_plans ORDER BY ordine ASC")
    fun getAllPlans(): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plans WHERE is_active = 1 ORDER BY ordine ASC")
    fun getActivePlans(): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plans WHERE is_active = 0 ORDER BY ordine ASC")
    fun getExpiredPlans(): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plans ORDER BY is_active DESC, ordine ASC")
    fun getAllPlansSorted(): Flow<List<WorkoutPlanEntity>>

    @Transaction
    @Query("SELECT * FROM workout_plans WHERE id = :planId")
    fun getPlanWithDetails(planId: Int): Flow<PlanWithDetails?>

    @Transaction
    @Query("SELECT * FROM workout_plans WHERE id IN (:planIds)")
    suspend fun getPlansWithDetails(planIds: List<Int>): List<PlanWithDetails>

    @Transaction
    @Query("SELECT * FROM workout_plans ORDER BY is_active DESC, ordine ASC")
    fun getAllPlansWithDetails(): Flow<List<PlanWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: WorkoutPlanEntity)

    @Update
    suspend fun updatePlans(plans: List<WorkoutPlanEntity>)

    @Query("UPDATE workout_plans SET is_active = :active WHERE id = :planId")
    suspend fun setPlanActive(planId: Int, active: Boolean)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlanEntity)

    // --- Workout Plan Images ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanImage(image: WorkoutPlanImageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanImages(images: List<WorkoutPlanImageEntity>)

    @Update
    suspend fun updatePlanImage(image: WorkoutPlanImageEntity)

    @Delete
    suspend fun deletePlanImage(image: WorkoutPlanImageEntity)

    @Query("DELETE FROM workout_plan_images WHERE plan_id = :planId")
    suspend fun deleteImagesForPlan(planId: Int)

    @Query("SELECT * FROM workout_plan_images WHERE plan_id = :planId ORDER BY ordine ASC")
    fun getImagesForPlan(planId: Int): Flow<List<WorkoutPlanImageEntity>>

    @Transaction
    suspend fun updatePlanImageOrders(images: List<WorkoutPlanImageEntity>) {
        images.forEach { updatePlanImage(it) }
    }

    // --- Plan Exercises ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercise(exercise: PlanExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercises(exercises: List<PlanExerciseEntity>)

    @Update
    suspend fun updatePlanExercise(exercise: PlanExerciseEntity)

    @Delete
    suspend fun deletePlanExercise(exercise: PlanExerciseEntity)

    // --- Sessions & Sets ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun getSessionWithSets(sessionId: Int): Flow<SessionWithSets?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun getSessionWithDetails(sessionId: Int): Flow<SessionWithDetails?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE is_finished = 1 ORDER BY timestamp DESC")
    fun getAllSessionsWithDetails(): Flow<List<SessionWithDetails>>

    @Query("SELECT * FROM workout_sessions WHERE is_finished = 1 ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE is_finished = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE is_finished = 0 ORDER BY timestamp DESC")
    fun getUnfinishedSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("""
        SELECT ws.*, wp.nome as plan_nome 
        FROM workout_sessions ws 
        INNER JOIN workout_plans wp ON ws.plan_id = wp.id 
        WHERE ws.is_finished = 0 
        ORDER BY ws.timestamp DESC
    """)
    fun getUnfinishedSessionsWithPlanName(): Flow<List<SessionWithPlanName>>

    @Query("UPDATE workout_sessions SET is_finished = 1 WHERE id = :sessionId")
    suspend fun setSessionFinished(sessionId: Int)

    @Query("UPDATE workout_sessions SET rest_timer_end_time = :endTime, total_rest_seconds = :totalSeconds WHERE id = :sessionId")
    suspend fun updateRestTimer(sessionId: Int, endTime: Long?, totalSeconds: Int?)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetLogEntity): Long
    
    @Update
    suspend fun updateSet(set: SetLogEntity)
    
    @Delete
    suspend fun deleteSet(set: SetLogEntity)
    
    @Query("""
        SELECT * FROM set_logs 
        WHERE exercise_id = :exerciseId 
        AND session_id = (
            SELECT session_id FROM set_logs 
            WHERE exercise_id = :exerciseId 
            ORDER BY session_id DESC LIMIT 1
        )
        ORDER BY numero_serie ASC
    """)
    fun getPreviousSetsForExercise(exerciseId: Int): Flow<List<SetLogEntity>>

    @Query("""
        SELECT sl.* FROM set_logs sl
        INNER JOIN workout_sessions ws ON sl.session_id = ws.id
        WHERE ws.plan_id = :planId
        AND sl.exercise_id = :exerciseId
        AND ws.is_finished = 1
        ORDER BY sl.session_id DESC
        LIMIT :limitSets
    """)
    fun getLastSessionSetsForExercise(planId: Int, exerciseId: Int, limitSets: Int): Flow<List<SetLogEntity>>

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Int)

    @Query("DELETE FROM set_logs WHERE session_id = :sessionId AND exercise_id = :exerciseId")
    suspend fun deleteExerciseFromSession(sessionId: Int, exerciseId: Int)

    @Transaction
    suspend fun updateSetOrders(sets: List<SetLogEntity>) {
        sets.forEach { updateSet(it) }
    }

    // --- Exercise Swaps ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseSwap(swap: SessionExerciseSwapEntity)

    @Query("SELECT * FROM session_exercise_swaps WHERE session_id = :sessionId")
    fun getSwapsForSession(sessionId: Int): Flow<List<SessionExerciseSwapEntity>>

    @Query("DELETE FROM session_exercise_swaps WHERE session_id = :sessionId AND original_exercise_id = :originalExerciseId")
    suspend fun removeSwapForExercise(sessionId: Int, originalExerciseId: Int)
}
