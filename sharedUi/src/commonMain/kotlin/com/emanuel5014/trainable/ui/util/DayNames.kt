package com.emanuel5014.trainable.ui.util

import kotlinx.datetime.DayOfWeek

/** The one-letter name of [day] in the app's language ("L" for Monday in Italian). */
expect fun narrowDayName(day: DayOfWeek): String

/** The short name of [day] in the app's language, as the locale writes it ("lun", "Mon"). */
expect fun shortDayName(day: DayOfWeek): String

/** The standalone month name ([month] is 1..12), first letter uppercased ("Novembre"). */
expect fun monthName(month: Int): String

/**
 * The full date of [timestampMillis] in the device time zone, as the language writes it, first letter uppercased
 * ("Lunedì 2 novembre 2026", "Montag, 2. November 2026").
 */
expect fun fullDate(timestampMillis: Long): String
