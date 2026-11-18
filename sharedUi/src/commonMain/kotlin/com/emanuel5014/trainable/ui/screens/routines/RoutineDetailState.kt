package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.domain.prescription.PrescriptionResolver
import com.emanuel5014.trainable.domain.prescription.ResolvedPrescription
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class RoutineDetailUiState(
    val isLoading: Boolean = true,
    val planDetails: PlanWithDetails? = null,
    val availableExercises: List<ExerciseEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val error: String? = null,
    val estimatedDurationMinutes: Int = 0,
    val estimatedVolumeKg: Float = 0f,
    val unfinishedSessions: List<SessionWithPlanName> = emptyList()
)

/**
 * The routine detail's state and edits, the same on every platform: each one gives it the routine ([planId]) and its
 * own lifetime ([scope]). Scanning a routine sheet with AI is Android's own and lives in its ViewModel.
 */
@OptIn(ExperimentalUuidApi::class)
class RoutineDetailState(
    private val planId: Int,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val oneRepMaxRepository: OneRepMaxRepository,
    private val scope: CoroutineScope
) {
    companion object {
        const val MAX_WEEKS = 52
    }

    private val _uiState = MutableStateFlow(RoutineDetailUiState(isLoading = true))
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()

    val weightUnit = userPreferencesRepository.weightUnit.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "kg"
    )

    /** Whether the top of the screen is open or compact; one choice for every routine. */
    val headerExpanded = userPreferencesRepository.routineHeaderExpanded.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setHeaderExpanded(expanded: Boolean) {
        scope.launch { userPreferencesRepository.setRoutineHeaderExpanded(expanded) }
    }

    /** With starting weights on, the exercise editor offers a weight field. */
    val startingWeightEnabled = userPreferencesRepository.startingWeightEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /** The type each exercise was last added with: adding it again opens the selector on it. */
    val exerciseTypes = userPreferencesRepository.exerciseTypes.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )

    val loadRoundingIncrement = userPreferencesRepository.loadRoundingIncrement.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LoadCalculator.DEFAULT_INCREMENT_KG
    )

    /** exerciseId → current 1RM (kg). */
    val oneRepMaxes = oneRepMaxRepository.currentByExercise().map { map ->
        map.mapValues { it.value.weightKg }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    /** exerciseId → Epley estimate from recent logs (kg). */
    val estimatedOneRepMaxes = oneRepMaxRepository.estimatedByExercise().stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun saveOneRepMax(exerciseId: Int, weightKg: Float, source: String) {
        scope.launch {
            oneRepMaxRepository.add(exerciseId = exerciseId, weightKg = weightKg, source = source)
        }
    }


    /** The app's light or dark choice: 0 follows the system, 1 light, 2 dark. */
    val themeMode = userPreferencesRepository.themeMode.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    init {
        loadPlanDetails()
        loadExercisesCatalog()
        scope.launch {
            workoutRepository.getUnfinishedSessionsWithPlanName().collect { sessions ->
                _uiState.value = _uiState.value.copy(unfinishedSessions = sessions)
            }
        }
    }

    private fun loadPlanDetails() {
        scope.launch {
            try {
                workoutRepository.getPlanWithDetails(planId).collect { details ->
                    if (details != null) {
                        // Auto-migrate old single image if multiple images list is empty
                        if (details.images.isEmpty() && details.plan.imageUri != null) {
                            val oldUri = details.plan.imageUri!!
                            addPlanImage(oldUri)
                            // Clear the old URI to avoid re-migration
                            workoutRepository.updatePlan(details.plan.copy(imageUri = null))
                        }

                        // Sort exercises by 'ordine'
                        val sortedExercises = details.exercises.sortedBy { it.planExercise.ordine }
                        val sortedDetails = details.copy(exercises = sortedExercises)
                        
                        val duration = calculateEstimatedDuration(sortedDetails)
                        val volume = calculateEstimatedVolume(sortedDetails)
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            planDetails = sortedDetails,
                            estimatedDurationMinutes = duration,
                            estimatedVolumeKg = volume,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Errore sconosciuto"
                )
            }
        }
    }

    private fun calculateEstimatedDuration(details: PlanWithDetails): Int {
        var totalSeconds = 0
        details.exercises.forEach { item ->
            val sets = item.planExercise.serieTarget
            val rest = item.planExercise.recuperoTarget
            // Average of 45s per set + rest interval between sets
            totalSeconds += (sets * 45) + (sets * rest)
        }
        return totalSeconds / 60
    }

    private fun calculateEstimatedVolume(details: PlanWithDetails): Float {
        // Here we mock a weight of 60kg if we don't have PR data yet
        // and take the min of the reps range (e.g. '8-12' -> 8)
        var totalVol = 0f
        details.exercises.forEach { item ->
            val sets = item.planExercise.serieTarget
            val reps = item.planExercise.repsTarget.takeWhile { it.isDigit() }.toIntOrNull() ?: 8
            totalVol += sets * reps * 60f 
        }
        return totalVol
    }

    private fun loadExercisesCatalog() {
        scope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _uiState.value = _uiState.value.copy(availableExercises = exercises)
            }
        }
        scope.launch {
            exerciseRepository.getCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
    }

    fun addCustomExercise(nome: String, categoria: String, onCreated: (ExerciseEntity) -> Unit = {}) {
        scope.launch {
            val newId = exerciseRepository.addCustomExercise(nome, categoria)
            onCreated(ExerciseEntity(id = newId, nome = nome, categoria = categoria))
        }
    }

    /** Adds an exercise the user deleted from the exercise list back, for a routine that still uses it. */
    fun restoreExercise(exerciseId: Int) {
        scope.launch { exerciseRepository.restoreExercise(exerciseId) }
    }

    fun addExercise(
        exerciseId: Int,
        serieTarget: Int,
        repsTarget: String,
        recuperoTarget: Int,
        exerciseType: String = "strength",
        durataTargetSecondi: Int? = null,
        excludedWeeks: Set<Int> = emptySet(),
        /** Top of a rep range; [repsTarget] is then its bottom. */
        repsMax: Int? = null,
        /** Weight in kg the sets start from until the routine has a workout to copy it from. */
        weightPreset: Float? = null
    ) {
        scope.launch {
            val current = _uiState.value.planDetails ?: return@launch
            val nextOrder = (current.exercises.maxOfOrNull { it.planExercise.ordine } ?: -1) + 1

            workoutRepository.savePlanExercise(
                PlanExerciseEntity(
                    planId = current.plan.id,
                    exerciseId = exerciseId,
                    serieTarget = serieTarget,
                    repsTarget = repsTarget,
                    repsMax = repsMax,
                    recuperoTarget = recuperoTarget,
                    ordine = nextOrder,
                    exerciseType = exerciseType,
                    durataTargetSecondi = durataTargetSecondi,
                    excludedWeeks = com.emanuel5014.trainable.domain.prescription.WeekSetCodec.encode(excludedWeeks),
                    weightPreset = weightPreset
                )
            )
            userPreferencesRepository.rememberExerciseType(exerciseId, exerciseType)
        }
    }

    /**
     * Creates or updates an advanced (%1RM / blocks) exercise. Legacy sets/reps are derived from
     * the plan's current week so every older consumer still shows a sensible summary.
     */
    fun saveAdvancedExercise(
        original: PlanExerciseEntity?,
        exerciseId: Int,
        recuperoTarget: Int,
        blocksByWeek: Map<Int, List<PrescriptionBlock>>,
        excludedWeeks: Set<Int>,
        weeksCount: Int
    ) {
        scope.launch {
            val current = _uiState.value.planDetails ?: return@launch
            val defined = blocksByWeek.filterValues { it.isNotEmpty() }
            if (defined.isEmpty()) return@launch
            val summaryBlocks = when (val resolved = PrescriptionResolver.resolve(defined, emptySet(), current.plan.currentWeek)) {
                is ResolvedPrescription.Blocks -> resolved.blocks
                else -> defined.values.first()
            }
            val (sets, reps) = PrescriptionExpander.legacyTargets(summaryBlocks)
            val encodedExcluded = com.emanuel5014.trainable.domain.prescription.WeekSetCodec.encode(excludedWeeks)

            val planExerciseId = if (original == null) {
                val nextOrder = (current.exercises.maxOfOrNull { it.planExercise.ordine } ?: -1) + 1
                workoutRepository.savePlanExercise(
                    PlanExerciseEntity(
                        planId = current.plan.id,
                        exerciseId = exerciseId,
                        serieTarget = sets,
                        repsTarget = reps,
                        recuperoTarget = recuperoTarget,
                        ordine = nextOrder,
                        exerciseType = "strength",
                        excludedWeeks = encodedExcluded
                    )
                ).toInt()
            } else {
                workoutRepository.updatePlanExercise(
                    original.copy(
                        exerciseId = exerciseId,
                        serieTarget = sets,
                        repsTarget = reps,
                        repsMax = null,
                        recuperoTarget = recuperoTarget,
                        exerciseType = "strength",
                        durataTargetSecondi = null,
                        excludedWeeks = encodedExcluded
                    )
                )
                original.id
            }
            workoutRepository.savePlanExerciseBlocks(planExerciseId, defined)

            if (weeksCount > current.plan.weeksCount) {
                workoutRepository.updatePlan(current.plan.copy(weeksCount = weeksCount))
            }
        }
    }

    /** Deletes [week] from the whole routine; every later week moves up. */
    fun deleteWeek(week: Int) {
        scope.launch {
            val plan = _uiState.value.planDetails?.plan ?: return@launch
            workoutRepository.deletePlanWeek(plan, week)
        }
    }

    fun setCurrentWeek(week: Int) {
        scope.launch {
            val plan = _uiState.value.planDetails?.plan ?: return@launch
            workoutRepository.setPlanCurrentWeek(plan.id, week.coerceIn(1, plan.weeksCount))
        }
    }

    fun addCardioExercise(
        exerciseId: Int,
        cardioCategoria: String?,
        durataTargetSecondi: Int?,
        distanzaTargetKm: Float? = null,
        recuperoTarget: Int = 0,
        sets: Int = 1
    ) {
        scope.launch {
            val current = _uiState.value.planDetails ?: return@launch
            val nextOrder = (current.exercises.maxOfOrNull { it.planExercise.ordine } ?: -1) + 1

            workoutRepository.savePlanExercise(
                PlanExerciseEntity(
                    planId = current.plan.id,
                    exerciseId = exerciseId,
                    serieTarget = sets.coerceAtLeast(1),
                    repsTarget = "1",
                    recuperoTarget = recuperoTarget,
                    ordine = nextOrder,
                    exerciseType = "cardio",
                    cardioCategoria = cardioCategoria,
                    durataTargetSecondi = durataTargetSecondi,
                    distanzaTargetKm = distanzaTargetKm
                )
            )
        }
    }

    fun updateExercise(
        original: PlanExerciseEntity,
        exerciseId: Int,
        serieTarget: Int,
        repsTarget: String,
        recuperoTarget: Int,
        exerciseType: String = original.exerciseType,
        durataTargetSecondi: Int? = original.durataTargetSecondi,
        excludedWeeks: Set<Int>? = null,
        clearAdvanced: Boolean = false,
        /** Top of a rep range; [repsTarget] is then its bottom. Anything but a plain strength exercise leaves it null. */
        repsMax: Int? = null,
        /** Weight in kg the sets start from; unchanged unless passed. */
        weightPreset: Float? = original.weightPreset
    ) {
        scope.launch {
            workoutRepository.updatePlanExercise(
                original.copy(
                    exerciseId = exerciseId,
                    serieTarget = serieTarget,
                    repsTarget = repsTarget,
                    repsMax = repsMax,
                    weightPreset = weightPreset,
                    recuperoTarget = recuperoTarget,
                    exerciseType = exerciseType,
                    durataTargetSecondi = durataTargetSecondi,
                    excludedWeeks = excludedWeeks?.let { com.emanuel5014.trainable.domain.prescription.WeekSetCodec.encode(it) }
                        ?: original.excludedWeeks
                )
            )
            if (clearAdvanced) workoutRepository.savePlanExerciseBlocks(original.id, emptyMap())
            // Only a change of type or of exercise is a choice: saving another edit must not undo what was chosen before
            if (exerciseType != original.exerciseType || exerciseId != original.exerciseId) {
                userPreferencesRepository.rememberExerciseType(exerciseId, exerciseType)
            }
        }
    }

    fun removeExercise(planExercise: PlanExerciseEntity) {
        scope.launch {
            // Delete first
            workoutRepository.deletePlanExercise(planExercise)
            
            // Immediately re-calculate order from current state but excluding the deleted one
            // to avoid waiting for the Flow to emit a new state which might be too slow
            val currentDetails = _uiState.value.planDetails ?: return@launch
            val remaining = currentDetails.exercises
                .filter { it.planExercise.id != planExercise.id }
                .sortedBy { it.planExercise.ordine }
            
            val updates = remaining.mapIndexed { index, item ->
                item.planExercise.copy(ordine = index)
            }
            workoutRepository.savePlanExercises(updates)
        }
    }

    private fun renormalizeOrder() {
        scope.launch {
            val details = _uiState.value.planDetails ?: return@launch
            val sorted = details.exercises.sortedBy { it.planExercise.ordine }
            val updates = sorted.mapIndexed { index, item ->
                item.planExercise.copy(ordine = index)
            }
            workoutRepository.savePlanExercises(updates)
        }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        scope.launch {
            val details = _uiState.value.planDetails ?: return@launch
            val exercises = details.exercises.toMutableList()
            if (fromIndex in exercises.indices && toIndex in exercises.indices) {
                val item = exercises.removeAt(fromIndex)
                exercises.add(toIndex, item)
                
                val updates = exercises.mapIndexed { index, it ->
                    it.planExercise.copy(ordine = index)
                }
                workoutRepository.savePlanExercises(updates)
            }
        }
    }

    fun deletePlan(onSuccess: () -> Unit) {
        scope.launch {
            uiState.value.planDetails?.plan?.let { plan ->
                workoutRepository.deletePlan(plan)
                onSuccess()
            }
        }
    }

    fun addPlanImage(imageUri: String) {
        scope.launch {
            val current = uiState.value.planDetails ?: return@launch
            val nextOrder = (current.images.maxOfOrNull { it.ordine } ?: -1) + 1
            workoutRepository.savePlanImage(
                WorkoutPlanImageEntity(
                    planId = current.plan.id,
                    imageUri = imageUri,
                    ordine = nextOrder
                )
            )
        }
    }

    fun removePlanImage(image: WorkoutPlanImageEntity) {
        scope.launch {
            workoutRepository.deletePlanImage(image)
        }
    }

    fun resetRoutineContent() {
        scope.launch {
            val planId = _uiState.value.planDetails?.plan?.id ?: return@launch
            workoutRepository.resetPlanContent(planId)
        }
    }

    fun updatePlan(
        nome: String,
        note: String?,
        giorniSettimana: String? = null,
        dataInizio: Long? = null,
        dataFine: Long? = null,
        weeksCount: Int? = null,
        currentWeek: Int? = null,
        autoAdvanceWeek: Boolean? = null
    ) {
        scope.launch {
            uiState.value.planDetails?.plan?.let { plan ->
                val newWeeks = (weeksCount ?: plan.weeksCount).coerceIn(1, MAX_WEEKS)
                workoutRepository.updatePlan(
                    plan.copy(
                        nome = nome,
                        note = note,
                        giorniSettimana = giorniSettimana,
                        dataInizio = dataInizio ?: plan.dataInizio,
                        dataFine = dataFine,
                        weeksCount = newWeeks,
                        currentWeek = (currentWeek ?: plan.currentWeek).coerceIn(1, newWeeks),
                        autoAdvanceWeek = autoAdvanceWeek ?: plan.autoAdvanceWeek
                    )
                )
                if (newWeeks < plan.weeksCount) workoutRepository.trimPlanWeeks(plan.id, newWeeks)
            }
        }
    }

    fun updateExercisesOrder(orderedExercises: List<PlanExerciseEntity>) {
        scope.launch {
            val updates = orderedExercises.mapIndexed { index, it ->
                it.copy(ordine = index)
            }
            workoutRepository.savePlanExercises(updates)
        }
    }

    fun toggleSupersetWithNext(planExercise: PlanExerciseEntity, customSupersetId: String? = null) {
        scope.launch {
            val details = _uiState.value.planDetails ?: return@launch
            val exercises = details.exercises.sortedBy { it.planExercise.ordine }
            val index = exercises.indexOfFirst { it.planExercise.id == planExercise.id }

            if (index != -1 && index < exercises.size - 1) {
                val current = exercises[index].planExercise
                val next = exercises[index + 1].planExercise

                if (current.supersetId != null && current.supersetId == next.supersetId) {
                    // Break link
                    workoutRepository.savePlanExercises(listOf(
                        current.copy(supersetId = null),
                        next.copy(supersetId = null)
                    ))
                } else {
                    // Create link
                    val sid = current.supersetId ?: next.supersetId ?: customSupersetId ?: Uuid.random().toString()
                    workoutRepository.savePlanExercises(listOf(
                        current.copy(supersetId = sid),
                        next.copy(supersetId = sid)
                    ))
                }
            }
        }
    }
}
