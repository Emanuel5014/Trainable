package com.emanuel5014.trainable.di

import android.content.Context
import android.net.Uri
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.repository.ExerciseMediaFiles
import com.emanuel5014.trainable.data.repository.ExerciseMediaImport
import com.emanuel5014.trainable.data.repository.WorkoutPlatform
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.ui.components.techniqueLabel
import com.emanuel5014.trainable.util.ExerciseMediaStorage
import com.emanuel5014.trainable.util.ImageStorageUtils
import com.emanuel5014.trainable.util.UriMigrationHelper
import com.emanuel5014.trainable.widget.TrainableWidget
import com.emanuel5014.trainable.widget.WeeklyGoalWidget

/** Exercise media in `filesDir/exercise_media` (ExerciseMediaStorage); [source] is a content uri. */
class AndroidExerciseMediaFiles(private val context: Context) : ExerciseMediaFiles {
    override fun import(source: String, exerciseId: Int): ExerciseMediaImport =
        ExerciseMediaStorage.import(context, Uri.parse(source), exerciseId)

    override fun delete(fileName: String?) = ExerciseMediaStorage.delete(context, fileName)
}

/** Routine photos through ImageStorageUtils, the home-screen widgets and the app's strings. */
class AndroidWorkoutPlatform(private val context: Context) : WorkoutPlatform {
    override fun encodeImage(uri: String): String? {
        val fixedUri = UriMigrationHelper.fixPath(uri, context) ?: uri
        return ImageStorageUtils.encodeImageToBase64(context, fixedUri)
    }

    override fun saveImage(base64: String): String? = ImageStorageUtils.saveBase64Image(context, base64)

    override fun onWorkoutsChanged() {
        try {
            TrainableWidget.update(context)
            WeeklyGoalWidget.update(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun techniqueLabel(technique: Technique): String = techniqueLabel(context, technique)

    override val noTechniqueLabel: String
        get() = context.getString(R.string.performed_technique_none)
}
