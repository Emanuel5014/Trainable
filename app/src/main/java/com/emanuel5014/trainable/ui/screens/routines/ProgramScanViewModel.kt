package com.emanuel5014.trainable.ui.screens.routines

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.ai.AiResourceTracker
import com.emanuel5014.trainable.data.ai.RoutineScanner
import com.emanuel5014.trainable.data.ai.ScanPhase
import com.emanuel5014.trainable.data.ai.ScannedProgram
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProgramScanState {
    data object Idle : ProgramScanState
    data class Scanning(val phase: ScanPhase = ScanPhase.LOADING_MODEL) : ProgramScanState
    data class Result(val program: ScannedProgram) : ProgramScanState
    data object Failed : ProgramScanState
}

/** Runs the on-device model on a photo of a program page for the advanced-prescription editor. */
@HiltViewModel
class ProgramScanViewModel @Inject constructor(
    private val routineScanner: RoutineScanner,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiResourceTracker: AiResourceTracker
) : ViewModel() {

    private val _state = MutableStateFlow<ProgramScanState>(ProgramScanState.Idle)
    val state: StateFlow<ProgramScanState> = _state.asStateFlow()

    private val _stream = MutableStateFlow(AiScanStreamState())
    val stream: StateFlow<AiScanStreamState> = _stream.asStateFlow()

    val resourceAnalyticsEnabled = userPreferencesRepository.aiResourceAnalyticsEnabled

    private var job: Job? = null

    fun scan(imageUri: Uri, exerciseName: String?) {
        if (_state.value is ProgramScanState.Scanning) return
        _state.value = ProgramScanState.Scanning()
        _stream.value = AiScanStreamState()
        job?.cancel()
        job = viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val analytics = userPreferencesRepository.aiResourceAnalyticsEnabled.first()
                var latestOutput = ""
                var latestThinking = ""
                var lastEmit = 0L

                fun emit(force: Boolean = false) {
                    val now = System.currentTimeMillis()
                    if (!force && now - lastEmit < EMIT_INTERVAL_MS) return
                    lastEmit = now
                    val metrics = if (analytics) {
                        aiResourceTracker.captureMetrics(
                            charsGenerated = latestOutput.length + latestThinking.length,
                            elapsedSeconds = ((now - startTime) / 1000L).toInt()
                        )
                    } else null
                    _stream.value = AiScanStreamState(
                        output = latestOutput.takeLast(MAX_CHARS),
                        thinking = latestThinking.takeLast(MAX_CHARS),
                        metrics = metrics
                    )
                }

                val metricsJob = if (analytics) launch {
                    while (isActive) {
                        delay(500)
                        val elapsed = ((System.currentTimeMillis() - startTime) / 1000L).toInt()
                        _stream.value = _stream.value.copy(
                            metrics = aiResourceTracker.captureMetrics(latestOutput.length + latestThinking.length, elapsed)
                        )
                    }
                } else null

                val program = routineScanner.scanProgram(
                    imageUri = imageUri,
                    exerciseName = exerciseName,
                    onPhase = { phase ->
                        (_state.value as? ProgramScanState.Scanning)?.let { _state.value = it.copy(phase = phase) }
                    },
                    onStreamUpdate = { output, thinking ->
                        latestOutput = output
                        latestThinking = thinking
                        emit()
                    }
                )
                metricsJob?.cancel()
                emit(force = true)
                _state.value = if (program == null) ProgramScanState.Failed else ProgramScanState.Result(program)
            } catch (e: CancellationException) {
                _state.value = ProgramScanState.Idle
                _stream.value = AiScanStreamState()
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = ProgramScanState.Failed
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = ProgramScanState.Idle
        _stream.value = AiScanStreamState()
        viewModelScope.launch(NonCancellable + Dispatchers.IO) { routineScanner.release() }
    }

    fun reset() {
        _state.value = ProgramScanState.Idle
        _stream.value = AiScanStreamState()
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
        viewModelScope.launch(NonCancellable + Dispatchers.IO) { routineScanner.release() }
    }

    private companion object {
        const val EMIT_INTERVAL_MS = 250L
        const val MAX_CHARS = 4000
    }
}
