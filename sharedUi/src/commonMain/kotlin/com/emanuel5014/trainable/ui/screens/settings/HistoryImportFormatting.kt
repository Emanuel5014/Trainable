package com.emanuel5014.trainable.ui.screens.settings

import kotlinx.datetime.LocalDate

/** A day in the device's medium date style ("5 Nov 2026"). */
internal expect fun formatMediumDate(date: LocalDate): String

/** A whole number with the device's grouping ("12,345"). */
internal expect fun formatGroupedWhole(value: Float): String
