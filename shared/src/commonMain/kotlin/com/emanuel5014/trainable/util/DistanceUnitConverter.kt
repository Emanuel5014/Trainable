package com.emanuel5014.trainable.util

import com.emanuel5014.trainable.domain.exercise.CardioHistory
import com.emanuel5014.trainable.domain.exercise.DurationDigits
import com.emanuel5014.trainable.platform.formatDecimals
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Distances are stored in km everywhere (cardio logs, routine targets, CSV); miles exist only on screen and when typed.
 * Any unit other than "mi" is km. Pace and speed keep the [CardioHistory.speedKmh] thresholds (100 m, one minute) in km.
 */
object DistanceUnitConverter {
    const val KM_PER_MILE = 1.609344f
    private const val KM_PER_MILE_EXACT = 1.609344

    const val KM = "km"
    const val MILES = "mi"

    fun isMiles(unit: String): Boolean = unit == MILES

    fun toDisplay(km: Float, unit: String): Float =
        if (isMiles(unit)) (km.toDouble() / KM_PER_MILE_EXACT).toFloat() else km

    fun toStorage(value: Float, unit: String): Float =
        if (isMiles(unit)) (value.toDouble() * KM_PER_MILE_EXACT).toFloat() else value

    fun unitLabel(unit: String): String = if (isMiles(unit)) MILES else KM

    fun speedLabel(unit: String): String = if (isMiles(unit)) "mph" else "km/h"

    /** [km] in [unit] the way cardio distances have always been written ("2.4", "10.0"), without float noise. */
    fun displayNumber(km: Float, unit: String): String =
        (round(toDisplay(km, unit) * 100.0) / 100.0).toString()

    /** "2.4 km", "3.11 mi". */
    fun format(km: Float, unit: String): String = "${displayNumber(km, unit)} ${unitLabel(unit)}"

    /** Seconds per km or per mile, null when the values tell no speed. */
    fun paceSeconds(seconds: Int, km: Float, unit: String): Int? =
        CardioHistory.speedKmh(seconds, km)?.let { (seconds / toDisplay(km, unit)).roundToInt() }

    /** Km/h or mph, null when the values tell no speed. */
    fun speed(seconds: Int, km: Float, unit: String): Float? =
        CardioHistory.speedKmh(seconds, km)?.let { toDisplay(it, unit) }

    /** "5:00 /km", "8:03 /mi". */
    fun formatPace(seconds: Int, km: Float, unit: String): String? =
        paceSeconds(seconds, km, unit)?.let { "${DurationDigits.format(it)} /${unitLabel(unit)}" }

    /** "12.0 km/h", "7.5 mph". */
    fun formatSpeed(seconds: Int, km: Float, unit: String): String? =
        speed(seconds, km, unit)?.let { "${formatDecimals(it.toDouble(), 1, deviceLocale = false)} ${speedLabel(unit)}" }
}
