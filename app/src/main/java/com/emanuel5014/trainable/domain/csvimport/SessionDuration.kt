package com.emanuel5014.trainable.domain.csvimport

import java.util.Locale
import kotlin.math.roundToLong

/**
 * How long a whole workout took, as the CSV carries it: minutes in the export (`65`, `62.5`), and in an import whatever
 * people write: a number in the unit of the header (minutes when it names none), `1:05` / `1:05:30` as hours and
 * minutes (and seconds), or words like `1h 5m`, `90 min`, `1,5 h`.
 */
object SessionDuration {
    private const val MAX_MILLIS = 48L * 3_600_000L

    private val hours = setOf(
        "h", "hr", "hrs", "hour", "hours", "ora", "ore", "hora", "horas", "heure", "heures", "stunde", "stunden", "std"
    )
    private val minutes = setOf(
        "m", "min", "mins", "minute", "minutes", "minuto", "minuti", "minutos", "minuten"
    )
    private val seconds = setOf(
        "s", "sec", "secs", "second", "seconds", "secondo", "secondi", "segundo", "segundos", "seconde", "secondes", "sekunde", "sekunden"
    )

    private val token = Regex("(\\d+(?:[.,]\\d+)?)\\s*([\\p{L}]+)")
    private val onlyTokens = Regex("^\\s*(?:(?:\\d+(?:[.,]\\d+)?)\\s*[\\p{L}]+\\s*)+$")

    /** Milliseconds, or null when the text is empty, not a duration, zero or longer than two days. */
    fun parse(raw: String, unit: DurationUnit? = null): Long? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val millis = when {
            text.contains(':') -> clock(text)
            onlyTokens.matches(text) -> words(text)
            else -> NumberReader.decimal(text)?.let { (it.toDouble() * (unit ?: DurationUnit.MINUTES).millis).roundToLong() }
        } ?: return null
        return millis.takeIf { it in 1..MAX_MILLIS }
    }

    /** "1:05" is an hour and five minutes, "1:05:30" adds seconds. */
    private fun clock(text: String): Long? {
        val parts = text.split(":").map { it.trim().toIntOrNull() ?: return null }
        if (parts.size !in 2..3 || parts.any { it < 0 }) return null
        val (h, m) = parts
        val s = parts.getOrElse(2) { 0 }
        if (m >= 60 || s >= 60) return null
        return h * 3_600_000L + m * 60_000L + s * 1_000L
    }

    private fun words(text: String): Long? {
        var total = 0.0
        for (match in token.findAll(text)) {
            val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
            val word = match.groupValues[2].lowercase(Locale.ROOT)
            val unit = when (word) {
                in hours -> DurationUnit.HOURS
                in minutes -> DurationUnit.MINUTES
                in seconds -> DurationUnit.SECONDS
                else -> return null
            }
            total += amount * unit.millis
        }
        return total.roundToLong()
    }

    /** Minutes with at most one decimal ("65", "62.5"), empty for an unknown or zero duration. */
    fun format(millis: Long?): String {
        if (millis == null || millis <= 0L) return ""
        val tenths = (millis / 6000.0).roundToLong()
        return if (tenths % 10 == 0L) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
    }
}
