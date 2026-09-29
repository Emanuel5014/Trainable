package com.emanuel5014.trainable.data.ai

import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionNotationParser
import org.json.JSONObject

/** The prescription of one exercise read from a photographed program page. */
data class ScannedProgram(
    val weeks: Map<Int, List<PrescriptionBlock>>,
    /** 1RM written on the page, e.g. "Panca Piana (90kg)". */
    val oneRepMaxKg: Float? = null
)

object ProgramScanPrompt {
    fun build(exerciseName: String?): String {
        val target = exerciseName?.takeIf { it.isNotBlank() }
            ?.let { "the exercise \"$it\" (or the closest match written on the page)" }
            ?: "the exercise written on the page"
        return """
You are an expert powerlifting coach AI reading a photographed training PROGRAM page (handwritten or printed).
Extract the weekly prescription of $target.

RULES:
- Copy each week's prescription LITERALLY into "notation", character by character. Do NOT translate, expand or reinterpret it.
  Keep everything exactly as written: percentages ("80%"), SETSxREPS ("4x4"), rep lists ("3-4-2-1-5"), "MAX", "20 REP ALSAP", "@RPE8", "BW", added loads ("2.5kg 4"), techniques ("STOP 2\"", "FERMO", "CATENE", "PIEDI SU", "GARA", "D4F2S4", "TEMPO 3-1-0", "EMOM", "+ 3 SINGOLE").
- week: the week number as an integer ("W1:" -> 1, "Settimana 3" -> 3). If the page has no weeks, use week 1 only.
- Skip weeks with nothing written after the label (e.g. an empty "W5:").
- one_rep_max_kg: the max / 1RM written next to the exercise name (e.g. "Panca Piana (90kg)" -> 90, "1RM 150" -> 150). null when there is none.
- If several exercises are on the page, use only the requested one. If it is not there, use the first exercise.

OUTPUT FORMAT:
- ONLY a valid JSON object, no extra text, no markdown.

JSON Schema Example:
{
  "exercise": "Panca Piana",
  "one_rep_max_kg": 90,
  "weeks": [
    { "week": 1, "notation": "80% 4x4 D4F2S4" },
    { "week": 2, "notation": "85% 4x3 D4F2S4" },
    { "week": 4, "notation": "70% 2x4 75% 2x3 80% 3 + 3 SINGOLE" }
  ]
}
""".trim()
    }
}

object ProgramScanParser {

    fun parse(raw: String): ScannedProgram? {
        val text = raw.trim()
        if (text.isBlank()) return null
        return parseJson(text) ?: parsePlainText(text)
    }

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

    /** The model ignored the JSON format and just wrote the program ("W1: 80% 4x4 …"). */
    private fun parsePlainText(text: String): ScannedProgram? {
        val program = PrescriptionNotationParser.parseProgram(text)
        if (program.weeks.isEmpty()) return null
        return ScannedProgram(program.weeks, program.oneRepMaxKg)
    }
}
