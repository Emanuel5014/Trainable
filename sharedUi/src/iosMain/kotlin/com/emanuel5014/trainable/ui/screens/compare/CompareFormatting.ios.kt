package com.emanuel5014.trainable.ui.screens.compare

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

private fun oneDecimal(value: Float): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        usesGroupingSeparator = false
        minimumFractionDigits = 1u
        maximumFractionDigits = 1u
    }
    return formatter.stringFromNumber(NSNumber(double = value.toDouble())) ?: value.toString()
}

internal actual fun formatSignedPercent(percent: Float): String =
    (if (percent >= 0f) "+" else "-") + oneDecimal(kotlin.math.abs(percent)) + "%"

internal actual fun formatOneDecimal(value: Float): String = oneDecimal(value)
