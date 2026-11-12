package com.emanuel5014.trainable.domain.exercise

import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** What the cardio entry sheet types and shows besides the duration ([DurationDigits]): distance, pace and the moment. */
object CardioEntryValues {
    private const val MAX_INTEGER_DIGITS = 4
    private const val MAX_DECIMALS = 3

    /** The distance typed with a comma or a point; nothing typed is zero. */
    fun parseDistance(text: String): Float = text.trim().replace(',', '.').let { t ->
        when {
            t.isEmpty() || t == "." -> 0f
            t.startsWith(".") -> "0$t".toFloatOrNull() ?: 0f
            else -> t.toFloatOrNull() ?: 0f
        }
    }

    /**
     * [input] reduced to digits and the separator the user typed, or null when the change must be refused (a second
     * separator, more than [MAX_DECIMALS] decimals or [MAX_INTEGER_DIGITS] whole digits).
     */
    fun cleanDistanceInput(input: String): String? {
        val kept = input.filter { it.isDigit() || it == ',' || it == '.' }
        val separators = kept.count { it == ',' || it == '.' }
        if (separators > 1) return null
        val sepIndex = kept.indexOfFirst { it == ',' || it == '.' }
        val whole = if (sepIndex < 0) kept else kept.substring(0, sepIndex)
        val decimals = if (sepIndex < 0) "" else kept.substring(sepIndex + 1)
        if (whole.length > MAX_INTEGER_DIGITS || decimals.length > MAX_DECIMALS) return null
        return kept
    }

    /** [km] as it is typed again, with [separator] and without trailing zeros ("5,2", "10"); empty for zero. */
    fun distanceText(km: Float, separator: Char): String {
        val thousandths = (km.toDouble() * 1000).roundToLong()
        if (thousandths <= 0L) return ""
        val whole = thousandths / 1000
        val fraction = (thousandths % 1000).toString().padStart(MAX_DECIMALS, '0').trimEnd('0')
        return if (fraction.isEmpty()) whole.toString() else "$whole$separator$fraction"
    }

    /** Seconds per km, only when [CardioHistory.speedKmh] says the values tell a speed (100 m and one minute). */
    fun paceSecondsPerKm(seconds: Int, km: Float): Int? =
        CardioHistory.speedKmh(seconds, km)?.let { (seconds / km).roundToInt() }

    /**
     * The moment of a cardio done on the day the date picker gave ([pickerDateMillis], UTC midnight of that day) at
     * [hour]:[minute] in [zone]; never after [now] (a later time today is today at [now]).
     */
    fun timestampOf(pickerDateMillis: Long, hour: Int, minute: Int, zone: TimeZone, now: Long): Long {
        val day = Instant.fromEpochMilliseconds(pickerDateMillis).toLocalDateTime(TimeZone.UTC).date
        val chosen = LocalDateTime(day, LocalTime(hour, minute)).toInstant(zone).toEpochMilliseconds()
        return minOf(chosen, now)
    }

    /** The day of [timestamp] in [zone] the way the date picker holds it (UTC midnight of that day). */
    fun pickerDateOf(timestamp: Long, zone: TimeZone): Long =
        Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(zone).date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    /** True for a picker day ([pickerDateMillis], UTC midnight) that is not after today in [zone]. */
    fun isSelectableDay(pickerDateMillis: Long, now: Long, zone: TimeZone): Boolean =
        pickerDateMillis <= pickerDateOf(now, zone)
}
