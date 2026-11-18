package com.emanuel5014.trainable.ui.screens.analytics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.ExerciseMergeRepository
import com.emanuel5014.trainable.data.repository.KeyValueStore
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** The shared [AnalyticsState] kept for the screen's lifetime, with the app's resources, choices file and language. */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    analyticsRepository: AnalyticsRepository,
    workoutRepository: WorkoutRepository,
    userPreferencesRepository: UserPreferencesRepository,
    localeManager: AppLocaleManager,
    mergeRepository: ExerciseMergeRepository,
    analyticsPrefs: KeyValueStore,
    @ApplicationContext context: Context
) : ViewModel() {

    val state = AnalyticsState(
        analyticsRepository, workoutRepository, userPreferencesRepository, mergeRepository, analyticsPrefs,
        AndroidAnalyticsText(context),
        localeManager.currentLanguage.map { localeManager.resolveLanguageForCompose(it) },
        viewModelScope
    )
}

/** The analytics words from the app's resources, as the state always wrote them. */
private class AndroidAnalyticsText(private val context: Context) : AnalyticsText {
    override val compareVolume: String get() = context.getString(R.string.compare_volume)
    override val compareSessions: String get() = context.getString(R.string.compare_sessions)
    override val trainingDays: String get() = context.getString(R.string.analytics_training_days)
    override val thisWeek: String get() = context.getString(R.string.this_week)
    override val thisMonth: String get() = context.getString(R.string.this_month)
    override val previous: String get() = context.getString(R.string.previous_exercise)
    override fun timeRange(range: AnalyticsTimeRange): String = context.getString(range.labelRes())
    override val consistencyNoSessions: String get() = context.getString(R.string.analytics_consistency_no_sessions)
    override fun consistencySummary(completed: Int, expected: Int): String =
        context.getString(R.string.analytics_consistency_summary, completed, expected)
    override val strengthNoData: String get() = context.getString(R.string.analytics_strength_no_data)
    override fun strengthPositive(value: String): String = context.getString(R.string.analytics_strength_summary_positive, value)
    override fun strengthNegative(value: String): String = context.getString(R.string.analytics_strength_summary_negative, value)
    override fun shortDate(timestampMillis: Long): String =
        java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(java.util.Date(timestampMillis))
}
