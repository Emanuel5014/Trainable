package com.example.gymtracking.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymtracking.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(entry: WeightLogEntity)

    @Query(
        """
        SELECT *
        FROM weight_logs
        WHERE timestamp >= :startDate
        ORDER BY timestamp ASC
        """
    )
    fun getWeightHistory(startDate: Long): Flow<List<WeightLogEntity>>
}