package com.emanuel5014.trainable.domain.csvimport

import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** How a row was measured, which decides how its set is written. */
enum class RowKind { STRENGTH, TIME_AND_WEIGHT, TIME_AND_REPS, CARDIO }

enum class IssueSeverity {
    /** The row cannot be imported and is left out. */
    ERROR,

    /** The row is imported, but something was guessed or dropped. */
    WARNING
}

enum class IssueCode {
    MISSING_DATE, INVALID_DATE, MISSING_EXERCISE, INVALID_NUMBER, VALUE_TOO_LARGE, NO_VALUES, MISSING_REPS,
    MISSING_DURATION, MISSING_CARDIO_VALUES, CARDIO_NOT_CARDIO_EXERCISE,
    UNKNOWN_TYPE, IGNORED_VALUE, FUTURE_DATE
}

/** [value] is the text of the cell the problem is about, [column] which one it is. */
data class ImportIssue(
    val line: Int,
    val severity: IssueSeverity,
    val code: IssueCode,
    val column: CsvColumn? = null,
    val value: String = ""
)

/** One set (or cardio set) of the file with its cells read, nothing resolved against the app yet. */
data class ParsedRow(
    val line: Int,
    val date: LocalDate,
    /** Time of day when the date cell carries one. */
    val time: LocalTime?,
    val sessionId: String,
    val plan: String,
    val exercise: String,
    val category: String,
    /** What the Type cell says, null when it is empty or not understood. */
    val declaredKind: RowKind?,
    /** What the values alone suggest, used when [declaredKind] is null. */
    val inferredKind: RowKind,
    val set: Int?,
    /** In the unit of the file. */
    val weight: Float?,
    val reps: Int?,
    val durationSeconds: Int?,
    val distanceKm: Float?,
    val note: String,
    val week: Int?,
    val rpe: Float?,
    val warmup: Boolean,
    val parts: List<Int>,
    val pauses: List<Int>,
    val assisted: Int,
    val partials: Int
)

enum class DateOrder { DAY_FIRST, MONTH_FIRST }

data class ParsedHistory(
    val delimiter: Char,
    /** Unit named by the weight header, null when it does not say. */
    val weightUnit: String?,
    val dateOrder: DateOrder,
    /** The file has dates like 03/04/2026, so [dateOrder] mattered. */
    val dateOrderApplied: Boolean,
    /** ... and nothing that tells day from month. */
    val dateOrderGuessed: Boolean,
    val rows: List<ParsedRow>,
    val issues: List<ImportIssue>,
    /** Records after the header, whether they could be read or not. */
    val recordCount: Int,
    /** Columns this version does not import that hold values (program targets, techniques ...). */
    val ignoredColumns: List<String>,
    /** Columns nobody knows. */
    val unknownColumns: List<String>
)

enum class ReadFailure { EMPTY, NOT_A_HISTORY, NO_ROWS }

sealed interface ReadResult {
    data class Success(val history: ParsedHistory) : ReadResult
    data class Failure(val reason: ReadFailure, val missing: List<CsvColumn> = emptyList()) : ReadResult
}

/** Reads the CSV the app exports (or a file edited into its shape) into [ParsedRow]s, keeping a list of what is wrong. */
object HistoryCsvReader {
    private const val MAX_WEIGHT = 5000f
    private const val MAX_REPS = 10_000
    private const val MAX_SECONDS = 48 * 3600
    private const val MAX_KM = 5000f
    private const val MAX_SET = 999
    private const val FIRST_YEAR = 1990
    private val whitespace = Regex("\\s+")

    fun read(text: String, today: LocalDate = LocalDate.now()): ReadResult {
        val records = CsvParser.parse(text)
        if (records.isEmpty()) return ReadResult.Failure(ReadFailure.EMPTY)
        val header = HistoryCsvColumns.read(records.first().cells)
        val missing = header.missingRequired
        if (missing.isNotEmpty()) return ReadResult.Failure(ReadFailure.NOT_A_HISTORY, missing)
        val body = records.drop(1)
        if (body.isEmpty()) return ReadResult.Failure(ReadFailure.NO_ROWS)

        val order = DateReader.detectOrder(body.mapNotNull { cell(it, header, CsvColumn.DATE).takeIf(String::isNotEmpty) })
        val issues = mutableListOf<ImportIssue>()
        val rows = body.mapNotNull { readRow(it, header, order.order, today, issues) }

        val titles = records.first().cells
        val ignored = CsvColumn.entries
            .filter { !it.imported && header.has(it) }
            .filter { column -> body.any { cell(it, header, column).isNotEmpty() } }
            .map { titles[header.columns.getValue(it)].trim() }
        return ReadResult.Success(
            ParsedHistory(
                delimiter = CsvParser.detectDelimiter(text),
                weightUnit = header.weightUnit,
                dateOrder = order.order,
                dateOrderApplied = order.applied,
                dateOrderGuessed = order.guessed,
                rows = rows,
                issues = issues.sortedWith(compareBy({ it.line }, { it.severity })),
                recordCount = body.size,
                ignoredColumns = ignored,
                unknownColumns = header.unknown
            )
        )
    }

