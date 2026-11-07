package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.data.ai.ScanDraft
import com.emanuel5014.trainable.data.ai.RoutineScanDraftStore
import com.emanuel5014.trainable.data.ai.ScanPhase
import com.emanuel5014.trainable.data.ai.ScannedExerciseEntry
import com.emanuel5014.trainable.data.ai.backend.ConsentRequest
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.TimeSource

sealed interface AiScanState {
    data object Idle : AiScanState
    data class Scanning(
        val phase: ScanPhase = ScanPhase.LOADING_MODEL
    ) : AiScanState
    data class Success(
        val entries: List<ScannedExerciseEntry>,
        val imageUri: String? = null
    ) : AiScanState
    /** [failure] is set when a scan backend explained what went wrong. */
    data class Error(val message: String?, val failure: ScanBackendException? = null) : AiScanState
}

data class RoutineScanStream<Metrics>(
    val output: String = "",
    val thinking: String = "",
    val metrics: Metrics? = null
)

interface RoutineScanBackend {
    val isReady: Flow<Boolean>
    suspend fun consentRequired(): ConsentRequest?
    suspend fun acceptConsent(request: ConsentRequest)
    suspend fun scan(
        imageUri: String,
        catalog: List<ExerciseEntity>,
        categories: List<String>,
        languageCode: String,
        onPhase: (ScanPhase) -> Unit,
        onStreamUpdate: (String, String) -> Unit
    ): List<ScannedExerciseEntry>

    suspend fun release() {}
}

