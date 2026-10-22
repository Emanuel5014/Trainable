package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.ExerciseData
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.entity.CustomCategoryEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import android.content.Context
import android.net.Uri
import com.emanuel5014.trainable.util.ExerciseMediaStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    @ApplicationContext private val context: Context
) {
    fun getAllExercises(): Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()
    
    /**
     * Also the exercises deleted from the library: what shows or resumes a workout or a routine looks its exercises up here,
     * since those keep an exercise the user has deleted (see [deleteExercise]). Pickers use [getAllExercises].
     */
    fun getAllExercisesIncludingDeleted(): Flow<List<ExerciseEntity>> = exerciseDao.getAllExercisesIncludingDeleted()

    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>> = 
        exerciseDao.getExercisesByCategory(category)
        
    fun getCategories(): Flow<List<String>> =
        combine(
            exerciseDao.getCategories(),
            exerciseDao.getAllCustomCategories()
        ) { presetCategories, customCategories ->
            val customNames = customCategories.map { it.name }
            (presetCategories + customNames).distinct().sorted()
        }

    fun getCustomCategories(): Flow<List<CustomCategoryEntity>> =
        exerciseDao.getAllCustomCategories()
    
    suspend fun addCustomExercise(nome: String, categoria: String, description: String? = null): Int {
        // Creating again an exercise that was deleted brings the old one back, so its history and routines go on with it
        exerciseDao.findDeletedByName(nome.trim())?.let { deleted ->
            exerciseDao.reviveExercise(deleted.id, categoria, description)
            return deleted.id
        }
        val maxId = exerciseDao.getMaxId()
        val newId = if (maxId < 1000) 1000 else maxId + 1
        exerciseDao.insertExercise(
            ExerciseEntity(id = newId, nome = nome, categoria = categoria, descrizione = description)
        )
        return newId
    }

    /** The exercise with this id, also when it was deleted from the library: its history stays and can still be opened. */
    suspend fun getExercise(id: Int): ExerciseEntity? = exerciseDao.getExerciseById(id)

    /** The exercise with this id as it changes (hidden, restored, renamed); emits null once it is gone for good. */
    fun observeExercise(id: Int): Flow<ExerciseEntity?> = exerciseDao.observeExercise(id)

    suspend fun saveExercise(exercise: ExerciseEntity) {
        exerciseDao.updateExercise(exercise)
    }

    /**
     * Removes [exercise] from the library. The workouts, routines and maxes that use it keep it (it is only hidden and
     * can be [restoreExercise]d, so its media stays too); one nothing uses is deleted for good with its media.
     */
    suspend fun deleteExercise(exercise: ExerciseEntity) {
        val mediaFile = exerciseDao.getExerciseMedia(exercise.id)
        if (exerciseDao.removeFromLibrary(exercise.id)) {
            withContext(Dispatchers.IO) { ExerciseMediaStorage.delete(context, mediaFile) }
        }
    }

    /** Puts an exercise deleted from the library back, with everything that was attached to it. */
    suspend fun restoreExercise(exerciseId: Int) {
        exerciseDao.setExerciseDeleted(exerciseId, false)
    }

    suspend fun restoreExercises(exerciseIds: Collection<Int>) {
        exerciseIds.forEach { exerciseDao.setExerciseDeleted(it, false) }
    }

    /** Stores the picked image/GIF as the media of [exerciseId], replacing (and deleting) any previous one. */
    suspend fun attachExerciseMedia(exerciseId: Int, uri: Uri): ExerciseMediaStorage.ImportResult =
        withContext(Dispatchers.IO) {
            val result = ExerciseMediaStorage.import(context, uri, exerciseId)
            if (result is ExerciseMediaStorage.ImportResult.Saved) {
                val previous = exerciseDao.getExerciseMedia(exerciseId)
                exerciseDao.updateExerciseMedia(exerciseId, result.fileName)
                ExerciseMediaStorage.delete(context, previous)
            }
            result
        }

    /** Drops the media of every exercise and deletes the files. */
    suspend fun removeAllExerciseMedia() = withContext(Dispatchers.IO) {
        val files = exerciseDao.getAllExerciseMedia()
        exerciseDao.clearAllExerciseMedia()
        files.forEach { ExerciseMediaStorage.delete(context, it) }
    }

    suspend fun removeExerciseMedia(exerciseId: Int) = withContext(Dispatchers.IO) {
        val previous = exerciseDao.getExerciseMedia(exerciseId)
        exerciseDao.updateExerciseMedia(exerciseId, null)
        ExerciseMediaStorage.delete(context, previous)
    }

    /** Turns the plate calculator on or off for [exerciseId]; [barKg] is the bar weight in storage units (kg). */
    suspend fun setPlateCalculator(exerciseId: Int, enabled: Boolean, barKg: Float?) {
        exerciseDao.updateExercisePlateCalculator(exerciseId, enabled, barKg)
    }

    fun isCustomExercise(exercise: ExerciseEntity): Boolean = exercise.id >= 1000

    suspend fun resetPresetExerciseNames() {
        ExerciseData.initialExercises.forEach { presetExercise ->
            exerciseDao.updateExerciseName(presetExercise.id, presetExercise.nome)
        }
    }

    suspend fun addCustomCategory(name: String): Long {
        return exerciseDao.insertCustomCategory(CustomCategoryEntity(name = name))
    }

    suspend fun updateCustomCategory(category: CustomCategoryEntity) {
        exerciseDao.updateCustomCategory(category)
    }

    suspend fun deleteCustomCategory(id: Int) {
        exerciseDao.deleteCustomCategoryById(id)
    }
}
