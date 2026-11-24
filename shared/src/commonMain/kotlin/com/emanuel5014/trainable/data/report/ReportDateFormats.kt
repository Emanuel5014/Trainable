package com.emanuel5014.trainable.data.report

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The report's dates in its own language (not the device's): a day in the medium style, a moment with its time. */
internal expect class ReportDateFormats(languageCode: String) {
    fun day(millis: Long): String
    fun moment(millis: Long): String
    /** "Tue", "mar": the short name of the day of the week. */
    fun weekday(millis: Long): String
    /** "November 2026", "novembre 2026": a month on its own, as a heading. */
    fun monthYear(millis: Long): String
}

private fun local(millis: Long) = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())

/** The day of the month (1-31) in the device's time zone. */
internal fun ReportDateFormats.dayOfMonth(millis: Long): Int = local(millis).date.day

/** Year and month as one number (202611), to group the workouts of a month. */
internal fun ReportDateFormats.monthKey(millis: Long): Int = local(millis).date.let { it.year * 100 + it.month.ordinal + 1 }

private fun Int.pad2() = toString().padStart(2, '0')

/** "2026-11-05", for the datetime attribute. */
internal fun ReportDateFormats.isoDay(millis: Long): String = local(millis).date.toString()

/** "2026-11-05T09:30", for the datetime attribute. */
internal fun ReportDateFormats.isoMoment(millis: Long): String =
    local(millis).let { "${it.date}T${it.hour.pad2()}:${it.minute.pad2()}" }
