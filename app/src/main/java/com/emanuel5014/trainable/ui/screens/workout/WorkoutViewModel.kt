package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.data.local.entity.straightReps
import com.emanuel5014.trainable.data.local.entity.encodePerformed
import com.emanuel5014.trainable.data.local.entity.parts
import com.emanuel5014.trainable.data.local.entity.pauses
import com.emanuel5014.trainable.data.local.entity.performed
import com.emanuel5014.trainable.domain.prescription.SetPerformance
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.kind
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
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
import com.emanuel5014.trainable.data.local.entity.repsRange
import com.emanuel5014.trainable.data.local.entity.toPrescriptionBlocks
import com.emanuel5014.trainable.data.local.entity.withRepsRange
import com.emanuel5014.trainable.domain.emom.EmomClock
import com.emanuel5014.trainable.domain.emom.EmomPhase
import com.emanuel5014.trainable.domain.prescription.DoubleProgression
import com.emanuel5014.trainable.domain.prescription.LegacyReps
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.prescription.PlannedSet
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.RepMode
import com.emanuel5014.trainable.domain.prescription.RepRange
import com.emanuel5014.trainable.domain.prescription.RepsTarget
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
import com.emanuel5014.trainable.util.PlateCalculator
import com.emanuel5014.trainable.util.TimerAdjustment
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    /** Seconds the + and - buttons of the rest and warmup timers move the countdown by. */
    val timerAddSeconds: Int = TimerAdjustment.DEFAULT_ADD_SECONDS,
    val timerSubtractSeconds: Int = TimerAdjustment.DEFAULT_SUBTRACT_SECONDS,
    /** Which buttons the rest and warmup timers show: the - / + pair and the skip button. */
    val timerShowTimeButtons: Boolean = true,
    val timerShowSkipButton: Boolean = true,
    /** Each time button on its own; a button shows only while both this and [timerShowTimeButtons] are on. */
    val timerAddEnabled: Boolean = true,
    val timerSubtractEnabled: Boolean = true,
    val plateCalculatorEnabled: Boolean = false,
    /** Mini-sets, assisted and partial reps and the technique as done; off, a set carries only its note. */
    val setPerformanceEnabled: Boolean = false,
    /** Plates the gym has, in [weightUnit], heaviest first. */
    val availablePlates: List<Float> = PlateCalculator.DEFAULT_PLATES_KG,
    val warmupTimerEnabled: Boolean = false,
    val warmupTimerRemaining: Int = 0,
    val warmupTimerEndTime: Long? = null,
    val warmupTimerTotalSeconds: Int = 0,
    /** Next real execution order to hand out, see [WorkoutExerciseState.executionOrder]. */
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
    val oneRepMaxSuggestions: List<OneRepMaxSuggestion> = emptyList(),
    /** The set just logged came out short of its reps: the rest card offers to finish it after a short pause. */
    val miniSetOffer: MiniSetOffer? = null,
    /** A mini-set on its way: its pause runs on the rest timer, then the hub asks how many reps it had. */
    val miniSet: MiniSetRun? = null
) {
    val currentExercise: WorkoutExerciseState?
        get() = exercises.getOrNull(currentExerciseIndex)

    val completedExercises: Int
        get() = exercises.count { ex ->
            if (ex.isCardio) ex.isCardioCompleted else ex.sets.all { it.isCompleted }
        }

    val totalExercises: Int
        get() = exercises.size

    /** The time buttons the rest and warmup timers actually show. */
    val showTimerAdd: Boolean
        get() = timerShowTimeButtons && timerAddEnabled

    val showTimerSubtract: Boolean
        get() = timerShowTimeButtons && timerSubtractEnabled
}

data class MiniSetOffer(val exerciseIndex: Int, val setIndex: Int, val missing: Int)

