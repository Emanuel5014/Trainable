package com.emanuel5014.trainable.ui.screens.dashboard

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.toKotlinDayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

actual fun firstDayOfWeek(): DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek.toKotlinDayOfWeek()
