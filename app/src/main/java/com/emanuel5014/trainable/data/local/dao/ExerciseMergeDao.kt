package com.emanuel5014.trainable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.emanuel5014.trainable.data.local.entity.ExerciseMergeEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseMergeMoveEntity
import kotlinx.coroutines.flow.Flow

/** The merge log and the SQL that merges two exercises and undoes it. */
@Dao
interface ExerciseMergeDao {
    @Insert
    suspend fun insertMerge(merge: ExerciseMergeEntity): Long

    @Query("SELECT * FROM exercise_merges ORDER BY id")
    suspend fun getAllMerges(): List<ExerciseMergeEntity>

    @Query("SELECT * FROM exercise_merges ORDER BY id")
    fun observeAllMerges(): Flow<List<ExerciseMergeEntity>>

    @Query("SELECT * FROM exercise_merge_moves WHERE merge_id = :mergeId ORDER BY id")
    suspend fun getMoves(mergeId: Int): List<ExerciseMergeMoveEntity>
}
