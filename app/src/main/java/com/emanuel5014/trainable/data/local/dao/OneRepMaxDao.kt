package com.emanuel5014.trainable.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.data.local.relation.OneRepMaxWithExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface OneRepMaxDao {
    @Insert
    suspend fun insert(entry: OneRepMaxEntity): Long

    @Delete
    suspend fun delete(entry: OneRepMaxEntity)

    @Query("SELECT * FROM one_rep_maxes WHERE exercise_id = :exerciseId ORDER BY date DESC, id DESC")
    fun getHistory(exerciseId: Int): Flow<List<OneRepMaxEntity>>

    @Query("SELECT * FROM one_rep_maxes WHERE exercise_id = :exerciseId ORDER BY date DESC, id DESC LIMIT 1")
    fun getCurrent(exerciseId: Int): Flow<OneRepMaxEntity?>

    @Query("SELECT * FROM one_rep_maxes WHERE exercise_id = :exerciseId ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getCurrentOnce(exerciseId: Int): OneRepMaxEntity?

    /** Latest entry per exercise. */
    @Query(
        """
        SELECT o.* FROM one_rep_maxes o
        WHERE o.id = (
            SELECT o2.id FROM one_rep_maxes o2 WHERE o2.exercise_id = o.exercise_id
            ORDER BY o2.date DESC, o2.id DESC LIMIT 1
        )
        """
    )
    fun getAllCurrent(): Flow<List<OneRepMaxEntity>>

    @Query("SELECT * FROM one_rep_maxes ORDER BY date DESC, id DESC")
    fun getAll(): Flow<List<OneRepMaxEntity>>

    @Query(
        """
        SELECT o.*, e.nome AS exercise_nome, e.categoria AS exercise_categoria FROM one_rep_maxes o
        INNER JOIN exercises e ON e.id = o.exercise_id
        ORDER BY o.date DESC, o.id DESC
        """
    )
    fun getAllWithExercise(): Flow<List<OneRepMaxWithExercise>>

    /** Best Epley e1RM per exercise from completed, non-warmup working sets of finished sessions. */
    @Query(
        """
        SELECT s.exercise_id AS exerciseId,
               MAX(CASE WHEN s.reps_effettive = 1 THEN s.peso_sollevato
                        ELSE s.peso_sollevato * (1 + s.reps_effettive / 30.0) END) AS e1rm
        FROM set_logs s INNER JOIN workout_sessions ws ON ws.id = s.session_id
        WHERE ws.is_finished = 1 AND s.is_completed = 1 AND s.is_warmup = 0
          AND s.peso_sollevato > 0 AND s.reps_effettive BETWEEN 1 AND 12
          AND s.durata_secondi IS NULL AND ws.timestamp >= :sinceTimestamp
        GROUP BY s.exercise_id
        """
    )
    fun getEstimatedOneRepMaxes(sinceTimestamp: Long): Flow<List<EstimatedOneRepMaxRow>>
}

data class EstimatedOneRepMaxRow(val exerciseId: Int, val e1rm: Float)
