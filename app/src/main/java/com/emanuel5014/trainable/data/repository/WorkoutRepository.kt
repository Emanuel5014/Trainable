package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.entity.parts
import com.emanuel5014.trainable.data.local.entity.pauses
import com.emanuel5014.trainable.data.local.entity.performed
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.TIME_AND_WEIGHT
import com.emanuel5014.trainable.data.local.entity.kind
import android.content.Context
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.dao.OneRepMaxDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseBlockEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.byWeek
import com.emanuel5014.trainable.data.local.entity.withRepsRange
import com.emanuel5014.trainable.data.local.entity.SessionExerciseSwapEntity
import com.emanuel5014.trainable.data.local.entity.SessionPrescriptionEditEntity
import com.emanuel5014.trainable.data.local.entity.forExercise
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.ExerciseSetWithSession
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.local.relation.SessionWithSets
import com.emanuel5014.trainable.data.remote.dto.PlanExerciseExportDto
import com.emanuel5014.trainable.data.remote.dto.TrainablePlanParser
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvHeaders
import com.emanuel5014.trainable.domain.csvimport.JournalCsv
import com.emanuel5014.trainable.domain.csvimport.SessionDuration
import com.emanuel5014.trainable.util.WeightUnitConverter
import com.emanuel5014.trainable.domain.prescription.LegacyReps
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.domain.prescription.PrescriptionResolver
import com.emanuel5014.trainable.domain.prescription.RepRange
import com.emanuel5014.trainable.domain.prescription.ResolvedPrescription
import com.emanuel5014.trainable.domain.prescription.StartingWeight
import com.emanuel5014.trainable.domain.prescription.WeekSetCodec
import com.emanuel5014.trainable.data.remote.dto.WorkoutPlanExportDto
import com.emanuel5014.trainable.util.ImageStorageUtils
import com.emanuel5014.trainable.util.UriMigrationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val userDao: UserDao,
    private val exerciseDao: ExerciseDao,
    private val oneRepMaxDao: OneRepMaxDao,
    private val userPreferencesRepository: UserPreferencesRepository,
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

    /**
     * Replaces the advanced prescription of a plan exercise. An empty map turns it back into a
     * plain exercise. Legacy `serieTarget`/`repsTarget` are kept in sync by the caller.
     */
    suspend fun savePlanExerciseBlocks(planExerciseId: Int, blocksByWeek: Map<Int, List<PrescriptionBlock>>) {
        val entities = blocksByWeek.toSortedMap().flatMap { (week, blocks) ->
            blocks.mapIndexed { index, block -> PlanExerciseBlockEntity.fromDomain(block, planExerciseId, week, index) }
        }
        workoutDao.replaceAllBlocks(planExerciseId, entities)
    }

    /**
     * Gives one week of a plan exercise a new prescription, leaving the other weeks alone. The sets and reps the plan
     * exercise carries for older consumers follow the week the routine is on, as when the routine itself is edited.
     */
    suspend fun replacePlanWeekBlocks(planExerciseId: Int, week: Int, blocks: List<PrescriptionBlock>) {
        workoutDao.replaceWeekBlocks(
            planExerciseId, week,
            blocks.mapIndexed { index, block -> PlanExerciseBlockEntity.fromDomain(block, planExerciseId, week, index) }
        )
        val planExercise = workoutDao.getPlanExerciseOnce(planExerciseId) ?: return
        val currentWeek = workoutDao.getPlanCurrentWeek(planExercise.planId) ?: return
        val byWeek = workoutDao.getBlocksForPlanExercise(planExerciseId).byWeek()
        val summary = (PrescriptionResolver.resolve(byWeek, emptySet(), currentWeek) as? ResolvedPrescription.Blocks)?.blocks ?: return
        val (sets, reps) = PrescriptionExpander.legacyTargets(summary)
        if (sets != planExercise.serieTarget || reps != planExercise.repsTarget) {
            workoutDao.updatePlanExercise(planExercise.copy(serieTarget = sets, repsTarget = reps))
        }
    }

    suspend fun recordPrescriptionEdit(edit: SessionPrescriptionEditEntity): Long = workoutDao.insertPrescriptionEdit(edit)

    suspend fun getPrescriptionEditsForSession(sessionId: Int): List<SessionPrescriptionEditEntity> =
        workoutDao.getPrescriptionEditsForSession(sessionId)

    suspend fun getPrescriptionEditsForSessions(sessionIds: List<Int>): List<SessionPrescriptionEditEntity> =
        if (sessionIds.isEmpty()) emptyList() else workoutDao.getPrescriptionEditsForSessions(sessionIds)

    suspend fun setExcludedWeeks(planExerciseId: Int, weeks: Set<Int>) =
        workoutDao.setExcludedWeeks(planExerciseId, WeekSetCodec.encode(weeks))

    suspend fun setPlanCurrentWeek(planId: Int, week: Int) = workoutDao.setPlanCurrentWeek(planId, week)

    /** Removes a week from every exercise of the plan; later weeks shift down by one. */
    suspend fun deletePlanWeek(plan: WorkoutPlanEntity, week: Int) = workoutDao.deletePlanWeek(plan, week)

    /** Rewrites the rows of one session exercise after its prescription changed (see [PrescriptionSetSync]). */
    suspend fun applySetSync(result: SetSyncResult) =
        workoutDao.applySetChanges(result.toDelete, result.toUpdate, result.toInsert)

    /** Drops blocks of weeks beyond [weeksCount] after the plan has been shortened. */
    suspend fun trimPlanWeeks(planId: Int, weeksCount: Int) = workoutDao.deleteBlocksBeyondWeek(planId, weeksCount)

    suspend fun setSessionProgramWeek(sessionId: Int, week: Int?) = workoutDao.setSessionProgramWeek(sessionId, week)

    suspend fun deletePlanExercise(exercise: PlanExerciseEntity) = workoutDao.deletePlanExercise(exercise)

    suspend fun savePlanImage(image: WorkoutPlanImageEntity) = workoutDao.insertPlanImage(image)

    suspend fun deletePlanImage(image: WorkoutPlanImageEntity) = workoutDao.deletePlanImage(image)

    suspend fun deleteImagesForPlan(planId: Int) = workoutDao.deleteImagesForPlan(planId)

    suspend fun deleteExercisesForPlan(planId: Int) = workoutDao.deleteExercisesForPlan(planId)

    suspend fun resetPlanContent(planId: Int) {
        workoutDao.deleteExercisesForPlan(planId)
        workoutDao.deleteImagesForPlan(planId)
        // Also clear the cover/legacy image URI on the plan itself, otherwise
        // the auto-migration in RoutineDetailViewModel would re-add it as a plan image.
        workoutDao.clearPlanCoverImage(planId)
    }
    
    /** What a share of [planIds] could carry beyond the plain routine, so the user can be asked about it. */
    data class ExportOptions(val hasImages: Boolean, val hasMaximums: Boolean)

    suspend fun exportOptionsFor(planIds: List<Int>): ExportOptions {
        val plans = workoutDao.getPlansWithDetails(planIds)
        val hasImages = plans.any { it.plan.imageUri != null || it.images.isNotEmpty() }
        val hasMaximums = plans.any { plan ->
            plan.exercises.any { it.isAdvanced && oneRepMaxDao.getCurrentOnce(it.exercise.id) != null }
        }
        return ExportOptions(hasImages = hasImages, hasMaximums = hasMaximums)
    }

    /**
     * @param includeMaximums also writes the sender's current 1RM of every advanced exercise, so %1RM
     * prescriptions resolve to kg on the receiving device (which only uses them where it has no 1RM yet).
     */
    suspend fun exportPlans(planIds: List<Int>, includeImages: Boolean = true, includeMaximums: Boolean = false): String {
        val plans = workoutDao.getPlansWithDetails(planIds)
        val exportDtos = plans.map { planWithDetails ->
            WorkoutPlanExportDto(
                nome = planWithDetails.plan.nome,
                note = planWithDetails.plan.note,
                sessioniTargetSettimana = planWithDetails.plan.sessioniTargetSettimana,
                giorniSettimana = planWithDetails.plan.giorniSettimana,
                dataInizio = planWithDetails.plan.dataInizio,
                dataFine = planWithDetails.plan.dataFine,
                weeksCount = planWithDetails.plan.weeksCount,
                currentWeek = planWithDetails.plan.currentWeek,
                autoAdvanceWeek = planWithDetails.plan.autoAdvanceWeek,
                // Only keep raw URIs when blobs are not included (legacy fallback);
                // otherwise the receiver restores images from blobs with fresh local URIs.
                imageUri = if (includeImages) null else planWithDetails.plan.imageUri,
                images = if (includeImages) emptyList() else planWithDetails.images.map { it.imageUri },
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
                        repsMax = exerciseWithDetails.planExercise.repsMax,
                        recuperoTarget = exerciseWithDetails.planExercise.recuperoTarget,
                        ordine = exerciseWithDetails.planExercise.ordine,
                        supersetId = exerciseWithDetails.planExercise.supersetId,
                        exerciseType = exerciseWithDetails.planExercise.exerciseType,
                        durataTargetSecondi = exerciseWithDetails.planExercise.durataTargetSecondi,
                        distanzaTargetKm = exerciseWithDetails.planExercise.distanzaTargetKm,
                        cardioCategoria = exerciseWithDetails.planExercise.cardioCategoria,
                        excludedWeeks = exerciseWithDetails.planExercise.excludedWeeks,
                        oneRepMaxKg = if (includeMaximums && exerciseWithDetails.isAdvanced) {
                            oneRepMaxDao.getCurrentOnce(exerciseWithDetails.exercise.id)?.weightKg
                        } else null,
                        blocks = exerciseWithDetails.blocks.sortedWith(compareBy({ it.week }, { it.ordine })).map { block ->
                            com.emanuel5014.trainable.data.remote.dto.PrescriptionBlockExportDto(
                                week = block.week,
                                ordine = block.ordine,
                                sets = block.sets,
                                reps = block.reps,
                                repMode = block.repMode,
                                totalReps = block.totalReps,
                                intensityType = block.intensityType,
                                intensityValue = block.intensityValue,
                                techniques = block.techniques,
                                restSeconds = block.restSeconds,
                                note = block.note
                            )
                        }
                    )
                }
            )
        }
        return TrainablePlanParser.encode(exportDtos)
    }

    /**
     * Imports plans from a .trainableplan payload (v2 envelope, v1 list or v1 single object).
     * @return number of plans actually imported.
     */
    suspend fun importPlans(jsonData: String): Int {
        val user = userDao.getUser().first() ?: return 0
        val importDtos = TrainablePlanParser.decode(jsonData)
        if (importDtos.isEmpty()) return 0

        val currentPlans = workoutDao.getAllPlans().first()
        // Also the exercises deleted from the library: an imported routine that uses one brings it back instead of duplicating it
        val knownExercises = exerciseDao.getAllExercisesIncludingDeleted().first().toMutableList()
        var nextOrder = (currentPlans.maxOfOrNull { it.ordine } ?: -1) + 1
        var imported = 0

        importDtos.forEach { dto ->
            var insertedPlan: WorkoutPlanEntity? = null
            try {
                // Remap superset ids per imported plan so they never collide
                // with existing plans nor across imported plans.
                val supersetRemap = mutableMapOf<String, String>()
                fun remapSuperset(raw: String?): String? {
                    if (raw == null) return null
                    return supersetRemap.getOrPut(raw) { java.util.UUID.randomUUID().toString() }
                }

                val newPlan = WorkoutPlanEntity(
                    userId = user.id,
                    nome = dto.nome,
                    dataInizio = dto.dataInizio ?: System.currentTimeMillis(),
                    dataFine = dto.dataFine,
                    note = dto.note,
                    isActive = true,
                    sessioniTargetSettimana = dto.sessioniTargetSettimana,
                    imageUri = null,
                    ordine = nextOrder++,
                    giorniSettimana = dto.giorniSettimana,
                    weeksCount = dto.weeksCount.coerceIn(1, 52),
                    // An imported program always starts from its first week
                    currentWeek = 1,
                    autoAdvanceWeek = dto.autoAdvanceWeek
                )
                val planId = workoutDao.insertPlan(newPlan).toInt()
                insertedPlan = newPlan.copy(id = planId)

                val imagesToInsert = mutableListOf<WorkoutPlanImageEntity>()

                // 1. From encoded blobs (restored to fresh local files)
                if (dto.imageBlobs.isNotEmpty()) {
                    dto.imageBlobs.forEachIndexed { index, blob ->
                        ImageStorageUtils.saveBase64Image(context, blob)?.let { newUri ->
                            imagesToInsert.add(WorkoutPlanImageEntity(planId = planId, imageUri = newUri, ordine = index))
                        }
                    }
                } else {
                    // 2. Legacy: raw URIs from the sender device (kept for backward
                    // compatibility, but they may not resolve on this device).
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
                    // Point the cover only at an image that actually resolves locally:
                    // prefer a blob-restored image, otherwise leave the cover empty
                    // instead of referencing a foreign URI.
                    val cover = if (dto.imageBlobs.isNotEmpty()) {
                        imagesToInsert.firstOrNull()
                    } else {
                        null
                    }
                    cover?.let { firstImage ->
                        workoutDao.updatePlan(newPlan.copy(id = planId, imageUri = firstImage.imageUri))
                    }
                }

                val exercisesToInsert = mutableListOf<Triple<PlanExerciseEntity, List<com.emanuel5014.trainable.data.remote.dto.PrescriptionBlockExportDto>, Float?>>()

                dto.exercises.forEach { exerciseDto ->
                    val finalExerciseId: Int? = when (
                        val match = ImportedExerciseMatcher.match(knownExercises, exerciseDto.exerciseId, exerciseDto.exerciseName)
                    ) {
                        is ImportedExerciseMatcher.Match.Existing -> {
                            if (match.exercise.isDeleted) {
                                exerciseDao.setExerciseDeleted(match.exercise.id, false)
                                val index = knownExercises.indexOfFirst { it.id == match.exercise.id }
                                if (index >= 0) knownExercises[index] = match.exercise.copy(isDeleted = false)
                            }
                            match.exercise.id
                        }
                        ImportedExerciseMatcher.Match.Unresolvable -> null
                        ImportedExerciseMatcher.Match.CreateCustom -> try {
                            val maxId = knownExercises.maxOfOrNull { it.id } ?: exerciseDao.getMaxId()
                            val newId = if (maxId < ImportedExerciseMatcher.FIRST_CUSTOM_ID) ImportedExerciseMatcher.FIRST_CUSTOM_ID else maxId + 1
                            val newExercise = ExerciseEntity(
                                id = newId,
                                nome = exerciseDto.exerciseName!!.trim(),
                                categoria = exerciseDto.exerciseCategory ?: "Custom"
                            )
                            exerciseDao.insertExercise(newExercise)
                            knownExercises.add(newExercise)
                            newId
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }

                    if (finalExerciseId != null) {
                        exercisesToInsert.add(
                            Triple(
                                PlanExerciseEntity(
                                    planId = planId,
                                    exerciseId = finalExerciseId,
                                    serieTarget = exerciseDto.serieTarget,
                                    repsTarget = exerciseDto.repsTarget,
                                    // A range only counts next to a plain bottom number, never next to a per-set list
                                    repsMax = exerciseDto.repsMax?.takeIf { RepRange.of(exerciseDto.repsTarget.trim().toIntOrNull(), it) != null },
                                    recuperoTarget = exerciseDto.recuperoTarget,
                                    ordine = exerciseDto.ordine,
                                    supersetId = remapSuperset(exerciseDto.supersetId),
                                    exerciseType = exerciseDto.exerciseType,
                                    durataTargetSecondi = exerciseDto.durataTargetSecondi,
                                    distanzaTargetKm = exerciseDto.distanzaTargetKm,
                                    cardioCategoria = exerciseDto.cardioCategoria,
                                    excludedWeeks = exerciseDto.excludedWeeks
                                ),
                                exerciseDto.blocks,
                                exerciseDto.oneRepMaxKg
                            )
                        )
                    }
                }

                exercisesToInsert.forEach { (entity, blocks, sharedMaximumKg) ->
                    val planExerciseId = workoutDao.insertPlanExercise(entity).toInt()
                    // Someone else's 1RM only fills a gap: it never replaces one the user already has
                    if (sharedMaximumKg != null && sharedMaximumKg > 0f && oneRepMaxDao.getCurrentOnce(entity.exerciseId) == null) {
                        oneRepMaxDao.insert(
                            OneRepMaxEntity(
                                exerciseId = entity.exerciseId,
                                weightKg = sharedMaximumKg,
                                date = System.currentTimeMillis(),
                                source = OneRepMaxEntity.SOURCE_IMPORTED
                            )
                        )
                    }
                    if (blocks.isNotEmpty()) {
                        workoutDao.insertBlocks(blocks.map { b ->
                            PlanExerciseBlockEntity(
                                planExerciseId = planExerciseId,
                                week = b.week.coerceAtLeast(1),
                                ordine = b.ordine,
                                sets = b.sets,
                                reps = b.reps,
                                repMode = b.repMode,
                                totalReps = b.totalReps,
                                intensityType = b.intensityType,
                                intensityValue = b.intensityValue,
                                techniques = b.techniques,
                                restSeconds = b.restSeconds,
                                note = b.note
                            )
                        })
                    }
                }
                imported++
            } catch (e: Exception) {
                e.printStackTrace()
                // Skip the broken plan and continue with the rest, without leaving a half-imported one behind
                insertedPlan?.let { runCatching { workoutDao.deletePlan(it) } }
            }
        }
        return imported
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
    
    private fun triggerWidgetUpdate() {
        try {
            com.emanuel5014.trainable.widget.TrainableWidget.update(context)
            com.emanuel5014.trainable.widget.WeeklyGoalWidget.update(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun startSession(planId: Int, timestamp: Long, isFinished: Boolean = false, note: String? = null): Long {
        val sessionId = workoutDao.insertSession(WorkoutSessionEntity(planId = planId, timestamp = timestamp, isFinished = isFinished, noteSessione = note))
        triggerWidgetUpdate()
        return sessionId
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

    fun getCardioSessionCountSince(sinceTimestamp: Long): Flow<Int> = workoutDao.getCardioSessionCountSince(sinceTimestamp)
    
    fun getRecentSessions(limit: Int = 10): Flow<List<WorkoutSessionEntity>> = workoutDao.getRecentSessions(limit)

    fun getUnfinishedSessions(): Flow<List<WorkoutSessionEntity>> = workoutDao.getUnfinishedSessions()

    fun getUnfinishedSessionsWithPlanName(): Flow<List<SessionWithPlanName>> = workoutDao.getUnfinishedSessionsWithPlanName()

    suspend fun setSessionFinished(sessionId: Int, durationMs: Long? = null) {
        workoutDao.setSessionFinished(sessionId)
        if (durationMs != null) {
            workoutDao.setSessionDuration(sessionId, durationMs)
        }
        triggerWidgetUpdate()
    }

    suspend fun updateRestTimer(sessionId: Int, endTime: Long?, totalSeconds: Int?) = workoutDao.updateRestTimer(sessionId, endTime, totalSeconds)

    suspend fun updateWarmupTimer(sessionId: Int, endTime: Long?, totalSeconds: Int?) = workoutDao.updateWarmupTimer(sessionId, endTime, totalSeconds)

    suspend fun updateCardioTimer(sessionId: Int, seconds: Int, running: Boolean, paused: Boolean, startedAt: Long?) = workoutDao.updateCardioTimer(sessionId, seconds, running, paused, startedAt)

    suspend fun updateSetTimer(sessionId: Int, seconds: Int, running: Boolean, paused: Boolean, startedAt: Long?) = workoutDao.updateSetTimer(sessionId, seconds, running, paused, startedAt)
    
    suspend fun logSet(set: SetLogEntity) = workoutDao.insertSet(set)
    
    suspend fun saveCardioLog(cardio: CardioLogEntity): Long = workoutDao.insertCardioLog(cardio)

    suspend fun updateCardioLog(cardio: CardioLogEntity) = workoutDao.updateCardioLog(cardio)

    fun getCardioLogsForSession(sessionId: Int): Flow<List<CardioLogEntity>> = workoutDao.getCardioLogsForSession(sessionId)

    suspend fun updateCardioSetValues(id: Int, seconds: Int, km: Float, note: String?, rpe: Float?) =
        workoutDao.updateCardioSetValues(id, seconds, km, note, rpe)

    suspend fun updateCardioRpe(id: Int, rpe: Float?) = workoutDao.updateCardioRpe(id, rpe)

    suspend fun updateCardioNote(id: Int, note: String?) = workoutDao.updateCardioNote(id, note)

    suspend fun updateCardioSetNumber(id: Int, number: Int) = workoutDao.updateCardioSetNumber(id, number)

    suspend fun saveCardioSession(categoria: String, distanza: Float, durataSecondi: Int, timestamp: Long, note: String? = null) {
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
                timestamp = timestamp,
                note = note
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
        
        val advanced = userPreferencesRepository.advancedProgrammingEnabled.first()
        val week = if (advanced) planDetails.plan.currentWeek.coerceIn(1, planDetails.plan.weeksCount.coerceAtLeast(1)) else 1
        val sessionId = workoutDao.insertSession(
            WorkoutSessionEntity(
                planId = planId,
                timestamp = timestamp,
                isFinished = true,
                programWeek = if (advanced && planDetails.plan.weeksCount > 1) week else null
            )
        )
        
        planDetails.exercises.forEach { exerciseWithDetails ->
            val planEx = exerciseWithDetails.planExercise
            val exerciseId = planEx.exerciseId
            val resolved = if (advanced) exerciseWithDetails.resolve(week) else ResolvedPrescription.Legacy
            if (resolved is ResolvedPrescription.Excluded) return@forEach
            if (resolved is ResolvedPrescription.Blocks) {
                // Advanced exercise: one row per planned set, carrying the prescription snapshot
                val prevByBlock = lastSessionSets?.filter { it.exerciseId == exerciseId && it.blockIndex != null }
                    ?.sortedBy { it.numeroSerie }?.groupBy { it.blockIndex!! }.orEmpty()
                PrescriptionExpander.expand(resolved.blocks).forEachIndexed { index, planned ->
                    val prevSet = prevByBlock[planned.blockIndex]?.getOrNull(planned.indexInBlock)
                    workoutDao.insertSet(
                        SetLogEntity(
                            sessionId = sessionId.toInt(),
                            exerciseId = exerciseId,
                            pesoSollevato = planned.fixedWeightKg ?: prevSet?.pesoSollevato ?: 0f,
                            repsEffettive = planned.targetReps ?: prevSet?.repsEffettive ?: 0,
                            numeroSerie = index + 1,
                            ordineEsercizio = planEx.ordine,
                            supersetId = planEx.supersetId,
                            targetPercent = planned.percent,
                            targetRpe = planned.targetRpe,
                            targetReps = planned.targetReps?.toString() ?: if (planned.repMode == com.emanuel5014.trainable.domain.prescription.RepMode.AMRAP) "MAX" else null,
                            repMode = planned.repMode.code,
                            blockIndex = planned.blockIndex,
                            techniques = com.emanuel5014.trainable.domain.prescription.TechniqueCodec.encode(planned.techniques),
                            targetTotalReps = planned.totalReps
                        )
                    )
                }
                return@forEach
            }
            
            // Try to find sets for this exercise in the last session
            val prevSets = lastSessionSets?.filter { it.exerciseId == exerciseId }?.sortedBy { it.numeroSerie }
            
            val numSets = if (prevSets != null && prevSets.isNotEmpty()) {
                prevSets.size
            } else if (planEx.serieTarget > 0) {
                planEx.serieTarget
            } else {
                3
            }
            
            // A routine's first workout has nothing to copy: start from the exercise's starting weight (if on)
            val startWeight = if (prevSets.isNullOrEmpty()) startingWeightFor(planEx, exerciseId) else 0f
            // Planned reps per set: a single number for every set, a range starts at its bottom, "8-6-4-2" goes set by set
            val plannedReps = if (planEx.repsTarget.split("-").any { it.trim().toIntOrNull() != null }) {
                LegacyReps.parse(planEx.repsTarget, numSets)
            } else emptyList()
            // Timed exercises keep their kind: the hold or interval of the routine (a hold copies last time's)
            val kind = SetLogKind.ofExerciseType(planEx.exerciseType)
            val planSeconds = planEx.durataTargetSecondi ?: 45
            for (i in 1..numSets) {
                val prevSet = prevSets?.getOrNull(i - 1)
                val seconds = when (kind) {
                    SetLogKind.TIME_AND_WEIGHT -> prevSet?.durataSecondi ?: planSeconds
                    SetLogKind.TIME_AND_REPS -> planSeconds
                    SetLogKind.STRENGTH -> null
                }
                workoutDao.insertSet(
                    SetLogEntity(
                        sessionId = sessionId.toInt(),
                        exerciseId = exerciseId,
                        pesoSollevato = if (kind == SetLogKind.TIME_AND_REPS) 0f else prevSet?.pesoSollevato ?: startWeight,
                        repsEffettive = if (kind == SetLogKind.TIME_AND_WEIGHT) 0 else prevSet?.repsEffettive ?: plannedReps.getOrNull(i - 1) ?: 10,
                        numeroSerie = i,
                        ordineEsercizio = planEx.ordine,
                        supersetId = planEx.supersetId,
                        durataSecondi = seconds,
                        exerciseType = kind.rowType
                    ).withRepsRange(planEx.repsRange.takeIf { planEx.exerciseType == "strength" })
                )
            }
        }
        triggerWidgetUpdate()
        return sessionId
    }

    suspend fun updateSet(set: SetLogEntity) = workoutDao.updateSet(set)

    suspend fun deleteSet(set: SetLogEntity) = workoutDao.deleteSet(set)

    suspend fun deleteCardioLog(cardio: CardioLogEntity) = workoutDao.deleteCardioLog(cardio)
    
    fun getPreviousSetsForExercise(exerciseId: Int): Flow<List<SetLogEntity>> = workoutDao.getPreviousSetsForExercise(exerciseId)

    fun getLastFinishedSetsForExercise(exerciseId: Int): Flow<List<SetLogEntity>> = workoutDao.getLastFinishedSetsForExercise(exerciseId)

    /** Every completed set of the exercise in a finished workout, newest first (see [WorkoutDao.getSetHistoryForExercise]). */
    fun getSetHistoryForExercise(exerciseId: Int): Flow<List<ExerciseSetWithSession>> = workoutDao.getSetHistoryForExercise(exerciseId)

    /** Weight in kg the exercise was last lifted with in any routine, null if never. */
    suspend fun getLastWorkingWeightForExercise(exerciseId: Int): Float? = workoutDao.getLastWorkingWeightForExercise(exerciseId)

    /**
     * The weight (kg) the sets of an exercise start from when its routine has no workout to copy them from:
     * the one set on the exercise, else the last one used for it in any routine. 0 with starting weights off.
     */
    suspend fun startingWeightFor(planExercise: PlanExerciseEntity?, exerciseId: Int): Float {
        if (!userPreferencesRepository.startingWeightEnabled.first()) return 0f
        val preset = planExercise?.weightPreset
        val lastUsed = if (preset != null && preset > 0f) null else workoutDao.getLastWorkingWeightForExercise(exerciseId)
        return StartingWeight.resolve(enabled = true, presetKg = preset, lastUsedKg = lastUsed)
    }

    fun getLastSessionSetsForExercise(planId: Int, exerciseId: Int, limitSets: Int): Flow<List<SetLogEntity>> = 
        workoutDao.getLastSessionSetsForExercise(planId, exerciseId, limitSets)

    suspend fun deleteSession(sessionId: Int) {
        workoutDao.deleteSession(sessionId)
        triggerWidgetUpdate()
    }

    suspend fun updateSession(session: WorkoutSessionEntity) = workoutDao.updateSession(session)

    suspend fun deleteExerciseFromSession(sessionId: Int, exerciseId: Int) = 
        workoutDao.deleteExerciseFromSession(sessionId, exerciseId)

    suspend fun updateSetOrders(sets: List<SetLogEntity>) = workoutDao.updateSetOrders(sets)

    suspend fun updateExerciseOrderInSession(sessionId: Int, exerciseId: Int, order: Int) = workoutDao.updateExerciseOrderInSession(sessionId, exerciseId, order)

    suspend fun moveExerciseRows(sessionId: Int, exerciseId: Int, from: Int, to: Int) =
        workoutDao.moveExerciseRows(sessionId, exerciseId, from, to)

    suspend fun moveCardioRows(sessionId: Int, categoria: String, from: Int, to: Int) =
        workoutDao.moveCardioRows(sessionId, categoria, from, to)

    suspend fun deleteExerciseRows(sessionId: Int, exerciseId: Int, order: Int) =
        workoutDao.deleteExerciseRows(sessionId, exerciseId, order)

    suspend fun deleteCardioRows(sessionId: Int, categoria: String, order: Int) =
        workoutDao.deleteCardioRows(sessionId, categoria, order)

    suspend fun deleteUncompletedSetsForSession(sessionId: Int) = workoutDao.deleteUncompletedSetsForSession(sessionId)

    suspend fun deleteUncompletedCardioLogsForSession(sessionId: Int) = workoutDao.deleteUncompletedCardioLogsForSession(sessionId)

    suspend fun saveExerciseSwap(swap: SessionExerciseSwapEntity) = workoutDao.insertExerciseSwap(swap)

    fun getSwapsForSession(sessionId: Int): Flow<List<SessionExerciseSwapEntity>> = workoutDao.getSwapsForSession(sessionId)

    suspend fun removeExerciseSwap(sessionId: Int, originalExerciseId: Int) = workoutDao.removeSwapForExercise(sessionId, originalExerciseId)

    suspend fun getFinishedSessionsWithDetailsForPlans(planIds: List<Int>) =
        workoutDao.getFinishedSessionsWithDetailsForPlans(planIds)

    suspend fun getSwapsForSessions(sessionIds: List<Int>) =
        workoutDao.getSwapsForSessions(sessionIds)

    suspend fun exportAllWorkoutsToCsv(weightUnit: String = "kg", languageCode: String = "en"): String {
        val sessions = workoutDao.getAllSessionsWithDetails().first()
        // With advanced programming off the file keeps the classic columns
        val advanced = userPreferencesRepository.advancedProgrammingEnabled.first()
        // The workout journal adds its three columns only while it is on
        val journalEnabled = userPreferencesRepository.journalEnabled.first()
        // Without advanced programming the file only gets an RPE column when there is an RPE to write
        val rpeColumn = !advanced && sessions.any { s -> s.sets.any { it.setLog.rpe != null } || s.cardio.any { it.rpe != null } }

        val sb = StringBuilder()
        sb.appendLine(HistoryCsvHeaders.full(languageCode, weightUnit, advanced, journalEnabled, rpeColumn))

        fun escapeCsv(value: String): String {
            var v = value.replace("\r", " ").replace("\n", " ")
            if (v.contains(",") || v.contains("\"")) {
                v = "\"" + v.replace("\"", "\"\"") + "\""
            }
            return v
        }

        sessions.forEach { session ->
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(session.session.timestamp))
            val planName = escapeCsv(session.plan.nome)
            // How long the whole workout took, repeated on each of its rows (empty when it was never recorded)
            val sessionMinutes = SessionDuration.format(session.session.durationMs)
            // Its rating, tags and note, repeated on each row like the duration (nothing at all with the journal off)
            val journalCells = if (journalEnabled) {
                JournalCsv.cells(session.session.rating, session.tags.map { it.name }.sorted(), session.session.journalNote)
            } else ""

            session.sets.forEach { setWithExercise ->
                val setLog = setWithExercise.setLog
                val exercise = setWithExercise.exercise
                // Whether the prescription of the exercise was changed in this workout (and whether the routine was updated
                // by any of the changes), and what it was at the start
                val edits = session.prescriptionEdits.forExercise(exercise.id)
                val programEditColumns = if (edits.isEmpty()) ",," else
                    ",${(if (edits.any { it.editScope == com.emanuel5014.trainable.domain.prescription.ProgramEditScope.PLAN }) com.emanuel5014.trainable.domain.prescription.ProgramEditScope.PLAN else com.emanuel5014.trainable.domain.prescription.ProgramEditScope.WORKOUT).code},${escapeCsv(com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter.notation(edits.first().before))}"
                val exerciseName = escapeCsv(ExerciseTranslations.translate(exercise.nome, languageCode))
                val category = escapeCsv(ExerciseTranslations.translateCategory(exercise.categoria, languageCode))
                val note = escapeCsv(setLog.note ?: "")
                val weight = WeightUnitConverter.convertDisplay(setLog.pesoSollevato, weightUnit)

                val kind = setLog.kind
                val type = when (kind) {
                    SetLogKind.STRENGTH -> "strength"
                    SetLogKind.TIME_AND_WEIGHT -> TIME_AND_WEIGHT
                    SetLogKind.TIME_AND_REPS -> TIME_AND_REPS
                }
                val reps = if (kind == SetLogKind.TIME_AND_WEIGHT) "" else setLog.repsEffettive.toString()
                val duration = setLog.durataSecondi?.toString() ?: ""

                val week = session.session.programWeek?.toString() ?: ""
                val targetPercent = setLog.targetPercent?.let { com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter.number(it) } ?: ""
                val targetReps = when (setLog.repMode) {
                    "total" -> setLog.targetTotalReps?.let { "$it ALSAP" } ?: ""
                    else -> setLog.targetReps ?: ""
                }
                val techniques = escapeCsv(
                    com.emanuel5014.trainable.domain.prescription.TechniqueCodec.decode(setLog.techniques)
                        .joinToString(" | ") { com.emanuel5014.trainable.ui.components.techniqueLabel(context, it) }
                )
                val rpe = setLog.rpe?.let { com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter.number(it) } ?: ""
                val extra = if (setLog.isExtra) "1" else "0"
                val warmup = if (setLog.isWarmup) "1" else "0"

                val prescriptionColumns = when {
                    advanced -> ",$week,$targetPercent,$targetReps,$techniques,$rpe,$extra,$warmup"
                    rpeColumn -> ",$rpe"
                    else -> ""
                }
                // How the set really went: its parts and pauses, help, partials, the technique as done
                val performed = setLog.performed?.let { list ->
                    if (list.isEmpty()) context.getString(com.emanuel5014.trainable.R.string.performed_technique_none)
                    else list.joinToString(" | ") { com.emanuel5014.trainable.ui.components.techniqueLabel(context, it) }
                }.orEmpty()
                val performanceColumns = ",${setLog.parts.joinToString("+")},${setLog.pauses.joinToString("/")},${setLog.assistedReps},${setLog.partialReps},${escapeCsv(performed)}"
                sb.appendLine("$date,${session.session.id},$planName,$exerciseName,$category,$type,${setLog.numeroSerie},$weight,$reps,$duration,,$note$prescriptionColumns$performanceColumns$programEditColumns,$sessionMinutes$journalCells")
            }

            session.cardio.forEach { cardio ->
                val exerciseName = escapeCsv(ExerciseTranslations.translate(cardio.categoria, languageCode))
                val cardioCategory = escapeCsv(ExerciseTranslations.translateCategory("Cardio", languageCode))
                val week = session.session.programWeek?.toString() ?: ""
                val cardioRpe = cardio.rpe?.let { com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter.number(it) } ?: ""
                val prescriptionColumns = when {
                    advanced -> ",$week,,,,$cardioRpe,0,0"
                    rpeColumn -> ",$cardioRpe"
                    else -> ""
                }
                val note = escapeCsv(cardio.note ?: "")
                sb.appendLine("$date,${session.session.id},$planName,$exerciseName,$cardioCategory,cardio,${cardio.numeroSerie},,,${cardio.durataSecondi},${cardio.distanza},$note$prescriptionColumns,,,0,0,,,,$sessionMinutes$journalCells")
            }
        }

        return sb.toString()
    }
}
