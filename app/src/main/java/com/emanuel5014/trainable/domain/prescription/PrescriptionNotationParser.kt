package com.emanuel5014.trainable.domain.prescription

import com.emanuel5014.trainable.util.WeightUnitConverter
import java.util.Locale

/**
 * Parses the handwritten "gym notebook" notation into [PrescriptionBlock]s, e.g.
 * `65% 6 70% 4x5 65% 8`, `70% 3x3 STOP 2" + INF. 75% 3x2`, `80% 20 REP ALSAP`, `85% 3xMAX`,
 * `80% 4x4 D4F2S4`, `BW 5x4 EMOM`, `70% 2x4 75% 2x3 80% 3 + 3 SINGOLE`.
 *
 * The algorithm masks recognised tokens in three passes (techniques → intensity markers →
 * volumes) so digits inside a technique (`STOP 2"`) are never mistaken for reps. Every volume
 * match becomes a block; intensity markers attach to the next volume and techniques / leftover
 * text attach to the block whose span contains them.
 */
object PrescriptionNotationParser {

    private data class Found<T>(val range: IntRange, val value: T)

    private enum class VolumeKind { TOTAL, SETS_MAX, SINGLES, SETS_REPS, LIST, REPS_WORD, MAX, NUMBER }

    private data class Intensity(val type: IntensityType, val value: Float?)

    private val weekPrefix = Regex("""^\s*(?:W\s*\d+(?:\s*D\s*\d+)?|WK\s*\d+|WEEK\s*\d+|SETT(?:IMANA)?\.?\s*\d+)\s*[:.)\-]?""")

    private val techniquePatterns: List<Pair<Regex, (MatchResult) -> Technique>> = listOf(
        Regex("""\bD\s*(\d{1,2})\s*F\s*(\d{1,2})\s*S\s*(\d{1,2})\b""") to { m ->
            Technique.Tempo("${m.groupValues[1]}-${m.groupValues[2]}-${m.groupValues[3]}")
        },
        Regex("""\bTEMPO\s*([0-9X](?:\s*[-/]?\s*[0-9X]){2,3})""") to { m ->
            val digits = m.groupValues[1].filter { it.isDigit() || it == 'X' }
            Technique.Tempo(digits.toList().joinToString("-"))
        },
        Regex("""(\d{1,2})\s*(?:"|'')\s*(?:DI\s*)?(?:STOP|FERMO|PAUSA|PAUSE|ISO(?:M[EÉ]TR(?:IA|IC[OA]?|IE))?)\b""") to { m ->
            Technique.Pause(m.groupValues[1].toInt().coerceAtLeast(1))
        },
        // ISO 2" = isometric hold (a pause) of 2 seconds
        Regex("""\bISO(?:M[EÉ]TR(?:IA|IC[OA]?|IE))?\b\s*(?:(\d{1,2})\s*(?:"|''|SEC(?:ONDI|ONDS|S)?\b|S\b)?)?""") to { m ->
            Technique.Pause(m.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1)
        },
        Regex("""\b(?:STOP|FERMO|FERMI|PAUSA|PAUSE|PAUSED|PAUSIERT|ARR[EÊ]T|PARADA)\b\s*(?:DI\s*)?(?:(\d{1,2})\s*(?:"|''|SEC(?:ONDI|ONDS|S)?\b|S\b)?)?""") to { m ->
            Technique.Pause(m.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1)
        },
        Regex("""\b(?:CATENE|CATENA|CHAINS?|KETTEN|CADENAS|CHA[IÎ]NES|CORRENTES)\b""") to { _ -> Technique.Chains },
        Regex("""\b(?:ELASTICI|ELASTICO|BANDS?|B[AÄ]NDER|BANDAS|[EÉ]LASTIQUES|EL[AÁ]STICOS)\b""") to { _ -> Technique.Bands },
        Regex("""\b(?:PIEDI\s*(?:SU|ALTI|SOLLEVATI)|FEET\s*UP|F[UÜ]SSE\s*HOCH|PIES\s*ARRIBA|PIEDS\s*LEV[EÉ]S|P[EÉ]S\s*(?:PARA\s*CIMA|ELEVADOS))\b""") to { _ -> Technique.FeetUp },
        Regex("""\b(?:GARA|COMP|COMPETITION|WETTKAMPF|COMPETICI[OÓ]N|COMP[EÉ]TITION|COMPETI[CÇ][AÃ]O)\b""") to { _ -> Technique.Competition },
        Regex("""\b(?:DEFICIT|D[EÉ]FICIT|DEFIZIT)\b""") to { _ -> Technique.Deficit },
        Regex("""\bEMOM\b""") to { _ -> Technique.Emom }
    )

