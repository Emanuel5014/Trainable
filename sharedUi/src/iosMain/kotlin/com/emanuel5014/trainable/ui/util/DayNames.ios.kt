package com.emanuel5014.trainable.ui.util

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import platform.Foundation.NSDateFormatter

/** In the phone's language for now: the app's own language choice is not applied on iOS yet. */
actual fun narrowDayName(day: DayOfWeek): String {
    // The symbols start on Sunday; ISO numbers Monday 1 to Sunday 7
    val symbols = NSDateFormatter().veryShortWeekdaySymbols
    return symbols[day.isoDayNumber % 7] as String
}
