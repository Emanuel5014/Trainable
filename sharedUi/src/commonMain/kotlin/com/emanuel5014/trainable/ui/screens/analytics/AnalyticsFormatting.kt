package com.emanuel5014.trainable.ui.screens.analytics

/** Signed change in the device locale, preserving Android's existing Formatter output. */
internal expect fun formatAnalyticsChange(value: Float, decimals: Int = 1, percent: Boolean = true): String

/** Full month name, capitalized in the device locale, followed by the year. */
internal expect fun analyticsMonthTitle(year: Int, month: Int): String
