package com.emanuel5014.trainable.ui.screens.analytics

import androidx.annotation.StringRes
import com.emanuel5014.trainable.R

/** The name of the range in the app's resources, for the texts the ViewModel writes outside of composition. */
@StringRes
fun AnalyticsTimeRange.labelRes(): Int = when (this) {
    AnalyticsTimeRange.OneWeek -> R.string.analytics_time_range_1w
    AnalyticsTimeRange.OneMonth -> R.string.analytics_time_range_1m
    AnalyticsTimeRange.SixMonths -> R.string.analytics_time_range_6m
    AnalyticsTimeRange.All -> R.string.analytics_time_range_all
}