    private fun cell(record: CsvRecord, header: CsvHeader, column: CsvColumn): String {
        val index = header.columns[column] ?: return ""
        return record.cells.getOrNull(index)?.trim().orEmpty()
    }

    private fun readRow(
        record: CsvRecord,
        header: CsvHeader,
        order: DateOrder,
        today: LocalDate,
        issues: MutableList<ImportIssue>
    ): ParsedRow? {
        val line = record.line
        fun text(column: CsvColumn) = cell(record, header, column)
        fun reject(code: IssueCode, column: CsvColumn? = null, value: String = ""): ParsedRow? {
            issues.add(ImportIssue(line, IssueSeverity.ERROR, code, column, value))
            return null
        }
        fun warn(code: IssueCode, column: CsvColumn? = null, value: String = "") {
            issues.add(ImportIssue(line, IssueSeverity.WARNING, code, column, value))
        }

        val dateText = text(CsvColumn.DATE)
        if (dateText.isEmpty()) return reject(IssueCode.MISSING_DATE, CsvColumn.DATE)
        val stamp = DateReader.parse(dateText, order)
        if (stamp == null || stamp.date.year < FIRST_YEAR) return reject(IssueCode.INVALID_DATE, CsvColumn.DATE, dateText)
        val exercise = text(CsvColumn.EXERCISE).replace(whitespace, " ")
        if (exercise.isEmpty()) return reject(IssueCode.MISSING_EXERCISE, CsvColumn.EXERCISE)

        // A number that is there but cannot be read is an error: dropping it silently would import a different set
        var failed = false
        fun number(column: CsvColumn, max: Float): Float? {
            val raw = text(column)
            if (raw.isEmpty()) return null
            val value = NumberReader.decimal(raw)
            if (value == null || value < 0f) {
                failed = true
                issues.add(ImportIssue(line, IssueSeverity.ERROR, IssueCode.INVALID_NUMBER, column, raw))
                return null
            }
            if (value > max) {
                failed = true
                issues.add(ImportIssue(line, IssueSeverity.ERROR, IssueCode.VALUE_TOO_LARGE, column, raw))
                return null
            }
            return value
        }
        fun whole(column: CsvColumn, max: Int): Int? {
            val raw = text(column)
            if (raw.isEmpty()) return null
            val value = NumberReader.decimal(raw)
            if (value == null || value < 0f || abs(value - value.roundToInt()) > 1e-4f) {
                failed = true
                issues.add(ImportIssue(line, IssueSeverity.ERROR, IssueCode.INVALID_NUMBER, column, raw))
                return null
            }
            if (value > max) {
                failed = true
                issues.add(ImportIssue(line, IssueSeverity.ERROR, IssueCode.VALUE_TOO_LARGE, column, raw))
                return null
            }
            return value.roundToInt()
        }

        val weight = number(CsvColumn.WEIGHT, MAX_WEIGHT)
        val reps = whole(CsvColumn.REPS, MAX_REPS)
        val distance = number(CsvColumn.DISTANCE, MAX_KM)
        val set = whole(CsvColumn.SET, MAX_SET)
        val durationText = text(CsvColumn.DURATION)
        val duration = if (durationText.isEmpty()) null else NumberReader.seconds(durationText)
        if (durationText.isNotEmpty()) {
            if (duration == null) {
                failed = true
                issues.add(ImportIssue(line, IssueSeverity.ERROR, IssueCode.INVALID_NUMBER, CsvColumn.DURATION, durationText))
            } else if (duration > MAX_SECONDS) {
                failed = true
                issues.add(ImportIssue(line, IssueSeverity.ERROR, IssueCode.VALUE_TOO_LARGE, CsvColumn.DURATION, durationText))
            }
        }
        if (failed) return null
        if (weight == null && reps == null && duration == null && distance == null) {
            return reject(IssueCode.NO_VALUES)
        }

        // Nice-to-have values: a bad one is dropped with a warning instead of costing the row
        val rpeText = text(CsvColumn.RPE)
        val rpe = if (rpeText.isEmpty()) null else NumberReader.decimal(rpeText)?.takeIf { it in 0f..10f }
        if (rpeText.isNotEmpty() && rpe == null) warn(IssueCode.IGNORED_VALUE, CsvColumn.RPE, rpeText)
        val partsText = text(CsvColumn.PARTS)
        val parts = if (partsText.isEmpty()) emptyList() else parseParts(partsText)
        if (partsText.isNotEmpty() && parts.isEmpty()) warn(IssueCode.IGNORED_VALUE, CsvColumn.PARTS, partsText)
        val pausesText = text(CsvColumn.PAUSES)
        val pauses = if (pausesText.isEmpty() || parts.isEmpty()) emptyList() else parsePauses(pausesText)
        val assisted = text(CsvColumn.ASSISTED).let { raw -> NumberReader.decimal(raw)?.roundToInt()?.takeIf { it in 0..MAX_REPS } }
        val partials = text(CsvColumn.PARTIALS).let { raw -> NumberReader.decimal(raw)?.roundToInt()?.takeIf { it in 0..MAX_REPS } }
        val week = text(CsvColumn.WEEK).let { raw -> NumberReader.decimal(raw)?.roundToInt()?.takeIf { it in 1..99 } }

        val typeText = text(CsvColumn.TYPE)
        val declared = if (typeText.isEmpty()) null else kindOf(typeText)
        if (typeText.isNotEmpty() && declared == null) warn(IssueCode.UNKNOWN_TYPE, CsvColumn.TYPE, typeText)
        if (stamp.date.isAfter(today.plusDays(1))) warn(IssueCode.FUTURE_DATE, CsvColumn.DATE, dateText)

        return ParsedRow(
            line = line,
            date = stamp.date,
            time = stamp.time,
            sessionId = text(CsvColumn.SESSION_ID),
            plan = text(CsvColumn.PLAN).replace(whitespace, " "),
            exercise = exercise,
            category = text(CsvColumn.CATEGORY).replace(whitespace, " "),
            declaredKind = declared,
            inferredKind = inferKind(reps, duration, distance),
            set = set?.takeIf { it > 0 },
            weight = weight,
            reps = reps,
            durationSeconds = duration,
            distanceKm = distance,
            note = text(CsvColumn.NOTE),
            week = week,
            rpe = rpe,
            warmup = flag(text(CsvColumn.WARMUP)),
            parts = parts,
            pauses = pauses,
            assisted = assisted ?: 0,
            partials = partials ?: 0
        )
    }

