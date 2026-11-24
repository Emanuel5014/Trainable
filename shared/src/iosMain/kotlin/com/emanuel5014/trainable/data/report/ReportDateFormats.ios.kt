package com.emanuel5014.trainable.data.report

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.dateWithTimeIntervalSince1970

internal actual class ReportDateFormats actual constructor(languageCode: String) {
    private val locale = NSLocale(localeIdentifier = languageCode)
    private val date = NSDateFormatter().apply {
        locale = this@ReportDateFormats.locale
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
    }
    private val dateTime = NSDateFormatter().apply {
        locale = this@ReportDateFormats.locale
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
    }

    private val weekdayFormat = NSDateFormatter().apply {
        locale = this@ReportDateFormats.locale
        setLocalizedDateFormatFromTemplate("EEE")
    }
    private val monthFormat = NSDateFormatter().apply {
        locale = this@ReportDateFormats.locale
        setLocalizedDateFormatFromTemplate("LLLLyyyy")
    }

    actual fun weekday(millis: Long): String = weekdayFormat.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)).trimEnd('.')
    actual fun monthYear(millis: Long): String = monthFormat.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
        .replaceFirstChar { it.uppercase() }
    actual fun day(millis: Long): String = date.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
    actual fun moment(millis: Long): String = dateTime.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
}
