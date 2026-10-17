package com.emanuel5014.trainable.domain.csvimport

import java.text.Normalizer
import java.util.Locale

/** A column of the workout history CSV the app exports (the same file is what an import reads back). */
enum class CsvColumn(
    /** Read into the history; the others are recognised so the file does not look broken, but not imported. */
    val imported: Boolean = true
) {
    DATE, SESSION_ID, PLAN, EXERCISE, CATEGORY, TYPE, SET, WEIGHT, REPS, DURATION, DISTANCE, NOTE,
    SESSION_DURATION, WEEK, RPE, WARMUP, PARTS, PAUSES, ASSISTED, PARTIALS,
    /** The workout journal: how the workout went (1..5), its tags, its note (the name of a workout is the plan cell). */
    RATING, TAGS, JOURNAL_NOTE,
    EXTRA(imported = false), PERCENT(imported = false), TARGET_REPS(imported = false), TECHNIQUES(imported = false),
    PERFORMED_TECHNIQUE(imported = false), PROGRAM_CHANGED(imported = false), PROGRAM_ORIGINAL(imported = false)
}

/** What the number in a duration column counts. */
enum class DurationUnit(val millis: Long) { HOURS(3_600_000L), MINUTES(60_000L), SECONDS(1_000L) }

/** Which header cell of the file is which column. */
data class CsvHeader(
    val columns: Map<CsvColumn, Int>,
    /** Unit written next to the weight header ("kg", "lb"), null when the file does not say. */
    val weightUnit: String?,
    /** Unit written next to the session duration header ("min", "s", "h"), null when the file does not say. */
    val sessionDurationUnit: DurationUnit?,
    /** Header cells that match no known column, as written. */
    val unknown: List<String>
) {
    fun has(column: CsvColumn): Boolean = column in columns

    /** The columns without which the file is not a workout history. */
    val missingRequired: List<CsvColumn>
        get() = buildList {
            if (!has(CsvColumn.DATE)) add(CsvColumn.DATE)
            if (!has(CsvColumn.EXERCISE)) add(CsvColumn.EXERCISE)
            // A history with no measurement at all has nothing to import
            if (listOf(CsvColumn.WEIGHT, CsvColumn.REPS, CsvColumn.DURATION, CsvColumn.DISTANCE).none { has(it) }) {
                add(CsvColumn.REPS)
            }
        }
}

/**
 * Recognises the header the app writes. It is written in the language of the app at export time, so every language is
 * known; case, accents and punctuation do not matter, and a few plain English synonyms are accepted for files edited by
 * hand.
 */
object HistoryCsvColumns {
    private val aliases: Map<CsvColumn, List<String>> = mapOf(
        CsvColumn.DATE to listOf("date", "data", "fecha", "datum"),
        CsvColumn.SESSION_ID to listOf("session id", "id sessione", "id sesion", "id seance", "sitzungs id", "id sessao"),
        CsvColumn.PLAN to listOf("plan", "scheda", "programme", "plano", "routine", "workout", "program"),
        CsvColumn.EXERCISE to listOf("exercise", "esercizio", "ejercicio", "exercice", "ubung", "exercicio"),
        CsvColumn.CATEGORY to listOf("category", "categoria", "categorie", "kategorie"),
        CsvColumn.TYPE to listOf("type", "tipo", "typ"),
        CsvColumn.SET to listOf("set", "serie", "satz"),
        CsvColumn.WEIGHT to listOf("weight", "peso", "poids", "gewicht"),
        CsvColumn.REPS to listOf("reps", "repetitions", "ripetizioni", "repeticiones", "wiederholungen", "repeticoes"),
        CsvColumn.DURATION to listOf("duration", "durata", "duracion", "duree", "dauer", "duracao"),
        CsvColumn.DISTANCE to listOf("distance", "distanza", "distancia", "distanz"),
        CsvColumn.NOTE to listOf("note", "notes", "nota", "notas", "notiz"),
        CsvColumn.SESSION_DURATION to listOf(
            "session duration", "workout duration", "session length", "durata allenamento", "duracion sesion",
            "duree seance", "sitzungsdauer", "duracao sessao"
        ),
        CsvColumn.RATING to listOf("rating", "voto", "valoracion", "evaluation", "bewertung", "avaliacao"),
        CsvColumn.TAGS to listOf("tags", "tag", "etiquetas", "etichette", "etiquettes", "labels"),
        CsvColumn.JOURNAL_NOTE to listOf(
            "workout note", "workout notes", "session note", "nota allenamento", "nota sesion", "note seance",
            "sitzungsnotiz", "nota sessao"
        ),
        CsvColumn.WEEK to listOf("week", "settimana", "semana", "semaine", "woche"),
        CsvColumn.RPE to listOf("rpe"),
        CsvColumn.EXTRA to listOf("extra", "bonus"),
        CsvColumn.WARMUP to listOf("warm up", "warmup", "riscaldamento", "calentamiento", "echauffement", "aufwarmen", "aquecimento"),
        CsvColumn.PARTS to listOf("mini sets", "mini serie", "mini series", "minisatze"),
        CsvColumn.PAUSES to listOf("pauses", "pause", "pausas", "pausen"),
        CsvColumn.ASSISTED to listOf("assisted", "aiutate", "asistidas", "aidees", "mit hilfe", "assistidas"),
        CsvColumn.PARTIALS to listOf("partials", "parziali", "parciales", "partielles", "teilwdh", "parciais"),
        CsvColumn.PERCENT to listOf("% 1rm", "% massimale"),
        CsvColumn.TARGET_REPS to listOf("target reps", "rep target", "reps objetivo", "reps cible", "ziel wdh", "reps alvo"),
        CsvColumn.TECHNIQUES to listOf("techniques", "tecniche", "tecnicas", "techniken"),
        CsvColumn.PERFORMED_TECHNIQUE to listOf(
            "technique done", "tecnica eseguita", "tecnica realizada", "technique realisee", "ausgefuhrte technik"
        ),
        CsvColumn.PROGRAM_CHANGED to listOf(
            "program changed", "programma modificato", "programacion modificada", "programmation modifiee",
            "programm geandert", "programacao alterada"
        ),
        CsvColumn.PROGRAM_ORIGINAL to listOf(
            "original program", "programma originale", "programacion original", "programmation d origine",
            "ursprungliches programm", "programacao original"
        )
    )

