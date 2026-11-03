package com.emanuel5014.trainable.domain.csvimport

/** One record of a CSV file: its cells and the line it starts on (1-based, so the header is line 1). */
data class CsvRecord(val line: Int, val cells: List<String>)

/**
 * A small, forgiving CSV reader. Files a person edited in a spreadsheet are rarely clean, so it copes with a byte-order
 * mark, `;` or tab instead of `,` (Excel in many locales), CRLF / LF / CR line ends, quoted cells with doubled quotes and
 * line breaks inside them, and empty lines.
 */
object CsvParser {
    private val DELIMITERS = charArrayOf(',', ';', '\t')

    /** The separator the header line uses: the one it contains most outside quotes, `,` when it has none. */
    fun detectDelimiter(text: String): Char {
        val counts = IntArray(DELIMITERS.size)
        var inQuotes = false
        for (ch in text.removePrefix(BOM)) {
            if (ch == '"') inQuotes = !inQuotes
            else if (!inQuotes) {
                if (ch == '\n' || ch == '\r') {
                    // The first line with anything in it is the header
                    if (counts.any { it > 0 }) break
                } else {
                    val index = DELIMITERS.indexOf(ch)
                    if (index >= 0) counts[index]++
                }
            }
        }
        val best = counts.indices.maxByOrNull { counts[it] } ?: 0
        return if (counts[best] == 0) ',' else DELIMITERS[best]
    }

    fun parse(text: String, delimiter: Char = detectDelimiter(text)): List<CsvRecord> {
        val records = mutableListOf<CsvRecord>()
        val cells = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        // A quote only opens a quoted cell at its start: a 5" bar in the middle of a cell stays literal
        var cellStart = true
        var line = 1
        var recordLine = 1

        fun endCell() {
            cells.add(cell.toString())
            cell.setLength(0)
            cellStart = true
        }

        fun endRecord() {
            endCell()
            if (cells.any { it.isNotBlank() }) records.add(CsvRecord(recordLine, cells.toList()))
            cells.clear()
        }

        val source = text.removePrefix(BOM)
        var i = 0
        while (i < source.length) {
            val ch = source[i]
            when {
                inQuotes -> when {
                    ch == '"' && source.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                    ch == '"' -> inQuotes = false
                    else -> {
                        cell.append(ch)
                        if (ch == '\n') line++
                        else if (ch == '\r' && source.getOrNull(i + 1) != '\n') line++
                    }
                }
                ch == '"' && cellStart -> { inQuotes = true; cellStart = false }
                ch == delimiter -> endCell()
                ch == '\n' || ch == '\r' -> {
                    if (ch == '\r' && source.getOrNull(i + 1) == '\n') i++
                    endRecord()
                    line++
                    recordLine = line
                }
                else -> { cell.append(ch); cellStart = false }
            }
            i++
        }
        // The last record may have no line end
        if (cell.isNotEmpty() || cells.isNotEmpty()) endRecord()
        return records
    }

    private const val BOM = "﻿"
}
