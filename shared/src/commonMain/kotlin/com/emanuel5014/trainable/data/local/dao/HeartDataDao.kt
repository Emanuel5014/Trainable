package com.emanuel5014.trainable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emanuel5014.trainable.data.local.entity.SessionHeartDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HeartDataDao {
    @Query("SELECT * FROM session_heart_data WHERE session_id = :sessionId")
    fun observe(sessionId: Int): Flow<SessionHeartDataEntity?>

    @Query("SELECT * FROM session_heart_data WHERE session_id = :sessionId")
    suspend fun get(sessionId: Int): SessionHeartDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(data: SessionHeartDataEntity)

    @Query("DELETE FROM session_heart_data WHERE session_id = :sessionId")
    suspend fun delete(sessionId: Int)
}
