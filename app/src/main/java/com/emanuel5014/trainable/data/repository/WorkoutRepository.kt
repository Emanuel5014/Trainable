package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SessionExerciseSwapEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.local.relation.SessionWithSets
import com.emanuel5014.trainable.data.remote.dto.PlanExerciseExportDto
import com.emanuel5014.trainable.data.remote.dto.WorkoutPlanExportDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val userDao: UserDao
) {
    fun getAllPlans(): Flow<List<WorkoutPlanEntity>> = workoutDao.getAllPlans()

    fun getActivePlans(): Flow<List<WorkoutPlanEntity>> = workoutDao.getActivePlans()

    fun getExpiredPlans(): Flow<List<WorkoutPlanEntity>> = workoutDao.getExpiredPlans()
    
    fun getAllPlansSorted(): Flow<List<WorkoutPlanEntity>> = workoutDao.getAllPlansSorted()
    
    fun getPlanWithDetails(planId: Int): Flow<PlanWithDetails?> = workoutDao.getPlanWithDetails(planId)
    
    suspend fun savePlan(plan: WorkoutPlanEntity): Long {
        return workoutDao.insertPlan(plan)
    }

    suspend fun savePlans(plans: List<WorkoutPlanEntity>) = workoutDao.updatePlans(plans)

    suspend fun updatePlan(plan: WorkoutPlanEntity) = workoutDao.updatePlan(plan)

    suspend fun setPlanActive(planId: Int, active: Boolean) = workoutDao.setPlanActive(planId, active)

    suspend fun savePlanExercise(exercise: PlanExerciseEntity): Long = workoutDao.insertPlanExercise(exercise)
    
    suspend fun savePlanExercises(exercises: List<PlanExerciseEntity>) {
        workoutDao.insertPlanExercises(exercises)
    }

    suspend fun updatePlanExercise(exercise: PlanExerciseEntity) = workoutDao.updatePlanExercise(exercise)

    suspend fun deletePlanExercise(exercise: PlanExerciseEntity) = workoutDao.deletePlanExercise(exercise)
    
    suspend fun exportPlans(planIds: List<Int>): String {
        val plans = workoutDao.getPlansWithDetails(planIds)
        val exportDtos = plans.map { planWithDetails ->
            WorkoutPlanExportDto(
                nome = planWithDetails.plan.nome,
                note = planWithDetails.plan.note,
                sessioniTargetSettimana = planWithDetails.plan.sessioniTargetSettimana,
                exercises = planWithDetails.exercises.map { exerciseWithDetails ->
                    PlanExerciseExportDto(
                        exerciseId = exerciseWithDetails.exercise.id,
                        serieTarget = exerciseWithDetails.planExercise.serieTarget,
                        repsTarget = exerciseWithDetails.planExercise.repsTarget,
                        recuperoTarget = exerciseWithDetails.planExercise.recuperoTarget,
                        ordine = exerciseWithDetails.planExercise.ordine
                    )
                }
            )
        }
        return Json.encodeToString(exportDtos)
    }

    suspend fun importPlans(jsonData: String) {
        val user = userDao.getUser().first() ?: return
        val importDtos = Json.decodeFromString<List<WorkoutPlanExportDto>>(jsonData)
        
        val currentPlans = workoutDao.getAllPlans().first()
        var nextOrder = (currentPlans.maxOfOrNull { it.ordine } ?: -1) + 1

        importDtos.forEach { dto ->
            val newPlan = WorkoutPlanEntity(
                userId = user.id,
                nome = dto.nome,
                dataInizio = System.currentTimeMillis(),
                note = dto.note,
                isActive = true,
                sessioniTargetSettimana = dto.sessioniTargetSettimana,
                ordine = nextOrder++
            )
            val planId = workoutDao.insertPlan(newPlan).toInt()
            
            val exercises = dto.exercises.map { exerciseDto ->
                PlanExerciseEntity(
                    planId = planId,
                    exerciseId = exerciseDto.exerciseId,
                    serieTarget = exerciseDto.serieTarget,
                    repsTarget = exerciseDto.repsTarget,
                    recuperoTarget = exerciseDto.recuperoTarget,
                    ordine = exerciseDto.ordine
                )
            }
            workoutDao.insertPlanExercises(exercises)
        }
    }

    suspend fun deletePlan(plan: WorkoutPlanEntity) = workoutDao.deletePlan(plan)
    
    suspend fun startSession(planId: Int, timestamp: Long, isFinished: Boolean = false): Long {
        return workoutDao.insertSession(WorkoutSessionEntity(planId = planId, timestamp = timestamp, isFinished = isFinished))
    }
    
    fun getSessionWithSets(sessionId: Int): Flow<SessionWithSets?> = workoutDao.getSessionWithSets(sessionId)

    fun getSessionWithDetails(sessionId: Int): Flow<SessionWithDetails?> = workoutDao.getSessionWithDetails(sessionId)

    fun getAllSessionsWithDetails(): Flow<List<SessionWithDetails>> = workoutDao.getAllSessionsWithDetails()

    fun getAllSessions(): Flow<List<WorkoutSessionEntity>> = workoutDao.getAllSessions()
    
    fun getRecentSessions(limit: Int = 10): Flow<List<WorkoutSessionEntity>> = workoutDao.getRecentSessions(limit)

    fun getUnfinishedSessions(): Flow<List<WorkoutSessionEntity>> = workoutDao.getUnfinishedSessions()

    fun getUnfinishedSessionsWithPlanName(): Flow<List<SessionWithPlanName>> = workoutDao.getUnfinishedSessionsWithPlanName()

    suspend fun setSessionFinished(sessionId: Int) = workoutDao.setSessionFinished(sessionId)

    suspend fun updateRestTimer(sessionId: Int, endTime: Long?, totalSeconds: Int?) = workoutDao.updateRestTimer(sessionId, endTime, totalSeconds)
    
    suspend fun logSet(set: SetLogEntity) = workoutDao.insertSet(set)
    
    suspend fun updateSet(set: SetLogEntity) = workoutDao.updateSet(set)
    
    suspend fun deleteSet(set: SetLogEntity) = workoutDao.deleteSet(set)
    
    fun getPreviousSetsForExercise(exerciseId: Int): Flow<List<SetLogEntity>> = workoutDao.getPreviousSetsForExercise(exerciseId)

    fun getLastSessionSetsForExercise(planId: Int, exerciseId: Int, limitSets: Int): Flow<List<SetLogEntity>> = 
        workoutDao.getLastSessionSetsForExercise(planId, exerciseId, limitSets)

    suspend fun deleteSession(sessionId: Int) = workoutDao.deleteSession(sessionId)

    suspend fun updateSession(session: WorkoutSessionEntity) = workoutDao.updateSession(session)

    suspend fun deleteExerciseFromSession(sessionId: Int, exerciseId: Int) = 
        workoutDao.deleteExerciseFromSession(sessionId, exerciseId)

    suspend fun updateSetOrders(sets: List<SetLogEntity>) = workoutDao.updateSetOrders(sets)

    suspend fun saveExerciseSwap(swap: SessionExerciseSwapEntity) = workoutDao.insertExerciseSwap(swap)

    fun getSwapsForSession(sessionId: Int): Flow<List<SessionExerciseSwapEntity>> = workoutDao.getSwapsForSession(sessionId)

    suspend fun removeExerciseSwap(sessionId: Int, originalExerciseId: Int) = workoutDao.removeSwapForExercise(sessionId, originalExerciseId)

    suspend fun exportAllWorkoutsToCsv(): String {
        val sessions = workoutDao.getAllSessionsWithDetails().first()
        
        val sb = StringBuilder()
        sb.appendLine("Date,Session ID,Exercise,Category,Set,Weight (kg),Reps,Note,Warmup")
        
        sessions.forEach { session ->
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(session.session.timestamp))
            
            session.sets.forEach { setWithExercise ->
                val setLog = setWithExercise.setLog
                val exercise = setWithExercise.exercise
                val exerciseName = exercise.nome
                val category = exercise.categoria
                val note = setLog.note?.replace(",", ";")?.replace("\n", " ") ?: ""
                
                sb.appendLine("$date,${session.session.id},$exerciseName,$category,${setLog.numeroSerie},${setLog.pesoSollevato},${setLog.repsEffettive},$note,${setLog.isWarmup}")
            }
        }
        
        return sb.toString()
    }
}
