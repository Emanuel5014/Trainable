package com.emanuel5014.trainable.ui.screens.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SessionExerciseSwapEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import com.emanuel5014.trainable.util.notification.TimerNotificationHelper
import com.emanuel5014.trainable.util.notification.TimerNotificationReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WorkoutState(
    val isLoading: Boolean = true,
    val planId: Int? = null,
    val planName: String = "",
    val sessionId: Int? = null,
    val exercises: List<WorkoutExerciseState> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val remainingRestSeconds: Int = 0,
    val totalRestSeconds: Int = 90,
    val restTimerEndTime: Long? = null,
    val isFinished: Boolean = false,
    val isFinishing: Boolean = false,
    val isNavigating: Boolean = false,
    val exerciseSwaps: Map<Int, Int> = emptyMap(),
    val weightUnit: String = "kg",
    val timerNotificationsEnabled: Boolean = true,
    val isQuickWorkout: Boolean = false,
    val swipeActionsEnabled: Boolean = true,
    val warmupTimerEnabled: Boolean = false,
    val warmupTimerRemaining: Int = 0,
    val warmupTimerEndTime: Long? = null,
    val warmupTimerTotalSeconds: Int = 0
) {
    val currentExercise: WorkoutExerciseState?
        get() = exercises.getOrNull(currentExerciseIndex)

    val completedExercises: Int
        get() = exercises.count { ex -> ex.sets.all { it.isCompleted } }

    val totalExercises: Int
        get() = exercises.size
}

data class WorkoutExerciseState(
    val exercise: ExerciseEntity,
    val planDetails: PlanExerciseEntity?,
    val sets: List<WorkoutSetState> = emptyList(),
    val previousPerformance: String? = null,
    val swappedExerciseId: Int? = null,
    val customRestSeconds: Int? = null,
    val customRepsTarget: String? = null,
    val supersetId: String? = null
)

data class WorkoutSetState(
    val id: Int? = null,
    val setNumber: Int,
    val weight: Float,
    val reps: Int,
    val note: String? = null,
    val previousNote: String? = null,
    val previousReps: Int? = null,
    val previousWeight: Float? = null,
    val isCompleted: Boolean = false,
    val isWarmup: Boolean = false
)