    private val intensityPatterns: List<Pair<Regex, (MatchResult) -> Intensity>> = listOf(
        Regex("""(\d{1,3}(?:[.,]\d{1,2})?)\s*%""") to { m ->
            Intensity(IntensityType.PERCENT, m.groupValues[1].toNumber())
        },
        Regex("""@?\s*\bRPE\s*(\d{1,2}(?:[.,]\d)?)""") to { m ->
            Intensity(IntensityType.RPE, m.groupValues[1].toNumber())
        },
        Regex("""\+?\s*(\d{1,3}(?:[.,]\d{1,3})?)\s*(KG|LBS?)\b""") to { m ->
            val value = m.groupValues[1].toNumber() ?: 0f
            val kg = if (m.groupValues[2].startsWith("LB")) WeightUnitConverter.lbToKg(value) else value
            Intensity(IntensityType.WEIGHT, kg)
        },
        Regex("""@\s*(\d{1,2}(?:[.,]5)?)\b""") to { m ->
            Intensity(IntensityType.RPE, m.groupValues[1].toNumber())
        },
        Regex("""\b(?:BW|BODYWEIGHT|CORPO\s*LIBERO|PESO\s*CORPOREO|K[OÖ]RPERGEWICHT|PESO\s*CORPORAL|POIDS\s*DU\s*CORPS)\b""") to { _ ->
            Intensity(IntensityType.BODYWEIGHT, 0f)
        }
    )

    /** Time tokens (`2"`) that must never be read as reps. */
    private val secondsToken = Regex("""\d{1,3}\s*(?:"|'')""")

    private val volumePatterns: List<Pair<VolumeKind, Regex>> = listOf(
        VolumeKind.TOTAL to Regex("""(\d{1,3})\s*(?:REPS?|RIP(?:ETIZIONI)?|WDH)?\s*(?:ALSAP|AMRAP|TOT(?:ALI|ALE|ALES|AL)?)\b"""),
        VolumeKind.SETS_MAX to Regex("""(\d{1,2})\s*X\s*(?:MAX|AMRAP)\b"""),
        VolumeKind.SINGLES to Regex("""(\d{1,2})\s*(?:SINGOL[EI]|SINGLES?)\b"""),
        VolumeKind.SETS_REPS to Regex("""(\d{1,2})\s*X\s*(\d{1,3})\b"""),
        VolumeKind.LIST to Regex("""\d{1,3}(?:\s*-\s*\d{1,3})+"""),
        VolumeKind.REPS_WORD to Regex("""(\d{1,3})\s*(?:REPS?|RIP(?:ETIZIONI)?)\b"""),
        VolumeKind.MAX to Regex("""\b(?:MAX|AMRAP)\b"""),
        VolumeKind.NUMBER to Regex("""\b(\d{1,3})\b""")
    )

    private val leftoverPunctuation = Regex("""[+·,;:()\[\]{}|]""")

