package com.emanuel5014.trainable.ui.screens.routines

import androidx.lifecycle.SavedStateHandle
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.ai.AiModelVariant
import com.emanuel5014.trainable.data.ai.AiResourceTracker
import com.emanuel5014.trainable.data.ai.DeviceCapabilityChecker
import com.emanuel5014.trainable.data.ai.DeviceResourceMetrics
import com.emanuel5014.trainable.data.ai.ModelFileManager
import com.emanuel5014.trainable.data.ai.ScanPhase
import com.emanuel5014.trainable.data.ai.ScannedExerciseEntry
import com.emanuel5014.trainable.data.ai.RoutineScanner
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

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

sealed interface AiScanState {
    data object Idle : AiScanState
    data class Scanning(
        val phase: ScanPhase = ScanPhase.LOADING_MODEL
    ) : AiScanState
    data class Success(
        val entries: List<ScannedExerciseEntry>,
        val imageUri: Uri? = null
    ) : AiScanState
    data class Error(val message: String?) : AiScanState
}

data class AiScanStreamState(
    val output: String = "",
    val thinking: String = "",
    val metrics: DeviceResourceMetrics? = null
)

@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager,
    private val routineScanner: RoutineScanner,
    private val modelFileManager: ModelFileManager,
    private val deviceCapabilityChecker: DeviceCapabilityChecker,
    private val aiResourceTracker: AiResourceTracker,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val planId: Int = checkNotNull(savedStateHandle["planId"])

    private companion object {
        const val STREAM_EMIT_INTERVAL_MS = 250L
        const val STREAM_MAX_CHARS = 4000
    }

    private val _uiState = MutableStateFlow(RoutineDetailUiState(isLoading = true))
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    val editablePresetExercises = userPreferencesRepository.editablePresetExercises.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val aiScanAvailable = combine(
        userPreferencesRepository.aiScanEnabled,
        userPreferencesRepository.aiModelVariant,
        modelFileManager.filesUpdatedTrigger
    ) { enabled, variantId, _ ->
        enabled && modelFileManager.isDownloaded(AiModelVariant.fromId(variantId))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val aiResourceAnalyticsEnabled = userPreferencesRepository.aiResourceAnalyticsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _aiScanState = MutableStateFlow<AiScanState>(AiScanState.Idle)
    val aiScanState: StateFlow<AiScanState> = _aiScanState.asStateFlow()

    private val _aiScanStream = MutableStateFlow(AiScanStreamState())
    val aiScanStream: StateFlow<AiScanStreamState> = _aiScanStream.asStateFlow()

    private var scanJob: kotlinx.coroutines.Job? = null

    fun scanRoutineSheet(imageUri: Uri) {
        if (_aiScanState.value is AiScanState.Scanning) return
        _aiScanState.value = AiScanState.Scanning()
        _aiScanStream.value = AiScanStreamState()
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val isAnalyticsEnabled = userPreferencesRepository.aiResourceAnalyticsEnabled.first()

                // Throttle stream state emissions to ~4 Hz so token-frequency
                // updates don't trigger full-screen recomposition + blur recompute
                var lastEmitAt = 0L
                var latestOutput = ""
                var latestThinking = ""
                fun emit(force: Boolean = false) {
                    val now = System.currentTimeMillis()
                    if (force || now - lastEmitAt >= STREAM_EMIT_INTERVAL_MS) {
                        lastEmitAt = now
                        val elapsedSec = ((now - startTime) / 1000L).toInt()
                        val metrics = if (isAnalyticsEnabled) {
                            aiResourceTracker.captureMetrics(
                                charsGenerated = latestOutput.length + latestThinking.length,
                                elapsedSeconds = elapsedSec
                            )
                        } else null
                        _aiScanStream.value = AiScanStreamState(
                            output = latestOutput.takeLast(STREAM_MAX_CHARS),
                            thinking = latestThinking.takeLast(STREAM_MAX_CHARS),
                            metrics = metrics
                        )
                    }
                }

                // Initial metrics capture
                if (isAnalyticsEnabled) {
                    _aiScanStream.value = _aiScanStream.value.copy(
                        metrics = aiResourceTracker.captureMetrics(
                            charsGenerated = 0,
                            elapsedSeconds = 0
                        )
                    )
                }

                // Live background metrics ticker during pre-generation phases
                val metricsJob = if (isAnalyticsEnabled) {
                    launch {
                        while (isActive) {
                            delay(500)
                            val now = System.currentTimeMillis()
                            val elapsedSec = ((now - startTime) / 1000L).toInt()
                            val metrics = aiResourceTracker.captureMetrics(
                                charsGenerated = latestOutput.length + latestThinking.length,
                                elapsedSeconds = elapsedSec
                            )
                            _aiScanStream.value = _aiScanStream.value.copy(metrics = metrics)
                        }
                    }
                } else null

                val entries = routineScanner.scan(
                    imageUri = imageUri,
                    catalog = _uiState.value.availableExercises,
                    categories = _uiState.value.categories,
                    languageCode = localeManager.getResolvedLanguage(),
                    onPhase = { phase ->
                        val current = _aiScanState.value
                        if (current is AiScanState.Scanning) {
                            _aiScanState.value = current.copy(phase = phase)
                        }
                    },
                    onStreamUpdate = { partialOutput, thinking ->
                        latestOutput = partialOutput
                        latestThinking = thinking
                        emit()
                    }
                )
                metricsJob?.cancel()
                emit(force = true)
                _aiScanState.value =
                    if (entries.isEmpty()) AiScanState.Error(null)
                    else AiScanState.Success(entries = entries, imageUri = imageUri)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancelled by user
                _aiScanState.value = AiScanState.Idle
                _aiScanStream.value = AiScanStreamState()
            } catch (e: Exception) {
                e.printStackTrace()
                _aiScanState.value = AiScanState.Error(e.message)
            }
        }
    }

    fun cancelAiScan() {
        scanJob?.cancel()
        scanJob = null
        _aiScanState.value = AiScanState.Idle
        _aiScanStream.value = AiScanStreamState()
        viewModelScope.launch(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.IO) {
            routineScanner.release()
        }
    }

    fun dismissScanResult() {
        _aiScanState.value = AiScanState.Idle
        _aiScanStream.value = AiScanStreamState()
        viewModelScope.launch(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.IO) {
            routineScanner.release()
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        scanJob = null
        viewModelScope.launch(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.IO) {
            routineScanner.release()
        }
    }

    fun applyScannedExercises(entries: List<ScannedExerciseEntry>) {
        viewModelScope.launch {
            val details = _uiState.value.planDetails ?: return@launch
            var nextOrder = (details.exercises.maxOfOrNull { it.planExercise.ordine } ?: -1) + 1

            entries.forEach { entry ->
                val exerciseId = entry.exerciseId ?: exerciseRepository.addCustomExercise(
                    nome = entry.rawName,
                    categoria = entry.suggestedCategory.ifBlank { "Custom" }
                )

                workoutRepository.savePlanExercise(
                    if (entry.isCardio) {
                        PlanExerciseEntity(
                            planId = details.plan.id,
                            exerciseId = exerciseId,
                            serieTarget = 1,
                            repsTarget = "1",
                            recuperoTarget = entry.restSeconds,
                            ordine = nextOrder++,
                            exerciseType = "cardio",
                            durataTargetSecondi = entry.cardioMinutes?.let { it * 60 }
                        )
                    } else if (entry.isTimeAndWeight) {
                        val sec = entry.timeSeconds ?: entry.reps.filter { it.isDigit() }.toIntOrNull() ?: 45
                        PlanExerciseEntity(
                            planId = details.plan.id,
                            exerciseId = exerciseId,
                            serieTarget = entry.sets,
                            repsTarget = "${sec}s",
                            recuperoTarget = entry.restSeconds,
                            ordine = nextOrder++,
                            exerciseType = "time_and_weight",
                            durataTargetSecondi = sec
                        )
                    } else {
                        PlanExerciseEntity(
                            planId = details.plan.id,
                            exerciseId = exerciseId,
                            serieTarget = entry.sets,
                            repsTarget = entry.reps,
                            recuperoTarget = entry.restSeconds,
                            ordine = nextOrder++,
                            exerciseType = "strength"
                        )
                    }
                )
            }

            _aiScanState.value = AiScanState.Idle
            _aiScanStream.value = AiScanStreamState()
        }
    }

    init {
        viewModelScope.launch {
            localeManager.userSelectedLanguage.collect { _ ->
                _languageCode.value = localeManager.getResolvedLanguage()
            }
        }
        loadPlanDetails()
        loadExercisesCatalog()
        viewModelScope.launch {
            workoutRepository.getUnfinishedSessionsWithPlanName().collect { sessions ->
                _uiState.value = _uiState.value.copy(unfinishedSessions = sessions)
            }
        }
    }

    private fun loadPlanDetails() {
        viewModelScope.launch {
            try {
                workoutRepository.getPlanWithDetails(planId).collect { details ->
                    if (details != null) {
                        // Auto-migrate old single image if multiple images list is empty
                        if (details.images.isEmpty() && details.plan.imageUri != null) {
                            val oldUri = details.plan.imageUri
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
                    error = e.localizedMessage ?: "Errore sconosciuto"
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
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _uiState.value = _uiState.value.copy(availableExercises = exercises)
            }
        }
        viewModelScope.launch {
            exerciseRepository.getCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
    }

    fun addCustomExercise(nome: String, categoria: String, onCreated: (ExerciseEntity) -> Unit = {}) {
        viewModelScope.launch {
            val newId = exerciseRepository.addCustomExercise(nome, categoria)
            onCreated(ExerciseEntity(id = newId, nome = nome, categoria = categoria))
        }
    }

    fun updateCustomExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            exerciseRepository.saveExercise(exercise)
        }
    }

    fun deleteCustomExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            exerciseRepository.deleteExercise(exercise)
        }
    }

    fun addExercise(
        exerciseId: Int,
        serieTarget: Int,
        repsTarget: String,
        recuperoTarget: Int,
        exerciseType: String = "strength",
        durataTargetSecondi: Int? = null
    ) {
        viewModelScope.launch {
            val current = _uiState.value.planDetails ?: return@launch
            val nextOrder = (current.exercises.maxOfOrNull { it.planExercise.ordine } ?: -1) + 1

            workoutRepository.savePlanExercise(
                PlanExerciseEntity(
                    planId = current.plan.id,
                    exerciseId = exerciseId,
                    serieTarget = serieTarget,
                    repsTarget = repsTarget,
                    recuperoTarget = recuperoTarget,
                    ordine = nextOrder,
                    exerciseType = exerciseType,
                    durataTargetSecondi = durataTargetSecondi
                )
            )
        }
    }

    fun addCardioExercise(
        exerciseId: Int,
        cardioCategoria: String?,
        durataTargetSecondi: Int?,
        distanzaTargetKm: Float? = null,
        recuperoTarget: Int = 0
    ) {
        viewModelScope.launch {
            val current = _uiState.value.planDetails ?: return@launch
            val nextOrder = (current.exercises.maxOfOrNull { it.planExercise.ordine } ?: -1) + 1

            workoutRepository.savePlanExercise(
                PlanExerciseEntity(
                    planId = current.plan.id,
                    exerciseId = exerciseId,
                    serieTarget = 1,
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
        durataTargetSecondi: Int? = original.durataTargetSecondi
    ) {
        viewModelScope.launch {
            workoutRepository.updatePlanExercise(
                original.copy(
                    exerciseId = exerciseId,
                    serieTarget = serieTarget,
                    repsTarget = repsTarget,
                    recuperoTarget = recuperoTarget,
                    exerciseType = exerciseType,
                    durataTargetSecondi = durataTargetSecondi
                )
            )
        }
    }

    fun removeExercise(planExercise: PlanExerciseEntity) {
        viewModelScope.launch {
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
        viewModelScope.launch {
            val details = _uiState.value.planDetails ?: return@launch
            val sorted = details.exercises.sortedBy { it.planExercise.ordine }
            val updates = sorted.mapIndexed { index, item ->
                item.planExercise.copy(ordine = index)
            }
            workoutRepository.savePlanExercises(updates)
        }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
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
        viewModelScope.launch {
            uiState.value.planDetails?.plan?.let { plan ->
                workoutRepository.deletePlan(plan)
                onSuccess()
            }
        }
    }

    fun addPlanImage(imageUri: String) {
        viewModelScope.launch {
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
        viewModelScope.launch {
            workoutRepository.deletePlanImage(image)
        }
    }

    fun updatePlan(
        nome: String,
        note: String?,
        giorniSettimana: String? = null,
        dataInizio: Long? = null,
        dataFine: Long? = null
    ) {
        viewModelScope.launch {
            uiState.value.planDetails?.plan?.let { plan ->
                workoutRepository.updatePlan(
                    plan.copy(
                        nome = nome,
                        note = note,
                        giorniSettimana = giorniSettimana,
                        dataInizio = dataInizio ?: plan.dataInizio,
                        dataFine = dataFine
                    )
                )
            }
        }
    }

    fun updateExercisesOrder(orderedExercises: List<PlanExerciseEntity>) {
        viewModelScope.launch {
            val updates = orderedExercises.mapIndexed { index, it ->
                it.copy(ordine = index)
            }
            workoutRepository.savePlanExercises(updates)
        }
    }

    fun toggleSupersetWithNext(planExercise: PlanExerciseEntity, customSupersetId: String? = null) {
        viewModelScope.launch {
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
                    val sid = current.supersetId ?: next.supersetId ?: customSupersetId ?: java.util.UUID.randomUUID().toString()
                    workoutRepository.savePlanExercises(listOf(
                        current.copy(supersetId = sid),
                        next.copy(supersetId = sid)
                    ))
                }
            }
        }
    }
}
