package com.emanuel5014.trainable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.emanuel5014.trainable.data.local.entity.CustomCategoryEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    /** The exercise library: what the user can pick. Deleted exercises are not part of it, see [getAllExercisesIncludingDeleted]. */
    @Query("SELECT * FROM exercises WHERE is_deleted = 0 ORDER BY nome ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    /** Every row, also the exercises deleted from the library that history and routines still use (matching imported data). */
    @Query("SELECT * FROM exercises ORDER BY nome ASC")
    fun getAllExercisesIncludingDeleted(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE categoria = :category AND is_deleted = 0 ORDER BY nome ASC")
    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>>

    @Query("SELECT DISTINCT categoria FROM exercises WHERE is_deleted = 0 ORDER BY categoria ASC")
    fun getCategories(): Flow<List<String>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Int): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE is_deleted = 1 AND LOWER(nome) = LOWER(:nome) LIMIT 1")
    suspend fun findDeletedByName(nome: String): ExerciseEntity?

    @Query("UPDATE exercises SET is_deleted = :deleted WHERE id = :id")
    suspend fun setExerciseDeleted(id: Int, deleted: Boolean)

    /** Brings a deleted exercise back to the library as the one the user is creating now. */
    @Query("UPDATE exercises SET is_deleted = 0, categoria = :categoria, descrizione = :descrizione WHERE id = :id")
    suspend fun reviveExercise(id: Int, categoria: String, descrizione: String?)

    /** True when a workout, a routine, a swap, a program change or a 1RM still refers to the exercise. */
    @Query(
        """
        SELECT EXISTS(SELECT 1 FROM set_logs WHERE exercise_id = :id)
            OR EXISTS(SELECT 1 FROM plan_exercises WHERE exercise_id = :id)
            OR EXISTS(SELECT 1 FROM one_rep_maxes WHERE exercise_id = :id)
            OR EXISTS(SELECT 1 FROM session_exercise_swaps WHERE replacement_exercise_id = :id)
            OR EXISTS(SELECT 1 FROM session_prescription_edits WHERE exercise_id = :id)
        """
    )
    suspend fun isExerciseInUse(id: Int): Boolean

    /**
     * Takes the exercise out of the library without touching the history and the routines that use it: those keep it
     * (the row is only hidden, the foreign keys cascade on a real delete). An exercise nothing refers to is deleted for real.
     *
     * @return true when the row was deleted for good, false when it was only hidden.
     */
    @Transaction
    suspend fun removeFromLibrary(id: Int): Boolean {
        if (isExerciseInUse(id)) {
            setExerciseDeleted(id, true)
            return false
        }
        deleteExerciseById(id)
        return true
    }

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

    /** Deletes the row for good, and with it (foreign keys cascade) every set, routine entry and 1RM of the exercise: use [removeFromLibrary]. */
    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Int)

    @Query("DELETE FROM exercises WHERE id IN (:ids)")
    suspend fun deleteExercisesByIds(ids: List<Int>)

    @Query("DELETE FROM custom_categories WHERE id IN (:ids)")
    suspend fun deleteCustomCategoriesByIds(ids: List<Int>)

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
