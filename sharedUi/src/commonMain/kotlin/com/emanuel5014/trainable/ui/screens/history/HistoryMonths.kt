package com.emanuel5014.trainable.ui.screens.history

import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.platform.formatLocalDecimals
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.round
import kotlin.time.Instant

data class HistoryRowSummary(
    val sets: Int,
    val volumeKg: Float,
    val cardioKm: Float,
    val durationMs: Long?,
    val muscles: List<String>,
    val isCardioOnly: Boolean
)

fun historyRowSummary(details: SessionWithDetails): HistoryRowSummary {
    val sets = details.sets
    val cardioSeconds = details.cardio.sumOf { it.durataSecondi.toLong() }
    return HistoryRowSummary(
        sets = sets.size,
        volumeKg = sets.sumOf { (it.setLog.pesoSollevato * it.setLog.repsEffettive).toDouble() }.toFloat(),
        cardioKm = details.cardio.sumOf { it.distanza.toDouble() }.toFloat(),
        durationMs = details.session.durationMs
            ?: if (cardioSeconds > 0) cardioSeconds * 1000 else null,
        muscles = sets
            .sortedWith(compareBy({ it.setLog.ordineEsercizio }, { it.setLog.numeroSerie }))
            .map { it.exercise.categoria }
            .filter { it.isNotBlank() }
            .distinct(),
        isCardioOnly = sets.isEmpty() && details.cardio.isNotEmpty()
    )
}

data class HistoryMonth(val year: Int, val month: Int, val sessions: List<SessionWithDetails>, val volumeKg: Float)

/** Keeps the input order (newest first); a new group starts whenever (year, month) of the local date changes. */
fun historyMonths(sessions: List<SessionWithDetails>, timeZone: TimeZone): List<HistoryMonth> {
    val groups = mutableListOf<Pair<Pair<Int, Int>, MutableList<SessionWithDetails>>>()
    for (s in sessions) {
        val date = Instant.fromEpochMilliseconds(s.session.timestamp).toLocalDateTime(timeZone)
        val key = date.year to date.month.ordinal + 1
        if (groups.lastOrNull()?.first == key) groups.last().second.add(s) else groups.add(key to mutableListOf(s))
    }
    return groups.map { (key, list) ->
        HistoryMonth(key.first, key.second, list, list.sumOf { historyRowSummary(it).volumeKg.toDouble() }.toFloat())
    }
}

/** "7,1 t" / "15,7k lb" when the rounded converted value is 1000 or more (one decimal), else whole number + " kg"/" lb". */
fun formatVolume(volumeKg: Float, weightUnit: String): String {
    val value = WeightUnitConverter.convertDisplay(volumeKg, weightUnit).toDouble()
    return if (round(value) >= 1000) {
        val big = formatLocalDecimals(value / 1000, 1)
        if (weightUnit == "lb") "${big}k lb" else "$big t"
    } else {
        "${formatLocalDecimals(value, 0)} ${if (weightUnit == "lb") "lb" else "kg"}"
    }
}

/** "45s", "58m", "1h 04m" (minutes zero-padded only after hours). */
fun formatSessionDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
        minutes > 0 -> "${minutes}m"
        else -> "${totalSeconds}s"
    }
}
