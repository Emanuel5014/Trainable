package com.emanuel5014.trainable.util.notification

import kotlinx.coroutines.flow.SharedFlow

/** NEXT_SET logs the set the finished-rest card shows; MORE_REST gives the rest back for the "+" seconds. */
enum class TimerAction { SKIP, ADD, SUBTRACT, DISMISS, FINISHED, NEXT_SET, MORE_REST }
enum class WarmupTimerAction { SKIP, ADD, SUBTRACT, DISMISS, FINISHED }

/** What the EMOM notification or its round alarm asks of the workout. */
enum class EmomAction { BOUNDARY, DONE, PAUSE, RESUME, STOP }

/**
 * The set the finished-rest "Set done" button would log, exactly as the card shows it (weight in kg).
 * Null wherever the button must not appear.
 */
data class NextSetKey(val exerciseId: Int, val setNumber: Int, val weight: Float, val reps: Int) {
    /** "1001:2:2.5:6": how a card that can only hold text (an iOS Live Activity) carries the key. */
    fun token(): String = "$exerciseId:$setNumber:$weight:$reps"

    companion object {
        /** The key a card [token] carries, or null when the token is missing or damaged. */
        fun parse(token: String): NextSetKey? {
            val parts = token.split(':')
            if (parts.size != 4) return null
            return NextSetKey(
                parts[0].toIntOrNull() ?: return null,
                parts[1].toIntOrNull() ?: return null,
                parts[2].toFloatOrNull() ?: return null,
                parts[3].toIntOrNull() ?: return null
            )
        }
    }
}

/** What the finished-rest card shows as the next set, with the key of its "Set done" button. */
data class RestNextSet(
    val exerciseName: String?,
    val setNumber: Int?,
    val weight: Float?,
    val reps: Int?,
    val previousReps: Int?,
    val weightUnit: String?,
    val repsLabel: String?,
    val detail: String?,
    val text: String?,
    val key: NextSetKey?
)

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

    /** The rest is over; [next] is the set the card announces, null keeps what the card already shows. */
    fun showRestFinished(next: RestNextSet? = null)

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
        nextSetText: String? = null,
        /** The announced set when "Set done" may log it once the rest ends; null hides the button. */
        nextSetKey: NextSetKey? = null
    )

    fun startOrUpdateWarmupTimerNotification(
        remainingSeconds: Int,
        totalSeconds: Int? = null,
        scheduleAlarm: Boolean = true
    )

    fun vibrateEmomCue(cue: EmomCue)
}
