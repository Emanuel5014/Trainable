package com.emanuel5014.trainable.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.quick_workout
import com.emanuel5014.trainable.sharedui.resources.extra_badge
import com.emanuel5014.trainable.sharedui.resources.max_label
import com.emanuel5014.trainable.sharedui.resources.cardio_min_format
import com.emanuel5014.trainable.sharedui.resources.cardio_dist_value
import com.emanuel5014.trainable.sharedui.resources.cardio_free_form
import com.emanuel5014.trainable.sharedui.resources.time_reps_value
import com.emanuel5014.trainable.sharedui.resources.mini_set_notification
import com.emanuel5014.trainable.sharedui.resources.emom_notification_finished_title
import com.emanuel5014.trainable.sharedui.resources.emom_notification_finished_text
import com.emanuel5014.trainable.sharedui.resources.emom_notification_title_paused
import com.emanuel5014.trainable.sharedui.resources.emom_notification_title_get_ready
import com.emanuel5014.trainable.sharedui.resources.emom_notification_title_rest
import com.emanuel5014.trainable.sharedui.resources.emom_notification_title_work
import com.emanuel5014.trainable.sharedui.resources.emom_notification_next
import com.emanuel5014.trainable.sharedui.resources.emom_notification_set
import com.emanuel5014.trainable.sharedui.resources.emom_then
import com.emanuel5014.trainable.sharedui.resources.emom_action_resume
import com.emanuel5014.trainable.sharedui.resources.emom_action_stop
import com.emanuel5014.trainable.sharedui.resources.emom_action_done
import com.emanuel5014.trainable.sharedui.resources.emom_action_pause
import com.emanuel5014.trainable.ui.components.LocalPrescriptionText
import com.emanuel5014.trainable.ui.components.PrescriptionText
import com.emanuel5014.trainable.ui.screens.workout.WorkoutText
import org.jetbrains.compose.resources.stringResource

/** Captures translated templates in composition for use by the workout's coroutines. */
@Composable
internal fun rememberIosWorkoutText(): WorkoutText {
    val prescription = LocalPrescriptionText.current
    val templates = WorkoutTemplates(
        quickWorkout = stringResource(Res.string.quick_workout),
        extra = stringResource(Res.string.extra_badge),
        max = stringResource(Res.string.max_label),
        cardioMinutes = stringResource(Res.string.cardio_min_format),
        cardioDistance = stringResource(Res.string.cardio_dist_value),
        cardioFreeForm = stringResource(Res.string.cardio_free_form),
        timeReps = stringResource(Res.string.time_reps_value),
        miniSetNotification = stringResource(Res.string.mini_set_notification),
        emomFinishedTitle = stringResource(Res.string.emom_notification_finished_title),
        emomFinished = stringResource(Res.string.emom_notification_finished_text),
        emomPaused = stringResource(Res.string.emom_notification_title_paused),
        emomGetReady = stringResource(Res.string.emom_notification_title_get_ready),
        emomRest = stringResource(Res.string.emom_notification_title_rest),
        emomWork = stringResource(Res.string.emom_notification_title_work),
        emomNext = stringResource(Res.string.emom_notification_next),
        emomSet = stringResource(Res.string.emom_notification_set),
        emomThen = stringResource(Res.string.emom_then),
        emomResume = stringResource(Res.string.emom_action_resume),
        emomStop = stringResource(Res.string.emom_action_stop),
        emomDone = stringResource(Res.string.emom_action_done),
        emomPause = stringResource(Res.string.emom_action_pause)
    )
    return remember(prescription, templates) { IosWorkoutText(prescription, templates) }
}

private data class WorkoutTemplates(
    val quickWorkout: String,
    val extra: String,
    val max: String,
    val cardioMinutes: String,
    val cardioDistance: String,
    val cardioFreeForm: String,
    val timeReps: String,
    val miniSetNotification: String,
    val emomFinishedTitle: String,
    val emomFinished: String,
    val emomPaused: String,
    val emomGetReady: String,
    val emomRest: String,
    val emomWork: String,
    val emomNext: String,
    val emomSet: String,
    val emomThen: String,
    val emomResume: String,
    val emomStop: String,
    val emomDone: String,
    val emomPause: String
)

private class IosWorkoutText(
    private val prescription: PrescriptionText,
    private val templates: WorkoutTemplates
) : WorkoutText {
    override fun labels(weightUnit: String): PrescriptionLabels = prescription.labels(weightUnit)
    override fun technique(technique: Technique): String = prescription.technique(technique)

    // Replace numbered placeholders in one pass: exercise names may themselves contain "%2$s".
    private fun fill(template: String, vararg args: Any): String =
        Regex("%(\\d+)\\$[sd]").replace(template) { match ->
            args[match.groupValues[1].toInt() - 1].toString()
        }

    override val quickWorkout: String get() = templates.quickWorkout
    override val extra: String get() = templates.extra
    override val max: String get() = templates.max
    override fun cardioMinutes(minutes: Int): String = fill(templates.cardioMinutes, minutes)
    override fun cardioDistance(distance: String): String = fill(templates.cardioDistance, distance)
    override val cardioFreeForm: String get() = templates.cardioFreeForm
    override fun timeReps(seconds: Int, reps: Int): String = fill(templates.timeReps, seconds, reps)
    override val miniSetNotification: String get() = templates.miniSetNotification
    override val emomFinishedTitle: String get() = templates.emomFinishedTitle
    override fun emomFinished(name: String): String = fill(templates.emomFinished, name)
    override fun emomPaused(round: Int, rounds: Int): String = fill(templates.emomPaused, round, rounds)
    override val emomGetReady: String get() = templates.emomGetReady
    override fun emomRest(round: Int, rounds: Int): String = fill(templates.emomRest, round, rounds)
    override fun emomWork(round: Int, rounds: Int): String = fill(templates.emomWork, round, rounds)
    override fun emomNext(name: String, set: Int, load: String): String = fill(templates.emomNext, name, set, load)
    override fun emomSet(name: String, set: Int, load: String): String = fill(templates.emomSet, name, set, load)
    override fun emomThen(load: String): String = fill(templates.emomThen, load)
    override val emomResume: String get() = templates.emomResume
    override val emomStop: String get() = templates.emomStop
    override val emomDone: String get() = templates.emomDone
    override val emomPause: String get() = templates.emomPause
}
