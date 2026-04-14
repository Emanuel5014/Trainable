package com.emanuel5014.trainable.ui.screens.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditWorkoutState(
    val isLoading: Boolean = true,
    val sessionId: Int = 0,
    val planName: String = "",
    val exercises: List<EditExerciseState> = emptyList(),
    val availableExercises: List<ExerciseEntity> = emptyList(),
    val error: String? = null,
    val weightUnit: String = "kg"
)

data class EditExerciseState(
    val exercise: ExerciseEntity,
    val sets: List<SetLogEntity> = emptyList()
)

@HiltViewModel
class EditWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(EditWorkoutState())
    val state: StateFlow<EditWorkoutState> = _state.asStateFlow()

    private val sessionId: Int = savedStateHandle.get<Int>("sessionId") ?: 0

    val languageCode = userPreferencesRepository.userLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    init {
        loadSessionData()
        loadAvailableExercises()
        viewModelScope.launch {
            userPreferencesRepository.weightUnit.collect { unit ->
                _state.update { it.copy(weightUnit = unit) }
            }
        }
    }

    private fun loadSessionData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            workoutRepository.getSessionWithDetails(sessionId)
                .onEach { sessionDetails ->
                    if (sessionDetails != null) {
                        val groupedSets = sessionDetails.sets.groupBy { it.exercise.id }
                        val exercises = groupedSets.map { (exerciseId, setsWithEx) ->
                            EditExerciseState(
                                exercise = setsWithEx.first().exercise,
                                sets = setsWithEx.map { it.setLog }.sortedBy { it.numeroSerie }
                            )
                        }.sortedBy { it.sets.firstOrNull()?.ordineEsercizio ?: 0 }

                        // Check if we need to normalize orders (e.g., after migration they might all be 0)
                        val orders = exercises.map { it.sets.firstOrNull()?.ordineEsercizio ?: 0 }
                        if (orders.size > 1 && orders.distinct().size < orders.size) {
                            normalizeExerciseOrders(exercises)
                        }
                        
                        _state.update {
                            it.copy(
                                isLoading = false,
                                sessionId = sessionId,
                                planName = sessionDetails.plan.nome,
                                exercises = exercises
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Session not found") }
                    }
                }
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect()
        }
    }

    private fun normalizeExerciseOrders(exercises: List<EditExerciseState>) {
        viewModelScope.launch {
            val setsToUpdate = mutableListOf<SetLogEntity>()
            exercises.forEachIndexed { index, exerciseState ->
                exerciseState.sets.forEach { set ->
                    setsToUpdate.add(set.copy(ordineEsercizio = index))
                }
            }
            if (setsToUpdate.isNotEmpty()) {
                workoutRepository.updateSetOrders(setsToUpdate)
            }
        }
    }

    private fun loadAvailableExercises() {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _state.update { it.copy(availableExercises = exercises) }
            }
        }
    }

    fun updateSet(updatedSet: SetLogEntity) {
        viewModelScope.launch {
            workoutRepository.updateSet(updatedSet)
        }
    }

    fun deleteSet(set: SetLogEntity) {
        viewModelScope.launch {
            workoutRepository.deleteSet(set)
            // After deleting, we might need to reorder remaining sets
            reorderSets(set.exerciseId)
        }
    }

    fun addSet(exerciseId: Int) {
        viewModelScope.launch {
            val exerciseState = _state.value.exercises.find { it.exercise.id == exerciseId }
            val nextSerie = (exerciseState?.sets?.maxOfOrNull { it.numeroSerie } ?: 0) + 1
            val lastWeight = exerciseState?.sets?.lastOrNull()?.pesoSollevato ?: 0f
            val lastReps = exerciseState?.sets?.lastOrNull()?.repsEffettive ?: 10
            val exerciseOrder = exerciseState?.sets?.firstOrNull()?.ordineEsercizio ?: 0

            workoutRepository.logSet(
                SetLogEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    pesoSollevato = lastWeight,
                    repsEffettive = lastReps,
                    numeroSerie = nextSerie,
                    ordineEsercizio = exerciseOrder
                )
            )
        }
    }

    fun deleteExercise(exerciseId: Int) {
        viewModelScope.launch {
            workoutRepository.deleteExerciseFromSession(sessionId, exerciseId)
        }
    }

    fun swapExercise(oldExerciseId: Int, newExerciseId: Int) {
        viewModelScope.launch {
            val oldExerciseState = _state.value.exercises.find { it.exercise.id == oldExerciseId } ?: return@launch
            val sets = oldExerciseState.sets
            val exerciseOrder = sets.firstOrNull()?.ordineEsercizio ?: 0

            // Delete old sets
            workoutRepository.deleteExerciseFromSession(sessionId, oldExerciseId)

            // Insert new sets with new exerciseId and same order
            sets.forEach { set ->
                workoutRepository.logSet(
                    set.copy(id = 0, exerciseId = newExerciseId, ordineEsercizio = exerciseOrder)
                )
            }
        }
    }

    fun addExercise(exerciseId: Int) {
        viewModelScope.launch {
            val nextOrder = (_state.value.exercises.maxOfOrNull { it.sets.firstOrNull()?.ordineEsercizio ?: 0 } ?: -1) + 1
            // Add a first set for the new exercise
            workoutRepository.logSet(
                SetLogEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    pesoSollevato = 0f,
                    repsEffettive = 10,
                    numeroSerie = 1,
                    ordineEsercizio = nextOrder
                )
            )
        }
    }

    fun addCustomExercise(name: String, category: String) {
        viewModelScope.launch {
            val exerciseId = exerciseRepository.addCustomExercise(name, category)
            addExercise(exerciseId)
        }
    }

    fun moveExerciseUp(exerciseId: Int) {
        val exercises = _state.value.exercises
        val index = exercises.indexOfFirst { it.exercise.id == exerciseId }
        if (index > 0) {
            val currentEx = exercises[index]
            val prevEx = exercises[index - 1]
            
            // Ensure we have valid orders to swap
            val currentOrder = currentEx.sets.firstOrNull()?.ordineEsercizio ?: index
            val prevOrder = prevEx.sets.firstOrNull()?.ordineEsercizio ?: (index - 1)
            
            // If they are the same, we must force a difference
            val finalPrevOrder = if (currentOrder == prevOrder) index - 1 else prevOrder
            val finalCurrentOrder = if (currentOrder == prevOrder) index else currentOrder

            viewModelScope.launch {
                val setsToUpdate = mutableListOf<SetLogEntity>()
                currentEx.sets.forEach { setsToUpdate.add(it.copy(ordineEsercizio = finalPrevOrder)) }
                prevEx.sets.forEach { setsToUpdate.add(it.copy(ordineEsercizio = finalCurrentOrder)) }
                if (setsToUpdate.isNotEmpty()) {
                    workoutRepository.updateSetOrders(setsToUpdate)
                }
            }
        }
    }

    fun moveExerciseDown(exerciseId: Int) {
        val exercises = _state.value.exercises
        val index = exercises.indexOfFirst { it.exercise.id == exerciseId }
        if (index < exercises.size - 1) {
            val currentEx = exercises[index]
            val nextEx = exercises[index + 1]
            
            val currentOrder = currentEx.sets.firstOrNull()?.ordineEsercizio ?: index
            val nextOrder = nextEx.sets.firstOrNull()?.ordineEsercizio ?: (index + 1)
            
            // If they are the same, we must force a difference
            val finalNextOrder = if (currentOrder == nextOrder) index + 1 else nextOrder
            val finalCurrentOrder = if (currentOrder == nextOrder) index else currentOrder

            viewModelScope.launch {
                val setsToUpdate = mutableListOf<SetLogEntity>()
                currentEx.sets.forEach { setsToUpdate.add(it.copy(ordineEsercizio = finalNextOrder)) }
                nextEx.sets.forEach { setsToUpdate.add(it.copy(ordineEsercizio = finalCurrentOrder)) }
                if (setsToUpdate.isNotEmpty()) {
                    workoutRepository.updateSetOrders(setsToUpdate)
                }
            }
        }
    }

    fun moveSetUp(set: SetLogEntity) {
        val exerciseState = _state.value.exercises.find { it.exercise.id == set.exerciseId } ?: return
        val index = exerciseState.sets.indexOfFirst { it.id == set.id }
        if (index > 0) {
            val prevSet = exerciseState.sets[index - 1]
            viewModelScope.launch {
                workoutRepository.updateSetOrders(listOf(
                    set.copy(numeroSerie = prevSet.numeroSerie),
                    prevSet.copy(numeroSerie = set.numeroSerie)
                ))
            }
        }
    }

    fun moveSetDown(set: SetLogEntity) {
        val exerciseState = _state.value.exercises.find { it.exercise.id == set.exerciseId } ?: return
        val index = exerciseState.sets.indexOfFirst { it.id == set.id }
        if (index < exerciseState.sets.size - 1) {
            val nextSet = exerciseState.sets[index + 1]
            viewModelScope.launch {
                workoutRepository.updateSetOrders(listOf(
                    set.copy(numeroSerie = nextSet.numeroSerie),
                    nextSet.copy(numeroSerie = set.numeroSerie)
                ))
            }
        }
    }

    private suspend fun reorderSets(exerciseId: Int) {
        val sessionDetails = workoutRepository.getSessionWithDetails(sessionId).firstOrNull() ?: return
        val sets = sessionDetails.sets
            .filter { it.exercise.id == exerciseId }
            .map { it.setLog }
            .sortedBy { it.numeroSerie }
        
        val updatedSets = sets.mapIndexed { index, setLogEntity ->
            setLogEntity.copy(numeroSerie = index + 1)
        }
        
        workoutRepository.updateSetOrders(updatedSets)
    }
}
