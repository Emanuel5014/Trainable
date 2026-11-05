package com.emanuel5014.trainable.di

import android.content.Context
import com.emanuel5014.trainable.data.local.dao.AnalyticsDao
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.dao.ExerciseMergeDao
import com.emanuel5014.trainable.data.local.dao.JournalDao
import com.emanuel5014.trainable.data.local.dao.OneRepMaxDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.dao.WeightLogDao
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.ExerciseMergeRepository
import com.emanuel5014.trainable.data.repository.KeyValueStore
import com.emanuel5014.trainable.data.repository.JournalRepository
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.repository.PlanRepository
import com.emanuel5014.trainable.data.repository.TransactionRunner
import com.emanuel5014.trainable.data.repository.HistoryCsvImporter
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The repositories of the shared module, which carry no injection annotations (iOS has no Hilt). */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePhysicalCheckRepository(
        dao: com.emanuel5014.trainable.data.local.dao.PhysicalCheckDao,
        crypto: com.emanuel5014.trainable.util.PhysicalCheckCryptoManager,
        @ApplicationContext context: Context,
        preferences: UserPreferencesRepository
    ): com.emanuel5014.trainable.data.repository.PhysicalCheckRepository =
        com.emanuel5014.trainable.data.repository.PhysicalCheckRepository(dao, crypto, AndroidPhysicalCheckFiles(context), preferences)

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
    fun provideWorkoutRepository(
        workoutDao: WorkoutDao,
        userDao: UserDao,
        exerciseDao: ExerciseDao,
        oneRepMaxDao: OneRepMaxDao,
        userPreferencesRepository: UserPreferencesRepository,
        @ApplicationContext context: Context
    ): WorkoutRepository = WorkoutRepository(
        workoutDao, userDao, exerciseDao, oneRepMaxDao, userPreferencesRepository, AndroidWorkoutPlatform(context)
    )

    /** The report in the language the app shows, with its own resources (as before the report moved to the shared module). */
    @Provides
    fun provideReportGenerator(
        workoutRepository: WorkoutRepository,
        userPreferencesRepository: UserPreferencesRepository,
        localeManager: com.emanuel5014.trainable.util.AppLocaleManager
    ): com.emanuel5014.trainable.data.report.ReportGenerator =
        com.emanuel5014.trainable.data.report.ReportGenerator(workoutRepository, userPreferencesRepository) {
            AndroidReportText(localeManager.localizedContext())
        }

    @Provides
    fun provideHtmlReportFormatter(): com.emanuel5014.trainable.data.report.HtmlReportFormatter =
        com.emanuel5014.trainable.data.report.HtmlReportFormatter()

    @Provides
    @Singleton
    fun provideHistoryCsvImporter(
        transactions: TransactionRunner,
        workoutDao: WorkoutDao,
        exerciseDao: ExerciseDao,
        userDao: UserDao,
        journalRepository: JournalRepository,
        @ApplicationContext context: Context
    ): HistoryCsvImporter = HistoryCsvImporter(
        transactions, workoutDao, exerciseDao, userDao, journalRepository, AndroidWorkoutPlatform(context)
    )

    @Provides
    @Singleton
    fun provideExerciseRepository(exerciseDao: ExerciseDao, @ApplicationContext context: Context): ExerciseRepository =
        ExerciseRepository(exerciseDao, AndroidExerciseMediaFiles(context))

    /** The analytics choices, in the "analytics_prefs" file they always lived in. */
    @Provides
    @Singleton
    fun provideAnalyticsPrefs(@ApplicationContext context: Context): KeyValueStore =
        SharedPreferencesStore(context, "analytics_prefs")

    @Provides
    @Singleton
    fun provideExerciseMergeRepository(
        mergeDao: ExerciseMergeDao,
        exerciseDao: ExerciseDao,
        analyticsPrefs: KeyValueStore
    ): ExerciseMergeRepository = ExerciseMergeRepository(mergeDao, exerciseDao, analyticsPrefs)

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
