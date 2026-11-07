package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.data.ai.ScanPhase
import com.emanuel5014.trainable.data.ai.ScannedProgram
import com.emanuel5014.trainable.data.ai.backend.ConsentRequest
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

sealed interface ProgramScanState {
    data object Idle : ProgramScanState
    data class Scanning(val phase: ScanPhase = ScanPhase.LOADING_MODEL) : ProgramScanState
    data class Result(val program: ScannedProgram) : ProgramScanState
    /** [failure] is set when a scan backend explained what went wrong. */
    data class Failed(val failure: ScanBackendException? = null) : ProgramScanState
}

interface ProgramScanBackend {
    suspend fun consentRequired(): ConsentRequest?
    suspend fun acceptConsent(request: ConsentRequest)
    suspend fun scan(
        imageUri: String,
        exerciseName: String?,
        onPhase: (ScanPhase) -> Unit,
        onStreamUpdate: (String, String) -> Unit
    ): ScannedProgram?

    suspend fun release() {}
}

/** Reads the program page for the advanced-prescription editor and owns the pending review. */
class ProgramScanModel<Metrics>(
    private val backend: ProgramScanBackend,
    private val scope: CoroutineScope,
    private val releaseDispatcher: CoroutineDispatcher,
    private val metricsProvider: suspend () -> ((Int, Int) -> Metrics)? = { null }
) {
    private companion object {
        const val EMIT_INTERVAL_MS = 250L
        const val MAX_CHARS = 4000
    }

    private val _state = MutableStateFlow<ProgramScanState>(ProgramScanState.Idle)
    val state = _state.asStateFlow()
    private val _stream = MutableStateFlow(RoutineScanStream<Metrics>())
    val stream = _stream.asStateFlow()
    private val _consent = MutableStateFlow<ConsentRequest?>(null)
    val consent = _consent.asStateFlow()
    private var pendingScan: Pair<String, String?>? = null
    private var job: Job? = null

    fun scan(imageUri: String, exerciseName: String?) {
        if (job?.isActive == true || _consent.value != null) return
        launchScan {
            val request = backend.consentRequired()
            if (request == null) {
                startScan(imageUri, exerciseName)
            } else {
                pendingScan = imageUri to exerciseName
                _consent.value = request
            }
        }
    }

    fun acceptConsent() {
        val request = _consent.value ?: return
        val (imageUri, exerciseName) = pendingScan ?: return
        declineConsent()
        launchScan {
            backend.acceptConsent(request)
            startScan(imageUri, exerciseName)
        }
    }

    fun declineConsent() {
        pendingScan = null
        _consent.value = null
    }

    private fun launchScan(block: suspend () -> Unit) {
        job = scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ScanBackendException) {
                _state.value = ProgramScanState.Failed(e)
            } catch (_: Exception) {
                _state.value = ProgramScanState.Failed()
            }
        }
    }

    private suspend fun startScan(imageUri: String, exerciseName: String?) = coroutineScope {
        _state.value = ProgramScanState.Scanning()
        _stream.value = RoutineScanStream()
        val startTime = TimeSource.Monotonic.markNow()
        val captureMetrics = metricsProvider()
        val generatedChars = MutableStateFlow(0)
        var lastEmit: Long? = null
        var latestOutput = ""
        var latestThinking = ""
        fun metrics() = captureMetrics?.invoke(generatedChars.value, startTime.elapsedNow().inWholeSeconds.toInt())

        fun emit(force: Boolean = false) {
            val elapsed = startTime.elapsedNow().inWholeMilliseconds
            val previous = lastEmit
            if (!force && previous != null && elapsed - previous < EMIT_INTERVAL_MS) return
            lastEmit = elapsed
            val next = RoutineScanStream(latestOutput.takeLast(MAX_CHARS), latestThinking.takeLast(MAX_CHARS), metrics())
            _stream.update { if (isActive) next else it }
        }

        val metricsJob = captureMetrics?.let {
            launch {
                while (isActive) {
                    delay(500)
                    val sample = metrics()
                    _stream.update { it.copy(metrics = sample) }
                }
            }
        }

        val program = try {
            backend.scan(
                imageUri,
                exerciseName,
                onPhase = { phase ->
                    _state.update { if (isActive && it is ProgramScanState.Scanning) it.copy(phase = phase) else it }
                },
                onStreamUpdate = { output, thinking ->
                    latestOutput = output
                    latestThinking = thinking
                    generatedChars.value = output.length + thinking.length
                    emit()
                }
            )
        } finally {
            metricsJob?.cancel()
        }
        ensureActive()
        emit(force = true)
        _state.value = if (program == null) ProgramScanState.Failed() else ProgramScanState.Result(program)
    }

    fun cancel() {
        job?.cancel()
        job = null
        declineConsent()
        reset()
        releaseBackend()
    }

    fun reset() {
        _state.value = ProgramScanState.Idle
        _stream.value = RoutineScanStream()
    }

    fun close() {
        job?.cancel()
        releaseBackend()
    }

    private fun releaseBackend() {
        scope.launch(NonCancellable + releaseDispatcher) { backend.release() }
    }
}
