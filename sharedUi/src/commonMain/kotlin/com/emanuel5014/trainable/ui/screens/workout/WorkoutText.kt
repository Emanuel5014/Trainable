package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.Technique

/** Localized words for the workout model, captured in the current app language by each platform. */
interface WorkoutText {
    fun labels(weightUnit: String): PrescriptionLabels
    fun technique(technique: Technique): String

    val quickWorkout: String
    val extra: String
    val max: String
    fun cardioMinutes(minutes: Int): String
    fun cardioDistance(distance: String): String
    val cardioFreeForm: String
    fun timeReps(seconds: Int, reps: Int): String
    val miniSetNotification: String
    val emomFinishedTitle: String
    fun emomFinished(name: String): String
    fun emomPaused(round: Int, rounds: Int): String
    val emomGetReady: String
    fun emomRest(round: Int, rounds: Int): String
    fun emomWork(round: Int, rounds: Int): String
    fun emomNext(name: String, set: Int, load: String): String
    fun emomSet(name: String, set: Int, load: String): String
    fun emomThen(load: String): String
    val emomResume: String
    val emomStop: String
    val emomDone: String
    val emomPause: String
}
