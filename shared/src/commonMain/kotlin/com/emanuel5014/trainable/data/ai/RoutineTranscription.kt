package com.emanuel5014.trainable.data.ai

/**
 * The on-device way to read a routine sheet on iPhone (A5): Gemma E2B on the phone cannot produce the routine JSON
 * reliably (measured on an iPhone 15 Pro, checks/a5-local-gemma), but copies the sheet's table well. So the model only
 * transcribes the table ([RoutineTranscriptionPrompt]) and [RoutineTranscriptionParser] turns it into exercises.
 * Android keeps its JSON prompt; nothing here is used there.
 */
object RoutineTranscriptionPrompt {
    /** A sheet has a few dozen rows at most; anything longer is the model running on. */
    const val MAX_OUTPUT_TOKENS = 1536

    fun build(): String = """
This is a photo of a gym workout sheet, handwritten or printed, usually a table with one exercise per row.
Copy the table exactly as it is written, row by row, in the same order. Do not explain, translate, correct or add anything.

Write one row per exercise in this form, with the cells separated by " | ":
EXERCISE NAME | SETS | REPS | REST

- SETS is the number of sets. If the sheet writes sets and reps together (4x10), put 4 in SETS and 10 in REPS.
- REPS is what the sheet says for the repetitions: a number, a range (8-12), a pyramid (12-10-8-6), MAX, or a time (45").
- REST is the recovery exactly as written (90", 1', 1:30, 2 min). If the cell repeats the row above with a mark (" or 〃), write ".
  If the cell is empty, leave it empty.
- Skip the header lines with the athlete's name, dates and goals, and skip empty rows.

Output only the rows, nothing else.
""".trim()
}

object RoutineTranscriptionParser {

    /** The exercises of a transcribed table; empty when [raw] has no table rows. */
    fun parse(raw: String): List<ParsedExercise> {
        val list = mutableListOf<ParsedExercise>()
        var lastRest: Int? = null
        for (line in raw.lines()) {
            if ('|' !in line) continue
            val cells = line.split('|').map { it.trim().trim('*', '`') }
            // Separator rows (---|---) and the header row carry no exercise
            if (cells.all { it.isEmpty() || it.all { c -> c == '-' || c == ':' || c == ' ' } }) continue
            if (cells.count { isHeaderCell(it) } >= 2) continue
            val nameIndex = cells.indexOfFirst { isName(it) }
            if (nameIndex < 0) continue
            val name = RoutineScanParser.cleanExerciseName(cells[nameIndex])
            if (name.length < 3) continue
            val rest = cells.drop(nameIndex + 1)

            var sets: Int? = null
            var reps: String? = null
            val setsCell = rest.getOrNull(0).orEmpty()
            val setsByReps = Regex("""^(\d+)\s*[xX×*]\s*(.+)$""").find(setsCell)
            var nextCell = 1
            if (setsByReps != null) {
                sets = setsByReps.groupValues[1].toIntOrNull()
                reps = setsByReps.groupValues[2]
            } else {
                sets = Regex("""\d+""").find(setsCell)?.value?.toIntOrNull()
                reps = rest.getOrNull(1)
                nextCell = 2
            }
            val restCell = rest.getOrNull(nextCell).orEmpty()
            val restSeconds = when {
                isDitto(restCell) -> lastRest
                restCell.isBlank() -> lastRest
                else -> parseRest(restCell) ?: lastRest
            }
            if (restSeconds != null) lastRest = restSeconds

            val rawReps = reps?.trim().orEmpty()
            val cleanReps = rawReps.takeIf { it.isNotEmpty() }?.let {
                if (it.equals("max", ignoreCase = true)) "MAX" else RoutineScanParser.cleanRepsString(it)
            } ?: ""
            // A time in the reps cell (45", 45s, 45 sec) makes a timed exercise
            val timed = Regex("""^\d+\s*(?:"|”|″|s|sec|secondi|seconds)$""", RegexOption.IGNORE_CASE).matches(rawReps)
            val timeSeconds = if (timed) cleanReps.filter(Char::isDigit).toIntOrNull() else null
            list += ParsedExercise(
                name = name,
                sets = (sets ?: 3).coerceIn(1, 30),
                reps = if (timed) "${timeSeconds ?: 45}s" else cleanReps,
                restSeconds = (restSeconds ?: DEFAULT_REST).coerceIn(0, 600),
                cardioMinutes = null,
                category = null,
                exerciseType = if (timed) "time_and_weight" else "strength",
                timeSeconds = timeSeconds
            )
        }
        return list
    }

    private const val DEFAULT_REST = 120

    private val HEADER_WORDS = setOf(
        "esercizio", "esercizi", "exercise", "exercises", "nome", "name", "serie", "sets", "reps", "ripetizioni",
        "rest", "recupero", "carico", "note", "notes", "giorno", "day"
    )

    private fun isName(cell: String): Boolean = cell.count { it.isLetter() } >= 3

    private fun isHeaderCell(cell: String): Boolean =
        cell.lowercase().split(Regex("""[\s/]+""")).any { it.trim('.', ':', '°') in HEADER_WORDS }

    private fun isDitto(cell: String): Boolean = cell.trim() in setOf("\"", "''", "〃", "″", "”", "“", "=", "id", "idem")

    /** "90\"", "90s", "90" → 90; "1'", "1 min", "2m" → minutes; "1:30" / "1'30" → 90; a bare 1–10 is minutes. */
    internal fun parseRest(cell: String): Int? {
        val text = cell.lowercase().trim()
        Regex("""(\d+)\s*[':]\s*(\d+)""").find(text)?.let { m ->
            return m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
        }
        Regex("""(\d+)\s*(?:'|’|min|m\b|minuti|minutes)""").find(text)?.let { return it.groupValues[1].toInt() * 60 }
        Regex("""(\d+)\s*(?:"|”|″|s\b|sec|secondi|seconds)""").find(text)?.let { return it.groupValues[1].toInt() }
        val bare = Regex("""^\d+$""").find(text)?.value?.toIntOrNull() ?: return null
        return if (bare <= 10) bare * 60 else bare
    }
}
