package com.emanuel5014.trainable.ui.util

/** Fills the "%1$s" / "%2$d" placeholders of a shared string read without arguments, as Android's getString would. */
fun fillPlaceholders(template: String, vararg args: Any): String =
    Regex("%(\\d+)\\$[sd]").replace(template) { match ->
        args.getOrNull(match.groupValues[1].toInt() - 1)?.toString() ?: match.value
    }

/** The time of day of [timestampMillis] in the phone's short format ("14:05", "2:05 PM"). */
expect fun formatShortTime(timestampMillis: Long): String

/** Day and short month of [timestampMillis] in the phone's language ("05 ott"). */
expect fun formatDayMonth(timestampMillis: Long): String
