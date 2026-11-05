package com.emanuel5014.trainable.di

import android.content.Context
import android.net.Uri
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.repository.ExerciseMediaFiles
import com.emanuel5014.trainable.data.repository.ExerciseMediaImport
import com.emanuel5014.trainable.data.repository.KeyValueStore
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

/** A [KeyValueStore] over a SharedPreferences file of the app, written in the background as before (apply). */
class SharedPreferencesStore(context: Context, name: String) : KeyValueStore {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    override fun getString(key: String, default: String?): String? = prefs.getString(key, default)
    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    override fun getStringSet(key: String, default: Set<String>?): Set<String>? = prefs.getStringSet(key, default)
    override fun putStringSet(key: String, value: Set<String>) = prefs.edit().putStringSet(key, value).apply()
}

/** The physical-check photos in `filesDir/physical_checks`, compressed by ImageStorageUtils as before. */
class AndroidPhysicalCheckFiles(private val context: Context) : com.emanuel5014.trainable.data.repository.PhysicalCheckFiles {
    private val filesDirectory: java.io.File
        get() = java.io.File(context.filesDir, "physical_checks").apply { if (!exists()) mkdirs() }

    override fun list(): List<String> = filesDirectory.listFiles()?.filter { it.isFile }?.map { it.name } ?: emptyList()
    override fun exists(name: String): Boolean = java.io.File(filesDirectory, name).exists()
    override fun read(name: String): ByteArray = java.io.File(filesDirectory, name).readBytes()
    override fun write(name: String, bytes: ByteArray) = java.io.File(filesDirectory, name).writeBytes(bytes)
    override fun delete(name: String) {
        java.io.File(filesDirectory, name).delete()
    }
    override fun compressImage(bytes: ByteArray): ByteArray = ImageStorageUtils.compressImageBytes(bytes)
}
