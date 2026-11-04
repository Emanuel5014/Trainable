package com.emanuel5014.trainable.ui.util

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import java.time.format.TextStyle
import java.util.Locale

actual fun narrowDayName(day: DayOfWeek): String =
    java.time.DayOfWeek.of(day.isoDayNumber).getDisplayName(TextStyle.NARROW, Locale.getDefault())
