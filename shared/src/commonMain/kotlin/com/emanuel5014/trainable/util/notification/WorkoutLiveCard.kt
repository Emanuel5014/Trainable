package com.emanuel5014.trainable.util.notification

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** How one piece of the live card's bar is drawn: a set done, the one to do now, one still to come. */
@Serializable
enum class LiveSegment { DONE, CURRENT, TODO }

/** What kind of set the live card announces: each one shows its value in its own way. */
@Serializable
enum class LiveSetKind { STRENGTH, TIMED, CARDIO, EMOM }

/**
 * What the open workout tells the live card (one card for the whole workout, it takes the place of the rest, warm-up
 * and EMOM cards): the set to do now, the bar of its exercise and where the workout stands.
 *
 * The timers are not in here. Each platform already gets them through [WorkoutTimers] and adds them itself
 * ([liveModeOf], [liveButtonsOf]), so a button pressed while the workout is closed still redraws the card.
 * Kept as JSON by the platform so a card can be redrawn after the process was recreated.
 */
@Serializable
data class WorkoutLiveCard(
    val sessionId: Int,
    /** The workout's own name or its routine's; null for a quick workout without a name ([cardioOnly] says which). */
    val workoutName: String? = null,
    val cardioOnly: Boolean = false,
    /** When the workout started, for the clock that counts the whole workout. */
    val startedAtMillis: Long,
    /** The exercise of the set to do; null once every set is done. */
    val exerciseName: String? = null,
    /** 1-based place of that exercise in the workout, and how many exercises the workout has. */
    val exerciseNumber: Int = 0,
    val exerciseCount: Int = 0,
    val setNumber: Int? = null,
    val setsInExercise: Int = 0,
    /** What the set asks, already written ("80 kg × 8", "45s · 20 reps", "20 min · 3 km"). */
    val value: String? = null,
    /** Reps of the same set last time, only when they differ from the ones asked. */
    val previousReps: Int? = null,
    /** Extra words of an advanced prescription ("75% · Pause 2″"). */
    val detail: String? = null,
    /** The exercise that comes after the one of [exerciseName]; null for the last one. */
    val nextExerciseName: String? = null,
    /** The set to do is the first of its exercise: a rest before it leads to a new exercise. */
    val startsExercise: Boolean = false,
    /** The exercise is part of a superset: the bar holds every set of the block, in the order they are done. */
    val inSuperset: Boolean = false,
    val kind: LiveSetKind = LiveSetKind.STRENGTH,
    /** One piece per set of the exercise (of the block, in a superset); once all is done, one per exercise. */
    val segments: List<LiveSegment> = emptyList(),
    /** [NextSetKey.token] of the set "Set done" logs from the card during the set; null hides the button. */
    val setDoneToken: String? = null,
    /** A timed or cardio set on its clock: when it started (time paused taken out) and, with a goal, when it ends. */
    val clockStartedAtMillis: Long? = null,
    val clockEndsAtMillis: Long? = null,
    val allDone: Boolean = false,
    val doneSets: Int = 0,
    val doneExercises: Int = 0,
    /** Volume of the sets done, already written in the user's unit; null when there is none. */
    val volume: String? = null
) {
    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): WorkoutLiveCard? =
            text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
    }
}

/** The timers the platform knows of, as its [WorkoutTimers] calls left them. */
data class LiveTimers(
    val restEndsAtMillis: Long? = null,
    val restTotalSeconds: Int = 0,
    /** The rest ran out and its "finished" state is still on (until a button, a tick or a new rest). */
    val restFinished: Boolean = false,
    val warmupEndsAtMillis: Long? = null,
    val warmupTotalSeconds: Int = 0,
    val warmupFinished: Boolean = false,
    val emom: EmomNotificationInfo? = null,
    /** Title and text of an EMOM run that just ended. */
    val emomFinishedTitle: String? = null,
    val emomFinishedText: String? = null
)

/** What the live card shows now. */
enum class LiveMode { SET, CLOCK, REST, REST_FINISHED, WARMUP, WARMUP_FINISHED, EMOM, EMOM_FINISHED, ALL_DONE }

/**
 * One countdown at a time wins over the set; a run that is on (EMOM, warm-up) wins over the rest.
 * Once every set is done the rest no longer matters: the card says the workout is done.
 */
