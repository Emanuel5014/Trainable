package com.emanuel5014.trainable.ui.screens.analytics

// In the shared module because the exercise history of the domain builds its chart from these points
data class AnalyticsChartPoint(
    val timestamp: Long,
    val value: Float,
    val id: Int = -1
)
