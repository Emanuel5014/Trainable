package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.ExerciseData
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.entity.CustomCategoryEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.exercise.CardioExerciseMatcher
import com.emanuel5014.trainable.domain.exercise.CategoryChangeBlock
import com.emanuel5014.trainable.domain.exercise.ExerciseNames
import com.emanuel5014.trainable.domain.exercise.ExerciseTraits
import com.emanuel5014.trainable.domain.exercise.StoredTraits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val mediaFiles: ExerciseMediaFiles
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
    
    suspend fun addCustomExercise(
        nome: String,
        categoria: String,
        description: String? = null,
        traits: ExerciseTraits = ExerciseTraits.EMPTY
    ): Int {
        // Creating again an exercise that was hidden brings the old one back, so its history and routines go on with it.
        // A custom one takes the category picked now (issue #41); a preset keeps its own, since it cannot be edited while
        // the presets are locked. The description it had stays unless one is typed now (a picker has no description field);
        // likewise a custom one takes the muscles and equipment chosen now, and a preset keeps its own. A name in any
        // language finds it, as the duplicate check does.
        val hidden = ExerciseNames.findHidden(
            nome,
            exerciseDao.getAllExercisesIncludingDeleted().first(),
            exerciseDao.getExerciseMerges().map { it.loserId }.toSet()
        )
        if (hidden != null) {
            val revived = ExerciseTraits.forRevival(hidden.id, StoredTraits(hidden.muscles, hidden.equipment), traits)
            // Nor does a custom one take a category its history does not allow (cardio logs, strength sets)
            val keepCategory = hidden.id < ExerciseEntity.FIRST_CUSTOM_ID || categoryChangeBlock(hidden.id, categoria) != null
            exerciseDao.reviveExercise(
                hidden.id,
                if (keepCategory) hidden.categoria else categoria,
                description?.takeIf { it.isNotBlank() } ?: hidden.descrizione,
                revived.muscles,
                revived.equipment
            )
            return hidden.id
        }
        val maxId = exerciseDao.getMaxId()
        val newId = if (maxId < 1000) 1000 else maxId + 1
        val stored = ExerciseTraits.forStorage(newId, traits)
        exerciseDao.insertExercise(
            ExerciseEntity(
                id = newId, nome = nome, categoria = categoria, descrizione = description,
                muscles = stored.muscles, equipment = stored.equipment
            )
        )
        return newId
    }

    /**
     * The cardio exercise a typed activity name stands for, decided like the database update and the startup repair
     * ([CardioExerciseMatcher]): an existing one, or a new custom cardio exercise added now. For the dialogs that still
     * take a free-text name, so even their logs carry an exercise id.
     */
    suspend fun cardioExerciseForName(name: String): ExerciseEntity {
        val exercises = exerciseDao.getAllExercisesIncludingDeleted().first()
        val skipped = exerciseDao.getSkippedPresetIds().toSet()
        val plan = CardioExerciseMatcher.plan(
            listOf(CardioExerciseMatcher.Row(logId = 0, name = name)), exercises, skipped, exerciseDao.getMaxId() + 1
        )
        // The new id is above every id of the table: the insert cannot replace anything
        plan.newExercises.firstOrNull()?.let { created ->
            exerciseDao.insertExercises(listOf(created))
            return exerciseDao.getExerciseById(created.id) ?: created
        }
        val id = plan.links.getValue(0)
        return exercises.first { it.id == id }
    }

    /** The exercise with this id, also when it was deleted from the library: its history stays and can still be opened. */
    suspend fun getExercise(id: Int): ExerciseEntity? = exerciseDao.getExerciseById(id)

    /** True when a workout, a routine or anything else still refers to the exercise (it would be hidden, not deleted). */
    suspend fun isExerciseInUse(id: Int): Boolean = exerciseDao.isExerciseInUse(id)

    /** The exercise with this id as it changes (hidden, restored, renamed); emits null once it is gone for good. */
    fun observeExercise(id: Int): Flow<ExerciseEntity?> = exerciseDao.observeExercise(id)

    /**
     * Why moving the exercise to [newCategory] is not allowed, or null when it is: out of Cardio while cardio logs are
     * linked to it, into Cardio while it has strength sets (see [CategoryChangeBlock.of]).
     */
    suspend fun categoryChangeBlock(id: Int, newCategory: String): CategoryChangeBlock? {
        val current = exerciseDao.getExerciseById(id) ?: return null
        return CategoryChangeBlock.of(
            from = current.categoria,
            to = newCategory,
            hasCardioLogs = exerciseDao.hasCardioLogs(id),
            hasStrengthSets = exerciseDao.hasSetLogs(id)
        )
    }

    /**
     * Saves what the exercise form edits (name, category, description, muscles and equipment) and nothing else of the row.
     * A category change its history does not allow saves nothing and returns why.
     */
    suspend fun updateExerciseDetails(id: Int, nome: String, categoria: String, descrizione: String?, traits: StoredTraits): CategoryChangeBlock? {
        categoryChangeBlock(id, categoria)?.let { return it }
        exerciseDao.updateExerciseDetails(id, nome, categoria, descrizione, traits.muscles, traits.equipment)
        return null
    }

    /** Stars or unstars the exercise; the rest of its row is not touched. */
    suspend fun setFavorite(id: Int, favorite: Boolean) {
        exerciseDao.setFavorite(id, favorite)
    }

    /**
     * Removes [exercise] from the library. The workouts, routines and maxes that use it keep it (it is only hidden and
     * can be [restoreExercise]d, so its media stays too); one nothing uses is deleted for good with its media.
     */
    suspend fun deleteExercise(exercise: ExerciseEntity) {
        val mediaFile = exerciseDao.getExerciseMedia(exercise.id)
        if (exerciseDao.removeFromLibrary(exercise.id)) {
            withContext(Dispatchers.IO) { mediaFiles.delete(mediaFile) }
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
    suspend fun attachExerciseMedia(exerciseId: Int, source: String): ExerciseMediaImport =
        withContext(Dispatchers.IO) {
            val result = mediaFiles.import(source, exerciseId)
            if (result is ExerciseMediaImport.Saved) {
                val previous = exerciseDao.getExerciseMedia(exerciseId)
                exerciseDao.updateExerciseMedia(exerciseId, result.fileName)
                mediaFiles.delete(previous)
            }
            result
        }

    /** Drops the media of every exercise and deletes the files. */
    suspend fun removeAllExerciseMedia() = withContext(Dispatchers.IO) {
        val files = exerciseDao.getAllExerciseMedia()
        exerciseDao.clearAllExerciseMedia()
        files.forEach { mediaFiles.delete(it) }
    }

    suspend fun removeExerciseMedia(exerciseId: Int) = withContext(Dispatchers.IO) {
        val previous = exerciseDao.getExerciseMedia(exerciseId)
        exerciseDao.updateExerciseMedia(exerciseId, null)
        mediaFiles.delete(previous)
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