data class MiniSetRun(
    val exerciseIndex: Int,
    val setIndex: Int,
    /** Reps still missing to the set's goal when the pause started (0 when it has none). */
    val missing: Int,
    val pauseStartedAt: Long,
    /** How long the pause really lasted, known once it is over: the hub then asks for the reps. */
    val pauseSeconds: Int? = null
) {
    val awaitingReps: Boolean get() = pauseSeconds != null
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

/**
 * Execution orders from this value up are placeholders for exercises that have rows in the session but nothing
 * done yet (edited weight, swapped, just added): the real orders (0, 1, 2...) are only handed out when a set is
 * completed, so a placeholder can never be mistaken for the order of another exercise.
 */
internal const val PROVISIONAL_ORDER_BASE = 10_000

private const val MINI_SESSION = "session_id"
private const val MINI_SET = "set_id"
private const val MINI_MISSING = "missing"
private const val MINI_STARTED = "started_at"
private const val MINI_ENDS = "ends_at"
private const val MINI_PAUSE = "pause_seconds"

data class WorkoutExerciseState(
    val exercise: ExerciseEntity,
    val planDetails: PlanExerciseEntity?,
    /**
     * Key of this exercise in the rows of the session (set logs and cardio log `ordineEsercizio`): together with the
     * exercise id it identifies the exercise on screen, so the same exercise can appear twice in a session. It is the
     * order the exercise was started in, or a placeholder ([PROVISIONAL_ORDER_BASE] and up) until then.
     */
    val executionOrder: Int,
    val sets: List<WorkoutSetState> = emptyList(),
    val previousPerformance: String? = null,
    val swappedExerciseId: Int? = null,
    val customRestSeconds: Int? = null,
    val customRepsTarget: String? = null,
    /** Top of the rep range chosen when this exercise was swapped in or added during the workout. */
    val customRepsMax: Int? = null,
    val supersetId: String? = null,
    val isCardio: Boolean = false,
    val cardioCategoria: String? = null,
    val cardioDurataTargetSeconds: Int? = null,
    val cardioDistanzaTargetKm: Float? = null,
    /** The sets of a cardio exercise (at least one); the first one not completed is the one on the timer. */
    val cardioSets: List<CardioSetState> = emptyList(),
    val exerciseType: String = "strength",
    val timeTargetSeconds: Int? = null,
    /** Advanced prescription blocks for this session's week (empty for plain exercises). */
    val blocks: List<PrescriptionBlock> = emptyList(),
    val oneRepMaxKg: Float? = null,
    /** Every set of the last workout reached the top of the rep range: the load can go up (see [DoubleProgression]). */
    val progression: DoubleProgression.Suggestion? = null,
    /** The lifter turned the suggestion down for this workout. */
    val progressionDismissed: Boolean = false
) {
    val isAdvanced: Boolean get() = blocks.isNotEmpty()

    val activeCardioSetIndex: Int get() = cardioSets.indexOfFirst { !it.isCompleted }

    /** The cardio set on the timer, null once every set is done. */
    val activeCardioSet: CardioSetState? get() = cardioSets.getOrNull(activeCardioSetIndex)

    val cardioLogId: Int? get() = activeCardioSet?.id
    val cardioElapsedSeconds: Int get() = (activeCardioSet ?: cardioSets.lastOrNull())?.elapsedSeconds ?: 0
    val cardioDistanceKm: Float get() = (activeCardioSet ?: cardioSets.lastOrNull())?.distanceKm ?: 0f
    val isCardioCompleted: Boolean get() = cardioSets.isNotEmpty() && cardioSets.all { it.isCompleted }
    val cardioTotalSeconds: Int get() = cardioSets.filter { it.isCompleted }.sumOf { it.elapsedSeconds }
    val cardioTotalKm: Float get() = cardioSets.filter { it.isCompleted }.sumOf { it.distanceKm.toDouble() }.toFloat()

    /** Applies [update] to the cardio set on the timer. */
    fun withActiveCardioSet(update: (CardioSetState) -> CardioSetState): WorkoutExerciseState {
        val index = activeCardioSetIndex.takeIf { it >= 0 } ?: return this
        return copy(cardioSets = cardioSets.toMutableList().also { it[index] = update(it[index]) })
    }

    /**
     * The reps of this exercise as it is now, in the form the editors take. A plan exercise has the plan's; one that was
     * swapped in or added during the workout has no plan, so its range comes from its sets and its reps from what it
     * was started with (or, once the workout was left and re-entered, from its rows).
     */
    val currentReps: RepsTarget
        get() {
            val plan = planDetails?.takeIf { swappedExerciseId == null }
            if (plan != null) return RepsTarget(plan.repsTarget, plan.repsMax.takeIf { plan.repsRange != null })
            sets.firstNotNullOfOrNull { it.repsRange }?.let { return RepsTarget(it.min.toString(), it.max) }
            customRepsTarget?.takeIf { it.isNotBlank() }?.let { return RepsTarget(it, null) }
            val reps = sets.filter { !it.isWarmup && !it.isExtra }.map { it.reps }
            val text = when {
                reps.isEmpty() -> "8"
                reps.distinct().size == 1 -> reps.first().toString()
                else -> reps.joinToString("-")
            }
            return RepsTarget(text, null)
        }

    /** [currentReps] as shown to the user: "10–15" for a range, otherwise as typed ("8", "8-6-4"). */
    val repsDisplay: String
        get() = currentReps.let { RepRange.of(it.repsTarget.trim().toIntOrNull(), it.repsMax)?.text ?: it.repsTarget }

    /**
     * The weight suggestion to show: only before the exercise is started, and only while some set still sits below
     * the suggested weight (applying it, or typing a heavier weight by hand, makes it go away).
     */
    val progressionToShow: DoubleProgression.Suggestion?
        get() {
            val suggestion = progression?.takeIf { !progressionDismissed } ?: return null
            if (sets.any { it.isCompleted }) return null
            val working = sets.filter { !it.isWarmup }
            val stillBelow = working.withIndex().any { (i, set) -> (suggestion.weightsKg[i + 1] ?: return@any false) > set.weight + 0.001f }
            return suggestion.takeIf { stillBelow }
        }

    /** True once the exercise has its real execution order (a set of it was completed). */
    val hasExecutionRank: Boolean get() = executionOrder < PROVISIONAL_ORDER_BASE

    /** Index of the next set to do when it is an EMOM set, i.e. where an EMOM run would start. */
    val emomStartIndex: Int?
        get() = sets.indexOfFirst { !it.isCompleted }.takeIf { it >= 0 && sets[it].isEmom }

    /** Timed intervals measured by the reps done in them, with no weight. */
    val isTimeAndReps: Boolean
        get() = exerciseType == TIME_AND_REPS ||
                (planDetails?.exerciseType == TIME_AND_REPS && swappedExerciseId == null) ||
                sets.any { it.isTimeAndReps }

    val isTimeAndWeight: Boolean
        get() = !isTimeAndReps && (
            exerciseType == "time_and_weight" ||
                (planDetails?.exerciseType == "time_and_weight" && swappedExerciseId == null) ||
                sets.any { it.timeSeconds != null }
            )

    /** Sets that run on the set timer: Time & Weight holds and Time & Reps intervals. */
    val isTimed: Boolean get() = isTimeAndWeight || isTimeAndReps

    val setKind: SetLogKind
        get() = when {
            isTimeAndReps -> SetLogKind.TIME_AND_REPS
            isTimeAndWeight -> SetLogKind.TIME_AND_WEIGHT
            else -> SetLogKind.STRENGTH
        }

    /** The reps goal of a Time & Reps exercise: the plan's, else what it was started with. */
    val repsGoal: Int?
        get() = if (!isTimeAndReps) null else TimeAndReps.goalOf(currentReps.repsTarget)
}

/** One set of a cardio exercise during the workout; [id] is its cardio log once it has one. */
data class CardioSetState(
    val id: Int? = null,
    val setNumber: Int,
    val elapsedSeconds: Int = 0,
    val distanceKm: Float = 0f,
    val isCompleted: Boolean = false,
    val note: String? = null
)

/** [count] cardio sets still to do, numbered from 1. */
internal fun pendingCardioSets(count: Int): List<CardioSetState> =
    (1..count.coerceAtLeast(1)).map { CardioSetState(setNumber = it) }

/**
 * The sets of a cardio exercise from its logs. Once the exercise has a log every set of it has one, so the logs are all
 * there is; an exercise with none yet has the [plannedSets] of the plan.
 */
internal fun cardioSetsFrom(logs: List<CardioLogEntity>, plannedSets: Int): List<CardioSetState> {
    val logged = logs.sortedWith(compareBy({ it.numeroSerie }, { it.id })).mapIndexed { i, log ->
        CardioSetState(
            id = log.id,
            setNumber = i + 1,
            elapsedSeconds = log.durataSecondi,
            distanceKm = log.distanza,
            isCompleted = log.isCompleted,
            note = log.note
        )
    }
    if (logged.isNotEmpty()) return logged
    return pendingCardioSets(plannedSets)
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
    val isExtra: Boolean = false,
    /** The rep range the exercise asks for ("10–15"); [reps] is the number the set is currently at. */
    val repsRange: RepRange? = null,
    /** A set of a Time & Reps exercise: [timeSeconds] is the interval, [reps] what was done in it, no weight. */
    val isTimeAndReps: Boolean = false,
    /** Parts of a set finished after short pauses ("2+1+1"); empty for one done in one go. [reps] is their sum. */
    val repParts: List<Int> = emptyList(),
    /** Seconds of each pause between [repParts]. */
    val pauses: List<Int> = emptyList(),
    /** Reps a spotter helped with, on top of [reps]. */
    val assistedReps: Int = 0,
    /** Partial reps after the full ones, on top of [reps]. */
    val partialReps: Int = 0,
    /** Techniques the set was really done with when they differ from the prescription's; null as prescribed. */
    val performedTechniques: List<Technique>? = null
) {
    /** Reps done in one go: what an estimated 1RM or a progression can trust. */
    val straightReps: Int get() = repParts.firstOrNull() ?: reps

    /** New total reps; a set in parts takes the difference on its last part (see [SetPerformance.adjustParts]). */
    fun withTotalReps(total: Int): WorkoutSetState {
        if (repParts.isEmpty()) return copy(reps = total)
        val parts = SetPerformance.adjustParts(repParts, total)
        return copy(reps = total, repParts = parts, pauses = pauses.take((parts.size - 1).coerceAtLeast(0)))
    }

    /** The set as it came back from its row: how it really went. */
    fun withPerformanceOf(row: SetLogEntity): WorkoutSetState = copy(
        repParts = row.parts,
        pauses = row.pauses,
        assistedReps = row.assistedReps,
        partialReps = row.partialReps,
        performedTechniques = row.performed
    )

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
    val detail: String? = null,
    /** The whole value of a timed set ("45s · 20 reps"), which has no weight × reps to show. */
    val text: String? = null
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

    /** Serialises the creation of set rows so a set is never inserted twice (draft rows vs. completing it). */
    private val setRowsLock = Mutex()

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

    /** Exercise id -> bar weight in kg (null = the default bar), for the exercises that use the plate calculator. */
    val plateCalculatorExercises: StateFlow<Map<Int, Float?>> = _availableExercises
        .map { exercises -> exercises.filter { it.plateCalculator }.associate { it.id to it.plateBarKg } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun setPlateCalculator(exerciseId: Int, enabled: Boolean, barKg: Float?) {
        viewModelScope.launch { exerciseRepository.setPlateCalculator(exerciseId, enabled, barKg) }
    }

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
                    TimerNotificationReceiver.TimerAction.ADD -> adjustRestTime(_state.value.timerAddSeconds)
                    TimerNotificationReceiver.TimerAction.SUBTRACT -> adjustRestTime(-_state.value.timerSubtractSeconds)
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
                    TimerNotificationReceiver.WarmupTimerAction.ADD -> adjustWarmupTime(_state.value.timerAddSeconds)
                    TimerNotificationReceiver.WarmupTimerAction.SUBTRACT -> adjustWarmupTime(-_state.value.timerSubtractSeconds)
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
            userPreferencesRepository.timerAddSeconds.collect { seconds ->
                _state.update { it.copy(timerAddSeconds = seconds) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.timerSubtractSeconds.collect { seconds ->
                _state.update { it.copy(timerSubtractSeconds = seconds) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.timerAddEnabled.collect { enabled ->
                _state.update { it.copy(timerAddEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.timerSubtractEnabled.collect { enabled ->
                _state.update { it.copy(timerSubtractEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.timerShowTimeButtons.collect { show ->
                _state.update { it.copy(timerShowTimeButtons = show) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.timerShowSkipButton.collect { show ->
                _state.update { it.copy(timerShowSkipButton = show) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.plateCalculatorEnabled.collect { enabled ->
                _state.update { it.copy(plateCalculatorEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.setPerformanceEnabled.collect { enabled ->
                _state.update { it.copy(setPerformanceEnabled = enabled, miniSetOffer = if (enabled) it.miniSetOffer else null) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.plateCalculatorPlates.collect { plates ->
                _state.update { it.copy(availablePlates = plates) }
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
        // Rows saved before exercises had an order all carry 0 for several exercises: those are matched by exercise id.
        // A single exercise at order 0 (the first one started) is a perfectly good order.
        val instanceCount = sessionWithSets.sets.map { it.ordineEsercizio to it.exerciseId }.distinct().size +
            cardioLogs.map { it.ordineEsercizio to it.categoria.trim().lowercase() }.distinct().size
        val useOrdine = sessionWithSets.sets.any { it.ordineEsercizio > 0 } || cardioLogs.any { it.ordineEsercizio > 0 } || instanceCount <= 1

        suspend fun createExerciseState(exercise: ExerciseEntity, planDetail: PlanExerciseEntity?, exerciseIndex: Int, useOrdine: Boolean, executionOrder: Int): WorkoutExerciseState {
            val isSwapped = (planDetail != null && swapMap[planDetail.id] != null) || reverseSwapMap[exercise.id] != null
            // A plan cardio exercise swapped for a strength one is no longer cardio
            val isCardio = exercise.categoria.equals("Cardio", ignoreCase = true) || (planDetail?.exerciseType == "cardio" && !isSwapped)
            val exerciseCardioLogs = if (isCardio) {
                cardioLogs.filter {
                    if (useOrdine) it.ordineEsercizio == exerciseIndex && it.categoria.equals(exercise.nome, ignoreCase = true)
                    else it.categoria.equals(exercise.nome, ignoreCase = true)
                }
            } else emptyList()
            val cardioLog = exerciseCardioLogs.firstOrNull()

            // Load already completed or uncompleted sets for this session
            val loggedSets = if (useOrdine) {
                sessionWithSets.sets.filter { it.ordineEsercizio == exerciseIndex && it.exerciseId == exercise.id }
            } else {
                sessionWithSets.sets.filter { it.exerciseId == exercise.id }
            }

            val loggedTargetSeconds = loggedSets.firstOrNull { it.durataSecondi != null }?.durataSecondi

            // The rows say how the exercise was done; without any, the plan does (a swapped-in one is plain)
            val setKind = when {
                isCardio -> SetLogKind.STRENGTH
                loggedSets.isNotEmpty() && loggedSets.kind != SetLogKind.STRENGTH -> loggedSets.kind
                isSwapped -> SetLogKind.STRENGTH
                planDetail != null && planDetail.exerciseType != "cardio" -> SetLogKind.ofExerciseType(planDetail.exerciseType)
                else -> SetLogKind.STRENGTH
            }
            val isTimeAndWeight = setKind == SetLogKind.TIME_AND_WEIGHT
            val isTimeAndReps = setKind == SetLogKind.TIME_AND_REPS
            val isTimed = setKind.isTimed
            val planTargetSeconds = if (!isSwapped) planDetail?.durataTargetSecondi else null

            val resolvedExerciseType = when {
                isCardio -> "cardio"
                isTimeAndWeight -> "time_and_weight"
                isTimeAndReps -> TIME_AND_REPS
                else -> "strength"
            }

            val resolvedTargetSeconds = when {
                isTimeAndWeight -> loggedTargetSeconds ?: planTargetSeconds ?: 45
                // The interval is the routine's: the reps are what changes from one workout to the next
                isTimeAndReps -> planTargetSeconds ?: loggedTargetSeconds ?: TimeAndReps.DEFAULT_SECONDS
                else -> null
            }

            val defaultTargetSeconds = resolvedTargetSeconds ?: 45
            val resumeOneRepMaxKg = resumeOneRepMaxes[exercise.id]?.weightKg
            val planDetailWithBlocks = planDetail?.let { pd -> planExercises.find { it.planExercise.id == pd.id } }
            val resolvedBlocks = if (resumeAdvanced) (planDetailWithBlocks?.resolve(resumeWeek) as? ResolvedPrescription.Blocks)?.blocks else null
            if (resolvedBlocks != null && !isCardio && !isTimed && !isSwapped) {
                val previous = getPreviousSetsForExercise(planId, exercise.id, ALL_SETS)
                return WorkoutExerciseState(
                    exercise = exercise,
                    planDetails = planDetail,
                    executionOrder = executionOrder,
                    sets = buildAdvancedSets(resolvedBlocks, loggedSets, previous, resumeOneRepMaxKg, resumeUnit, resumeIncrement),
                    previousPerformance = previous.maxByOrNull { it.pesoSollevato }?.let { "Last: ${it.pesoSollevato}kg × ${it.repsEffettive}" },
                    supersetId = planDetail?.supersetId,
                    exerciseType = "strength",
                    blocks = resolvedBlocks,
                    oneRepMaxKg = resumeOneRepMaxKg
                )
            }
            if (resumeAdvanced && !isCardio && !isTimed && loggedSets.any { it.blockIndex != null }) {
                // Quick workouts and swapped exercises have no plan blocks: rebuild them from the snapshots on the logged rows
                val rebuilt = loggedSets.toPrescriptionBlocks()
                if (rebuilt.isNotEmpty()) {
                    val previous = getPreviousSetsForExercise(planId, exercise.id, ALL_SETS)
                    return WorkoutExerciseState(
                        exercise = exercise,
                        planDetails = planDetail,
                        executionOrder = executionOrder,
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
            val prevPerfStr = previousPerformanceText(previousSets, setKind)
            
            // A swapped-in or added exercise has as many sets as it was given, not as many as the plan exercise it replaced
            val targetSets = if ((planDetail == null || isSwapped) && loggedSets.isNotEmpty()) {
                loggedSets.size
            } else {
                planDetail?.serieTarget ?: loggedSets.size.coerceAtLeast(3)
            }
            val repsList = if (previousSets.isEmpty()) {
                parseReps(planDetail?.repsTarget ?: "8", targetSets)
            } else {
                previousSets.map { it.repsEffettive }
            }
            val defaultPrevWeight = if (isTimeAndReps) 0f else previousSets.firstOrNull()?.pesoSollevato ?: previousSets.lastOrNull()?.pesoSollevato
                ?: if (planDetail != null && !isSwapped && !isCardio) workoutRepository.startingWeightFor(planDetail, exercise.id) else 0f
            // The range the exercise was started with is on its rows: that is all a swapped-in or added exercise has to
            // go on. Rows saved without one (or before ranges existed) fall back to the plan's range.
            val rowsRange = if (isCardio || isTimed) null else loggedSets.firstNotNullOfOrNull { it.repsRange }
            val planRange = if (planDetail?.exerciseType == "strength" && !isSwapped && !isCardio && !isTimed) planDetail.repsRange else null
            val repsRange = rowsRange ?: planRange
            val progression = if (repsRange != null) {
                progressionFor(targetSets, repsRange, getPreviousSetsForExercise(planId, exercise.id, ALL_SETS), resumeUnit, resumeIncrement)
            } else null

            // While a workout runs, a weight typed on one set is copied to the following unlogged sets
            // (quick / custom exercises, or ones with no history). Those copies only live in memory, so
            // on resume the same rule is re-applied from the last logged set instead of falling back to 0.
            val carriesWeight = planDetail == null || isSwapped || prevPerfStr == null
            var carriedWeight: Float? = null
            val sets = if (isCardio) emptyList() else (1..targetSets.coerceAtLeast(loggedSets.size)).map { num ->
                val loggedSet = loggedSets.find { it.numeroSerie == num }
                val prevSet = previousSets.getOrNull(num - 1)
                val setDuration = when {
                    isTimeAndWeight -> loggedSet?.durataSecondi ?: prevSet?.durataSecondi ?: defaultTargetSeconds
                    isTimeAndReps -> loggedSet?.durataSecondi ?: defaultTargetSeconds
                    else -> loggedSet?.durataSecondi ?: prevSet?.durataSecondi
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
                        isExtra = loggedSet.isExtra,
                        repsRange = repsRange,
                        isTimeAndReps = isTimeAndReps
                    ).withPerformanceOf(loggedSet)
                } else {
                    WorkoutSetState(
                        setNumber = num,
                        weight = if (isTimeAndReps) 0f else carriedWeight.takeIf { carriesWeight } ?: prevSet?.pesoSollevato ?: defaultPrevWeight,
                        reps = prevSet?.repsEffettive ?: repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 },
                        previousNote = prevSet?.note,
                        previousReps = prevSet?.repsEffettive,
                        previousWeight = prevSet?.pesoSollevato,
                        timeSeconds = setDuration,
                        previousTimeSeconds = prevSet?.durataSecondi,
                        repsRange = repsRange,
                        isTimeAndReps = isTimeAndReps
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
                executionOrder = executionOrder,
                sets = sets,
                previousPerformance = prevPerfStr,
                progression = progression,
                swappedExerciseId = planDetail?.id?.let { swapMap[it] } ?: if (isSwapped) exercise.id else null,
                supersetId = planDetail?.supersetId ?: loggedSets.firstOrNull()?.supersetId,
                customRestSeconds = restoredRestSeconds,
                isCardio = isCardio,
                cardioCategoria = exercise.nome,
                cardioDurataTargetSeconds = cardioLog?.durataTargetSecondi ?: planDetail?.durataTargetSecondi,
                cardioDistanzaTargetKm = planDetail?.distanzaTargetKm,
                // A swapped-in or added cardio exercise has a log for every set it was given
                cardioSets = if (isCardio) cardioSetsFrom(exerciseCardioLogs, if (isSwapped || planDetail == null) 0 else planDetail.serieTarget) else emptyList(),
                exerciseType = resolvedExerciseType,
                timeTargetSeconds = resolvedTargetSeconds,
                oneRepMaxKg = resumeOneRepMaxKg
            )
        }

        val exerciseStates: List<WorkoutExerciseState>
        val activeIndex: Int
        val nextRank: Int

        if (useOrdine) {
            val planDetailByOrigIndex = planExercises.mapIndexed { index, detail ->
                index to detail
            }.toMap()

            // One entry per exercise on screen: its rows share the order and the exercise id, so two exercises that
            // happen to carry the same order (or the same exercise added twice) stay apart.
            val resumed = mutableListOf<Pair<Int, WorkoutExerciseState>>()
            val consumedPlanDetailIds = mutableSetOf<Int>()

            val setGroups = sessionWithSets.sets
                .groupBy { it.ordineEsercizio to it.exerciseId }
                .entries
                .sortedWith(compareBy({ it.key.first }, { entry -> entry.value.minOf { it.id } }))
            for (group in setGroups) {
                val (order, exerciseId) = group.key
                val exercise = allAvailableExercises.find { it.id == exerciseId } ?: continue
                // The plan's own exercise is the first instance of it; a second one was added during the session
                var planDetail = planExercises.find { it.exercise.id == exerciseId && it.planExercise.id !in consumedPlanDetailIds }
                if (planDetail == null) {
                    val originalPlanExerciseId = reverseSwapMap[exerciseId]
                    if (originalPlanExerciseId != null) {
                        planDetail = planExercises.find { it.planExercise.id == originalPlanExerciseId }
                    }
                }
                resumed += order to createExerciseState(exercise, planDetail?.planExercise, order, true, order)
                if (planDetail != null) consumedPlanDetailIds.add(planDetail.planExercise.id)
                // If this exercise was swapped in, also consume the original plan exercise
                reverseSwapMap[exerciseId]?.let { consumedPlanDetailIds.add(it) }
            }

            val cardioGroups = cardioLogs
                .groupBy { it.ordineEsercizio to it.categoria.trim().lowercase() }
                .entries
                .sortedWith(compareBy({ it.key.first }, { entry -> entry.value.minOf { it.id } }))
            for (group in cardioGroups) {
                val order = group.key.first
                val exercise = allAvailableExercises.find { it.nome.equals(group.value.first().categoria, ignoreCase = true) } ?: continue
                var planDetail = planExercises.find { it.exercise.id == exercise.id && it.planExercise.id !in consumedPlanDetailIds }
                // If the cardio exercise was swapped in from a plan exercise, use the original plan detail
                if (planDetail == null) {
                    val originalPlanExerciseId = reverseSwapMap[exercise.id]
                    if (originalPlanExerciseId != null) {
                        planDetail = planExercises.find { it.planExercise.id == originalPlanExerciseId }
                        consumedPlanDetailIds.add(originalPlanExerciseId)
                    }
                }
                resumed += order to createExerciseState(exercise, planDetail?.planExercise, order, true, order)
                if (planDetail != null) {
                    consumedPlanDetailIds.add(planDetail.planExercise.id)
                }
            }

            // Plan exercises with nothing logged yet get a placeholder order of their own
            var nextPlaceholder = (resumed.map { it.first }.filter { it >= PROVISIONAL_ORDER_BASE }.maxOrNull() ?: (PROVISIONAL_ORDER_BASE - 1)) + 1
            for ((_, detail) in planDetailByOrigIndex) {
                if (detail.planExercise.id in consumedPlanDetailIds) continue
                val swappedId = swapMap[detail.planExercise.id]
                val exercise = if (swappedId != null) {
                    allAvailableExercises.find { it.id == swappedId } ?: detail.exercise
                } else {
                    detail.exercise
                }
                val order = nextPlaceholder++
                resumed += order to createExerciseState(exercise, detail.planExercise, order, true, order)
            }

            // What was started comes first, in the order it was done; the rest follows the plan
            fun planPosition(exState: WorkoutExerciseState): Int =
                planExercises.indexOfFirst { it.planExercise.id == exState.planDetails?.id }.let { if (it < 0) Int.MAX_VALUE else it }
            val started = resumed.filter { it.first < PROVISIONAL_ORDER_BASE }.sortedBy { it.first }
            val pending = resumed.filter { it.first >= PROVISIONAL_ORDER_BASE }
                .sortedWith(compareBy({ planPosition(it.second) }, { it.first }))
            exerciseStates = (started + pending).map { it.second }
            nextRank = (started.maxOfOrNull { it.first } ?: -1) + 1
            activeIndex = exerciseStates.indexOfFirst { exState ->
                if (exState.isCardio) !exState.isCardioCompleted
                else exState.sets.any { !it.isCompleted }
            }.coerceAtLeast(0)
        } else {
            // Rows without a usable order (sessions saved before it existed): exercises are matched by id
            fun orderOnRows(exercise: ExerciseEntity): Int? =
                sessionWithSets.sets.firstOrNull { it.exerciseId == exercise.id }?.ordineEsercizio
                    ?: cardioLogs.firstOrNull { it.categoria.equals(exercise.nome, ignoreCase = true) }?.ordineEsercizio

            val planExerciseStates = planExercises.mapIndexed { index, detail ->
                val swappedId = swapMap[detail.planExercise.id]
                val exercise = if (swappedId != null) {
                    allAvailableExercises.find { it.id == swappedId } ?: detail.exercise
                } else {
                    detail.exercise
                }
                createExerciseState(exercise, detail.planExercise, index, false, orderOnRows(exercise) ?: (PROVISIONAL_ORDER_BASE + index))
            }

            val consumedExerciseIds = planExerciseStates.map { it.exercise.id }.toSet()
            val loggedExerciseIds = sessionWithSets.sets.map { it.exerciseId }.distinct()
            val cardioExerciseIds = cardioLogs.mapNotNull { log ->
                allAvailableExercises.find { it.nome.equals(log.categoria, ignoreCase = true) }?.id
            }.distinct()
            val extraExerciseIds = (loggedExerciseIds + cardioExerciseIds).filter { it !in consumedExerciseIds }

            val extraExerciseStates = extraExerciseIds.mapIndexed { idx, exerciseId ->
                val exercise = allAvailableExercises.find { it.id == exerciseId } ?: return@mapIndexed null
                createExerciseState(exercise, null, planExercises.size + idx, false, orderOnRows(exercise) ?: (PROVISIONAL_ORDER_BASE + planExercises.size + idx))
            }.filterNotNull()

            exerciseStates = planExerciseStates + extraExerciseStates
            activeIndex = exerciseStates.indexOfFirst { exState ->
                if (exState.isCardio) !exState.isCardioCompleted
                else exState.sets.any { !it.isCompleted }
            }.coerceAtLeast(0)
            nextRank = (exerciseStates.map { it.executionOrder }.filter { it < PROVISIONAL_ORDER_BASE }.maxOrNull() ?: -1) + 1
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
                nextExecutionOrder = nextRank,
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
        restoreMiniSet()
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
            if (targetExState != null && targetExState.isTimed) {
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

        var nextPlaceholder = PROVISIONAL_ORDER_BASE
        val exerciseStates = planWithDetails.exercises.sortedBy { it.planExercise.ordine }.mapNotNull { detail ->
            val resolved = if (advanced) detail.resolve(week) else ResolvedPrescription.Legacy
            if (resolved is ResolvedPrescription.Excluded) return@mapNotNull null
            val placeholderOrder = nextPlaceholder++
            val oneRepMaxKg = oneRepMaxes[detail.exercise.id]?.weightKg
            if (resolved is ResolvedPrescription.Blocks && detail.planExercise.exerciseType == "strength") {
                val previous = getPreviousSetsForExercise(planId, detail.exercise.id, ALL_SETS)
                return@mapNotNull WorkoutExerciseState(
                    exercise = detail.exercise,
                    planDetails = detail.planExercise,
                    executionOrder = placeholderOrder,
                    sets = buildAdvancedSets(resolved.blocks, emptyList(), previous, oneRepMaxKg, unit, increment),
                    previousPerformance = previous.maxByOrNull { it.pesoSollevato }?.let { "Last: ${it.pesoSollevato}kg × ${it.repsEffettive}" },
                    supersetId = detail.planExercise.supersetId,
                    exerciseType = "strength",
                    blocks = resolved.blocks,
                    oneRepMaxKg = oneRepMaxKg
                )
            }
            val previousSets = getPreviousSetsForExercise(planId, detail.exercise.id, detail.planExercise.serieTarget)
            val setKind = SetLogKind.ofExerciseType(detail.planExercise.exerciseType)
            val isTimeAndWeight = setKind == SetLogKind.TIME_AND_WEIGHT
            val isTimeAndReps = setKind == SetLogKind.TIME_AND_REPS
            val defaultTargetSeconds = detail.planExercise.durataTargetSecondi ?: 45
            val repsRange = detail.planExercise.repsRange?.takeIf { detail.planExercise.exerciseType == "strength" }
            val prevPerfStr = previousPerformanceText(previousSets, setKind)

            val targetSets = detail.planExercise.serieTarget
            val repsList = if (previousSets.isEmpty()) {
                parseReps(detail.planExercise.repsTarget, targetSets)
            } else {
                previousSets.map { it.repsEffettive }
            }
            // The first workouts of a routine have nothing to copy: start from the starting weight (if turned on)
            val defaultWeight = if (isTimeAndReps) 0f else previousSets.firstOrNull()?.pesoSollevato ?: previousSets.lastOrNull()?.pesoSollevato
                ?: workoutRepository.startingWeightFor(detail.planExercise, detail.exercise.id)

            val isCardio = detail.exercise.categoria.equals("Cardio", ignoreCase = true) || detail.planExercise.exerciseType == "cardio"
            val progression = if (repsRange != null && !isCardio) {
                // Every set of the last workout, not just the planned count: warm-ups would otherwise eat into it
                val lastWorkout = getPreviousSetsForExercise(planId, detail.exercise.id, ALL_SETS)
                progressionFor(detail.planExercise.serieTarget, repsRange, lastWorkout, unit, increment)
            } else null

            val initialSets = if (isCardio) emptyList() else (1..targetSets).map { num ->
                val prevSet = previousSets.getOrNull(num - 1)
                val setDuration = when {
                    isTimeAndWeight -> prevSet?.durataSecondi ?: defaultTargetSeconds
                    // The interval is the routine's, the reps start from last time's
                    isTimeAndReps -> defaultTargetSeconds
                    else -> prevSet?.durataSecondi
                }
                WorkoutSetState(
                    setNumber = num,
                    weight = if (isTimeAndReps) 0f else prevSet?.pesoSollevato ?: defaultWeight,
                    reps = prevSet?.repsEffettive ?: repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 },
                    previousNote = prevSet?.note,
                    previousReps = prevSet?.repsEffettive,
                    previousWeight = prevSet?.pesoSollevato,
                    timeSeconds = setDuration,
                    previousTimeSeconds = prevSet?.durataSecondi,
                    repsRange = repsRange,
                    isTimeAndReps = isTimeAndReps
                )
            }

            WorkoutExerciseState(
                exercise = detail.exercise,
                planDetails = detail.planExercise,
                executionOrder = placeholderOrder,
                sets = initialSets,
                previousPerformance = prevPerfStr,
                supersetId = detail.planExercise.supersetId,
                progression = progression,
                isCardio = isCardio,
                cardioCategoria = detail.exercise.nome,
                cardioDurataTargetSeconds = detail.planExercise.durataTargetSecondi,
                cardioDistanzaTargetKm = detail.planExercise.distanzaTargetKm,
                cardioSets = if (isCardio) pendingCardioSets(detail.planExercise.serieTarget) else emptyList(),
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
            // Advanced sets carry their planned reps, plain ones the rep range (if any) they were done against
            targetReps = if (p != null) p.targetReps?.toString() ?: if (p.repMode == RepMode.AMRAP) "MAX" else null else set.repsRange?.text,
            repMode = p?.repMode?.code,
            blockIndex = p?.blockIndex,
            techniques = p?.let { TechniqueCodec.encode(it.techniques) },
            targetTotalReps = p?.totalReps,
            exerciseType = if (set.isTimeAndReps) TIME_AND_REPS else null,
            pesoSollevato = if (set.isTimeAndReps) 0f else pesoSollevato,
            repParts = SetPerformance.encodeParts(set.repParts),
            intraRests = SetPerformance.encodePauses(set.pauses).takeIf { set.repParts.size > 1 },
            assistedReps = set.assistedReps,
            partialReps = set.partialReps,
            performedTechniques = encodePerformed(set.performedTechniques)
        )
    }

    /** "Last: …" of the best set of the last workout; only its presence is used (it decides whether weights carry over). */
    private fun previousPerformanceText(previousSets: List<SetLogEntity>, kind: SetLogKind): String? {
        if (kind == SetLogKind.TIME_AND_REPS) {
            val best = previousSets.maxByOrNull { it.repsEffettive } ?: return null
            return "Last: ${best.durataSecondi ?: 0}s × ${best.repsEffettive}"
        }
        val bestSet = previousSets.maxByOrNull { it.pesoSollevato } ?: return null
        return if (kind == SetLogKind.TIME_AND_WEIGHT || bestSet.durataSecondi != null) {
            "Last: ${bestSet.pesoSollevato}kg × ${bestSet.durataSecondi ?: bestSet.repsEffettive}s"
        } else {
            "Last: ${bestSet.pesoSollevato}kg × ${bestSet.repsEffettive}"
        }
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
        val previousPlain = previous.filter { !it.isExtra }.sortedBy { it.numeroSerie }
        val previousByBlock = previousPlain.filter { it.blockIndex != null }.groupBy { it.blockIndex!! }
        // A session logged before the exercise had blocks has no block on its rows: match those by position instead
        val previousWithoutBlock = previousPlain.filter { it.blockIndex == null }
        val loggedSorted = logged.sortedBy { it.numeroSerie }
        val loggedByBlock = loggedSorted.filter { it.blockIndex != null && !it.isExtra }.groupBy { it.blockIndex!! }
        val result = mutableListOf<WorkoutSetState>()

        fun plannedState(p: PlannedSet, lastWeightInBlock: Float?): WorkoutSetState {
            val prev = previousByBlock[p.blockIndex]?.getOrNull(p.indexInBlock)
                ?: previousWithoutBlock.getOrNull(planned.count { it.blockIndex < p.blockIndex } + p.indexInBlock)
            val target = LoadCalculator.targetWeightKg(p, oneRepMaxKg, unit, increment)
            val reps = when (p.repMode) {
                RepMode.FIXED -> p.targetReps ?: prev?.repsEffettive ?: 5
                RepMode.AMRAP -> prev?.repsEffettive ?: 5
                RepMode.TOTAL -> (prev?.repsEffettive ?: p.targetReps ?: 5).coerceAtMost(p.targetReps ?: Int.MAX_VALUE)
            }
            return WorkoutSetState(
                setNumber = 0,
                // The load of the same set last time wins over the one just before it, so ramps keep their steps
                weight = target ?: prev?.pesoSollevato ?: lastWeightInBlock ?: 0f,
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
                ).withPerformanceOf(row)
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
            ).withPerformanceOf(row)
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
        // The technique the set is being done with, when it was changed from the prescribed one
        val parts = listOfNotNull(PrescriptionFormatter.intensity(p.intensityType, p.intensityValue, labels)) +
            (set.performedTechniques ?: p.techniques).map { techniqueLabel(context, it) }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private fun repsLabelFor(set: WorkoutSetState?): String? = when {
        set?.isAmrap == true -> localeManager.getString(R.string.max_label)
        set?.repsRange != null -> set.repsRange.text
        else -> null
    }

    /**
     * Double progression for a plain exercise with a rep range: the working sets of the last workout of this
     * routine, numbered 1..n without the warm-ups so they line up with the sets of the new workout.
     */
    private fun progressionFor(
        plannedSets: Int,
        range: RepRange?,
        previousSets: List<SetLogEntity>,
        unit: String,
        increment: Float
    ): DoubleProgression.Suggestion? {
        if (range == null) return null
        val done = previousSets.filter { it.isCompleted && !it.isWarmup }.sortedBy { it.numeroSerie }
            // Reps after a pause (a set finished in parts) do not show the weight can go up
            .mapIndexed { i, set -> DoubleProgression.DoneSet(i + 1, set.pesoSollevato, set.straightReps) }
        return DoubleProgression.suggest(done, range, plannedSets, unit, increment)
    }

    /**
     * Creates the rows of a plain sets × reps exercise swapped in or added during the workout, and the sets that stand
     * for them. A rep range goes on every row as its snapshot, so the exercise keeps it when the workout is left and
     * re-entered. [repsTarget] is one number for every set or a per-set list ("8-6-4"); a range starts at its bottom.
     */
    private suspend fun createPlainRows(
        sessionId: Int,
        exerciseId: Int,
        executionOrder: Int,
        supersetId: String?,
        restTimer: Int?,
        targetSets: Int,
        repsTarget: String,
        repsRange: RepRange?,
        previousSets: List<SetLogEntity>,
        kind: SetLogKind,
        defaultTargetSeconds: Int
    ): List<WorkoutSetState> {
        val repsList = parseReps(repsTarget, targetSets)
        return (1..targetSets).map { num ->
            val prevSet = previousSets.getOrNull(num - 1)
            val reps = repsList.getOrElse(num - 1) { repsList.lastOrNull() ?: 8 }
            val setDuration = when (kind) {
                SetLogKind.TIME_AND_WEIGHT -> prevSet?.durataSecondi ?: defaultTargetSeconds
                SetLogKind.TIME_AND_REPS -> defaultTargetSeconds
                SetLogKind.STRENGTH -> prevSet?.durataSecondi
            }
            val logId = workoutRepository.logSet(
                SetLogEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    pesoSollevato = 0f,
                    repsEffettive = reps,
                    numeroSerie = num,
                    isCompleted = false,
                    ordineEsercizio = executionOrder,
                    restTimerSeconds = restTimer,
                    supersetId = supersetId,
                    durataSecondi = setDuration,
                    exerciseType = kind.rowType
                ).withRepsRange(repsRange)
            )
            WorkoutSetState(
                id = logId.toInt(),
                setNumber = num,
                weight = 0f,
                reps = reps,
                isCompleted = false,
                previousNote = prevSet?.note,
                previousReps = prevSet?.repsEffettive,
                previousWeight = prevSet?.pesoSollevato,
                timeSeconds = setDuration,
                previousTimeSeconds = prevSet?.durataSecondi,
                repsRange = repsRange,
                isTimeAndReps = kind == SetLogKind.TIME_AND_REPS
            )
        }
    }

    /** The range a swapped-in or added exercise starts with: only plain strength exercises have one. */
    private fun rangeFor(exerciseType: String, repsTarget: String, repsMax: Int?): RepRange? =
        if (exerciseType == "strength") RepRange.of(repsTarget.trim().toIntOrNull(), repsMax) else null

    /** Takes the weight suggested by double progression: heavier weight on the sets still to do, reps back to the bottom of the range. */
    fun applyProgression(exerciseIndex: Int) {
        val exState = _state.value.exercises.getOrNull(exerciseIndex) ?: return
        val suggestion = exState.progression ?: return
        var workingIndex = 0
        exState.sets.forEachIndexed { setIndex, set ->
            if (set.isWarmup) return@forEachIndexed
            workingIndex++
            val newWeight = suggestion.weightsKg[workingIndex] ?: return@forEachIndexed
            if (set.isCompleted) return@forEachIndexed
            updateSetWeight(exerciseIndex, setIndex, newWeight)
            updateSetReps(exerciseIndex, setIndex, suggestion.restartReps)
        }
    }

    /** Keeps the weight for this workout: the suggestion does not come back until the next one. */
    fun dismissProgression(exerciseIndex: Int) {
        _state.update { curr ->
            val exercises = curr.exercises.toMutableList()
            val exState = exercises.getOrNull(exerciseIndex) ?: return@update curr
            exercises[exerciseIndex] = exState.copy(progressionDismissed = true)
            curr.copy(exercises = exercises)
        }
    }

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
            val executionOrder = exState.executionOrder
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
        val executionOrder = exState.executionOrder
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
            if (ex.isCardio || ex.isTimed) return@mapNotNull null
            val best = ex.sets
                .filter { it.isCompleted && !it.isWarmup && it.weight > 0f && it.straightReps in 1..5 }
                .maxByOrNull { LoadCalculator.epley(it.weight, it.straightReps) } ?: return@mapNotNull null
            val estimate = LoadCalculator.epley(best.weight, best.straightReps)
            val beats = if (best.straightReps == 1) best.weight > current else estimate > current * 1.025f
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
                val executionOrder = exState.executionOrder
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
            // A weight typed on a set is copied to the following sets that have no history to show. Advanced
            // exercises decide that per set and stay inside the block: a back-off block must not inherit the top sets.
            fun followsEdit(next: WorkoutSetState): Boolean = when {
                next.isCompleted -> false
                exState.isAdvanced ->
                    next.prescription?.blockIndex == set.prescription?.blockIndex && (isQuickOrCustom || next.previousWeight == null)
                else -> isQuickOrCustom || exState.previousPerformance == null
            }

            for (i in (setIndex + 1) until mutableSets.size) {
                if (followsEdit(mutableSets[i])) {
                    val propSet = mutableSets[i].copy(weight = weight)
                    mutableSets[i] = propSet
                    if (propSet.id != null && curr.sessionId != null) {
                        val executionOrder = exState.executionOrder
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
            
            mutableExercises[exerciseIndex] = exState.copy(sets = mutableSets)
            curr.copy(exercises = mutableExercises)
        }
        persistPendingSets(exerciseIndex)
    }

    /**
     * Plan sets only get a log row once they are completed, so a weight typed before that would be gone after
     * leaving and re-entering the workout. The first weight edit of an exercise therefore saves its pending sets
     * as uncompleted rows (finishing the workout drops them), which is what the resume rebuilds the sets from.
     */
    private fun persistPendingSets(exerciseIndex: Int) {
        val hasPending = _state.value.exercises.getOrNull(exerciseIndex)?.sets?.any { it.id == null } ?: false
        if (!hasPending) return
        viewModelScope.launch {
            setRowsLock.withLock {
                val curr = _state.value
                val sessionId = curr.sessionId ?: return@withLock
                val exState = curr.exercises.getOrNull(exerciseIndex) ?: return@withLock
                val pending = exState.sets.filter { it.id == null }
                if (exState.isCardio || pending.isEmpty()) return@withLock
                val executionOrder = exState.executionOrder
                val inserted = pending.associate { set ->
                    set.setNumber to set.copy(id = workoutRepository.logSet(set.toLogEntity(sessionId, exState, executionOrder)).toInt())
                }
                _state.update { state ->
                    val exercises = state.exercises.toMutableList()
                    val inner = exercises.getOrNull(exerciseIndex) ?: return@update state
                    if (inner.exercise.id != exState.exercise.id) return@update state
                    exercises[exerciseIndex] = inner.copy(sets = inner.sets.map { set ->
                        if (set.id == null) inserted[set.setNumber]?.let { set.copy(id = it.id) } ?: set else set
                    })
                    state.copy(exercises = exercises)
                }
                // Weights typed while the rows were being inserted had no id to be saved with yet
                _state.value.exercises.getOrNull(exerciseIndex)?.sets?.forEach { set ->
                    val saved = inserted[set.setNumber] ?: return@forEach
                    if (set.id == saved.id && set != saved) {
                        workoutRepository.updateSet(set.toLogEntity(sessionId, exState, executionOrder))
                    }
                }
            }
        }
    }

    private fun WorkoutSetState.toLogEntity(sessionId: Int, exState: WorkoutExerciseState, executionOrder: Int): SetLogEntity =
        SetLogEntity(
            id = id ?: 0,
            sessionId = sessionId,
            exerciseId = exState.exercise.id,
            pesoSollevato = weight,
            repsEffettive = reps,
            numeroSerie = setNumber,
            isWarmup = isWarmup,
            note = note,
            supersetId = exState.supersetId,
            isCompleted = isCompleted,
            ordineEsercizio = executionOrder,
            restTimerSeconds = exState.customRestSeconds,
            durataSecondi = timeSeconds
        ).withSnapshot(this)

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        _state.update { curr ->
            val mutableExercises = curr.exercises.toMutableList()
            val exState = mutableExercises.getOrNull(exerciseIndex) ?: return@update curr
            val mutableSets = exState.sets.toMutableList()
            val set = mutableSets.getOrNull(setIndex) ?: return@update curr

            if (set.isCompleted) return@update curr

            val updatedSet = set.withTotalReps(reps)
            mutableSets[setIndex] = updatedSet
            
            if (updatedSet.id != null && curr.sessionId != null) {
                val executionOrder = exState.executionOrder
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
            val executionOrder = exState.executionOrder
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

    // ---- Editing a set that is already logged ----
    // These only rewrite the saved values of that one set. They never touch whether it is completed, the rest
    // timer, the order the sets were done in or the weights of the other sets, which is what unchecking a set,
    // editing it and checking it again used to disturb.

    fun editCompletedSetWeight(exerciseIndex: Int, setIndex: Int, weightKg: Float) =
        editCompletedSet(exerciseIndex, setIndex) { it.copy(weight = weightKg) }

    fun editCompletedSetReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        editCompletedSet(exerciseIndex, setIndex) { it.withTotalReps(reps) }
        // The reps of a logged set count towards a total-reps block, whose pending sets follow what is left to do
        _state.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)?.prescription
            ?.takeIf { it.repMode == RepMode.TOTAL }
            ?.let { reconcileTotalBlock(exerciseIndex, it.blockIndex) }
    }

    fun editCompletedSetRpe(exerciseIndex: Int, setIndex: Int, rpe: Float?) =
        editCompletedSet(exerciseIndex, setIndex) { it.copy(rpe = rpe) }

    fun editCompletedSetSeconds(exerciseIndex: Int, setIndex: Int, seconds: Int) =
        editCompletedSet(exerciseIndex, setIndex) { it.copy(timeSeconds = seconds) }

    // Editing a set in place, whether it is done or still to do. A done set only rewrites its own values; a set
    // still to do goes through the same paths as the panel of the active set (a new weight also carries to the
    // sets after it that have nothing to go by).

    fun editSetWeight(exerciseIndex: Int, setIndex: Int, weightKg: Float) {
        if (isSetCompleted(exerciseIndex, setIndex)) editCompletedSetWeight(exerciseIndex, setIndex, weightKg)
        else updateSetWeight(exerciseIndex, setIndex, weightKg)
    }

    fun editSetReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        if (isSetCompleted(exerciseIndex, setIndex)) editCompletedSetReps(exerciseIndex, setIndex, reps)
        else updateSetReps(exerciseIndex, setIndex, reps)
    }

    fun editSetRpe(exerciseIndex: Int, setIndex: Int, rpe: Float?) {
        if (isSetCompleted(exerciseIndex, setIndex)) editCompletedSetRpe(exerciseIndex, setIndex, rpe)
        else updateSetRpe(exerciseIndex, setIndex, rpe)
    }

    fun editSetSeconds(exerciseIndex: Int, setIndex: Int, seconds: Int) {
        if (isSetCompleted(exerciseIndex, setIndex)) editCompletedSetSeconds(exerciseIndex, setIndex, seconds)
        else updateSetTimeSeconds(exerciseIndex, setIndex, seconds)
    }

    private fun isSetCompleted(exerciseIndex: Int, setIndex: Int): Boolean =
        _state.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)?.isCompleted == true

    private fun editCompletedSet(exerciseIndex: Int, setIndex: Int, update: (WorkoutSetState) -> WorkoutSetState) {
        val set = _state.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex) ?: return
        if (!set.isCompleted) return
        updateSetState(exerciseIndex, setIndex) { update(it).copy(isCompleted = true) }
    }

    /** A placeholder order no exercise of [state] uses yet, for an exercise that has nothing done. */
    private fun nextPlaceholderOrder(state: WorkoutState): Int =
        (state.exercises.map { it.executionOrder }.filter { it >= PROVISIONAL_ORDER_BASE }.maxOrNull() ?: (PROVISIONAL_ORDER_BASE - 1)) + 1

    /**
     * Gives the exercise at [exerciseIndex] its real execution order when it only has a placeholder, and moves its
     * logged rows to it. Returns the order the exercise ends up with. Call it with [setRowsLock] held, so that no
     * draft row is saved under the placeholder while the rows are being moved.
     */
    private suspend fun assignExecutionRank(sessionId: Int, exerciseIndex: Int, exState: WorkoutExerciseState): Int {
        var order = exState.executionOrder
        var placeholder: Int? = null
        _state.update { curr ->
            val inner = curr.exercises.getOrNull(exerciseIndex)
            if (inner == null || inner.exercise.id != exState.exercise.id || inner.hasExecutionRank) {
                order = inner?.takeIf { it.exercise.id == exState.exercise.id }?.executionOrder ?: exState.executionOrder
                placeholder = null
                return@update curr
            }
            order = curr.nextExecutionOrder
            placeholder = inner.executionOrder
            val exercises = curr.exercises.toMutableList()
            exercises[exerciseIndex] = inner.copy(executionOrder = order)
            curr.copy(exercises = exercises, nextExecutionOrder = order + 1)
        }
        placeholder?.let { from ->
            workoutRepository.moveExerciseRows(sessionId, exState.exercise.id, from, order)
            if (exState.isCardio) workoutRepository.moveCardioRows(sessionId, exState.exercise.nome, from, order)
        }
        return order
    }

    fun toggleSetComplete(exerciseIndex: Int, setIndex: Int) {
        val currentState = _state.value
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val setState = exState.sets.getOrNull(setIndex) ?: return
        
        val newIsCompleted = !setState.isCompleted
        // A set short of its reps can be finished after a short pause; unchecking it takes the offer back
        val missing = if (newIsCompleted && currentState.setPerformanceEnabled) SetPerformance.missingReps(miniSetTargetOf(exState, setState), setState.reps) else 0
        _state.update {
            it.copy(
                miniSetOffer = if (missing > 0) MiniSetOffer(exerciseIndex, setIndex, missing) else null,
                miniSet = null
            )
        }
        saveMiniSet(null)

        viewModelScope.launch {
            var newSetId = setState.id

            // The first set done of an exercise puts it in the running order of the workout
            var executionOrder = exState.executionOrder
            if (newIsCompleted && !exState.hasExecutionRank && currentState.sessionId != null) {
                executionOrder = setRowsLock.withLock { assignExecutionRank(currentState.sessionId, exerciseIndex, exState) }
            }

            if (currentState.sessionId != null) {
                val logId = setRowsLock.withLock {
                    // A draft row may have been saved for this set since the tap: reuse it instead of inserting another
                    val rowId = setState.id
                        ?: _state.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)?.id
                    val id = workoutRepository.logSet(
                        SetLogEntity(
                            id = rowId ?: 0,
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
                    // Hand the id to the state before the lock is released, or a draft save could insert it again
                    _state.update { state ->
                        val exercises = state.exercises.toMutableList()
                        val inner = exercises.getOrNull(exerciseIndex) ?: return@update state
                        val sets = inner.sets.toMutableList()
                        val current = sets.getOrNull(setIndex) ?: return@update state
                        sets[setIndex] = current.copy(id = id.toInt())
                        exercises[exerciseIndex] = inner.copy(sets = sets)
                        state.copy(exercises = exercises)
                    }
                    id
                }
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
                                nextSetDetail = prescriptionDetail(nextSet),
                                nextSetText = timedSetText(exState, nextSet)
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
                                    nextSetDetail = prescriptionDetail(firstSet),
                                    nextSetText = timedSetText(nextExState, firstSet)
                                )
                            } else {
                                // A cardio exercise has no weight sets: its next set is announced by its targets
                                startRestTimer(restTime, nextExState?.let { nextSetInfoOf(it) })
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
                val executionOrder = exState.executionOrder
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
        nextSetDetail: String? = null,
        nextSetText: String? = null
    ) {
        if (seconds <= 0) return
        stopRestTimer()
        val endTime = System.currentTimeMillis() + (seconds * 1000L)
        _state.update { it.copy(remainingRestSeconds = seconds, totalRestSeconds = seconds, restTimerEndTime = endTime) }
        if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
            // The rest timer takes the place of a "warmup finished" notification still on screen; a warmup that is
            // still counting down keeps its own
            if (_state.value.warmupTimerEndTime == null) timerNotificationHelper.cancelWarmupTimer()
            _state.value.sessionId?.let { sessionId ->
                timerNotificationHelper.startOrUpdateTimerNotification(
                    seconds, sessionId, exerciseName, nextSetNumber, nextSetWeight, nextSetReps, previousReps, weightUnit,
                    totalSeconds = seconds,
                    nextSetRepsLabel = nextSetRepsLabel,
                    nextSetDetail = nextSetDetail,
                    nextSetText = nextSetText
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
        // A swapped-in exercise only keeps the plan exercise it replaced for the record: its reps are its own sets'
        if (exState?.swappedExerciseId != null) return null
        val plan = exState?.planDetails ?: return null
        val repsList = parseReps(plan.repsTarget, plan.serieTarget)
        return repsList.getOrElse(setNumber - 1) { repsList.lastOrNull() }
    }

    /** What the rest notification announces for [exState]: its next set to do (a cardio one included), null when none is left. */
    private fun nextSetInfoOf(exState: WorkoutExerciseState): NextSetInfo? {
        val name = ExerciseTranslations.translate(exState.exercise.nome, _languageCode.value)
        val unit = _state.value.weightUnit
        if (exState.isCardio) {
            val set = exState.activeCardioSet ?: return null
            return NextSetInfo(exerciseName = name, setNumber = set.setNumber, weight = 0f, reps = 0, weightUnit = unit, text = cardioTargetText(exState))
        }
        val nextSet = exState.sets.firstOrNull { !it.isCompleted } ?: return null
        val plannedReps = getPlannedRepsForSet(exState, nextSet.setNumber) ?: nextSet.reps
        return NextSetInfo(
            exerciseName = name,
            setNumber = nextSet.setNumber,
            weight = nextSet.weight,
            reps = plannedReps,
            weightUnit = unit,
            previousReps = nextSet.previousReps,
            repsLabel = repsLabelFor(nextSet),
            detail = prescriptionDetail(nextSet),
            text = timedSetText(exState, nextSet)
        )
    }

    /** The next set of the exercise on screen or, once it is all done, the first one of the exercise after it. */
    private fun buildNextSetLabel(): NextSetInfo? {
        val state = _state.value
        // During the pause of a mini-set, what comes next is the mini-set
        state.miniSet?.takeIf { !it.awaitingReps }?.let { run -> return miniSetInfo(run) }
        val currentEx = state.currentExercise ?: return null
        return nextSetInfoOf(currentEx) ?: state.exercises.getOrNull(state.currentExerciseIndex + 1)?.let { nextSetInfoOf(it) }
    }

    /** A cardio set as the rest notification announces it: its targets ("5 min · 1.5 km"). */
    private fun cardioTargetText(exState: WorkoutExerciseState): String {
        val context = localeManager.localizedContext()
        val parts = listOfNotNull(
            exState.cardioDurataTargetSeconds?.takeIf { it >= 60 }?.let { context.getString(R.string.cardio_min_format, it / 60) },
            exState.cardioDistanzaTargetKm?.takeIf { it > 0f }?.let { context.getString(R.string.cardio_dist_value, it.toString()) }
        )
        return parts.joinToString(" · ").ifEmpty { context.getString(R.string.cardio_free_form) }
    }

    private fun startRestTimer(seconds: Int, info: NextSetInfo?) {
        if (info == null) return startRestTimer(seconds)
        startRestTimer(
            seconds, info.exerciseName, info.setNumber, info.weight, info.reps, info.previousReps, info.weightUnit,
            nextSetRepsLabel = info.repsLabel,
            nextSetDetail = info.detail,
            nextSetText = info.text
        )
    }

    /** A timed set as the rest notification writes it: "45s · 20 reps", "20 kg × 45s"; null for weight × reps sets. */
    private fun timedSetText(exState: WorkoutExerciseState?, set: WorkoutSetState?): String? {
        if (exState == null || set == null || !exState.isTimed) return null
        val seconds = set.timeSeconds ?: exState.timeTargetSeconds ?: 45
        if (exState.isTimeAndReps) return localeManager.localizedContext().getString(R.string.time_reps_value, seconds, set.reps)
        val unit = _state.value.weightUnit
        return WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(set.weight, unit), unit) + " × ${seconds}s"
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
                        nextSetDetail = info?.detail,
                        nextSetText = info?.text
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
            endMiniSetPause()
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
                                nextSetDetail = info?.detail,
                                nextSetText = info?.text
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

    /** Moves the rest countdown by [seconds] (negative takes time off); taking off all that is left ends it. */
    fun adjustRestTime(seconds: Int) {
        val currentEnd = _state.value.restTimerEndTime
        if (currentEnd != null) {
            val newEnd = currentEnd + (seconds * 1000L)
            val newRemaining = ((newEnd - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            if (seconds < 0 && newRemaining <= 0) {
                skipRestTimer()
                return
            }
            val newTotal = (_state.value.totalRestSeconds + seconds).coerceAtLeast(newRemaining)
            _state.update { it.copy(restTimerEndTime = newEnd, remainingRestSeconds = newRemaining, totalRestSeconds = newTotal) }
            if (_state.value.timerNotificationsEnabled && timerNotificationHelper.hasNotificationPermission()) {
                _state.value.sessionId?.let { sessionId ->
                    val info = buildNextSetLabel()
                    timerNotificationHelper.startOrUpdateTimerNotification(
                        newRemaining, sessionId,
                        info?.exerciseName, info?.setNumber, info?.weight, info?.reps, info?.previousReps, info?.weightUnit,
                        totalSeconds = newTotal,
                        nextSetRepsLabel = info?.repsLabel,
                        nextSetDetail = info?.detail,
                        nextSetText = info?.text
                    )
                }
            }
            saveTimerToSession(newEnd, newTotal)
        }
    }

    fun skipRestTimer() {
        endMiniSetPause()
        stopRestTimer()
        clearTimerInSession()
    }

    // ---- Mini-sets, assisted and partial reps, technique as done ----

    /**
     * The reps a set is meant to reach, when it has a goal a mini-set can make up for: an advanced set with fixed
     * reps, the bottom of a range, the reps of the plan. AMRAP and total-reps sets, EMOM rounds, timed sets and cardio
     * have none.
     */
    private fun miniSetTargetOf(ex: WorkoutExerciseState, set: WorkoutSetState): Int? {
        if (ex.isCardio || ex.isTimed || set.isEmom) return null
        val p = set.prescription
        if (p != null) return p.targetReps.takeIf { p.repMode == RepMode.FIXED }
        set.repsRange?.let { return it.min }
        return getPlannedRepsForSet(ex, set.setNumber)
    }

    /** Starts the short pause of a mini-set on the done set at [setIndex]; its reps are asked for when the pause ends. */
    fun startMiniSet(exerciseIndex: Int, setIndex: Int) {
        val ex = _state.value.exercises.getOrNull(exerciseIndex) ?: return
        val set = ex.sets.getOrNull(setIndex)?.takeIf { it.isCompleted && !ex.isTimed && !ex.isCardio } ?: return
        val missing = SetPerformance.missingReps(miniSetTargetOf(ex, set), set.reps)
        val run = MiniSetRun(exerciseIndex, setIndex, missing, System.currentTimeMillis())
        startRestTimer(SetPerformance.DEFAULT_PAUSE_SECONDS, miniSetInfo(run))
        _state.update { it.copy(miniSetOffer = null, miniSet = run) }
        saveMiniSet(run)
    }

    // A mini-set lives through the app being closed: its set, its pause and what is missing are kept on the device
    private val miniSetPrefs by lazy {
        localeManager.localizedContext().getSharedPreferences("workout_mini_set", android.content.Context.MODE_PRIVATE)
    }

    private fun saveMiniSet(run: MiniSetRun?) {
        val sessionId = _state.value.sessionId
        val setId = run?.let { _state.value.exercises.getOrNull(it.exerciseIndex)?.sets?.getOrNull(it.setIndex)?.id }
        if (run == null || sessionId == null || setId == null) {
            miniSetPrefs.edit().clear().apply()
            return
        }
        miniSetPrefs.edit()
            .putInt(MINI_SESSION, sessionId)
            .putInt(MINI_SET, setId)
            .putInt(MINI_MISSING, run.missing)
            .putLong(MINI_STARTED, run.pauseStartedAt)
            .putLong(MINI_ENDS, _state.value.restTimerEndTime ?: 0L)
            .putInt(MINI_PAUSE, run.pauseSeconds ?: -1)
            .apply()
    }

    /** Brings back the mini-set the workout was in when it was left: still pausing, or asking for its reps. */
    private fun restoreMiniSet() {
        val state = _state.value
        val sessionId = state.sessionId ?: return
        if (miniSetPrefs.getInt(MINI_SESSION, -1) != sessionId) return
        val setId = miniSetPrefs.getInt(MINI_SET, -1)
        val exerciseIndex = state.exercises.indexOfFirst { ex -> ex.sets.any { it.id == setId } }
        val setIndex = state.exercises.getOrNull(exerciseIndex)?.sets?.indexOfFirst { it.id == setId } ?: -1
        if (exerciseIndex < 0 || setIndex < 0) {
            saveMiniSet(null)
            return
        }
        val startedAt = miniSetPrefs.getLong(MINI_STARTED, System.currentTimeMillis())
        val savedPause = miniSetPrefs.getInt(MINI_PAUSE, -1).takeIf { it >= 0 }
        // The pause ran out while the app was closed: it lasted what it was set to
        val pause = savedPause ?: if (state.remainingRestSeconds <= 0) {
            val endsAt = miniSetPrefs.getLong(MINI_ENDS, 0L).takeIf { it > startedAt } ?: System.currentTimeMillis()
            ((endsAt - startedAt) / 1000).toInt()
        } else null
        _state.update {
            it.copy(miniSet = MiniSetRun(exerciseIndex, setIndex, miniSetPrefs.getInt(MINI_MISSING, 0), startedAt, pause), miniSetOffer = null)
        }
    }

    /** The rest notification during the pause of a mini-set: "Next: Bench - set 3: mini-set". */
    private fun miniSetInfo(run: MiniSetRun): NextSetInfo? {
        val ex = _state.value.exercises.getOrNull(run.exerciseIndex) ?: return null
        val set = ex.sets.getOrNull(run.setIndex) ?: return null
        return NextSetInfo(
            exerciseName = ExerciseTranslations.translate(ex.exercise.nome, _languageCode.value),
            setNumber = set.setNumber,
            weight = set.weight,
            reps = run.missing,
            weightUnit = _state.value.weightUnit,
            text = localeManager.localizedContext().getString(R.string.mini_set_notification)
        )
    }

    private fun endMiniSetPause() {
        val run = _state.value.miniSet ?: return
        if (run.awaitingReps) return
        val seconds = ((System.currentTimeMillis() - run.pauseStartedAt) / 1000).toInt().coerceAtLeast(0)
        _state.update { it.copy(miniSet = run.copy(pauseSeconds = seconds)) }
        saveMiniSet(_state.value.miniSet)
    }

    /** Adds the [reps] of the mini-set to its set, then the normal rest starts (and offers another one if still short). */
    fun logMiniSet(reps: Int) {
        val run = _state.value.miniSet ?: return
        val ex = _state.value.exercises.getOrNull(run.exerciseIndex) ?: return
        val set = ex.sets.getOrNull(run.setIndex) ?: return
        if (reps > 0) {
            val parts = SetPerformance.withPart(set.repParts, set.reps, reps)
            updateSetState(run.exerciseIndex, run.setIndex) {
                it.copy(repParts = parts, reps = parts.sum(), pauses = it.pauses + (run.pauseSeconds ?: SetPerformance.DEFAULT_PAUSE_SECONDS))
            }
        }
        val updated = _state.value.exercises.getOrNull(run.exerciseIndex)?.sets?.getOrNull(run.setIndex) ?: set
        finishMiniSets(run.exerciseIndex, run.setIndex, updated)
    }

    /** No more mini-sets on this set: the normal rest starts. */
    fun endMiniSet() {
        val run = _state.value.miniSet ?: return
        val ex = _state.value.exercises.getOrNull(run.exerciseIndex) ?: return
        val set = ex.sets.getOrNull(run.setIndex) ?: return
        finishMiniSets(run.exerciseIndex, run.setIndex, set, offerAgain = false)
    }

    private fun finishMiniSets(exerciseIndex: Int, setIndex: Int, set: WorkoutSetState, offerAgain: Boolean = true) {
        val ex = _state.value.exercises.getOrNull(exerciseIndex) ?: return
        _state.update { it.copy(miniSet = null) }
        saveMiniSet(null)
        val missing = if (offerAgain && _state.value.setPerformanceEnabled) SetPerformance.missingReps(miniSetTargetOf(ex, set), set.reps) else 0
        val rest = restSecondsAfter(ex, set)
        if (rest > 0) startRestTimer(rest, buildNextSetLabel())
        _state.update { it.copy(miniSetOffer = if (missing > 0) MiniSetOffer(exerciseIndex, setIndex, missing) else null) }
    }

    fun dismissMiniSetOffer() = _state.update { it.copy(miniSetOffer = null) }

    // A set still to do keeps these through leaving the workout: its rows are saved like for a typed weight
    fun updateSetAssistedReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        updateSetState(exerciseIndex, setIndex) { it.copy(assistedReps = reps.coerceIn(0, 99)) }
        persistPendingSets(exerciseIndex)
    }

    fun updateSetPartialReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        updateSetState(exerciseIndex, setIndex) { it.copy(partialReps = reps.coerceIn(0, 99)) }
        persistPendingSets(exerciseIndex)
    }

    /**
     * The techniques a set was really done with ([techniques] null: as prescribed). With [applyToFollowing] the sets of
     * the same block still to do get them too: what could not be done once rarely can on the next set.
     */
    fun setPerformedTechniques(exerciseIndex: Int, setIndex: Int, techniques: List<Technique>?, applyToFollowing: Boolean) {
        val ex = _state.value.exercises.getOrNull(exerciseIndex) ?: return
        val set = ex.sets.getOrNull(setIndex) ?: return
        val planned = set.prescription?.techniques.orEmpty()
        // Back to the plan when nothing differs from it
        val performed = techniques?.takeIf { it != planned }
        val block = set.prescription?.blockIndex
        ex.sets.forEachIndexed { index, other ->
            val sameBlock = other.prescription?.blockIndex == block && other.isExtra == set.isExtra
            if (index == setIndex || (applyToFollowing && index > setIndex && !other.isCompleted && sameBlock)) {
                updateSetState(exerciseIndex, index) { it.copy(performedTechniques = performed) }
            }
        }
        persistPendingSets(exerciseIndex)
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

    /** Moves the warmup countdown by [seconds] (negative takes time off); taking off all that is left ends it. */
    fun adjustWarmupTime(seconds: Int) {
        val currentEnd = _state.value.warmupTimerEndTime
        if (currentEnd != null) {
            val newEnd = currentEnd + (seconds * 1000L)
            val newRemaining = ((newEnd - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            if (seconds < 0 && newRemaining <= 0) {
                skipWarmupTimer()
                return
            }
            val newTotal = (_state.value.warmupTimerTotalSeconds + seconds).coerceAtLeast(newRemaining)
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
        blocks: List<PrescriptionBlock>? = null,
        /** Top of a rep range; [repsTarget] is then its bottom. */
        repsMax: Int? = null
    ) {
        val currentState = _state.value
        val sessionId = currentState.sessionId ?: return
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val originalExerciseId = exState.planDetails?.id

        val replacementExercise = _availableExercises.value.find { it.id == newExerciseId } ?: return

        viewModelScope.launch {
            // Delete any existing completed/saved sets of the old exercise from the database for this session
            removeExerciseRows(sessionId, exerciseIndex, exState)

            val advancedBlocks = blocks.orEmpty()
            val newOneRepMaxKg = if (advancedBlocks.isNotEmpty()) oneRepMaxRepository.currentOnce(newExerciseId)?.weightKg else null
            
            val previousSets = getPreviousSetsForExercise(currentState.planId, newExerciseId, if (advancedBlocks.isNotEmpty()) ALL_SETS else targetSets)
            val setKind = SetLogKind.ofExerciseType(exerciseType)
            val defaultTargetSeconds = durataTargetSecondi ?: 45
            val prevPerfStr = previousPerformanceText(previousSets, setKind)

            val repsRange = rangeFor(exerciseType, repsTarget, repsMax)

            // The replacement takes the place of the old exercise in the running order
            val executionOrder = exState.executionOrder

            val initialSets = if (advancedBlocks.isNotEmpty()) {
                createAdvancedRows(sessionId, newExerciseId, executionOrder, exState.supersetId, restTimer, advancedBlocks, previousSets, newOneRepMaxKg)
            } else {
                createPlainRows(
                    sessionId, newExerciseId, executionOrder, exState.supersetId, restTimer,
                    targetSets, repsTarget, repsRange, previousSets, setKind, defaultTargetSeconds
                )
            }
            val progression = if (repsRange != null && advancedBlocks.isEmpty()) {
                progressionFor(
                    targetSets, repsRange,
                    getPreviousSetsForExercise(currentState.planId, newExerciseId, ALL_SETS),
                    currentState.weightUnit, currentState.loadRoundingIncrement
                )
            } else null

            if (originalExerciseId != null) {
                // One swap per plan exercise: a second swap replaces the first instead of piling up next to it
                workoutRepository.removeExerciseSwap(sessionId, originalExerciseId)
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
                        sets = initialSets,
                        previousPerformance = prevPerfStr,
                        customRestSeconds = restTimer,
                        customRepsTarget = repsTarget,
                        customRepsMax = repsRange?.max,
                        progression = progression,
                        progressionDismissed = false,
                        exerciseType = exerciseType,
                        timeTargetSeconds = durataTargetSecondi,
                        blocks = advancedBlocks,
                        oneRepMaxKg = newOneRepMaxKg,
                        isCardio = false,
                        cardioSets = emptyList()
                    )
                    curr.copy(exercises = mutableExercises, exerciseSwaps = mutableSwaps)
                }
            } else {
                _state.update { curr ->
                    val mutableExercises = curr.exercises.toMutableList()

                    mutableExercises[exerciseIndex] = exState.copy(
                        exercise = replacementExercise,
                        swappedExerciseId = newExerciseId,
                        sets = initialSets,
                        previousPerformance = prevPerfStr,
                        customRestSeconds = restTimer,
                        customRepsTarget = repsTarget,
                        customRepsMax = repsRange?.max,
                        progression = progression,
                        progressionDismissed = false,
                        exerciseType = exerciseType,
                        timeTargetSeconds = durataTargetSecondi,
                        blocks = advancedBlocks,
                        oneRepMaxKg = newOneRepMaxKg,
                        isCardio = false,
                        cardioSets = emptyList()
                    )
                    curr.copy(exercises = mutableExercises)
                }
            }
        }
    }

    /**
     * Drops the rows [exState] left in the session when it is swapped out. Rows are matched by exercise and order
     * so that another instance of the same exercise stays; a cardio exercise leaves a cardio log instead of sets.
     */
    private suspend fun removeExerciseRows(sessionId: Int, exerciseIndex: Int, exState: WorkoutExerciseState) {
        val sameExerciseElsewhere = _state.value.exercises.withIndex().any { (index, other) ->
            index != exerciseIndex && other.exercise.id == exState.exercise.id
        }
        if (sameExerciseElsewhere) {
            workoutRepository.deleteExerciseRows(sessionId, exState.exercise.id, exState.executionOrder)
        } else {
            workoutRepository.deleteExerciseFromSession(sessionId, exState.exercise.id)
        }
        if (exState.isCardio) workoutRepository.deleteCardioRows(sessionId, exState.exercise.nome, exState.executionOrder)
    }

    /**
     * Gives every set of a cardio exercise swapped in or added during the workout its log right away: there is no plan
     * to tell how many sets it has when the workout is reopened.
     */
    private suspend fun createCardioRows(sessionId: Int, exercise: ExerciseEntity, order: Int, targetSeconds: Int?, count: Int): List<CardioSetState> =
        (1..count.coerceAtLeast(1)).map { number ->
            val id = workoutRepository.saveCardioLog(
                CardioLogEntity(
                    sessionId = sessionId,
                    categoria = exercise.nome,
                    distanza = 0f,
                    durataSecondi = 0,
                    timestamp = System.currentTimeMillis(),
                    ordineEsercizio = order,
                    durataTargetSecondi = targetSeconds,
                    isCompleted = false,
                    numeroSerie = number
                )
            ).toInt()
            CardioSetState(id = id, setNumber = number)
        }

    fun swapToCardioExercise(exerciseIndex: Int, newExerciseId: Int, durationMinutes: Int, restTimer: Int? = null, cardioSetsCount: Int = 1) {
        val currentState = _state.value
        val sessionId = currentState.sessionId ?: return
        val exState = currentState.exercises.getOrNull(exerciseIndex) ?: return
        val originalExerciseId = exState.planDetails?.id

        val replacementExercise = _availableExercises.value.find { it.id == newExerciseId } ?: return

        viewModelScope.launch {
            removeExerciseRows(sessionId, exerciseIndex, exState)
            val newCardioSets = createCardioRows(sessionId, replacementExercise, exState.executionOrder, durationMinutes * 60, cardioSetsCount)

            if (originalExerciseId != null) {
                workoutRepository.removeExerciseSwap(sessionId, originalExerciseId)
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
                        cardioDurataTargetSeconds = durationMinutes * 60,
                        cardioSets = newCardioSets,
                        exerciseType = "cardio",
                        timeTargetSeconds = null,
                        blocks = emptyList(),
                        oneRepMaxKg = null
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
                        cardioDurataTargetSeconds = durationMinutes * 60,
                        cardioSets = newCardioSets,
                        exerciseType = "cardio",
                        timeTargetSeconds = null,
                        blocks = emptyList(),
                        oneRepMaxKg = null
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
                val currentOrder = currentEx.executionOrder
                val nextOrder = nextEx.executionOrder
                
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
        /** Sets of a cardio exercise. */
        cardioSetsCount: Int = 1,
        durataTargetSecondi: Int? = null,
        blocks: List<PrescriptionBlock>? = null,
        /** Top of a rep range; [repsTarget] is then its bottom. */
        repsMax: Int? = null
    ) {
        val sessionId = _state.value.sessionId ?: return
        val isCardio = exercise.categoria.equals("Cardio", ignoreCase = true) || exerciseType == "cardio"
        val setKind = SetLogKind.ofExerciseType(exerciseType)
        val targetDurationSeconds = if (isCardio) (cardioDurationMinutes ?: 15) * 60 else durataTargetSecondi
        val defaultTargetSeconds = durataTargetSecondi ?: 45
        val advancedBlocks = if (isCardio) emptyList() else blocks.orEmpty()

        viewModelScope.launch {
            val currentState = _state.value
            // A placeholder until the exercise is started, so it cannot clash with the order of one that already was
            val executionOrder = nextPlaceholderOrder(currentState)
            val newOneRepMaxKg = if (advancedBlocks.isNotEmpty()) oneRepMaxRepository.currentOnce(exercise.id)?.weightKg else null

            val previousSets = if (isCardio) emptyList() else getPreviousSetsForExercise(currentState.planId, exercise.id, if (advancedBlocks.isNotEmpty()) ALL_SETS else targetSets)
            val prevPerfStr = previousPerformanceText(previousSets, setKind)

            val repsRange = rangeFor(exerciseType, repsTarget, repsMax)

            val newCardioSets = if (isCardio) createCardioRows(sessionId, exercise, executionOrder, targetDurationSeconds, cardioSetsCount) else emptyList()
            
            val initialSets = if (advancedBlocks.isNotEmpty()) {
                createAdvancedRows(sessionId, exercise.id, executionOrder, null, restTimer, advancedBlocks, previousSets, newOneRepMaxKg)
            } else if (isCardio) emptyList() else {
                createPlainRows(
                    sessionId, exercise.id, executionOrder, null, restTimer,
                    targetSets, repsTarget, repsRange, previousSets, setKind, defaultTargetSeconds
                )
            }
            val progression = if (repsRange != null && !isCardio && advancedBlocks.isEmpty()) {
                progressionFor(
                    targetSets, repsRange,
                    getPreviousSetsForExercise(currentState.planId, exercise.id, ALL_SETS),
                    currentState.weightUnit, currentState.loadRoundingIncrement
                )
            } else null

            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val actualIndex = mutableExercises.size

                mutableExercises.add(
                    WorkoutExerciseState(
                        exercise = exercise,
                        planDetails = null,
                        executionOrder = executionOrder,
                        sets = initialSets,
                        previousPerformance = prevPerfStr,
                        customRestSeconds = restTimer,
                        customRepsTarget = repsTarget,
                        customRepsMax = repsRange?.max,
                        progression = progression,
                        isCardio = isCardio,
                        cardioCategoria = exercise.categoria,
                        cardioDurataTargetSeconds = targetDurationSeconds,
                        cardioSets = newCardioSets,
                        exerciseType = exerciseType,
                        timeTargetSeconds = durataTargetSecondi,
                        blocks = advancedBlocks,
                        oneRepMaxKg = newOneRepMaxKg
                    )
                )
                curr.copy(
                    exercises = mutableExercises,
                    currentExerciseIndex = actualIndex
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
        /** Sets of a cardio exercise. */
        cardioSetsCount: Int = 1,
        durataTargetSecondi: Int? = null,
        blocks: List<PrescriptionBlock>? = null,
        /** Top of a rep range; [repsTarget] is then its bottom. */
        repsMax: Int? = null
    ) {
        val sessionId = _state.value.sessionId ?: return
        val isCardio = exercise.categoria.equals("Cardio", ignoreCase = true) || exerciseType == "cardio"
        val setKind = SetLogKind.ofExerciseType(exerciseType)
        val targetDurationSeconds = if (isCardio) (cardioDurationMinutes ?: 15) * 60 else durataTargetSecondi
        val defaultTargetSeconds = durataTargetSecondi ?: 45
        val advancedBlocks = if (isCardio) emptyList() else blocks.orEmpty()

        viewModelScope.launch {
            val currentState = _state.value
            val newOneRepMaxKg = if (advancedBlocks.isNotEmpty()) oneRepMaxRepository.currentOnce(exercise.id)?.weightKg else null
            val insertAt = currentState.currentExerciseIndex + 1
            // Where it sits on screen does not depend on its order: it gets a placeholder, and the real order
            // comes with its first completed set, so nothing else has to be renumbered.
            val newOrder = nextPlaceholderOrder(currentState)

            val previousSets = if (isCardio) emptyList() else getPreviousSetsForExercise(currentState.planId, exercise.id, if (advancedBlocks.isNotEmpty()) ALL_SETS else targetSets)
            val prevPerfStr = previousPerformanceText(previousSets, setKind)

            val repsRange = rangeFor(exerciseType, repsTarget, repsMax)

            val newCardioSets = if (isCardio) createCardioRows(sessionId, exercise, newOrder, targetDurationSeconds, cardioSetsCount) else emptyList()

            val initialSets = if (advancedBlocks.isNotEmpty()) {
                createAdvancedRows(sessionId, exercise.id, newOrder, null, restTimer, advancedBlocks, previousSets, newOneRepMaxKg)
            } else if (isCardio) emptyList() else {
                createPlainRows(
                    sessionId, exercise.id, newOrder, null, restTimer,
                    targetSets, repsTarget, repsRange, previousSets, setKind, defaultTargetSeconds
                )
            }
            val progression = if (repsRange != null && !isCardio && advancedBlocks.isEmpty()) {
                progressionFor(
                    targetSets, repsRange,
                    getPreviousSetsForExercise(currentState.planId, exercise.id, ALL_SETS),
                    currentState.weightUnit, currentState.loadRoundingIncrement
                )
            } else null

            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                mutableExercises.add(
                    insertAt,
                    WorkoutExerciseState(
                        exercise = exercise,
                        planDetails = null,
                        executionOrder = newOrder,
                        sets = initialSets,
                        previousPerformance = prevPerfStr,
                        customRestSeconds = restTimer,
                        customRepsTarget = repsTarget,
                        customRepsMax = repsRange?.max,
                        progression = progression,
                        isCardio = isCardio,
                        cardioCategoria = exercise.categoria,
                        cardioDurataTargetSeconds = targetDurationSeconds,
                        cardioSets = newCardioSets,
                        exerciseType = exerciseType,
                        timeTargetSeconds = durataTargetSecondi,
                        blocks = advancedBlocks,
                        oneRepMaxKg = newOneRepMaxKg
                    )
                )

                curr.copy(
                    exercises = mutableExercises,
                    currentExerciseIndex = insertAt
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
        val weight = if (exState.isTimeAndReps) 0f else lastSet?.weight ?: 0f
        val reps = lastSet?.reps ?: exState.repsGoal ?: 8
        val setDuration = if (exState.isTimed) (lastSet?.timeSeconds ?: exState.timeTargetSeconds ?: 45) else lastSet?.timeSeconds
        // On advanced exercises a new set repeats the last one's prescription (same block)
        val prescription = lastSet?.prescription?.takeIf { it.repMode != RepMode.TOTAL }?.let { it.copy(indexInBlock = it.indexInBlock + 1) }
        val template = WorkoutSetState(
            setNumber = newSetNumber,
            weight = weight,
            reps = reps,
            isCompleted = false,
            timeSeconds = setDuration,
            prescription = prescription,
            isExtra = lastSet?.isExtra == true,
            repsRange = lastSet?.repsRange,
            isTimeAndReps = exState.isTimeAndReps
        )

        viewModelScope.launch {
            val executionOrder = exState.executionOrder
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
            val executionOrder = exState.executionOrder
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
                    // An exercise added during the workout has its log from the start: starting it is what ranks it
                    setRowsLock.withLock { assignExecutionRank(sessionId, currState.currentExerciseIndex, currentEx) }
                } else {
                    val order = setRowsLock.withLock { assignExecutionRank(sessionId, currState.currentExerciseIndex, currentEx) }
                    val newLog = com.emanuel5014.trainable.data.local.entity.CardioLogEntity(
                        sessionId = sessionId,
                        categoria = currentEx.exercise.nome,
                        distanza = currentEx.cardioDistanceKm,
                        durataSecondi = initialSeconds,
                        durataTargetSecondi = currentEx.cardioDurataTargetSeconds,
                        timestamp = System.currentTimeMillis(),
                        ordineEsercizio = order,
                        isCompleted = false,
                        numeroSerie = currentEx.activeCardioSet?.setNumber ?: 1,
                        note = currentEx.activeCardioSet?.note
                    )
                    logId = workoutRepository.saveCardioLog(newLog).toInt()
                }

                _state.update { state ->
                    val updatedExercises = state.exercises.toMutableList()
                    val idx = state.currentExerciseIndex
                    if (idx in updatedExercises.indices) {
                        updatedExercises[idx] = updatedExercises[idx].withActiveCardioSet { it.copy(id = logId) }
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
                persistCardioSets(currState.currentExerciseIndex)
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
                                updatedExercises[idx] = updatedExercises[idx].withActiveCardioSet { it.copy(elapsedSeconds = elapsed) }
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
                            updatedExercises[idx] = updatedExercises[idx].withActiveCardioSet { it.copy(elapsedSeconds = elapsed) }
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
                updatedExercises[idx] = updatedExercises[idx].withActiveCardioSet { it.copy(elapsedSeconds = target) }
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

    /** Marks the current cardio exercise as done without running its timer, logging [durationSeconds] and the distance. */
    fun completeCardio(distanzaKm: Float, durationSeconds: Int) = stopCardioTimer(distanzaKm, durationSeconds)

    /** Saves the cardio log with the time on the timer, or with [durationSeconds] when it is given. */
    fun stopCardioTimer(distanzaKm: Float, durationSeconds: Int? = null) {
        cardioTimerJob?.cancel()
        cardioTimerJob = null
        val currState = _state.value
        val sessionId = currState.sessionId ?: return
        val currentEx = currState.currentExercise ?: return
        val logId = currentEx.cardioLogId
        val doneSet = currentEx.activeCardioSet ?: return

        val elapsed = durationSeconds ?: currState.cardioTimerSeconds
        _state.update { state ->
            val updatedExercises = state.exercises.toMutableList()
            val idx = state.currentExerciseIndex
            if (idx in updatedExercises.indices) {
                updatedExercises[idx] = updatedExercises[idx].withActiveCardioSet {
                    it.copy(elapsedSeconds = elapsed, distanceKm = distanzaKm, isCompleted = true)
                }
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
            val order = setRowsLock.withLock { assignExecutionRank(sessionId, currState.currentExerciseIndex, currentEx) }
            val cardioEntity = com.emanuel5014.trainable.data.local.entity.CardioLogEntity(
                id = logId ?: 0,
                sessionId = sessionId,
                categoria = currentEx.exercise.nome,
                distanza = distanzaKm,
                durataSecondi = elapsed,
                durataTargetSecondi = currentEx.cardioDurataTargetSeconds,
                timestamp = System.currentTimeMillis(),
                ordineEsercizio = order,
                isCompleted = true,
                numeroSerie = doneSet.setNumber,
                note = doneSet.note
            )
            if (logId != null && logId > 0) {
                workoutRepository.updateCardioLog(cardioEntity)
            } else {
                val newId = workoutRepository.saveCardioLog(cardioEntity).toInt()
                setCardioSetId(currState.currentExerciseIndex, doneSet.setNumber, newId)
            }
            persistCardioSets(currState.currentExerciseIndex)
        }

        val restSeconds = currentEx.customRestSeconds ?: currentEx.planDetails?.recuperoTarget ?: 0
        if (restSeconds > 0) {
            // The next set of this exercise, or the first of the next one
            val info = buildNextSetLabel()
            if (info != null) {
                startRestTimer(restSeconds, info)
            } else {
                startRestTimer(restSeconds, exerciseName = ExerciseTranslations.translate(currentEx.exercise.nome, _languageCode.value))
            }
        }
    }

    // ---- Cardio sets ----

    private fun updateCardioSets(exerciseIndex: Int, update: (List<CardioSetState>) -> List<CardioSetState>) {
        _state.update { curr ->
            val ex = curr.exercises.getOrNull(exerciseIndex) ?: return@update curr
            val exercises = curr.exercises.toMutableList()
            exercises[exerciseIndex] = ex.copy(cardioSets = update(ex.cardioSets))
            curr.copy(exercises = exercises)
        }
    }

    private fun setCardioSetId(exerciseIndex: Int, setNumber: Int, id: Int) =
        updateCardioSets(exerciseIndex) { sets -> sets.map { if (it.setNumber == setNumber && it.id == null) it.copy(id = id) else it } }

    /**
     * Gives every set of the cardio exercise that has none its log. Once one set of an exercise has a log they all
     * have one, so a workout that is reopened finds exactly the sets it had (sets added or removed included) instead
     * of the plan's count.
     */
    private suspend fun persistCardioSets(exerciseIndex: Int) = setRowsLock.withLock {
        val state = _state.value
        val sessionId = state.sessionId ?: return@withLock
        val ex = state.exercises.getOrNull(exerciseIndex)?.takeIf { it.isCardio } ?: return@withLock
        ex.cardioSets.filter { it.id == null }.forEach { set ->
            val id = workoutRepository.saveCardioLog(
                CardioLogEntity(
                    sessionId = sessionId,
                    categoria = ex.exercise.nome,
                    distanza = set.distanceKm,
                    durataSecondi = set.elapsedSeconds,
                    timestamp = System.currentTimeMillis(),
                    ordineEsercizio = ex.executionOrder,
                    durataTargetSecondi = ex.cardioDurataTargetSeconds,
                    isCompleted = false,
                    numeroSerie = set.setNumber,
                    note = set.note
                )
            ).toInt()
            setCardioSetId(exerciseIndex, set.setNumber, id)
        }
    }

    /** One more set of the cardio exercise, with the same targets; a done exercise goes back to its timer. */
    fun addCardioSet(exerciseIndex: Int) {
        val ex = _state.value.exercises.getOrNull(exerciseIndex)?.takeIf { it.isCardio } ?: return
        updateCardioSets(exerciseIndex) { it + CardioSetState(setNumber = ex.cardioSets.size + 1) }
        viewModelScope.launch { persistCardioSets(exerciseIndex) }
    }

    /** Removes a cardio set (never the last one, nor the one whose timer is running) and numbers the rest again. */
    fun removeCardioSet(exerciseIndex: Int, setIndex: Int) {
        val state = _state.value
        val ex = state.exercises.getOrNull(exerciseIndex) ?: return
        if (ex.cardioSets.size <= 1) return
        val removed = ex.cardioSets.getOrNull(setIndex) ?: return
        val onTimer = setIndex == ex.activeCardioSetIndex && exerciseIndex == state.currentExerciseIndex &&
            (state.cardioTimerRunning || state.cardioTimerPaused)
        if (onTimer) return
        val remaining = ex.cardioSets.filterIndexed { i, _ -> i != setIndex }.mapIndexed { i, set -> set.copy(setNumber = i + 1) }
        updateCardioSets(exerciseIndex) { remaining }
        viewModelScope.launch {
            removed.id?.let { id ->
                state.sessionId?.let { sessionId ->
                    workoutRepository.deleteCardioLog(
                        CardioLogEntity(id = id, sessionId = sessionId, categoria = ex.exercise.nome, distanza = 0f, durataSecondi = 0, timestamp = 0L)
                    )
                }
            }
            remaining.forEach { set -> set.id?.let { workoutRepository.updateCardioSetNumber(it, set.setNumber) } }
            // A planned set removed before it had a log would otherwise come back with the plan's count
            persistCardioSets(exerciseIndex)
        }
    }

    /** Corrects a cardio set that was already done. */
    fun editCardioSet(exerciseIndex: Int, setIndex: Int, seconds: Int, distanceKm: Float, note: String?) {
        val set = _state.value.exercises.getOrNull(exerciseIndex)?.cardioSets?.getOrNull(setIndex) ?: return
        val cleanNote = note?.trim()?.ifBlank { null }
        updateCardioSets(exerciseIndex) { sets ->
            sets.toMutableList().also { it[setIndex] = set.copy(elapsedSeconds = seconds, distanceKm = distanceKm, note = cleanNote) }
        }
        set.id?.let { id -> viewModelScope.launch { workoutRepository.updateCardioSetValues(id, seconds, distanceKm, cleanNote) } }
    }

    /** The note of a cardio set; one that has no log yet takes it with it when it gets one. */
    fun updateCardioSetNote(exerciseIndex: Int, setIndex: Int, note: String) {
        val set = _state.value.exercises.getOrNull(exerciseIndex)?.cardioSets?.getOrNull(setIndex) ?: return
        val cleanNote = note.trim().ifBlank { null }
        updateCardioSets(exerciseIndex) { sets -> sets.toMutableList().also { it[setIndex] = set.copy(note = cleanNote) } }
        set.id?.let { id -> viewModelScope.launch { workoutRepository.updateCardioNote(id, cleanNote) } }
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
        if (!ex.isTimed) return null
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
                        ordineEsercizio = exState.executionOrder,
                        restTimerSeconds = exState.customRestSeconds,
                        durataSecondi = seconds
                    ).withSnapshot(setState)
                )
            }
        }
        updateSetState(exerciseIndex, setIndex) { it.copy(timeSeconds = seconds) }
    }
}
