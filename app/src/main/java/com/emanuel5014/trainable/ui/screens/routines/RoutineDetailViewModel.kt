package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import androidx.lifecycle.SavedStateHandle
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.ai.AiResourceTracker
import com.emanuel5014.trainable.data.ai.DeviceResourceMetrics
import com.emanuel5014.trainable.data.ai.backend.ConsentRequest
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import com.emanuel5014.trainable.data.ai.backend.ScanBackendProvider
import com.emanuel5014.trainable.data.ai.ScanPhase
import com.emanuel5014.trainable.data.ai.ScannedExerciseEntry
import com.emanuel5014.trainable.data.ai.RoutineScanner
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.domain.prescription.PrescriptionResolver
import com.emanuel5014.trainable.domain.prescription.ResolvedPrescription
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed interface AiScanState {
    data object Idle : AiScanState
    data class Scanning(
        val phase: ScanPhase = ScanPhase.LOADING_MODEL
    ) : AiScanState
    data class Success(
        val entries: List<ScannedExerciseEntry>,
        val imageUri: Uri? = null
    ) : AiScanState
    /** [failure] is set when a scan backend explained what went wrong. */
    data class Error(val message: String?, val failure: ScanBackendException? = null) : AiScanState
}

data class AiScanStreamState(
    val output: String = "",
    val thinking: String = "",
    val metrics: DeviceResourceMetrics? = null
)


