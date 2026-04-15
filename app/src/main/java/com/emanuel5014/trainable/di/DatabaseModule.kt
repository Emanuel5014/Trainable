package com.emanuel5014.trainable.di

import android.content.Context
import com.emanuel5014.trainable.data.local.GymDatabase
import com.emanuel5014.trainable.data.local.dao.AnalyticsDao
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WeightLogDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GymDatabase {
        return GymDatabase.getDatabase(context)
    }

    @Provides
    fun provideUserDao(database: GymDatabase): UserDao = database.userDao()

    @Provides
    fun provideExerciseDao(database: GymDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideWorkoutDao(database: GymDatabase): WorkoutDao = database.workoutDao()

    @Provides
    fun provideAnalyticsDao(database: GymDatabase): AnalyticsDao = database.analyticsDao()

    @Provides
    fun provideWeightLogDao(database: GymDatabase): WeightLogDao = database.weightLogDao()
}
