package com.emanuel5014.trainable.ui.screens.analytics

import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.dao.CategoryVolumeRow
import java.util.concurrent.TimeUnit

enum class AnalyticsTimeRange(val labelResId: Int, private val durationDays: Long?) {
    OneWeek(R.string.analytics_time_range_1w, 7),
    OneMonth(R.string.analytics_time_range_1m, 30),
    SixMonths(R.string.analytics_time_range_6m, 180),
    All(R.string.analytics_time_range_all, null);

    fun startDate(now: Long = System.currentTimeMillis()): Long {
        return durationDays?.let { now - TimeUnit.DAYS.toMillis(it) } ?: 0L
    }
}

data class AnalyticsChartPoint(
    val timestamp: Long,
    val value: Float,
    val id: Int = -1
)

data class PersonalBestUiModel(
    val exerciseId: Int,
    val exerciseName: String,
    val category: String,
    val maxWeightKg: Float,
    val reps: Int
)

data class ConsistencyUiModel(
    val completedSessions: Int,
    val targetSessions: Int,
    val progress: Float,
    val summary: String
)

data class StrengthIndexUiModel(
    val percent: Float?,
    val summary: String
)


sealed class AnalyticsWidget(val id: String) {
    data class BodyWeight(
        val history: List<AnalyticsChartPoint>,
        val timeRange: AnalyticsTimeRange = AnalyticsTimeRange.OneMonth
    ) : AnalyticsWidget("weight")
    data class Calendar(val workoutDates: List<Long>) : AnalyticsWidget("calendar")
    data class Exercise(
        val exerciseId: Int,
        val exerciseName: String,
        val history: List<AnalyticsChartPoint>
    ) : AnalyticsWidget("exercise_$exerciseId")

    data class Volume(
        val widgetId: String,
        val planId: Int,
        val planName: String,
        val timeRange: AnalyticsTimeRange,
        val history: List<AnalyticsChartPoint>
    ) : AnalyticsWidget(widgetId)

    data class CategoryVolume(
        val history: List<CategoryVolumeRow>,
        val timeRange: AnalyticsTimeRange,
        val startDate: Long
    ) : AnalyticsWidget("category_volume")

    data class TimePeriodComparison(
        val period1Name: String,
        val period2Name: String,
        val period1DateRange: String,
        val period2DateRange: String,
        val period1Metrics: PeriodComparisonMetrics,
        val period2Metrics: PeriodComparisonMetrics,
        val period1Exercises: List<PeriodExerciseComparison>,
        val period2Exercises: List<PeriodExerciseComparison>,
        val summaryParts: List<SummaryPart> = emptyList(),
        val timeRange: AnalyticsTimeRange = AnalyticsTimeRange.OneMonth
    ) : AnalyticsWidget("time_period_comparison")
}

data class SummaryPart(
    val label: String,
    val deltaPercent: Float,
    val isPositive: Boolean
)

data class PeriodComparisonMetrics(
    val volume: Float,
    val sessionCount: Int,
    val setCount: Int,
    val avgWeight: Float,
    val trainingDays: Int
)

data class PeriodExerciseComparison(
    val exerciseName: String,
    val volume: Float,
    val setCount: Int,
    val maxWeight: Float,
    val max1rm: Float
)

data class AnalyticsUiState(
    val activePlanName: String = "No Active Plan",
    val selectedTimeRange: AnalyticsTimeRange = AnalyticsTimeRange.OneMonth,
    val totalVolumeKg: Float = 0f,
    val volumeHistory: List<AnalyticsChartPoint> = emptyList(),
    val consistency: ConsistencyUiModel = ConsistencyUiModel(0, 0, 0f, "No scheduled sessions yet."),
    val strengthIndex: StrengthIndexUiModel = StrengthIndexUiModel(null, "Not enough PR history yet."),
    val personalBests: List<PersonalBestUiModel> = emptyList(),
    val selectedExerciseIds: Set<Int> = emptySet(),
    val widgets: List<AnalyticsWidget> = emptyList(),
    val bodyWeightHistory: List<AnalyticsChartPoint> = emptyList(),
    val bodyWeightInput: String = "",
    val weightUnit: String = "kg",
    val workoutDates: List<Long> = emptyList(),
    val allPlans: List<WorkoutPlanEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showProgressCards: Boolean = false
)