    private val byAlias: Map<String, CsvColumn> =
        aliases.flatMap { (column, names) -> names.map { it to column } }.toMap()

    private val unitInBrackets = Regex("\\(([^)]*)\\)")

    private val marks = Regex("\\p{M}+")
    private val separators = Regex("[^\\p{L}\\p{N}%]+")
    private val folded = java.util.concurrent.ConcurrentHashMap<String, String>()

    /**
     * Lower case, no accents, every run of punctuation / spaces a single space: "Sitzungs-ID" and "sitzungs id" agree.
     * It runs for every row of a file, so plain ASCII takes a loop instead of the regex and normalizer, and a result is
     * remembered (the same few names come back thousands of times).
     */
    fun fold(text: String): String {
        folded[text]?.let { return it }
        val result = if (text.all { it.code < 128 }) foldAscii(text) else foldUnicode(text)
        if (folded.size > MAX_CACHED) folded.clear()
        folded[text] = result
        return result
    }

    private fun foldAscii(text: String): String {
        val out = StringBuilder(text.length)
        var pendingSpace = false
        for (ch in text) {
            if (ch.isLetterOrDigit() || ch == '%') {
                if (pendingSpace && out.isNotEmpty()) out.append(' ')
                pendingSpace = false
                out.append(ch.lowercaseChar())
            } else {
                pendingSpace = true
            }
        }
        return out.toString()
    }

    private fun foldUnicode(text: String): String =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(marks, "")
            .replace(separators, " ")
            .trim()

    private const val MAX_CACHED = 20_000

    fun read(cells: List<String>): CsvHeader {
        val columns = mutableMapOf<CsvColumn, Int>()
        val unknown = mutableListOf<String>()
        var weightUnit: String? = null
        var sessionDurationUnit: DurationUnit? = null
        cells.forEachIndexed { index, raw ->
            val title = raw.replace("﻿", "").trim()
            if (title.isEmpty()) return@forEachIndexed
            val bracket = unitInBrackets.find(title)?.groupValues?.get(1)
            val column = byAlias[fold(title.replace(unitInBrackets, " "))]
            when {
                column == null -> unknown.add(title)
                column in columns -> Unit
                else -> {
                    columns[column] = index
                    if (column == CsvColumn.WEIGHT) weightUnit = unitOf(bracket)
                    if (column == CsvColumn.SESSION_DURATION) sessionDurationUnit = durationUnitOf(bracket)
                }
            }
        }
        return CsvHeader(columns, weightUnit, sessionDurationUnit, unknown)
    }

    private fun durationUnitOf(text: String?): DurationUnit? = when (text?.trim()?.lowercase(Locale.ROOT)) {
        "min", "mins", "minute", "minutes", "m" -> DurationUnit.MINUTES
        "s", "sec", "secs", "second", "seconds" -> DurationUnit.SECONDS
        "h", "hr", "hrs", "hour", "hours" -> DurationUnit.HOURS
        else -> null
    }

    private fun unitOf(text: String?): String? = when (text?.trim()?.lowercase(Locale.ROOT)) {
        "kg", "kgs", "kilogram", "kilograms", "kilo", "kilos" -> "kg"
        "lb", "lbs", "pound", "pounds" -> "lb"
        else -> null
    }
}