    fun parse(input: String): List<PrescriptionBlock> {
        val text = normalize(input)
        if (text.isBlank()) return emptyList()

        val consumed = BooleanArray(text.length)
        fun claim(range: IntRange) = range.forEach { consumed[it] = true }
        fun isFree(range: IntRange) = range.none { consumed[it] }

        // 1. Techniques
        val techniques = mutableListOf<Found<Technique>>()
        techniquePatterns.forEach { (regex, factory) ->
            regex.findAll(text).forEach { m ->
                val range = m.range.trimmed(text)
                if (!range.isEmpty() && isFree(range)) {
                    techniques += Found(range, factory(m))
                    claim(range)
                }
            }
        }

        // 2. Intensity markers
        val intensities = mutableListOf<Found<Intensity>>()
        intensityPatterns.forEach { (regex, factory) ->
            regex.findAll(text).forEach { m ->
                val range = m.range.trimmed(text)
                if (!range.isEmpty() && isFree(range)) {
                    intensities += Found(range, factory(m))
                    claim(range)
                }
            }
        }

        // 3. Volumes, searched on a copy where consumed text and seconds tokens are masked
        val searchChars = text.toCharArray()
        consumed.forEachIndexed { i, c -> if (c) searchChars[i] = ' ' }
        secondsToken.findAll(String(searchChars)).forEach { m -> m.range.forEach { searchChars[it] = ' ' } }
        val search = String(searchChars)

        val candidates = volumePatterns.flatMapIndexed { priority, (kind, regex) ->
            regex.findAll(search).map { Triple(priority, kind, it) }.toList()
        }.sortedWith(compareBy({ it.third.range.first }, { it.first }))

        val volumes = mutableListOf<Found<PrescriptionBlock>>()
        candidates.forEach { (_, kind, m) ->
            val range = m.range.trimmed(search)
            if (range.isEmpty() || !isFree(range)) return@forEach
            volumes += Found(range, volumeBlock(kind, m))
            claim(range)
        }
        volumes.sortBy { it.range.first }

        if (volumes.isEmpty()) {
            if (intensities.isEmpty() && techniques.isEmpty()) return emptyList()
            val intensity = combine(intensities.map { it.value })
            return listOf(
                PrescriptionBlock(
                    intensityType = intensity?.type ?: IntensityType.NONE,
                    intensityValue = intensity?.value,
                    techniques = techniques.sortedBy { it.range.first }.map { it.value } + leftovers(text, consumed, 0 until text.length)
                )
            )
        }

        // Assign intensity markers: each goes to the first volume that starts after it; trailing
        // markers go back to the last volume ("3x5 @RPE8").
        val markersByVolume = Array(volumes.size) { mutableListOf<Found<Intensity>>() }
        intensities.forEach { marker ->
            val target = volumes.indexOfFirst { it.range.first > marker.range.first }
            markersByVolume[if (target == -1) volumes.lastIndex else target] += marker
        }

        val starts = volumes.mapIndexed { i, v ->
            (markersByVolume[i].filter { it.range.first < v.range.first }.minOfOrNull { it.range.first } ?: v.range.first)
        }

        return volumes.mapIndexed { i, volume ->
            val spanStart = if (i == 0) 0 else starts[i]
            val spanEnd = if (i == volumes.lastIndex) text.length else starts[i + 1]
            val span = spanStart until spanEnd
            val blockTechniques = techniques.filter { it.range.first in span }.sortedBy { it.range.first }.map { it.value }
            val intensity = combine(markersByVolume[i].sortedBy { it.range.first }.map { it.value })
            volume.value.copy(
                intensityType = intensity?.type ?: IntensityType.NONE,
                intensityValue = intensity?.value,
                techniques = blockTechniques + leftovers(text, consumed, span)
            )
        }
    }

    data class ParsedProgram(
        val oneRepMaxKg: Float?,
        /** Weeks that have at least one block; unwritten weeks ("W5:") are omitted. */
        val weeks: Map<Int, List<PrescriptionBlock>>
    )

    private const val WEEK_LABEL = """(?:W|WK|WEEK|SETT(?:IMANA)?|SEM(?:ANA)?|SEMAINE|WOCHE)\.?"""
    private const val DAY_SUFFIX = """(?:\s*[-–]?\s*D\s*\d{1,2})?"""

