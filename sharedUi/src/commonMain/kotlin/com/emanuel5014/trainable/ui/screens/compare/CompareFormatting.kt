package com.emanuel5014.trainable.ui.screens.compare

/** A change with its sign and one decimal, in the device's number style: "+12.5%". */
internal expect fun formatSignedPercent(percent: Float): String

/** One decimal in the device's number style: "7.5". */
internal expect fun formatOneDecimal(value: Float): String
