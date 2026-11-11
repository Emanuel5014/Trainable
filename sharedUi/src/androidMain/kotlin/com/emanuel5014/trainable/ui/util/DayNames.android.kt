package com.emanuel5014.trainable.ui.util

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import java.time.format.TextStyle
import java.util.Locale

actual fun narrowDayName(day: DayOfWeek): String =
    java.time.DayOfWeek.of(day.isoDayNumber).getDisplayName(TextStyle.NARROW, Locale.getDefault())

actual fun shortDayName(day: DayOfWeek): String =
    java.time.DayOfWeek.of(day.isoDayNumber).getDisplayName(TextStyle.SHORT, Locale.getDefault())

actual fun monthName(month: Int): String =
    java.time.Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

actual fun fullDate(timestampMillis: Long): String =
    java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL)
        .withLocale(Locale.getDefault())
        .format(java.time.Instant.ofEpochMilli(timestampMillis).atZone(java.time.ZoneId.systemDefault()))
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
