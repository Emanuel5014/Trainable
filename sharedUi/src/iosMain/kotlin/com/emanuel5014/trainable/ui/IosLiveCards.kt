package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.util.notification.LiveSegment
import com.emanuel5014.trainable.util.notification.WorkoutLiveCard

/** The one Live Activity of the whole workout ("workout live card" setting). */
internal const val LIVE_NOTICE = "workout.live"

/** "Set done" on the live card during the set. */
internal const val LIVE_NEXT_ACTION = "live.next"

/** What the live card is showing, as the widget reads it ([IosLiveTimer.liveMode]). */
internal object LiveModes {
    const val SET = "set"
    const val CLOCK = "clock"
    const val STOPWATCH = "stopwatch"
    const val REST = "rest"
    const val WARMUP = "warmup"
    const val EMOM = "emom"
    const val DONE = "done"
}

/** The live card's words in the app language, captured in composition like [IosNotificationWords]. */
internal data class IosLiveCardWords(
    val setTitle: String = "%1\$s · set %2\$d of %3\$d",
    val lastTime: String = "last time %1\$d",
    val exerciseOf: String = "Exercise %1\$d of %2\$d",
    val then: String = "then %1\$s",
    val next: String = "Next: %1\$s",
    val nextExercise: String = "Next exercise: %1\$s",
    val allDone: String = "All sets done",
    val volume: String = "Volume %1\$s",
    val superset: String = "Superset",
    val exercisesDone: String = "%1\$d of %2\$d exercises",
    val setOne: String = "%d set",
    val setMany: String = "%d sets",
    val exerciseOne: String = "%d exercise",
    val exerciseMany: String = "%d exercises",
    val quickWorkout: String = "Quick Workout",
    val cardioWorkout: String = "Cardio Workout",
    val setDone: String = "Set done"
) {
    fun fill(template: String, vararg args: Any): String =
        Regex("%(\\d+\\$)?[sd]").let { pattern ->
            var next = 0
            pattern.replace(template) { match ->
                val index = match.groupValues[1].removeSuffix("$").toIntOrNull()?.minus(1) ?: next++
                args.getOrNull(index)?.toString() ?: match.value
            }
        }
    fun sets(n: Int) = fill(if (n == 1) setOne else setMany, n)
    fun exercises(n: Int) = fill(if (n == 1) exerciseOne else exerciseMany, n)
}

/**
 * Sits in front of the Swift Live Activity service. While the open workout sends a live card ([showBase]) the rest,
 * warm-up and EMOM cards the timers publish are not shown on their own: they are folded into one `workout.live`
 * activity with the sets part of the workout. The timer code and its button handling stay as they are; a button of
 * the live card is routed back to the timer card it shows (Swift sends the timer's kind and instance).
 * Without a live card everything passes straight through.
 */
