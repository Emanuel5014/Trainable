package com.example.gymtracking.data.repository

import com.example.gymtracking.data.local.dao.UserDao
import com.example.gymtracking.data.local.dao.WeightLogDao
import com.example.gymtracking.data.local.entity.UserEntity
import com.example.gymtracking.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val weightLogDao: WeightLogDao
) {
    val currentUser: Flow<UserEntity?> = userDao.getUser()

    fun weightHistory(startDate: Long = 0L): Flow<List<WeightLogEntity>> =
        weightLogDao.getWeightHistory(startDate)

    suspend fun saveUser(user: UserEntity) {
        // Just use insertUser which has REPLACE strategy
        userDao.insertUser(user)
    }

    suspend fun addWeightLog(peso: Float, data: Long, userId: Int) {
        weightLogDao.insertWeight(
            WeightLogEntity(userId = userId, pesoCorporeo = peso, timestamp = data)
        )
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }
}
