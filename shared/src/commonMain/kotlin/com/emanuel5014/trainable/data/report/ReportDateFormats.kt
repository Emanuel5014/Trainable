package com.emanuel5014.trainable.data.report

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The report's dates in its own language (not the device's): a day in the medium style, a moment with its time. */
internal expect class ReportDateFormats(languageCode: String) {
    fun day(millis: Long): String
    fun moment(millis: Long): String
}

private fun local(millis: Long) = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())

private fun Int.pad2() = toString().padStart(2, '0')

/** "2026-11-05", for the datetime attribute. */
internal fun ReportDateFormats.isoDay(millis: Long): String = local(millis).date.toString()

/** "2026-11-05T09:30", for the datetime attribute. */
internal fun ReportDateFormats.isoMoment(millis: Long): String =
    local(millis).let { "${it.date}T${it.hour.pad2()}:${it.minute.pad2()}" }
