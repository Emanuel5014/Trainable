package com.emanuel5014.trainable.domain.csvimport

import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Which kind of rows an export keeps. */
enum class CsvRowKinds { ALL, STRENGTH, CARDIO }

/**
 * What an export keeps of the workouts chosen for it: some exercises only, strength or cardio rows only, without the
 * warm-up sets, without the deload workouts. A workout left with no row is not exported at all; the sets that stay keep
 * their own numbers, so a file without warm-ups can start from set 2.
 */
data class HistoryCsvRowFilter(
    /** Exercise ids (strength and cardio); empty keeps every exercise. */
    val exerciseIds: Set<Int> = emptySet(),
    val kinds: CsvRowKinds = CsvRowKinds.ALL,
    val includeWarmup: Boolean = true,
    val includeDeload: Boolean = true
) {
    fun apply(sessions: List<SessionWithDetails>): List<SessionWithDetails> = sessions.mapNotNull(::narrow)

    /** [details] with only the rows this filter keeps, or null when none is left (or it is a deload left out). */
    fun narrow(details: SessionWithDetails): SessionWithDetails? {
        if (!includeDeload && details.session.isDeload) return null
        val sets = if (kinds == CsvRowKinds.CARDIO) emptyList() else details.sets.filter { set ->
            (includeWarmup || !set.setLog.isWarmup) && (exerciseIds.isEmpty() || set.exercise.id in exerciseIds)
        }
        val cardio = if (kinds == CsvRowKinds.STRENGTH) emptyList() else details.cardio.filter { log ->
            exerciseIds.isEmpty() || log.exerciseId in exerciseIds
        }
        if (sets.isEmpty() && cardio.isEmpty()) return null
        return if (sets.size == details.sets.size && cardio.size == details.cardio.size) details
        else details.copy(sets = sets, cardio = cardio)
    }

    companion object {
        /** How many rows the file of [sessions] has, one per set and per cardio set. */
        fun rowCount(sessions: List<SessionWithDetails>): Int = sessions.sumOf { it.sets.size + it.cardio.size }
    }
}

/**
 * The name an export is saved under: `Trainable_<what>_<first day>_<last day>.csv`. The days are those of the first and
 * the last workout in the file; [scope] names what it was narrowed to (routines, groups, exercises), at most three
 * names and a count of the others.
 */
object HistoryCsvFileName {
    private const val MAX_NAMES = 3
    private const val MAX_NAME_LENGTH = 32
    private val unsafe = Regex("[\\\\/:*?\"<>|\\s.,;]+")

    fun of(scope: List<String>, first: LocalDate?, last: LocalDate?, today: LocalDate): String {
        val names = scope.map(::clean).filter { it.isNotEmpty() }.distinct()
        val what = when {
            names.isEmpty() -> null
            names.size <= MAX_NAMES -> names.joinToString("-")
            else -> names.take(MAX_NAMES).joinToString("-") + "+" + (names.size - MAX_NAMES)
        }
        val days = when {
            first == null || last == null -> today.toString()
            first == last -> first.toString()
            else -> "${minOf(first, last)}_${maxOf(first, last)}"
        }
        return listOfNotNull("Trainable", what, days).joinToString("_") + ".csv"
    }

    /** The first and the last day of [sessions], in [zone]. */
    fun span(sessions: List<SessionWithDetails>, zone: TimeZone = TimeZone.currentSystemDefault()): Pair<LocalDate, LocalDate>? {
        if (sessions.isEmpty()) return null
        fun day(millis: Long) = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date
        val stamps = sessions.map { it.session.timestamp }
        return day(stamps.min()) to day(stamps.max())
    }

    private fun clean(name: String): String = name.trim().replace(unsafe, "-").trim('-').take(MAX_NAME_LENGTH).trim('-')
}
