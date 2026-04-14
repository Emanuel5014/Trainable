package com.example.gymtracking.data.repository

import com.example.gymtracking.data.local.dao.AnalyticsDao
import com.example.gymtracking.data.local.dao.CategoryVolumeRow
import com.example.gymtracking.data.local.dao.ConsistencyRow
import com.example.gymtracking.data.local.dao.DailyVolume
import com.example.gymtracking.data.local.dao.PersonalBestRow
import com.example.gymtracking.data.local.dao.WeightLogDao
import com.example.gymtracking.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val weightLogDao: WeightLogDao
) {
    fun getTotalVolume(): Flow<Float?> = analyticsDao.getTotalVolume()

    fun getTotalVolumeSince(startDate: Long): Flow<Float?> = analyticsDao.getTotalVolumeSince(startDate)
    
    fun getVolumeHistory(startDate: Long): Flow<List<DailyVolume>> = 
        analyticsDao.getVolumeHistory(startDate)

    fun getAllPersonalBests(): Flow<List<PersonalBestRow>> = analyticsDao.getAllPersonalBests()

    fun getConsistency(planId: Int, startDate: Long): Flow<ConsistencyRow?> =
        analyticsDao.getConsistency(planId, startDate)

    fun getStrengthIndex(startDate: Long): Flow<Float?> {
        val previousStartDate = startDate - TimeUnit.DAYS.toMillis(30)
        return analyticsDao.getStrengthIndex(startDate, previousStartDate)
    }

    fun getVolumeByCategory(startDate: Long): Flow<List<CategoryVolumeRow>> =
        analyticsDao.getVolumeByCategory(startDate)

    fun getPersonalBestForExercise(exerciseId: Int): Flow<Float?> = 
        analyticsDao.getPersonalBest(exerciseId)

    fun getExerciseProgressHistory(exerciseId: Int, startDate: Long) =
        analyticsDao.getExerciseProgressHistory(exerciseId, startDate)

    fun getWeightHistory(startDate: Long): Flow<List<WeightLogEntity>> =
        weightLogDao.getWeightHistory(startDate)

    suspend fun addWeightLog(userId: Int, peso: Float, timestamp: Long) {
        weightLogDao.insertWeight(
            WeightLogEntity(
                userId = userId,
                pesoCorporeo = peso,
                timestamp = timestamp
            )
        )
    }
}
