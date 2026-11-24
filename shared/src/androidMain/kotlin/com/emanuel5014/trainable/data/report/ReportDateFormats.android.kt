package com.emanuel5014.trainable.data.report

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal actual class ReportDateFormats actual constructor(languageCode: String) {
    private val locale: Locale = Locale.forLanguageTag(languageCode)
    private val date: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
    private val dateTime: DateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)

    actual fun day(millis: Long): String = date.format(Date(millis))
    private val weekdayFormat = SimpleDateFormat("EEE", locale)
    private val monthFormat = SimpleDateFormat("LLLL yyyy", locale)

    actual fun moment(millis: Long): String = dateTime.format(Date(millis))
    actual fun weekday(millis: Long): String = weekdayFormat.format(Date(millis)).trimEnd('.')
    actual fun monthYear(millis: Long): String = monthFormat.format(Date(millis)).replaceFirstChar { it.titlecase(locale) }
}
