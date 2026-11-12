package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.emanuel5014.trainable.platform.formatLocalDecimals
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.cardio_dist_value
import com.emanuel5014.trainable.sharedui.resources.cardio_speed_value
import com.emanuel5014.trainable.sharedui.resources.mi
import com.emanuel5014.trainable.sharedui.resources.mph
import com.emanuel5014.trainable.util.DistanceUnitConverter
import org.jetbrains.compose.resources.stringResource

/**
 * The distance unit the user reads and types in ("km" or "mi"), provided by the app from the preference. Storage stays
 * km: everything here only converts for the screen, through [DistanceUnitConverter].
 */
val LocalDistanceUnit = compositionLocalOf { DistanceUnitConverter.KM }

/** "km", or "mi" as the language writes it. */
@Composable
fun distanceUnitLabel(unit: String = LocalDistanceUnit.current): String =
    if (DistanceUnitConverter.isMiles(unit)) stringResource(Res.string.mi) else DistanceUnitConverter.KM

/** [km] the way cardio distances have always been written ("2.4 km"), or in miles ("1.49 mi"). */
@Composable
fun distanceValueText(km: Float, unit: String = LocalDistanceUnit.current): String =
    if (DistanceUnitConverter.isMiles(unit)) {
        DistanceUnitConverter.displayNumber(km, unit) + " " + stringResource(Res.string.mi)
    } else {
        stringResource(Res.string.cardio_dist_value, cardioKmText(km))
    }

/** [km] with one decimal in the device's format and the unit ("2,4 km", "1,5 mi"), as History writes it. */
@Composable
fun distanceLocalText(km: Float, unit: String = LocalDistanceUnit.current): String =
    formatLocalDecimals(DistanceUnitConverter.toDisplay(km, unit).toDouble(), 1) + " " + distanceUnitLabel(unit)

/** [kmh] as "12,0 km/h" or "7,5 mph" (one decimal in the device's format). */
@Composable
fun speedValueText(kmh: Float, unit: String = LocalDistanceUnit.current): String {
    val number = formatLocalDecimals(DistanceUnitConverter.toDisplay(kmh, unit).toDouble(), 1)
    return if (DistanceUnitConverter.isMiles(unit)) {
        number + " " + stringResource(Res.string.mph)
    } else {
        stringResource(Res.string.cardio_speed_value, number)
    }
}

/** A routine's distance target as it has always been written in km ("5.0 km"), or converted ("3.11 mi"). */
@Composable
fun distanceTargetText(km: Float, unit: String = LocalDistanceUnit.current): String =
    if (DistanceUnitConverter.isMiles(unit)) distanceValueText(km, unit) else stringResource(Res.string.cardio_dist_value, km.toString())
