package com.emanuel5014.trainable.ui.screens.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SessionExerciseSwapEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.entity.toPrescriptionBlocks
import com.emanuel5014.trainable.domain.emom.EmomClock
import com.emanuel5014.trainable.domain.emom.EmomPhase
import com.emanuel5014.trainable.domain.prescription.LegacyReps
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.prescription.PlannedSet
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.RepMode
import com.emanuel5014.trainable.domain.prescription.ResolvedPrescription
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.domain.prescription.TechniqueCodec
import com.emanuel5014.trainable.ui.components.prescriptionLabels
import com.emanuel5014.trainable.ui.components.techniqueLabel
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import com.emanuel5014.trainable.util.WeightUnitConverter
import com.emanuel5014.trainable.util.notification.EmomNotificationAction
import com.emanuel5014.trainable.util.notification.EmomNotificationInfo
import com.emanuel5014.trainable.util.notification.TimerNotificationHelper
import com.emanuel5014.trainable.util.notification.TimerNotificationReceiver
import com.emanuel5014.trainable.util.ExerciseMediaMessage
import com.emanuel5014.trainable.util.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val exerciseMediaEnabled: Boolean = false,
    val exerciseMediaLarge: Boolean = true,
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
    /** The EMOM run on the set timer (running or paused), null when none is. */
    val emomRun: EmomRun? = null,
    val autoStopCardioAtTarget: Boolean = false,
    val autoStopTimeWeightAtTarget: Boolean = false,
    val keepScreenOnCardioTimer: Boolean = true,
    val keepScreenOnSetTimer: Boolean = true,
    val sessionStartTime: Long? = null,
    /** Week of a periodized plan this session follows (null for plain routines). */
    val programWeek: Int? = null,
    val weeksCount: Int = 1,
    val autoAdvanceWeek: Boolean = true,
    val loadRoundingIncrement: Float = LoadCalculator.DEFAULT_INCREMENT_KG,
    /** 0 = advanced exercises only, 1 = always, 2 = never. */
    val rpeInputMode: Int = 0,
    /** Pending "new 1RM?" prompts shown before the session is closed. */
    val oneRepMaxSuggestions: List<OneRepMaxSuggestion> = emptyList()
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

/**
 * An EMOM run: [roundCount] consecutive EMOM sets of one exercise, starting at [startSetIndex],
 * one per minute of the set timer.
 */
data class EmomRun(
    val exerciseIndex: Int,
    val startSetIndex: Int,
    val roundCount: Int
)

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
    val timeTargetSeconds: Int? = null,
    /** Advanced prescription blocks for this session's week (empty for plain exercises). */
    val blocks: List<PrescriptionBlock> = emptyList(),
    val oneRepMaxKg: Float? = null
) {
    val isAdvanced: Boolean get() = blocks.isNotEmpty()

    /** Index of the next set to do when it is an EMOM set, i.e. where an EMOM run would start. */
    val emomStartIndex: Int?
        get() = sets.indexOfFirst { !it.isCompleted }.takeIf { it >= 0 && sets[it].isEmom }

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
    val previousTimeSeconds: Int? = null,
    /** Planned %1RM / reps / techniques for this set, null for plain sets. */
    val prescription: PlannedSet? = null,
    val rpe: Float? = null,
    /** Set added on top of the plan (e.g. top singles). */
    val isExtra: Boolean = false
) {
    val isAmrap: Boolean get() = prescription?.repMode == RepMode.AMRAP

    val isEmom: Boolean get() = prescription?.techniques?.contains(Technique.Emom) == true

    /** The load as shown to the lifter, e.g. "100 kg × 3" ([maxLabel] stands in for the reps of an AMRAP set). */
    fun loadText(weightUnit: String, maxLabel: String): String {
        val load = WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(weight, weightUnit), weightUnit)
        return "$load × ${if (isAmrap) maxLabel else reps.toString()}"
    }
}

data class OneRepMaxSuggestion(
    val exerciseId: Int,
    val exerciseName: String,
    val weightKg: Float,
    val reps: Int,
    val currentKg: Float,
    val suggestedKg: Float
)

