package com.emanuel5014.trainable.ui.screens.settings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal actual fun formatMediumDate(date: LocalDate): String =
    date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

internal actual fun formatGroupedWhole(value: Float): String = String.format(Locale.getDefault(), "%,.0f", value)
