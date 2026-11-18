package com.emanuel5014.trainable.ui.util

import com.emanuel5014.trainable.platform.IosAppLocale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterFullStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.dateWithTimeIntervalSince1970

/** In the app's language (like Android's per-app locale), the phone's own locale when none is chosen. */
actual fun narrowDayName(day: DayOfWeek): String {
    // The symbols start on Sunday; ISO numbers Monday 1 to Sunday 7
    val formatter = NSDateFormatter().apply { locale = IosAppLocale.formattingLocale() }
    return formatter.veryShortWeekdaySymbols[day.isoDayNumber % 7] as String
}

actual fun shortDayName(day: DayOfWeek): String {
    val formatter = NSDateFormatter().apply { locale = IosAppLocale.formattingLocale() }
    return formatter.shortWeekdaySymbols[day.isoDayNumber % 7] as String
}

actual fun monthName(month: Int): String {
    val formatter = NSDateFormatter().apply { locale = IosAppLocale.formattingLocale() }
    return (formatter.standaloneMonthSymbols[month - 1] as String).replaceFirstChar { it.titlecase() }
}

actual fun fullDate(timestampMillis: Long): String {
    val formatter = NSDateFormatter().apply {
        locale = IosAppLocale.formattingLocale()
        dateStyle = NSDateFormatterFullStyle
        timeStyle = NSDateFormatterNoStyle
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1000.0))
        .replaceFirstChar { it.titlecase() }
}
