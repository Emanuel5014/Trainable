package com.emanuel5014.trainable.ui.components

import android.content.Context
import androidx.annotation.StringRes
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.rpe.RpeLevel
import com.emanuel5014.trainable.domain.rpe.RpeScale

/** An RPE as a badge in the app: the level name on the simplified scale ("Hard"), "RPE 8" on the others. */
fun rpeBadgeText(context: Context, value: Float, scale: RpeScale): String =
    if (scale == RpeScale.BASIC) context.getString(RpeLevel.of(value).nameRes())
    else context.getString(R.string.rpe_short, PrescriptionFormatter.number(value))

@StringRes
fun RpeLevel.nameRes(): Int = when (this) {
    RpeLevel.WARM_UP -> R.string.rpe_level_warmup
    RpeLevel.EASY -> R.string.rpe_level_easy
    RpeLevel.MODERATE -> R.string.rpe_level_moderate
    RpeLevel.HARD -> R.string.rpe_level_hard
    RpeLevel.MAXIMUM -> R.string.rpe_level_maximum
}

@StringRes
fun RpeLevel.descriptionRes(): Int = when (this) {
    RpeLevel.WARM_UP -> R.string.rpe_level_warmup_desc
    RpeLevel.EASY -> R.string.rpe_level_easy_desc
    RpeLevel.MODERATE -> R.string.rpe_level_moderate_desc
    RpeLevel.HARD -> R.string.rpe_level_hard_desc
    RpeLevel.MAXIMUM -> R.string.rpe_level_maximum_desc
}
