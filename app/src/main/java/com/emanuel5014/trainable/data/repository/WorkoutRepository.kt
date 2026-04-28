package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
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
    private val userDao: UserDao,
    private val exerciseDao: ExerciseDao
) {
    fun getAllPlans(): Flow<List<WorkoutPlanEntity>> = workoutDao.getAllPlans()

    fun getActivePlans(): Flow<List<WorkoutPlanEntity>> = workoutDao.getActivePlans()

    fun getExpiredPlans(): Flow<List<WorkoutPlanEntity>> = workoutDao.getExpiredPlans()
    
    fun getAllPlansSorted(): Flow<List<WorkoutPlanEntity>> = workoutDao.getAllPlansSorted()

    fun getAllPlansWithDetails(): Flow<List<PlanWithDetails>> = workoutDao.getAllPlansWithDetails()
    
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

    suspend fun savePlanImage(image: WorkoutPlanImageEntity) = workoutDao.insertPlanImage(image)

    suspend fun deletePlanImage(image: WorkoutPlanImageEntity) = workoutDao.deletePlanImage(image)
    
    suspend fun exportPlans(planIds: List<Int>): String {
        val plans = workoutDao.getPlansWithDetails(planIds)
        val exportDtos = plans.map { planWithDetails ->
            WorkoutPlanExportDto(
                nome = planWithDetails.plan.nome,
                note = planWithDetails.plan.note,
                sessioniTargetSettimana = planWithDetails.plan.sessioniTargetSettimana,
                imageUri = planWithDetails.plan.imageUri,
                images = planWithDetails.images.map { it.imageUri },
                exercises = planWithDetails.exercises.map { exerciseWithDetails ->
                    PlanExerciseExportDto(
                        exerciseId = exerciseWithDetails.exercise.id,
                        exerciseName = exerciseWithDetails.exercise.nome,
                        exerciseCategory = exerciseWithDetails.exercise.categoria,
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
        val json = Json { ignoreUnknownKeys = true }
        val importDtos = json.decodeFromString<List<WorkoutPlanExportDto>>(jsonData)
        
        val currentPlans = workoutDao.getAllPlans().first()
        val allExercises = exerciseDao.getAllExercises().first()
        var nextOrder = (currentPlans.maxOfOrNull { it.ordine } ?: -1) + 1

        importDtos.forEach { dto ->
            val newPlan = WorkoutPlanEntity(
                userId = user.id,
                nome = dto.nome,
                dataInizio = System.currentTimeMillis(),
                note = dto.note,
                isActive = true,
                sessioniTargetSettimana = dto.sessioniTargetSettimana,
                imageUri = dto.imageUri,
                ordine = nextOrder++
            )
            val planId = workoutDao.insertPlan(newPlan).toInt()
            
            // Import multiple images
            val imagesToInsert = mutableListOf<WorkoutPlanImageEntity>()
            
            // 1. From the new 'images' list
            dto.images.forEachIndexed { index, uri ->
                imagesToInsert.add(WorkoutPlanImageEntity(planId = planId, imageUri = uri, ordine = index))
            }
            
            // 2. Fallback for old single 'imageUri' if 'images' is empty
            if (imagesToInsert.isEmpty() && dto.imageUri != null) {
                imagesToInsert.add(WorkoutPlanImageEntity(planId = planId, imageUri = dto.imageUri, ordine = 0))
            }
            
            if (imagesToInsert.isNotEmpty()) {
                workoutDao.insertPlanImages(imagesToInsert)
            }
            
            val exercisesToInsert = mutableListOf<PlanExerciseEntity>()
            
            dto.exercises.forEach { exerciseDto ->
                var finalExerciseId: Int? = null
                
                // 1. Try to find by ID
                val exerciseById = allExercises.find { it.id == exerciseDto.exerciseId }
                if (exerciseById != null) {
                    finalExerciseId = exerciseById.id
                } else if (exerciseDto.exerciseName != null) {
                    // 2. Try to find by Name
                    val exerciseByName = allExercises.find { 
                        it.nome.equals(exerciseDto.exerciseName, ignoreCase = true) 
                    }
                    if (exerciseByName != null) {
                        finalExerciseId = exerciseByName.id
                    } else {
                        // 3. Create new custom exercise if we have name info
                        try {
                            val maxId = exerciseDao.getMaxId()
                            val newId = if (maxId < 1000) 1000 else maxId + 1
                            val newExercise = ExerciseEntity(
                                id = newId,
                                nome = exerciseDto.exerciseName,
                                categoria = exerciseDto.exerciseCategory ?: "Custom"
                            )
                            exerciseDao.insertExercise(newExercise)
                            finalExerciseId = newId
                            // Update allExercises to include the new one for subsequent lookups
                            // (though unlikely to be needed in the same import loop unless duplicate exercises exist)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                if (finalExerciseId != null) {
                    exercisesToInsert.add(
                        PlanExerciseEntity(
                            planId = planId,
                            exerciseId = finalExerciseId,
                            serieTarget = exerciseDto.serieTarget,
                            repsTarget = exerciseDto.repsTarget,
                            recuperoTarget = exerciseDto.recuperoTarget,
                            ordine = exerciseDto.ordine
                        )
                    )
                }
            }
            
            if (exercisesToInsert.isNotEmpty()) {
                workoutDao.insertPlanExercises(exercisesToInsert)
            }
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
