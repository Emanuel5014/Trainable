package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.cardio_dist_value
import com.emanuel5014.trainable.sharedui.resources.time_reps_value
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.kind
import com.emanuel5014.trainable.util.WeightUnitConverter

/**
 * A set as the app writes it: "100 kg × 8", "20 kg × 45s" for a hold, "45s · 20 reps" for a Time & Reps interval
 * (which has no weight). [repsText] stands in for the reps of a plain set (e.g. "MAX").
 */
@Composable
fun setValueText(
    kind: SetLogKind,
    weightKg: Float,
    reps: Int,
    seconds: Int?,
    weightUnit: String,
    repsText: String? = null
): String {
    if (kind == SetLogKind.TIME_AND_REPS) return stringResource(Res.string.time_reps_value, seconds ?: 0, reps)
    val load = WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(weightKg, weightUnit), weightUnit)
    val value = if (seconds != null) "${seconds}s" else repsText ?: "$reps"
    return "$load × $value"
}

/** Kilometres the way the cardio logs have always been written ("2.4"), without float noise from sums. */
fun cardioKmText(km: Float): String = (kotlin.math.round(km * 100.0) / 100.0).toString()

/** A cardio set as the app writes it: "10:00 · 2.5 km", or just the time without a distance. */
@Composable
fun cardioSetValueText(set: CardioLogEntity): String {
    val h = set.durataSecondi / 3600
    val m = (set.durataSecondi % 3600) / 60
    val s = set.durataSecondi % 60
    fun twoDigits(value: Int) = value.toString().padStart(2, '0')
    val time = if (h > 0) "$h:${twoDigits(m)}:${twoDigits(s)}" else "${twoDigits(m)}:${twoDigits(s)}"
    return if (set.distanza > 0f) time + " · " + stringResource(Res.string.cardio_dist_value, cardioKmText(set.distanza)) else time
}

@Composable
fun setValueText(set: SetLogEntity, weightUnit: String): String =
    setValueText(set.kind, set.pesoSollevato, set.repsEffettive, set.durataSecondi, weightUnit)
