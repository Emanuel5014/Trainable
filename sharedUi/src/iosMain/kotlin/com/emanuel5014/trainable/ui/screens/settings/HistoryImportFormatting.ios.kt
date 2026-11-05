package com.emanuel5014.trainable.ui.screens.settings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.dateWithTimeIntervalSince1970

internal actual fun formatMediumDate(date: LocalDate): String {
    // Noon, so no time-zone shift can move it to another day
    val instant = date.atTime(LocalTime(12, 0)).toInstant(TimeZone.currentSystemDefault())
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble()))
}

internal actual fun formatGroupedWhole(value: Float): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        maximumFractionDigits = 0u
    }
    return formatter.stringFromNumber(NSNumber(double = value.toDouble())) ?: value.toLong().toString()
}
