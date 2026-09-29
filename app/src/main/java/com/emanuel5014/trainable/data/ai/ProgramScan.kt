package com.emanuel5014.trainable.data.ai

import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionNotationParser
import org.json.JSONObject
import java.util.Locale

/** The prescription of one exercise read from a photographed program page. */
data class ScannedProgram(
    val weeks: Map<Int, List<PrescriptionBlock>>,
    /** 1RM written on the page, e.g. "Panca Piana (90kg)". */
    val oneRepMaxKg: Float? = null
)

/**
 * The small on-device model is asked only to copy the page (OCR), never to structure it: asking for
 * nested JSON with examples made it parrot the example. Structure comes from [ProgramScanParser].
 */
object ProgramScanPrompt {
    fun build(): String = """
This is a photo of a strength-training program page (powerlifting style), handwritten or printed.
Copy the text exactly as it is written, line by line, in the same order. Do not explain, translate, correct or add anything.

How these pages are usually laid out:
- The first line is the exercise name, often followed by the athlete's max in brackets.
- Then one line per week. Each line starts with a week label ("W1:", "W2:", ...) followed by the blocks of that week, from left to right.
- A block is a percentage (or a load in kg, or an RPE) followed by the reps, or by sets x reps. For example "62% 5", "68% 3x6", "74% 3-2-1", "80% 4xMAX".
- A technique can follow a block: STOP 2", ISO 2" (an isometric pause: the handwritten letters ISO often look like the number 150), CATENE, ELASTICI, PIEDI SU, GARA, TEMPO 3-1-0.
- A week label with nothing after it is a week that is not written yet. Copy the label alone, for example "W5:".

Write sets and reps with a lowercase "x" (4x5), keep the "%" after every percentage and the " mark after seconds, and put each week on its own line.
Output only the copied text, nothing else.
""".trim()
}

object ProgramScanParser {

    /**
     * @param exerciseName the exercise being edited; when the page lists several exercises the
     * section whose title matches it is used, otherwise the first one.
     */
    fun parse(raw: String, exerciseName: String? = null): ScannedProgram? {
        val text = sanitize(raw)
        if (text.isBlank()) return null
        return parseJson(text) ?: parsePlainText(text, exerciseName)
    }

    // ---- JSON (kept for models that answer with a structure anyway) --------------------------------

    private fun parseJson(text: String): ScannedProgram? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val obj = try { JSONObject(text.substring(start, end + 1)) } catch (_: Exception) { return null }

        val oneRepMax = obj.optDouble("one_rep_max_kg").takeIf { !it.isNaN() && it > 0 }?.toFloat()
        val weeks = sortedMapOf<Int, List<PrescriptionBlock>>()