class RoutineScanModel<Metrics>(
    private val planId: Int,
    val detail: RoutineDetailState,
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
    private val oneRepMaxes: OneRepMaxRepository,
    private val language: suspend () -> String,
    private val backend: RoutineScanBackend,
    private val drafts: RoutineScanDraftStore,
    private val scope: CoroutineScope,
    private val diskDispatcher: CoroutineDispatcher,
    private val metricsProvider: suspend () -> ((Int, Int) -> Metrics)? = { null }
) {
    private companion object {
        const val STREAM_EMIT_INTERVAL_MS = 250L
        const val STREAM_MAX_CHARS = 4000
    }

    val aiScanAvailable = backend.isReady.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    private val _aiScanState = MutableStateFlow<AiScanState>(AiScanState.Idle)
    val aiScanState = _aiScanState.asStateFlow()
    private val _aiScanStream = MutableStateFlow(RoutineScanStream<Metrics>())
    val aiScanStream = _aiScanStream.asStateFlow()
    private val _scanDraft = MutableStateFlow<ScanDraft?>(null)
    val scanDraft = _scanDraft.asStateFlow()
    private val _consent = MutableStateFlow<ConsentRequest?>(null)
    val consent = _consent.asStateFlow()

    private val draftLock = Mutex()
    private var pendingScanUri: String? = null
    private var scanJob: Job? = null
    private var applying = false

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            draftLock.withLock {
                _scanDraft.value = withContext(diskDispatcher) { drafts.load(planId) }
            }
        }
    }

    fun resumeScanDraft() {
        val draft = _scanDraft.value ?: return
        _aiScanState.value = AiScanState.Success(draft.entries, draft.imageUri)
    }

    fun discardScanDraft() {
        _scanDraft.value = null
        writeDraft { drafts.clear(planId) }
    }

    fun saveScanDraft(entries: List<ScannedExerciseEntry>) {
        val draft = _scanDraft.value ?: return
        _scanDraft.value = draft.copy(entries = entries, updatedAt = Clock.System.now().toEpochMilliseconds())
        writeDraft { drafts.save(planId, entries) }
    }

    // Order edits and deletion even if the review closes before disk I/O finishes.
    private fun writeDraft(write: () -> Unit) {
        scope.launch(NonCancellable, start = CoroutineStart.UNDISPATCHED) {
            draftLock.withLock { withContext(diskDispatcher) { write() } }
        }
    }

    fun scanRoutineSheet(imageUri: String) {
        if (scanJob?.isActive == true || _consent.value != null) return
        scanJob = scope.launch {
            runScan {
                val request = backend.consentRequired()
                if (request == null) {
                    startScan(imageUri)
                } else {
                    pendingScanUri = imageUri
                    _consent.value = request
                }
            }
        }
    }

    fun acceptConsent() {
        val request = _consent.value ?: return
        val uri = pendingScanUri ?: return
        declineConsent()
        scanJob = scope.launch {
            runScan {
                backend.acceptConsent(request)
                startScan(uri)
            }
        }
    }

    fun declineConsent() {
        pendingScanUri = null
        _consent.value = null
    }

    private suspend fun runScan(scan: suspend () -> Unit) {
        try {
            scan()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ScanBackendException) {
            currentCoroutineContext().ensureActive()
            _aiScanState.value = AiScanState.Error(e.message, e)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            _aiScanState.value = AiScanState.Error(e.message ?: "")
        }
    }

    private suspend fun startScan(imageUri: String) = kotlinx.coroutines.coroutineScope {
        _aiScanState.value = AiScanState.Scanning()
        _aiScanStream.value = RoutineScanStream()
        val startTime = TimeSource.Monotonic.markNow()
        val captureMetrics = metricsProvider()
        var lastEmitAt: Long? = null
        var latestOutput = ""
        var latestThinking = ""
        fun metrics() = captureMetrics?.invoke(
            latestOutput.length + latestThinking.length,
            startTime.elapsedNow().inWholeSeconds.toInt()
        )

        fun emit(force: Boolean = false) {
            val now = startTime.elapsedNow().inWholeMilliseconds
            val last = lastEmitAt
            if (force || last == null || now - last >= STREAM_EMIT_INTERVAL_MS) {
                lastEmitAt = now
                _aiScanStream.value = RoutineScanStream(
                    output = latestOutput.takeLast(STREAM_MAX_CHARS),
                    thinking = latestThinking.takeLast(STREAM_MAX_CHARS),
                    metrics = metrics()
                )
            }
        }

        val metricsJob = captureMetrics?.let {
            _aiScanStream.value = _aiScanStream.value.copy(metrics = it(0, 0))
            launch {
                while (isActive) {
                    delay(500)
                    _aiScanStream.value = _aiScanStream.value.copy(metrics = metrics())
                }
            }
        }

        val entries = try {
            val state = detail.uiState.value
            backend.scan(
                imageUri = imageUri,
                catalog = state.availableExercises,
                categories = state.categories,
                languageCode = language(),
                onPhase = { phase ->
                    if (isActive) _aiScanState.value = AiScanState.Scanning(phase)
                },
                onStreamUpdate = { output, thinking ->
                    if (isActive) {
                        latestOutput = output
                        latestThinking = thinking
                        emit()
                    }
                }
            )
        } finally {
            metricsJob?.cancel()
        }
        ensureActive()
        emit(force = true)
        if (entries.isEmpty()) {
            _aiScanState.value = AiScanState.Error(null)
        } else {
            draftLock.withLock {
                val draftImage = withContext(diskDispatcher) { drafts.create(planId, entries, imageUri) }
                _scanDraft.value = ScanDraft(entries, draftImage, Clock.System.now().toEpochMilliseconds())
                _aiScanState.value = AiScanState.Success(entries, draftImage ?: imageUri)
            }
        }
    }

    fun cancelAiScan() {
        scanJob?.cancel()
        scanJob = null
        dismissScanResult()
    }

    fun dismissScanResult() {
        _aiScanState.value = AiScanState.Idle
        _aiScanStream.value = RoutineScanStream()
        releaseBackend()
    }

    fun close() {
        scanJob?.cancel()
        releaseBackend()
    }

    private fun releaseBackend() {
        scope.launch(NonCancellable + diskDispatcher) { backend.release() }
    }

    fun applyScannedExercises(entries: List<ScannedExerciseEntry>, targetWeek: Int = 1) {
        if (applying) return
        scope.launch {
            val details = detail.uiState.value.planDetails ?: return@launch
            applying = true
            try {
                var nextOrder = (details.exercises.maxOfOrNull { it.planExercise.ordine } ?: -1) + 1
                var weeksNeeded = details.plan.weeksCount
                val currentMaxes = oneRepMaxes.currentByExercise().first()

                entries.forEach { entry ->
                    val exerciseId = entry.exerciseId ?: exercises.addCustomExercise(
                        nome = entry.rawName,
                        categoria = entry.suggestedCategory.ifBlank { "Custom" }
                    )

                    entry.oneRepMaxKg?.let { kg ->
                        if (currentMaxes[exerciseId]?.weightKg != kg) {
                            oneRepMaxes.add(exerciseId = exerciseId, weightKg = kg, source = OneRepMaxEntity.SOURCE_MANUAL)
                        }
                    }

                    if (entry.isAdvanced) {
                        // A single unlabelled week goes to the week being viewed; "W1:…W4:" sheets keep their weeks
                        val byWeek = if (entry.blocksByWeek.keys == setOf(1)) mapOf(targetWeek to entry.blocksByWeek.getValue(1)) else entry.blocksByWeek
                        val planExerciseId = workouts.savePlanExercise(
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
                        workouts.savePlanExerciseBlocks(planExerciseId, byWeek)
                        weeksNeeded = maxOf(weeksNeeded, byWeek.keys.max())
                        return@forEach
                    }

                    workouts.savePlanExercise(
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
                    workouts.updatePlan(details.plan.copy(weeksCount = weeksNeeded.coerceAtMost(RoutineDetailState.MAX_WEEKS)))
                }

                _aiScanState.value = AiScanState.Idle
                _aiScanStream.value = RoutineScanStream<Metrics>()
                discardScanDraft()
            } finally {
                applying = false
            }
        }
    }

}