    /** "W3:", "Week 3 -", "Sett. 3)", "W3D2:" or, at the start of a line, "W3 70% 5". */
    private val weekMarker = Regex(
        """(?im)(?:(?<![A-Z0-9])$WEEK_LABEL\s*(\d{1,2})$DAY_SUFFIX\s*[:.)\-–]|^[ \t]*$WEEK_LABEL\s*(\d{1,2})$DAY_SUFFIX(?=[ \t]))"""
    )
    private val headerOneRepMax = Regex("""(?i)\(\s*(\d{2,3}(?:[.,]\d{1,2})?)\s*(KG|LBS?)\s*\)""")
    private val looseOneRepMax = Regex("""(?i)(?:\b1\s*RM\s*[:=]?\s*(\d{2,3}(?:[.,]\d{1,2})?)\s*(KG|LBS?)?|\b(\d{2,3}(?:[.,]\d{1,2})?)\s*(KG|LBS?)\b)""")

    /**
     * Parses a multi-week sheet such as:
     * ```
     * PANCA PIANA (90kg)
     * W1: 80% 4x4 D4F2S4
     * W2: 85% 4x3 D4F2S4
     * W5:
     * ```
     * Text without week markers is treated as week 1.
     */
    fun parseProgram(input: String): ParsedProgram {
        val markers = weekMarker.findAll(input).toList()

        // The max is written next to the exercise name: "(90kg)", "90 kg" or "1RM 90" before the first week
        val headerText = if (markers.isEmpty()) input else input.substring(0, markers.first().range.first)
        val header = headerOneRepMax.find(input)
        var oneRepMaxKg = header?.let { toKg(it.groupValues[1], it.groupValues[2]) }
        val body = header?.let { input.removeRange(it.range) } ?: input
        if (oneRepMaxKg == null && markers.isNotEmpty()) {
            val loose = looseOneRepMax.find(headerText)
            if (loose != null) {
                val number = loose.groupValues[1].ifEmpty { loose.groupValues[3] }
                val unit = loose.groupValues[2].ifEmpty { loose.groupValues[4] }.ifEmpty { "KG" }
                oneRepMaxKg = toKg(number, unit)
            }
        }

        val bodyMarkers = weekMarker.findAll(body).toList()
        if (bodyMarkers.isEmpty()) {
            val blocks = parse(body)
            return ParsedProgram(oneRepMaxKg, if (blocks.isEmpty()) emptyMap() else mapOf(1 to blocks))
        }
        val weeks = bodyMarkers.mapIndexedNotNull { i, marker ->
            val end = bodyMarkers.getOrNull(i + 1)?.range?.first ?: body.length
            val blocks = parse(body.substring(marker.range.last + 1, end))
            val week = marker.groupValues[1].ifEmpty { marker.groupValues[2] }.toInt()
            if (blocks.isEmpty() || week <= 0) null else week to blocks
        }.toMap()
        return ParsedProgram(oneRepMaxKg, weeks)
    }

    private fun toKg(number: String, unit: String): Float? {
        val value = number.replace(',', '.').toFloatOrNull() ?: return null
        return if (unit.uppercase(Locale.ROOT).startsWith("LB")) WeightUnitConverter.lbToKg(value) else value
    }

    // BW followed by a load means added weight on a bodyweight movement.
    private fun combine(markers: List<Intensity>): Intensity? {
        if (markers.isEmpty()) return null
        val bodyweight = markers.firstOrNull { it.type == IntensityType.BODYWEIGHT }
        val weight = markers.lastOrNull { it.type == IntensityType.WEIGHT }
        return if (bodyweight != null && weight != null) {
            Intensity(IntensityType.BODYWEIGHT, weight.value)
        } else {
            markers.last()
        }
    }

