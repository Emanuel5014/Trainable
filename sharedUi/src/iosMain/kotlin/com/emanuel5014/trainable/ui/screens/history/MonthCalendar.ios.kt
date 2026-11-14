package com.emanuel5014.trainable.ui.screens.history

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import platform.Foundation.NSDateFormatter
import kotlin.time.Clock
import kotlin.time.Instant

actual object MonthCalendar {
    private val zone get() = TimeZone.currentSystemDefault()

    private fun lastDay(monthIndex: Int, year: Int): LocalDate =
        LocalDate(year, monthIndex + 1, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

    actual fun currentMonthAndYear(): Pair<Int, Int> {
        val today = Clock.System.todayIn(zone)
        return today.month.ordinal to today.year
    }

    actual fun monthRange(monthIndex: Int, year: Int): Pair<Long, Long> {
        val start = LocalDate(year, monthIndex + 1, 1).atStartOfDayIn(zone).toEpochMilliseconds()
        val last = lastDay(monthIndex, year)
        val end = LocalDateTime(last.year, last.month, last.day, 23, 59, 59, 999_000_000).toInstant(zone).toEpochMilliseconds()
        return start to end
    }

    actual fun isWholeMonth(start: Long, end: Long, monthIndex: Int, year: Int): Boolean {
        val s = Instant.fromEpochMilliseconds(start).toLocalDateTime(zone).date
        val e = Instant.fromEpochMilliseconds(end).toLocalDateTime(zone).date
        return s == LocalDate(year, monthIndex + 1, 1) && e == lastDay(monthIndex, year)
    }

    // In the app's language, like the other dates on iOS (not the phone's)
    actual fun shortMonthNames(): List<String> = NSDateFormatter()
        .apply { locale = com.emanuel5014.trainable.platform.IosAppLocale.formattingLocale() }
        .shortMonthSymbols.map { it as String }
}
