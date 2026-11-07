package com.emanuel5014.trainable.ui.screens.routines

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.ai.AiResourceTracker
import com.emanuel5014.trainable.data.ai.RoutineScanner
import com.emanuel5014.trainable.data.ai.ScanPhase
import com.emanuel5014.trainable.data.ai.backend.ConsentRequest
import com.emanuel5014.trainable.data.ai.backend.ScanBackendProvider
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class ProgramScanViewModel @Inject constructor(
    routineScanner: RoutineScanner,
    userPreferencesRepository: UserPreferencesRepository,
    aiResourceTracker: AiResourceTracker,
    backends: ScanBackendProvider
) : ViewModel() {
    /** The phone's live metrics only mean something while the on-device model is the one reading. */
    val resourceAnalyticsEnabled = combine(
        userPreferencesRepository.aiResourceAnalyticsEnabled,
        backends.localActive
    ) { enabled, local -> enabled && local }

    val model = ProgramScanModel(
        backend = object : ProgramScanBackend {
            override suspend fun consentRequired() = backends.consentRequired()
            override suspend fun acceptConsent(request: ConsentRequest) = backends.acceptConsent()

            override suspend fun scan(
                imageUri: String,
                exerciseName: String?,
                onPhase: (ScanPhase) -> Unit,
                onStreamUpdate: (String, String) -> Unit
            ) = routineScanner.scanProgram(Uri.parse(imageUri), exerciseName, onPhase, onStreamUpdate)

            override suspend fun release() = routineScanner.release()
        },
        scope = viewModelScope,
        releaseDispatcher = Dispatchers.IO,
        metricsProvider = {
            if (userPreferencesRepository.aiResourceAnalyticsEnabled.first() && backends.isLocalActive()) {
                aiResourceTracker::captureMetrics
            } else null
        }
    )

    override fun onCleared() {
        super.onCleared()
        model.close()
    }
}
