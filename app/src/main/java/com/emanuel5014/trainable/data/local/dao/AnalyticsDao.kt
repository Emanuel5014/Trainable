package com.emanuel5014.trainable.data.local.dao

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
        SELECT SUM(s.peso_sollevato * s.reps_effettive) AS volume, MIN(w.timestamp) AS timestamp
        FROM set_logs s
        INNER JOIN workout_sessions w ON s.session_id = w.id
        WHERE w.timestamp >= :startDate AND w.plan_id = :planId
        GROUP BY date(w.timestamp / 1000, 'unixepoch')
        ORDER BY MIN(w.timestamp) ASC
    """)
    fun getVolumeHistoryForPlan(planId: Int, startDate: Long): Flow<List<DailyVolume>>
    
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
            COALESCE(s.peso_sollevato, 0) AS maxWeight,
            COALESCE(s.reps_effettive, 0) AS reps
        FROM exercises e
        LEFT JOIN set_logs s ON s.id = (
            SELECT id FROM set_logs 
            WHERE exercise_id = e.id 
            ORDER BY peso_sollevato DESC, reps_effettive DESC 
            LIMIT 1
        )
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
            CAST(COUNT(s.id) AS REAL) AS volume
        FROM set_logs s
        INNER JOIN exercises e ON e.id = s.exercise_id
        INNER JOIN workout_sessions w ON w.id = s.session_id
        WHERE w.timestamp >= :startDate
            AND s.is_completed = 1
            AND s.is_warmup = 0
        GROUP BY e.categoria
        ORDER BY volume DESC
        """
    )
    fun getVolumeByCategory(startDate: Long): Flow<List<CategoryVolumeRow>>

    @Query("""
        SELECT 
            CASE 
                WHEN MAX(s.reps_effettive) = 1 THEN MAX(s.peso_sollevato)
                ELSE MAX(s.peso_sollevato) * (1.0 + MAX(s.reps_effettive) / 30.0)
            END AS maxValue,
            MIN(w.timestamp) AS timestamp
        FROM set_logs s
        INNER JOIN workout_sessions w ON s.session_id = w.id
        WHERE s.exercise_id = :exerciseId 
            AND w.timestamp >= :startDate
            AND s.reps_effettive > 0
        GROUP BY date(w.timestamp / 1000, 'unixepoch')
        ORDER BY MIN(w.timestamp) ASC
    """)
    fun getExerciseProgressHistory(exerciseId: Int, startDate: Long): Flow<List<DailyExerciseMax>>

    @Query("""
        SELECT 
            COALESCE(SUM(s.peso_sollevato * s.reps_effettive), 0) AS volume,
            COUNT(DISTINCT w.id) AS sessionCount,
            COUNT(s.id) AS setCount,
            COALESCE(AVG(s.peso_sollevato), 0) AS avgWeight
        FROM set_logs s
        INNER JOIN workout_sessions w ON s.session_id = w.id
        WHERE w.timestamp >= :startDate AND w.timestamp <= :endDate
    """)
    fun getPeriodMetrics(startDate: Long, endDate: Long): Flow<PeriodMetrics>

    @Query("""
        SELECT 
            e.nome AS exerciseName,
            COALESCE(SUM(s.peso_sollevato * s.reps_effettive), 0) AS volume,
            COUNT(s.id) AS setCount,
            COALESCE(MAX(s.peso_sollevato), 0) AS maxWeight,
            COALESCE(MAX(
                CASE 
                    WHEN s.reps_effettive = 1 THEN s.peso_sollevato
                    ELSE s.peso_sollevato * (1.0 + CAST(s.reps_effettive AS REAL) / 30.0)
                END
            ), 0) AS max1rm
        FROM set_logs s
        INNER JOIN workout_sessions w ON s.session_id = w.id
        INNER JOIN exercises e ON e.id = s.exercise_id
        WHERE w.timestamp >= :startDate AND w.timestamp <= :endDate
        GROUP BY e.id
        ORDER BY volume DESC
    """)
    fun getPeriodExerciseBreakdown(startDate: Long, endDate: Long): Flow<List<PeriodExerciseRow>>

    @Query("""
        SELECT 
            COUNT(DISTINCT date(w.timestamp / 1000, 'unixepoch')) AS trainingDays
        FROM workout_sessions w
        WHERE w.timestamp >= :startDate AND w.timestamp <= :endDate AND w.is_finished = 1
    """)
    fun getTrainingDays(startDate: Long, endDate: Long): Flow<Int>
}

data class DailyExerciseMax(
    val maxValue: Float,
    val timestamp: Long
)

data class DailyVolume(
    val volume: Float,
    val timestamp: Long
)

data class PersonalBestRow(
    val exerciseId: Int,
    val exerciseName: String,
    val category: String,
    val maxWeight: Float,
    val reps: Int
)

data class ConsistencyRow(
    val completedSessions: Int,
    val targetSessionsPerWeek: Int
)

data class CategoryVolumeRow(
    val category: String,
    val volume: Float
)

data class PeriodMetrics(
    val volume: Float,
    val sessionCount: Int,
    val setCount: Int,
    val avgWeight: Float
)

data class PeriodExerciseRow(
    val exerciseName: String,
    val volume: Float,
    val setCount: Int,
    val maxWeight: Float,
    val max1rm: Float
)
