package com.emanuel5014.trainable.ui.screens.dashboard

import kotlinx.datetime.DayOfWeek
import platform.Foundation.NSCalendar

// NSCalendar counts 1 = Sunday ... 7 = Saturday
actual fun firstDayOfWeek(): DayOfWeek = DayOfWeek(((NSCalendar.currentCalendar.firstWeekday.toInt() + 5) % 7) + 1)
