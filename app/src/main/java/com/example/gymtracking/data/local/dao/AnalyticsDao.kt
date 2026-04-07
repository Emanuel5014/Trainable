package com.example.gymtracking.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {
    @Query("SELECT SUM(peso_sollevato * reps_effettive) FROM set_logs")
    fun getTotalVolume(): Flow<Float?>

    @Query(
        """
        SELECT SUM(s.peso_sollevato * s.reps_effettive)
        FROM set_logs s
        INNER JOIN workout_sessions w ON s.session_id = w.id
        WHERE w.timestamp >= :startDate
        """
    )
    fun getTotalVolumeSince(startDate: Long): Flow<Float?>
    
    @Query("""
        SELECT SUM(s.peso_sollevato * s.reps_effettive) AS volume, MIN(w.timestamp) AS timestamp
        FROM set_logs s
        INNER JOIN workout_sessions w ON s.session_id = w.id
        WHERE w.timestamp >= :startDate
        GROUP BY date(w.timestamp / 1000, 'unixepoch')
        ORDER BY MIN(w.timestamp) ASC
    """)
    fun getVolumeHistory(startDate: Long): Flow<List<DailyVolume>>
    
    @Query("""
        SELECT MAX(peso_sollevato) 
        FROM set_logs 
        WHERE exercise_id = :exerciseId
    """)
    fun getPersonalBest(exerciseId: Int): Flow<Float?>

    @Query(
        """
        SELECT
            e.id AS exerciseId,
            e.nome AS exerciseName,
            e.categoria AS category,
            COALESCE(MAX(s.peso_sollevato), 0) AS maxWeight
        FROM exercises e
        LEFT JOIN set_logs s ON s.exercise_id = e.id
        GROUP BY e.id, e.nome, e.categoria
        ORDER BY maxWeight DESC, e.nome ASC
        """
    )
    fun getAllPersonalBests(): Flow<List<PersonalBestRow>>

    @Query(
        """
        SELECT
            COALESCE(COUNT(DISTINCT date(w.timestamp / 1000, 'unixepoch')), 0) AS completedSessions,
            p.sessioni_target_settimana AS targetSessionsPerWeek
        FROM workout_plans p
        LEFT JOIN workout_sessions w ON w.plan_id = p.id AND w.timestamp >= :startDate
        WHERE p.id = :planId
        GROUP BY p.id, p.sessioni_target_settimana
        """
    )
    fun getConsistency(planId: Int, startDate: Long): Flow<ConsistencyRow?>

    @Query(
        """
        WITH recent AS (
            SELECT s.exercise_id, MAX(s.peso_sollevato) AS recent_max
            FROM set_logs s
            INNER JOIN workout_sessions w ON w.id = s.session_id
            WHERE w.timestamp >= :startDate
            GROUP BY s.exercise_id
        ),
        previous AS (
            SELECT s.exercise_id, MAX(s.peso_sollevato) AS previous_max
            FROM set_logs s
            INNER JOIN workout_sessions w ON w.id = s.session_id
            WHERE w.timestamp >= :previousStartDate AND w.timestamp < :startDate
            GROUP BY s.exercise_id
        )
        SELECT AVG(
            CASE
                WHEN previous.previous_max > 0 THEN ((recent.recent_max - previous.previous_max) * 100.0) / previous.previous_max
                ELSE NULL
            END
        ) AS strengthIndex
        FROM recent
        INNER JOIN previous ON previous.exercise_id = recent.exercise_id
        """
    )
    fun getStrengthIndex(startDate: Long, previousStartDate: Long): Flow<Float?>

    @Query(
        """
        SELECT
            e.categoria AS category,
            COALESCE(SUM(s.peso_sollevato * s.reps_effettive), 0) AS volume
        FROM set_logs s
        INNER JOIN exercises e ON e.id = s.exercise_id
        INNER JOIN workout_sessions w ON w.id = s.session_id
        WHERE w.timestamp >= :startDate
        GROUP BY e.categoria
        ORDER BY volume DESC
        """
    )
    fun getVolumeByCategory(startDate: Long): Flow<List<CategoryVolumeRow>>
}

data class DailyVolume(
    val volume: Float,
    val timestamp: Long
)

data class PersonalBestRow(
    val exerciseId: Int,
    val exerciseName: String,
    val category: String,
    val maxWeight: Float
)

data class ConsistencyRow(
    val completedSessions: Int,
    val targetSessionsPerWeek: Int
)

data class CategoryVolumeRow(
    val category: String,
    val volume: Float
)
