package com.emanuel5014.trainable.ui.screens.analytics

import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

internal actual fun formatAnalyticsChange(value: Float, decimals: Int, percent: Boolean): String =
    String.format("%+.${decimals}f" + if (percent) "%%" else "", value)

internal actual fun analyticsMonthTitle(year: Int, month: Int): String {
    val locale = Locale.getDefault()
    val name = Month.of(month).getDisplayName(TextStyle.FULL, locale)
    return "${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }} $year"
}