        obj.optJSONArray("weeks")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val week = item.opt("week")?.let { value ->
                    (value as? Number)?.toInt() ?: Regex("""\d+""").find(value.toString())?.value?.toIntOrNull()
                } ?: (i + 1)
                val notation = item.optString("notation").ifBlank { item.optString("prescription") }
                val blocks = PrescriptionNotationParser.parse(notation)
                if (week > 0 && blocks.isNotEmpty()) weeks[week] = blocks
            }
        }

        if (weeks.isEmpty()) {
            val notation = obj.optString("notation").ifBlank { obj.optString("prescription") }
            if (notation.isNotBlank()) {
                PrescriptionNotationParser.parseProgram(notation).weeks.let { weeks.putAll(it) }
            }
        }
        if (weeks.isEmpty()) return null
        return ScannedProgram(weeks = weeks, oneRepMaxKg = oneRepMax)
    }

    // ---- plain transcription ----------------------------------------------------------------------

    private class Section(val header: String?) {
        val lines = mutableListOf<String>()
        val hasWeeks: Boolean get() = lines.isNotEmpty()
    }

    private fun parsePlainText(text: String, exerciseName: String?): ScannedProgram? {
        val sections = splitSections(text)
        val candidates = sections.filter { it.hasWeeks }
        val chosen = pickSection(candidates, exerciseName)
        val program = if (chosen != null) {
            PrescriptionNotationParser.parseProgram(listOfNotNull(chosen.header).plus(chosen.lines).joinToString("\n"))
        } else {
            // No week labels anywhere: the whole text is one week's prescription
            PrescriptionNotationParser.parseProgram(text)
        }
        if (program.weeks.isEmpty()) return null
        return ScannedProgram(program.weeks, program.oneRepMaxKg)
    }

    private val weekLine = Regex("""(?i)^(?:(?:W|WK|WEEK|SETT(?:IMANA)?|SEM(?:ANA)?|SEMAINE|WOCHE)\.?\s*\d{1,2})(?![A-Z0-9])""")
    private val shortWeekLine = Regex("""(?i)^S\s*(\d{1,2})\s*([:)])""")
    private val numberedLine = Regex("""^(\d{1,2})\s*[.):]\s+(\S.*)$""")
    private val volumeLike = Regex("""(?i)\d\s*x\s*\d|%|\bRPE\b|@|\bMAX\b|\bAMRAP\b|\bALSAP\b""")

    private fun isWeekLine(line: String) = weekLine.containsMatchIn(line)

    private fun isHeader(line: String, next: String?): Boolean {
        if (isWeekLine(line)) return false
        if (line.count { it.isLetter() } < 3 || line.length > 70) return false
        if (volumeLike.containsMatchIn(line)) return false
        return next != null && isWeekLine(next)
    }

    private fun splitSections(text: String): List<Section> {
        val lines = normalizeWeekNumbering(text.lines().map { it.trim() })
        val sections = mutableListOf<Section>()
        var current: Section? = null
        var continuing = false
        for ((i, line) in lines.withIndex()) {
            if (line.isBlank()) { continuing = false; continue }
            val next = lines.drop(i + 1).firstOrNull { it.isNotBlank() }
            when {
                isWeekLine(line) -> {
                    val section = current ?: Section(null).also { sections += it; current = it }
                    section.lines += line
                    continuing = true
                }
                isHeader(line, next) -> {
                    current = Section(line).also { sections += it }
                    continuing = false
                }
                // A week's blocks wrapped onto the next line
                continuing && current != null && volumeLike.containsMatchIn(line) -> {
                    val section = current!!
                    section.lines[section.lines.lastIndex] = section.lines.last() + " " + line
                }
                else -> continuing = false
            }
        }
        return sections
    }

    /** "S1:" and "1)" style week labels become "W1:" so the rest of the pipeline sees one format. */
    private fun normalizeWeekNumbering(lines: List<String>): List<String> {
        val shortened = lines.map { line ->
            shortWeekLine.find(line)?.let { "W${it.groupValues[1]}:" + line.substring(it.range.last + 1) } ?: line
        }
        // Numbered lines only count as weeks when they run 1, 2, 3… and carry a prescription
        val numbered = shortened.mapNotNull { numberedLine.find(it) }
            .filter { volumeLike.containsMatchIn(it.groupValues[2]) }
        val isSequence = numbered.size >= 2 && numbered.map { it.groupValues[1].toInt() } == (1..numbered.size).toList()
        if (!isSequence) return shortened
        return shortened.map { line ->
            val m = numberedLine.find(line)
            if (m != null && volumeLike.containsMatchIn(m.groupValues[2])) "W${m.groupValues[1]}: ${m.groupValues[2]}" else line
        }
    }

    private fun pickSection(candidates: List<Section>, exerciseName: String?): Section? {
        if (candidates.isEmpty()) return null
        val wanted = exerciseName?.let(::nameTokens)?.takeIf { it.isNotEmpty() } ?: return candidates.first()
        val best = candidates
            .map { it to overlap(wanted, nameTokens(it.header.orEmpty())) }
            .maxByOrNull { it.second }
        return if (best != null && best.second > 0f) best.first else candidates.first()
    }

    private fun nameTokens(name: String): Set<String> = name.lowercase(Locale.ROOT)
        .replace(Regex("""\(.*?\)"""), " ")
        .split(Regex("""[^\p{L}]+"""))
        .filter { it.length >= 3 }
        .toSet()

    private fun overlap(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        return a.intersect(b).size.toFloat() / a.size
    }

    /** Models like to wrap answers in fences, bold labels and bullets; none of that is part of the page. */
    private fun sanitize(raw: String): String = raw
        .replace(Regex("""```[a-zA-Z]*"""), "\n")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .lines()
        .joinToString("\n") { line ->
            line.trim()
                .replace(Regex("""^[-•·▪●]\s+"""), "")
                .replace(Regex("""^#+\s*"""), "")
                .replace(Regex("""^\*\s+"""), "")
        }
        .trim()
}
