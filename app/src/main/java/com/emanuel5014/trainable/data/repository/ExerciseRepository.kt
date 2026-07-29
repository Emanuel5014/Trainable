package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.ExerciseData
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.entity.CustomCategoryEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao
) {
    fun getAllExercises(): Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()
    
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
        val maxId = exerciseDao.getMaxId()
        val newId = if (maxId < 1000) 1000 else maxId + 1
        exerciseDao.insertExercise(
            ExerciseEntity(id = newId, nome = nome, categoria = categoria, descrizione = description)
        )
        return newId
    }

    suspend fun saveExercise(exercise: ExerciseEntity) {
        exerciseDao.updateExercise(exercise)
    }

    suspend fun deleteExercise(exercise: ExerciseEntity) {
        exerciseDao.deleteExerciseById(exercise.id)
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
