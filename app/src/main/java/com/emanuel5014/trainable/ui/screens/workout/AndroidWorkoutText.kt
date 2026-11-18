package com.emanuel5014.trainable.ui.screens.workout

import android.content.Context
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.prescriptionLabels
import com.emanuel5014.trainable.ui.components.techniqueLabel
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.Technique

/** Uses the same context resources and prescription helpers as WorkoutViewModel did before extraction. */
class AndroidWorkoutText(private val context: Context) : WorkoutText {
    override fun labels(weightUnit: String): PrescriptionLabels = prescriptionLabels(context, weightUnit)
    override fun technique(technique: Technique): String = techniqueLabel(context, technique)

    override val quickWorkout: String get() = context.getString(R.string.quick_workout)
    override val extra: String get() = context.getString(R.string.extra_badge)
    override val max: String get() = context.getString(R.string.max_label)
    override fun cardioMinutes(minutes: Int): String =
        context.getString(R.string.cardio_min_format, minutes)
    override fun cardioDistance(distance: String): String =
        context.getString(R.string.cardio_dist_value, distance)
    override val cardioFreeForm: String get() = context.getString(R.string.cardio_free_form)
    override fun timeReps(seconds: Int, reps: Int): String =
        context.getString(R.string.time_reps_value, seconds, reps)
    override val miniSetNotification: String get() = context.getString(R.string.mini_set_notification)
    override val emomFinishedTitle: String get() = context.getString(R.string.emom_notification_finished_title)
    override fun emomFinished(name: String): String =
        context.getString(R.string.emom_notification_finished_text, name)
    override fun emomPaused(round: Int, rounds: Int): String =
        context.getString(R.string.emom_notification_title_paused, round, rounds)
    override val emomGetReady: String get() = context.getString(R.string.emom_notification_title_get_ready)
    override fun emomRest(round: Int, rounds: Int): String =
        context.getString(R.string.emom_notification_title_rest, round, rounds)
    override fun emomWork(round: Int, rounds: Int): String =
        context.getString(R.string.emom_notification_title_work, round, rounds)
    override fun emomNext(name: String, set: Int, load: String): String =
        context.getString(R.string.emom_notification_next, name, set, load)
    override fun emomSet(name: String, set: Int, load: String): String =
        context.getString(R.string.emom_notification_set, name, set, load)
    override fun emomThen(load: String): String =
        context.getString(R.string.emom_then, load)
    override val emomResume: String get() = context.getString(R.string.emom_action_resume)
    override val emomStop: String get() = context.getString(R.string.emom_action_stop)
    override val emomDone: String get() = context.getString(R.string.emom_action_done)
    override val emomPause: String get() = context.getString(R.string.emom_action_pause)
}
