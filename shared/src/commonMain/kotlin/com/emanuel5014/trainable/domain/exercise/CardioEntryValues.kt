package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.util.DistanceUnitConverter
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

    /** A distance ([km], or miles already converted) as it is typed again, with [separator] and without trailing zeros ("5,2", "10"); empty for zero. */
    fun distanceText(km: Float, separator: Char): String {
        val thousandths = (km.toDouble() * 1000).roundToLong()
        if (thousandths <= 0L) return ""
        val whole = thousandths / 1000
        val fraction = (thousandths % 1000).toString().padStart(MAX_DECIMALS, '0').trimEnd('0')
        return if (fraction.isEmpty()) whole.toString() else "$whole$separator$fraction"
    }

    /**
     * The start text of a plain distance field (the workout's cardio dialogs): [km] as those fields have always shown it
     * ("5.0"), or in miles with up to three decimals ("3.107"); empty for no distance.
     */
    fun fieldText(km: Float, unit: String): String = when {
        km <= 0f -> ""
        DistanceUnitConverter.isMiles(unit) -> distanceText(DistanceUnitConverter.toDisplay(km, unit), '.')
        else -> km.toString()
    }

    /**
     * The km of a plain distance field opened on [initialKm] (0 for none) in [unit]: still showing [fieldText] it gives
     * [initialKm] back exactly, anything typed is read in [unit] and turned into km.
     */
    fun fieldKm(text: String, initialKm: Float, unit: String): Float =
        if (initialKm > 0f && text == fieldText(initialKm, unit)) initialKm
        else DistanceUnitConverter.toStorage(parseDistance(text), unit)

    /** The duration and distance a save gives back; [unchanged] when both are still the ones the sheet opened with. */
    data class Read(val seconds: Int, val km: Float, val unchanged: Boolean)

    /**
     * What the tiles hold: [digits] and [distanceText] as typed, the distance in [unit] ("km" or "mi"). A value still
     * showing the text it was opened with ([initialSeconds], [initialKm], null when adding) is given back exactly as
     * stored, so a set saved only for its note keeps 5.2345 km or a duration over 99:59:59 instead of the rounded or
     * capped text, and 5 km shown as 3.107 mi never comes back as 5.0004 km. A typed distance becomes km.
     */
    fun read(
        digits: String,
        distanceText: String,
        initialSeconds: Int?,
        initialKm: Float?,
        separator: Char,
        unit: String = DistanceUnitConverter.KM
    ): Read {
        val sameSeconds = initialSeconds != null && digits == DurationDigits.digitsOf(initialSeconds)
        val sameKm = initialKm != null && distanceText == distanceText(DistanceUnitConverter.toDisplay(initialKm, unit), separator)
        return Read(
            seconds = if (sameSeconds) initialSeconds!! else DurationDigits.seconds(digits),
            km = if (sameKm) initialKm!! else DistanceUnitConverter.toStorage(parseDistance(distanceText), unit),
            unchanged = sameSeconds && sameKm
        )
    }

    /**
     * True when saving must ask for a duration or a distance: neither is there, unless a set is being [editing] with both
     * values as they were (a stored 0/0 set can still have its note or RPE changed).
     */
    fun isMissing(read: Read, editing: Boolean): Boolean =
        !(editing && read.unchanged) && read.seconds <= 0 && read.km <= 0f

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