    private fun inferKind(reps: Int?, duration: Int?, distance: Float?): RowKind = when {
        distance != null && distance > 0f -> RowKind.CARDIO
        duration != null && reps != null -> RowKind.TIME_AND_REPS
        duration != null -> RowKind.TIME_AND_WEIGHT
        else -> RowKind.STRENGTH
    }

    /** The codes the app writes in the Type column, plus the way people tend to write them. */
    private fun kindOf(text: String): RowKind? = when (HistoryCsvColumns.fold(text)) {
        "strength", "weight and reps", "weight reps" -> RowKind.STRENGTH
        "time and weight", "time weight" -> RowKind.TIME_AND_WEIGHT
        "time and reps", "time reps" -> RowKind.TIME_AND_REPS
        "cardio", "time and distance", "time distance" -> RowKind.CARDIO
        else -> null
    }

    private fun flag(text: String): Boolean = text.trim().lowercase(Locale.ROOT) in setOf("1", "true", "yes", "y", "si", "sì", "x", "oui", "ja", "sim")

    /** "2+1+1" -> [2, 1, 1]; empty unless every part is a whole number and there are at least two. */
    private fun parseParts(text: String): List<Int> {
        val tokens = text.split("+").map { it.trim() }
        if (tokens.size < 2 || tokens.size > 10 || tokens.any { it.isEmpty() }) return emptyList()
        val parts = tokens.map { it.toIntOrNull()?.takeIf { n -> n in 0..999 } ?: return emptyList() }
        return parts
    }

    /** "60/50" -> [60, 50]. */
    private fun parsePauses(text: String): List<Int> =
        text.split("/").mapNotNull { it.trim().toIntOrNull()?.takeIf { s -> s in 0..3600 } }
}

