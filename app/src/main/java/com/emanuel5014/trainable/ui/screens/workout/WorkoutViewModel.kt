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
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
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
    val warmupTimerTotalSeconds: Int = 0,
    val exerciseExecutionOrder: Map<Int, Int> = emptyMap(),
    val nextExecutionOrder: Int = 0,
    val editablePresetExercises: Boolean = false,
    val categories: List<String> = emptyList(),
    val workoutTimerEnabled: Boolean = false,
    val inlineExerciseModificationsEnabled: Boolean = false,
    val hapticEnabled: Boolean = true,
    val cardioTimerSeconds: Int = 0,
    val cardioTimerRunning: Boolean = false,
    val cardioTimerPaused: Boolean = false,
    val cardioTimerStartedAt: Long? = null,
    val cardioTimerBaseSeconds: Int = 0,
    val setTimerSeconds: Int = 0,
    val setTimerRunning: Boolean = false,
    val setTimerPaused: Boolean = false,
    val setTimerStartedAt: Long? = null,
    val setTimerBaseSeconds: Int = 0,
    val autoStopCardioAtTarget: Boolean = false,
    val autoStopTimeWeightAtTarget: Boolean = false,
    val keepScreenOnCardioTimer: Boolean = true,
    val keepScreenOnSetTimer: Boolean = true,
    val sessionStartTime: Long? = null
) {
    val currentExercise: WorkoutExerciseState?
        get() = exercises.getOrNull(currentExerciseIndex)

    val completedExercises: Int
        get() = exercises.count { ex ->
            if (ex.isCardio) ex.isCardioCompleted else ex.sets.all { it.isCompleted }
        }

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
    val supersetId: String? = null,
    val isCardio: Boolean = false,
    val cardioCategoria: String? = null,
    val cardioDurataTargetSeconds: Int? = null,
    val cardioDistanzaTargetKm: Float? = null,
    val cardioLogId: Int? = null,
    val cardioElapsedSeconds: Int = 0,
    val cardioDistanceKm: Float = 0f,
    val isCardioCompleted: Boolean = false,
    val exerciseType: String = "strength",
    val timeTargetSeconds: Int? = null
) {
    val isTimeAndWeight: Boolean
        get() = exerciseType == "time_and_weight" || 
                (planDetails?.exerciseType == "time_and_weight" && swappedExerciseId == null) ||
                sets.any { it.timeSeconds != null }
}

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
    val isWarmup: Boolean = false,
    val timeSeconds: Int? = null,
    val previousTimeSeconds: Int? = null
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

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

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
            userPreferencesRepository.workoutTimerEnabled.collect { enabled ->
                _state.update { it.copy(workoutTimerEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.editablePresetExercises.collect { enabled ->
                _state.update { it.copy(editablePresetExercises = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.inlineExerciseModificationsEnabled.collect { enabled ->
                _state.update { it.copy(inlineExerciseModificationsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.hapticEnabled.collect { enabled ->
                _state.update { it.copy(hapticEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.autoStopCardioAtTarget.collect { enabled ->
                _state.update { it.copy(autoStopCardioAtTarget = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.autoStopTimeWeightAtTarget.collect { enabled ->
                _state.update { it.copy(autoStopTimeWeightAtTarget = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.keepScreenOnCardioTimer.collect { enabled ->
                _state.update { it.copy(keepScreenOnCardioTimer = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.keepScreenOnSetTimer.collect { enabled ->
                _state.update { it.copy(keepScreenOnSetTimer = enabled) }
            }
        }
        
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _availableExercises.value = exercises
            }
        }

        viewModelScope.launch {
            exerciseRepository.getCategories().collect { categories ->
                _categories.value = categories
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

    private suspend fun getPreviousSetsForExercise(planId: Int?, exerciseId: Int, limitSets: Int = 3): List<SetLogEntity> {
        if (planId != null && planId != 0 && planId != -1) {
            return workoutRepository.getLastSessionSetsForExercise(planId, exerciseId, limitSets).firstOrNull() ?: emptyList()
        }
        return emptyList()
    }

    private suspend fun resumeWorkout(sessionId: Int) {
        val sessionWithSets = workoutRepository.getSessionWithSets(sessionId).firstOrNull() ?: return
        val planId = sessionWithSets.session.planId
        val planWithDetails = workoutRepository.getPlanWithDetails(planId).firstOrNull() ?: return
        val planName = sessionWithSets.session.noteSessione ?: planWithDetails.plan.nome
        
        val swaps = workoutRepository.getSwapsForSession(sessionId).firstOrNull()
        val swapMap = swaps?.associate { it.originalExerciseId to it.replacementExerciseId } ?: emptyMap()
        val reverseSwapMap = swaps?.associate { it.replacementExerciseId to it.originalExerciseId } ?: emptyMap()

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

        // Load saved cardio timer from session
        val savedCardioSeconds = sessionWithSets.session.cardioTimerSeconds
        val savedCardioRunning = sessionWithSets.session.cardioTimerRunning
        val savedCardioPaused = sessionWithSets.session.cardioTimerPaused
        val savedCardioStartedAt = sessionWithSets.session.cardioTimerStartedAt
        val restoredCardioSeconds = if (savedCardioRunning && savedCardioStartedAt != null) {
            savedCardioSeconds + ((System.currentTimeMillis() - savedCardioStartedAt) / 1000).toInt().coerceAtLeast(0)
        } else {
            savedCardioSeconds
        }

        // Load saved set timer from session
        val savedSetSeconds = sessionWithSets.session.setTimerSeconds
        val savedSetRunning = sessionWithSets.session.setTimerRunning
        val savedSetPaused = sessionWithSets.session.setTimerPaused
        val savedSetStartedAt = sessionWithSets.session.setTimerStartedAt
        val restoredSetSeconds = if (savedSetRunning && savedSetStartedAt != null) {
            savedSetSeconds + ((System.currentTimeMillis() - savedSetStartedAt) / 1000).toInt().coerceAtLeast(0)
        } else {
            savedSetSeconds
        }

        val planExercises = planWithDetails.exercises.sortedBy { it.planExercise.ordine }
        
        val allAvailableExercises = _availableExercises.value.ifEmpty { 
            exerciseRepository.getAllExercises().firstOrNull() ?: emptyList() 
        }

        val cardioLogs = workoutRepository.getCardioLogsForSession(sessionId).firstOrNull() ?: emptyList()
        val useOrdine = sessionWithSets.sets.any { it.ordineEsercizio > 0 } || cardioLogs.any { it.ordineEsercizio > 0 }

        suspend fun createExerciseState(exercise: ExerciseEntity, planDetail: PlanExerciseEntity?, exerciseIndex: Int, useOrdine: Boolean): WorkoutExerciseState {
            val isCardio = exercise.categoria.equals("Cardio", ignoreCase = true) || planDetail?.exerciseType == "cardio"
            val cardioLog = if (isCardio) {
                cardioLogs.find { if (useOrdine) it.ordineEsercizio == exerciseIndex else it.categoria.equals(exercise.nome, ignoreCase = true) }
            } else null

            // Load already completed or uncompleted sets for this session
            val loggedSets = if (useOrdine) {
                sessionWithSets.sets.filter { it.ordineEsercizio == exerciseIndex }
            } else {
                sessionWithSets.sets.filter { it.exerciseId == exercise.id }
            }

            val isSwapped = (planDetail != null && swapMap[planDetail.id] != null) || reverseSwapMap[exercise.id] != null
            val hasLoggedTime = loggedSets.any { it.durataSecondi != null }
            val loggedTargetSeconds = loggedSets.firstOrNull { it.durataSecondi != null }?.durataSecondi

            val isTimeAndWeight = when {
                isCardio -> false
                hasLoggedTime -> true
                isSwapped -> false
                planDetail != null -> planDetail.exerciseType == "time_and_weight"
                else -> false
            }

            val resolvedExerciseType = when {
                isCardio -> "cardio"
                isTimeAndWeight -> "time_and_weight"
                else -> "strength"
            }

            val resolvedTargetSeconds = when {
                isTimeAndWeight -> loggedTargetSeconds ?: (if (!isSwapped) planDetail?.durataTargetSecondi else null) ?: 45
                else -> null
            }

            val defaultTargetSeconds = resolvedTargetSeconds ?: 45
            val previousSets = getPreviousSetsForExercise(planId, exercise.id, planDetail?.serieTarget ?: 3)
            val prevPerfStr = if (previousSets.isNotEmpty()) {
                val bestSet = previousSets.maxByOrNull { it.pesoSollevato }
                if (bestSet != null) {
                    if (isTimeAndWeight || bestSet.durataSecondi != null) {
                        "Last: ${bestSet.pesoSollevato}kg × ${bestSet.durataSecondi ?: bestSet.repsEffettive}s"
                    } else {
                        "Last: ${bestSet.pesoSollevato}kg × ${bestSet.repsEffettive}"
                    }
                } else null
            } else null
            
            val targetSets = if (planDetail == null && loggedSets.isNotEmpty()) {
                loggedSets.size
            } else {
                planDetail?.serieTarget ?: loggedSets.size.coerceAtLeast(3)
            }
            val repsList = if (previousSets.isEmpty()) {
                parseReps(planDetail?.repsTarget ?: "8", targetSets)
            } else {
                previousSets.map { it.repsEffettive }
            }
            val defaultPrevWeight = previousSets.firstOrNull()?.pesoSollevato ?: previousSets.lastOrNull()?.pesoSollevato ?: 0f

            val sets = if (isCardio) emptyList() else (1..targetSets.coerceAtLeast(loggedSets.size)).map { num ->
                val loggedSet = loggedSets.find { it.numeroSerie == num }
                val prevSet = previousSets.getOrNull(num - 1)
                val setDuration = if (isTimeAndWeight) {
                    loggedSet?.durataSecondi ?: prevSet?.durataSecondi ?: defaultTargetSeconds
                } else {
                    loggedSet?.durataSecondi ?: prevSet?.durataSecondi
                }
                if (loggedSet != null) {
                    WorkoutSetState(
                        id = loggedSet.id,
                        setNumber = num,
                        weight = loggedSet.pesoSollevato,
                        reps = loggedSet.repsEffettive,
                        note = loggedSet.note,
                        previousNote = prevSet?.note,
                        previousReps = prevSet?.repsEffettive,
                        previousWeight = prevSet?.pesoSollevato,
                        isCompleted = loggedSet.isCompleted,
                        isWarmup = loggedSet.isWarmup,
                        timeSeconds = setDuration,
                        previousTimeSeconds = prevSet?.durataSecondi
                    )
                } else {
                    WorkoutSetState(
                        setNumber = num,
                        weight = prevSet?.pesoSollevato ?: defaultPrevWeight,
                        reps = prevSet?.repsEffettive ?: repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 },
                        previousNote = prevSet?.note,
                        previousReps = prevSet?.repsEffettive,
                        previousWeight = prevSet?.pesoSollevato,
                        timeSeconds = setDuration,
                        previousTimeSeconds = prevSet?.durataSecondi
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
                swappedExerciseId = planDetail?.id?.let { swapMap[it] } ?: if (isSwapped) exercise.id else null,
                supersetId = planDetail?.supersetId ?: loggedSets.firstOrNull()?.supersetId,
                customRestSeconds = restoredRestSeconds,
                isCardio = isCardio,
                cardioCategoria = exercise.nome,
                cardioDurataTargetSeconds = cardioLog?.durataTargetSecondi ?: planDetail?.durataTargetSecondi,
                cardioDistanzaTargetKm = planDetail?.distanzaTargetKm,
                cardioLogId = cardioLog?.id,
                cardioElapsedSeconds = cardioLog?.durataSecondi ?: 0,
                cardioDistanceKm = cardioLog?.distanza ?: 0f,
                isCardioCompleted = cardioLog?.isCompleted ?: false,
                exerciseType = resolvedExerciseType,
                timeTargetSeconds = resolvedTargetSeconds
            )
        }

        val exerciseStates: List<WorkoutExerciseState>
        val activeIndex: Int
        var executionOrderMap = mutableMapOf<Int, Int>()
        var maxOrder = -1

        if (useOrdine) {
            val setsByOrder = sessionWithSets.sets
                .groupBy { it.ordineEsercizio }
                .toSortedMap()

            val cardioByOrder = cardioLogs
                .groupBy { it.ordineEsercizio }
                .toSortedMap()

            val planDetailByOrigIndex = planExercises.mapIndexed { index, detail ->
                index to detail
            }.toMap()
            val planDetailByExerciseId = planExercises.associateBy { it.exercise.id }

            val exerciseStatesByOrder = mutableMapOf<Int, WorkoutExerciseState>()
            val consumedPlanDetailIds = mutableSetOf<Int>()

            for ((order, setsForOrder) in setsByOrder) {
                val firstSet = setsForOrder.firstOrNull() ?: continue
                val exerciseId = firstSet.exerciseId
                var planDetail = planDetailByExerciseId[exerciseId]
                if (planDetail == null) {
                    val originalPlanExerciseId = reverseSwapMap[exerciseId]
                    if (originalPlanExerciseId != null) {
                        planDetail = planExercises.find { it.planExercise.id == originalPlanExerciseId }
                    }
                }
                val exercise = allAvailableExercises.find { it.id == exerciseId } ?: continue

                val planState = if (planDetail != null) planDetail.planExercise else null
                exerciseStatesByOrder[order] = createExerciseState(exercise, planState, order, true)
                if (planDetail != null) consumedPlanDetailIds.add(planDetail.planExercise.id)
                // If this exercise was swapped in, also consume the original plan exercise
                reverseSwapMap[exerciseId]?.let { consumedPlanDetailIds.add(it) }
            }

            for ((order, cardioLogsForOrder) in cardioByOrder) {
                if (order in exerciseStatesByOrder) continue
                val firstCardio = cardioLogsForOrder.firstOrNull() ?: continue
                val exercise = allAvailableExercises.find { it.nome.equals(firstCardio.categoria, ignoreCase = true) } ?: continue
                var planDetail = planExercises.find { it.exercise.id == exercise.id }
                // If the cardio exercise was swapped in from a plan exercise, use the original plan detail
                if (planDetail == null) {
                    val originalPlanExerciseId = reverseSwapMap[exercise.id]
                    if (originalPlanExerciseId != null) {
                        planDetail = planExercises.find { it.planExercise.id == originalPlanExerciseId }
                        consumedPlanDetailIds.add(originalPlanExerciseId)
                    }
                }
                exerciseStatesByOrder[order] = createExerciseState(exercise, planDetail?.planExercise, order, true)
                if (planDetail != null) {
                    consumedPlanDetailIds.add(planDetail.planExercise.id)
                }
            }

            for ((origIndex, detail) in planDetailByOrigIndex) {
                if (detail.planExercise.id in consumedPlanDetailIds) continue
                val swappedId = swapMap[detail.planExercise.id]
                val exercise = if (swappedId != null) {
                    allAvailableExercises.find { it.id == swappedId } ?: detail.exercise
                } else {
                    detail.exercise
                }
                val maxExistingOrder = exerciseStatesByOrder.keys.maxOrNull() ?: -1
                val order = if (origIndex > maxExistingOrder) origIndex else maxExistingOrder + 1
                exerciseStatesByOrder[order] = createExerciseState(exercise, detail.planExercise, order, true)
            }

            exerciseStates = exerciseStatesByOrder.toSortedMap().values.toList()
            activeIndex = exerciseStates.indexOfFirst { exState ->
                if (exState.isCardio) !exState.isCardioCompleted
                else exState.sets.any { !it.isCompleted }
            }.coerceAtLeast(0)

            exerciseStatesByOrder.forEach { (order, exState) ->
                executionOrderMap[exState.exercise.id] = order
                if (order > maxOrder) maxOrder = order
            }
        } else {
            val planExerciseStates = planExercises.mapIndexed { index, detail ->
                val swappedId = swapMap[detail.planExercise.id]
                val exercise = if (swappedId != null) {
                    allAvailableExercises.find { it.id == swappedId } ?: detail.exercise
                } else {
                    detail.exercise
                }
                createExerciseState(exercise, detail.planExercise, index, false)
            }

            val consumedExerciseIds = planExerciseStates.map { it.exercise.id }.toSet()
            val loggedExerciseIds = sessionWithSets.sets.map { it.exerciseId }.distinct()
            val cardioExerciseIds = cardioLogs.mapNotNull { log ->
                allAvailableExercises.find { it.nome.equals(log.categoria, ignoreCase = true) }?.id
            }.distinct()
            val extraExerciseIds = (loggedExerciseIds + cardioExerciseIds).filter { it !in consumedExerciseIds }

            val extraExerciseStates = extraExerciseIds.mapIndexed { idx, exerciseId ->
                val exercise = allAvailableExercises.find { it.id == exerciseId } ?: return@mapIndexed null
                createExerciseState(exercise, null, planExercises.size + idx, false)
            }.filterNotNull()

            exerciseStates = planExerciseStates + extraExerciseStates
            activeIndex = exerciseStates.indexOfFirst { exState ->
                if (exState.isCardio) !exState.isCardioCompleted
                else exState.sets.any { !it.isCompleted }
            }.coerceAtLeast(0)

            executionOrderMap = mutableMapOf()
            exerciseStates.forEachIndexed { index, exState ->
                val existingOrder = sessionWithSets.sets.find { it.exerciseId == exState.exercise.id }?.ordineEsercizio
                val order = existingOrder ?: index
                executionOrderMap[exState.exercise.id] = order
                if (order > maxOrder) maxOrder = order
            }
        }

        val isQuick = planWithDetails.plan.note == "SYSTEM_PLAN" && (planWithDetails.plan.nome == "Quick Workout" || planWithDetails.plan.nome == "Allenamento Veloce")

        val finalActiveIndex = if (savedCardioRunning || savedCardioPaused) {
            val activeCardioLog = cardioLogs.find { !it.isCompleted }
            if (activeCardioLog != null) {
                val cardioIdx = exerciseStates.indexOfFirst { exState ->
                    exState.isCardio && !exState.isCardioCompleted && 
                    exState.exercise.nome.equals(activeCardioLog.categoria, ignoreCase = true)
                }
                if (cardioIdx >= 0) cardioIdx else activeIndex
            } else {
                activeIndex
            }
        } else {
            activeIndex
        }

        _state.update {
            it.copy(
                isLoading = false,
                planId = planId,
                planName = planName,
                sessionId = sessionId,
                exercises = exerciseStates,
                currentExerciseIndex = finalActiveIndex,
                exerciseSwaps = swapMap,
                remainingRestSeconds = savedRemainingSeconds,
                totalRestSeconds = savedTotalSeconds ?: 90,
                restTimerEndTime = if (savedRemainingSeconds > 0) savedEndTime else null,
                isQuickWorkout = isQuick,
                warmupTimerRemaining = savedWarmupRemainingSeconds,
                warmupTimerEndTime = if (savedWarmupRemainingSeconds > 0) savedWarmupEndTime else null,
                warmupTimerTotalSeconds = savedWarmupTotalSeconds ?: 0,
                exerciseExecutionOrder = executionOrderMap,
                nextExecutionOrder = maxOrder + 1,
                cardioTimerSeconds = restoredCardioSeconds,
                cardioTimerRunning = savedCardioRunning,
                cardioTimerPaused = savedCardioPaused,
                cardioTimerBaseSeconds = restoredCardioSeconds,
                setTimerSeconds = restoredSetSeconds,
                setTimerRunning = savedSetRunning,
                setTimerPaused = savedSetPaused,
                setTimerBaseSeconds = restoredSetSeconds,
                setTimerStartedAt = if (savedSetRunning) System.currentTimeMillis() else null,
                sessionStartTime = sessionWithSets.session.timestamp
            )
        }

        // Resume timers if still valid
        if (savedRemainingSeconds > 0 && savedEndTime != null) {
            resumeRestTimer(savedEndTime)
        }
        if (savedWarmupRemainingSeconds > 0 && savedWarmupEndTime != null) {
            resumeWarmupTimer(savedWarmupEndTime)
        }
        if (savedCardioRunning || savedCardioPaused) {
            val cardioExState = exerciseStates.getOrNull(finalActiveIndex)
            if (cardioExState != null && cardioExState.isCardio && !cardioExState.isCardioCompleted) {
                if (savedCardioRunning) {
                    startCardioTimer()
                }
            } else {
                clearCardioTimerInSession()
            }
        }
        if (savedSetRunning || savedSetPaused) {
            val targetExState = exerciseStates.getOrNull(finalActiveIndex)
            if (targetExState != null && targetExState.isTimeAndWeight) {
                if (savedSetRunning) {
                    startSetTimer()
                }
            } else {
                clearSetTimerInSession()
            }
        }
    }

    private suspend fun initializeNewWorkout(planId: Int) {
        val planWithDetails = workoutRepository.getPlanWithDetails(planId).firstOrNull() ?: return
        val planName = planWithDetails.plan.nome

        val startTime = System.currentTimeMillis()
        val sessionId = workoutRepository.startSession(planId, startTime).toInt()

        val exerciseStates = planWithDetails.exercises.sortedBy { it.planExercise.ordine }.map { detail ->
            val previousSets = getPreviousSetsForExercise(planId, detail.exercise.id, detail.planExercise.serieTarget)
            val isTimeAndWeight = detail.planExercise.exerciseType == "time_and_weight"
            val defaultTargetSeconds = detail.planExercise.durataTargetSecondi ?: 45
            val prevPerfStr = if (previousSets.isNotEmpty()) {
                val bestSet = previousSets.maxByOrNull { it.pesoSollevato }
                if (bestSet != null) {
                    if (isTimeAndWeight || bestSet.durataSecondi != null) {
                        "Last: ${bestSet.pesoSollevato}kg × ${bestSet.durataSecondi ?: bestSet.repsEffettive}s"
                    } else {
                        "Last: ${bestSet.pesoSollevato}kg × ${bestSet.repsEffettive}"
                    }
                } else null
            } else null

            val targetSets = detail.planExercise.serieTarget
            val repsList = if (previousSets.isEmpty()) {
                parseReps(detail.planExercise.repsTarget, targetSets)
            } else {
                previousSets.map { it.repsEffettive }
            }
            val defaultWeight = previousSets.firstOrNull()?.pesoSollevato ?: previousSets.lastOrNull()?.pesoSollevato ?: 0f

            val isCardio = detail.exercise.categoria.equals("Cardio", ignoreCase = true) || detail.planExercise.exerciseType == "cardio"

            val initialSets = if (isCardio) emptyList() else (1..targetSets).map { num ->
                val prevSet = previousSets.getOrNull(num - 1)
                val setDuration = if (isTimeAndWeight) (prevSet?.durataSecondi ?: defaultTargetSeconds) else prevSet?.durataSecondi
                WorkoutSetState(
                    setNumber = num,
                    weight = prevSet?.pesoSollevato ?: defaultWeight,
                    reps = prevSet?.repsEffettive ?: repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 },
                    previousNote = prevSet?.note,
                    previousReps = prevSet?.repsEffettive,
                    previousWeight = prevSet?.pesoSollevato,
                    timeSeconds = setDuration,
                    previousTimeSeconds = prevSet?.durataSecondi
                )
            }

            WorkoutExerciseState(
                exercise = detail.exercise,
                planDetails = detail.planExercise,
                sets = initialSets,
                previousPerformance = prevPerfStr,
                supersetId = detail.planExercise.supersetId,
                isCardio = isCardio,
                cardioCategoria = detail.exercise.nome,
                cardioDurataTargetSeconds = detail.planExercise.durataTargetSecondi,
                cardioDistanzaTargetKm = detail.planExercise.distanzaTargetKm,
                exerciseType = detail.planExercise.exerciseType,
                timeTargetSeconds = detail.planExercise.durataTargetSecondi
            )
        }

        _state.update {
            it.copy(
                isLoading = false,
                planId = planId,
                planName = planName,
                sessionId = sessionId,
                exercises = exerciseStates,
                currentExerciseIndex = 0,
                exerciseExecutionOrder = emptyMap(),
                nextExecutionOrder = 0,
                sessionStartTime = startTime
            )
        }
    }

    private suspend fun initializeQuickWorkout(name: String?) {
        val startTime = System.currentTimeMillis()
        val sessionId = workoutRepository.startQuickWorkoutSession(name).toInt()
        val sessionWithSets = workoutRepository.getSessionWithSets(sessionId).firstOrNull()
        val planId = sessionWithSets?.session?.planId
        val displayName = name ?: localeManager.getString(R.string.quick_workout)

        _state.update {
            it.copy(
                isLoading = false,
                planId = planId,
                planName = displayName,
                sessionId = sessionId,
                exercises = emptyList(),
                currentExerciseIndex = 0,
                isQuickWorkout = true,
                sessionStartTime = startTime
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

            val oldWeight = set.weight
            // Update current set
            val updatedSet = set.copy(weight = weight)
            mutableSets[setIndex] = updatedSet
            
            if (updatedSet.id != null && curr.sessionId != null) {
                val executionOrder = curr.exerciseExecutionOrder[exState.exercise.id] ?: exerciseIndex
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
                            ordineEsercizio = executionOrder,
                            restTimerSeconds = exState.customRestSeconds,
                            durataSecondi = updatedSet.timeSeconds
                        )
                    )
                }
            }
            
            val isQuickOrCustom = curr.isQuickWorkout || exState.swappedExerciseId != null || exState.planDetails == null
            val shouldPropagate = isQuickOrCustom || exState.previousPerformance == null

            if (shouldPropagate) {
                for (i in (setIndex + 1) until mutableSets.size) {
                    if (!mutableSets[i].isCompleted) {
                        val propSet = mutableSets[i].copy(weight = weight)
                        mutableSets[i] = propSet
                        if (propSet.id != null && curr.sessionId != null) {
                            val executionOrder = curr.exerciseExecutionOrder[exState.exercise.id] ?: exerciseIndex
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
                                        ordineEsercizio = executionOrder,
                                        restTimerSeconds = exState.customRestSeconds,
                                        durataSecondi = propSet.timeSeconds
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
                val executionOrder = curr.exerciseExecutionOrder[exState.exercise.id] ?: exerciseIndex
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
                            ordineEsercizio = executionOrder,
                            restTimerSeconds = exState.customRestSeconds,
                            durataSecondi = updatedSet.timeSeconds
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
            val executionOrder = currentState.exerciseExecutionOrder[exState.exercise.id] ?: exerciseIndex
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
                        ordineEsercizio = executionOrder,
                        restTimerSeconds = exState.customRestSeconds,
                        durataSecondi = setState.timeSeconds
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
        val isFirstCompletion = newIsCompleted && !exState.sets.any { it.isCompleted } && !setState.isWarmup

        viewModelScope.launch {
            var newSetId = setState.id

            if (isFirstCompletion && exState.exercise.id !in currentState.exerciseExecutionOrder) {
                val assignedOrder = currentState.nextExecutionOrder
                _state.update { it.copy(
                    exerciseExecutionOrder = it.exerciseExecutionOrder + (exState.exercise.id to assignedOrder),
                    nextExecutionOrder = assignedOrder + 1
                )}
                if (currentState.sessionId != null) {
                    workoutRepository.updateExerciseOrderInSession(
                        currentState.sessionId,
                        exState.exercise.id,
                        assignedOrder
                    )
                }
            }

            if (currentState.sessionId != null) {
                val executionOrder = _state.value.exerciseExecutionOrder[exState.exercise.id] ?: exerciseIndex
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
                        ordineEsercizio = executionOrder,
                        restTimerSeconds = exState.customRestSeconds,
                        durataSecondi = setState.timeSeconds
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
                val executionOrder = curr.exerciseExecutionOrder[exState.exercise.id] ?: exerciseIndex
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
                            ordineEsercizio = executionOrder,
                            restTimerSeconds = exState.customRestSeconds,
                            durataSecondi = updatedSet.timeSeconds
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
                    val durationMs = if (_state.value.workoutTimerEnabled) {
                        _state.value.sessionStartTime?.let { System.currentTimeMillis() - it }
                    } else null
                    withContext(Dispatchers.IO) {
                        workoutRepository.deleteUncompletedSetsForSession(id)
                        workoutRepository.deleteUncompletedCardioLogsForSession(id)
                        workoutRepository.setSessionFinished(id, durationMs)
                    }
                    stopRestTimer()
                    stopWarmupTimer()
                    cardioTimerJob?.cancel()
                    cardioTimerJob = null
                    clearCardioTimerInSession()
                    setTimerJob?.cancel()
                    setTimerJob = null
                    clearSetTimerInSession()
                    _state.update { it.copy(
                        isFinished = true, 
                        isFinishing = false, 
                        cardioTimerRunning = false, 
                        cardioTimerPaused = false, 
                        cardioTimerSeconds = 0, 
                        cardioTimerStartedAt = null, 
                        cardioTimerBaseSeconds = 0,
                        setTimerRunning = false,
                        setTimerPaused = false,
                        setTimerSeconds = 0,
                        setTimerStartedAt = null,
                        setTimerBaseSeconds = 0
                    ) }
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

    fun swapExercise(
        exerciseIndex: Int,
        newExerciseId: Int,
        targetSets: Int,
        repsTarget: String,
        restTimer: Int? = null,
        exerciseType: String = "strength",
        durataTargetSecondi: Int? = null
    ) {
        val currentState = _state.value
        val sessionId = currentState.sessionId ?: return
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val originalExerciseId = exState.planDetails?.id

        val replacementExercise = _availableExercises.value.find { it.id == newExerciseId } ?: return

        viewModelScope.launch {
            // Delete any existing completed/saved sets of the old exercise from the database for this session
            workoutRepository.deleteExerciseFromSession(sessionId, exState.exercise.id)
            
            val previousSets = getPreviousSetsForExercise(currentState.planId, newExerciseId, targetSets)
            val isTimeAndWeight = exerciseType == "time_and_weight"
            val defaultTargetSeconds = durataTargetSecondi ?: 45
            val prevPerfStr = if (previousSets.isNotEmpty()) {
                val bestSet = previousSets.maxByOrNull { it.pesoSollevato }
                if (bestSet != null) {
                    if (isTimeAndWeight || bestSet.durataSecondi != null) {
                        "Last: ${bestSet.pesoSollevato}kg × ${bestSet.durataSecondi ?: bestSet.repsEffettive}s"
                    } else {
                        "Last: ${bestSet.pesoSollevato}kg × ${bestSet.repsEffettive}"
                    }
                } else null
            } else null

            val repsList = parseReps(repsTarget, targetSets)
            val defaultWeight = 0f

            val executionOrder = currentState.exerciseExecutionOrder[exState.exercise.id] ?: exerciseIndex

            val initialSets = (1..targetSets).map { num ->
                val prevSet = previousSets.getOrNull(num - 1)
                val weight = defaultWeight
                val reps = repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 }
                val setDuration = if (isTimeAndWeight) (prevSet?.durataSecondi ?: defaultTargetSeconds) else prevSet?.durataSecondi
                val setLog = SetLogEntity(
                    sessionId = sessionId,
                    exerciseId = newExerciseId,
                    pesoSollevato = weight,
                    repsEffettive = reps,
                    numeroSerie = num,
                    isCompleted = false,
                    ordineEsercizio = executionOrder,
                    restTimerSeconds = restTimer,
                    supersetId = exState.supersetId,
                    durataSecondi = setDuration
                )
                val logId = workoutRepository.logSet(setLog)
                WorkoutSetState(
                    id = logId.toInt(),
                    setNumber = num,
                    weight = weight,
                    reps = reps,
                    isCompleted = false,
                    previousNote = prevSet?.note,
                    previousReps = prevSet?.repsEffettive,
                    previousWeight = prevSet?.pesoSollevato,
                    timeSeconds = setDuration,
                    previousTimeSeconds = prevSet?.durataSecondi
                )
            }

            if (originalExerciseId != null) {
                workoutRepository.saveExerciseSwap(
                    SessionExerciseSwapEntity(
                        sessionId = sessionId,
                        originalExerciseId = originalExerciseId,
                        replacementExerciseId = newExerciseId
                    )
                )

                _state.update { curr ->
                    val mutableExercises = curr.exercises.toMutableList()
                    val mutableSwaps = curr.exerciseSwaps.toMutableMap()
                    mutableSwaps[originalExerciseId] = newExerciseId
                    val updatedOrderMap = curr.exerciseExecutionOrder.toMutableMap()
                    updatedOrderMap[newExerciseId] = executionOrder
                    
                    mutableExercises[exerciseIndex] = exState.copy(
                        exercise = replacementExercise,
                        swappedExerciseId = newExerciseId,
                        sets = initialSets,
                        previousPerformance = prevPerfStr,
                        customRestSeconds = restTimer,
                        customRepsTarget = repsTarget,
                        exerciseType = exerciseType,
                        timeTargetSeconds = durataTargetSecondi
                    )
                    curr.copy(exercises = mutableExercises, exerciseSwaps = mutableSwaps, exerciseExecutionOrder = updatedOrderMap)
                }
            } else {
                _state.update { curr ->
                    val mutableExercises = curr.exercises.toMutableList()
                    val updatedOrderMap = curr.exerciseExecutionOrder.toMutableMap()
                    updatedOrderMap[newExerciseId] = executionOrder

                    mutableExercises[exerciseIndex] = exState.copy(
                        exercise = replacementExercise,
                        swappedExerciseId = newExerciseId,
                        sets = initialSets,
                        previousPerformance = prevPerfStr,
                        customRestSeconds = restTimer,
                        customRepsTarget = repsTarget,
                        exerciseType = exerciseType,
                        timeTargetSeconds = durataTargetSecondi
                    )
                    curr.copy(exercises = mutableExercises, exerciseExecutionOrder = updatedOrderMap)
                }
            }
        }
    }

    fun swapToCardioExercise(exerciseIndex: Int, newExerciseId: Int, durationMinutes: Int, restTimer: Int? = null) {
        val currentState = _state.value
        val sessionId = currentState.sessionId ?: return
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val originalExerciseId = exState.planDetails?.id

        val replacementExercise = _availableExercises.value.find { it.id == newExerciseId } ?: return

        viewModelScope.launch {
            workoutRepository.deleteExerciseFromSession(sessionId, exState.exercise.id)

            if (originalExerciseId != null) {
                workoutRepository.saveExerciseSwap(
                    SessionExerciseSwapEntity(
                        sessionId = sessionId,
                        originalExerciseId = originalExerciseId,
                        replacementExerciseId = newExerciseId
                    )
                )

                _state.update { curr ->
                    val mutableExercises = curr.exercises.toMutableList()
                    val mutableSwaps = curr.exerciseSwaps.toMutableMap()
                    mutableSwaps[originalExerciseId] = newExerciseId

                    mutableExercises[exerciseIndex] = exState.copy(
                        exercise = replacementExercise,
                        swappedExerciseId = newExerciseId,
                        sets = emptyList(),
                        previousPerformance = null,
                        customRestSeconds = restTimer,
                        customRepsTarget = null,
                        isCardio = true,
                        cardioCategoria = replacementExercise.categoria,
                        cardioDurataTargetSeconds = durationMinutes * 60
                    )
                    curr.copy(exercises = mutableExercises, exerciseSwaps = mutableSwaps)
                }
            } else {
                _state.update { curr ->
                    val mutableExercises = curr.exercises.toMutableList()
                    mutableExercises[exerciseIndex] = exState.copy(
                        exercise = replacementExercise,
                        swappedExerciseId = newExerciseId,
                        sets = emptyList(),
                        previousPerformance = null,
                        customRestSeconds = restTimer,
                        customRepsTarget = null,
                        isCardio = true,
                        cardioCategoria = replacementExercise.categoria,
                        cardioDurataTargetSeconds = durationMinutes * 60
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
                val currentOrder = currentState.exerciseExecutionOrder[currentEx.exercise.id] ?: exerciseIndex
                val nextOrder = currentState.exerciseExecutionOrder[nextEx.exercise.id] ?: (exerciseIndex + 1)
                
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
                                ordineEsercizio = currentOrder,
                                restTimerSeconds = currentEx.customRestSeconds,
                                durataSecondi = s.timeSeconds
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
                                ordineEsercizio = nextOrder,
                                restTimerSeconds = nextEx.customRestSeconds,
                                durataSecondi = s.timeSeconds
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
            cardioTimerJob?.cancel()
            cardioTimerJob = null
            setTimerJob?.cancel()
            setTimerJob = null
            onComplete()
        }
    }

    fun addCustomExercise(nome: String, categoria: String, onCreated: (ExerciseEntity) -> Unit = {}) {
        viewModelScope.launch {
            val newId = exerciseRepository.addCustomExercise(nome, categoria)
            onCreated(ExerciseEntity(id = newId, nome = nome, categoria = categoria))
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

    fun addExerciseToActiveSession(
        exercise: ExerciseEntity,
        targetSets: Int = 3,
        repsTarget: String = "8",
        restTimer: Int? = 90,
        cardioDurationMinutes: Int? = null,
        exerciseType: String = "strength",
        durataTargetSecondi: Int? = null
    ) {
        val sessionId = _state.value.sessionId ?: return
        val isCardio = exercise.categoria.equals("Cardio", ignoreCase = true) || exerciseType == "cardio"
        val isTimeAndWeight = exerciseType == "time_and_weight"
        val targetDurationSeconds = if (isCardio) (cardioDurationMinutes ?: 15) * 60 else durataTargetSecondi
        val defaultTargetSeconds = durataTargetSecondi ?: 45

        viewModelScope.launch {
            val currentState = _state.value
            val executionOrder = currentState.nextExecutionOrder

            val previousSets = if (isCardio) emptyList() else getPreviousSetsForExercise(currentState.planId, exercise.id, targetSets)
            val prevPerfStr = if (previousSets.isNotEmpty()) {
                val bestSet = previousSets.maxByOrNull { it.pesoSollevato }
                if (bestSet != null) {
                    if (isTimeAndWeight || bestSet.durataSecondi != null) {
                        "Last: ${bestSet.pesoSollevato}kg × ${bestSet.durataSecondi ?: bestSet.repsEffettive}s"
                    } else {
                        "Last: ${bestSet.pesoSollevato}kg × ${bestSet.repsEffettive}"
                    }
                } else null
            } else null

            val repsList = parseReps(repsTarget, targetSets)
            val defaultWeight = 0f
            
            var cardioLogId: Int? = null
            if (isCardio) {
                val cardioLog = CardioLogEntity(
                    sessionId = sessionId,
                    categoria = exercise.nome,
                    distanza = 0f,
                    durataSecondi = 0,
                    timestamp = System.currentTimeMillis(),
                    ordineEsercizio = executionOrder,
                    durataTargetSecondi = targetDurationSeconds,
                    isCompleted = false
                )
                cardioLogId = workoutRepository.saveCardioLog(cardioLog).toInt()
            }
            
            val initialSets = if (isCardio) emptyList() else (1..targetSets).map { num ->
                val prevSet = previousSets.getOrNull(num - 1)
                val weight = defaultWeight
                val reps = repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 }
                val setDuration = if (isTimeAndWeight) (prevSet?.durataSecondi ?: defaultTargetSeconds) else prevSet?.durataSecondi
                val setLog = SetLogEntity(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    pesoSollevato = weight,
                    repsEffettive = reps,
                    numeroSerie = num,
                    isCompleted = false,
                    ordineEsercizio = executionOrder,
                    restTimerSeconds = restTimer,
                    durataSecondi = setDuration
                )
                val logId = workoutRepository.logSet(setLog)
                WorkoutSetState(
                    id = logId.toInt(),
                    setNumber = num,
                    weight = weight,
                    reps = reps,
                    isCompleted = false,
                    previousNote = prevSet?.note,
                    previousReps = prevSet?.repsEffettive,
                    previousWeight = prevSet?.pesoSollevato,
                    timeSeconds = setDuration,
                    previousTimeSeconds = prevSet?.durataSecondi
                )
            }

            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val actualIndex = mutableExercises.size

                mutableExercises.add(
                    WorkoutExerciseState(
                        exercise = exercise,
                        planDetails = null,
                        sets = initialSets,
                        previousPerformance = prevPerfStr,
                        customRestSeconds = restTimer,
                        customRepsTarget = repsTarget,
                        isCardio = isCardio,
                        cardioCategoria = exercise.categoria,
                        cardioDurataTargetSeconds = targetDurationSeconds,
                        cardioLogId = cardioLogId,
                        exerciseType = exerciseType,
                        timeTargetSeconds = durataTargetSecondi
                    )
                )
                curr.copy(
                    exercises = mutableExercises,
                    currentExerciseIndex = actualIndex,
                    exerciseExecutionOrder = curr.exerciseExecutionOrder + (exercise.id to executionOrder),
                    nextExecutionOrder = executionOrder + 1
                )
            }
        }
    }

    fun addExerciseAfterCurrent(
        exercise: ExerciseEntity,
        targetSets: Int = 3,
        repsTarget: String = "8",
        restTimer: Int? = 90,
        cardioDurationMinutes: Int? = null,
        exerciseType: String = "strength",
        durataTargetSecondi: Int? = null
    ) {
        val sessionId = _state.value.sessionId ?: return
        val isCardio = exercise.categoria.equals("Cardio", ignoreCase = true) || exerciseType == "cardio"
        val isTimeAndWeight = exerciseType == "time_and_weight"
        val targetDurationSeconds = if (isCardio) (cardioDurationMinutes ?: 15) * 60 else durataTargetSecondi
        val defaultTargetSeconds = durataTargetSecondi ?: 45

        viewModelScope.launch {
            val currentState = _state.value
            val insertAt = currentState.currentExerciseIndex + 1
            val currentOrder = currentState.exerciseExecutionOrder[currentState.exercises.getOrNull(currentState.currentExerciseIndex)?.exercise?.id] ?: currentState.currentExerciseIndex
            val newOrder = currentOrder + 1

            val exercisesToShift = currentState.exercises.filterIndexed { index, _ -> index >= insertAt }
            val setsToUpdate = mutableListOf<SetLogEntity>()

            exercisesToShift.forEach { exState ->
                val oldOrder = currentState.exerciseExecutionOrder[exState.exercise.id] ?: return@forEach
                val newExOrder = oldOrder + 1
                exState.sets.forEach { set ->
                    if (set.id != null) {
                        setsToUpdate.add(
                            SetLogEntity(
                                id = set.id,
                                sessionId = sessionId,
                                exerciseId = exState.exercise.id,
                                pesoSollevato = set.weight,
                                repsEffettive = set.reps,
                                numeroSerie = set.setNumber,
                                isCompleted = set.isCompleted,
                                isWarmup = set.isWarmup,
                                note = set.note,
                                supersetId = exState.supersetId,
                                ordineEsercizio = newExOrder,
                                restTimerSeconds = exState.customRestSeconds,
                                durataSecondi = set.timeSeconds
                            )
                        )
                    }
                }
            }

            if (setsToUpdate.isNotEmpty()) {
                workoutRepository.updateSetOrders(setsToUpdate)
            }

            val previousSets = if (isCardio) emptyList() else getPreviousSetsForExercise(currentState.planId, exercise.id, targetSets)
            val prevPerfStr = if (previousSets.isNotEmpty()) {
                val bestSet = previousSets.maxByOrNull { it.pesoSollevato }
                if (bestSet != null) {
                    if (isTimeAndWeight || bestSet.durataSecondi != null) {
                        "Last: ${bestSet.pesoSollevato}kg × ${bestSet.durataSecondi ?: bestSet.repsEffettive}s"
                    } else {
                        "Last: ${bestSet.pesoSollevato}kg × ${bestSet.repsEffettive}"
                    }
                } else null
            } else null

            val repsList = parseReps(repsTarget, targetSets)
            val defaultWeight = 0f

            var cardioLogId: Int? = null
            if (isCardio) {
                val cardioLog = CardioLogEntity(
                    sessionId = sessionId,
                    categoria = exercise.nome,
                    distanza = 0f,
                    durataSecondi = 0,
                    timestamp = System.currentTimeMillis(),
                    ordineEsercizio = newOrder,
                    durataTargetSecondi = targetDurationSeconds,
                    isCompleted = false
                )
                cardioLogId = workoutRepository.saveCardioLog(cardioLog).toInt()
            }

            val initialSets = if (isCardio) emptyList() else (1..targetSets).map { num ->
                val prevSet = previousSets.getOrNull(num - 1)
                val weight = defaultWeight
                val reps = repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 }
                val setDuration = if (isTimeAndWeight) (prevSet?.durataSecondi ?: defaultTargetSeconds) else prevSet?.durataSecondi
                val setLog = SetLogEntity(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    pesoSollevato = weight,
                    repsEffettive = reps,
                    numeroSerie = num,
                    isCompleted = false,
                    ordineEsercizio = newOrder,
                    restTimerSeconds = restTimer,
                    durataSecondi = setDuration
                )
                val logId = workoutRepository.logSet(setLog)
                WorkoutSetState(
                    id = logId.toInt(),
                    setNumber = num,
                    weight = weight,
                    reps = reps,
                    isCompleted = false,
                    previousNote = prevSet?.note,
                    previousReps = prevSet?.repsEffettive,
                    previousWeight = prevSet?.pesoSollevato,
                    timeSeconds = setDuration,
                    previousTimeSeconds = prevSet?.durataSecondi
                )
            }

            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                mutableExercises.add(
                    insertAt,
                    WorkoutExerciseState(
                        exercise = exercise,
                        planDetails = null,
                        sets = initialSets,
                        previousPerformance = prevPerfStr,
                        customRestSeconds = restTimer,
                        customRepsTarget = repsTarget,
                        isCardio = isCardio,
                        cardioCategoria = exercise.categoria,
                        cardioDurataTargetSeconds = targetDurationSeconds,
                        cardioLogId = cardioLogId,
                        exerciseType = exerciseType,
                        timeTargetSeconds = durataTargetSecondi
                    )
                )

                val updatedOrderMap = curr.exerciseExecutionOrder.toMutableMap()
                exercisesToShift.forEach { ex ->
                    val cur = updatedOrderMap[ex.exercise.id]
                    if (cur != null) {
                        updatedOrderMap[ex.exercise.id] = cur + 1
                    }
                }
                updatedOrderMap[exercise.id] = newOrder

                curr.copy(
                    exercises = mutableExercises,
                    currentExerciseIndex = insertAt,
                    exerciseExecutionOrder = updatedOrderMap,
                    nextExecutionOrder = maxOf(curr.nextExecutionOrder, newOrder + 1)
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
        val setDuration = if (exState.isTimeAndWeight) (lastSet?.timeSeconds ?: exState.timeTargetSeconds ?: 45) else lastSet?.timeSeconds

        viewModelScope.launch {
            val executionOrder = currentState.exerciseExecutionOrder[exState.exercise.id] ?: exerciseIndex
            val setLog = SetLogEntity(
                sessionId = sessionId,
                exerciseId = exState.exercise.id,
                pesoSollevato = weight,
                repsEffettive = reps,
                numeroSerie = newSetNumber,
                isCompleted = false,
                ordineEsercizio = executionOrder,
                restTimerSeconds = exState.customRestSeconds,
                durataSecondi = setDuration
            )
            val logId = workoutRepository.logSet(setLog)
            val newSet = WorkoutSetState(
                id = logId.toInt(),
                setNumber = newSetNumber,
                weight = weight,
                reps = reps,
                isCompleted = false,
                timeSeconds = setDuration
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
            val executionOrder = currentState.exerciseExecutionOrder[exState.exercise.id] ?: exerciseIndex
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
                        ordineEsercizio = executionOrder
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
                                ordineEsercizio = executionOrder,
                                restTimerSeconds = exState.customRestSeconds,
                                durataSecondi = s.timeSeconds
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

    private var cardioTimerJob: Job? = null

    private fun saveCardioTimerToSession(seconds: Int, running: Boolean, paused: Boolean, startedAt: Long?) {
        viewModelScope.launch {
            _state.value.sessionId?.let { sessionId ->
                workoutRepository.updateCardioTimer(sessionId, seconds, running, paused, startedAt)
            }
        }
    }

    private fun clearCardioTimerInSession() {
        saveCardioTimerToSession(0, false, false, null)
    }

    fun restartCardioTimerIfNeeded() {
        val state = _state.value
        if (state.cardioTimerRunning && !state.cardioTimerPaused && cardioTimerJob?.isActive != true) {
            val currentEx = state.currentExercise
            if (currentEx != null && currentEx.isCardio && !currentEx.isCardioCompleted) {
                val now = System.currentTimeMillis()
                _state.update { it.copy(cardioTimerStartedAt = now) }
                saveCardioTimerToSession(state.cardioTimerBaseSeconds, true, false, now)
                startCardioTimer()
            }
        }
    }

    fun startCardioTimer() {
        val currState = _state.value
        val sessionId = currState.sessionId ?: return
        val currentEx = currState.currentExercise ?: return
        if (!currentEx.isCardio) return
        if (currentEx.isCardioCompleted) return

        val now = System.currentTimeMillis()

        if (currState.cardioTimerPaused) {
            val baseSeconds = currState.cardioTimerSeconds
            _state.update { it.copy(cardioTimerRunning = true, cardioTimerPaused = false, cardioTimerStartedAt = now, cardioTimerBaseSeconds = baseSeconds) }
            saveCardioTimerToSession(baseSeconds, true, false, now)
        } else if (!currState.cardioTimerRunning) {
            val initialSeconds = currentEx.cardioElapsedSeconds
            viewModelScope.launch {
                val existingLogId = currentEx.cardioLogId
                val logId: Int
                if (existingLogId != null && existingLogId > 0) {
                    logId = existingLogId
                } else {
                    var order = currState.exerciseExecutionOrder[currentEx.exercise.id]
                    if (order == null) {
                        order = currState.nextExecutionOrder
                        _state.update { it.copy(
                            exerciseExecutionOrder = it.exerciseExecutionOrder + (currentEx.exercise.id to order),
                            nextExecutionOrder = order + 1
                        )}
                    }
                    val newLog = com.emanuel5014.trainable.data.local.entity.CardioLogEntity(
                        sessionId = sessionId,
                        categoria = currentEx.exercise.nome,
                        distanza = currentEx.cardioDistanceKm,
                        durataSecondi = initialSeconds,
                        durataTargetSecondi = currentEx.cardioDurataTargetSeconds,
                        timestamp = System.currentTimeMillis(),
                        ordineEsercizio = order,
                        isCompleted = false
                    )
                    logId = workoutRepository.saveCardioLog(newLog).toInt()
                }

                _state.update { state ->
                    val updatedExercises = state.exercises.toMutableList()
                    val idx = state.currentExerciseIndex
                    if (idx in updatedExercises.indices) {
                        updatedExercises[idx] = updatedExercises[idx].copy(cardioLogId = logId)
                    }
                    state.copy(
                        exercises = updatedExercises,
                        cardioTimerSeconds = initialSeconds,
                        cardioTimerRunning = true,
                        cardioTimerPaused = false,
                        cardioTimerStartedAt = now,
                        cardioTimerBaseSeconds = initialSeconds
                    )
                }
                saveCardioTimerToSession(initialSeconds, true, false, now)
            }
        } else {
            val baseSeconds = currState.cardioTimerBaseSeconds
            _state.update { it.copy(cardioTimerStartedAt = now, cardioTimerBaseSeconds = baseSeconds) }
            saveCardioTimerToSession(baseSeconds, true, false, now)
            
            cardioTimerJob?.cancel()
            cardioTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1000L)
                    val state = _state.value
                    if (state.cardioTimerRunning && !state.cardioTimerPaused) {
                        val startedAt = state.cardioTimerStartedAt ?: now
                        val elapsed = state.cardioTimerBaseSeconds + ((System.currentTimeMillis() - startedAt) / 1000).toInt().coerceAtLeast(0)
                        if (maybeAutoPauseCardioAtTarget(elapsed)) break
                        _state.update { s ->
                            val updatedExercises = s.exercises.toMutableList()
                            val idx = s.currentExerciseIndex
                            if (idx in updatedExercises.indices) {
                                updatedExercises[idx] = updatedExercises[idx].copy(cardioElapsedSeconds = elapsed)
                            }
                            s.copy(
                                cardioTimerSeconds = elapsed,
                                exercises = updatedExercises
                            )
                        }
                    }
                }
            }
            return
        }

        cardioTimerJob?.cancel()
        cardioTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _state.value
                if (state.cardioTimerRunning && !state.cardioTimerPaused) {
                    val startedAt = state.cardioTimerStartedAt ?: continue
                    val elapsed = state.cardioTimerBaseSeconds + ((System.currentTimeMillis() - startedAt) / 1000).toInt().coerceAtLeast(0)
                    if (maybeAutoPauseCardioAtTarget(elapsed)) break
                    _state.update { s ->
                        val updatedExercises = s.exercises.toMutableList()
                        val idx = s.currentExerciseIndex
                        if (idx in updatedExercises.indices) {
                            updatedExercises[idx] = updatedExercises[idx].copy(cardioElapsedSeconds = elapsed)
                        }
                        s.copy(
                            cardioTimerSeconds = elapsed,
                            exercises = updatedExercises
                        )
                    }
                }
            }
        }
    }

    /**
     * If auto-stop at target is enabled and [elapsed] reached the cardio target,
     * clamp the timer to the target and pause it. Returns true when auto-paused.
     */
    private fun maybeAutoPauseCardioAtTarget(elapsed: Int): Boolean {
        val state = _state.value
        if (!state.autoStopCardioAtTarget) return false
        val target = state.currentExercise?.cardioDurataTargetSeconds?.takeIf { it > 0 } ?: return false
        if (elapsed < target) return false
        cardioTimerJob?.cancel()
        cardioTimerJob = null
        _state.update { s ->
            val updatedExercises = s.exercises.toMutableList()
            val idx = s.currentExerciseIndex
            if (idx in updatedExercises.indices) {
                updatedExercises[idx] = updatedExercises[idx].copy(cardioElapsedSeconds = target)
            }
            s.copy(
                cardioTimerSeconds = target,
                cardioTimerRunning = false,
                cardioTimerPaused = true,
                cardioTimerStartedAt = null,
                cardioTimerBaseSeconds = target,
                exercises = updatedExercises
            )
        }
        saveCardioTimerToSession(target, false, true, null)
        return true
    }

    fun pauseCardioTimer() {
        cardioTimerJob?.cancel()
        cardioTimerJob = null
        val elapsed = _state.value.cardioTimerSeconds
        _state.update { it.copy(cardioTimerRunning = false, cardioTimerPaused = true, cardioTimerStartedAt = null) }
        saveCardioTimerToSession(elapsed, false, true, null)
    }

    fun stopCardioTimer(distanzaKm: Float) {
        cardioTimerJob?.cancel()
        cardioTimerJob = null
        val currState = _state.value
        val sessionId = currState.sessionId ?: return
        val currentEx = currState.currentExercise ?: return
        val logId = currentEx.cardioLogId

        val elapsed = currState.cardioTimerSeconds
        _state.update { state ->
            val updatedExercises = state.exercises.toMutableList()
            val idx = state.currentExerciseIndex
            if (idx in updatedExercises.indices) {
                updatedExercises[idx] = updatedExercises[idx].copy(
                    cardioElapsedSeconds = elapsed,
                    cardioDistanceKm = distanzaKm,
                    isCardioCompleted = true
                )
            }
            state.copy(
                cardioTimerRunning = false,
                cardioTimerPaused = false,
                cardioTimerSeconds = 0,
                cardioTimerStartedAt = null,
                cardioTimerBaseSeconds = 0,
                exercises = updatedExercises
            )
        }
        clearCardioTimerInSession()

        viewModelScope.launch {
            var order = currState.exerciseExecutionOrder[currentEx.exercise.id]
            if (order == null) {
                order = _state.value.nextExecutionOrder
                _state.update { it.copy(
                    exerciseExecutionOrder = it.exerciseExecutionOrder + (currentEx.exercise.id to order),
                    nextExecutionOrder = order + 1
                )}
            }
            val cardioEntity = com.emanuel5014.trainable.data.local.entity.CardioLogEntity(
                id = logId ?: 0,
                sessionId = sessionId,
                categoria = currentEx.exercise.nome,
                distanza = distanzaKm,
                durataSecondi = elapsed,
                durataTargetSecondi = currentEx.cardioDurataTargetSeconds,
                timestamp = System.currentTimeMillis(),
                ordineEsercizio = order,
                isCompleted = true
            )
            if (logId != null && logId > 0) {
                workoutRepository.updateCardioLog(cardioEntity)
            } else {
                workoutRepository.saveCardioLog(cardioEntity)
            }
        }

        val restSeconds = currentEx.customRestSeconds ?: currentEx.planDetails?.recuperoTarget ?: 0
        if (restSeconds > 0) {
            val exerciseName = ExerciseTranslations.translate(currentEx.exercise.nome, _languageCode.value)
            startRestTimer(restSeconds, exerciseName = exerciseName)
        }
    }

    private var setTimerJob: Job? = null

    private fun saveSetTimerToSession(seconds: Int, running: Boolean, paused: Boolean, startedAt: Long?) {
        viewModelScope.launch {
            _state.value.sessionId?.let { sessionId ->
                workoutRepository.updateSetTimer(sessionId, seconds, running, paused, startedAt)
            }
        }
    }

    private fun clearSetTimerInSession() {
        saveSetTimerToSession(0, false, false, null)
    }

    fun restartSetTimerIfNeeded() {
        val state = _state.value
        if (state.setTimerRunning && !state.setTimerPaused) {
            val now = System.currentTimeMillis()
            val startedAt = state.setTimerStartedAt ?: now
            val elapsed = state.setTimerBaseSeconds + ((now - startedAt) / 1000).toInt().coerceAtLeast(0)
            _state.update { 
                it.copy(
                    setTimerSeconds = elapsed,
                    setTimerBaseSeconds = elapsed,
                    setTimerStartedAt = now
                ) 
            }
            saveSetTimerToSession(elapsed, true, false, now)
            if (setTimerJob?.isActive != true) {
                startSetTimer()
            }
        }
    }

    fun startSetTimer() {
        val currState = _state.value
        val now = System.currentTimeMillis()
        val baseSeconds = currState.setTimerSeconds

        _state.update { 
            it.copy(
                setTimerRunning = true, 
                setTimerPaused = false, 
                setTimerStartedAt = now, 
                setTimerBaseSeconds = baseSeconds,
                setTimerSeconds = baseSeconds
            ) 
        }
        saveSetTimerToSession(baseSeconds, true, false, now)

        setTimerJob?.cancel()
        setTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _state.value
                if (state.setTimerRunning && !state.setTimerPaused) {
                    val startedAt = state.setTimerStartedAt ?: continue
                    val elapsed = state.setTimerBaseSeconds + ((System.currentTimeMillis() - startedAt) / 1000).toInt().coerceAtLeast(0)
                    if (maybeAutoPauseSetTimerAtTarget(elapsed)) break
                    _state.update { s ->
                        s.copy(setTimerSeconds = elapsed)
                    }
                }
            }
        }
    }

    private fun activeTimeWeightTargetSeconds(): Int? {
        val ex = _state.value.currentExercise ?: return null
        if (!ex.isTimeAndWeight) return null
        val activeSet = ex.sets.firstOrNull { !it.isCompleted } ?: return null
        return (activeSet.timeSeconds ?: ex.timeTargetSeconds)?.takeIf { it > 0 }
    }

    /**
     * If auto-stop at target is enabled and [elapsed] reached the active set target,
     * clamp the timer to the target and pause it. Returns true when auto-paused.
     */
    private fun maybeAutoPauseSetTimerAtTarget(elapsed: Int): Boolean {
        if (!_state.value.autoStopTimeWeightAtTarget) return false
        val target = activeTimeWeightTargetSeconds() ?: return false
        if (elapsed < target) return false
        setTimerJob?.cancel()
        setTimerJob = null
        _state.update {
            it.copy(
                setTimerRunning = false,
                setTimerPaused = true,
                setTimerSeconds = target,
                setTimerBaseSeconds = target,
                setTimerStartedAt = null
            )
        }
        saveSetTimerToSession(target, false, true, null)
        return true
    }

    fun pauseSetTimer() {
        setTimerJob?.cancel()
        setTimerJob = null
        val elapsed = _state.value.setTimerSeconds
        _state.update { it.copy(setTimerRunning = false, setTimerPaused = true, setTimerStartedAt = null, setTimerBaseSeconds = elapsed) }
        saveSetTimerToSession(elapsed, false, true, null)
    }

    fun resetSetTimer() {
        setTimerJob?.cancel()
        setTimerJob = null
        _state.update { it.copy(setTimerRunning = false, setTimerPaused = false, setTimerSeconds = 0, setTimerStartedAt = null, setTimerBaseSeconds = 0) }
        clearSetTimerInSession()
    }

    fun finishSetTimer(exerciseIndex: Int, setIndex: Int, weight: Float) {
        val elapsed = _state.value.setTimerSeconds
        val ex = _state.value.exercises.getOrNull(exerciseIndex)
        val set = ex?.sets?.getOrNull(setIndex)
        val targetSeconds = set?.timeSeconds ?: ex?.timeTargetSeconds ?: 45
        val finalDuration = if (elapsed > 0) elapsed else targetSeconds

        resetSetTimer()
        updateSetState(exerciseIndex, setIndex) { it.copy(weight = weight, timeSeconds = finalDuration) }
        toggleSetComplete(exerciseIndex, setIndex)
    }

    fun skipTimerAndLogSet(exerciseIndex: Int, setIndex: Int, weight: Float, durationSeconds: Int) {
        resetSetTimer()
        updateSetState(exerciseIndex, setIndex) { it.copy(weight = weight, timeSeconds = durationSeconds) }
        toggleSetComplete(exerciseIndex, setIndex)
    }

    fun updateSetTimeSeconds(exerciseIndex: Int, setIndex: Int, seconds: Int) {
        val exState = _state.value.exercises.getOrNull(exerciseIndex) ?: return
        val setState = exState.sets.getOrNull(setIndex) ?: return
        viewModelScope.launch {
            if (setState.id != null) {
                workoutRepository.updateSet(
                    SetLogEntity(
                        id = setState.id,
                        sessionId = _state.value.sessionId ?: 0,
                        exerciseId = exState.exercise.id,
                        pesoSollevato = setState.weight,
                        repsEffettive = setState.reps,
                        numeroSerie = setState.setNumber,
                        isWarmup = setState.isWarmup,
                        note = setState.note,
                        supersetId = exState.supersetId,
                        isCompleted = setState.isCompleted,
                        ordineEsercizio = _state.value.exerciseExecutionOrder[exState.exercise.id] ?: exerciseIndex,
                        restTimerSeconds = exState.customRestSeconds,
                        durataSecondi = seconds
                    )
                )
            }
        }
        updateSetState(exerciseIndex, setIndex) { it.copy(timeSeconds = seconds) }
    }
}
