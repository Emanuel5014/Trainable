package com.emanuel5014.trainable.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

/** Start of the week [day] is in (midnight of the latest [firstDay] on or before it, in [zone]): the dashboard and the widgets count from here. */
fun weekStartDate(day: LocalDate, firstDay: DayOfWeek): LocalDate =
    day.minus((day.dayOfWeek.isoDayNumber - firstDay.isoDayNumber + 7) % 7, DateTimeUnit.DAY)

fun weekStartMillis(day: LocalDate, firstDay: DayOfWeek, zone: TimeZone): Long =
    weekStartDate(day, firstDay).atStartOfDayIn(zone).toEpochMilliseconds()
