package com.emanuel5014.trainable.ui.screens.dashboard

import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first

/** A rest or warm-up after a button pressed outside the workout: [end] null means it is over. */
data class SavedTimerChange(val sessionId: Int, val warmup: Boolean, val end: Long?, val total: Int?)

/**
 * −/+/Skip on the rest (or warm-up) saved on the session row, with no workout open: the dashboard's buttons, and the
 * Android notification's once the workout screen is closed. Only the row changes; the caller then redraws its
 * notification or Live Activity. Nothing is written when no timer runs or when it changed in the meantime.
 */
object SavedTimers {
    suspend fun adjust(
        workouts: WorkoutRepository,
        preferences: UserPreferencesRepository,
        sessionId: Int,
        button: RestButton,
        now: Long
    ): SavedTimerChange? {
        val session = workouts.getSessionWithSets(sessionId).first()?.session ?: return null
        if (session.isFinished) return null
        val delta = when (button) {
            RestButton.ADD -> preferences.timerAddSeconds.first()
            RestButton.SUBTRACT -> -preferences.timerSubtractSeconds.first()
            RestButton.SKIP -> 0
        }
        val restEnd = session.restTimerEndTime?.takeIf { it > now }
        val warmupEnd = session.warmupTimerEndTime?.takeIf { it > now }
        return when {
            restEnd != null -> {
                val change = LiveWorkout.adjustRest(restEnd, session.totalRestSeconds, delta, button == RestButton.SKIP, now)
                if (!workouts.compareAndSetRestTimer(session.id, session.timestamp, restEnd, change.end, change.total)) null
                else SavedTimerChange(session.id, warmup = false, end = change.end, total = change.total)
            }
            warmupEnd != null -> {
                val change = LiveWorkout.adjustRest(warmupEnd, session.totalWarmupSeconds, delta, button == RestButton.SKIP, now)
                if (!workouts.compareAndSetWarmupTimer(session.id, session.timestamp, warmupEnd, change.end, change.total)) null
                else SavedTimerChange(session.id, warmup = true, end = change.end, total = change.total)
            }
            else -> null
        }
    }
}
