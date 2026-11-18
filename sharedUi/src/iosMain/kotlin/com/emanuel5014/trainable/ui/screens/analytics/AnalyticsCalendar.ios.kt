package com.emanuel5014.trainable.ui.screens.analytics

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

actual object AnalyticsCalendar {
    private val zone get() = TimeZone.currentSystemDefault()
    private fun today() = Clock.System.todayIn(zone)

    actual fun startOfWeek(): Long {
        val today = today()
        return today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
    }

    actual fun startOfMonth(): Long {
        val today = today()
        return LocalDate(today.year, today.month, 1).atStartOfDayIn(zone).toEpochMilliseconds()
    }

    actual fun startOfSixMonths(): Long {
        val first = today().let { LocalDate(it.year, it.month, 1) }.minus(5, DateTimeUnit.MONTH)
        return first.atStartOfDayIn(zone).toEpochMilliseconds()
    }
}
