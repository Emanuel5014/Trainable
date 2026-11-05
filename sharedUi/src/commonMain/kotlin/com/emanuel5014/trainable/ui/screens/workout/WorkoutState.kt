package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.data.local.entity.straightReps
import com.emanuel5014.trainable.data.local.entity.parts
import com.emanuel5014.trainable.data.local.entity.pauses
import com.emanuel5014.trainable.data.local.entity.performed
import com.emanuel5014.trainable.domain.prescription.SetPerformance
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SessionPrescriptionEditEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.domain.journal.SessionJournal
import com.emanuel5014.trainable.data.local.entity.repsRange
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.domain.prescription.DoubleProgression
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.prescription.PlannedSet
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.RepMode
import com.emanuel5014.trainable.domain.prescription.RepRange
import com.emanuel5014.trainable.domain.prescription.RepsTarget
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.util.WeightUnitConverter
import com.emanuel5014.trainable.util.PlateCalculator
import com.emanuel5014.trainable.util.RestSeconds
import com.emanuel5014.trainable.util.TimerAdjustment

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
    /** The workout journal is on: finishing a workout can carry a rating, tags and a note. */
    val journalEnabled: Boolean = false,
    /** With the journal on: finishing a workout opens the journal sheet instead of the plain dialog. */
    val journalPromptAtFinish: Boolean = true,
    /**
     * What the lifter has put in the finish sheet so far. It lives here, not in the sheet, so cancelling the sheet keeps
     * it and the "new 1RM?" review that can come between Finish and the end of the session cannot lose it.
     */
    val journalDraft: SessionJournal = SessionJournal(),
    /** Plates the gym has, in [weightUnit], heaviest first. */
    val availablePlates: List<Float> = PlateCalculator.DEFAULT_PLATES_KG,
    val warmupTimerEnabled: Boolean = false,
    val warmupTimerRemaining: Int = 0,
    val warmupTimerEndTime: Long? = null,
    val warmupTimerTotalSeconds: Int = 0,
    /** Off: no countdown after a set. The pause of a mini-set is part of the technique and still runs. */
    val restTimerEnabled: Boolean = true,
    /** The warmup the lifter started last: the warmup slider opens on it. */
    val lastWarmupSeconds: Int = RestSeconds.DEFAULT,
    /** Next real execution order to hand out, see [WorkoutExerciseState.executionOrder]. */
    val nextExecutionOrder: Int = 0,
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
    /** Whether the workout asks how hard each set felt, and on which scale. */
    val rpeEnabled: Boolean = false,
    val rpeScale: RpeScale = RpeScale.BASIC,
    /** Pending "new 1RM?" prompts shown before the session is closed. */
    val oneRepMaxSuggestions: List<OneRepMaxSuggestion> = emptyList(),
    /** The set just logged came out short of its reps: the rest card offers to finish it after a short pause. */
    val miniSetOffer: MiniSetOffer? = null,
    /** A mini-set on its way: its pause runs on the rest timer, then the hub asks how many reps it had. */
    val miniSet: MiniSetRun? = null,
    /** Exercises of the routine the user deleted from the exercise list, to ask once if they are added back. */
    val deletedExercises: List<ExerciseEntity> = emptyList()
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
const val PROVISIONAL_ORDER_BASE = 10_000

const val MINI_SESSION = "session_id"
const val MINI_SET = "set_id"
const val MINI_MISSING = "missing"
const val MINI_STARTED = "started_at"
const val MINI_ENDS = "ends_at"
const val MINI_PAUSE = "pause_seconds"

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
    val progressionDismissed: Boolean = false,
    /** Changes made to this exercise's prescription during the workout, oldest first. */
    val programEdits: List<SessionPrescriptionEditEntity> = emptyList()
) {
    val isAdvanced: Boolean get() = blocks.isNotEmpty()

    /** The last change made to the prescription, null while it is as the routine has it. */
    val lastProgramEdit: SessionPrescriptionEditEntity? get() = programEdits.lastOrNull()

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
    val note: String? = null,
    /** How hard the set felt (RPE), once it is done. */
    val rpe: Float? = null
)

/** [count] cardio sets still to do, numbered from 1. */
fun pendingCardioSets(count: Int): List<CardioSetState> =
    (1..count.coerceAtLeast(1)).map { CardioSetState(setNumber = it) }

/**
 * The sets of a cardio exercise from its logs. Once the exercise has a log every set of it has one, so the logs are all
 * there is; an exercise with none yet has the [plannedSets] of the plan.
 */
fun cardioSetsFrom(logs: List<CardioLogEntity>, plannedSets: Int): List<CardioSetState> {
    val logged = logs.sortedWith(compareBy({ it.numeroSerie }, { it.id })).mapIndexed { i, log ->
        CardioSetState(
            id = log.id,
            setNumber = i + 1,
            elapsedSeconds = log.durataSecondi,
            distanceKm = log.distanza,
            isCompleted = log.isCompleted,
            note = log.note,
            rpe = log.rpe
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