/** Numbers as people write them in a spreadsheet: `80`, `82.5`, `82,5`, `1.234,5`. */
object NumberReader {
    fun decimal(raw: String): Float? {
        var text = raw.trim()
        if (text.indexOf(' ') >= 0 || text.indexOf('\u00A0') >= 0) text = text.replace(" ", "").replace("\u00A0", "")
        if (text.isEmpty()) return null
        val comma = text.lastIndexOf(',')
        val dot = text.lastIndexOf('.')
        text = when {
            // Both present: the one that comes last is the decimal point, the other groups thousands
            comma >= 0 && dot >= 0 -> if (comma > dot) text.replace(".", "").replace(',', '.') else text.replace(",", "")
            comma >= 0 -> text.replace(',', '.')
            else -> text
        }
        // Digits with at most one point, and at least one digit
        var points = 0
        var digits = 0
        for (ch in text) {
            when {
                ch in '0'..'9' -> digits++
                ch == '.' -> points++
                else -> return null
            }
        }
        if (digits == 0 || points > 1) return null
        return text.toFloatOrNull()?.takeIf { it.isFinite() }
    }

    /** Seconds from `90`, `90.5`, `1:30` or `0:01:30`. */
    fun seconds(raw: String): Int? {
        val text = raw.trim()
        if (text.contains(':')) {
            val parts = text.split(":").map { it.trim().toIntOrNull() ?: return null }
            if (parts.size !in 2..3 || parts.any { it < 0 }) return null
            return if (parts.size == 2) parts[0] * 60 + parts[1] else parts[0] * 3600 + parts[1] * 60 + parts[2]
        }
        val value = decimal(text) ?: return null
        return value.roundToInt()
    }
}

/** The day (and time, when the cell has one) a date cell stands for. */
data class DateStamp(val date: LocalDate, val time: LocalTime?)

object DateReader {
    private val isoLike = Regex(
        "^(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})(?:[T ,]+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?(?:\\.\\d+)?\\s*(?:Z|[+-]\\d{2}:?\\d{2})?)?$"
    )
    private val partsFirst = Regex(
        "^(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{2}|\\d{4})(?:[T ,]+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?)?$"
    )

    data class Detected(val order: DateOrder, val guessed: Boolean, val applied: Boolean)

    /**
     * Day or month first for dates like 03/04/2026: decided by the first date of the file that cannot be read the other
     * way (a day above 12), day first when none does - and flagged as a guess when some date really is ambiguous.
     */
    fun detectOrder(cells: List<String>): Detected {
        var dayFirstProof = false
        var monthFirstProof = false
        var ambiguous = false
        var applied = false
        cells.forEach { cell ->
            val match = partsFirst.matchEntire(cell.trim()) ?: return@forEach
            applied = true
            val a = match.groupValues[1].toInt()
            val b = match.groupValues[2].toInt()
            when {
                a > 12 && b <= 12 -> dayFirstProof = true
                b > 12 && a <= 12 -> monthFirstProof = true
                a != b && a <= 12 && b <= 12 -> ambiguous = true
            }
        }
        return when {
            monthFirstProof && !dayFirstProof -> Detected(DateOrder.MONTH_FIRST, guessed = false, applied = applied)
            dayFirstProof -> Detected(DateOrder.DAY_FIRST, guessed = false, applied = applied)
            else -> Detected(DateOrder.DAY_FIRST, guessed = ambiguous, applied = applied)
        }
    }

    fun parse(text: String, order: DateOrder): DateStamp? {
        val value = text.trim()
        isoLike.matchEntire(value)?.let { m ->
            return build(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt(), m.groupValues, 4)
        }
        partsFirst.matchEntire(value)?.let { m ->
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            val yearText = m.groupValues[3]
            val year = if (yearText.length == 2) 2000 + yearText.toInt() else yearText.toInt()
            val (day, month) = if (order == DateOrder.DAY_FIRST) a to b else b to a
            return build(year, month, day, m.groupValues, 4)
        }
        return null
    }

    private fun build(year: Int, month: Int, day: Int, groups: List<String>, timeIndex: Int): DateStamp? {
        val date = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: return null
        val hour = groups.getOrNull(timeIndex)?.toIntOrNull()
        val minute = groups.getOrNull(timeIndex + 1)?.toIntOrNull()
        val second = groups.getOrNull(timeIndex + 2)?.toIntOrNull() ?: 0
        val time = if (hour != null && minute != null) runCatching { LocalTime.of(hour, minute, second) }.getOrNull() else null
        return DateStamp(date, time)
    }
}
