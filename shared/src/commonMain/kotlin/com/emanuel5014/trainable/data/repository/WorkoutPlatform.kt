package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.domain.prescription.Technique

/** What the workouts and routines need from the platform: photos, home-screen widgets and a few words. */
interface WorkoutPlatform {
    /** The routine photo at [uri] as base64 for a routine file, or null when it cannot be read. */
    fun encodeImage(uri: String): String?

    /** Stores a routine file's base64 photo; its new address, or null when it could not be saved. */
    fun saveImage(base64: String): String?

    /** Called after workouts change, so what shows them outside the app (widgets) can refresh. */
    fun onWorkoutsChanged()

    /** The name of [technique] in the app's language, as the CSV export writes it. */
    fun techniqueLabel(technique: Technique): String

    /** What the CSV export writes for a set done with no technique. */
    val noTechniqueLabel: String
}
