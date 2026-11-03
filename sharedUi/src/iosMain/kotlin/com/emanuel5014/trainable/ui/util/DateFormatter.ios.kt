package com.emanuel5014.trainable.ui.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

actual object DateFormatter {
    actual fun format(timestamp: Long): String = day(timestamp).let { "${it.day.pad()}/${(it.month.ordinal + 1).pad()}/${it.year}" }

    actual fun formatShort(timestamp: Long): String = day(timestamp).let { "${it.day.pad()}/${(it.month.ordinal + 1).pad()}" }

    private fun day(timestamp: Long): LocalDate =
        Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault()).date

    private fun Int.pad() = toString().padStart(2, '0')
}
