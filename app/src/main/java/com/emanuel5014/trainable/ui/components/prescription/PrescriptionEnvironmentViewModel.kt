package com.emanuel5014.trainable.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.ai.AiModelVariant
import com.emanuel5014.trainable.data.ai.ModelFileManager
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Everything the advanced-prescription editor needs from the rest of the app, so any screen can
 * show it without wiring 1RMs, units and AI availability itself.
 */
@HiltViewModel
class PrescriptionEnvironmentViewModel @Inject constructor(
    private val oneRepMaxRepository: OneRepMaxRepository,
    userPreferencesRepository: UserPreferencesRepository,
    modelFileManager: ModelFileManager
) : ViewModel() {

    val weightUnit: StateFlow<String> = userPreferencesRepository.weightUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "kg")

    val roundingIncrement: StateFlow<Float> = userPreferencesRepository.loadRoundingIncrement
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoadCalculator.DEFAULT_INCREMENT_KG)

    /** exerciseId → current 1RM in kg. */
    val oneRepMaxes: StateFlow<Map<Int, Float>> = oneRepMaxRepository.currentByExercise()
        .map { current -> current.mapValues { it.value.weightKg } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** exerciseId → Epley estimate from recent logs, in kg. */
    val estimatedOneRepMaxes: StateFlow<Map<Int, Float>> = oneRepMaxRepository.estimatedByExercise()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** The AI button is only offered when AI is enabled in settings and the model is on the device. */
    val aiAvailable: StateFlow<Boolean> = combine(
        userPreferencesRepository.aiScanEnabled,
        userPreferencesRepository.aiModelVariant,
        modelFileManager.filesUpdatedTrigger
    ) { enabled, variantId, _ ->
        enabled && modelFileManager.isDownloaded(AiModelVariant.fromId(variantId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun saveOneRepMax(exerciseId: Int, weightKg: Float, source: String = OneRepMaxEntity.SOURCE_MANUAL) {
        viewModelScope.launch { oneRepMaxRepository.add(exerciseId = exerciseId, weightKg = weightKg, source = source) }
    }
}
