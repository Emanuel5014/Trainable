package com.emanuel5014.trainable.ui.screens.workout

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The workout route argument (and saved-state key) that names an open session. */
internal const val WORKOUT_SESSION_ID = "sessionId"

/**
 * Follows the session of the open workout and keeps its id in the screen's saved state. A workout started from a plan
 * has only the plan in its route: without this, a screen recreated after the system killed the app would start a new,
 * empty session instead of resuming this one. [onSession] gets each new id.
 */
internal fun CoroutineScope.followWorkoutSession(
    state: StateFlow<WorkoutState>,
    savedState: SavedStateHandle,
    onSession: (Int) -> Unit = {}
): Job = launch {
    state.map { it.sessionId }.distinctUntilChanged().collect { id ->
        if (id != null) {
            savedState[WORKOUT_SESSION_ID] = id
            onSession(id)
        }
    }
}
