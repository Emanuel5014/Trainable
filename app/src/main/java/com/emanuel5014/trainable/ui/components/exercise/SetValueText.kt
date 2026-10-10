package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.emanuel5014.trainable.R
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
    if (kind == SetLogKind.TIME_AND_REPS) return stringResource(R.string.time_reps_value, seconds ?: 0, reps)
    val load = WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(weightKg, weightUnit), weightUnit)
    val value = if (seconds != null) "${seconds}s" else repsText ?: "$reps"
    return "$load × $value"
}

@Composable
fun setValueText(set: SetLogEntity, weightUnit: String): String =
    setValueText(set.kind, set.pesoSollevato, set.repsEffettive, set.durataSecondi, weightUnit)
