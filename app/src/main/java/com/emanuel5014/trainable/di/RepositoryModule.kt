package com.emanuel5014.trainable.di

import com.emanuel5014.trainable.data.local.dao.AnalyticsDao
import com.emanuel5014.trainable.data.local.dao.JournalDao
import com.emanuel5014.trainable.data.local.dao.OneRepMaxDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.dao.WeightLogDao
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.JournalRepository
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.repository.PlanRepository
import com.emanuel5014.trainable.data.repository.TransactionRunner
import com.emanuel5014.trainable.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The repositories of the shared module, which carry no injection annotations (iOS has no Hilt). */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAnalyticsRepository(analyticsDao: AnalyticsDao, weightLogDao: WeightLogDao): AnalyticsRepository =
        AnalyticsRepository(analyticsDao, weightLogDao)

    @Provides
    @Singleton
    fun provideJournalRepository(dao: JournalDao, transactions: TransactionRunner): JournalRepository =
        JournalRepository(dao, transactions)

    @Provides
    @Singleton
    fun providePlanRepository(workoutDao: WorkoutDao, userDao: UserDao, oneRepMaxDao: OneRepMaxDao): PlanRepository =
        PlanRepository(workoutDao, userDao, oneRepMaxDao)

    @Provides
    @Singleton
    fun provideOneRepMaxRepository(oneRepMaxDao: OneRepMaxDao): OneRepMaxRepository = OneRepMaxRepository(oneRepMaxDao)

    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao, weightLogDao: WeightLogDao): UserRepository =
        UserRepository(userDao, weightLogDao)
}
