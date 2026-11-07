package com.emanuel5014.trainable.ui.screens.dashboard

import com.emanuel5014.trainable.platform.IosAppLocale
import kotlinx.datetime.DayOfWeek
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian

// Like Android's WeekFields.of(Locale.getDefault()): follows the app's language when one is chosen.
// NSCalendar counts 1 = Sunday ... 7 = Saturday
actual fun firstDayOfWeek(): DayOfWeek {
    val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
        ?: NSCalendar.currentCalendar
    calendar.locale = IosAppLocale.formattingLocale()
    return DayOfWeek(((calendar.firstWeekday.toInt() + 5) % 7) + 1)
}
