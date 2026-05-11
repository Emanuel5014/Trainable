package com.emanuel5014.trainable.ui.screens.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditWorkoutState(
    val isLoading: Boolean = true,
    val sessionId: Int = 0,
    val planName: String = "",
    val exercises: List<EditExerciseState> = emptyList(),
    val cardioLogs: List<CardioLogEntity> = emptyList(),
    val availableExercises: List<ExerciseEntity> = emptyList(),
    val sessionTimestamp: Long = 0L,
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
    private val localeManager: AppLocaleManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(EditWorkoutState())
    val state: StateFlow<EditWorkoutState> = _state.asStateFlow()

    private val sessionId: Int = savedStateHandle.get<Int>("sessionId") ?: 0

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    init {
        loadSessionData()
        loadAvailableExercises()
        viewModelScope.launch {
            userPreferencesRepository.userLanguage.collect { _ ->
                _languageCode.value = localeManager.getResolvedLanguage()
            }
        }
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
                                sessionTimestamp = sessionDetails.session.timestamp,
                                exercises = exercises,
                                cardioLogs = sessionDetails.cardio
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

    fun updateCardioLog(updatedCardio: CardioLogEntity) {
        viewModelScope.launch {
            workoutRepository.saveCardioLog(updatedCardio)
        }
    }

    fun addCardioLog(categoria: String, distanza: Float, durataSecondi: Int) {
        viewModelScope.launch {
            val newCardio = CardioLogEntity(
                sessionId = sessionId,
                categoria = categoria,
                distanza = distanza,
                durataSecondi = durataSecondi,
                timestamp = _state.value.sessionTimestamp
            )
            workoutRepository.saveCardioLog(newCardio)
        }
    }

    fun deleteCardioLog(cardio: CardioLogEntity) {
        viewModelScope.launch {
            // Optimistic update
            _state.update { curr ->
                curr.copy(cardioLogs = curr.cardioLogs.filter { it.id != cardio.id })
            }
            workoutRepository.deleteCardioLog(cardio)
        }
    }

    fun deleteSet(set: SetLogEntity) {
        viewModelScope.launch {
            // Optimistic update
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val index = mutableExercises.indexOfFirst { it.exercise.id == set.exerciseId }
                if (index != -1) {
                    val exState = mutableExercises[index]
                    // Use equality instead of ID to avoid deleting all id=0 sets
                    val newSets = exState.sets.filter { it != set }
                    mutableExercises[index] = exState.copy(sets = newSets)
                }
                curr.copy(exercises = mutableExercises)
            }
            
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

            val newSet = SetLogEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                pesoSollevato = lastWeight,
                repsEffettive = lastReps,
                numeroSerie = nextSerie,
                ordineEsercizio = exerciseOrder
            )

            // Optimistic update to prevent duplicates on rapid clicks
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val index = mutableExercises.indexOfFirst { it.exercise.id == exerciseId }
                if (index != -1) {
                    val exState = mutableExercises[index]
                    mutableExercises[index] = exState.copy(sets = exState.sets + newSet)
                }
                curr.copy(exercises = mutableExercises)
            }

            val id = workoutRepository.logSet(newSet)
            
            // Update the optimistic set with the real ID
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val exIndex = mutableExercises.indexOfFirst { it.exercise.id == exerciseId }
                if (exIndex != -1) {
                    val exState = mutableExercises[exIndex]
                    val mutableSets = exState.sets.toMutableList()
                    val setIndex = mutableSets.indexOfFirst { it.numeroSerie == nextSerie && it.id == 0 }
                    if (setIndex != -1) {
                        mutableSets[setIndex] = mutableSets[setIndex].copy(id = id.toInt())
                    }
                    mutableExercises[exIndex] = exState.copy(sets = mutableSets)
                }
                curr.copy(exercises = mutableExercises)
            }
        }
    }

    fun deleteExercise(exerciseId: Int) {
        viewModelScope.launch {
            // Optimistic update
            _state.update { curr ->
                curr.copy(exercises = curr.exercises.filter { it.exercise.id != exerciseId })
            }
            workoutRepository.deleteExerciseFromSession(sessionId, exerciseId)
        }
    }

    fun deleteSession() {
        viewModelScope.launch {
            workoutRepository.deleteSession(sessionId)
        }
    }

    fun swapExercise(oldExerciseId: Int, newExerciseId: Int) {
        viewModelScope.launch {
            val oldExerciseState = _state.value.exercises.find { it.exercise.id == oldExerciseId } ?: return@launch
            val sets = oldExerciseState.sets
            val exerciseOrder = sets.firstOrNull()?.ordineEsercizio ?: 0

            // Optimistic update
            val replacementExercise = _state.value.availableExercises.find { it.id == newExerciseId }
            if (replacementExercise != null) {
                _state.update { curr ->
                    val mutableExercises = curr.exercises.toMutableList()
                    val index = mutableExercises.indexOfFirst { it.exercise.id == oldExerciseId }
                    if (index != -1) {
                        mutableExercises[index] = EditExerciseState(
                            exercise = replacementExercise,
                            sets = sets.map { it.copy(exerciseId = newExerciseId, id = 0) }
                        )
                    }
                    curr.copy(exercises = mutableExercises)
                }
            }

            // Delete old sets
            workoutRepository.deleteExerciseFromSession(sessionId, oldExerciseId)

            // Insert new sets with new exerciseId and same order, then update state with real IDs
            val newSetIds = sets.map { set ->
                workoutRepository.logSet(
                    set.copy(id = 0, exerciseId = newExerciseId, ordineEsercizio = exerciseOrder)
                )
            }
            
            // Update IDs in state
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val index = mutableExercises.indexOfFirst { it.exercise.id == newExerciseId }
                if (index != -1) {
                    val exState = mutableExercises[index]
                    mutableExercises[index] = exState.copy(
                        sets = exState.sets.mapIndexed { i, s -> 
                            if (s.id == 0 && i < newSetIds.size) s.copy(id = newSetIds[i].toInt()) else s 
                        }
                    )
                }
                curr.copy(exercises = mutableExercises)
            }
        }
    }

    fun addExercise(exerciseId: Int) {
        viewModelScope.launch {
            val nextOrder = (_state.value.exercises.maxOfOrNull { it.sets.firstOrNull()?.ordineEsercizio ?: 0 } ?: -1) + 1
            val exercise = _state.value.availableExercises.find { it.id == exerciseId }
            
            val newSet = SetLogEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                pesoSollevato = 0f,
                repsEffettive = 10,
                numeroSerie = 1,
                ordineEsercizio = nextOrder
            )

            // Optimistic update
            if (exercise != null) {
                _state.update { curr ->
                    curr.copy(exercises = curr.exercises + EditExerciseState(exercise, listOf(newSet)))
                }
            }

            // Add a first set for the new exercise and update ID
            val id = workoutRepository.logSet(newSet)
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val index = mutableExercises.indexOfFirst { it.exercise.id == exerciseId }
                if (index != -1) {
                    val exState = mutableExercises[index]
                    mutableExercises[index] = exState.copy(sets = exState.sets.map { 
                        if (it.id == 0 && it.numeroSerie == 1) it.copy(id = id.toInt()) else it 
                    })
                }
                curr.copy(exercises = mutableExercises)
            }
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

            // Optimistic update
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                mutableExercises[index] = currentEx.copy(sets = currentEx.sets.map { it.copy(ordineEsercizio = finalPrevOrder) })
                mutableExercises[index - 1] = prevEx.copy(sets = prevEx.sets.map { it.copy(ordineEsercizio = finalCurrentOrder) })
                // Re-sort the list for UI
                curr.copy(exercises = mutableExercises.sortedBy { it.sets.firstOrNull()?.ordineEsercizio ?: 0 })
            }

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

            // Optimistic update
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                mutableExercises[index] = currentEx.copy(sets = currentEx.sets.map { it.copy(ordineEsercizio = finalNextOrder) })
                mutableExercises[index + 1] = nextEx.copy(sets = nextEx.sets.map { it.copy(ordineEsercizio = finalCurrentOrder) })
                // Re-sort the list for UI
                curr.copy(exercises = mutableExercises.sortedBy { it.sets.firstOrNull()?.ordineEsercizio ?: 0 })
            }

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
        // Use indexOf(set) to correctly identify the set even if multiple have id=0
        val index = exerciseState.sets.indexOf(set)
        if (index > 0) {
            val prevSet = exerciseState.sets[index - 1]
            
            // Optimistic update
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val exIndex = mutableExercises.indexOfFirst { it.exercise.id == set.exerciseId }
                if (exIndex != -1) {
                    val exState = mutableExercises[exIndex]
                    val mutableSets = exState.sets.toMutableList()
                    mutableSets[index] = set.copy(numeroSerie = prevSet.numeroSerie)
                    mutableSets[index - 1] = prevSet.copy(numeroSerie = set.numeroSerie)
                    mutableExercises[exIndex] = exState.copy(sets = mutableSets.sortedBy { it.numeroSerie })
                }
                curr.copy(exercises = mutableExercises)
            }

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
        // Use indexOf(set) to correctly identify the set even if multiple have id=0
        val index = exerciseState.sets.indexOf(set)
        if (index < exerciseState.sets.size - 1) {
            val nextSet = exerciseState.sets[index + 1]

            // Optimistic update
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val exIndex = mutableExercises.indexOfFirst { it.exercise.id == set.exerciseId }
                if (exIndex != -1) {
                    val exState = mutableExercises[exIndex]
                    val mutableSets = exState.sets.toMutableList()
                    mutableSets[index] = set.copy(numeroSerie = nextSet.numeroSerie)
                    mutableSets[index + 1] = nextSet.copy(numeroSerie = set.numeroSerie)
                    mutableExercises[exIndex] = exState.copy(sets = mutableSets.sortedBy { it.numeroSerie })
                }
                curr.copy(exercises = mutableExercises)
            }

            viewModelScope.launch {
                workoutRepository.updateSetOrders(listOf(
                    set.copy(numeroSerie = nextSet.numeroSerie),
                    nextSet.copy(numeroSerie = set.numeroSerie)
                ))
            }
        }
    }

    private suspend fun reorderSets(exerciseId: Int) {
        val exerciseState = _state.value.exercises.find { it.exercise.id == exerciseId } ?: return
        val sets = exerciseState.sets.sortedBy { it.numeroSerie }
        
        val updatedSets = sets.mapIndexed { index, setLogEntity ->
            setLogEntity.copy(numeroSerie = index + 1)
        }

        // Optimistic update for reordering
        _state.update { curr ->
            val mutableExercises = curr.exercises.toMutableList()
            val exIndex = mutableExercises.indexOfFirst { it.exercise.id == exerciseId }
            if (exIndex != -1) {
                mutableExercises[exIndex] = exerciseState.copy(sets = updatedSets)
            }
            curr.copy(exercises = mutableExercises)
        }
        
        workoutRepository.updateSetOrders(updatedSets)
    }

    fun updateSessionDate(newTimestamp: Long) {
        viewModelScope.launch {
            val sessionDetails = workoutRepository.getSessionWithDetails(sessionId).first()
            if (sessionDetails != null) {
                workoutRepository.updateSession(sessionDetails.session.copy(timestamp = newTimestamp))
            }
        }
    }
}
