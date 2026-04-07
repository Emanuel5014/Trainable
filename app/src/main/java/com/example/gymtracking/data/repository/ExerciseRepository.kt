package com.example.gymtracking.data.repository

import com.example.gymtracking.data.local.dao.ExerciseDao
import com.example.gymtracking.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao
) {
    fun getAllExercises(): Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()
    
    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>> = 
        exerciseDao.getExercisesByCategory(category)
        
    fun getCategories(): Flow<List<String>> = exerciseDao.getCategories()
    
    suspend fun addCustomExercise(nome: String, categoria: String, description: String? = null) {
        val maxId = exerciseDao.getMaxId()
        val newId = if (maxId < 1000) 1000 else maxId + 1
        exerciseDao.insertExercise(
            ExerciseEntity(id = newId, nome = nome, categoria = categoria, descrizione = description)
        )
    }

    suspend fun updateExercise(exercise: ExerciseEntity) {
        exerciseDao.insertExercise(exercise)
    }

    suspend fun deleteExercise(exercise: ExerciseEntity) {
        exerciseDao.deleteExerciseById(exercise.id)
    }

    fun isCustomExercise(exercise: ExerciseEntity): Boolean = exercise.id >= 1000
}
