package com.emanuel5014.trainable.data.repository

import android.content.Context
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
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
import com.emanuel5014.trainable.util.ImageStorageUtils
import com.emanuel5014.trainable.util.UriMigrationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val userDao: UserDao,
    private val exerciseDao: ExerciseDao,
    @ApplicationContext private val context: Context
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
    
    suspend fun exportPlans(planIds: List<Int>, includeImages: Boolean = true): String {
        val plans = workoutDao.getPlansWithDetails(planIds)
        val exportDtos = plans.map { planWithDetails ->
            WorkoutPlanExportDto(
                nome = planWithDetails.plan.nome,
                note = planWithDetails.plan.note,
                sessioniTargetSettimana = planWithDetails.plan.sessioniTargetSettimana,
                imageUri = planWithDetails.plan.imageUri,
                images = planWithDetails.images.map { it.imageUri },
                imageBlobs = if (includeImages) {
                    buildList {
                        // Collect all unique URIs to encode
                        val uris = (listOfNotNull(planWithDetails.plan.imageUri) + planWithDetails.images.map { it.imageUri }).distinct()
                        uris.forEach { uri ->
                            val fixedUri = UriMigrationHelper.fixPath(uri, context) ?: uri
                            ImageStorageUtils.encodeImageToBase64(context, fixedUri)?.let { add(it) }
                        }
                    }
                } else emptyList(),
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
            
            val imagesToInsert = mutableListOf<WorkoutPlanImageEntity>()

            // 1. From encoded blobs (new way)
            if (dto.imageBlobs.isNotEmpty()) {
                dto.imageBlobs.forEachIndexed { index, blob ->
                    ImageStorageUtils.saveBase64Image(context, blob)?.let { newUri ->
                        imagesToInsert.add(WorkoutPlanImageEntity(planId = planId, imageUri = newUri, ordine = index))
                    }
                }
            } else {
                // 2. From the new 'images' list (old way, might contain invalid URIs)
                dto.images.forEachIndexed { index, uri ->
                    imagesToInsert.add(WorkoutPlanImageEntity(planId = planId, imageUri = uri, ordine = index))
                }
                
                // 3. Fallback for old single 'imageUri' if 'images' is empty
                if (imagesToInsert.isEmpty() && dto.imageUri != null) {
                    imagesToInsert.add(WorkoutPlanImageEntity(planId = planId, imageUri = dto.imageUri, ordine = 0))
                }
            }
            
            if (imagesToInsert.isNotEmpty()) {
                workoutDao.insertPlanImages(imagesToInsert)
                // Update the main plan image with the first imported image if it was null or broken
                imagesToInsert.firstOrNull()?.let { firstImage ->
                    workoutDao.updatePlan(newPlan.copy(id = planId, imageUri = firstImage.imageUri))
                }
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

    suspend fun deletePlan(plan: WorkoutPlanEntity) {
        val user = userDao.getUser().first()
        if (user != null) {
            val allPlans = workoutDao.getAllPlans().first()
            val customPlan = allPlans.find { it.nome == "Custom Workout" && it.note == "SYSTEM_PLAN" }
                ?: allPlans.find { it.nome == "Custom Workout" && it.note == null && !it.isActive } // Legacy
                ?: run {
                    val newId = workoutDao.insertPlan(
                        WorkoutPlanEntity(
                            userId = user.id,
                            nome = "Custom Workout",
                            note = "SYSTEM_PLAN",
                            dataInizio = System.currentTimeMillis(),
                            isActive = false
                        )
                    )
                    workoutDao.getAllPlans().first().find { it.id == newId.toInt() }!!
                }

            if (customPlan.note == null) {
                workoutDao.updatePlan(customPlan.copy(note = "SYSTEM_PLAN"))
            }

            workoutDao.reassignSessions(plan.id, customPlan.id, plan.nome)
        }
        workoutDao.deletePlan(plan)
    }
    
    suspend fun startSession(planId: Int, timestamp: Long, isFinished: Boolean = false, note: String? = null): Long {
        return workoutDao.insertSession(WorkoutSessionEntity(planId = planId, timestamp = timestamp, isFinished = isFinished, noteSessione = note))
    }

    suspend fun startQuickWorkoutSession(name: String? = null): Long {
        val user = userDao.getUser().first() ?: return -1
        val allPlans = workoutDao.getAllPlans().first()
        val quickWorkoutPlan = allPlans.find { it.nome == "Quick Workout" && it.note == "SYSTEM_PLAN" }
            ?: allPlans.find { it.nome == "Allenamento Veloce" && it.note == "SYSTEM_PLAN" }
            ?: run {
                val newId = workoutDao.insertPlan(
                    WorkoutPlanEntity(
                        userId = user.id,
                        nome = "Quick Workout",
                        note = "SYSTEM_PLAN",
                        dataInizio = System.currentTimeMillis(),
                        isActive = false
                    )
                )
                workoutDao.getAllPlans().first().find { it.id == newId.toInt() }!!
            }
        
        return startSession(
            planId = quickWorkoutPlan.id,
            timestamp = System.currentTimeMillis(),
            note = name
        )
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
    
    suspend fun saveCardioLog(cardio: CardioLogEntity) = workoutDao.insertCardioLog(cardio)

    suspend fun saveCardioSession(categoria: String, distanza: Float, durataSecondi: Int, timestamp: Long) {
        val user = userDao.getUser().first() ?: return

        // Find or create "Cardio" plan with system tag
        val plans = workoutDao.getAllPlans().first()
        var cardioPlan = plans.find { it.nome == "Cardio" && it.note == "SYSTEM_PLAN" }
            ?: plans.find { it.nome == "Cardio" && it.note == null && !it.isActive } // Legacy check

        if (cardioPlan == null) {
            val newPlan = WorkoutPlanEntity(
                userId = user.id,
                nome = "Cardio",
                note = "SYSTEM_PLAN",
                dataInizio = System.currentTimeMillis(),
                isActive = false, // Not a regular training plan
                ordine = (plans.maxOfOrNull { it.ordine } ?: -1) + 1
            )
            val planId = workoutDao.insertPlan(newPlan).toInt()
            cardioPlan = newPlan.copy(id = planId)
        } else if (cardioPlan.note == null) {
            // Tag legacy plan
            cardioPlan = cardioPlan.copy(note = "SYSTEM_PLAN")
            workoutDao.updatePlan(cardioPlan)
        }

        val sessionId = workoutDao.insertSession(
            WorkoutSessionEntity(
                planId = cardioPlan.id,
                timestamp = timestamp,
                isFinished = true
            )
        ).toInt()

        workoutDao.insertCardioLog(
            CardioLogEntity(
                sessionId = sessionId,
                categoria = categoria,
                distanza = distanza,
                durataSecondi = durataSecondi,
                timestamp = timestamp
            )
        )
    }

    suspend fun createEmptyManualSession(timestamp: Long): Long {
        val user = userDao.getUser().first() ?: return -1
        val allPlans = workoutDao.getAllPlans().first()
        val customPlan = allPlans.find { it.nome == "Custom Workout" && it.note == "SYSTEM_PLAN" }
            ?: allPlans.find { it.nome == "Custom Workout" && it.note == null && !it.isActive } // Legacy
            ?: run {
                val newId = workoutDao.insertPlan(
                    WorkoutPlanEntity(
                        userId = user.id,
                        nome = "Custom Workout",
                        note = "SYSTEM_PLAN",
                        dataInizio = System.currentTimeMillis(),
                        isActive = false
                    )
                )
                // Retrieve the inserted plan
                workoutDao.getAllPlans().first().find { it.id == newId.toInt() }!!
            }
        
        if (customPlan.note == null) {
            workoutDao.updatePlan(customPlan.copy(note = "SYSTEM_PLAN"))
        }

        return workoutDao.insertSession(
            WorkoutSessionEntity(
                planId = customPlan.id,
                timestamp = timestamp,
                isFinished = true
            )
        )
    }

    suspend fun createManualSessionFromPlan(planId: Int, timestamp: Long): Long {
        val planDetails = workoutDao.getPlanWithDetails(planId).first() ?: return -1
        
        // Find last session for this plan to autocomplete values
        val lastSession = workoutDao.getLastFinishedSessionForPlan(planId).first()
        val lastSessionSets = lastSession?.let { workoutDao.getSessionWithSets(it.id).first()?.sets }
        
        val sessionId = workoutDao.insertSession(
            WorkoutSessionEntity(
                planId = planId,
                timestamp = timestamp,
                isFinished = true
            )
        )
        
        planDetails.exercises.forEach { exerciseWithDetails ->
            val planEx = exerciseWithDetails.planExercise
            val exerciseId = planEx.exerciseId
            
            // Try to find sets for this exercise in the last session
            val prevSets = lastSessionSets?.filter { it.exerciseId == exerciseId }?.sortedBy { it.numeroSerie }
            
            val numSets = if (prevSets != null && prevSets.isNotEmpty()) {
                prevSets.size
            } else if (planEx.serieTarget > 0) {
                planEx.serieTarget
            } else {
                3
            }
            
            for (i in 1..numSets) {
                val prevSet = prevSets?.getOrNull(i - 1)
                workoutDao.insertSet(
                    SetLogEntity(
                        sessionId = sessionId.toInt(),
                        exerciseId = exerciseId,
                        pesoSollevato = prevSet?.pesoSollevato ?: 0f,
                        repsEffettive = prevSet?.repsEffettive ?: planEx.repsTarget.toIntOrNull() ?: 10,
                        numeroSerie = i,
                        ordineEsercizio = planEx.ordine,
                        supersetId = planEx.supersetId
                    )
                )
            }
        }
        return sessionId
    }

    suspend fun updateSet(set: SetLogEntity) = workoutDao.updateSet(set)

    suspend fun deleteSet(set: SetLogEntity) = workoutDao.deleteSet(set)

    suspend fun deleteCardioLog(cardio: CardioLogEntity) = workoutDao.deleteCardioLog(cardio)
    
    fun getPreviousSetsForExercise(exerciseId: Int): Flow<List<SetLogEntity>> = workoutDao.getPreviousSetsForExercise(exerciseId)

    fun getLastSessionSetsForExercise(planId: Int, exerciseId: Int, limitSets: Int): Flow<List<SetLogEntity>> = 
        workoutDao.getLastSessionSetsForExercise(planId, exerciseId, limitSets)

    suspend fun deleteSession(sessionId: Int) = workoutDao.deleteSession(sessionId)

    suspend fun updateSession(session: WorkoutSessionEntity) = workoutDao.updateSession(session)

    suspend fun deleteExerciseFromSession(sessionId: Int, exerciseId: Int) = 
        workoutDao.deleteExerciseFromSession(sessionId, exerciseId)

    suspend fun updateSetOrders(sets: List<SetLogEntity>) = workoutDao.updateSetOrders(sets)

    suspend fun deleteUncompletedSetsForSession(sessionId: Int) = workoutDao.deleteUncompletedSetsForSession(sessionId)

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