/** The shared [RoutineDetailState] kept for the screen's lifetime, plus what only Android has: scanning a routine sheet. */
@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager,
    private val routineScanner: RoutineScanner,
    private val backends: ScanBackendProvider,
    private val aiResourceTracker: AiResourceTracker,
    private val oneRepMaxRepository: OneRepMaxRepository,
    private val scanDraftStore: com.emanuel5014.trainable.data.ai.ScanDraftStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val planId: Int = checkNotNull(savedStateHandle["planId"])

    val state = RoutineDetailState(
        planId, workoutRepository, exerciseRepository, userPreferencesRepository, oneRepMaxRepository, viewModelScope
    )

    companion object {
        private const val STREAM_EMIT_INTERVAL_MS = 250L
        private const val STREAM_MAX_CHARS = 4000
    }

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    val aiScanAvailable = backends.isReady.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /** The phone's live metrics only mean something while the on-device model is the one reading. */
    val aiResourceAnalyticsEnabled = combine(
        userPreferencesRepository.aiResourceAnalyticsEnabled,
        backends.localActive
    ) { enabled, local -> enabled && local }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _aiScanState = MutableStateFlow<AiScanState>(AiScanState.Idle)
    val aiScanState: StateFlow<AiScanState> = _aiScanState.asStateFlow()

    private val _aiScanStream = MutableStateFlow(AiScanStreamState())
    val aiScanStream: StateFlow<AiScanStreamState> = _aiScanStream.asStateFlow()

    private var scanJob: kotlinx.coroutines.Job? = null

    /** The last full-routine scan that wasn't imported, kept on disk until it is imported or discarded. */
    private val _scanDraft = MutableStateFlow<com.emanuel5014.trainable.data.ai.ScanDraft?>(null)
    val scanDraft: StateFlow<com.emanuel5014.trainable.data.ai.ScanDraft?> = _scanDraft.asStateFlow()

    fun resumeScanDraft() {
        val draft = _scanDraft.value ?: return
        _aiScanState.value = AiScanState.Success(entries = draft.entries, imageUri = draft.imageUri)
    }

    fun discardScanDraft() {
        _scanDraft.value = null
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { scanDraftStore.clear(planId) }
    }

    /** Called while the review is edited; the draft always mirrors what is on screen. */
    fun saveScanDraft(entries: List<ScannedExerciseEntry>) {
        val draft = _scanDraft.value ?: return
        _scanDraft.value = draft.copy(entries = entries, updatedAt = System.currentTimeMillis())
        viewModelScope.launch(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.IO) {
            scanDraftStore.save(planId, entries)
        }
    }

    private val _consent = MutableStateFlow<ConsentRequest?>(null)

    /** Set while the user is asked whether the photo may go to a remote provider. */
    val consent: StateFlow<ConsentRequest?> = _consent.asStateFlow()

    private var pendingScanUri: Uri? = null

    /** Starts a scan; when the photo would go somewhere the user hasn't agreed to yet, asks first. */
    fun scanRoutineSheet(imageUri: Uri) {
        if (_aiScanState.value is AiScanState.Scanning) return
        viewModelScope.launch {
            val request = backends.consentRequired()
            if (request == null) {
                startScan(imageUri)
            } else {
                pendingScanUri = imageUri
                _consent.value = request
            }
        }
    }

    fun acceptConsent() {
        val uri = pendingScanUri
        pendingScanUri = null
        _consent.value = null
        viewModelScope.launch {
            backends.acceptConsent()
            uri?.let { startScan(it) }
        }
    }

    fun declineConsent() {
        pendingScanUri = null
        _consent.value = null
    }

    private fun startScan(imageUri: Uri) {
        if (_aiScanState.value is AiScanState.Scanning) return
        _aiScanState.value = AiScanState.Scanning()
        _aiScanStream.value = AiScanStreamState()
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val isAnalyticsEnabled = userPreferencesRepository.aiResourceAnalyticsEnabled.first() && backends.isLocalActive()

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
                    catalog = state.uiState.value.availableExercises,
                    categories = state.uiState.value.categories,
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
                if (entries.isEmpty()) {
                    _aiScanState.value = AiScanState.Error(null)
                } else {
                    // Persist right away: from here on the scan survives closing the review or the app
                    val draftImage = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        scanDraftStore.create(planId, entries, imageUri)
                    }
                    _scanDraft.value = com.emanuel5014.trainable.data.ai.ScanDraft(entries, draftImage, System.currentTimeMillis())
                    _aiScanState.value = AiScanState.Success(entries = entries, imageUri = draftImage ?: imageUri)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancelled by user
                _aiScanState.value = AiScanState.Idle
                _aiScanStream.value = AiScanStreamState()
            } catch (e: ScanBackendException) {
                _aiScanState.value = AiScanState.Error(e.message, e)
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

    fun applyScannedExercises(entries: List<ScannedExerciseEntry>, targetWeek: Int = 1) {
        viewModelScope.launch {
            val details = state.uiState.value.planDetails ?: return@launch
            var nextOrder = (details.exercises.maxOfOrNull { it.planExercise.ordine } ?: -1) + 1
            var weeksNeeded = details.plan.weeksCount
            val currentMaxes = oneRepMaxRepository.currentByExercise().first()

            entries.forEach { entry ->
                val exerciseId = entry.exerciseId ?: exerciseRepository.addCustomExercise(
                    nome = entry.rawName,
                    categoria = entry.suggestedCategory.ifBlank { "Custom" }
                )

                entry.oneRepMaxKg?.let { kg ->
                    if (currentMaxes[exerciseId]?.weightKg != kg) {
                        oneRepMaxRepository.add(exerciseId = exerciseId, weightKg = kg, source = com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity.SOURCE_MANUAL)
                    }
                }

                if (entry.isAdvanced) {
                    // A single unlabelled week goes to the week being viewed; "W1:…W4:" sheets keep their weeks
                    val byWeek = if (entry.blocksByWeek.keys == setOf(1)) mapOf(targetWeek to entry.blocksByWeek.getValue(1)) else entry.blocksByWeek
                    val planExerciseId = workoutRepository.savePlanExercise(
                        PlanExerciseEntity(
                            planId = details.plan.id,
                            exerciseId = exerciseId,
                            serieTarget = entry.sets,
                            repsTarget = entry.reps,
                            recuperoTarget = entry.restSeconds,
                            ordine = nextOrder++,
                            exerciseType = "strength"
                        )
                    ).toInt()
                    workoutRepository.savePlanExerciseBlocks(planExerciseId, byWeek)
                    weeksNeeded = maxOf(weeksNeeded, byWeek.keys.max())
                    return@forEach
                }

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
                    } else if (entry.isTimeAndReps) {
                        PlanExerciseEntity(
                            planId = details.plan.id,
                            exerciseId = exerciseId,
                            serieTarget = entry.sets,
                            repsTarget = TimeAndReps.goalOf(entry.reps).toString(),
                            recuperoTarget = entry.restSeconds,
                            ordine = nextOrder++,
                            exerciseType = TIME_AND_REPS,
                            durataTargetSecondi = entry.timeSeconds ?: TimeAndReps.DEFAULT_SECONDS
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

            if (weeksNeeded > details.plan.weeksCount) {
                workoutRepository.updatePlan(details.plan.copy(weeksCount = weeksNeeded.coerceAtMost(RoutineDetailState.MAX_WEEKS)))
            }

            _aiScanState.value = AiScanState.Idle
            _aiScanStream.value = AiScanStreamState()
            discardScanDraft()
        }
    }

    init {
        viewModelScope.launch {
            _scanDraft.value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { scanDraftStore.load(planId) }
        }
        viewModelScope.launch {
            localeManager.userSelectedLanguage.collect { _ ->
                _languageCode.value = localeManager.getResolvedLanguage()
            }
        }
    }
}
