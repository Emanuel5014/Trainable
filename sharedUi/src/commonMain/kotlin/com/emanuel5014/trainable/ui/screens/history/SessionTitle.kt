package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.runtime.Composable
import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.domain.session.WorkoutTitle
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.workout_title_cardio
import com.emanuel5014.trainable.sharedui.resources.workout_title_quick
import org.jetbrains.compose.resources.stringResource

/** The name to show for a workout without a routine, in the language of the app. */
@Composable
fun WorkoutTitle.displayText(): String = when (this) {
    WorkoutTitle.Quick -> stringResource(Res.string.workout_title_quick)
    WorkoutTitle.Cardio -> stringResource(Res.string.workout_title_cardio)
    is WorkoutTitle.Named -> name
}

/** What this workout is called, from what it holds now (#75). */
val SessionWithDetails.title: WorkoutTitle
    get() = WorkoutTitle.of(session.noteSessione, plan.nome, plan.isSystemPlan, hasStrength = sets.isNotEmpty(), hasCardio = cardio.isNotEmpty())

/**
 * What an unfinished workout is called where its exercises are not at hand (dashboard, widgets): a workout without a
 * routine is a quick workout until it is done.
 */
val SessionWithPlanName.title: WorkoutTitle
    get() = WorkoutTitle.of(session.noteSessione, planNome, isSystemPlan = planIsSystem, hasStrength = false, hasCardio = false)

/** The name a workout goes by: the note the user gave the session, or else the name of its plan. */
fun sessionName(note: String?, planName: String): String = note?.takeIf { it.isNotBlank() } ?: planName
