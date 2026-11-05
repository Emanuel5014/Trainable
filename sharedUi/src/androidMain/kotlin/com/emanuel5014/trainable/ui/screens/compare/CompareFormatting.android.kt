package com.emanuel5014.trainable.ui.screens.compare

import java.util.Locale

internal actual fun formatSignedPercent(percent: Float): String = String.format(Locale.getDefault(), "%+.1f%%", percent)

internal actual fun formatOneDecimal(value: Float): String = String.format(Locale.getDefault(), "%.1f", value)
