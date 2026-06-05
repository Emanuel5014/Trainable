package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.dao.AnalyticsDao
import com.emanuel5014.trainable.data.local.dao.CategoryVolumeRow
import com.emanuel5014.trainable.data.local.dao.ConsistencyRow
import com.emanuel5014.trainable.data.local.dao.DailyVolume
import com.emanuel5014.trainable.data.local.dao.PeriodExerciseRow
import com.emanuel5014.trainable.data.local.dao.PeriodMetrics
import com.emanuel5014.trainable.data.local.dao.PersonalBestRow
import com.emanuel5014.trainable.data.local.dao.WeightLogDao
import com.emanuel5014.trainable.data.local.entity.WeightLogEntity
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

    fun getVolumeHistoryForPlan(planId: Int, startDate: Long): Flow<List<DailyVolume>> =
        analyticsDao.getVolumeHistoryForPlan(planId, startDate)

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

    fun getPeriodMetrics(startDate: Long, endDate: Long): Flow<PeriodMetrics> =
        analyticsDao.getPeriodMetrics(startDate, endDate)

    fun getPeriodExerciseBreakdown(startDate: Long, endDate: Long): Flow<List<PeriodExerciseRow>> =
        analyticsDao.getPeriodExerciseBreakdown(startDate, endDate)

    fun getTrainingDays(startDate: Long, endDate: Long): Flow<Int> =
        analyticsDao.getTrainingDays(startDate, endDate)

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
