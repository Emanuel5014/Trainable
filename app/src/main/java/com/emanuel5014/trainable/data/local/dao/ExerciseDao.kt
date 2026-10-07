package com.emanuel5014.trainable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emanuel5014.trainable.data.local.entity.CustomCategoryEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY nome ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE categoria = :category ORDER BY nome ASC")
    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>>

    @Query("SELECT DISTINCT categoria FROM exercises ORDER BY categoria ASC")
    fun getCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<ExerciseEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query("SELECT COALESCE(MAX(id), 0) FROM exercises")
    suspend fun getMaxId(): Int

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Int)

    @Query("UPDATE exercises SET nome = :nome WHERE id = :id")
    suspend fun updateExerciseName(id: Int, nome: String)

    @Query("SELECT media_path FROM exercises WHERE id = :id")
    suspend fun getExerciseMedia(id: Int): String?

    @Query("UPDATE exercises SET media_path = :mediaPath WHERE id = :id")
    suspend fun updateExerciseMedia(id: Int, mediaPath: String?)

    @Query("SELECT media_path FROM exercises WHERE media_path IS NOT NULL")
    suspend fun getAllExerciseMedia(): List<String>

    @Query("UPDATE exercises SET media_path = NULL WHERE media_path IS NOT NULL")
    suspend fun clearAllExerciseMedia()

    @Query("UPDATE exercises SET plate_calc = :enabled, plate_bar_kg = :barKg WHERE id = :id")
    suspend fun updateExercisePlateCalculator(id: Int, enabled: Boolean, barKg: Float?)

    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    fun getAllCustomCategories(): Flow<List<CustomCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(category: CustomCategoryEntity): Long

    @Update
    suspend fun updateCustomCategory(category: CustomCategoryEntity)

    @Query("DELETE FROM custom_categories WHERE id = :id")
    suspend fun deleteCustomCategoryById(id: Int)
}
