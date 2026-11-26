package com.emanuel5014.trainable.ui.util

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.text.toUpperCase

/**
 * Upper case for text the user reads, following the app language: in Turkish "i" becomes "İ" (the plain
 * `uppercase()` gives a dotless "I"). Keep `uppercase()` for codes, keys, file names and hex colours.
 */
fun String.uppercaseUi(): String = toUpperCase(Locale.current)

/** Lower case for text the user reads, following the app language (Turkish "I" becomes "ı"). */
fun String.lowercaseUi(): String = toLowerCase(Locale.current)
