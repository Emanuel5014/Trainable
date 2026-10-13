package com.emanuel5014.trainable.data.report

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class HtmlReportFormatter @Inject constructor() {

    fun format(report: PlanReport, languageCode: String = "en"): String {
        val strings = getStrings(languageCode)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        return buildString {
            append("<!DOCTYPE html>\n<html lang=\"$languageCode\">\n<head>\n")
            append("<meta charset=\"UTF-8\">\n")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            append("<title>${escapeHtml(report.planName)} - ${strings["report"]}</title>\n")
            append("<style>\n")
            append(getEmbeddedCss())
            append("</style>\n")
            append("</head>\n<body>\n")

            append("<div class=\"container\">\n")

            append("<div class=\"page-header\">\n")
            append("<div class=\"page-label\">${strings["report"]}</div>\n")
            append("<h1 class=\"page-title\">${escapeHtml(report.planName)}</h1>\n")
            if (!report.planNote.isNullOrBlank()) {
                append("<p class=\"page-note\">${escapeHtml(report.planNote)}</p>\n")
            }
            append("</div>\n")

            append("<div class=\"stats-grid\">\n")
            append(buildStatCard(strings["total_sessions"] ?: "Total Sessions", report.totalSessions.toString(), "fitness_center"))
            append(buildStatCard(
                strings["period"] ?: "Period",
                if (report.periodFirstSession != null && report.periodLastSession != null) {
                    "${dateFormat.format(Date(report.periodFirstSession))} - ${dateFormat.format(Date(report.periodLastSession))}"
                } else "-",
                "date_range"
            ))
            append(buildStatCard(strings["exercises"] ?: "Exercises", report.exercises.size.toString(), "list"))
            append("</div>\n")

            if (report.exercises.isNotEmpty()) {
                append("<div class=\"section-header\">\n")
                append("<h2 class=\"section-title\">${strings["exercises_history"] ?: "Exercises History"}</h2>\n")
                append("</div>\n")

                report.exercises.forEachIndexed { index, exercise ->
                    append("<div class=\"exercise-card ${if (!exercise.isCurrentlyInPlan) "removed\" data-removed=\"true\"" else "\""}>\n")

                    append("<div class=\"exercise-header\">\n")
                    append("<div class=\"exercise-info\">\n")
                    append("<div class=\"exercise-number\">${index + 1}</div>\n")
                    append("<div>\n")
                    append("<h3 class=\"exercise-name\">${escapeHtml(exercise.exerciseName)}</h3>\n")
                    append("<div class=\"exercise-meta\">\n")
                    append("<span class=\"badge badge-neutral\">${escapeHtml(exercise.muscleGroup)}</span>\n")
                    if (!exercise.isCurrentlyInPlan) {
                        append("<span class=\"badge badge-removed\">${strings["removed"] ?: "Removed"}</span>\n")
                    }
                    append("</div>\n")
                    append("</div>\n")
                    append("</div>\n")
                    append("</div>\n")

                    append(buildExerciseSummary(exercise, report.weightUnit, strings))

                    val allSets = exercise.sessions.flatMap { it.sets }.filter { !it.isWarmup }
                    if (allSets.isNotEmpty()) {
                        append(buildExerciseTable(exercise, report.weightUnit, strings, dateFormat))
                    }

                    if (exercise.programEdits.isNotEmpty()) {
                        append(buildProgramEdits(exercise.programEdits, strings, dateTimeFormat))
                    }

                    if (exercise.swapEvents.isNotEmpty()) {
                        append("<div class=\"swap-section\">\n")
                        append("<div class=\"swap-title\">${strings["swap_history"] ?: "Swap History"}</div>\n")
                        exercise.swapEvents.sortedBy { it.sessionDate }.forEach { swap ->
                            append("<div class=\"swap-event\">\n")
                            append("<span class=\"swap-date\">${dateFormat.format(Date(swap.sessionDate))}</span>\n")
                            append("<span class=\"swap-text\">${escapeHtml(swap.originalExerciseName)} → ${escapeHtml(swap.replacementExerciseName)}</span>\n")
                            append("</div>\n")
                        }
                        append("</div>\n")
                    }

                    append("</div>\n")
                }
            }

            append("<div class=\"footer\">\n")
            append("<p class=\"footer-text\">${strings["generated_by"]} ${dateTimeFormat.format(Date())}</p>\n")
            append("</div>\n")

            append("</div>\n")
            append("</body>\n</html>")
        }
    }

    /** What was changed in the prescription of an exercise along the way: when, how, and what it was before and after. */
    private fun buildProgramEdits(
        edits: List<ProgramEditEvent>,
        strings: Map<String, String>,
        dateTimeFormat: SimpleDateFormat
    ): String = buildString {
        append("<div class=\"swap-section\">\n")
        append("<div class=\"swap-title\">${escapeHtml(strings["program_edits"] ?: "Program Changes")}</div>\n")
        edits.forEach { edit ->
            val kind = if (edit.kind == com.emanuel5014.trainable.domain.prescription.ProgramEditKind.LOAD) {
                strings["program_edit_kind_load"] ?: "Load"
            } else strings["program_edit_kind_program"] ?: "Program"
            val scope = if (edit.scope == com.emanuel5014.trainable.domain.prescription.ProgramEditScope.PLAN) {
                strings["program_edit_plan"] ?: "Routine updated"
            } else strings["program_edit_workout"] ?: "This workout only"
            val scopeClass = if (edit.scope == com.emanuel5014.trainable.domain.prescription.ProgramEditScope.PLAN) "edit-plan" else "edit-workout"
            append("<div class=\"edit-event\">\n")
            append("<div class=\"edit-head\">")
            // The moment of the change: two workouts of the same day tell apart by it
            append("<span class=\"swap-date\">${dateTimeFormat.format(Date(edit.createdAt))}</span>")
            append("<span class=\"edit-badge\">${escapeHtml(kind)}</span>")
            edit.week?.let { append("<span class=\"edit-badge\">${escapeHtml(strings["week_short"] ?: "Week")} $it</span>") }
            append("<span class=\"edit-badge $scopeClass\">${escapeHtml(scope)}</span>")
            append("</div>\n")
            edit.changes.forEach { line ->
                append("<div class=\"edit-line edit-change\">${escapeHtml(line)}</div>\n")
            }
            append("<div class=\"edit-line\"><span class=\"edit-label\">${escapeHtml(strings["program_edit_before"] ?: "Before")}</span>${escapeHtml(edit.before)}</div>\n")
            append("<div class=\"edit-line\"><span class=\"edit-label\">${escapeHtml(strings["program_edit_after"] ?: "After")}</span>${escapeHtml(edit.after)}</div>\n")
            append("</div>\n")
        }
        append("</div>\n")
    }

    /** The figures that sum an exercise up, by how it is measured. */
    private fun buildExerciseSummary(exercise: ExerciseReport, unit: String, strings: Map<String, String>): String = buildString {
        val summary = exercise.summary
        append("<div class=\"summary-grid\">\n")
        when (exercise.kind) {
            ReportExerciseKind.STRENGTH -> {
                append(buildSummaryItem(strings["max_weight"] ?: "Max Weight", "${formatWeight(summary.maxWeight)} $unit", "fitness_center"))
                append(buildSummaryItem(strings["max_volume"] ?: "Max Volume", "${formatWeight(summary.maxVolume)} $unit", "bar_chart"))
                append(buildSummaryItem(strings["total_sets"] ?: "Total Sets", summary.totalSets.toString(), "format_list_numbered"))
                if (summary.bestEstimatedOneRM != null) {
                    append(buildSummaryItem(strings["estimated_1rm"] ?: "Est. 1RM", "${formatWeight(summary.bestEstimatedOneRM)} $unit", "whatshot"))
                }
            }
            ReportExerciseKind.TIME_AND_WEIGHT -> {
                append(buildSummaryItem(strings["longest_set"] ?: "Longest Set", formatSeconds(summary.bestSeconds), "timer"))
                if (summary.maxWeight > 0f) {
                    append(buildSummaryItem(strings["max_weight"] ?: "Max Weight", "${formatWeight(summary.maxWeight)} $unit", "fitness_center"))
                }
                append(buildSummaryItem(strings["total_time"] ?: "Total Time", formatSeconds(summary.totalSeconds), "bar_chart"))
                append(buildSummaryItem(strings["total_sets"] ?: "Total Sets", summary.totalSets.toString(), "format_list_numbered"))
            }
            ReportExerciseKind.TIME_AND_REPS -> {
                append(buildSummaryItem(strings["best_reps"] ?: "Most Reps", summary.bestReps.toString(), "whatshot"))
                append(buildSummaryItem(strings["total_reps"] ?: "Total Reps", summary.totalReps.toString(), "bar_chart"))
                append(buildSummaryItem(strings["total_time"] ?: "Total Time", formatSeconds(summary.totalSeconds), "timer"))
                append(buildSummaryItem(strings["total_sets"] ?: "Total Sets", summary.totalSets.toString(), "format_list_numbered"))
            }
            ReportExerciseKind.CARDIO -> {
                append(buildSummaryItem(strings["total_time"] ?: "Total Time", formatSeconds(summary.totalSeconds), "timer"))
                if (summary.totalKm > 0f) {
                    append(buildSummaryItem(strings["total_distance"] ?: "Total Distance", "${formatWeight(summary.totalKm)} km", "bar_chart"))
                }
                append(buildSummaryItem(strings["longest_set"] ?: "Longest Set", formatSeconds(summary.bestSeconds), "whatshot"))
                append(buildSummaryItem(strings["total_sets"] ?: "Total Sets", summary.totalSets.toString(), "format_list_numbered"))
            }
        }
        append("</div>\n")
    }

    /** Every working set of the exercise, a session per block of rows; the columns follow how it is measured. */
    private fun buildExerciseTable(exercise: ExerciseReport, unit: String, strings: Map<String, String>, dateFormat: SimpleDateFormat): String = buildString {
        val kind = exercise.kind
        val hasNotes = exercise.sessions.any { it.sets.any { s -> !s.note.isNullOrBlank() } }
        val hasRpeColumn = kind == ReportExerciseKind.STRENGTH && exercise.sessions.any { it.sets.any { s -> s.rpe != null } }
        val hasPrescription = kind == ReportExerciseKind.STRENGTH && exercise.sessions.any { it.sets.any { s -> !s.prescription.isNullOrBlank() } }
        val hasWeight = kind == ReportExerciseKind.STRENGTH || (kind == ReportExerciseKind.TIME_AND_WEIGHT && exercise.summary.maxWeight > 0f)
        val hasReps = kind == ReportExerciseKind.STRENGTH || kind == ReportExerciseKind.TIME_AND_REPS
        val hasTime = kind != ReportExerciseKind.STRENGTH
        val hasDistance = kind == ReportExerciseKind.CARDIO && exercise.summary.totalKm > 0f
        // A session total that means something: tonnage for weights, reps for intervals, time for holds and cardio
        val sessionTotalLabel = when (kind) {
            ReportExerciseKind.STRENGTH -> strings["volume"] ?: "Volume"
            ReportExerciseKind.TIME_AND_REPS -> strings["total_reps"] ?: "Total Reps"
            else -> strings["total_time"] ?: "Total Time"
        }

        append("<div class=\"data-table-wrapper\">\n")
        append("<table class=\"data-table\">\n")
        append("<thead>\n<tr>\n")
        append("<th>${strings["date"] ?: "Date"}</th>\n")
        append("<th>${strings["set"] ?: "Set"}</th>\n")
        if (hasWeight) append("<th>${strings["weight"] ?: "Weight"}</th>\n")
        if (hasTime) append("<th>${strings["time"] ?: "Time"}</th>\n")
        if (hasReps) append("<th>${strings["reps"] ?: "Reps"}</th>\n")
        if (hasDistance) append("<th>${strings["distance"] ?: "Distance"}</th>\n")
        if (hasPrescription) append("<th>${strings["details"] ?: "Details"}</th>\n")
        append("<th>$sessionTotalLabel</th>\n")
        if (hasRpeColumn) append("<th>RPE</th>\n")
        if (hasNotes) append("<th>${strings["notes"] ?: "Notes"}</th>\n")
        append("</tr>\n</thead>\n<tbody>\n")

        exercise.sessions.forEach { session ->
            val working = session.sets.filter { !it.isWarmup }
            val sessionTotal = when (kind) {
                ReportExerciseKind.STRENGTH -> "${formatWeight(working.sumOf { (it.weight * it.reps).toDouble() }.toFloat())} $unit"
                ReportExerciseKind.TIME_AND_REPS -> working.sumOf { it.reps }.toString()
                else -> formatSeconds(working.sumOf { it.seconds ?: 0 })
            }
            val rowSpan = working.size
            working.forEachIndexed { setIndex, set ->
                append("<tr>\n")
                if (setIndex == 0) {
                    append(if (rowSpan > 1) "<td rowspan=\"$rowSpan\">" else "<td>")
                    append("${dateFormat.format(Date(session.date))}</td>\n")
                }
                append("<td><span class=\"set-number\">${set.setNumber}</span></td>\n")
                if (hasWeight) append("<td>${formatWeight(set.weight)} $unit</td>\n")
                if (hasTime) append("<td>${set.seconds?.let { formatSeconds(it) } ?: "-"}</td>\n")
                if (hasReps) append("<td>${set.reps}</td>\n")
                if (hasDistance) append("<td>${set.distanceKm?.takeIf { it > 0f }?.let { "${formatWeight(it)} km" } ?: "-"}</td>\n")
                if (hasPrescription) append("<td>${set.prescription?.let { escapeHtml(it) } ?: "-"}</td>\n")
                if (setIndex == 0) {
                    append(if (rowSpan > 1) "<td rowspan=\"$rowSpan\">" else "<td>")
                    append("$sessionTotal</td>\n")
                }
                if (hasRpeColumn) {
                    append("<td>${set.rpe?.let { com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter.number(it) } ?: "-"}</td>\n")
                }
                if (hasNotes) append("<td>${set.note?.let { escapeHtml(it) } ?: ""}</td>\n")
                append("</tr>\n")
            }
        }

        append("</tbody>\n</table>\n")
        append("</div>\n")
    }

    /** "45s" for a short set, "12:30" or "1:05:00" for a longer one. */
    private fun formatSeconds(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val sec = seconds % 60
        return when {
            seconds < 60 -> "${seconds}s"
            h > 0 -> String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec)
            else -> String.format(Locale.ROOT, "%d:%02d", m, sec)
        }
    }

    private fun buildStatCard(label: String, value: String, icon: String): String {
        return """
            <div class="stat-card">
                <div class="stat-card-icon">${iconSvg(icon)}</div>
                <div class="stat-card-value">$value</div>
                <div class="stat-card-label">$label</div>
            </div>
        """.trimIndent()
    }

    private fun buildSummaryItem(label: String, value: String, icon: String): String {
        return """
            <div class="summary-item">
                ${iconSvg(icon)}
                <div class="summary-content">
                    <div class="summary-value">$value</div>
                    <div class="summary-label">$label</div>
                </div>
            </div>
        """.trimIndent()
    }

    private fun formatWeight(weight: Float): String {
        return if (weight == weight.toLong().toFloat()) {
            weight.toLong().toString()
        } else {
            String.format("%.2f", weight)
        }
    }

    private fun iconSvg(icon: String): String {
        val paths = when (icon) {
            "fitness_center" -> """<rect x="4" y="8" width="4" height="8" rx="1.5"/><line x1="8" y1="12" x2="16" y2="12" stroke-width="3"/><rect x="16" y="8" width="4" height="8" rx="1.5"/>"""
            "date_range" -> """<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>"""
            "list" -> """<line x1="9" y1="6" x2="21" y2="6"/><line x1="9" y1="12" x2="21" y2="12"/><line x1="9" y1="18" x2="21" y2="18"/><circle cx="4.5" cy="6" r="2"/><circle cx="4.5" cy="12" r="2"/><circle cx="4.5" cy="18" r="2"/>"""
            "bar_chart" -> """<line x1="12" y1="20" x2="12" y2="10"/><line x1="18" y1="20" x2="18" y2="4"/><line x1="6" y1="20" x2="6" y2="16"/>"""
            "format_list_numbered" -> """<line x1="10" y1="6" x2="21" y2="6"/><line x1="10" y1="12" x2="21" y2="12"/><line x1="10" y1="18" x2="21" y2="18"/><path d="M4 6h1v4"/><path d="M4 10h2"/><path d="M6 18H4c0-1 2-2 2-3s-1-1.5-2-1"/>"""
            "timer" -> """<circle cx="12" cy="13" r="8"/><line x1="12" y1="13" x2="12" y2="9"/><line x1="10" y1="2" x2="14" y2="2"/>"""
            "whatshot" -> """<path d="M12 22c-3.5 0-6-2.5-6-6 0-3 2-5.5 4-7s2-3 2-5c3 2 6 5 6 10 0 3.5-2.5 6-6 6z"/>"""
            else -> """<circle cx="12" cy="12" r="10"/>"""
        }
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">$paths</svg>"""
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun getStrings(languageCode: String): Map<String, String> {
        return when (languageCode) {
            "it" -> mapOf(
                "report" to "Report Scheda",
                "total_sessions" to "Sessioni Totali",
                "period" to "Periodo",
                "exercises" to "Esercizi",
                "exercises_history" to "Storico Esercizi",
                "removed" to "Rimosso",
                "max_weight" to "Peso Max",
                "max_volume" to "Volume Max",
                "total_sets" to "Serie Totali",
                "estimated_1rm" to "1RM Stimato",
                "date" to "Data",
                "set" to "Serie",
                "weight" to "Peso",
                "reps" to "Reps",
                "volume" to "Volume",
                "notes" to "Note",
                "prescription" to "Prescrizione",
                "details" to "Dettagli",
                "swap_history" to "Cambi Esercizi",
                "program_edits" to "Modifiche alla Programmazione",
                "program_edit_workout" to "Solo questo allenamento",
                "program_edit_plan" to "Scheda aggiornata",
                "program_edit_kind_load" to "Carico",
                "program_edit_kind_program" to "Programma",
                "program_edit_before" to "Prima",
                "program_edit_after" to "Dopo",
                "week_short" to "Sett.",
                "generated_by" to "Generato da Trainable il",
                "time" to "Tempo",
                "distance" to "Distanza",
                "total_time" to "Tempo Totale",
                "longest_set" to "Serie più Lunga",
                "best_reps" to "Reps Max",
                "total_reps" to "Reps Totali",
                "total_distance" to "Distanza Totale"
            )
            "es" -> mapOf(
                "report" to "Informe de Rutina",
                "total_sessions" to "Sesiones Totales",
                "period" to "Período",
                "exercises" to "Ejercicios",
                "exercises_history" to "Historial de Ejercicios",
                "removed" to "Eliminado",
                "max_weight" to "Peso Máx",
                "max_volume" to "Volumen Máx",
                "total_sets" to "Series Totales",
                "estimated_1rm" to "1RM Estimado",
                "date" to "Fecha",
                "set" to "Serie",
                "weight" to "Peso",
                "reps" to "Reps",
                "volume" to "Volumen",
                "notes" to "Notas",
                "prescription" to "Prescripción",
                "details" to "Detalles",
                "swap_history" to "Cambios de Ejercicios",
                "program_edits" to "Cambios de Programación",
                "program_edit_workout" to "Solo ese entrenamiento",
                "program_edit_plan" to "Rutina actualizada",
                "program_edit_kind_load" to "Carga",
                "program_edit_kind_program" to "Programa",
                "program_edit_before" to "Antes",
                "program_edit_after" to "Después",
                "week_short" to "Sem.",
                "generated_by" to "Generado por Trainable el",
                "time" to "Tiempo",
                "distance" to "Distancia",
                "total_time" to "Tiempo Total",
                "longest_set" to "Serie más Larga",
                "best_reps" to "Reps Máx",
                "total_reps" to "Reps Totales",
                "total_distance" to "Distancia Total"
            )
            "fr" -> mapOf(
                "report" to "Rapport de Routine",
                "total_sessions" to "Sessions Totales",
                "period" to "Période",
                "exercises" to "Exercices",
                "exercises_history" to "Historique des Exercices",
                "removed" to "Supprimé",
                "max_weight" to "Poids Max",
                "max_volume" to "Volume Max",
                "total_sets" to "Séries Totales",
                "estimated_1rm" to "1RM Estimé",
                "date" to "Date",
                "set" to "Série",
                "weight" to "Poids",
                "reps" to "Reps",
                "volume" to "Volume",
                "notes" to "Notes",
                "prescription" to "Prescription",
                "details" to "Détails",
                "swap_history" to "Changements d'Exercices",
                "program_edits" to "Modifications de Programmation",
                "program_edit_workout" to "Seulement cette séance",
                "program_edit_plan" to "Routine mise à jour",
                "program_edit_kind_load" to "Charge",
                "program_edit_kind_program" to "Programme",
                "program_edit_before" to "Avant",
                "program_edit_after" to "Après",
                "week_short" to "Sem.",
                "generated_by" to "Généré par Trainable le",
                "time" to "Temps",
                "distance" to "Distance",
                "total_time" to "Temps Total",
                "longest_set" to "Série la plus Longue",
                "best_reps" to "Reps Max",
                "total_reps" to "Reps Totales",
                "total_distance" to "Distance Totale"
            )
            "de" -> mapOf(
                "report" to "Trainingsbericht",
                "total_sessions" to "Gesamteinheiten",
                "period" to "Zeitraum",
                "exercises" to "Übungen",
                "exercises_history" to "Übungsverlauf",
                "removed" to "Entfernt",
                "max_weight" to "Max Gewicht",
                "max_volume" to "Max Volumen",
                "total_sets" to "Gesamt Sätze",
                "estimated_1rm" to "Gesch. 1RM",
                "date" to "Datum",
                "set" to "Satz",
                "weight" to "Gewicht",
                "reps" to "Wdh",
                "volume" to "Volumen",
                "notes" to "Notizen",
                "prescription" to "Vorgabe",
                "details" to "Details",
                "swap_history" to "Übungswechsel",
                "program_edits" to "Programmänderungen",
                "program_edit_workout" to "Nur dieses Training",
                "program_edit_plan" to "Routine aktualisiert",
                "program_edit_kind_load" to "Last",
                "program_edit_kind_program" to "Programm",
                "program_edit_before" to "Vorher",
                "program_edit_after" to "Nachher",
                "week_short" to "Woche",
                "generated_by" to "Erstellt von Trainable am",
                "time" to "Zeit",
                "distance" to "Distanz",
                "total_time" to "Gesamtzeit",
                "longest_set" to "Längster Satz",
                "best_reps" to "Max Wdh",
                "total_reps" to "Gesamt Wdh",
                "total_distance" to "Gesamtdistanz"
            )
            "pt" -> mapOf(
                "report" to "Relatório de Treino",
                "total_sessions" to "Sessões Totais",
                "period" to "Período",
                "exercises" to "Exercícios",
                "exercises_history" to "Histórico de Exercícios",
                "removed" to "Removido",
                "max_weight" to "Peso Máx",
                "max_volume" to "Volume Máx",
                "total_sets" to "Séries Totais",
                "estimated_1rm" to "1RM Estimado",
                "date" to "Data",
                "set" to "Série",
                "weight" to "Peso",
                "reps" to "Reps",
                "volume" to "Volume",
                "notes" to "Notas",
                "prescription" to "Prescrição",
                "details" to "Detalhes",
                "swap_history" to "Trocas de Exercícios",
                "program_edits" to "Alterações da Programação",
                "program_edit_workout" to "Só esse treino",
                "program_edit_plan" to "Rotina atualizada",
                "program_edit_kind_load" to "Carga",
                "program_edit_kind_program" to "Programa",
                "program_edit_before" to "Antes",
                "program_edit_after" to "Depois",
                "week_short" to "Sem.",
                "generated_by" to "Gerado pelo Trainable em",
                "time" to "Tempo",
                "distance" to "Distância",
                "total_time" to "Tempo Total",
                "longest_set" to "Série mais Longa",
                "best_reps" to "Reps Máx",
                "total_reps" to "Reps Totais",
                "total_distance" to "Distância Total"
            )
            else -> mapOf(
                "report" to "Routine Report",
                "total_sessions" to "Total Sessions",
                "period" to "Period",
                "exercises" to "Exercises",
                "exercises_history" to "Exercises History",
                "removed" to "Removed",
                "max_weight" to "Max Weight",
                "max_volume" to "Max Volume",
                "total_sets" to "Total Sets",
                "estimated_1rm" to "Est. 1RM",
                "date" to "Date",
                "set" to "Set",
                "weight" to "Weight",
                "reps" to "Reps",
                "volume" to "Volume",
                "notes" to "Notes",
                "prescription" to "Prescription",
                "details" to "Details",
                "swap_history" to "Exercise Swaps",
                "program_edits" to "Program Changes",
                "program_edit_workout" to "This workout only",
                "program_edit_plan" to "Routine updated",
                "program_edit_kind_load" to "Load",
                "program_edit_kind_program" to "Program",
                "program_edit_before" to "Before",
                "program_edit_after" to "After",
                "week_short" to "Week",
                "generated_by" to "Generated by Trainable on",
                "time" to "Time",
                "distance" to "Distance",
                "total_time" to "Total Time",
                "longest_set" to "Longest Set",
                "best_reps" to "Most Reps",
                "total_reps" to "Total Reps",
                "total_distance" to "Total Distance"
            )
        }
    }

    private fun getEmbeddedCss(): String = """
        @import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&display=swap');

        *, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }

        :root {
            --md-primary: #3A59D1;
            --md-on-primary: #FFFFFF;
            --md-primary-container: #DDE1FF;
            --md-on-primary-container: #00164F;
            --md-surface: #FBF8FF;
            --md-on-surface: #1B1B21;
            --md-on-surface-variant: #45454F;
            --md-surface-container: #EFECF4;
            --md-surface-container-high: #E9E7EE;
            --md-surface-container-highest: #E2E0E8;
            --md-outline-variant: #C6C4D0;
            --md-error: #BA1A1A;
            --md-success: #4CAF50;
            --md-tertiary: #75546F;
        }

        body {
            font-family: 'Outfit', sans-serif;
            background-color: var(--md-surface);
            color: var(--md-on-surface);
            line-height: 1.6;
            -webkit-font-smoothing: antialiased;
        }

        svg {
            display: block;
        }

        .container {
            max-width: 900px;
            margin: 0 auto;
            padding: 16px 12px;
        }

        .page-header {
            text-align: center;
            padding: 24px 0 16px;
        }

        .page-label {
            font-size: 0.75rem;
            font-weight: 700;
            color: var(--md-primary);
            letter-spacing: 2px;
            text-transform: uppercase;
            margin-bottom: 4px;
        }

        .page-title {
            font-size: clamp(1.4rem, 4vw, 2.5rem);
            font-weight: 800;
            letter-spacing: -0.03em;
            color: var(--md-on-surface);
        }

        .page-note {
            font-size: 0.9rem;
            color: var(--md-on-surface-variant);
            margin-top: 8px;
            font-style: italic;
        }

        .stats-grid {
            display: grid;
            grid-template-columns: 1fr;
            gap: 10px;
            margin: 16px 0;
        }

        .stat-card {
            background: var(--md-surface-container);
            padding: 14px 16px;
            border-radius: 16px;
            border: 1px solid var(--md-outline-variant);
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .stat-card-icon {
            width: 36px;
            height: 36px;
            border-radius: 10px;
            background: var(--md-primary-container);
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
        }

        .stat-card-icon svg {
            width: 18px;
            height: 18px;
            color: var(--md-on-primary-container);
        }

        .stat-card-value {
            font-size: 1.1rem;
            font-weight: 700;
        }

        .stat-card-label {
            font-size: 0.75rem;
            color: var(--md-on-surface-variant);
        }

        .section-header {
            margin: 28px 0 12px;
        }

        .section-title {
            font-size: 1.2rem;
            font-weight: 700;
            letter-spacing: -0.02em;
        }

        .exercise-card {
            background: var(--md-surface-container);
            border-radius: 16px;
            border: 1px solid var(--md-outline-variant);
            padding: 14px;
            margin-bottom: 14px;
        }

        .exercise-card[data-removed="true"] {
            opacity: 0.7;
            border-style: dashed;
        }

        .exercise-header {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
            margin-bottom: 12px;
        }

        .exercise-info {
            display: flex;
            gap: 10px;
            align-items: center;
        }

        .exercise-number {
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: var(--md-primary);
            color: var(--md-on-primary);
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 0.85rem;
            flex-shrink: 0;
        }

        .exercise-name {
            font-size: 1rem;
            font-weight: 700;
            margin-bottom: 4px;
        }

        .exercise-meta {
            display: flex;
            gap: 6px;
            flex-wrap: wrap;
        }

        .badge {
            display: inline-flex;
            align-items: center;
            padding: 3px 10px;
            border-radius: 100px;
            font-size: 0.7rem;
            font-weight: 600;
        }

        .badge-neutral {
            background: var(--md-surface-container-high);
            color: var(--md-on-surface-variant);
        }

        .badge-removed {
            background: color-mix(in srgb, var(--md-error), transparent 85%);
            color: var(--md-error);
        }

        .summary-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
            margin-bottom: 14px;
        }

        .summary-item {
            display: flex;
            align-items: center;
            gap: 8px;
            background: var(--md-surface-container-high);
            padding: 10px 12px;
            border-radius: 12px;
        }

        .summary-item svg {
            width: 18px;
            height: 18px;
            flex-shrink: 0;
            color: var(--md-primary);
        }

        .summary-value {
            font-size: 0.85rem;
            font-weight: 700;
        }

        .summary-label {
            font-size: 0.65rem;
            color: var(--md-on-surface-variant);
        }

        .data-table-wrapper {
            background: var(--md-surface-container-high);
            border-radius: 12px;
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
        }

        .data-table {
            width: 100%;
            min-width: 380px;
            border-collapse: collapse;
        }

        .data-table th {
            padding: 10px 12px;
            text-align: left;
            font-weight: 600;
            font-size: 0.75rem;
            color: var(--md-on-surface-variant);
            border-bottom: 1px solid var(--md-outline-variant);
            background: var(--md-surface-container-highest);
            white-space: nowrap;
        }

        .data-table td {
            padding: 8px 12px;
            border-bottom: 1px solid var(--md-outline-variant);
            font-size: 0.85rem;
            white-space: nowrap;
        }

        .data-table tr:last-child td { border-bottom: none; }

        .set-number {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 26px;
            height: 26px;
            border-radius: 50%;
            background: var(--md-primary-container);
            color: var(--md-on-primary-container);
            font-weight: 600;
            font-size: 0.75rem;
        }

        .swap-section {
            margin-top: 12px;
            padding-top: 12px;
            border-top: 1px solid var(--md-outline-variant);
        }

        .swap-title {
            font-size: 0.75rem;
            font-weight: 700;
            color: var(--md-on-surface-variant);
            letter-spacing: 0.5px;
            margin-bottom: 6px;
        }

        .swap-event {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 8px 10px;
            background: color-mix(in srgb, var(--md-tertiary), transparent 90%);
            border-radius: 10px;
            margin-bottom: 6px;
            font-size: 0.8rem;
        }

        .swap-date {
            font-weight: 600;
            color: var(--md-on-surface-variant);
            white-space: nowrap;
        }

        .swap-text {
            color: var(--md-on-surface);
        }

        .edit-event {
            padding: 10px 12px;
            background: color-mix(in srgb, var(--md-primary), transparent 92%);
            border-radius: 10px;
            margin-bottom: 8px;
            font-size: 0.8rem;
        }

        .edit-head {
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            gap: 6px;
            margin-bottom: 6px;
        }

        .edit-badge {
            padding: 2px 8px;
            border-radius: 8px;
            font-weight: 700;
            font-size: 0.7rem;
            background: color-mix(in srgb, var(--md-primary), transparent 85%);
            color: var(--md-primary);
        }

        .edit-badge.edit-plan {
            background: color-mix(in srgb, var(--md-tertiary), transparent 80%);
            color: var(--md-tertiary);
        }

        .edit-badge.edit-workout {
            background: var(--md-surface-container-high, rgba(128,128,128,0.15));
            color: var(--md-on-surface-variant);
        }

        .edit-line {
            color: var(--md-on-surface);
            font-weight: 600;
            margin-top: 2px;
        }

        .edit-change {
            color: var(--md-primary);
        }

        .edit-label {
            display: inline-block;
            min-width: 56px;
            margin-right: 6px;
            font-size: 0.65rem;
            font-weight: 800;
            letter-spacing: 0.5px;
            text-transform: uppercase;
            color: var(--md-on-surface-variant);
        }

        .footer {
            text-align: center;
            padding: 24px 0 12px;
            border-top: 1px solid var(--md-outline-variant);
            margin-top: 28px;
        }

        .footer-text {
            color: var(--md-on-surface-variant);
            font-size: 0.8rem;
        }

        @media print {
            body { background: white; }
            .container { max-width: 100%; padding: 16px; }
            .exercise-card { break-inside: avoid; }
        }

        @media (min-width: 768px) {
            .container {
                padding: 32px 24px;
            }

            .page-header {
                padding: 48px 0 32px;
            }

            .page-label {
                font-size: 0.8rem;
                margin-bottom: 8px;
            }

            .page-title {
                font-size: clamp(1.8rem, 4vw, 2.5rem);
            }

            .page-note {
                font-size: 1rem;
                margin-top: 12px;
            }

            .stats-grid {
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 16px;
                margin: 32px 0;
            }

            .stat-card {
                padding: 24px;
                border-radius: 24px;
                display: block;
            }

            .stat-card-icon {
                width: 44px;
                height: 44px;
                border-radius: 14px;
                margin-bottom: 12px;
            }

            .stat-card-icon svg {
                width: 22px;
                height: 22px;
            }

            .stat-card-value {
                font-size: 1.4rem;
                margin-bottom: 2px;
            }

            .stat-card-label {
                font-size: 0.85rem;
            }

            .section-header {
                margin: 48px 0 20px;
            }

            .section-title {
                font-size: 1.5rem;
            }

            .exercise-card {
                padding: 24px;
                border-radius: 24px;
                margin-bottom: 20px;
            }

            .exercise-info {
                gap: 16px;
            }

            .exercise-number {
                width: 40px;
                height: 40px;
                font-size: 1rem;
            }

            .exercise-name {
                font-size: 1.15rem;
            }

            .badge {
                padding: 4px 12px;
                font-size: 0.75rem;
            }

            .summary-grid {
                grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
                gap: 12px;
                margin-bottom: 20px;
            }

            .summary-item {
                padding: 12px 16px;
                border-radius: 16px;
                gap: 10px;
            }

            .summary-item svg {
                width: 20px;
                height: 20px;
            }

            .summary-value {
                font-size: 0.95rem;
            }

            .summary-label {
                font-size: 0.7rem;
            }

            .data-table-wrapper {
                border-radius: 16px;
                overflow: hidden;
            }

            .data-table {
                min-width: unset;
            }

            .data-table th {
                padding: 12px 16px;
                font-size: 0.8rem;
            }

            .data-table td {
                padding: 10px 16px;
                font-size: 0.9rem;
            }

            .set-number {
                width: 28px;
                height: 28px;
                font-size: 0.8rem;
            }

            .swap-section {
                margin-top: 16px;
                padding-top: 16px;
            }

            .swap-title {
                font-size: 0.8rem;
                margin-bottom: 8px;
            }

            .swap-event {
                padding: 8px 12px;
                border-radius: 12px;
                font-size: 0.85rem;
                gap: 12px;
            }

            .footer {
                padding: 40px 0 20px;
                margin-top: 48px;
            }

            .footer-text {
                font-size: 0.85rem;
            }
        }
    """.trimIndent()
}
