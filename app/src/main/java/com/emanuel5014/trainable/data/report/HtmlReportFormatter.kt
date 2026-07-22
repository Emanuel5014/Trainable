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

                    append("<div class=\"summary-grid\">\n")
                    append(buildSummaryItem(strings["max_weight"] ?: "Max Weight", "${formatWeight(exercise.summary.maxWeight)} kg", "fitness_center"))
                    append(buildSummaryItem(strings["max_volume"] ?: "Max Volume", "${formatWeight(exercise.summary.maxVolume)} kg", "bar_chart"))
                    append(buildSummaryItem(strings["total_sets"] ?: "Total Sets", exercise.summary.totalSets.toString(), "format_list_numbered"))
                    if (exercise.summary.bestEstimatedOneRM != null) {
                        append(buildSummaryItem(strings["estimated_1rm"] ?: "Est. 1RM", "${formatWeight(exercise.summary.bestEstimatedOneRM)} kg", "whatshot"))
                    }
                    append("</div>\n")

                    val allSets = exercise.sessions.flatMap { it.sets }.filter { !it.isWarmup }
                    if (allSets.isNotEmpty()) {
                        append("<div class=\"data-table-wrapper\">\n")
                        append("<table class=\"data-table\">\n")
                        append("<thead>\n<tr>\n")
                        append("<th>${strings["date"] ?: "Date"}</th>\n")
                        append("<th>${strings["set"] ?: "Set"}</th>\n")
                        append("<th>${strings["weight"] ?: "Weight"}</th>\n")
                        append("<th>${strings["reps"] ?: "Reps"}</th>\n")
                        append("<th>${strings["volume"] ?: "Volume"}</th>\n")
                        if (exercise.sessions.any { it.sets.any { s -> s.rpe != null } }) {
                            append("<th>RPE</th>\n")
                        }
                        append("</tr>\n</thead>\n<tbody>\n")

                        exercise.sessions.forEach { session ->
                            val sessionVolume = session.sets.filter { !it.isWarmup }.sumOf { (it.weight * it.reps).toDouble() }.toFloat()
                            val hasRpe = session.sets.any { it.rpe != null }

                            session.sets.filter { !it.isWarmup }.forEachIndexed { setIndex, set ->
                                append("<tr>\n")
                                if (setIndex == 0) {
                                    val rowSpan = session.sets.count { !it.isWarmup }
                                    if (rowSpan > 1) {
                                        append("<td rowspan=\"$rowSpan\">${dateFormat.format(Date(session.date))}</td>\n")
                                    } else {
                                        append("<td>${dateFormat.format(Date(session.date))}</td>\n")
                                    }
                                }
                                append("<td><span class=\"set-number\">${set.setNumber}</span></td>\n")
                                append("<td>${formatWeight(set.weight)} kg</td>\n")
                                append("<td>${set.reps}</td>\n")
                                if (setIndex == 0) {
                                    val rowSpan = session.sets.count { !it.isWarmup }
                                    if (rowSpan > 1) {
                                        append("<td rowspan=\"$rowSpan\">${formatWeight(sessionVolume)} kg</td>\n")
                                    } else {
                                        append("<td>${formatWeight(sessionVolume)} kg</td>\n")
                                    }
                                }
                                if (hasRpe) {
                                    append("<td>${set.rpe ?: "-"}</td>\n")
                                }
                                append("</tr>\n")
                            }
                        }

                        append("</tbody>\n</table>\n")
                        append("</div>\n")
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

    private fun buildStatCard(label: String, value: String, icon: String): String {
        return """
            <div class="stat-card">
                <div class="stat-card-icon"><span class="material-symbols-outlined">$icon</span></div>
                <div class="stat-card-value">$value</div>
                <div class="stat-card-label">$label</div>
            </div>
        """.trimIndent()
    }

    private fun buildSummaryItem(label: String, value: String, icon: String): String {
        return """
            <div class="summary-item">
                <span class="material-symbols-outlined summary-icon">$icon</span>
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
                "swap_history" to "Cambi Esercizi",
                "generated_by" to "Generato da Trainable il"
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
                "swap_history" to "Cambios de Ejercicios",
                "generated_by" to "Generado por Trainable el"
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
                "swap_history" to "Changements d'Exercices",
                "generated_by" to "Généré par Trainable le"
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
                "swap_history" to "Übungswechsel",
                "generated_by" to "Erstellt von Trainable am"
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
                "swap_history" to "Trocas de Exercícios",
                "generated_by" to "Gerado pelo Trainable em"
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
                "swap_history" to "Exercise Swaps",
                "generated_by" to "Generated by Trainable on"
            )
        }
    }

    private fun getEmbeddedCss(): String = """
        @import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&display=swap');
        @import url('https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0');

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

        .material-symbols-outlined {
            font-family: 'Material Symbols Outlined';
            font-weight: normal;
            font-style: normal;
            font-size: 24px;
            line-height: 1;
            letter-spacing: normal;
            text-transform: none;
            display: inline-block;
            white-space: nowrap;
            word-wrap: normal;
            direction: ltr;
            -webkit-font-feature-settings: 'liga';
            -webkit-font-smoothing: antialiased;
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

        .stat-card-icon .material-symbols-outlined {
            font-size: 18px;
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

        .summary-icon {
            font-size: 18px;
            color: var(--md-primary);
            flex-shrink: 0;
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

            .stat-card-icon .material-symbols-outlined {
                font-size: 22px;
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

            .summary-icon {
                font-size: 20px;
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
