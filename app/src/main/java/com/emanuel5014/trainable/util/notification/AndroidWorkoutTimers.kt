package com.emanuel5014.trainable.util.notification

/** Forwards to the existing Android implementation without changing notification behavior. */
class AndroidWorkoutTimers(private val helper: TimerNotificationHelper) : WorkoutTimers {
    override val timerEvents get() = TimerNotificationReceiver.timerEvents
    override val warmupTimerEvents get() = TimerNotificationReceiver.warmupTimerEvents
    override val emomEvents get() = TimerNotificationReceiver.emomEvents

    override fun cancelCustomVibration() =
        helper.cancelCustomVibration()

    override fun cancelEmom() =
        helper.cancelEmom()

    override fun cancelEmomBoundaryAlarm() =
        helper.cancelEmomBoundaryAlarm()

    override fun cancelFinishAlarm() =
        helper.cancelFinishAlarm()

    override fun cancelTimer() =
        helper.cancelTimer()

    override fun cancelWarmupFinishAlarm() =
        helper.cancelWarmupFinishAlarm()

    override fun cancelWarmupTimer() =
        helper.cancelWarmupTimer()

    override fun hasNotificationPermission(): Boolean =
        helper.hasNotificationPermission()

    override fun isLiveNotificationSupported(): Boolean =
        helper.isLiveNotificationSupported()

    override fun scheduleEmomBoundaryAlarm(triggerAtMillis: Long) =
        helper.scheduleEmomBoundaryAlarm(triggerAtMillis)

    override fun showEmomFinished(title: String, text: String) =
        helper.showEmomFinished(title, text)

    override fun showEmomNotification(info: EmomNotificationInfo) =
        helper.showEmomNotification(info)

    override fun showRestFinished(next: RestNextSet?) =
        helper.showRestFinished(next)

    override fun showWarmupTimerFinished() =
        helper.showWarmupTimerFinished()

    override fun startOrUpdateTimerNotification(
        remainingSeconds: Int,
        sessionId: Int,
        exerciseName: String?,
        nextSetNumber: Int?,
        nextSetWeight: Float?,
        nextSetReps: Int?,
        previousReps: Int?,
        weightUnit: String?,
        totalSeconds: Int?,
        scheduleAlarm: Boolean,
        nextSetRepsLabel: String?,
        nextSetDetail: String?,
        nextSetText: String?,
        nextSetKey: NextSetKey?
    ) =
        helper.startOrUpdateTimerNotification(
            remainingSeconds = remainingSeconds,
            sessionId = sessionId,
            exerciseName = exerciseName,
            nextSetNumber = nextSetNumber,
            nextSetWeight = nextSetWeight,
            nextSetReps = nextSetReps,
            previousReps = previousReps,
            weightUnit = weightUnit,
            totalSeconds = totalSeconds,
            scheduleAlarm = scheduleAlarm,
            nextSetRepsLabel = nextSetRepsLabel,
            nextSetDetail = nextSetDetail,
            nextSetText = nextSetText,
            nextSetKey = nextSetKey
        )

    override fun startOrUpdateWarmupTimerNotification(
        remainingSeconds: Int,
        totalSeconds: Int?,
        scheduleAlarm: Boolean
    ) =
        helper.startOrUpdateWarmupTimerNotification(remainingSeconds, totalSeconds, scheduleAlarm)

    override fun vibrateEmomCue(cue: EmomCue) =
        helper.vibrateEmomCue(cue)
}
