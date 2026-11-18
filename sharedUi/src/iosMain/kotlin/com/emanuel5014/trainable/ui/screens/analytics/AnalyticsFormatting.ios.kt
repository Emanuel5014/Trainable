package com.emanuel5014.trainable.ui.screens.analytics

import com.emanuel5014.trainable.platform.formatLocalDecimals
import platform.Foundation.NSDateFormatter

internal actual fun formatAnalyticsChange(value: Float, decimals: Int, percent: Boolean): String =
    (if (value.toRawBits() < 0) "" else "+") + formatLocalDecimals(value.toDouble(), decimals) +
        if (percent) "%" else ""

internal actual fun analyticsMonthTitle(year: Int, month: Int): String {
    val name = NSDateFormatter().monthSymbols[month - 1] as String
    return "${name.replaceFirstChar { it.titlecase() }} $year"
}
