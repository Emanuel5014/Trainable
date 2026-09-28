package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.dao.EstimatedOneRepMaxRow
import com.emanuel5014.trainable.data.local.dao.OneRepMaxDao
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.data.local.relation.OneRepMaxWithExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OneRepMaxRepository @Inject constructor(
    private val oneRepMaxDao: OneRepMaxDao
) {
    fun current(exerciseId: Int): Flow<OneRepMaxEntity?> = oneRepMaxDao.getCurrent(exerciseId)

    suspend fun currentOnce(exerciseId: Int): OneRepMaxEntity? = oneRepMaxDao.getCurrentOnce(exerciseId)

    fun history(exerciseId: Int): Flow<List<OneRepMaxEntity>> = oneRepMaxDao.getHistory(exerciseId)

    /** exerciseId → current 1RM in kg. */
    fun currentByExercise(): Flow<Map<Int, OneRepMaxEntity>> =
        oneRepMaxDao.getAllCurrent().map { list -> list.associateBy { it.exerciseId } }

    fun allWithExercise(): Flow<List<OneRepMaxWithExercise>> = oneRepMaxDao.getAllWithExercise()

    /** exerciseId → best Epley e1RM from logs since [sinceTimestamp] (default: last 12 weeks). */
    fun estimatedByExercise(sinceTimestamp: Long = System.currentTimeMillis() - TWELVE_WEEKS_MS): Flow<Map<Int, Float>> =
        oneRepMaxDao.getEstimatedOneRepMaxes(sinceTimestamp).map { rows: List<EstimatedOneRepMaxRow> ->
            rows.associate { it.exerciseId to it.e1rm }
        }

    suspend fun add(
        exerciseId: Int,
        weightKg: Float,
        date: Long = System.currentTimeMillis(),
        source: String = OneRepMaxEntity.SOURCE_MANUAL,
        note: String? = null
    ): Long = oneRepMaxDao.insert(OneRepMaxEntity(exerciseId = exerciseId, weightKg = weightKg, date = date, source = source, note = note))

    suspend fun delete(entry: OneRepMaxEntity) = oneRepMaxDao.delete(entry)

    companion object {
        private const val TWELVE_WEEKS_MS = 12L * 7 * 24 * 60 * 60 * 1000
    }
}
