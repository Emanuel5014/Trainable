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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
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
    val categories: List<String> = emptyList(),
    val sessionTimestamp: Long = 0L,
    val sessionDurationMs: Long? = null,
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

    val editablePresetExercises = userPreferencesRepository.editablePresetExercises.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    init {
        loadSessionData()
        loadAvailableExercises()
        loadCategories()
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
                                planName = sessionDetails.session.noteSessione ?: sessionDetails.plan.nome,
                                sessionTimestamp = sessionDetails.session.timestamp,
                                sessionDurationMs = sessionDetails.session.durationMs,
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

    private fun loadCategories() {
        viewModelScope.launch {
            exerciseRepository.getCategories().collect { categories ->
                _state.update { it.copy(categories = categories) }
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
            val maxOrder = maxOf(
                _state.value.exercises.maxOfOrNull { it.sets.firstOrNull()?.ordineEsercizio ?: 0 } ?: 0,
                _state.value.cardioLogs.maxOfOrNull { it.ordineEsercizio } ?: 0
            )
            val newCardio = CardioLogEntity(
                sessionId = sessionId,
                categoria = categoria,
                distanza = distanza,
                durataSecondi = durataSecondi,
                timestamp = _state.value.sessionTimestamp,
                ordineEsercizio = maxOrder + 1
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

    fun moveCardioUp(cardio: CardioLogEntity) {
        val allItems = mutableListOf<Pair<Int, Any>>()
        _state.value.exercises.forEach { ex ->
            allItems.add(Pair(ex.sets.firstOrNull()?.ordineEsercizio ?: 0, ex as Any))
        }
        _state.value.cardioLogs.forEach { c ->
            allItems.add(Pair(c.ordineEsercizio, c as Any))
        }
        allItems.sortBy { it.first }

        val cardioIndex = allItems.indexOfFirst { it.second == cardio }
        if (cardioIndex > 0) {
            val prevItem = allItems[cardioIndex - 1]
            val prevOrder = prevItem.first
            val cardioOrder = cardio.ordineEsercizio

            _state.update { curr ->
                val updatedCardioLogs = curr.cardioLogs.map {
                    if (it.id == cardio.id) it.copy(ordineEsercizio = prevOrder) else it
                }
                val updatedExercises = curr.exercises.map { exState ->
                    if (exState.sets.firstOrNull()?.ordineEsercizio == prevOrder) {
                        exState.copy(sets = exState.sets.map { it.copy(ordineEsercizio = cardioOrder) })
                    } else {
                        exState
                    }
                }
                curr.copy(exercises = updatedExercises, cardioLogs = updatedCardioLogs)
            }

            viewModelScope.launch {
                workoutRepository.updateCardioLog(cardio.copy(ordineEsercizio = prevOrder))
                if (prevItem.second is EditExerciseState) {
                    val exState = prevItem.second as EditExerciseState
                    val updatedSets = exState.sets.map { it.copy(ordineEsercizio = cardioOrder) }
                    workoutRepository.updateSetOrders(updatedSets)
                } else if (prevItem.second is CardioLogEntity) {
                    val prevCardio = prevItem.second as CardioLogEntity
                    workoutRepository.updateCardioLog(prevCardio.copy(ordineEsercizio = cardioOrder))
                }
            }
        }
    }

    fun moveCardioDown(cardio: CardioLogEntity) {
        val allItems = mutableListOf<Pair<Int, Any>>()
        _state.value.exercises.forEach { ex ->
            allItems.add(Pair(ex.sets.firstOrNull()?.ordineEsercizio ?: 0, ex as Any))
        }
        _state.value.cardioLogs.forEach { c ->
            allItems.add(Pair(c.ordineEsercizio, c as Any))
        }
        allItems.sortBy { it.first }

        val cardioIndex = allItems.indexOfFirst { it.second == cardio }
        if (cardioIndex < allItems.lastIndex) {
            val nextItem = allItems[cardioIndex + 1]
            val nextOrder = nextItem.first
            val cardioOrder = cardio.ordineEsercizio

            _state.update { curr ->
                val updatedCardioLogs = curr.cardioLogs.map {
                    if (it.id == cardio.id) it.copy(ordineEsercizio = nextOrder) else it
                }
                val updatedExercises = curr.exercises.map { exState ->
                    if (exState.sets.firstOrNull()?.ordineEsercizio == nextOrder) {
                        exState.copy(sets = exState.sets.map { it.copy(ordineEsercizio = cardioOrder) })
                    } else {
                        exState
                    }
                }
                curr.copy(exercises = updatedExercises, cardioLogs = updatedCardioLogs)
            }

            viewModelScope.launch {
                workoutRepository.updateCardioLog(cardio.copy(ordineEsercizio = nextOrder))
                if (nextItem.second is EditExerciseState) {
                    val exState = nextItem.second as EditExerciseState
                    val updatedSets = exState.sets.map { it.copy(ordineEsercizio = cardioOrder) }
                    workoutRepository.updateSetOrders(updatedSets)
                } else if (nextItem.second is CardioLogEntity) {
                    val nextCardio = nextItem.second as CardioLogEntity
                    workoutRepository.updateCardioLog(nextCardio.copy(ordineEsercizio = cardioOrder))
                }
            }
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
            val lastReps = exerciseState?.sets?.lastOrNull()?.repsEffettive ?: 8
            val lastDuration = exerciseState?.sets?.lastOrNull()?.durataSecondi
            val exerciseOrder = exerciseState?.sets?.firstOrNull()?.ordineEsercizio ?: 0
            val supersetId = exerciseState?.sets?.firstOrNull()?.supersetId

            val newSet = SetLogEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                pesoSollevato = lastWeight,
                repsEffettive = lastReps,
                numeroSerie = nextSerie,
                ordineEsercizio = exerciseOrder,
                supersetId = supersetId,
                durataSecondi = lastDuration
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

    fun swapExercise(
        oldExerciseId: Int,
        newExerciseId: Int,
        exerciseType: String = "strength",
        durataTargetSecondi: Int? = null
    ) {
        viewModelScope.launch {
            val oldExerciseState = _state.value.exercises.find { it.exercise.id == oldExerciseId } ?: return@launch
            val sets = oldExerciseState.sets
            val exerciseOrder = sets.firstOrNull()?.ordineEsercizio ?: 0
            val supersetId = sets.firstOrNull()?.supersetId
            val isTimeAndWeight = exerciseType == "time_and_weight"
            val targetSec = if (isTimeAndWeight) (durataTargetSecondi ?: 45) else null

            // Optimistic update
            val replacementExercise = _state.value.availableExercises.find { it.id == newExerciseId }
            if (replacementExercise != null) {
                _state.update { curr ->
                    val mutableExercises = curr.exercises.toMutableList()
                    val index = mutableExercises.indexOfFirst { it.exercise.id == oldExerciseId }
                    if (index != -1) {
                        mutableExercises[index] = EditExerciseState(
                            exercise = replacementExercise,
                            sets = sets.map {
                                it.copy(
                                    exerciseId = newExerciseId,
                                    id = 0,
                                    supersetId = supersetId,
                                    durataSecondi = if (isTimeAndWeight) (it.durataSecondi ?: targetSec) else null,
                                    repsEffettive = if (isTimeAndWeight && it.repsEffettive == 0) 0 else it.repsEffettive
                                )
                            }
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
                    set.copy(
                        id = 0,
                        exerciseId = newExerciseId,
                        ordineEsercizio = exerciseOrder,
                        supersetId = supersetId,
                        durataSecondi = if (isTimeAndWeight) (set.durataSecondi ?: targetSec) else null,
                        repsEffettive = if (isTimeAndWeight && set.repsEffettive == 0) 0 else set.repsEffettive
                    )
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

    fun swapExerciseWithCardio(oldExerciseId: Int, cardioCategory: String, durationMinutes: Int) {
        viewModelScope.launch {
            val oldExerciseState = _state.value.exercises.find { it.exercise.id == oldExerciseId }
            val order = oldExerciseState?.sets?.firstOrNull()?.ordineEsercizio ?: 0

            workoutRepository.deleteExerciseFromSession(sessionId, oldExerciseId)
            _state.update { curr ->
                curr.copy(exercises = curr.exercises.filter { it.exercise.id != oldExerciseId })
            }

            val newCardio = CardioLogEntity(
                sessionId = sessionId,
                categoria = cardioCategory,
                distanza = 0f,
                durataSecondi = durationMinutes * 60,
                timestamp = _state.value.sessionTimestamp,
                ordineEsercizio = order
            )
            val cardioId = workoutRepository.saveCardioLog(newCardio)
            _state.update { curr ->
                curr.copy(cardioLogs = curr.cardioLogs + newCardio.copy(id = cardioId.toInt()))
            }
        }
    }

    fun addExercise(
        exerciseId: Int,
        targetSets: Int = 1,
        repsTarget: String = "8",
        exerciseType: String = "strength",
        durataTargetSecondi: Int? = null
    ) {
        viewModelScope.launch {
            val nextOrder = (_state.value.exercises.maxOfOrNull { it.sets.firstOrNull()?.ordineEsercizio ?: 0 } ?: -1) + 1
            val exercise = _state.value.availableExercises.find { it.id == exerciseId }
            val isTimeAndWeight = exerciseType == "time_and_weight"
            val targetSec = if (isTimeAndWeight) (durataTargetSecondi ?: 45) else null
            val repsNum = if (isTimeAndWeight) 0 else (repsTarget.toIntOrNull() ?: 8)
            val setsCount = targetSets.coerceAtLeast(1)

            val newSets = (1..setsCount).map { setNum ->
                SetLogEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    pesoSollevato = 0f,
                    repsEffettive = repsNum,
                    numeroSerie = setNum,
                    ordineEsercizio = nextOrder,
                    supersetId = null,
                    durataSecondi = targetSec
                )
            }

            // Optimistic update
            if (exercise != null) {
                _state.update { curr ->
                    curr.copy(exercises = curr.exercises + EditExerciseState(exercise, newSets))
                }
            }

            // Add sets to repository and update state with real IDs
            val realIds = newSets.map { set ->
                workoutRepository.logSet(set).toInt()
            }

            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val index = mutableExercises.indexOfFirst { it.exercise.id == exerciseId }
                if (index != -1) {
                    val exState = mutableExercises[index]
                    mutableExercises[index] = exState.copy(
                        sets = exState.sets.mapIndexed { i, s ->
                            if (s.id == 0 && i < realIds.size) s.copy(id = realIds[i]) else s
                        }
                    )
                }
                curr.copy(exercises = mutableExercises)
            }
        }
    }

    fun addCustomExercise(name: String, category: String, onCreated: (ExerciseEntity) -> Unit = {}) {
        viewModelScope.launch {
            val exerciseId = exerciseRepository.addCustomExercise(name, category)
            val newEx = ExerciseEntity(id = exerciseId, nome = name, categoria = category)
            onCreated(newEx)
        }
    }

    fun updateCustomExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            exerciseRepository.saveExercise(exercise)
        }
    }

    fun deleteCustomExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            exerciseRepository.deleteExercise(exercise)
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

    fun toggleSupersetWithNext(exerciseId: Int) {
        viewModelScope.launch {
            val exercises = _state.value.exercises
            val index = exercises.indexOfFirst { it.exercise.id == exerciseId }
            if (index != -1 && index < exercises.size - 1) {
                val currentEx = exercises[index]
                val nextEx = exercises[index + 1]

                val currentSid = currentEx.sets.firstOrNull()?.supersetId
                val nextSid = nextEx.sets.firstOrNull()?.supersetId

                val isLinked = currentSid != null && currentSid == nextSid

                val newSid = if (isLinked) {
                    null
                } else {
                    currentSid ?: nextSid ?: java.util.UUID.randomUUID().toString()
                }

                val setsToUpdate = mutableListOf<SetLogEntity>()
                val updatedCurrentSets = currentEx.sets.map { it.copy(supersetId = newSid) }
                val updatedNextSets = nextEx.sets.map { it.copy(supersetId = newSid) }

                setsToUpdate.addAll(updatedCurrentSets)
                setsToUpdate.addAll(updatedNextSets)

                // Optimistic update
                _state.update { curr ->
                    val mutableExercises = curr.exercises.toMutableList()
                    mutableExercises[index] = currentEx.copy(sets = updatedCurrentSets)
                    mutableExercises[index + 1] = nextEx.copy(sets = updatedNextSets)
                    curr.copy(exercises = mutableExercises)
                }

                workoutRepository.updateSetOrders(setsToUpdate)
            }
        }
    }

    fun updateSessionName(name: String) {
        viewModelScope.launch {
            val sessionDetails = workoutRepository.getSessionWithDetails(sessionId).first()
            if (sessionDetails != null) {
                workoutRepository.updateSession(sessionDetails.session.copy(noteSessione = name.ifBlank { null }))
            }
        }
    }

    fun updateSessionDate(newTimestamp: Long) {
        viewModelScope.launch {
            val sessionDetails = workoutRepository.getSessionWithDetails(sessionId).first()
            if (sessionDetails != null) {
                workoutRepository.updateSession(sessionDetails.session.copy(timestamp = newTimestamp))
            }
        }
    }

    fun updateSessionDuration(durationMs: Long?) {
        viewModelScope.launch {
            val sessionDetails = workoutRepository.getSessionWithDetails(sessionId).first()
            if (sessionDetails != null) {
                workoutRepository.updateSession(sessionDetails.session.copy(durationMs = durationMs))
                _state.update { it.copy(sessionDurationMs = durationMs) }
            }
        }
    }

    fun updateItemsOrder(orderedItems: List<Any>) {
        viewModelScope.launch {
            val setsToUpdate = mutableListOf<SetLogEntity>()
            val cardioToUpdate = mutableListOf<CardioLogEntity>()
            val updatedExercises = mutableListOf<EditExerciseState>()
            val updatedCardioLogs = mutableListOf<CardioLogEntity>()

            orderedItems.forEachIndexed { index, item ->
                when (item) {
                    is EditExerciseState -> {
                        val updatedSets = item.sets.map { it.copy(ordineEsercizio = index) }
                        setsToUpdate.addAll(updatedSets)
                        updatedExercises.add(item.copy(sets = updatedSets))
                    }
                    is CardioLogEntity -> {
                        val updatedCardio = item.copy(ordineEsercizio = index)
                        cardioToUpdate.add(updatedCardio)
                        updatedCardioLogs.add(updatedCardio)
                    }
                }
            }

            // Optimistic update
            _state.update { curr ->
                curr.copy(
                    exercises = updatedExercises,
                    cardioLogs = updatedCardioLogs
                )
            }

            if (setsToUpdate.isNotEmpty()) {
                workoutRepository.updateSetOrders(setsToUpdate)
            }
            cardioToUpdate.forEach { cardio ->
                workoutRepository.updateCardioLog(cardio)
            }
        }
    }
}
