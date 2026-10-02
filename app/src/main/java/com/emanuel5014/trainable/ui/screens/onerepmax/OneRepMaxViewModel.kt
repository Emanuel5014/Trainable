package com.emanuel5014.trainable.ui.screens.onerepmax

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OneRepMaxRow(
    val exerciseId: Int,
    val exerciseName: String,
    val category: String,
    val current: OneRepMaxEntity,
    val previousKg: Float?,
    val estimatedKg: Float?,
    /** Oldest → newest, for the trend chart. */
    val history: List<OneRepMaxEntity>
)

data class OneRepMaxUiState(
    val isLoading: Boolean = true,
    val rows: List<OneRepMaxRow> = emptyList(),
    val weightUnit: String = "kg",
    val roundingIncrement: Float = LoadCalculator.DEFAULT_INCREMENT_KG
)

@HiltViewModel
class OneRepMaxViewModel @Inject constructor(
    private val oneRepMaxRepository: OneRepMaxRepository,
    private val exerciseRepository: ExerciseRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager
) : ViewModel() {

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    val exercises: StateFlow<List<ExerciseEntity>> = exerciseRepository.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = exerciseRepository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<OneRepMaxUiState> = combine(
        oneRepMaxRepository.allWithExercise(),
        oneRepMaxRepository.estimatedByExercise(),
        userPreferencesRepository.weightUnit,
        userPreferencesRepository.loadRoundingIncrement
    ) { entries, estimates, unit, increment ->
        val rows = entries.groupBy { it.entry.exerciseId }.map { (exerciseId, list) ->
            val sorted = list.sortedWith(compareByDescending<com.emanuel5014.trainable.data.local.relation.OneRepMaxWithExercise> { it.entry.date }.thenByDescending { it.entry.id })
            OneRepMaxRow(
                exerciseId = exerciseId,
                exerciseName = sorted.first().exerciseName,
                category = sorted.first().exerciseCategory,
                current = sorted.first().entry,
                previousKg = sorted.getOrNull(1)?.entry?.weightKg,
                estimatedKg = estimates[exerciseId],
                history = sorted.map { it.entry }.reversed()
            )
        }.sortedByDescending { it.current.date }
        OneRepMaxUiState(isLoading = false, rows = rows, weightUnit = unit, roundingIncrement = increment)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OneRepMaxUiState())

    init {
        viewModelScope.launch {
            localeManager.userSelectedLanguage.collect { _languageCode.value = localeManager.getResolvedLanguage() }
        }
    }

    fun history(exerciseId: Int): Flow<List<OneRepMaxEntity>> = oneRepMaxRepository.history(exerciseId)

    fun add(exerciseId: Int, weightKg: Float, source: String) {
        viewModelScope.launch { oneRepMaxRepository.add(exerciseId = exerciseId, weightKg = weightKg, source = source) }
    }

    fun delete(entry: OneRepMaxEntity) {
        viewModelScope.launch { oneRepMaxRepository.delete(entry) }
    }

    fun addCustomExercise(nome: String, categoria: String, onCreated: (ExerciseEntity) -> Unit) {
        viewModelScope.launch {
            val id = exerciseRepository.addCustomExercise(nome, categoria)
            onCreated(ExerciseEntity(id = id, nome = nome, categoria = categoria))
        }
    }
}