data class NextSetInfo(
    val exerciseName: String,
    val setNumber: Int,
    val weight: Float,
    val reps: Int,
    val weightUnit: String,
    val previousReps: Int? = null
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exerciseRepository: ExerciseRepository,
    private val timerNotificationHelper: TimerNotificationHelper,
    private val localeManager: AppLocaleManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<WorkoutNavEvent>()
    val navigationEvent: SharedFlow<WorkoutNavEvent> = _navigationEvent.asSharedFlow()

    private var lastActionTime = 0L
    private val actionDebounce = 400L // 400ms hard debounce for physical clicks

    sealed class WorkoutNavEvent {
        object NavigateBack : WorkoutNavEvent()
    }

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _availableExercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    val availableExercises: StateFlow<List<ExerciseEntity>> = _availableExercises.asStateFlow()

    private var timerJob: Job? = null
    private var warmupTimerJob: Job? = null

    init {
        viewModelScope.launch {
            userPreferencesRepository.userLanguage.collect { _ ->
                _languageCode.value = localeManager.getResolvedLanguage()
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.timerNotificationsEnabled.collect { enabled ->
                _state.update { it.copy(timerNotificationsEnabled = enabled) }
                if (!enabled) {
                    timerNotificationHelper.cancelTimer()
                    timerNotificationHelper.cancelWarmupTimer()
                }
            }
        }

        viewModelScope.launch {
            TimerNotificationReceiver.timerEvents.collect { action ->
                when (action) {
                    TimerNotificationReceiver.TimerAction.SKIP -> skipRestTimer()
                    TimerNotificationReceiver.TimerAction.ADD_30S -> addRestTime(30)
                    TimerNotificationReceiver.TimerAction.DISMISS -> timerNotificationHelper.cancelTimer()
                    TimerNotificationReceiver.TimerAction.FINISHED -> handleTimerFinished()
                }
            }
        }

        viewModelScope.launch {
            TimerNotificationReceiver.warmupTimerEvents.collect { action ->
                when (action) {
                    TimerNotificationReceiver.WarmupTimerAction.SKIP -> skipWarmupTimer()
                    TimerNotificationReceiver.WarmupTimerAction.ADD_30S -> addWarmupTime(30)
                    TimerNotificationReceiver.WarmupTimerAction.DISMISS -> timerNotificationHelper.cancelWarmupTimer()
                    TimerNotificationReceiver.WarmupTimerAction.FINISHED -> handleWarmupTimerFinished()
                }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.weightUnit.collect { unit ->
                _state.update { it.copy(weightUnit = unit) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.swipeActionsEnabled.collect { enabled ->
                _state.update { it.copy(swipeActionsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.warmupTimerEnabled.collect { enabled ->
                _state.update { it.copy(warmupTimerEnabled = enabled) }
            }
        }
        
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _availableExercises.value = exercises
            }
        }

        val planId: Int? = savedStateHandle.get<Int>("planId")
        val sessionId: Int? = savedStateHandle.get<Int>("sessionId")
        val quickStart: Boolean = savedStateHandle.get<Boolean>("quickStart") ?: false
        val workoutName: String? = savedStateHandle.get<String>("workoutName")
        
        viewModelScope.launch {
            if (sessionId != null && sessionId != 0 && sessionId != -1) {
                resumeWorkout(sessionId)
            } else if (quickStart) {
                initializeQuickWorkout(workoutName)
            } else if (planId != null && planId != -1 && planId != 0) {
                initializeNewWorkout(planId)
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun resumeWorkout(sessionId: Int) {
        val sessionWithSets = workoutRepository.getSessionWithSets(sessionId).firstOrNull() ?: return
        val planId = sessionWithSets.session.planId
        val planWithDetails = workoutRepository.getPlanWithDetails(planId).firstOrNull() ?: return
        val planName = sessionWithSets.session.noteSessione ?: planWithDetails.plan.nome
        
        val swaps = workoutRepository.getSwapsForSession(sessionId).firstOrNull()
        val swapMap = swaps?.associate { it.originalExerciseId to it.replacementExerciseId } ?: emptyMap()

        // Load saved rest timer from session
        val savedEndTime = sessionWithSets.session.restTimerEndTime
        val savedTotalSeconds = sessionWithSets.session.totalRestSeconds
        val savedRemainingSeconds = savedEndTime?.let { 
            ((it - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        } ?: 0

        // Load saved warmup timer from session
        val savedWarmupEndTime = sessionWithSets.session.warmupTimerEndTime
        val savedWarmupTotalSeconds = sessionWithSets.session.totalWarmupSeconds
        val savedWarmupRemainingSeconds = savedWarmupEndTime?.let {
            ((it - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        } ?: 0

        val planExercises = planWithDetails.exercises.sortedBy { it.planExercise.ordine }
        
        val allAvailableExercises = _availableExercises.value.ifEmpty { 
            exerciseRepository.getAllExercises().firstOrNull() ?: emptyList() 
        }

        val useOrdine = sessionWithSets.sets.any { it.ordineEsercizio > 0 }

        suspend fun createExerciseState(exercise: ExerciseEntity, planDetail: PlanExerciseEntity?, exerciseIndex: Int, useOrdine: Boolean): WorkoutExerciseState {
            val previousSets = workoutRepository.getLastSessionSetsForExercise(planId, exercise.id, planDetail?.serieTarget ?: 3).firstOrNull()
            val prevPerfStr = if (!previousSets.isNullOrEmpty()) {
                val bestSet = previousSets.maxByOrNull { it.pesoSollevato }
                if (bestSet != null) "Last: ${bestSet.pesoSollevato}kg × ${bestSet.repsEffettive}" else null
            } else null

            // Load already completed or uncompleted sets for this session
            val loggedSets = if (useOrdine) {
                sessionWithSets.sets.filter { it.ordineEsercizio == exerciseIndex }
            } else {
                sessionWithSets.sets.filter { it.exerciseId == exercise.id }
            }
            
            val targetSets = if (planDetail == null && loggedSets.isNotEmpty()) {
                loggedSets.size
            } else {
                planDetail?.serieTarget ?: loggedSets.size.coerceAtLeast(3)
            }
            val repsList = if (previousSets.isNullOrEmpty()) {
                parseReps(planDetail?.repsTarget ?: "8", targetSets)
            } else {
                previousSets.map { it.repsEffettive }
            }
            val lastLoggedWeight = loggedSets.lastOrNull { it.isCompleted }?.pesoSollevato ?: previousSets?.lastOrNull()?.pesoSollevato ?: 0f

            val sets = (1..targetSets.coerceAtLeast(loggedSets.size)).map { num ->
                val loggedSet = loggedSets.find { it.numeroSerie == num }
                if (loggedSet != null) {
                    WorkoutSetState(
                        id = loggedSet.id,
                        setNumber = num,
                        weight = loggedSet.pesoSollevato,
                        reps = loggedSet.repsEffettive,
                        note = loggedSet.note,
                        isCompleted = loggedSet.isCompleted,
                        isWarmup = loggedSet.isWarmup
                    )
                } else {
                    val prevSet = previousSets?.getOrNull(num - 1)
                    WorkoutSetState(
                        setNumber = num,
                        weight = prevSet?.pesoSollevato ?: lastLoggedWeight,
                        reps = prevSet?.repsEffettive ?: repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 },
                        previousNote = prevSet?.note,
                        previousReps = prevSet?.repsEffettive,
                        previousWeight = prevSet?.pesoSollevato
                    )
                }
            }

            val restoredRestSeconds = if (planDetail == null) {
                loggedSets.firstOrNull()?.restTimerSeconds
            } else {
                null
            }

            return WorkoutExerciseState(
                exercise = exercise,
                planDetails = planDetail,
                sets = sets,
                previousPerformance = prevPerfStr,
                swappedExerciseId = planDetail?.id?.let { swapMap[it] },
                supersetId = planDetail?.supersetId ?: loggedSets.firstOrNull()?.supersetId,
                customRestSeconds = restoredRestSeconds
            )
        }

        val planExerciseStates = planExercises.mapIndexed { index, detail ->
            val swappedId = swapMap[detail.planExercise.id]
            val exercise = if (swappedId != null) {
                allAvailableExercises.find { it.id == swappedId } ?: detail.exercise
            } else {
                detail.exercise
            }
            createExerciseState(exercise, detail.planExercise, index, useOrdine)
        }

        val extraExerciseStates = if (useOrdine) {
            val extraSetsByOrder = sessionWithSets.sets
                .filter { it.ordineEsercizio >= planExercises.size }
                .groupBy { it.ordineEsercizio }
                .toSortedMap()
            
            extraSetsByOrder.map { (orderIdx, setsForExercise) ->
                val firstSet = setsForExercise.firstOrNull() ?: return@map null
                val exercise = allAvailableExercises.find { it.id == firstSet.exerciseId }
                if (exercise != null) {
                    createExerciseState(exercise, null, orderIdx, useOrdine)
                } else null
            }.filterNotNull()
        } else {
            val consumedExerciseIds = planExerciseStates.map { it.exercise.id }.toSet()
            val loggedExerciseIds = sessionWithSets.sets.map { it.exerciseId }.distinct()
            val extraExerciseIds = loggedExerciseIds.filter { it !in consumedExerciseIds }
            
            extraExerciseIds.mapIndexed { idx, exerciseId ->
                val exercise = allAvailableExercises.find { it.id == exerciseId } ?: return@mapIndexed null
                createExerciseState(exercise, null, planExercises.size + idx, useOrdine)
            }.filterNotNull()
        }

        val exerciseStates = planExerciseStates + extraExerciseStates

        val activeIndex = exerciseStates.indexOfFirst { exState ->
            exState.sets.any { !it.isCompleted }
        }.coerceAtLeast(0)
        val isQuick = planWithDetails.plan.note == "SYSTEM_PLAN" && (planWithDetails.plan.nome == "Quick Workout" || planWithDetails.plan.nome == "Allenamento Veloce")

        _state.update {
            it.copy(
                isLoading = false,
                planId = planId,
                planName = planName,
                sessionId = sessionId,
                exercises = exerciseStates,
                currentExerciseIndex = activeIndex,
                exerciseSwaps = swapMap,
                remainingRestSeconds = savedRemainingSeconds,
                totalRestSeconds = savedTotalSeconds ?: 90,
                restTimerEndTime = if (savedRemainingSeconds > 0) savedEndTime else null,
                isQuickWorkout = isQuick,
                warmupTimerRemaining = savedWarmupRemainingSeconds,
                warmupTimerEndTime = if (savedWarmupRemainingSeconds > 0) savedWarmupEndTime else null,
                warmupTimerTotalSeconds = savedWarmupTotalSeconds ?: 0
            )
        }

        // Resume timers if still valid
        if (savedRemainingSeconds > 0 && savedEndTime != null) {
            resumeRestTimer(savedEndTime)
        }
        if (savedWarmupRemainingSeconds > 0 && savedWarmupEndTime != null) {
            resumeWarmupTimer(savedWarmupEndTime)
        }
    }

    private suspend fun initializeNewWorkout(planId: Int) {
        val planWithDetails = workoutRepository.getPlanWithDetails(planId).firstOrNull() ?: return
        val planName = planWithDetails.plan.nome

        // Create New Session
        val sessionId = workoutRepository.startSession(planId, System.currentTimeMillis()).toInt()

        val exerciseStates = planWithDetails.exercises.sortedBy { it.planExercise.ordine }.map { detail ->
            val previousSets = workoutRepository.getLastSessionSetsForExercise(planId, detail.exercise.id, detail.planExercise.serieTarget).firstOrNull()
            val prevPerfStr = if (!previousSets.isNullOrEmpty()) {
                val bestSet = previousSets.maxByOrNull { it.pesoSollevato }
                if (bestSet != null) "Last: ${bestSet.pesoSollevato}kg × ${bestSet.repsEffettive}" else null
            } else null

            val targetSets = detail.planExercise.serieTarget
            val repsList = if (previousSets.isNullOrEmpty()) {
                parseReps(detail.planExercise.repsTarget, targetSets)
            } else {
                previousSets.map { it.repsEffettive }
            }
            val defaultWeight = previousSets?.lastOrNull()?.pesoSollevato ?: 0f

            val initialSets = (1..targetSets).map { num ->
                val prevSet = previousSets?.getOrNull(num - 1)
                WorkoutSetState(
                    setNumber = num,
                    weight = prevSet?.pesoSollevato ?: defaultWeight,
                    reps = prevSet?.repsEffettive ?: repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 },
                    previousNote = prevSet?.note,
                    previousReps = prevSet?.repsEffettive,
                    previousWeight = prevSet?.pesoSollevato
                )
            }

            WorkoutExerciseState(
                exercise = detail.exercise,
                planDetails = detail.planExercise,
                sets = initialSets,
                previousPerformance = prevPerfStr,
                supersetId = detail.planExercise.supersetId
            )
        }

        _state.update {
            it.copy(
                isLoading = false,
                planId = planId,
                planName = planName,
                sessionId = sessionId,
                exercises = exerciseStates,
                currentExerciseIndex = 0
            )
        }
    }

    private suspend fun initializeQuickWorkout(name: String?) {
        val sessionId = workoutRepository.startQuickWorkoutSession(name).toInt()
        val displayName = name ?: localeManager.getString(R.string.quick_workout)
        
        _state.update {
            it.copy(
                isLoading = false,
                planName = displayName,
                sessionId = sessionId,
                exercises = emptyList(),
                currentExerciseIndex = 0,
                isQuickWorkout = true
            )
        }
    }

    private fun parseReps(repsTarget: String, targetSets: Int): List<Int> {
        val parts = repsTarget.split("-").mapNotNull { it.trim().toIntOrNull() }
        return when {
            parts.isEmpty() -> List(targetSets) { 8 }
            parts.size == 1 -> List(targetSets) { parts[0] }
            parts.size >= targetSets -> parts.take(targetSets)
            else -> {
                val result = parts.toMutableList()
                while (result.size < targetSets) {
                    result.add(parts.last())
                }
                result
            }
        }
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: Float) {
        _state.update { curr ->
            val mutableExercises = curr.exercises.toMutableList()
            val exState = mutableExercises.getOrNull(exerciseIndex) ?: return@update curr
            val mutableSets = exState.sets.toMutableList()
            val set = mutableSets.getOrNull(setIndex) ?: return@update curr
            
            if (set.isCompleted) return@update curr

            // Update current set
            val updatedSet = set.copy(weight = weight)
            mutableSets[setIndex] = updatedSet
            
            if (updatedSet.id != null && curr.sessionId != null) {
                viewModelScope.launch {
                    workoutRepository.updateSet(
                        SetLogEntity(
                            id = updatedSet.id,
                            sessionId = curr.sessionId,
                            exerciseId = exState.exercise.id,
                            pesoSollevato = updatedSet.weight,
                            repsEffettive = updatedSet.reps,
                            numeroSerie = updatedSet.setNumber,
                            isWarmup = updatedSet.isWarmup,
                            note = updatedSet.note,
                            supersetId = exState.supersetId,
                            isCompleted = updatedSet.isCompleted,
                            ordineEsercizio = exerciseIndex,
                            restTimerSeconds = exState.customRestSeconds
                        )
                    )
                }
            }
            
            // Propagate to ALL subsequent uncompleted sets in THIS exercise
            // ONLY if there is no history for this exercise (new exercise or first time)
            if (exState.previousPerformance == null) {
                for (i in (setIndex + 1) until mutableSets.size) {
                    if (!mutableSets[i].isCompleted) {
                        val propSet = mutableSets[i].copy(weight = weight)
                        mutableSets[i] = propSet
                        if (propSet.id != null && curr.sessionId != null) {
                            viewModelScope.launch {
                                workoutRepository.updateSet(
                                    SetLogEntity(
                                        id = propSet.id,
                                        sessionId = curr.sessionId,
                                        exerciseId = exState.exercise.id,
                                        pesoSollevato = propSet.weight,
                                        repsEffettive = propSet.reps,
                                        numeroSerie = propSet.setNumber,
                                        isWarmup = propSet.isWarmup,
                                        note = propSet.note,
                                        supersetId = exState.supersetId,
                                        isCompleted = propSet.isCompleted,
                                        ordineEsercizio = exerciseIndex,
                                        restTimerSeconds = exState.customRestSeconds
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            mutableExercises[exerciseIndex] = exState.copy(sets = mutableSets)
            curr.copy(exercises = mutableExercises)
        }
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        _state.update { curr ->
            val mutableExercises = curr.exercises.toMutableList()
            val exState = mutableExercises.getOrNull(exerciseIndex) ?: return@update curr
            val mutableSets = exState.sets.toMutableList()
            val set = mutableSets.getOrNull(setIndex) ?: return@update curr

            if (set.isCompleted) return@update curr

            val updatedSet = set.copy(reps = reps)
            mutableSets[setIndex] = updatedSet
            
            if (updatedSet.id != null && curr.sessionId != null) {
                viewModelScope.launch {
                    workoutRepository.updateSet(
                        SetLogEntity(
                            id = updatedSet.id,
                            sessionId = curr.sessionId,
                            exerciseId = exState.exercise.id,
                            pesoSollevato = updatedSet.weight,
                            repsEffettive = updatedSet.reps,
                            numeroSerie = updatedSet.setNumber,
                            isWarmup = updatedSet.isWarmup,
                            note = updatedSet.note,
                            supersetId = exState.supersetId,
                            isCompleted = updatedSet.isCompleted,
                            ordineEsercizio = exerciseIndex,
                            restTimerSeconds = exState.customRestSeconds
                        )
                    )
                }
            }
            
            mutableExercises[exerciseIndex] = exState.copy(sets = mutableSets)
            curr.copy(exercises = mutableExercises)
        }
    }

    fun updateSetNote(exerciseIndex: Int, setIndex: Int, note: String) {
        updateSetState(exerciseIndex, setIndex) { it.copy(note = note) }
        val currentState = _state.value
        val exState = currentState.exercises[exerciseIndex]
        val setState = exState.sets[setIndex]
        
        if (setState.isCompleted && setState.id != null) {
            viewModelScope.launch {
                workoutRepository.updateSet(
                    SetLogEntity(
                        id = setState.id,
                        sessionId = currentState.sessionId ?: 0,
                        exerciseId = exState.exercise.id,
                        pesoSollevato = setState.weight,
                        repsEffettive = setState.reps,
                        numeroSerie = setState.setNumber,
                        isWarmup = setState.isWarmup,
                        note = note,
                        supersetId = exState.supersetId,
                        ordineEsercizio = exerciseIndex,
                        restTimerSeconds = exState.customRestSeconds
                    )
                )
            }
        }
    }

    fun toggleSetComplete(exerciseIndex: Int, setIndex: Int) {
        val currentState = _state.value
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val setState = exState.sets.getOrNull(setIndex) ?: return
        
        val newIsCompleted = !setState.isCompleted

        viewModelScope.launch {
            var newSetId = setState.id

            if (currentState.sessionId != null) {
                val logId = workoutRepository.logSet(
                    SetLogEntity(
                        id = setState.id ?: 0,
                        sessionId = currentState.sessionId,
                        exerciseId = exState.exercise.id,
                        pesoSollevato = setState.weight,
                        repsEffettive = setState.reps,
                        numeroSerie = setState.setNumber,
                        isWarmup = setState.isWarmup,
                        note = setState.note,
                        supersetId = exState.supersetId,
                        isCompleted = newIsCompleted,
                        ordineEsercizio = exerciseIndex,
                        restTimerSeconds = exState.customRestSeconds
                    )
                )
                newSetId = logId.toInt()
                
                val isLastExercise = exerciseIndex == currentState.exercises.size - 1
                val isLastSet = setIndex == exState.sets.size - 1
                
                var shouldStartTimer = true
                if (exState.supersetId != null) {
                    val setNumber = setState.setNumber
                    val supersetId = exState.supersetId
                    val supersetExercises = currentState.exercises.filter { it.supersetId == supersetId }
                    val otherExercises = supersetExercises.filter { it.exercise.id != exState.exercise.id }
                    val allOthersCompleted = otherExercises.all { otherEx ->
                        val otherSet = otherEx.sets.find { it.setNumber == setNumber }
                        otherSet == null || otherSet.isCompleted
                    }
                    if (!allOthersCompleted) {
                        shouldStartTimer = false
                    }
                }
                
                if (newIsCompleted) {
                    if (shouldStartTimer && !(isLastExercise && isLastSet)) {
                        val restTime = exState.customRestSeconds ?: exState.planDetails?.recuperoTarget ?: 90
                        val nextSet = exState.sets.drop(setIndex + 1).firstOrNull { !it.isCompleted }
                        if (nextSet != null) {
                            val translatedName = ExerciseTranslations.translate(exState.exercise.nome, _languageCode.value)
                            val plannedReps = getPlannedRepsForSet(exState, nextSet.setNumber) ?: nextSet.reps
                            startRestTimer(restTime, translatedName, nextSet.setNumber, nextSet.weight, plannedReps, nextSet.previousReps, currentState.weightUnit)
                        } else if (!isLastExercise) {
                            val nextExState = currentState.exercises.getOrNull(exerciseIndex + 1)
                            val firstSet = nextExState?.sets?.firstOrNull()
                            if (firstSet != null) {
                                val translatedName = ExerciseTranslations.translate(nextExState.exercise.nome, _languageCode.value)
                                val plannedReps = getPlannedRepsForSet(nextExState, firstSet.setNumber) ?: firstSet.reps
                                startRestTimer(restTime, translatedName, firstSet.setNumber, firstSet.weight, plannedReps, firstSet.previousReps, currentState.weightUnit)
                            } else {
                                startRestTimer(restTime)
                            }
                        } else {
                            startRestTimer(restTime)
                        }
                    }
                } else {
                    stopRestTimer()
                }
            }

            updateSetState(exerciseIndex, setIndex) { 
                it.copy(isCompleted = newIsCompleted, id = newSetId) 
            }

            // --- AUTO-NAVIGATION FOR SUPERSETS ---
            if (newIsCompleted && exState.supersetId != null) {
                val exercises = _state.value.exercises
                val supersetId = exState.supersetId
                
                // Find all exercises in this superset block
                val supersetBlock = exercises.filter { it.supersetId == supersetId }
                if (supersetBlock.size > 1) {
                    // Find the next exercise in the block that has uncompleted sets
                    // We start looking from the exercise AFTER the current one, and wrap around
                    val blockIndices = exercises.indices.filter { exercises[it].supersetId == supersetId }
                    val currentPosInBlock = blockIndices.indexOf(exerciseIndex)
                    
                    for (i in 1 until blockIndices.size) {
                        val nextIndex = blockIndices[(currentPosInBlock + i) % blockIndices.size]
                        val nextEx = exercises[nextIndex]
                        if (nextEx.sets.any { !it.isCompleted }) {
                            // Found it! Navigate after a short delay to allow the user to see the completion
                            viewModelScope.launch {
                                delay(300)
                                _state.update { it.copy(currentExerciseIndex = nextIndex) }
                            }
                            break
                        }
                    }
                }
            }
        }
    }
    private fun updateSetState(exerciseIndex: Int, setIndex: Int, updateFun: (WorkoutSetState) -> WorkoutSetState) {
        _state.update { curr ->
            val mutableExercises = curr.exercises.toMutableList()
            val exState = mutableExercises[exerciseIndex]
            val mutableSets = exState.sets.toMutableList()
            val updatedSet = updateFun(mutableSets[setIndex])
            mutableSets[setIndex] = updatedSet
            
            if (updatedSet.id != null && curr.sessionId != null) {
                viewModelScope.launch {
                    workoutRepository.updateSet(
                        SetLogEntity(
                            id = updatedSet.id,
                            sessionId = curr.sessionId,
                            exerciseId = exState.exercise.id,
                            pesoSollevato = updatedSet.weight,
                            repsEffettive = updatedSet.reps,
                            numeroSerie = updatedSet.setNumber,
                            isWarmup = updatedSet.isWarmup,
                            note = updatedSet.note,
                            supersetId = exState.supersetId,
                            isCompleted = updatedSet.isCompleted,
                            ordineEsercizio = exerciseIndex,
                            restTimerSeconds = exState.customRestSeconds
                        )
                    )
                }
            }
            
            mutableExercises[exerciseIndex] = exState.copy(sets = mutableSets)
            curr.copy(exercises = mutableExercises)
        }
    }

    fun previousExercise() {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < actionDebounce) return
        lastActionTime = now

        if (_state.value.isNavigating || _state.value.isFinishing) return
        
        val currentIndex = _state.value.currentExerciseIndex
        if (currentIndex > 0) {
            _state.update { it.copy(isNavigating = true) }
            _state.update { it.copy(currentExerciseIndex = currentIndex - 1, isNavigating = false) }
        }
    }

    fun nextExercise() {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < actionDebounce) return
        lastActionTime = now

        if (_state.value.isNavigating || _state.value.isFinishing) return
        
        val currentIndex = _state.value.currentExerciseIndex
        val maxIndex = _state.value.exercises.size - 1
        if (currentIndex < maxIndex) {
            _state.update { it.copy(isNavigating = true) }
            _state.update { it.copy(currentExerciseIndex = currentIndex + 1, isNavigating = false) }
        }
    }

    fun finishWorkout() {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < actionDebounce) return
        lastActionTime = now

        if (_state.value.isFinishing) return
        _state.update { it.copy(isFinishing = true) }
        
        viewModelScope.launch {
            try {
                _state.value.sessionId?.let { id ->
                    withContext(Dispatchers.IO) {
                        workoutRepository.deleteUncompletedSetsForSession(id)
                        workoutRepository.setSessionFinished(id)
                    }
                    stopRestTimer()
                    stopWarmupTimer()
                    _state.update { it.copy(isFinished = true, isFinishing = false) }
                    _navigationEvent.emit(WorkoutNavEvent.NavigateBack)
                } ?: run {
                    _state.update { it.copy(isFinishing = false) }
                    _navigationEvent.emit(WorkoutNavEvent.NavigateBack)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isFinishing = false) }
            }
        }
    }

    private fun startRestTimer(
        seconds: Int,
        exerciseName: String? = null,
        nextSetNumber: Int? = null,
        nextSetWeight: Float? = null,
        nextSetReps: Int? = null,
        previousReps: Int? = null,
        weightUnit: String? = null
    ) {
        if (seconds <= 0) return
        stopRestTimer()
        val endTime = System.currentTimeMillis() + (seconds * 1000L)
        _state.update { it.copy(remainingRestSeconds = seconds, totalRestSeconds = seconds, restTimerEndTime = endTime) }
        if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
            _state.value.sessionId?.let { sessionId ->
                timerNotificationHelper.startOrUpdateTimerNotification(seconds, sessionId, exerciseName, nextSetNumber, nextSetWeight, nextSetReps, previousReps, weightUnit)
            }
        }
        saveTimerToSession(endTime, seconds)
        startTimerJob()
    }

    private fun getPlannedRepsForSet(exState: WorkoutExerciseState?, setNumber: Int): Int? {
        val plan = exState?.planDetails ?: return null
        val repsList = parseReps(plan.repsTarget, plan.serieTarget)
        return repsList.getOrElse(setNumber - 1) { repsList.lastOrNull() }
    }

    private fun buildNextSetLabel(): NextSetInfo? {
        val currentEx = _state.value.currentExercise ?: return null
        val currentState = _state.value
        val nextSet = currentEx.sets.firstOrNull { !it.isCompleted }
        if (nextSet != null) {
            val plannedReps = getPlannedRepsForSet(currentEx, nextSet.setNumber) ?: nextSet.reps
            return NextSetInfo(
                exerciseName = ExerciseTranslations.translate(currentEx.exercise.nome, _languageCode.value),
                setNumber = nextSet.setNumber,
                weight = nextSet.weight,
                reps = plannedReps,
                weightUnit = currentState.weightUnit,
                previousReps = nextSet.previousReps
            )
        }
        return null
    }

    private fun resumeRestTimer(endTime: Long) {
        stopRestTimer()
        val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        val totalSeconds = _state.value.totalRestSeconds
        _state.update { it.copy(remainingRestSeconds = remaining, restTimerEndTime = endTime) }
        if (remaining > 0) {
            if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
                _state.value.sessionId?.let { sessionId ->
                    val info = buildNextSetLabel()
                    timerNotificationHelper.startOrUpdateTimerNotification(
                        remaining, sessionId,
                        info?.exerciseName, info?.setNumber, info?.weight, info?.reps, info?.previousReps, info?.weightUnit
                    )
                }
            }
            startTimerJob()
        } else {
            clearTimerInSession()
        }
    }

    private fun handleTimerFinished() {
        if (_state.value.restTimerEndTime != null) {
            _state.update { it.copy(remainingRestSeconds = 0, restTimerEndTime = null) }
            timerNotificationHelper.cancelFinishAlarm()
            if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
                timerNotificationHelper.showRestFinished()
            }
            clearTimerInSession()
            timerJob?.cancel()
            timerJob = null
        }
    }

    private fun startTimerJob() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(100L)
                val end = _state.value.restTimerEndTime ?: break
                val remaining = ((end - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                
                if (remaining == 0) {
                    handleTimerFinished()
                    break
                }
                
                if (remaining != _state.value.remainingRestSeconds) {
                    // We no longer call showOrUpdateTimer(remaining) here 
                    // because the System UI Chronometer handles the countdown.
                    _state.update { it.copy(remainingRestSeconds = remaining) }
                }
            }
        }
    }

    private fun saveTimerToSession(endTime: Long?, totalSeconds: Int?) {
        viewModelScope.launch {
            _state.value.sessionId?.let { sessionId ->
                workoutRepository.updateRestTimer(sessionId, endTime, totalSeconds)
            }
        }
    }

    private fun clearTimerInSession() {
        saveTimerToSession(null, null)
    }

    fun addRestTime(seconds: Int) {
        val currentEnd = _state.value.restTimerEndTime
        if (currentEnd != null) {
            val newEnd = currentEnd + (seconds * 1000L)
            val newRemaining = ((newEnd - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            val newTotal = _state.value.totalRestSeconds + seconds
            _state.update { it.copy(restTimerEndTime = newEnd, remainingRestSeconds = newRemaining, totalRestSeconds = newTotal) }
            if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
                _state.value.sessionId?.let { sessionId ->
                    val info = buildNextSetLabel()
                    timerNotificationHelper.startOrUpdateTimerNotification(
                        newRemaining, sessionId,
                        info?.exerciseName, info?.setNumber, info?.weight, info?.reps, info?.previousReps, info?.weightUnit
                    )
                }
            }
            saveTimerToSession(newEnd, newTotal)
        }
    }

    fun skipRestTimer() {
        stopRestTimer()
        clearTimerInSession()
    }

    // ---- Warmup / General Timer ----

    fun startWarmupTimer(seconds: Int) {
        if (seconds <= 0) return
        stopWarmupTimer()
        val endTime = System.currentTimeMillis() + (seconds * 1000L)
        _state.update { it.copy(warmupTimerRemaining = seconds, warmupTimerEndTime = endTime, warmupTimerTotalSeconds = seconds) }
        if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
            timerNotificationHelper.startOrUpdateWarmupTimerNotification(seconds)
        }
        saveWarmupTimerToSession(endTime, seconds)
        startWarmupTimerJob()
    }

    fun skipWarmupTimer() {
        stopWarmupTimer()
        clearWarmupTimerInSession()
    }

    fun addWarmupTime(seconds: Int) {
        val currentEnd = _state.value.warmupTimerEndTime
        if (currentEnd != null) {
            val newEnd = currentEnd + (seconds * 1000L)
            val newRemaining = ((newEnd - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            val newTotal = _state.value.warmupTimerTotalSeconds + seconds
            _state.update { it.copy(warmupTimerEndTime = newEnd, warmupTimerRemaining = newRemaining, warmupTimerTotalSeconds = newTotal) }
            if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
                timerNotificationHelper.startOrUpdateWarmupTimerNotification(newRemaining)
            }
            saveWarmupTimerToSession(newEnd, newTotal)
        }
    }

    private fun stopWarmupTimer() {
        warmupTimerJob?.cancel()
        warmupTimerJob = null
        timerNotificationHelper.cancelWarmupTimer()
        _state.update { it.copy(warmupTimerRemaining = 0, warmupTimerEndTime = null, warmupTimerTotalSeconds = 0) }
    }

    private fun handleWarmupTimerFinished() {
        if (_state.value.warmupTimerEndTime != null) {
            _state.update { it.copy(warmupTimerRemaining = 0, warmupTimerEndTime = null, warmupTimerTotalSeconds = 0) }
            timerNotificationHelper.cancelWarmupFinishAlarm()
            if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
                timerNotificationHelper.showWarmupTimerFinished()
            }
            warmupTimerJob?.cancel()
            warmupTimerJob = null
            clearWarmupTimerInSession()
        }
    }

    private fun startWarmupTimerJob() {
        warmupTimerJob = viewModelScope.launch {
            while (true) {
                delay(100L)
                val end = _state.value.warmupTimerEndTime ?: break
                val remaining = ((end - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)

                if (remaining == 0) {
                    handleWarmupTimerFinished()
                    break
                }

                if (remaining != _state.value.warmupTimerRemaining) {
                    _state.update { it.copy(warmupTimerRemaining = remaining) }
                }
            }
        }
    }

    private fun saveWarmupTimerToSession(endTime: Long?, totalSeconds: Int?) {
        viewModelScope.launch {
            _state.value.sessionId?.let { sessionId ->
                workoutRepository.updateWarmupTimer(sessionId, endTime, totalSeconds)
            }
        }
    }

    private fun clearWarmupTimerInSession() {
        saveWarmupTimerToSession(null, null)
    }

    private fun resumeWarmupTimer(endTime: Long) {
        stopWarmupTimer()
        val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        val totalSeconds = _state.value.warmupTimerTotalSeconds
        _state.update { it.copy(warmupTimerRemaining = remaining, warmupTimerEndTime = endTime, warmupTimerTotalSeconds = totalSeconds) }
        if (remaining > 0) {
            if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
                timerNotificationHelper.startOrUpdateWarmupTimerNotification(remaining)
            }
            startWarmupTimerJob()
        } else {
            clearWarmupTimerInSession()
        }
    }

    fun swapExercise(exerciseIndex: Int, newExerciseId: Int, targetSets: Int, repsTarget: String, restTimer: Int? = null) {
        val currentState = _state.value
        val sessionId = currentState.sessionId ?: return
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val originalExerciseId = exState.planDetails?.id

        val replacementExercise = _availableExercises.value.find { it.id == newExerciseId } ?: return

        viewModelScope.launch {
            // Delete any existing completed/saved sets of the old exercise from the database for this session
            workoutRepository.deleteExerciseFromSession(sessionId, exState.exercise.id)
            
            if (originalExerciseId != null) {
                workoutRepository.saveExerciseSwap(
                    SessionExerciseSwapEntity(
                        sessionId = sessionId,
                        originalExerciseId = originalExerciseId,
                        replacementExerciseId = newExerciseId
                    )
                )
                
                val repsList = parseReps(repsTarget, targetSets)
                val defaultWeight = 0f

                val newSets = (1..targetSets).map { num ->
                    WorkoutSetState(
                        setNumber = num,
                        weight = defaultWeight,
                        reps = repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 }
                    )
                }

                _state.update { curr ->
                    val mutableExercises = curr.exercises.toMutableList()
                    val mutableSwaps = curr.exerciseSwaps.toMutableMap()
                    mutableSwaps[originalExerciseId] = newExerciseId
                    
                    mutableExercises[exerciseIndex] = exState.copy(
                        exercise = replacementExercise,
                        swappedExerciseId = newExerciseId,
                        sets = newSets,
                        previousPerformance = null,
                        customRestSeconds = restTimer,
                        customRepsTarget = repsTarget
                    )
                    curr.copy(exercises = mutableExercises, exerciseSwaps = mutableSwaps)
                }
            } else {
                // For quick workouts, write the new uncompleted sets to the database so they persist upon reload
                val repsList = parseReps(repsTarget, targetSets)
                val defaultWeight = 0f
                val initialSets = (1..targetSets).map { num ->
                    val reps = repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 }
                    val setLog = SetLogEntity(
                        sessionId = sessionId,
                        exerciseId = newExerciseId,
                        pesoSollevato = defaultWeight,
                        repsEffettive = reps,
                        numeroSerie = num,
                        isCompleted = false,
                        ordineEsercizio = exerciseIndex,
                        restTimerSeconds = restTimer
                    )
                    val logId = workoutRepository.logSet(setLog)
                    WorkoutSetState(
                        id = logId.toInt(),
                        setNumber = num,
                        weight = defaultWeight,
                        reps = reps,
                        isCompleted = false
                    )
                }
                
                _state.update { curr ->
                    val mutableExercises = curr.exercises.toMutableList()
                    mutableExercises[exerciseIndex] = exState.copy(
                        exercise = replacementExercise,
                        swappedExerciseId = newExerciseId,
                        sets = initialSets,
                        previousPerformance = null,
                        customRestSeconds = restTimer,
                        customRepsTarget = repsTarget
                    )
                    curr.copy(exercises = mutableExercises)
                }
            }
        }
    }

    fun getSwappedExerciseId(originalExerciseId: Int): Int? {
        return _state.value.exerciseSwaps[originalExerciseId]
    }

    fun toggleSupersetWithNext(exerciseIndex: Int) {
        val currentState = _state.value
        val exercises = currentState.exercises
        if (exerciseIndex >= 0 && exerciseIndex < exercises.size - 1) {
            val currentEx = exercises[exerciseIndex]
            val nextEx = exercises[exerciseIndex + 1]

            val currentSid = currentEx.supersetId
            val nextSid = nextEx.supersetId

            val isLinked = currentSid != null && currentSid == nextSid

            val newSid = if (isLinked) {
                null
            } else {
                currentSid ?: nextSid ?: java.util.UUID.randomUUID().toString()
            }

            viewModelScope.launch {
                // Update current exercise sets in DB
                currentEx.sets.forEach { s ->
                    if (s.id != null) {
                        workoutRepository.updateSet(
                            SetLogEntity(
                                id = s.id,
                                sessionId = currentState.sessionId ?: 0,
                                exerciseId = currentEx.exercise.id,
                                pesoSollevato = s.weight,
                                repsEffettive = s.reps,
                                numeroSerie = s.setNumber,
                                isWarmup = s.isWarmup,
                                note = s.note,
                                supersetId = newSid,
                                isCompleted = s.isCompleted,
                                ordineEsercizio = exerciseIndex,
                                restTimerSeconds = currentEx.customRestSeconds
                            )
                        )
                    }
                }
                
                // Update next exercise sets in DB
                nextEx.sets.forEach { s ->
                    if (s.id != null) {
                        workoutRepository.updateSet(
                            SetLogEntity(
                                id = s.id,
                                sessionId = currentState.sessionId ?: 0,
                                exerciseId = nextEx.exercise.id,
                                pesoSollevato = s.weight,
                                repsEffettive = s.reps,
                                numeroSerie = s.setNumber,
                                isWarmup = s.isWarmup,
                                note = s.note,
                                supersetId = newSid,
                                isCompleted = s.isCompleted,
                                ordineEsercizio = exerciseIndex + 1,
                                restTimerSeconds = nextEx.customRestSeconds
                            )
                        )
                    }
                }
            }

            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                mutableExercises[exerciseIndex] = currentEx.copy(supersetId = newSid)
                mutableExercises[exerciseIndex + 1] = nextEx.copy(supersetId = newSid)
                curr.copy(exercises = mutableExercises)
            }
        }
    }

    private fun stopRestTimer() {
        timerJob?.cancel()
        timerJob = null
        timerNotificationHelper.cancelTimer()
        _state.update { it.copy(remainingRestSeconds = 0, restTimerEndTime = null) }
    }

    fun cancelCustomVibration() {
        timerNotificationHelper.cancelCustomVibration()
    }

    fun cancelWorkout(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value.sessionId?.let { sessionId ->
                workoutRepository.deleteSession(sessionId)
            }
            stopRestTimer()
            stopWarmupTimer()
            onComplete()
        }
    }

    fun addCustomExercise(nome: String, categoria: String) {
        viewModelScope.launch {
            exerciseRepository.addCustomExercise(nome, categoria)
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
    fun addExerciseToActiveSession(exercise: ExerciseEntity, targetSets: Int = 3, repsTarget: String = "8", restTimer: Int? = 90) {
        val repsList = parseReps(repsTarget, targetSets)
        val sessionId = _state.value.sessionId ?: return

        viewModelScope.launch {
            val newIndex = _state.value.exercises.size
            val initialSets = (1..targetSets).map { num ->
                val weight = 0f
                val reps = repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 }
                val setLog = SetLogEntity(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    pesoSollevato = weight,
                    repsEffettive = reps,
                    numeroSerie = num,
                    isCompleted = false,
                    ordineEsercizio = newIndex,
                    restTimerSeconds = restTimer
                )
                val logId = workoutRepository.logSet(setLog)
                WorkoutSetState(
                    id = logId.toInt(),
                    setNumber = num,
                    weight = weight,
                    reps = reps,
                    isCompleted = false
                )
            }

            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val actualIndex = mutableExercises.size
                
                if (actualIndex != newIndex) {
                    viewModelScope.launch {
                        initialSets.forEach { s ->
                            if (s.id != null) {
                                workoutRepository.updateSet(
                            SetLogEntity(
                                id = s.id,
                                sessionId = sessionId,
                                exerciseId = exercise.id,
                                pesoSollevato = s.weight,
                                repsEffettive = s.reps,
                                numeroSerie = s.setNumber,
                                isCompleted = s.isCompleted,
                                ordineEsercizio = actualIndex,
                                restTimerSeconds = restTimer
                            )
                                )
                            }
                        }
                    }
                }

                mutableExercises.add(
                    WorkoutExerciseState(
                        exercise = exercise,
                        planDetails = null,
                        sets = initialSets,
                        customRestSeconds = restTimer,
                        customRepsTarget = repsTarget
                    )
                )
                curr.copy(
                    exercises = mutableExercises,
                    currentExerciseIndex = actualIndex
                )
            }
        }
    }

    fun addSetToExercise(exerciseIndex: Int) {
        val currentState = _state.value
        val sessionId = currentState.sessionId ?: return
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val mutableSets = exState.sets.toMutableList()
        
        val lastSet = mutableSets.lastOrNull()
        val newSetNumber = (lastSet?.setNumber ?: 0) + 1
        val weight = lastSet?.weight ?: 0f
        val reps = lastSet?.reps ?: 8

        viewModelScope.launch {
            val setLog = SetLogEntity(
                sessionId = sessionId,
                exerciseId = exState.exercise.id,
                pesoSollevato = weight,
                repsEffettive = reps,
                numeroSerie = newSetNumber,
                isCompleted = false,
                ordineEsercizio = exerciseIndex,
                restTimerSeconds = exState.customRestSeconds
            )
            val logId = workoutRepository.logSet(setLog)
            val newSet = WorkoutSetState(
                id = logId.toInt(),
                setNumber = newSetNumber,
                weight = weight,
                reps = reps,
                isCompleted = false
            )
            
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val innerExState = mutableExercises.getOrNull(exerciseIndex) ?: return@update curr
                val innerSets = innerExState.sets.toMutableList()
                innerSets.add(newSet)
                mutableExercises[exerciseIndex] = innerExState.copy(sets = innerSets)
                curr.copy(exercises = mutableExercises)
            }
        }
    }

    fun removeSetFromExercise(exerciseIndex: Int, setIndex: Int) {
        val currentState = _state.value
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val setState = exState.sets.getOrNull(setIndex) ?: return

        viewModelScope.launch {
            if (setState.id != null) {
                workoutRepository.deleteSet(
                    SetLogEntity(
                        id = setState.id,
                        sessionId = currentState.sessionId ?: 0,
                        exerciseId = exState.exercise.id,
                        pesoSollevato = setState.weight,
                        repsEffettive = setState.reps,
                        numeroSerie = setState.setNumber,
                        isWarmup = setState.isWarmup,
                        note = setState.note,
                        supersetId = exState.supersetId,
                        isCompleted = setState.isCompleted,
                        ordineEsercizio = exerciseIndex
                    )
                )
            }
            
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val innerExState = mutableExercises[exerciseIndex]
                val mutableSets = innerExState.sets.toMutableList()
                mutableSets.removeAt(setIndex)
                
                // Re-number subsequent sets
                val renumberedSets = mutableSets.mapIndexed { idx, s ->
                    s.copy(setNumber = idx + 1)
                }
                
                // Update renumbered sets in database
                viewModelScope.launch {
                    renumberedSets.forEach { s ->
                        if (s.id != null) {
                        workoutRepository.updateSet(
                            SetLogEntity(
                                id = s.id,
                                sessionId = currentState.sessionId ?: 0,
                                exerciseId = exState.exercise.id,
                                pesoSollevato = s.weight,
                                repsEffettive = s.reps,
                                numeroSerie = s.setNumber,
                                isWarmup = s.isWarmup,
                                note = s.note,
                                supersetId = exState.supersetId,
                                isCompleted = s.isCompleted,
                                ordineEsercizio = exerciseIndex,
                                restTimerSeconds = exState.customRestSeconds
                            )
                        )
                    }
                }
            }
            
            mutableExercises[exerciseIndex] = innerExState.copy(sets = renumberedSets)
            curr.copy(exercises = mutableExercises)
            }
        }
    }
    fun updateSessionName(newName: String) {
        val sessionId = _state.value.sessionId ?: return
        viewModelScope.launch {
            val session = workoutRepository.getSessionWithSets(sessionId).firstOrNull()?.session ?: return@launch
            workoutRepository.updateSession(session.copy(noteSessione = newName))
            _state.update { it.copy(planName = newName) }
        }
    }
}
