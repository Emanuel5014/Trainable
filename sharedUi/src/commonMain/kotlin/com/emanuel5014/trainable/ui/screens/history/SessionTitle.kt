package com.emanuel5014.trainable.ui.screens.history

import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.add
import com.emanuel5014.trainable.sharedui.resources.add_cardio
import com.emanuel5014.trainable.sharedui.resources.custom_workout
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

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
    SessionTitle.Cardio -> stringResource(Res.string.add_cardio).replace(stringResource(Res.string.add) + " ", "")
    SessionTitle.CustomWorkout -> stringResource(Res.string.custom_workout)
    is SessionTitle.Named -> name
}

/** The name a workout goes by: the note the user gave the session, or else the name of its plan. */
fun sessionName(note: String?, planName: String): String = note?.takeIf { it.isNotBlank() } ?: planName
