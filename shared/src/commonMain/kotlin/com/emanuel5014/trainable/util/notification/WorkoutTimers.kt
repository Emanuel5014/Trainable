package com.emanuel5014.trainable.util.notification

import kotlinx.coroutines.flow.SharedFlow

enum class TimerAction { SKIP, ADD, SUBTRACT, DISMISS, FINISHED }
enum class WarmupTimerAction { SKIP, ADD, SUBTRACT, DISMISS, FINISHED }

/** What the EMOM notification or its round alarm asks of the workout. */
enum class EmomAction { BOUNDARY, DONE, PAUSE, RESUME, STOP }

/** Haptic signals before a round, at its start, and at the end of a run. */
enum class EmomCue { TICK, GO, DONE }

/** One button of the EMOM notification; [label] is already in the app language. */
data class EmomNotificationAction(val action: EmomAction, val label: String)

/** What the ongoing EMOM notification shows, all text already in the app language. */
data class EmomNotificationInfo(
    val title: String,
    val text: String,
    /** Extra line for the expanded notification where the system has no richer layout. */
    val detail: String?,
    /** Epoch millis at which the countdown reaches zero; null while the run is paused. */
    val countdownEndsAt: Long?,
    /** Seconds of the whole run already elapsed, for the live progress bar. */
    val runElapsedSeconds: Int,
    val rounds: Int,
    val actions: List<EmomNotificationAction>
)

/** Platform notifications, alarms and haptics used by the workout's in-app timers.
 * Implementations expose notification actions without owning the workout coroutine scope.
 */
interface WorkoutTimers {
    val timerEvents: SharedFlow<TimerAction>
    val warmupTimerEvents: SharedFlow<WarmupTimerAction>
    val emomEvents: SharedFlow<EmomAction>

    fun cancelCustomVibration()

    fun cancelEmom()

    fun cancelEmomBoundaryAlarm()

    fun cancelFinishAlarm()

    fun cancelTimer()

    fun cancelWarmupFinishAlarm()

    fun cancelWarmupTimer()

    fun hasNotificationPermission(): Boolean

    fun isLiveNotificationSupported(): Boolean

    fun scheduleEmomBoundaryAlarm(triggerAtMillis: Long)

    fun showEmomFinished(title: String, text: String)

    fun showEmomNotification(info: EmomNotificationInfo)

    fun showRestFinished()

    fun showWarmupTimerFinished()

    fun startOrUpdateTimerNotification(
        remainingSeconds: Int,
        sessionId: Int,
        exerciseName: String? = null,
        nextSetNumber: Int? = null,
        nextSetWeight: Float? = null,
        nextSetReps: Int? = null,
        previousReps: Int? = null,
        weightUnit: String? = null,
        totalSeconds: Int? = null,
        scheduleAlarm: Boolean = true,
        nextSetRepsLabel: String? = null,
        nextSetDetail: String? = null,
        /** Replaces weight × reps for a timed set ("45s · 20 reps"). */
        nextSetText: String? = null
    )

    fun startOrUpdateWarmupTimerNotification(
        remainingSeconds: Int,
        totalSeconds: Int? = null,
        scheduleAlarm: Boolean = true
    )

    fun vibrateEmomCue(cue: EmomCue)
}
