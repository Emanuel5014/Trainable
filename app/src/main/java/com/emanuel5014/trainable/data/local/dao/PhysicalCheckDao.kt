package com.emanuel5014.trainable.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhysicalCheckDao {
    @Query("SELECT * FROM physical_checks ORDER BY timestamp DESC")
    fun getAllPhysicalChecks(): Flow<List<PhysicalCheckEntity>>

    @Query("SELECT * FROM physical_checks WHERE id = :id LIMIT 1")
    suspend fun getPhysicalCheckById(id: Int): PhysicalCheckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhysicalCheck(check: PhysicalCheckEntity): Long

    @Update
    suspend fun updatePhysicalCheck(check: PhysicalCheckEntity)

    @Delete
    suspend fun deletePhysicalCheck(check: PhysicalCheckEntity)

    @Query("DELETE FROM physical_checks")
    suspend fun deleteAll()
}