internal class IosLiveCardComposer(
    private val target: IosWorkoutLiveActivities,
    private val words: () -> IosLiveCardWords? = { IosNotifications.liveWords },
    private val now: () -> Long = ::nowMillis
) : IosWorkoutLiveActivities {
    private val timers = mutableMapOf<String, IosLiveTimer>()
    private var base: WorkoutLiveCard? = null
    /** A "Set done" press found no open workout: the card says to open the app until the workout speaks again. */
    private var needsApp = false
    /** The live card last sent, for tests and for a press to check what it showed. */
    var shown: IosLiveTimer? = null
        private set

    override fun isAvailable(): Boolean = target.isAvailable()

    override fun update(timer: IosLiveTimer) {
        if (timer.kind == LIVE_NOTICE) { target.update(timer); return }
        timers[timer.kind] = timer
        if (base != null) publish() else target.update(timer)
    }

    override fun cancel(kind: String) {
        if (kind == LIVE_NOTICE) { target.cancel(kind); return }
        timers.remove(kind)
        if (base != null) publish() else target.cancel(kind)
    }

    override fun cancelAll() {
        timers.clear(); base = null; shown = null
        target.cancelAll()
    }

    override fun cancelSession(sessionId: Int) {
        timers.values.removeAll { it.sessionId == sessionId }
        if (base?.sessionId == sessionId) { base = null; shown = null }
        target.cancelSession(sessionId)
    }

    /** What the open workout says of its sets now; the first one takes the place of the timer cards. */
    fun showBase(card: WorkoutLiveCard) {
        val first = base == null
        base = card
        needsApp = false
        if (first) timers.keys.forEach { target.cancel(it) }
        publish()
    }

    /** The workout [sessionId] is over or the setting went off: the live card goes, the timer cards come back. */
    fun endBase(sessionId: Int) {
        if (base?.sessionId != sessionId) return
        base = null; shown = null
        target.cancel(LIVE_NOTICE)
        timers.values.forEach { target.update(it) }
    }

    /** "Set done" reached no open workout: no button until the workout is back, and the hint to open the app. */
    fun needingApp() {
        needsApp = true
        publish()
    }

    val isLive: Boolean get() = base != null

    private fun publish() {
        val card = base ?: return
        val text = words() ?: IosLiveCardWords()
        val live = compose(card, text)
        shown = live
        if (target.isAvailable()) target.update(live)
    }

    private fun compose(card: WorkoutLiveCard, text: IosLiveCardWords): IosLiveTimer {
        val emom = timers[EMOM_NOTICE]
        val warmup = timers[WARMUP_NOTICE]
        val rest = timers[REST_NOTICE]
        val mode = when {
            emom != null -> LiveModes.EMOM
            warmup != null -> LiveModes.WARMUP
            card.allDone -> LiveModes.DONE
            rest != null -> LiveModes.REST
            card.clockStartedAtMillis != null && card.clockEndsAtMillis != null -> LiveModes.CLOCK
            card.clockStartedAtMillis != null -> LiveModes.STOPWATCH
            else -> LiveModes.SET
        }
        val id = "live-${card.sessionId}-${card.startedAtMillis}"
        // Live Activities end after 8 hours anyway; the card is wanted until the workout ends
        val until = maxOf(card.startedAtMillis + 12 * HOUR, now() + HOUR)
        val setTitle = card.exerciseName?.let { name ->
            val title = card.setNumber?.let { text.fill(text.setTitle, name, it, card.setsInExercise) } ?: name
            if (card.inSuperset) "${text.superset} · $title" else title
        }.orEmpty()
        val upcoming = when {
            card.exerciseName == null -> ""
            card.startsExercise -> text.fill(text.nextExercise, card.exerciseName!!)
            else -> text.fill(text.next, setTitle)
        }
        val side = when {
            card.allDone -> text.fill(text.exercisesDone, card.doneExercises, card.exerciseCount)
            card.exerciseNumber > 0 -> text.fill(text.exerciseOf, card.exerciseNumber, card.exerciseCount)
            else -> ""
        }
        val (liveValue, liveTitle, liveDetail) = when (mode) {
            LiveModes.REST, LiveModes.WARMUP -> Triple("", upcoming, card.value.orEmpty())
            LiveModes.DONE -> Triple(text.allDone, "${text.sets(card.doneSets)} · ${text.exercises(card.doneExercises)}",
                card.volume?.let { text.fill(text.volume, it) }.orEmpty())
            LiveModes.EMOM -> Triple("", emom?.exercise?.ifEmpty { emom.title } ?: setTitle, emom?.setLine.orEmpty())
            else -> Triple(card.value.orEmpty(), setTitle, listOfNotNull(
                card.previousReps?.let { text.fill(text.lastTime, it) }, card.detail,
                card.nextExerciseName?.let { text.fill(text.then, it) }).joinToString(" · "))
        }
        val segments = card.segments.map { when (it) { LiveSegment.TODO -> 0; LiveSegment.CURRENT -> 1; LiveSegment.DONE -> 2 } }
        val workoutName = card.workoutName ?: if (card.cardioOnly) text.cardioWorkout else text.quickWorkout
        val source = when (mode) { LiveModes.EMOM -> emom; LiveModes.WARMUP -> warmup; LiveModes.REST -> rest; else -> null }
        if (source != null) {
            return source.copy(kind = LIVE_NOTICE, instanceId = id, liveMode = mode, workoutName = workoutName,
                liveValue = liveValue, liveTitle = liveTitle, liveDetail = liveDetail, liveSide = side, segments = segments,
                workoutStartedAtMillis = card.startedAtMillis, liveUntilMillis = until, timerKind = source.kind,
                timerInstanceId = source.instanceId, compactText = "")
        }
        val start = when (mode) { LiveModes.CLOCK, LiveModes.STOPWATCH -> card.clockStartedAtMillis!!; else -> card.startedAtMillis }
        val end = if (mode == LiveModes.CLOCK) card.clockEndsAtMillis!! else until
        val setDone = card.setDoneToken.takeIf { mode == LiveModes.SET && !needsApp }
        return IosLiveTimer(LIVE_NOTICE, id, workoutName, liveDetail, end, ((end - start) / 1000).toInt().coerceAtLeast(1),
            sessionId = card.sessionId, sessionStartedAtMillis = card.startedAtMillis,
            showAdd = false, showSubtract = false, showSkip = false, skipLabel = "",
            accentArgb = IosLiveActivityStyle.accentArgb,
            actionKeys = if (setDone != null) listOf(LIVE_NEXT_ACTION) else emptyList(),
            actionLabels = if (setDone != null) listOf(text.setDone) else emptyList(),
            needsApp = needsApp && mode == LiveModes.SET, openAppText = IosNotifications.words?.openAppText.orEmpty(),
            nextSetToken = setDone.orEmpty(),
            liveMode = mode, workoutName = workoutName, liveValue = liveValue, liveTitle = liveTitle, liveDetail = liveDetail,
            liveSide = side, segments = segments, workoutStartedAtMillis = card.startedAtMillis, liveUntilMillis = until,
            compactText = card.setNumber?.takeIf { mode == LiveModes.SET }?.let { "$it/${card.setsInExercise}" }.orEmpty())
    }

    private companion object {
        const val HOUR = 60 * 60 * 1000L
    }
}