fun liveModeOf(card: WorkoutLiveCard, timers: LiveTimers): LiveMode = when {
    timers.emom != null -> LiveMode.EMOM
    timers.warmupEndsAtMillis != null -> LiveMode.WARMUP
    card.allDone -> LiveMode.ALL_DONE
    timers.restEndsAtMillis != null -> LiveMode.REST
    timers.emomFinishedTitle != null -> LiveMode.EMOM_FINISHED
    timers.warmupFinished -> LiveMode.WARMUP_FINISHED
    timers.restFinished -> LiveMode.REST_FINISHED
    card.clockStartedAtMillis != null -> LiveMode.CLOCK
    else -> LiveMode.SET
}

/** A button of the live card. EMOM_ACTIONS stands for the EMOM run's own buttons, in their order. */
enum class LiveButton {
    SET_DONE, REST_SUBTRACT, REST_ADD, REST_SKIP, REST_MORE, REST_NEXT_SET, REST_DISMISS,
    WARMUP_SUBTRACT, WARMUP_ADD, WARMUP_SKIP, WARMUP_DISMISS, EMOM_ACTIONS, FINISH
}

/** The timer buttons the Workout settings allow (the same switches as the rest and warm-up cards). */
data class LiveButtonSettings(
    val showTimeButtons: Boolean = true,
    val addEnabled: Boolean = true,
    val subtractEnabled: Boolean = true,
    val showSkip: Boolean = true
)

/**
 * The buttons of the card in [mode], in the order they are drawn. [restNextSetKey] is the set the finished-rest
 * "Set done" logs (null hides it, as on the rest card); [needsApp] hides the buttons that only act through the open
 * workout, after one of them found it closed.
 */
fun liveButtonsOf(
    mode: LiveMode,
    card: WorkoutLiveCard,
    settings: LiveButtonSettings,
    restNextSetKey: NextSetKey?,
    needsApp: Boolean = false
): List<LiveButton> {
    val time = settings.showTimeButtons
    return when (mode) {
        LiveMode.SET -> listOfNotNull(LiveButton.SET_DONE.takeIf { card.setDoneToken != null && !needsApp })
        LiveMode.CLOCK, LiveMode.EMOM_FINISHED -> emptyList()
        LiveMode.REST -> listOfNotNull(
            LiveButton.REST_SUBTRACT.takeIf { time && settings.subtractEnabled },
            LiveButton.REST_ADD.takeIf { time && settings.addEnabled },
            LiveButton.REST_SKIP.takeIf { settings.showSkip }
        )
        LiveMode.REST_FINISHED -> if (needsApp) listOf(LiveButton.REST_DISMISS) else listOfNotNull(
            LiveButton.REST_MORE.takeIf { time && settings.addEnabled },
            LiveButton.REST_NEXT_SET.takeIf { restNextSetKey != null },
            LiveButton.REST_DISMISS
        )
        LiveMode.WARMUP -> listOfNotNull(
            LiveButton.WARMUP_SUBTRACT.takeIf { time && settings.subtractEnabled },
            LiveButton.WARMUP_ADD.takeIf { time && settings.addEnabled },
            LiveButton.WARMUP_SKIP.takeIf { settings.showSkip }
        )
        LiveMode.WARMUP_FINISHED -> listOf(LiveButton.WARMUP_DISMISS)
        LiveMode.EMOM -> listOf(LiveButton.EMOM_ACTIONS)
        LiveMode.ALL_DONE -> listOf(LiveButton.FINISH)
    }
}

/**
 * The bar of the card in [mode]: the sets of the exercise, or during an EMOM run one piece per round
 * (rounds already over done, the one on the clock current).
 */
fun liveSegmentsOf(mode: LiveMode, card: WorkoutLiveCard, timers: LiveTimers): List<LiveSegment> {
    val emom = timers.emom
    if (mode != LiveMode.EMOM || emom == null) return card.segments
    val rounds = emom.rounds.coerceAtLeast(1)
    val done = (emom.runElapsedSeconds / com.emanuel5014.trainable.domain.emom.EmomClock.MINUTE_SECONDS).coerceIn(0, rounds)
    return List(rounds) { i ->
        when {
            i < done -> LiveSegment.DONE
            i == done -> LiveSegment.CURRENT
            else -> LiveSegment.TODO
        }
    }
}
