@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.domain.emom.EmomClock
import com.emanuel5014.trainable.util.notification.*
import kotlinx.coroutines.flow.MutableSharedFlow
import platform.UIKit.*

/** The model owns in-app clocks. iOS owns pre-scheduled alerts, including while the app is suspended. */
internal class IosWorkoutTimers(
    private val scheduler: IosNotificationScheduler = IosNotifications.scheduler,
    private val words: () -> IosNotificationWords? = { IosNotifications.words },
    private val now: () -> Long = ::nowMillis
) : WorkoutTimers {
    override val timerEvents = MutableSharedFlow<TimerAction>(extraBufferCapacity = 1)
    override val warmupTimerEvents = MutableSharedFlow<WarmupTimerAction>(extraBufferCapacity = 1)
    override val emomEvents = MutableSharedFlow<EmomAction>(extraBufferCapacity = 4)
    private var restScheduled = false
    private var warmupScheduled = false
    private var emomInfo: EmomNotificationInfo? = null
    private var emomIds = emptyList<String>()
    private var emomFinishedScheduled = false

    override fun hasNotificationPermission() = scheduler.canAuthorize
    override fun isLiveNotificationSupported() = false
    override fun cancelCustomVibration() = Unit // iOS controls notification sound/haptics.
    // The shared model calls these immediately before show*Finished. Keep the pre-scheduled alert: cancelling it
    // can race its delivery, and posting another on resume would duplicate an alert already delivered in background.
    override fun cancelFinishAlarm() = Unit
    override fun cancelWarmupFinishAlarm() = Unit
    override fun cancelTimer() { scheduler.cancel(listOf(REST_NOTICE)); restScheduled = false }
    override fun cancelWarmupTimer() { scheduler.cancel(listOf(WARMUP_NOTICE)); warmupScheduled = false }
    override fun cancelEmom() { cancelEmomBoundaryAlarm(); emomInfo = null; emomFinishedScheduled = false }
    override fun cancelEmomBoundaryAlarm() {
        scheduler.cancel(emomIds); emomIds = emptyList(); emomFinishedScheduled = false
    }
    override fun startOrUpdateTimerNotification(remainingSeconds: Int, sessionId: Int, exerciseName: String?,
        nextSetNumber: Int?, nextSetWeight: Float?, nextSetReps: Int?, previousReps: Int?, weightUnit: String?,
        totalSeconds: Int?, scheduleAlarm: Boolean, nextSetRepsLabel: String?, nextSetDetail: String?, nextSetText: String?) {
        if (!scheduleAlarm) return
        val text = words() ?: return
        val detail = listOfNotNull(exerciseName, nextSetText ?: nextSetDetail ?: nextSetRepsLabel).joinToString(" · ")
        scheduler.schedule(listOf(IosNotice(REST_NOTICE, text.restTitle,
            text.restBody + if (detail.isEmpty()) "" else " — $detail", now() + remainingSeconds.coerceAtLeast(0) * 1000L)), prompt = true)
        restScheduled = true
    }
    override fun startOrUpdateWarmupTimerNotification(remainingSeconds: Int, totalSeconds: Int?, scheduleAlarm: Boolean) {
        if (!scheduleAlarm) return
        val text = words() ?: return
        scheduler.schedule(listOf(IosNotice(WARMUP_NOTICE, text.warmupTitle, text.warmupBody,
            now() + remainingSeconds.coerceAtLeast(0) * 1000L)), prompt = true)
        warmupScheduled = true
    }
    override fun showRestFinished() {
        val text = words() ?: return
        if (!restScheduled) scheduler.schedule(listOf(IosNotice(REST_NOTICE, text.restTitle, text.restBody, now())))
    }
    override fun showWarmupTimerFinished() {
        val text = words() ?: return
        if (!warmupScheduled) scheduler.schedule(listOf(IosNotice(WARMUP_NOTICE, text.warmupTitle, text.warmupBody, now())))
    }
    override fun showEmomNotification(info: EmomNotificationInfo) {
        emomInfo = info
        if (info.countdownEndsAt == null) cancelEmomBoundaryAlarm()
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
