package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.emanuel5014.trainable.R

/** What a workout is called in the history: the plans the app creates for itself have a translated name. */
sealed interface SessionTitle {
    data object Cardio : SessionTitle

    data object CustomWorkout : SessionTitle

    data class Named(val name: String) : SessionTitle

    companion object {
        fun of(name: String): SessionTitle = when (name) {
            "Cardio" -> Cardio
            "Custom Workout" -> CustomWorkout
            else -> Named(name)
        }
    }
}

@Composable
fun SessionTitle.displayText(): String = when (this) {
    SessionTitle.Cardio -> stringResource(R.string.add_cardio).replace(stringResource(R.string.add) + " ", "")
    SessionTitle.CustomWorkout -> stringResource(R.string.custom_workout)
    is SessionTitle.Named -> name
}

/** The name a workout goes by: the note the user gave the session, or else the name of its plan. */
fun sessionName(note: String?, planName: String): String = note?.takeIf { it.isNotBlank() } ?: planName
