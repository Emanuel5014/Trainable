package com.emanuel5014.trainable.ui.screens.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SessionExerciseSwapEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.TimerNotificationHelper
import com.emanuel5014.trainable.util.TimerNotificationReceiver
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
import kotlinx.coroutines.flow.collectLatest
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
    val timerNotificationsEnabled: Boolean = true
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
    val swappedExerciseId: Int? = null
)

data class WorkoutSetState(
    val id: Int? = null,
    val setNumber: Int,
    val weight: Float,
    val reps: Int,
    val note: String? = null,
    val isCompleted: Boolean = false,
    val isWarmup: Boolean = false
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exerciseRepository: ExerciseRepository,
    private val timerNotificationHelper: TimerNotificationHelper,
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

    init {
        viewModelScope.launch {
            userPreferencesRepository.userLanguage.collect { lang ->
                _languageCode.value = lang ?: "en"
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.timerNotificationsEnabled.collect { enabled ->
                _state.update { it.copy(timerNotificationsEnabled = enabled) }
                if (!enabled) {
                    timerNotificationHelper.cancelTimer()
                }
            }
        }

        viewModelScope.launch {
            TimerNotificationReceiver.timerEvents.collect { action ->
                when (action) {
                    TimerNotificationReceiver.TimerAction.SKIP -> skipRestTimer()
                    TimerNotificationReceiver.TimerAction.ADD_30S -> addRestTime(30)
                    TimerNotificationReceiver.TimerAction.DISMISS -> timerNotificationHelper.cancelTimer()
                }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.weightUnit.collect { unit ->
                _state.update { it.copy(weightUnit = unit) }
            }
        }
        
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _availableExercises.value = exercises
            }
        }

        val planId: Int? = savedStateHandle.get<Int>("planId")
        val sessionId: Int? = savedStateHandle.get<Int>("sessionId")
        
        viewModelScope.launch {
            if (sessionId != null && sessionId != 0 && sessionId != -1) {
                resumeWorkout(sessionId)
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
        val planName = planWithDetails.plan.nome
        
        val swaps = workoutRepository.getSwapsForSession(sessionId).firstOrNull()
        val swapMap = swaps?.associate { it.originalExerciseId to it.replacementExerciseId } ?: emptyMap()

        // Load saved rest timer from session
        val savedEndTime = sessionWithSets.session.restTimerEndTime
        val savedTotalSeconds = sessionWithSets.session.totalRestSeconds
        val savedRemainingSeconds = savedEndTime?.let { 
            ((it - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        } ?: 0

        val exerciseStates = planWithDetails.exercises.sortedBy { it.planExercise.ordine }.map { detail ->
            val previousSets = workoutRepository.getLastSessionSetsForExercise(planId, detail.exercise.id, detail.planExercise.serieTarget).firstOrNull()
            val prevPerfStr = if (!previousSets.isNullOrEmpty()) {
                val bestSet = previousSets.maxByOrNull { it.pesoSollevato }
                if (bestSet != null) "Last: ${bestSet.pesoSollevato}kg × ${bestSet.repsEffettive}" else null
            } else null

            // Load already completed sets for this session
            val loggedSets = sessionWithSets.sets.filter { it.exerciseId == detail.exercise.id }
            
            val targetSets = detail.planExercise.serieTarget
            val repsList = if (previousSets.isNullOrEmpty()) {
                parseReps(detail.planExercise.repsTarget, targetSets)
            } else {
                previousSets.map { it.repsEffettive }
            }
            val lastLoggedWeight = loggedSets.lastOrNull()?.pesoSollevato ?: previousSets?.lastOrNull()?.pesoSollevato ?: 0f

            val sets = (1..targetSets).map { num ->
                val loggedSet = loggedSets.find { it.numeroSerie == num }
                if (loggedSet != null) {
                    WorkoutSetState(
                        id = loggedSet.id,
                        setNumber = num,
                        weight = loggedSet.pesoSollevato,
                        reps = loggedSet.repsEffettive,
                        note = loggedSet.note,
                        isCompleted = true,
                        isWarmup = loggedSet.isWarmup
                    )
                } else {
                    val prevSet = previousSets?.getOrNull(num - 1)
                    WorkoutSetState(
                        setNumber = num,
                        weight = prevSet?.pesoSollevato ?: lastLoggedWeight,
                        reps = prevSet?.repsEffettive ?: repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 }
                    )
                }
            }

            WorkoutExerciseState(
                exercise = detail.exercise,
                planDetails = detail.planExercise,
                sets = sets,
                previousPerformance = prevPerfStr,
                swappedExerciseId = swapMap[detail.planExercise.id]
            )
        }

        // Find first unfinished exercise
        val activeIndex = exerciseStates.indexOfFirst { ex -> ex.sets.any { !it.isCompleted } }.coerceAtLeast(0)

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
                restTimerEndTime = if (savedRemainingSeconds > 0) savedEndTime else null
            )
        }

        // Resume timer if still valid
        if (savedRemainingSeconds > 0 && savedEndTime != null) {
            resumeRestTimer(savedEndTime)
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
                    reps = prevSet?.repsEffettive ?: repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 }
                )
            }

            WorkoutExerciseState(
                exercise = detail.exercise,
                planDetails = detail.planExercise,
                sets = initialSets,
                previousPerformance = prevPerfStr
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
            mutableSets[setIndex] = set.copy(weight = weight)
            
            // Propagate to ALL subsequent uncompleted sets in THIS exercise
            // ONLY if there is no history for this exercise (new exercise or first time)
            if (exState.previousPerformance == null) {
                for (i in (setIndex + 1) until mutableSets.size) {
                    if (!mutableSets[i].isCompleted) {
                        mutableSets[i] = mutableSets[i].copy(weight = weight)
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

            mutableSets[setIndex] = set.copy(reps = reps)
            mutableExercises[exerciseIndex] = exState.copy(sets = mutableSets)
            curr.copy(exercises = mutableExercises)
        }
    }

    fun toggleSetComplete(exerciseIndex: Int, setIndex: Int) {
        val currentState = _state.value
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val setState = exState.sets.getOrNull(setIndex) ?: return
        
        val newIsCompleted = !setState.isCompleted

        viewModelScope.launch {
            var newSetId = setState.id

            if (newIsCompleted && currentState.sessionId != null) {
                val logId = workoutRepository.logSet(
                    SetLogEntity(
                        id = setState.id ?: 0,
                        sessionId = currentState.sessionId,
                        exerciseId = exState.exercise.id,
                        pesoSollevato = setState.weight,
                        repsEffettive = setState.reps,
                        numeroSerie = setState.setNumber,
                        isWarmup = setState.isWarmup,
                        note = setState.note
                    )
                )
                newSetId = logId.toInt()

                val restTime = exState.planDetails?.recuperoTarget ?: 90
                startRestTimer(restTime)
            } else if (!newIsCompleted && setState.id != null) {
                workoutRepository.deleteSet(
                    SetLogEntity(
                        id = setState.id,
                        sessionId = currentState.sessionId ?: 0,
                        exerciseId = exState.exercise.id,
                        pesoSollevato = setState.weight,
                        repsEffettive = setState.reps,
                        numeroSerie = setState.setNumber,
                        isWarmup = setState.isWarmup,
                        note = setState.note
                    )
                )
                newSetId = null
                stopRestTimer()
            }

            updateSetState(exerciseIndex, setIndex) { 
                it.copy(isCompleted = newIsCompleted, id = newSetId) 
            }
        }
    }

    private fun updateSetState(exerciseIndex: Int, setIndex: Int, updateFun: (WorkoutSetState) -> WorkoutSetState) {
        _state.update { curr ->
            val mutableExercises = curr.exercises.toMutableList()
            val exState = mutableExercises[exerciseIndex]
            val mutableSets = exState.sets.toMutableList()
            mutableSets[setIndex] = updateFun(mutableSets[setIndex])
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
                        note = note
                    )
                )
            }
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
                        workoutRepository.setSessionFinished(id)
                    }
                    stopRestTimer()
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

    private fun startRestTimer(seconds: Int) {
        stopRestTimer()
        val endTime = System.currentTimeMillis() + (seconds * 1000L)
        _state.update { it.copy(remainingRestSeconds = seconds, totalRestSeconds = seconds, restTimerEndTime = endTime) }
        if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
            timerNotificationHelper.startOrUpdateTimerNotification(seconds)
        }
        saveTimerToSession(endTime, seconds)
        startTimerJob()
    }

    private fun resumeRestTimer(endTime: Long) {
        stopRestTimer()
        val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        val totalSeconds = _state.value.totalRestSeconds
        _state.update { it.copy(remainingRestSeconds = remaining, restTimerEndTime = endTime) }
        if (remaining > 0) {
            if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
                timerNotificationHelper.startOrUpdateTimerNotification(remaining)
            }
            startTimerJob()
        } else {
            clearTimerInSession()
        }
    }

    private fun startTimerJob() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(100L)
                val end = _state.value.restTimerEndTime ?: break
                val remaining = ((end - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                
                if (remaining == 0) {
                    _state.update { it.copy(remainingRestSeconds = 0, restTimerEndTime = null) }
                    if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
                        timerNotificationHelper.showRestFinished()
                    }
                    clearTimerInSession()
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
                timerNotificationHelper.startOrUpdateTimerNotification(newRemaining)
            }
            saveTimerToSession(newEnd, newTotal)
        }
    }

    fun skipRestTimer() {
        stopRestTimer()
        clearTimerInSession()
    }

    fun swapExercise(exerciseIndex: Int, newExerciseId: Int, targetSets: Int, repsTarget: String) {
        val currentState = _state.value
        val sessionId = currentState.sessionId ?: return
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val originalExerciseId = exState.planDetails?.id ?: return

        val replacementExercise = _availableExercises.value.find { it.id == newExerciseId } ?: return

        viewModelScope.launch {
            workoutRepository.saveExerciseSwap(
                SessionExerciseSwapEntity(
                    sessionId = sessionId,
                    originalExerciseId = originalExerciseId,
                    replacementExerciseId = newExerciseId
                )
            )
        }

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
                previousPerformance = null
            )
            curr.copy(exercises = mutableExercises, exerciseSwaps = mutableSwaps)
        }
    }

    fun getSwappedExerciseId(originalExerciseId: Int): Int? {
        return _state.value.exerciseSwaps[originalExerciseId]
    }

    private fun stopRestTimer() {
        timerJob?.cancel()
        timerJob = null
        timerNotificationHelper.cancelTimer()
        _state.update { it.copy(remainingRestSeconds = 0, restTimerEndTime = null) }
    }

    fun cancelWorkout(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value.sessionId?.let { sessionId ->
                workoutRepository.deleteSession(sessionId)
            }
            stopRestTimer()
            onComplete()
        }
    }
}
