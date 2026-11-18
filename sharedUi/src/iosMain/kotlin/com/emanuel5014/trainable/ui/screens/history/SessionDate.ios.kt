package com.emanuel5014.trainable.ui.screens.history

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970

internal actual fun formatSessionDate(timestampMillis: Long, longMonth: Boolean): String {
    val formatter = NSDateFormatter().apply {
        locale = com.emanuel5014.trainable.platform.IosAppLocale.formattingLocale()
        dateFormat = if (longMonth) "dd MMMM yyyy" else "dd MMM yyyy"
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1000.0))
}
