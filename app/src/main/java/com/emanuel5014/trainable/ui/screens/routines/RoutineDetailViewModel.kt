package com.emanuel5014.trainable.ui.screens.routines

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.ai.AiResourceTracker
import com.emanuel5014.trainable.data.ai.RoutineScanner
import com.emanuel5014.trainable.data.ai.ScanDraftStore
import com.emanuel5014.trainable.data.ai.ScanPhase
import com.emanuel5014.trainable.data.ai.backend.ConsentRequest
import com.emanuel5014.trainable.data.ai.backend.ScanBackendProvider
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    userPreferencesRepository: UserPreferencesRepository,
    localeManager: AppLocaleManager,
    routineScanner: RoutineScanner,
    backends: ScanBackendProvider,
    aiResourceTracker: AiResourceTracker,
    oneRepMaxRepository: OneRepMaxRepository,
    scanDraftStore: ScanDraftStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val planId: Int = checkNotNull(savedStateHandle["planId"])

    val state = RoutineDetailState(
        planId, workoutRepository, exerciseRepository, userPreferencesRepository, oneRepMaxRepository, viewModelScope
    )

    private val _languageCode = MutableStateFlow("en")
    val languageCode = _languageCode.asStateFlow()

    val aiResourceAnalyticsEnabled = combine(
        userPreferencesRepository.aiResourceAnalyticsEnabled,
        backends.localActive
    ) { enabled, local -> enabled && local }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val scan = RoutineScanModel(
        planId, state, workoutRepository, exerciseRepository, oneRepMaxRepository,
        language = localeManager::getResolvedLanguage,
        backend = object : RoutineScanBackend {
            override val isReady = backends.isReady
            override suspend fun consentRequired() = backends.consentRequired()
            override suspend fun acceptConsent(request: ConsentRequest) = backends.acceptConsent()

            override suspend fun scan(
                imageUri: String,
                catalog: List<ExerciseEntity>,
                categories: List<String>,
                languageCode: String,
                onPhase: (ScanPhase) -> Unit,
                onStreamUpdate: (String, String) -> Unit
            ) = routineScanner.scan(Uri.parse(imageUri), catalog, categories, languageCode, onPhase, onStreamUpdate)

            override suspend fun release() = routineScanner.release()
        },
        drafts = scanDraftStore,
        scope = viewModelScope,
        diskDispatcher = Dispatchers.IO,
        metricsProvider = {
            if (userPreferencesRepository.aiResourceAnalyticsEnabled.first() && backends.isLocalActive()) {
                aiResourceTracker::captureMetrics
            } else null
        }
    )

    override fun onCleared() {
        super.onCleared()
        scan.close()
    }

    init {
        viewModelScope.launch {
            localeManager.userSelectedLanguage.collect {
                _languageCode.value = localeManager.getResolvedLanguage()
            }
        }
    }
}
