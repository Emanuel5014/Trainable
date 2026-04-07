package com.example.gymtracking.ui.screens.analytics

import java.util.concurrent.TimeUnit

enum class AnalyticsTimeRange(val label: String, private val durationDays: Long?) {
    OneWeek("1W", 7),
    OneMonth("1M", 30),
    SixMonths("6M", 180),
    All("ALL", null);

    fun startDate(now: Long = System.currentTimeMillis()): Long {
        return durationDays?.let { now - TimeUnit.DAYS.toMillis(it) } ?: 0L
    }
}

data class AnalyticsChartPoint(
    val timestamp: Long,
    val value: Float
)

data class PersonalBestUiModel(
    val exerciseId: Int,
    val exerciseName: String,
    val category: String,
    val maxWeightKg: Float
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


data class AnalyticsUiState(
    val activePlanName: String = "No Active Plan",
    val selectedTimeRange: AnalyticsTimeRange = AnalyticsTimeRange.OneMonth,
    val totalVolumeKg: Float = 0f,
    val volumeHistory: List<AnalyticsChartPoint> = emptyList(),
    val consistency: ConsistencyUiModel = ConsistencyUiModel(0, 0, 0f, "No scheduled sessions yet."),
    val strengthIndex: StrengthIndexUiModel = StrengthIndexUiModel(null, "Not enough PR history yet."),
    val personalBests: List<PersonalBestUiModel> = emptyList(),
    val bodyWeightHistory: List<AnalyticsChartPoint> = emptyList(),
    val bodyWeightInput: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)