data class NextSetInfo(
    val exerciseName: String,
    val setNumber: Int,
    val weight: Float,
    val reps: Int,
    val weightUnit: String,
    val previousReps: Int? = null,
    val repsLabel: String? = null,
    val detail: String? = null
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exerciseRepository: ExerciseRepository,
    private val timerNotificationHelper: TimerNotificationHelper,
    private val localeManager: AppLocaleManager,
    private val oneRepMaxRepository: OneRepMaxRepository,
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
        object ProgramCompleted : WorkoutNavEvent()
    }

    /** Mirrors the "advanced programming" setting for code that can't suspend (e.g. finishing a workout). */
    private var advancedEnabled = false

    private companion object {
        /** [WorkoutState.rpeInputMode] value meaning "never ask for RPE". */
        const val RPE_INPUT_NEVER = 2
        /** Fetch every set of the previous session (advanced exercises match them per block). */
        const val ALL_SETS = 500
    }

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _availableExercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    val availableExercises: StateFlow<List<ExerciseEntity>> = _availableExercises.asStateFlow()

    /** Exercise id -> file name of the user's own image/GIF, for the exercises that have one. */
    val exerciseMedia: StateFlow<Map<Int, String>> = _availableExercises
        .map { exercises -> exercises.mapNotNull { exercise -> exercise.mediaPath?.let { exercise.id to it } }.toMap() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _mediaMessages = MutableSharedFlow<ExerciseMediaMessage>(extraBufferCapacity = 1)
    val mediaMessages: SharedFlow<ExerciseMediaMessage> = _mediaMessages.asSharedFlow()

    fun setExerciseMedia(exerciseId: Int, uri: Uri) {
        viewModelScope.launch {
            exerciseRepository.attachExerciseMedia(exerciseId, uri).toMessage()?.let { _mediaMessages.emit(it) }
        }
    }

    fun removeExerciseMedia(exerciseId: Int) {
        viewModelScope.launch { exerciseRepository.removeExerciseMedia(exerciseId) }
    }

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
                    timerNotificationHelper.cancelEmom()
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
            TimerNotificationReceiver.emomEvents.collect { action ->
                when (action) {
                    TimerNotificationReceiver.EmomAction.BOUNDARY -> syncEmomClock()
                    TimerNotificationReceiver.EmomAction.DONE -> completeEmomRound()
                    TimerNotificationReceiver.EmomAction.PAUSE -> pauseEmom()
                    TimerNotificationReceiver.EmomAction.RESUME -> if (_state.value.emomRun != null) startEmom()
                    TimerNotificationReceiver.EmomAction.STOP -> stopEmom()
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
            userPreferencesRepository.loadRoundingIncrement.collect { increment ->
                _state.update { it.copy(loadRoundingIncrement = increment) }
            }
        }

        viewModelScope.launch {
            // With advanced programming off there is no RPE logging at all
            combine(userPreferencesRepository.rpeInputMode, userPreferencesRepository.advancedProgrammingEnabled) { mode, advanced ->
                if (advanced) mode else RPE_INPUT_NEVER
            }.collect { mode ->
                _state.update { it.copy(rpeInputMode = mode) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.advancedProgrammingEnabled.collect { advancedEnabled = it }
        }

        viewModelScope.launch {
            userPreferencesRepository.swipeActionsEnabled.collect { enabled ->
                _state.update { it.copy(swipeActionsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.exerciseMediaEnabled.collect { enabled ->
                _state.update { it.copy(exerciseMediaEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.exerciseMediaLarge.collect { large ->
                _state.update { it.copy(exerciseMediaLarge = large) }
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
        val resumePlan = planWithDetails.plan
        val resumeAdvanced = userPreferencesRepository.advancedProgrammingEnabled.first()
        val resumeWeek = sessionWithSets.session.programWeek
            ?: resumePlan.currentWeek.coerceIn(1, resumePlan.weeksCount.coerceAtLeast(1))
        val resumeUnit = userPreferencesRepository.weightUnit.first()
        val resumeIncrement = userPreferencesRepository.loadRoundingIncrement.first()
        val resumeOneRepMaxes = oneRepMaxRepository.currentByExercise().first()
        
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
            val resumeOneRepMaxKg = resumeOneRepMaxes[exercise.id]?.weightKg
            val planDetailWithBlocks = planDetail?.let { pd -> planExercises.find { it.planExercise.id == pd.id } }
            val resolvedBlocks = if (resumeAdvanced) (planDetailWithBlocks?.resolve(resumeWeek) as? ResolvedPrescription.Blocks)?.blocks else null
            if (resolvedBlocks != null && !isCardio && !isTimeAndWeight && !isSwapped) {
                val previous = getPreviousSetsForExercise(planId, exercise.id, ALL_SETS)
                return WorkoutExerciseState(
                    exercise = exercise,
                    planDetails = planDetail,
                    sets = buildAdvancedSets(resolvedBlocks, loggedSets, previous, resumeOneRepMaxKg, resumeUnit, resumeIncrement),
                    previousPerformance = previous.maxByOrNull { it.pesoSollevato }?.let { "Last: ${it.pesoSollevato}kg × ${it.repsEffettive}" },
                    supersetId = planDetail?.supersetId,
                    exerciseType = "strength",
                    blocks = resolvedBlocks,
                    oneRepMaxKg = resumeOneRepMaxKg
                )
            }
            if (resumeAdvanced && !isCardio && !isTimeAndWeight && loggedSets.any { it.blockIndex != null }) {
                // Quick workouts and swapped exercises have no plan blocks: rebuild them from the snapshots on the logged rows
                val rebuilt = loggedSets.toPrescriptionBlocks()
                if (rebuilt.isNotEmpty()) {
                    val previous = getPreviousSetsForExercise(planId, exercise.id, ALL_SETS)
                    return WorkoutExerciseState(
                        exercise = exercise,
                        planDetails = planDetail,
                        sets = buildAdvancedSets(rebuilt, loggedSets, previous, resumeOneRepMaxKg, resumeUnit, resumeIncrement),
                        previousPerformance = previous.maxByOrNull { it.pesoSollevato }?.let { "Last: ${it.pesoSollevato}kg × ${it.repsEffettive}" },
                        swappedExerciseId = planDetail?.id?.let { swapMap[it] } ?: if (isSwapped) exercise.id else null,
                        supersetId = planDetail?.supersetId ?: loggedSets.firstOrNull()?.supersetId,
                        customRestSeconds = loggedSets.firstOrNull()?.restTimerSeconds,
                        exerciseType = "strength",
                        blocks = rebuilt,
                        oneRepMaxKg = resumeOneRepMaxKg
                    )
                }
            }
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

            // While a workout runs, a weight typed on one set is copied to the following unlogged sets
            // (quick / custom exercises, or ones with no history). Those copies only live in memory, so
            // on resume the same rule is re-applied from the last logged set instead of falling back to 0.
            val carriesWeight = planDetail == null || isSwapped || prevPerfStr == null
            var carriedWeight: Float? = null
            val sets = if (isCardio) emptyList() else (1..targetSets.coerceAtLeast(loggedSets.size)).map { num ->
                val loggedSet = loggedSets.find { it.numeroSerie == num }
                val prevSet = previousSets.getOrNull(num - 1)
                val setDuration = if (isTimeAndWeight) {
                    loggedSet?.durataSecondi ?: prevSet?.durataSecondi ?: defaultTargetSeconds
                } else {
                    loggedSet?.durataSecondi ?: prevSet?.durataSecondi
                }
                if (loggedSet != null) {
                    if (!loggedSet.isWarmup && loggedSet.pesoSollevato > 0f) carriedWeight = loggedSet.pesoSollevato
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
                        previousTimeSeconds = prevSet?.durataSecondi,
                        rpe = loggedSet.rpe,
                        isExtra = loggedSet.isExtra
                    )
                } else {
                    WorkoutSetState(
                        setNumber = num,
                        weight = carriedWeight.takeIf { carriesWeight } ?: prevSet?.pesoSollevato ?: defaultPrevWeight,
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
                timeTargetSeconds = resolvedTargetSeconds,
                oneRepMaxKg = resumeOneRepMaxKg
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
                programWeek = sessionWithSets.session.programWeek.takeIf { resumeAdvanced },
                weeksCount = if (resumeAdvanced) resumePlan.weeksCount else 1,
                autoAdvanceWeek = resumePlan.autoAdvanceWeek && resumeAdvanced,
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
            } else if (targetExState?.emomStartIndex != null) {
                parkEmomRun(finalActiveIndex, targetExState)
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

        val plan = planWithDetails.plan
        val advanced = userPreferencesRepository.advancedProgrammingEnabled.first()
        val week = if (advanced) plan.currentWeek.coerceIn(1, plan.weeksCount.coerceAtLeast(1)) else 1
        val programWeek = if (advanced && plan.weeksCount > 1) week else null
        if (programWeek != null) workoutRepository.setSessionProgramWeek(sessionId, programWeek)
        val unit = userPreferencesRepository.weightUnit.first()
        val increment = userPreferencesRepository.loadRoundingIncrement.first()
        val oneRepMaxes = oneRepMaxRepository.currentByExercise().first()

        val exerciseStates = planWithDetails.exercises.sortedBy { it.planExercise.ordine }.mapNotNull { detail ->
            val resolved = if (advanced) detail.resolve(week) else ResolvedPrescription.Legacy
            if (resolved is ResolvedPrescription.Excluded) return@mapNotNull null
            val oneRepMaxKg = oneRepMaxes[detail.exercise.id]?.weightKg
            if (resolved is ResolvedPrescription.Blocks && detail.planExercise.exerciseType == "strength") {
                val previous = getPreviousSetsForExercise(planId, detail.exercise.id, ALL_SETS)
                return@mapNotNull WorkoutExerciseState(
                    exercise = detail.exercise,
                    planDetails = detail.planExercise,
                    sets = buildAdvancedSets(resolved.blocks, emptyList(), previous, oneRepMaxKg, unit, increment),
                    previousPerformance = previous.maxByOrNull { it.pesoSollevato }?.let { "Last: ${it.pesoSollevato}kg × ${it.repsEffettive}" },
                    supersetId = detail.planExercise.supersetId,
                    exerciseType = "strength",
                    blocks = resolved.blocks,
                    oneRepMaxKg = oneRepMaxKg
                )
            }
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
                timeTargetSeconds = detail.planExercise.durataTargetSecondi,
                oneRepMaxKg = oneRepMaxKg
            )
        }

        _state.update {
            it.copy(
                isLoading = false,
                planId = planId,
                planName = planName,
                sessionId = sessionId,
                programWeek = programWeek,
                weeksCount = if (advanced) plan.weeksCount else 1,
                autoAdvanceWeek = plan.autoAdvanceWeek && advanced,
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

    private fun parseReps(repsTarget: String, targetSets: Int): List<Int> = LegacyReps.parse(repsTarget, targetSets)

    // --- Advanced (%1RM / blocks) prescriptions ---

    private fun SetLogEntity.withSnapshot(set: WorkoutSetState): SetLogEntity {
        val p = set.prescription
        return copy(
            rpe = set.rpe,
            isExtra = set.isExtra,
            targetPercent = p?.percent,
            targetRpe = p?.targetRpe,
            targetReps = p?.let { it.targetReps?.toString() ?: if (it.repMode == RepMode.AMRAP) "MAX" else null },
            repMode = p?.repMode?.code,
            blockIndex = p?.blockIndex,
            techniques = p?.let { TechniqueCodec.encode(it.techniques) },
            targetTotalReps = p?.totalReps
        )
    }

    /** Rebuilds a planned set from the snapshot stored on a log row (used on resume). */
    private fun SetLogEntity.snapshotPrescription(fallback: PlannedSet?): PlannedSet? {
        val block = blockIndex ?: return null
        val mode = RepMode.fromCode(repMode)
        return (fallback ?: PlannedSet(blockIndex = block, indexInBlock = 0, targetReps = null, repMode = mode)).copy(
            blockIndex = block,
            repMode = mode,
            targetReps = targetReps?.toIntOrNull(),
            totalReps = targetTotalReps ?: fallback?.totalReps,
            techniques = if (techniques != null) TechniqueCodec.decode(techniques) else fallback?.techniques.orEmpty()
        ).let { planned ->
            when {
                targetPercent != null -> planned.copy(intensityType = com.emanuel5014.trainable.domain.prescription.IntensityType.PERCENT, intensityValue = targetPercent)
                targetRpe != null -> planned.copy(intensityType = com.emanuel5014.trainable.domain.prescription.IntensityType.RPE, intensityValue = targetRpe)
                else -> planned
            }
        }
    }

    /**
     * Builds the set list of an advanced exercise: planned sets expanded from the blocks,
     * merged with rows already logged in this session (resume) and with the ALSAP follow-up
     * sets / extra sets they may contain. Loads come from %×1RM when possible, otherwise from
     * the same block/position of the previous session.
     */
    private fun buildAdvancedSets(
        blocks: List<PrescriptionBlock>,
        logged: List<SetLogEntity>,
        previous: List<SetLogEntity>,
        oneRepMaxKg: Float?,
        unit: String,
        increment: Float
    ): List<WorkoutSetState> {
        val planned = PrescriptionExpander.expand(blocks)
        val previousByBlock = previous.filter { it.blockIndex != null && !it.isExtra }
            .sortedBy { it.numeroSerie }.groupBy { it.blockIndex!! }
        val loggedSorted = logged.sortedBy { it.numeroSerie }
        val loggedByBlock = loggedSorted.filter { it.blockIndex != null && !it.isExtra }.groupBy { it.blockIndex!! }
        val result = mutableListOf<WorkoutSetState>()

        fun plannedState(p: PlannedSet, lastWeightInBlock: Float?): WorkoutSetState {
            val prev = previousByBlock[p.blockIndex]?.getOrNull(p.indexInBlock)
            val target = LoadCalculator.targetWeightKg(p, oneRepMaxKg, unit, increment)
            val reps = when (p.repMode) {
                RepMode.FIXED -> p.targetReps ?: prev?.repsEffettive ?: 5
                RepMode.AMRAP -> prev?.repsEffettive ?: 5
                RepMode.TOTAL -> (prev?.repsEffettive ?: p.targetReps ?: 5).coerceAtMost(p.targetReps ?: Int.MAX_VALUE)
            }
            return WorkoutSetState(
                setNumber = 0,
                weight = target ?: lastWeightInBlock ?: prev?.pesoSollevato ?: 0f,
                reps = reps,
                previousNote = prev?.note,
                previousReps = prev?.repsEffettive,
                previousWeight = prev?.pesoSollevato,
                prescription = p
            )
        }

        planned.groupBy { it.blockIndex }.toSortedMap().forEach { (blockIndex, blockPlan) ->
            val loggedInBlock = loggedByBlock[blockIndex].orEmpty()
            var lastWeight: Float? = null
            loggedInBlock.forEachIndexed { i, row ->
                val fallback = blockPlan.getOrNull(i) ?: blockPlan.last().copy(indexInBlock = i)
                lastWeight = row.pesoSollevato
                result += WorkoutSetState(
                    id = row.id,
                    setNumber = 0,
                    weight = row.pesoSollevato,
                    reps = row.repsEffettive,
                    note = row.note,
                    isCompleted = row.isCompleted,
                    isWarmup = row.isWarmup,
                    timeSeconds = row.durataSecondi,
                    prescription = row.snapshotPrescription(fallback) ?: fallback,
                    rpe = row.rpe
                )
            }
            val template = blockPlan.first()
            if (template.repMode == RepMode.TOTAL) {
                val done = loggedInBlock.filter { it.isCompleted }.sumOf { it.repsEffettive }
                val remaining = (template.totalReps ?: 0) - done
                if (remaining > 0 && loggedInBlock.none { !it.isCompleted }) {
                    val next = template.copy(indexInBlock = loggedInBlock.size, targetReps = remaining)
                    result += plannedState(next, lastWeight).copy(reps = if (loggedInBlock.isEmpty()) plannedState(next, lastWeight).reps else remaining)
                }
            } else {
                blockPlan.drop(loggedInBlock.size).forEach { p ->
                    val state = plannedState(p, lastWeight)
                    lastWeight = state.weight
                    result += state
                }
            }
        }
        // Extra sets and rows without a block (e.g. added manually) keep their logged order at the end
        loggedSorted.filter { it.isExtra || it.blockIndex == null }.forEach { row ->
            result += WorkoutSetState(
                id = row.id,
                setNumber = 0,
                weight = row.pesoSollevato,
                reps = row.repsEffettive,
                note = row.note,
                isCompleted = row.isCompleted,
                isWarmup = row.isWarmup,
                timeSeconds = row.durataSecondi,
                rpe = row.rpe,
                isExtra = row.isExtra
            )
        }
        return result.mapIndexed { i, set -> set.copy(setNumber = i + 1) }
    }

    /** Creates and persists the rows of a freshly added advanced exercise (add / swap / quick workout). */
    private suspend fun createAdvancedRows(
        sessionId: Int,
        exerciseId: Int,
        order: Int,
        supersetId: String?,
        restTimer: Int?,
        blocks: List<PrescriptionBlock>,
        previous: List<SetLogEntity>,
        oneRepMaxKg: Float?
    ): List<WorkoutSetState> {
        val built = buildAdvancedSets(
            blocks = blocks,
            logged = emptyList(),
            previous = previous,
            oneRepMaxKg = oneRepMaxKg,
            unit = _state.value.weightUnit,
            increment = _state.value.loadRoundingIncrement
        )
        return built.map { set ->
            val id = workoutRepository.logSet(
                SetLogEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    pesoSollevato = set.weight,
                    repsEffettive = set.reps,
                    numeroSerie = set.setNumber,
                    isCompleted = false,
                    ordineEsercizio = order,
                    restTimerSeconds = restTimer,
                    supersetId = supersetId
                ).withSnapshot(set)
            )
            set.copy(id = id.toInt())
        }
    }

    private fun prescriptionDetail(set: WorkoutSetState?): String? {
        val p = set?.prescription ?: return if (set?.isExtra == true) localeManager.getString(R.string.extra_badge) else null
        val context = localeManager.localizedContext()
        val labels = prescriptionLabels(context, _state.value.weightUnit)
        val parts = listOfNotNull(PrescriptionFormatter.intensity(p.intensityType, p.intensityValue, labels)) +
            p.techniques.map { techniqueLabel(context, it) }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private fun repsLabelFor(set: WorkoutSetState?): String? =
        if (set?.isAmrap == true) localeManager.getString(R.string.max_label) else null

    /** Rest after a set: block override, EMOM (rest of the minute), then exercise / plan rest. */
    private fun restSecondsAfter(exState: WorkoutExerciseState, set: WorkoutSetState): Int {
        val p = set.prescription
        if (p != null && Technique.Emom in p.techniques) {
            return (60 - set.reps * 3).coerceIn(20, 55)
        }
        return p?.restSeconds ?: exState.customRestSeconds ?: exState.planDetails?.recuperoTarget ?: 90
    }

    fun updateSetRpe(exerciseIndex: Int, setIndex: Int, rpe: Float?) {
        updateSetState(exerciseIndex, setIndex) { it.copy(rpe = rpe) }
    }

    /** Adds a set on top of the plan (e.g. top singles), prefilled with the last load and 1 rep. */
    fun addExtraSet(exerciseIndex: Int) {
        val currentState = _state.value
        val sessionId = currentState.sessionId ?: return
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val lastSet = exState.sets.lastOrNull { it.isCompleted } ?: exState.sets.lastOrNull()
        val newSet = WorkoutSetState(
            setNumber = exState.sets.size + 1,
            weight = lastSet?.weight ?: 0f,
            reps = 1,
            isExtra = true
        )
        viewModelScope.launch {
            val executionOrder = currentState.exerciseExecutionOrder[exState.exercise.id] ?: exerciseIndex
            val logId = workoutRepository.logSet(
                SetLogEntity(
                    sessionId = sessionId,
                    exerciseId = exState.exercise.id,
                    pesoSollevato = newSet.weight,
                    repsEffettive = newSet.reps,
                    numeroSerie = newSet.setNumber,
                    isCompleted = false,
                    ordineEsercizio = executionOrder,
                    supersetId = exState.supersetId,
                    restTimerSeconds = exState.customRestSeconds
                ).withSnapshot(newSet)
            )
            _state.update { curr ->
                val exercises = curr.exercises.toMutableList()
                val inner = exercises.getOrNull(exerciseIndex) ?: return@update curr
                exercises[exerciseIndex] = inner.copy(sets = inner.sets + newSet.copy(id = logId.toInt(), setNumber = inner.sets.size + 1))
                curr.copy(exercises = exercises)
            }
        }
    }

    /** The follow-up set a TOTAL (ALSAP) block needs after completing [setIndex], if any. */
    private fun totalFollowUpSet(exState: WorkoutExerciseState, setIndex: Int): WorkoutSetState? {
        val set = exState.sets.getOrNull(setIndex) ?: return null
        val p = set.prescription?.takeIf { it.repMode == RepMode.TOTAL } ?: return null
        val blockSets = exState.sets.filter { it.prescription?.blockIndex == p.blockIndex }
        if (blockSets.any { !it.isCompleted && it !== set }) return null
        val done = blockSets.filter { it.isCompleted || it === set }.sumOf { it.reps }
        val next = PrescriptionExpander.nextTotalRepsSet(p.copy(indexInBlock = blockSets.size - 1), done) ?: return null
        return WorkoutSetState(setNumber = set.setNumber + 1, weight = set.weight, reps = next.targetReps ?: 1, prescription = next)
    }

    /**
     * Keeps a TOTAL (ALSAP) block consistent with the reps done so far: adds the next set with the
     * remaining reps, updates it, or removes pending sets once the target is reached.
     */
    private fun reconcileTotalBlock(exerciseIndex: Int, blockIndex: Int) {
        val curr = _state.value
        val exState = curr.exercises.getOrNull(exerciseIndex) ?: return
        val blockEntries = exState.sets.withIndex().filter {
            it.value.prescription?.blockIndex == blockIndex && it.value.prescription?.repMode == RepMode.TOTAL
        }
        if (blockEntries.isEmpty()) return
        val template = blockEntries.first().value.prescription ?: return
        val total = template.totalReps ?: return
        val remaining = total - blockEntries.filter { it.value.isCompleted }.sumOf { it.value.reps }
        val pending = blockEntries.filter { !it.value.isCompleted }
        val sets = exState.sets.toMutableList()
        val toDelete = mutableListOf<WorkoutSetState>()

        when {
            remaining > 0 && pending.isEmpty() -> {
                val last = blockEntries.last()
                sets.add(
                    last.index + 1,
                    WorkoutSetState(
                        setNumber = 0,
                        weight = last.value.weight,
                        reps = remaining,
                        prescription = template.copy(indexInBlock = blockEntries.size, targetReps = remaining)
                    )
                )
            }
            remaining <= 0 && pending.isNotEmpty() -> {
                pending.sortedByDescending { it.index }.forEach { entry ->
                    sets.removeAt(entry.index)
                    if (entry.value.id != null) toDelete += entry.value
                }
            }
            remaining > 0 -> {
                val first = pending.first()
                sets[first.index] = first.value.copy(
                    reps = remaining,
                    prescription = first.value.prescription?.copy(targetReps = remaining)
                )
            }
            else -> return
        }

        val renumbered = sets.mapIndexed { i, set -> set.copy(setNumber = i + 1) }
        val changed = renumbered.filter { set -> set.id != null && exState.sets.none { it.id == set.id && it.setNumber == set.setNumber && it.reps == set.reps } }
        _state.update { state ->
            val exercises = state.exercises.toMutableList()
            val inner = exercises.getOrNull(exerciseIndex) ?: return@update state
            exercises[exerciseIndex] = inner.copy(sets = renumbered)
            state.copy(exercises = exercises)
        }
        val sessionId = curr.sessionId ?: return
        val executionOrder = curr.exerciseExecutionOrder[exState.exercise.id] ?: exerciseIndex
        viewModelScope.launch {
            toDelete.forEach { set ->
                workoutRepository.deleteSet(
                    SetLogEntity(
                        id = set.id!!, sessionId = sessionId, exerciseId = exState.exercise.id,
                        pesoSollevato = set.weight, repsEffettive = set.reps, numeroSerie = set.setNumber
                    )
                )
            }
            changed.forEach { set ->
                workoutRepository.updateSet(
                    SetLogEntity(
                        id = set.id!!,
                        sessionId = sessionId,
                        exerciseId = exState.exercise.id,
                        pesoSollevato = set.weight,
                        repsEffettive = set.reps,
                        numeroSerie = set.setNumber,
                        isWarmup = set.isWarmup,
                        note = set.note,
                        supersetId = exState.supersetId,
                        isCompleted = set.isCompleted,
                        ordineEsercizio = executionOrder,
                        restTimerSeconds = exState.customRestSeconds,
                        durataSecondi = set.timeSeconds
                    ).withSnapshot(set)
                )
            }
        }
    }

    /** New-1RM candidates: a single above the current 1RM, or a low-rep set whose e1RM beats it by >2.5%. */
    private fun computeOneRepMaxSuggestions(): List<OneRepMaxSuggestion> =
        if (!advancedEnabled) emptyList() else _state.value.exercises.mapNotNull { ex ->
            val current = ex.oneRepMaxKg ?: return@mapNotNull null
            if (ex.isCardio || ex.isTimeAndWeight) return@mapNotNull null
            val best = ex.sets
                .filter { it.isCompleted && !it.isWarmup && it.weight > 0f && it.reps in 1..5 }
                .maxByOrNull { LoadCalculator.epley(it.weight, it.reps) } ?: return@mapNotNull null
            val estimate = LoadCalculator.epley(best.weight, best.reps)
            val beats = if (best.reps == 1) best.weight > current else estimate > current * 1.025f
            if (!beats) return@mapNotNull null
            OneRepMaxSuggestion(
                exerciseId = ex.exercise.id,
                exerciseName = ExerciseTranslations.translate(ex.exercise.nome, _languageCode.value),
                weightKg = best.weight,
                reps = best.reps,
                currentKg = current,
                suggestedKg = if (best.reps == 1) best.weight else LoadCalculator.roundTo(estimate, 0.5f)
            )
        }

    /** Handles the first pending "new 1RM?" prompt; the session closes once none are left. */
    fun resolveOneRepMaxSuggestion(accept: Boolean) {
        val suggestion = _state.value.oneRepMaxSuggestions.firstOrNull() ?: return
        viewModelScope.launch {
            if (accept) {
                oneRepMaxRepository.add(
                    exerciseId = suggestion.exerciseId,
                    weightKg = suggestion.suggestedKg,
                    source = if (suggestion.reps == 1) OneRepMaxEntity.SOURCE_TESTED else OneRepMaxEntity.SOURCE_ESTIMATED
                )
            }
            val remaining = _state.value.oneRepMaxSuggestions.drop(1)
            _state.update { it.copy(oneRepMaxSuggestions = remaining) }
            if (remaining.isEmpty()) performFinish()
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
                        ).withSnapshot(updatedSet)
                    )
                }
            }
            
            val isQuickOrCustom = curr.isQuickWorkout || exState.swappedExerciseId != null || exState.planDetails == null
            val shouldPropagate = (isQuickOrCustom || exState.previousPerformance == null) && !exState.isAdvanced

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
                                    ).withSnapshot(propSet)
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
                        ).withSnapshot(updatedSet)
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
                    ).withSnapshot(setState)
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
                    ).withSnapshot(setState)
                )
                newSetId = logId.toInt()
                
                val isLastExercise = exerciseIndex == currentState.exercises.size - 1
                val isLastSet = setIndex == exState.sets.size - 1
                
                // An EMOM run on the minute clock already paces the rest: no rest timer on top of it.
                var shouldStartTimer = !(currentState.emomRun != null && setState.isEmom)
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
                
                val totalFollowUp = if (newIsCompleted) totalFollowUpSet(exState, setIndex) else null
                if (newIsCompleted) {
                    if (shouldStartTimer && (!(isLastExercise && isLastSet) || totalFollowUp != null)) {
                        val restTime = restSecondsAfter(exState, setState)
                        val nextSet = totalFollowUp ?: exState.sets.drop(setIndex + 1).firstOrNull { !it.isCompleted }
                        if (nextSet != null) {
                            val translatedName = ExerciseTranslations.translate(exState.exercise.nome, _languageCode.value)
                            val plannedReps = if (exState.isAdvanced) nextSet.prescription?.targetReps ?: nextSet.reps
                                else getPlannedRepsForSet(exState, nextSet.setNumber) ?: nextSet.reps
                            startRestTimer(
                                restTime, translatedName, nextSet.setNumber, nextSet.weight, plannedReps, nextSet.previousReps, currentState.weightUnit,
                                nextSetRepsLabel = repsLabelFor(nextSet),
                                nextSetDetail = prescriptionDetail(nextSet)
                            )
                        } else if (!isLastExercise) {
                            val nextExState = currentState.exercises.getOrNull(exerciseIndex + 1)
                            val firstSet = nextExState?.sets?.firstOrNull()
                            if (firstSet != null) {
                                val translatedName = ExerciseTranslations.translate(nextExState.exercise.nome, _languageCode.value)
                                val plannedReps = getPlannedRepsForSet(nextExState, firstSet.setNumber) ?: firstSet.reps
                                startRestTimer(
                                    restTime, translatedName, firstSet.setNumber, firstSet.weight, plannedReps, firstSet.previousReps, currentState.weightUnit,
                                    nextSetRepsLabel = repsLabelFor(firstSet),
                                    nextSetDetail = prescriptionDetail(firstSet)
                                )
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

            setState.prescription?.takeIf { it.repMode == RepMode.TOTAL }?.let { reconcileTotalBlock(exerciseIndex, it.blockIndex) }

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
                        ).withSnapshot(updatedSet)
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

        if (_state.value.isNavigating || _state.value.isFinishing || _state.value.emomRun != null) return

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

        if (_state.value.isNavigating || _state.value.isFinishing || _state.value.emomRun != null) return

        val currentIndex = _state.value.currentExerciseIndex
        val maxIndex = _state.value.exercises.size - 1
        if (currentIndex < maxIndex) {
            _state.update { it.copy(isNavigating = true) }
            _state.update { it.copy(currentExerciseIndex = currentIndex + 1, isNavigating = false) }
        }
    }

    private var oneRepMaxReviewDone = false

    fun finishWorkout() {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < actionDebounce) return
        lastActionTime = now

        if (_state.value.isFinishing) return
        if (!oneRepMaxReviewDone) {
            oneRepMaxReviewDone = true
            val suggestions = computeOneRepMaxSuggestions()
            if (suggestions.isNotEmpty()) {
                _state.update { it.copy(oneRepMaxSuggestions = suggestions) }
                return
            }
        }
        performFinish()
    }

    private fun performFinish() {
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
                    advanceProgramWeek()
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
                        setTimerBaseSeconds = 0,
                        emomRun = null
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

    /** Moves a periodized plan to its next week after a finished session. */
    private suspend fun advanceProgramWeek() {
        val state = _state.value
        val week = state.programWeek ?: return
        val planId = state.planId ?: return
        if (!state.autoAdvanceWeek || state.weeksCount <= 1) return
        if (week < state.weeksCount) {
            workoutRepository.setPlanCurrentWeek(planId, week + 1)
        } else {
            _navigationEvent.emit(WorkoutNavEvent.ProgramCompleted)
        }
    }

    private fun startRestTimer(
        seconds: Int,
        exerciseName: String? = null,
        nextSetNumber: Int? = null,
        nextSetWeight: Float? = null,
        nextSetReps: Int? = null,
        previousReps: Int? = null,
        weightUnit: String? = null,
        nextSetRepsLabel: String? = null,
        nextSetDetail: String? = null
    ) {
        if (seconds <= 0) return
        stopRestTimer()
        val endTime = System.currentTimeMillis() + (seconds * 1000L)
        _state.update { it.copy(remainingRestSeconds = seconds, totalRestSeconds = seconds, restTimerEndTime = endTime) }
        if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
            _state.value.sessionId?.let { sessionId ->
                timerNotificationHelper.startOrUpdateTimerNotification(
                    seconds, sessionId, exerciseName, nextSetNumber, nextSetWeight, nextSetReps, previousReps, weightUnit,
                    totalSeconds = seconds,
                    nextSetRepsLabel = nextSetRepsLabel,
                    nextSetDetail = nextSetDetail
                )
            }
        }
        saveTimerToSession(endTime, seconds)
        startTimerJob()
    }

    private fun getPlannedRepsForSet(exState: WorkoutExerciseState?, setNumber: Int): Int? {
        if (exState?.isAdvanced == true) {
            val set = exState.sets.firstOrNull { it.setNumber == setNumber }
            return set?.prescription?.targetReps ?: set?.reps
        }
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
                previousReps = nextSet.previousReps,
                repsLabel = repsLabelFor(nextSet),
                detail = prescriptionDetail(nextSet)
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
                        info?.exerciseName, info?.setNumber, info?.weight, info?.reps, info?.previousReps, info?.weightUnit,
                        totalSeconds = totalSeconds,
                        nextSetRepsLabel = info?.repsLabel,
                        nextSetDetail = info?.detail
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
                    _state.update { it.copy(remainingRestSeconds = remaining) }
                    if (timerNotificationHelper.isLiveNotificationSupported() && _state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
                        _state.value.sessionId?.let { sessionId ->
                            val info = buildNextSetLabel()
                            timerNotificationHelper.startOrUpdateTimerNotification(
                                remainingSeconds = remaining,
                                sessionId = sessionId,
                                exerciseName = info?.exerciseName,
                                nextSetNumber = info?.setNumber,
                                nextSetWeight = info?.weight,
                                nextSetReps = info?.reps,
                                previousReps = info?.previousReps,
                                weightUnit = info?.weightUnit,
                                totalSeconds = _state.value.totalRestSeconds,
                                scheduleAlarm = false,
                                nextSetRepsLabel = info?.repsLabel,
                                nextSetDetail = info?.detail
                            )
                        }
                    }
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
                        info?.exerciseName, info?.setNumber, info?.weight, info?.reps, info?.previousReps, info?.weightUnit,
                        totalSeconds = newTotal,
                        nextSetRepsLabel = info?.repsLabel,
                        nextSetDetail = info?.detail
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
            timerNotificationHelper.startOrUpdateWarmupTimerNotification(seconds, totalSeconds = seconds)
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
                timerNotificationHelper.startOrUpdateWarmupTimerNotification(newRemaining, totalSeconds = newTotal)
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
                    if (timerNotificationHelper.isLiveNotificationSupported() && _state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
                        timerNotificationHelper.startOrUpdateWarmupTimerNotification(
                            remainingSeconds = remaining,
                            totalSeconds = _state.value.warmupTimerTotalSeconds,
                            scheduleAlarm = false
                        )
                    }
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
                timerNotificationHelper.startOrUpdateWarmupTimerNotification(remaining, totalSeconds = totalSeconds)
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
        durataTargetSecondi: Int? = null,
        blocks: List<PrescriptionBlock>? = null
    ) {
        val currentState = _state.value
        val sessionId = currentState.sessionId ?: return
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val originalExerciseId = exState.planDetails?.id

        val replacementExercise = _availableExercises.value.find { it.id == newExerciseId } ?: return

        viewModelScope.launch {
            // Delete any existing completed/saved sets of the old exercise from the database for this session
            workoutRepository.deleteExerciseFromSession(sessionId, exState.exercise.id)

            val advancedBlocks = blocks.orEmpty()
            val newOneRepMaxKg = if (advancedBlocks.isNotEmpty()) oneRepMaxRepository.currentOnce(newExerciseId)?.weightKg else null
            
            val previousSets = getPreviousSetsForExercise(currentState.planId, newExerciseId, if (advancedBlocks.isNotEmpty()) ALL_SETS else targetSets)
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

            val initialSets = if (advancedBlocks.isNotEmpty()) {
                createAdvancedRows(sessionId, newExerciseId, executionOrder, exState.supersetId, restTimer, advancedBlocks, previousSets, newOneRepMaxKg)
            } else (1..targetSets).map { num ->
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
                        timeTargetSeconds = durataTargetSecondi,
                        blocks = advancedBlocks,
                        oneRepMaxKg = newOneRepMaxKg
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
                        timeTargetSeconds = durataTargetSecondi,
                        blocks = advancedBlocks,
                        oneRepMaxKg = newOneRepMaxKg
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
                            ).withSnapshot(s)
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
                            ).withSnapshot(s)
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
        durataTargetSecondi: Int? = null,
        blocks: List<PrescriptionBlock>? = null
    ) {
        val sessionId = _state.value.sessionId ?: return
        val isCardio = exercise.categoria.equals("Cardio", ignoreCase = true) || exerciseType == "cardio"
        val isTimeAndWeight = exerciseType == "time_and_weight"
        val targetDurationSeconds = if (isCardio) (cardioDurationMinutes ?: 15) * 60 else durataTargetSecondi
        val defaultTargetSeconds = durataTargetSecondi ?: 45
        val advancedBlocks = if (isCardio) emptyList() else blocks.orEmpty()

        viewModelScope.launch {
            val currentState = _state.value
            val executionOrder = currentState.nextExecutionOrder
            val newOneRepMaxKg = if (advancedBlocks.isNotEmpty()) oneRepMaxRepository.currentOnce(exercise.id)?.weightKg else null

            val previousSets = if (isCardio) emptyList() else getPreviousSetsForExercise(currentState.planId, exercise.id, if (advancedBlocks.isNotEmpty()) ALL_SETS else targetSets)
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
            
            val initialSets = if (advancedBlocks.isNotEmpty()) {
                createAdvancedRows(sessionId, exercise.id, executionOrder, null, restTimer, advancedBlocks, previousSets, newOneRepMaxKg)
            } else if (isCardio) emptyList() else (1..targetSets).map { num ->
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
                        timeTargetSeconds = durataTargetSecondi,
                        blocks = advancedBlocks,
                        oneRepMaxKg = newOneRepMaxKg
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
        durataTargetSecondi: Int? = null,
        blocks: List<PrescriptionBlock>? = null
    ) {
        val sessionId = _state.value.sessionId ?: return
        val isCardio = exercise.categoria.equals("Cardio", ignoreCase = true) || exerciseType == "cardio"
        val isTimeAndWeight = exerciseType == "time_and_weight"
        val targetDurationSeconds = if (isCardio) (cardioDurationMinutes ?: 15) * 60 else durataTargetSecondi
        val defaultTargetSeconds = durataTargetSecondi ?: 45
        val advancedBlocks = if (isCardio) emptyList() else blocks.orEmpty()

        viewModelScope.launch {
            val currentState = _state.value
            val newOneRepMaxKg = if (advancedBlocks.isNotEmpty()) oneRepMaxRepository.currentOnce(exercise.id)?.weightKg else null
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
                            ).withSnapshot(set)
                        )
                    }
                }
            }

            if (setsToUpdate.isNotEmpty()) {
                workoutRepository.updateSetOrders(setsToUpdate)
            }

            val previousSets = if (isCardio) emptyList() else getPreviousSetsForExercise(currentState.planId, exercise.id, if (advancedBlocks.isNotEmpty()) ALL_SETS else targetSets)
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

            val initialSets = if (advancedBlocks.isNotEmpty()) {
                createAdvancedRows(sessionId, exercise.id, newOrder, null, restTimer, advancedBlocks, previousSets, newOneRepMaxKg)
            } else if (isCardio) emptyList() else (1..targetSets).map { num ->
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
                        timeTargetSeconds = durataTargetSecondi,
                        blocks = advancedBlocks,
                        oneRepMaxKg = newOneRepMaxKg
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
        // On advanced exercises a new set repeats the last one's prescription (same block)
        val prescription = lastSet?.prescription?.takeIf { it.repMode != RepMode.TOTAL }?.let { it.copy(indexInBlock = it.indexInBlock + 1) }
        val template = WorkoutSetState(
            setNumber = newSetNumber,
            weight = weight,
            reps = reps,
            isCompleted = false,
            timeSeconds = setDuration,
            prescription = prescription,
            isExtra = lastSet?.isExtra == true
        )

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
            ).withSnapshot(template)
            val logId = workoutRepository.logSet(setLog)
            val newSet = template.copy(id = logId.toInt())
            
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
                            ).withSnapshot(s)
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
            val emomRun = _state.value.emomRun
            if (emomRun != null && handleEmomTick(emomRun, elapsed)) return
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
                delay(setTimerTickDelay())
                val state = _state.value
                if (state.setTimerRunning && !state.setTimerPaused) {
                    val startedAt = state.setTimerStartedAt ?: continue
                    val elapsed = state.setTimerBaseSeconds + ((System.currentTimeMillis() - startedAt) / 1000).toInt().coerceAtLeast(0)
                    if (maybeAutoPauseSetTimerAtTarget(elapsed)) break
                    _state.update { s ->
                        s.copy(setTimerSeconds = elapsed)
                    }
                    val emomRun = _state.value.emomRun
                    if (emomRun != null) {
                        if (handleEmomTick(emomRun, elapsed)) break
                        // The live bar fills second by second; older versions count down on their own.
                        if (timerNotificationHelper.isLiveNotificationSupported() && emomLastNotifiedElapsed != elapsed) {
                            refreshEmomNotification(scheduleAlarm = false)
                        }
                    }
                }
            }
        }
    }

    /**
     * A plain one-second tick. An EMOM run needs its rounds to start on the dot, so its ticks
     * are aligned to the whole seconds of the clock instead.
     */
    private fun setTimerTickDelay(): Long {
        val state = _state.value
        val startedAt = state.setTimerStartedAt
        if (state.emomRun == null || startedAt == null) return 1000L
        val intoSecond = (System.currentTimeMillis() - startedAt).mod(1000L)
        return 1000L - intoSecond + 5L
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

    // ---- EMOM ----
    // An EMOM run lives on the set timer: it counts up from EmomClock.START_ELAPSED (a get-ready
    // countdown) and every 60 s from zero a new round starts. Pause, resume, persistence and
    // keeping the screen on all come from the set timer; the run only adds what each minute means.
    // An ongoing notification (a live update from Android 16) counts down to the next round, and an
    // exact alarm per round keeps the run on time while the screen is off.

    /** Last minute of the clock whose boundary was handled; -1 is the lead-in. */
    private var emomLastMinute = EmomClock.minuteIndex(EmomClock.START_ELAPSED)
    private var emomLastCueElapsed: Int? = null
    private var emomLastNotifiedElapsed: Int? = null
    /** Sets of the run already sent to be logged, so a round is never logged twice. */
    private val emomRequested = mutableSetOf<Int>()

    /** Starts the EMOM run at the next set, or resumes a paused one. */
    fun startEmom() {
        val state = _state.value
        if (state.emomRun != null) {
            startSetTimer()
            refreshEmomNotification()
            return
        }
        val exerciseIndex = state.currentExerciseIndex
        val exercise = state.currentExercise ?: return
        val startIndex = exercise.emomStartIndex ?: return
        val rounds = EmomClock.runLength(exercise.sets.map { it.isEmom }, startIndex)
        stopRestTimer()
        resetEmomTracking()
        _state.update {
            it.copy(
                emomRun = EmomRun(exerciseIndex, startIndex, rounds),
                setTimerSeconds = EmomClock.START_ELAPSED,
                setTimerBaseSeconds = EmomClock.START_ELAPSED
            )
        }
        startSetTimer()
        refreshEmomNotification()
    }

    fun pauseEmom() {
        if (_state.value.emomRun == null) return
        pauseSetTimer()
        timerNotificationHelper.cancelEmomBoundaryAlarm()
        refreshEmomNotification(scheduleAlarm = false)
    }

    /** Ends the run, leaving the sets not logged yet to do. */
    fun stopEmom() {
        if (_state.value.emomRun != null) endEmomRun(completed = false)
    }

    /** The lifter is done with the round on the clock: log it and rest until the next minute. */
    fun completeEmomRound() {
        val state = _state.value
        val run = state.emomRun ?: return
        if (!state.setTimerRunning) return
        val minute = EmomClock.minuteIndex(state.setTimerSeconds)
        if (minute !in 0 until run.roundCount) return
        logEmomSet(run, run.startSetIndex + minute)
        if (minute == run.roundCount - 1) {
            // Nothing is left to wait for after the last round.
            endEmomRun(completed = true, byClock = false)
        } else {
            refreshEmomNotification(phaseOverride = EmomPhase.Rest, scheduleAlarm = false)
        }
    }

    /**
     * After the workout is reopened an EMOM run that was on the clock comes back paused before the
     * next set, ready to start again with a fresh get-ready countdown.
     */
    private fun parkEmomRun(exerciseIndex: Int, exercise: WorkoutExerciseState) {
        val startIndex = exercise.emomStartIndex ?: return
        val rounds = EmomClock.runLength(exercise.sets.map { it.isEmom }, startIndex)
        resetEmomTracking()
        _state.update {
            it.copy(
                emomRun = EmomRun(exerciseIndex, startIndex, rounds),
                setTimerRunning = false,
                setTimerPaused = true,
                setTimerSeconds = EmomClock.START_ELAPSED,
                setTimerBaseSeconds = EmomClock.START_ELAPSED,
                setTimerStartedAt = null
            )
        }
        saveSetTimerToSession(EmomClock.START_ELAPSED, false, true, null)
    }

    private fun resetEmomTracking() {
        emomLastMinute = EmomClock.minuteIndex(EmomClock.START_ELAPSED)
        emomLastCueElapsed = null
        emomLastNotifiedElapsed = null
        emomRequested.clear()
    }

    /**
     * Applies what the clock reading [elapsed] means for the run: the minute that just ended gets
     * its round logged when the lifter did not, the next round is announced, and the last seconds
     * before a round tick. Returns true when the run is over.
     */
    private fun handleEmomTick(run: EmomRun, elapsed: Int): Boolean {
        val minute = EmomClock.minuteIndex(elapsed)
        var roundStarted = false
        while (emomLastMinute < minute) {
            emomLastMinute++
            if (emomLastMinute >= 1) logEmomSet(run, run.startSetIndex + emomLastMinute - 1)
            if (emomLastMinute >= run.roundCount) {
                endEmomRun(completed = true)
                return true
            }
            roundStarted = true
        }
        when {
            roundStarted -> {
                emomCue(TimerNotificationHelper.EmomCue.GO)
                refreshEmomNotification()
            }
            EmomClock.isCountdownTick(elapsed) && elapsed != emomLastCueElapsed ->
                emomCue(TimerNotificationHelper.EmomCue.TICK)
        }
        emomLastCueElapsed = elapsed
        return false
    }

    /**
     * Catches the run up with the wall clock, for when a round alarm wakes the app while the screen
     * is off and the one-second ticks are not running.
     */
    private fun syncEmomClock() {
        val state = _state.value
        val run = state.emomRun ?: return
        val startedAt = state.setTimerStartedAt ?: return
        if (!state.setTimerRunning || state.setTimerPaused) return
        // A little tolerance: the alarm may land a few milliseconds before the clock reads the boundary.
        val elapsed = state.setTimerBaseSeconds + ((System.currentTimeMillis() - startedAt + 250L) / 1000).toInt().coerceAtLeast(0)
        _state.update { it.copy(setTimerSeconds = elapsed) }
        handleEmomTick(run, elapsed)
    }

    private fun logEmomSet(run: EmomRun, setIndex: Int) {
        val set = _state.value.exercises.getOrNull(run.exerciseIndex)?.sets?.getOrNull(setIndex) ?: return
        if (set.isCompleted || !emomRequested.add(setIndex)) return
        toggleSetComplete(run.exerciseIndex, setIndex)
    }

    /** Ends the run. [byClock] tells whether the minutes ran out, in which case the lifter is told in a notification. */
    private fun endEmomRun(completed: Boolean, byClock: Boolean = completed) {
        val state = _state.value
        val exercise = state.emomRun?.let { state.exercises.getOrNull(it.exerciseIndex) }
        resetEmomTracking()
        resetSetTimer()
        _state.update { it.copy(emomRun = null) }
        if (completed && byClock && exercise != null && emomNotificationsAllowed()) {
            val context = localeManager.localizedContext()
            timerNotificationHelper.showEmomFinished(
                context.getString(R.string.emom_notification_finished_title),
                context.getString(
                    R.string.emom_notification_finished_text,
                    ExerciseTranslations.translate(exercise.exercise.nome, _languageCode.value)
                )
            )
        } else {
            timerNotificationHelper.cancelEmom()
        }
        if (completed) emomCue(TimerNotificationHelper.EmomCue.DONE)
    }

    private fun emomCue(cue: TimerNotificationHelper.EmomCue) {
        if (_state.value.hapticEnabled) timerNotificationHelper.vibrateEmomCue(cue)
    }

    private fun emomNotificationsAllowed(): Boolean =
        _state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()

    /**
     * Posts the EMOM notification for the run as it stands and, unless told otherwise, schedules the
     * alarm for the next round. [phaseOverride] covers the instant after DONE, before the logged
     * set shows up in the state.
     */
    private fun refreshEmomNotification(phaseOverride: EmomPhase? = null, scheduleAlarm: Boolean = true) {
        if (!emomNotificationsAllowed()) return
        val state = _state.value
        val run = state.emomRun ?: return
        val exercise = state.exercises.getOrNull(run.exerciseIndex) ?: return
        val elapsed = state.setTimerSeconds
        emomLastNotifiedElapsed = elapsed

        val roundsLogged = (run.startSetIndex until run.startSetIndex + run.roundCount)
            .count { exercise.sets.getOrNull(it)?.isCompleted == true }
        val snapshot = EmomClock.snapshot(elapsed, started = true, roundsLogged = roundsLogged, totalRounds = run.roundCount)
        val phase = phaseOverride ?: snapshot.phase
        val paused = !state.setTimerRunning

        val context = localeManager.localizedContext()
        val unit = state.weightUnit
        val maxLabel = context.getString(R.string.max_label)
        val offset = EmomClock.focusOffset(snapshot.copy(phase = phase))
        val focus = exercise.sets.getOrNull(run.startSetIndex + offset)?.takeIf { offset < run.roundCount }
        val following = exercise.sets.getOrNull(run.startSetIndex + offset + 1)?.takeIf { offset + 1 < run.roundCount }
        val name = ExerciseTranslations.translate(exercise.exercise.nome, _languageCode.value)
        val round = (offset + 1).coerceIn(1, run.roundCount)

        val title = when {
            paused -> context.getString(R.string.emom_notification_title_paused, round, run.roundCount)
            phase == EmomPhase.LeadIn -> context.getString(R.string.emom_notification_title_get_ready)
            phase == EmomPhase.Rest -> context.getString(R.string.emom_notification_title_rest, round, run.roundCount)
            else -> context.getString(R.string.emom_notification_title_work, round, run.roundCount)
        }
        val text = when {
            focus == null -> name
            phase == EmomPhase.Rest ->
                context.getString(R.string.emom_notification_next, name, focus.setNumber, focus.loadText(unit, maxLabel))
            else ->
                context.getString(R.string.emom_notification_set, name, focus.setNumber, focus.loadText(unit, maxLabel))
        }
        val detail = following?.let { context.getString(R.string.emom_then, it.loadText(unit, maxLabel)) }

        fun button(action: TimerNotificationReceiver.EmomAction, label: Int) =
            EmomNotificationAction(action, context.getString(label))
        val actions = when {
            paused -> listOf(
                button(TimerNotificationReceiver.EmomAction.RESUME, R.string.emom_action_resume),
                button(TimerNotificationReceiver.EmomAction.STOP, R.string.emom_action_stop)
            )
            phase == EmomPhase.Work -> listOf(
                button(TimerNotificationReceiver.EmomAction.DONE, R.string.emom_action_done),
                button(TimerNotificationReceiver.EmomAction.PAUSE, R.string.emom_action_pause)
            )
            else -> listOf(button(TimerNotificationReceiver.EmomAction.PAUSE, R.string.emom_action_pause))
        }

        val startedAt = state.setTimerStartedAt
        val countdownEndsAt = if (paused || startedAt == null) null else {
            startedAt + (EmomClock.nextRoundAt(elapsed) - state.setTimerBaseSeconds) * 1000L
        }
        timerNotificationHelper.showEmomNotification(
            EmomNotificationInfo(
                title = title,
                text = text,
                detail = detail,
                countdownEndsAt = countdownEndsAt,
                runElapsedSeconds = elapsed.coerceAtLeast(0),
                rounds = run.roundCount,
                actions = actions
            )
        )
        if (scheduleAlarm && countdownEndsAt != null) timerNotificationHelper.scheduleEmomBoundaryAlarm(countdownEndsAt)
    }

    override fun onCleared() {
        // A run left behind no longer ticks, so its notification and round alarm must not outlive the screen.
        timerNotificationHelper.cancelEmom()
        super.onCleared()
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
                    ).withSnapshot(setState)
                )
            }
        }
        updateSetState(exerciseIndex, setIndex) { it.copy(timeSeconds = seconds) }
    }
}
