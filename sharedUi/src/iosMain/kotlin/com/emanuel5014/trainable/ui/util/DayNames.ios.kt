package com.emanuel5014.trainable.ui.util

import com.emanuel5014.trainable.platform.IosAppLocale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import platform.Foundation.NSDateFormatter

/** In the app's language (like Android's per-app locale), the phone's own locale when none is chosen. */
actual fun narrowDayName(day: DayOfWeek): String {
    // The symbols start on Sunday; ISO numbers Monday 1 to Sunday 7
    val formatter = NSDateFormatter().apply { locale = IosAppLocale.formattingLocale() }
    return formatter.veryShortWeekdaySymbols[day.isoDayNumber % 7] as String
}