    private fun volumeBlock(kind: VolumeKind, m: MatchResult): PrescriptionBlock {
        val g1 = m.groupValues.getOrNull(1)?.toIntOrNull()
        return when (kind) {
            VolumeKind.TOTAL -> PrescriptionBlock(sets = 1, repMode = RepMode.TOTAL, totalReps = g1)
            VolumeKind.SETS_MAX -> PrescriptionBlock(sets = g1 ?: 1, repMode = RepMode.AMRAP)
            VolumeKind.SINGLES -> PrescriptionBlock(sets = g1 ?: 1, reps = "1")
            VolumeKind.SETS_REPS -> PrescriptionBlock(sets = g1 ?: 1, reps = m.groupValues[2])
            VolumeKind.LIST -> {
                val reps = m.value.split("-").map { it.trim() }.filter { it.isNotEmpty() }
                PrescriptionBlock(sets = reps.size, reps = reps.joinToString("-"))
            }
            VolumeKind.REPS_WORD, VolumeKind.NUMBER -> PrescriptionBlock(sets = 1, reps = (g1 ?: 1).toString())
            VolumeKind.MAX -> PrescriptionBlock(sets = 1, repMode = RepMode.AMRAP)
        }
    }

    /** Unrecognised words ("+ INF.", "TENS 2\"", "(S.L.)") are kept as custom techniques. */
    private fun leftovers(text: String, consumed: BooleanArray, span: IntRange): List<Technique> {
        val chars = StringBuilder()
        for (i in span) chars.append(if (consumed[i]) ' ' else text[i])
        val cleaned = chars.toString()
            .replace(leftoverPunctuation, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (cleaned.none { it.isLetter() }) return emptyList()
        return listOf(Technique.Custom(cleaned))
    }

    /** Handwriting read by OCR: "70/." for "70%", a lone "150" that is really the "ISO" of `ISO 2"`. */
    private val slashDotPercent = Regex("""(?<=\d)\s*(?:/\s*\.|°\s*/\s*[O0]|٪|％)""")
    private val percentThenDot = Regex("""%\s*\.(?!\d)""")
    private val percentRange = Regex("""(\d{2,3})\s*[-–]\s*\d{2,3}\s*%""")
    private val misreadIso = Regex("""(?<![\dA-Z.,])(?:150|1S0|IS0|I50|15O|1SO|ISO)(?![\dA-Z])(?=\s*\d{1,2}\s*(?:"|''))""")
    private val loadPreposition = Regex("""\b(?:AL|AT|AU|A\s+LA|CON|WITH|MIT|AVEC)\s+(?=\d{2,3}(?:[.,]\d)?\s*(?:%|KG|LBS?))""")
    private val setsWord = Regex("""(\d{1,2})\s*(?:SERIE|SERIES|SETS?|S[ÉE]RIES?|S[ÄA]TZE)\s*(?:DA|X|OF|DE|VON|DE)?\s*(?=\d)""")

    private fun normalize(input: String): String {
        var s = input.uppercase(Locale.ROOT)
            .replace('×', 'X').replace('✕', 'X').replace('*', 'X')
            .replace('″', '"').replace('“', '"').replace('”', '"')
            .replace("’’", "''").replace('’', '\'').replace('′', '\'')
            .replace('\n', ' ').replace('\t', ' ')
        s = weekPrefix.replace(s, " ")
        s = slashDotPercent.replace(s, "%")
        s = percentThenDot.replace(s, "% ")
        s = percentRange.replace(s, "$1%")
        s = misreadIso.replace(s, "ISO")
        s = loadPreposition.replace(s, "")
        s = setsWord.replace(s) { m -> "${m.groupValues[1]}X" }
        return s
    }

    private fun IntRange.trimmed(text: String): IntRange {
        var start = first
        var end = last
        while (start <= end && text[start].isWhitespace()) start++
        while (end >= start && text[end].isWhitespace()) end--
        return start..end
    }

    private fun String.toNumber(): Float? = replace(',', '.').toFloatOrNull()
}
