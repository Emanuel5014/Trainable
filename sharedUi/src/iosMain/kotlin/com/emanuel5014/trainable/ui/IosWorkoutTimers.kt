@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.domain.emom.EmomClock
import com.emanuel5014.trainable.util.notification.*
import kotlinx.coroutines.flow.MutableSharedFlow
import platform.UIKit.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PAUSED_CARD_MILLIS = 4 * 60 * 60 * 1000L

/** The model owns in-app clocks. iOS owns pre-scheduled alerts, including while the app is suspended. */
@OptIn(ExperimentalUuidApi::class)
internal class IosWorkoutTimers(
    private val scheduler: IosNotificationScheduler = IosNotifications.scheduler,
    private val words: () -> IosNotificationWords? = { IosNotifications.words },
    private val now: () -> Long = ::nowMillis,
    private val live: () -> IosWorkoutLiveActivities? = { IosWorkoutLiveActivityBridge.service },
    private val context: () -> IosLiveTimerContext = { IosLiveTimerContext() }
) : WorkoutTimers {
    override val timerEvents = MutableSharedFlow<TimerAction>(extraBufferCapacity = 1)
    override val warmupTimerEvents = MutableSharedFlow<WarmupTimerAction>(extraBufferCapacity = 1)
    override val emomEvents = MutableSharedFlow<EmomAction>(extraBufferCapacity = 4)
    private var restLiveInstance: String? = null
    private var warmupLiveInstance: String? = null
    private var restScheduled = false
    private var warmupScheduled = false
    private var emomInfo: EmomNotificationInfo? = null
    private var emomIds = emptyList<String>()
    private var emomFinishedScheduled = false
    /** One logical EMOM Live Activity per run; actions from an older run are refused. */
    internal var emomLiveInstance: String? = null
        private set

    override fun hasNotificationPermission() = scheduler.canAuthorize || live()?.isAvailable() == true
    // ActivityKit renders date-based countdowns; it does not need the Android per-second update path.
    override fun isLiveNotificationSupported() = false
    override fun cancelCustomVibration() = Unit // iOS controls notification sound/haptics.
    // The shared model calls these immediately before show*Finished. Keep the pre-scheduled alert: cancelling it
    // can race its delivery, and posting another on resume would duplicate an alert already delivered in background.
    override fun cancelFinishAlarm() = Unit
    override fun cancelWarmupFinishAlarm() = Unit
    override fun cancelTimer() {
        scheduler.cancel(listOf(REST_NOTICE)); restScheduled = false
        IosLiveTimerSnapshots.forget(REST_NOTICE, restLiveInstance)
        live()?.cancel(REST_NOTICE); restLiveInstance = null
    }
    override fun cancelWarmupTimer() {
        scheduler.cancel(listOf(WARMUP_NOTICE)); warmupScheduled = false
        IosLiveTimerSnapshots.forget(WARMUP_NOTICE, warmupLiveInstance)
        live()?.cancel(WARMUP_NOTICE); warmupLiveInstance = null
    }
    override fun cancelEmom() {
        cancelEmomBoundaryAlarm(); emomInfo = null; emomFinishedScheduled = false
        endEmomLive()
    }
    private fun endEmomLive() {
        IosLiveTimerSnapshots.forget(EMOM_NOTICE, emomLiveInstance)
        if (emomLiveInstance != null) live()?.cancel(EMOM_NOTICE)
        emomLiveInstance = null
    }
    override fun cancelEmomBoundaryAlarm() {
        scheduler.cancel(emomIds); emomIds = emptyList(); emomFinishedScheduled = false
    }
    override fun startOrUpdateTimerNotification(remainingSeconds: Int, sessionId: Int, exerciseName: String?,
        nextSetNumber: Int?, nextSetWeight: Float?, nextSetReps: Int?, previousReps: Int?, weightUnit: String?,
        totalSeconds: Int?, scheduleAlarm: Boolean, nextSetRepsLabel: String?, nextSetDetail: String?, nextSetText: String?) {
        if (!scheduleAlarm) return
        val text = words() ?: return
        val detail = text.nextSet(exerciseName, nextSetNumber, nextSetWeight, nextSetReps, previousReps, weightUnit,
            nextSetRepsLabel, nextSetDetail, nextSetText)
        val (exercise, setLine) = text.nextSetParts(exerciseName, nextSetNumber, nextSetWeight, nextSetReps, previousReps,
            weightUnit, nextSetRepsLabel, nextSetDetail, nextSetText)
        val endsAt = now() + remainingSeconds.coerceAtLeast(0) * 1000L
        scheduler.schedule(listOf(IosNotice(REST_NOTICE, text.restTitle,
            text.restBody + if (detail.isEmpty()) "" else " — $detail", endsAt)), prompt = true)
        restScheduled = true
        if (remainingSeconds > 0) {
            val instance = restLiveInstance ?: Uuid.random().toString().also { restLiveInstance = it }
            publish(context().apply(IosLiveTimer(REST_NOTICE, instance, text.restTitle, detail,
                endsAt, (totalSeconds ?: remainingSeconds).coerceAtLeast(remainingSeconds), finishedText = text.restBody,
                exercise = exercise, setLine = setLine), text.skipLabel))
        }
    }
    override fun startOrUpdateWarmupTimerNotification(remainingSeconds: Int, totalSeconds: Int?, scheduleAlarm: Boolean) {
        if (!scheduleAlarm) return
        val text = words() ?: return
        val endsAt = now() + remainingSeconds.coerceAtLeast(0) * 1000L
        scheduler.schedule(listOf(IosNotice(WARMUP_NOTICE, text.warmupTitle, text.warmupBody, endsAt)), prompt = true)
        warmupScheduled = true
        if (remainingSeconds > 0) {
            val instance = warmupLiveInstance ?: Uuid.random().toString().also { warmupLiveInstance = it }
            publish(context().apply(IosLiveTimer(WARMUP_NOTICE, instance, text.warmupTitle, "",
                endsAt, (totalSeconds ?: remainingSeconds).coerceAtLeast(remainingSeconds), finishedText = text.warmupBody), text.skipLabel))
        }
    }
    private fun publish(original: IosLiveTimer) {
        val timer = IosLiveActivityStyle.accentArgb.takeIf { it != 0L }?.let { original.copy(accentArgb = it) } ?: original
        IosLiveTimerSnapshots.remember(timer)
        live()?.takeIf { it.isAvailable() }?.update(timer)
    }

    /** A Live Activity button: catch the clock up first, so DONE/PAUSE act on the minute really on the clock. */
    internal fun sendEmom(action: EmomAction?) {
        emomEvents.tryEmit(EmomAction.BOUNDARY)
        if (action != null) emomEvents.tryEmit(action)
    }

    fun refreshControls() {
        val label = words()?.skipLabel ?: return
        listOf(REST_NOTICE to restLiveInstance, WARMUP_NOTICE to warmupLiveInstance).forEach { (kind, instance) ->
            val current = IosLiveTimerSnapshots.get(kind)?.takeIf { it.instanceId == instance } ?: return@forEach
            val updated = context().apply(current, label)
            if (updated != current) publish(updated)
        }
    }

    override fun showRestFinished() {
        IosLiveTimerSnapshots.forget(REST_NOTICE, restLiveInstance)
        live()?.cancel(REST_NOTICE); restLiveInstance = null
        val text = words() ?: return
        if (!restScheduled) scheduler.schedule(listOf(IosNotice(REST_NOTICE, text.restTitle, text.restBody, now())))
    }
    override fun showWarmupTimerFinished() {
        IosLiveTimerSnapshots.forget(WARMUP_NOTICE, warmupLiveInstance)
        live()?.cancel(WARMUP_NOTICE); warmupLiveInstance = null
        val text = words() ?: return
        if (!warmupScheduled) scheduler.schedule(listOf(IosNotice(WARMUP_NOTICE, text.warmupTitle, text.warmupBody, now())))
    }
    override fun showEmomNotification(info: EmomNotificationInfo) {
        emomInfo = info
        if (info.countdownEndsAt == null) cancelEmomBoundaryAlarm()
        publishEmom(info)
    }

    /**
     * The card shows the round, a countdown to the next minute and the time left in the whole run. iOS runs no
     * app code while suspended, so a new round only reaches the card when the lifter taps a button or reopens
     * the app; the run-end countdown stays right meanwhile (the minute alerts are scheduled separately).
     */
    private fun publishEmom(info: EmomNotificationInfo) {
        val text = words() ?: return
        val instance = emomLiveInstance ?: Uuid.random().toString().also { emomLiveInstance = it }
        val ctx = context()
        val keys = info.actions.map { "emom." + it.action.name.lowercase() }
        val labels = info.actions.map { it.label }
        val roundEnd = info.countdownEndsAt
        val base = IosLiveTimer(EMOM_NOTICE, instance, info.title, listOfNotNull(info.text, info.detail).joinToString(" · "),
            0, 0, sessionId = ctx.sessionId, sessionStartedAtMillis = ctx.sessionStartedAtMillis,
            showAdd = false, showSubtract = false, showSkip = false, skipLabel = "", finishedText = text.emomFinished,
            exercise = info.text, setLine = info.detail ?: "", actionKeys = keys, actionLabels = labels)
        val timer = if (roundEnd == null) {
            // Paused: nothing counts; keep the card for a long while, the app replaces it on resume or stop.
            base.copy(endsAtMillis = now() + PAUSED_CARD_MILLIS, totalSeconds = (PAUSED_CARD_MILLIS / 1000).toInt(), paused = true)
        } else {
            val leadIn = info.title == text.emomReady
            val nextClock = if (leadIn) 0 else EmomClock.nextRoundAt(info.runElapsedSeconds)
            val clockZero = roundEnd - nextClock * 1000L
            val runStart = clockZero - EmomClock.LEAD_IN_SECONDS * 1000L
            val runEnd = clockZero + info.rounds * EmomClock.MINUTE_SECONDS * 1000L
            val roundLength = if (leadIn) EmomClock.LEAD_IN_SECONDS else EmomClock.MINUTE_SECONDS
            base.copy(endsAtMillis = runEnd, totalSeconds = ((runEnd - runStart) / 1000L).toInt(),
                roundStartsAtMillis = roundEnd - roundLength * 1000L, roundEndsAtMillis = roundEnd)
        }
        publish(timer)
    }
    override fun scheduleEmomBoundaryAlarm(triggerAtMillis: Long) {
        val info = emomInfo ?: return
        val text = words() ?: return
        cancelEmomBoundaryAlarm()
        // Round zero is the lead-in; a run of N rounds finishes at N*60 seconds. Reserve slots for rest,
        // warmup, membership and completion within iOS's pending-notification budget (no Live Activity yet).
        val firstRound = if (info.title == text.emomReady) 0 else EmomClock.nextRoundAt(info.runElapsedSeconds) / 60
        val boundaries = (firstRound..info.rounds.coerceAtMost(firstRound + 58)).toList() +
            if (info.rounds >= firstRound + 59) listOf(info.rounds) else emptyList()
        val notices = boundaries.mapIndexed { index, round ->
            IosNotice("workout.emom.$index",
                if (round == info.rounds) text.emomFinished else text.round(round + 1, info.rounds),
                if (round == firstRound) info.text else "", triggerAtMillis + (round - firstRound) * 60_000L)
        }
        emomIds = notices.map { it.id }
        emomFinishedScheduled = boundaries.lastOrNull() == info.rounds
        scheduler.schedule(notices, prompt = true)
    }
    override fun showEmomFinished(title: String, text: String) {
        endEmomLive()
        // The final alert was scheduled with all boundaries, so it also works after multiple suspended rounds.
        if (!emomFinishedScheduled) scheduler.schedule(listOf(IosNotice("workout.emom.0", title, text, now())))
    }
    override fun vibrateEmomCue(cue: EmomCue) {
        when (cue) {
            EmomCue.TICK -> UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
            EmomCue.GO -> UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy).impactOccurred()
            EmomCue.DONE -> UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
        }
    }
}

/**
 * A deleted workout takes its countdowns with it, also when its screen is gone and nothing ticks them any more:
 * the alerts this process scheduled for it, and (in Swift, which also knows cards from before a relaunch) its
 * Live Activities with their pending alerts.
 */
internal object IosWorkoutSessionEnd {
    fun deleted(sessionId: Int, scheduler: IosNotificationScheduler = IosNotifications.scheduler,
        live: IosWorkoutLiveActivities? = IosWorkoutLiveActivityBridge.service) {
        val kinds = IosLiveTimerSnapshots.forgetSession(sessionId)
        val ids = kinds.flatMap { kind -> if (kind == EMOM_NOTICE) (0..59).map { "$EMOM_NOTICE.$it" } else listOf(kind) }
        if (ids.isNotEmpty()) scheduler.cancel(ids)
        live?.cancelSession(sessionId)
    }
}
