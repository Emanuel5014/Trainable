package com.emanuel5014.trainable.data.report

import com.emanuel5014.trainable.util.DistanceUnitConverter
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.ProgramEditKind
import com.emanuel5014.trainable.domain.prescription.ProgramEditScope
import com.emanuel5014.trainable.domain.prescription.RepRange
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Clock

/**
 * The report as one self-contained HTML page drawn like the app (its font and the colours of the user's theme, see
 * [ReportTheme]). A routine reads in three parts: an overview, its exercises (one row each, opened for the figures,
 * the trend and the sheet week by week next to what was done) and its workouts month by month with their diary.
 * A phone gets a sticky bar to jump between the parts, a wide screen a sticky index of everything on the side.
 * Nothing is downloaded; the small script only marks where the reader is and opens what a link points at or what
 * gets printed, so the page reads the same without it. Several routines make one page.
 */
class HtmlReportFormatter {

    fun format(report: PlanReport, languageCode: String = "en", theme: ReportTheme = ReportTheme()): String =
        format(listOf(report), languageCode, theme)

    fun format(reports: List<PlanReport>, languageCode: String = "en", theme: ReportTheme = ReportTheme()): String {
        val page = Page(ReportStrings.forLanguage(languageCode), languageCode)
        val s = page.s
        val title = if (reports.size == 1) reports.first().planName else "Trainable"
        val generated = page.moment(Clock.System.now().toEpochMilliseconds())

        return buildString {
            append("<!DOCTYPE html>\n<html lang=\"${escapeHtml(s.languageTag)}\">\n<head>\n")
            append("<meta charset=\"UTF-8\">\n")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, viewport-fit=cover\">\n")
            append("<meta name=\"color-scheme\" content=\"light dark\">\n")
            // Keeps a phone browser from turning "100 5" into a call link
            append("<meta name=\"format-detection\" content=\"telephone=no\">\n")
            append("<title>${escapeHtml(title)} - ${escapeHtml(s["report"])}</title>\n")
            append("<style>\n").append(ReportCss.style(theme)).append("</style>\n")
            append("</head>\n<body>\n")
            append(SHAPES)
            append("<div class=\"shell\">\n")
            if (reports.isNotEmpty()) appendSideIndex(page, reports, generated)
            append("<main class=\"main\" id=\"top\">\n")

            if (reports.size > 1) appendPlansIndex(page, reports)
            reports.forEachIndexed { index, report -> appendPlan(page, report, index, single = reports.size == 1) }
            if (reports.isEmpty()) append("<p class=\"empty\">${escapeHtml(s["no_data"])}</p>\n")

            append("<footer class=\"footer\"><a class=\"to-top\" href=\"#top\">$ARROW_UP_ICON${escapeHtml(s["back_to_top"])}</a>")
            append("<p>${escapeHtml(s["generated_by"])} ${escapeHtml(generated)}</p></footer>\n")
            append("</main>\n</div>\n")
            append("<script>").append(SCRIPT).append("</script>\n")
            append("</body>\n</html>\n")
        }
    }

    /** Language, formats and words shared by every part of one page. Dates follow the language of the report, not the device. */
    private class Page(val s: ReportStrings, private val languageCode: String, val distanceUnit: String = DistanceUnitConverter.KM) {
        private val formats = ReportDateFormats(languageCode)

        /** The same page for a routine whose distances are written in [unit] ("km" or "mi"). */
        fun withDistanceUnit(unit: String): Page = if (unit == distanceUnit) this else Page(s, languageCode, unit)

        fun day(millis: Long): String = formats.day(millis)
        fun moment(millis: Long): String = formats.moment(millis)
        fun isoDay(millis: Long): String = formats.isoDay(millis)
        fun isoMoment(millis: Long): String = formats.isoMoment(millis)
        fun weekday(millis: Long): String = formats.weekday(millis)
        fun monthYear(millis: Long): String = formats.monthYear(millis)
        fun dayOfMonth(millis: Long): Int = formats.dayOfMonth(millis)
        fun monthKey(millis: Long): Int = formats.monthKey(millis)
    }

    // ── Index ────────────────────────────────────────────────────────────────────────────────────────────────────

    /** The index on the side of a wide screen: the parts of each routine and every exercise, always in reach. */
    private fun StringBuilder.appendSideIndex(page: Page, reports: List<PlanReport>, generated: String) {
        val s = page.s
        append("<aside class=\"side\" aria-label=\"${escapeHtml(s["contents"])}\"><div class=\"side-in\">\n")
        append("<p class=\"brand\">Trainable</p>\n<nav>\n")
        reports.forEachIndexed { index, report ->
            val id = "plan-${index + 1}"
            if (reports.size > 1) append("<p class=\"sb-plan\" dir=\"auto\">${escapeHtml(report.planName)}</p>\n")
            append("<ol class=\"sb\" data-spy>\n")
            append("<li><a class=\"sb-sec\" href=\"#$id-overview\">${escapeHtml(s["overview"])}</a></li>\n")
            if (report.exercises.isNotEmpty()) {
                append("<li><a class=\"sb-sec\" href=\"#$id-exercises\">${escapeHtml(s["exercises"])}</a>\n<ol class=\"sb-ex\" data-spy>\n")
                report.exercises.forEachIndexed { exerciseIndex, exercise ->
                    append("<li><a href=\"#$id-ex-${exerciseIndex + 1}\"><span class=\"sb-n\">${exerciseIndex + 1}</span>")
                    append("<span dir=\"auto\">${escapeHtml(exercise.exerciseName)}</span></a></li>\n")
                }
                append("</ol></li>\n")
            }
            if (workoutsOf(report).isNotEmpty()) append("<li><a class=\"sb-sec\" href=\"#$id-workouts\">${escapeHtml(s["workouts"])}</a></li>\n")
            append("</ol>\n")
        }
        append("</nav>\n<p class=\"side-foot\">${escapeHtml(s["generated_by"])} ${escapeHtml(generated)}</p>\n</div></aside>\n")
    }

    /** Several routines: one row each at the top of the page. */
    private fun StringBuilder.appendPlansIndex(page: Page, reports: List<PlanReport>) {
        val s = page.s
        append("<nav class=\"plans\" aria-labelledby=\"plans-title\">\n")
        append("<p class=\"eyebrow\">Trainable</p>\n")
        append("<div class=\"sec-head\"><h2 class=\"sec-title\" id=\"plans-title\">${escapeHtml(s["routines"])}</h2></div>\n")
        append("<ol class=\"rows\">\n")
        reports.forEachIndexed { index, report ->
            append("<li><a class=\"row\" href=\"#plan-${index + 1}\">${badge(index + 1)}")
            append("<span class=\"ex-main\"><span class=\"ex-name\" dir=\"auto\">${escapeHtml(report.planName)}</span>")
            append("<span class=\"ex-meta\">${escapeHtml(count(s, report.totalSessions, "session_one", "sessions_unit"))} · ")
            append("${escapeHtml(count(s, report.exercises.size, "exercise_one", "exercises_unit"))}</span></span></a></li>\n")
        }
        append("</ol>\n</nav>\n")
    }

    // ── A routine ────────────────────────────────────────────────────────────────────────────────────────────────

    private fun StringBuilder.appendPlan(documentPage: Page, report: PlanReport, index: Int, single: Boolean) {
        val page = documentPage.withDistanceUnit(report.distanceUnit)
        val s = page.s
        val id = "plan-${index + 1}"
        val workouts = workoutsOf(report)
        append("<section class=\"plan\" id=\"$id\" aria-labelledby=\"$id-title\">\n")

        append("<header>\n<p class=\"eyebrow\">${escapeHtml(s["report"])}</p>\n")
        append("<h1 class=\"title\" id=\"$id-title\" dir=\"auto\">${escapeHtml(report.planName)}</h1>\n")
        val subtitle = listOfNotNull(
            periodOf(page, report.periodFirstSession, report.periodLastSession),
            report.weeksCount.takeIf { it > 1 }?.let { s["program_length"].replace("{0}", it.toString()) }
        )
        if (subtitle.isNotEmpty()) append("<p class=\"subtitle\">${escapeHtml(subtitle.joinToString(" · "))}</p>\n")
        append("</header>\n")

        append("<nav class=\"tabs\" aria-label=\"${escapeHtml(s["sections"])}\"><ol data-spy>")
        append("<li><a href=\"#$id-overview\">${escapeHtml(s["overview"])}</a></li>")
        if (report.exercises.isNotEmpty()) append("<li><a href=\"#$id-exercises\">${escapeHtml(s["exercises"])}</a></li>")
        if (workouts.isNotEmpty()) append("<li><a href=\"#$id-workouts\">${escapeHtml(s["workouts"])}</a></li>")
        append("</ol></nav>\n")

        appendOverview(page, report, id)

        if (report.exercises.isEmpty()) {
            append("<p class=\"empty\" style=\"margin-top:26px\">${escapeHtml(s["no_data"])}</p>\n")
        } else {
            val totalSets = report.exercises.sumOf { it.summary.totalSets }
            append("<section class=\"sec\" id=\"$id-exercises\" aria-labelledby=\"$id-exercises-title\">\n")
            append("<div class=\"sec-head\"><h2 class=\"sec-title\" id=\"$id-exercises-title\">${escapeHtml(s["exercises"])}</h2>")
            append("<span class=\"pill\">${escapeHtml(count(s, report.exercises.size, "exercise_one", "exercises_unit"))} · ")
            append("${escapeHtml(count(s, totalSets, "set_one", "sets_unit"))}</span></div>\n")
            append("<ol class=\"rows\">\n")
            report.exercises.forEachIndexed { exerciseIndex, exercise -> appendExercise(page, report, id, exerciseIndex, exercise) }
            append("</ol>\n</section>\n")
        }

        if (workouts.isNotEmpty()) appendWorkouts(page, report, id, workouts)
        append("</section>\n")
    }

    private fun periodOf(page: Page, first: Long?, last: Long?): String? = when {
        first == null || last == null -> null
        page.day(first) == page.day(last) -> page.day(first)
        else -> "${page.day(first)} – ${page.day(last)}"
    }

    // ── Overview ─────────────────────────────────────────────────────────────────────────────────────────────────

    private fun StringBuilder.appendOverview(page: Page, report: PlanReport, planId: String) {
        val s = page.s
        append("<section class=\"sec\" id=\"$planId-overview\" aria-labelledby=\"$planId-overview-title\">\n")
        append("<div class=\"sec-head\"><h2 class=\"sec-title\" id=\"$planId-overview-title\">${escapeHtml(s["overview"])}</h2></div>\n")

        val volume = report.exercises.sumOf { exercise -> exercise.sessions.sumOf { volumeOf(exercise.kind, it.sets).toDouble() } }.toFloat()
        append("<ul class=\"stats\">\n")
        appendStat(report.totalSessions.toString(), s["workouts"])
        appendStat(report.exercises.sumOf { it.summary.totalSets }.toString(), s["total_sets"])
        if (volume > 0f) appendStat(volumeText(volume, report.weightUnit), s["volume"])
        appendStat(report.exercises.size.toString(), s["exercises"])
        append("</ul>\n")

        if (report.weeksCount > 1) {
            append("<div class=\"card\"><p class=\"card-k\">${escapeHtml(s["program"])}</p>")
            val current = s["week_of"].replace("{0}", report.currentWeek.toString()).replace("{1}", report.weeksCount.toString())
            append("<p class=\"card-t\">${escapeHtml(current)}</p>\n")
            if (report.weeks.isNotEmpty()) {
                append("<ol class=\"wk-strip\" aria-label=\"${escapeHtml(s["program_weeks"])}\">\n")
                report.weeks.forEach { week ->
                    val state = weekClass(week.week, week.workouts, report.currentWeek, excluded = false).ifEmpty { "is-done" }
                    val label = when {
                        week.week == report.currentWeek -> s["week_current"]
                        week.workouts > 0 -> count(s, week.workouts, "session_one", "sessions_unit")
                        week.week < report.currentWeek -> s["week_not_done"]
                        else -> s["week_upcoming"]
                    }
                    val dates = periodOf(page, week.firstDate, week.lastDate)
                    append("<li class=\"wk $state\"${dates?.let { " title=\"${escapeHtml(it)}\"" } ?: ""}>")
                    append("<span class=\"wk-n\">${escapeHtml(s["week_short"])} ${week.week}</span>")
                    append("<span class=\"wk-c\">${week.workouts}</span>")
                    append("<span class=\"wk-l\">${escapeHtml(label)}</span></li>\n")
                }
                append("</ol>\n")
            }
            append("</div>\n")
        }

        if (!report.planNote.isNullOrBlank()) appendNote(s["routine_note"], report.planNote)
        append("</section>\n")
    }

    private fun StringBuilder.appendStat(value: String, label: String) {
        append("<li class=\"stat\"><span class=\"stat-v\">${escapeHtml(value)}</span><span class=\"stat-l\">${escapeHtml(label)}</span></li>\n")
    }

    // ── Exercise ─────────────────────────────────────────────────────────────────────────────────────────────────

    private fun StringBuilder.appendExercise(page: Page, report: PlanReport, planId: String, index: Int, exercise: ExerciseReport) {
        val s = page.s
        val id = "$planId-ex-${index + 1}"
        val workouts = exercise.sessions.filter { session -> session.sets.any { !it.isWarmup } }
        val trend = trendOf(page, exercise, report.weightUnit, workouts)

        append("<li class=\"ex${if (exercise.isCurrentlyInPlan) "" else " is-removed"}\" id=\"$id\">\n<details>\n")
        append("<summary class=\"row\">${badge(index + 1)}<span class=\"ex-main\">")
        // A heading made of spans: a summary may hold phrasing content and nothing more
        append("<span class=\"ex-name\" role=\"heading\" aria-level=\"3\" dir=\"auto\">${escapeHtml(exercise.exerciseName)}</span>")
        append("<span class=\"ex-meta\">")
        val meta = listOfNotNull(exercise.muscleGroup.takeIf { it.isNotBlank() }, count(s, workouts.size, "session_one", "sessions_unit"))
        append(escapeHtml(meta.joinToString(" · ")))
        if (!exercise.isCurrentlyInPlan) append("<span class=\"tag tag-removed\">${escapeHtml(s["removed"])}</span>")
        append("</span></span>")
        trend?.let { t ->
            append("<span class=\"ex-fig\"><span class=\"ex-best\">${t.best}</span>")
            t.delta?.let { append("<span class=\"delta\">${escapeHtml(it)}</span>") }
            append("</span>")
            if (t.points.size > 1) append(sparkline(t.points.map { it.second }))
        }
        append(CHEVRON_ICON).append("</summary>\n")

        append("<div class=\"ex-body\">\n")
        if (workouts.isNotEmpty()) appendMetrics(page, exercise, report.weightUnit)
        if (trend != null && trend.points.size > 1) append(chart(page, trend, s["trend"]))
        if (exercise.programEdits.isNotEmpty()) appendProgramEdits(page, exercise.programEdits)
        if (exercise.swapEvents.isNotEmpty()) appendSwaps(page, exercise.swapEvents)
        appendProgramAndWorkouts(page, report, exercise)
        append("</div>\n</details>\n</li>\n")
    }

    /** The best of each workout (its heaviest set, most reps, longest set or distance) and what the row shows of it. */
    private class Trend(val best: String, val delta: String?, val points: List<Pair<Long, Float>>, val label: (Float) -> String)

    private fun trendOf(page: Page, exercise: ExerciseReport, unit: String, workouts: List<ExerciseSessionEntry>): Trend? {
        if (workouts.isEmpty()) return null
        val s = page.s
        val summary = exercise.summary
        fun working(session: ExerciseSessionEntry) = session.sets.filter { !it.isWarmup }
        fun small(text: String) = "<small> ${escapeHtml(text)}</small>"
        fun points(of: (List<SetEntry>) -> Float) = workouts.map { it.date to of(working(it)) }
        fun gain(points: List<Pair<Long, Float>>, text: (Float) -> String): String? {
            val first = points.first().second
            val best = points.maxOf { it.second }
            return (best - first).takeIf { it > 0f && points.size > 1 }?.let { "+${text(it)}" }
        }

        return when (exercise.kind) {
            ReportExerciseKind.STRENGTH -> if (summary.maxWeight > 0f) {
                val p = points { sets -> sets.maxOf { it.weight } }
                Trend(escapeHtml(formatNumber(summary.maxWeight)) + small(unit), gain(p) { formatNumber(it) }, p) { "${formatNumber(it)} $unit" }
            } else {
                val p = points { sets -> sets.maxOf { it.reps }.toFloat() }
                Trend(summary.bestReps.toString() + small(s["reps_unit"]), gain(p) { it.roundToInt().toString() }, p) { "${it.roundToInt()} ${s["reps_unit"]}" }
            }
            ReportExerciseKind.TIME_AND_REPS -> {
                val p = points { sets -> sets.maxOf { it.reps }.toFloat() }
                Trend(summary.bestReps.toString() + small(s["reps_unit"]), gain(p) { it.roundToInt().toString() }, p) { "${it.roundToInt()} ${s["reps_unit"]}" }
            }
            ReportExerciseKind.TIME_AND_WEIGHT -> {
                val p = points { sets -> sets.maxOf { it.seconds ?: 0 }.toFloat() }
                Trend(escapeHtml(formatSeconds(summary.bestSeconds)), null, p) { formatSeconds(it.roundToInt()) }
            }
            ReportExerciseKind.CARDIO -> if (summary.totalKm > 0f) {
                val p = points { sets -> sets.sumOf { (it.distanceKm ?: 0f).toDouble() }.toFloat() }
                val label = DistanceUnitConverter.unitLabel(page.distanceUnit)
                Trend(escapeHtml(formatNumber(DistanceUnitConverter.toDisplay(summary.totalKm, page.distanceUnit))) + small(label), null, p) { distance(page, it) }
            } else {
                val p = points { sets -> sets.sumOf { it.seconds ?: 0 }.toFloat() }
                Trend(escapeHtml(formatSeconds(summary.totalSeconds)), null, p) { formatSeconds(it.roundToInt()) }
            }
        }
    }

    private class Metric(val label: String, val value: String, val hint: String? = null)

    private fun StringBuilder.appendMetrics(page: Page, exercise: ExerciseReport, unit: String) {
        append("<ul class=\"metrics\">\n")
        metricsOf(page, exercise, unit).forEach { metric ->
            append("<li class=\"metric\">")
            append("<span class=\"metric-label\">${escapeHtml(metric.label)}</span>")
            append("<span class=\"metric-value\">${escapeHtml(metric.value)}</span>")
            metric.hint?.let { append("<span class=\"metric-hint\">${escapeHtml(it)}</span>") }
            append("</li>\n")
        }
        append("</ul>\n")
    }

    /** The figures that sum an exercise up, by how it is measured. */
    private fun metricsOf(page: Page, exercise: ExerciseReport, unit: String): List<Metric> {
        val s = page.s
        val summary = exercise.summary
        fun dateHint(millis: Long): String? = millis.takeIf { it > 0L }?.let { page.day(it) }

        return when (exercise.kind) {
            ReportExerciseKind.STRENGTH ->
                // Nothing was ever lifted (pull-ups, push-ups): a weight and a volume of 0 say nothing, the reps do
                if (summary.maxWeight <= 0f) listOf(
                    Metric(s["best_reps"], summary.bestReps.toString()),
                    Metric(s["total_reps"], summary.totalReps.toString()),
                    Metric(s["total_sets"], summary.totalSets.toString())
                ) else listOfNotNull(
                    Metric(s["max_weight"], "${formatNumber(summary.maxWeight)} $unit", dateHint(summary.maxWeightDate)),
                    summary.bestEstimatedOneRM?.let { Metric(s["estimated_1rm"], "${formatNumber(it)} $unit") },
                    Metric(s["max_volume"], volumeText(summary.maxVolume, unit), dateHint(summary.maxVolumeDate)),
                    Metric(s["total_sets"], summary.totalSets.toString())
                )
            ReportExerciseKind.TIME_AND_WEIGHT -> listOfNotNull(
                Metric(s["longest_set"], formatSeconds(summary.bestSeconds)),
                summary.maxWeight.takeIf { it > 0f }?.let { Metric(s["max_weight"], "${formatNumber(it)} $unit", dateHint(summary.maxWeightDate)) },
                Metric(s["total_time"], formatSeconds(summary.totalSeconds)),
                Metric(s["total_sets"], summary.totalSets.toString())
            )
            ReportExerciseKind.TIME_AND_REPS -> listOf(
                Metric(s["best_reps"], summary.bestReps.toString()),
                Metric(s["total_reps"], summary.totalReps.toString()),
                Metric(s["total_time"], formatSeconds(summary.totalSeconds)),
                Metric(s["total_sets"], summary.totalSets.toString())
            )
            ReportExerciseKind.CARDIO -> listOfNotNull(
                Metric(s["total_time"], formatSeconds(summary.totalSeconds)),
                summary.totalKm.takeIf { it > 0f }?.let { Metric(s["total_distance"], distance(page, it)) },
                Metric(s["longest_set"], formatSeconds(summary.bestSeconds)),
                Metric(s["total_sets"], summary.totalSets.toString())
            )
        }
    }

    // ── Charts ───────────────────────────────────────────────────────────────────────────────────────────────────

    /** A small line of the best of each workout, for the row of the exercise. */
    private fun sparkline(values: List<Float>): String {
        val w = 96f
        val h = 32f
        val low = values.min()
        val span = (values.max() - low).takeIf { it > 0f } ?: 1f
        val points = values.mapIndexed { i, v -> i * w / (values.size - 1) to h - 3f - (v - low) / span * (h - 6f) }
        val (lastX, lastY) = points.last()
        return "<svg class=\"spark\" viewBox=\"0 0 96 32\" preserveAspectRatio=\"none\" aria-hidden=\"true\">" +
            "<polyline points=\"${points.joinToString(" ") { "${svg(it.first)},${svg(it.second)}" }}\" vector-effect=\"non-scaling-stroke\"/>" +
            "<path class=\"dot\" d=\"M${svg(lastX)} ${svg(lastY)}h0\" vector-effect=\"non-scaling-stroke\"/></svg>"
    }

    /**
     * The best of each workout as a line: the drawing stretches to its box (strokes and dots keep their size) and the
     * labels are text around it, so they never scale with the width of the screen.
     */
    private fun chart(page: Page, trend: Trend, caption: String): String {
        val values = trend.points.map { it.second }
        val low = values.min()
        val high = values.max()
        val pad = ((high - low) * 0.15f).takeIf { it > 0f } ?: 1f
        val bottom = low - pad
        val range = high + pad - bottom
        val points = values.mapIndexed { i, v -> i * 100f / (values.size - 1) to 100f - (v - bottom) / range * 100f }
        val line = points.joinToString(" ") { "${svg(it.first)},${svg(it.second)}" }
        val area = "M0,100 L" + points.joinToString(" L") { "${svg(it.first)},${svg(it.second)}" } + " L100,100Z"
        val dots = points.mapIndexed { i, (x, y) ->
            "<path d=\"M${svg(x)} ${svg(y)}h0\"${if (values[i] == high) " class=\"best\"" else ""} vector-effect=\"non-scaling-stroke\"/>"
        }.joinToString("")
        val grid = listOf(25, 50, 75).joinToString("") { "<line x1=\"0\" x2=\"100\" y1=\"$it\" y2=\"$it\" vector-effect=\"non-scaling-stroke\"/>" }
        val first = trend.points.first().first
        val last = trend.points.last().first
        return "<figure class=\"chart\" aria-label=\"${escapeHtml(caption)}\"><div class=\"chart-plot\">" +
            "<span class=\"ax ax-top\">${escapeHtml(trend.label(high))}</span><span class=\"ax ax-bottom\">${escapeHtml(trend.label(low))}</span>" +
            "<svg viewBox=\"0 0 100 100\" preserveAspectRatio=\"none\" aria-hidden=\"true\"><g class=\"grid\">$grid</g>" +
            "<path class=\"area\" d=\"$area\"/><polyline class=\"line\" points=\"$line\" vector-effect=\"non-scaling-stroke\"/>" +
            "<g class=\"dots\">$dots</g></svg></div>" +
            "<figcaption><span>${escapeHtml(page.day(first))}</span><span>${escapeHtml(page.day(last))}</span></figcaption></figure>\n"
    }

    /** A coordinate with at most two decimals, so the drawing stays small. */
    private fun svg(value: Float): String = ((value * 100f).roundToInt() / 100f).let {
        if (it == it.toInt().toFloat()) it.toInt().toString() else it.toString()
    }

    // ── Workouts and sets of an exercise ─────────────────────────────────────────────────────────────────────────

    private fun StringBuilder.appendSessions(page: Page, kind: ReportExerciseKind, unit: String, sessions: List<ExerciseSessionEntry>, showWeek: Boolean) {
        append("<ol class=\"sessions\">\n")
        sessions.forEach { appendSession(page, kind, unit, it, showWeek) }
        append("</ol>\n")
    }

    /** The newest workouts in view, the older ones folded under one line: a long history stays short to cross. */
    private fun StringBuilder.appendRecentSessions(
        page: Page, kind: ReportExerciseKind, unit: String, sessions: List<ExerciseSessionEntry>,
        showWeek: Boolean = true, recent: Int = RECENT_WORKOUTS
    ) {
        val newestFirst = sessions.sortedByDescending { it.date }
        appendSessions(page, kind, unit, newestFirst.take(recent), showWeek)
        val older = newestFirst.drop(recent)
        if (older.isNotEmpty()) {
            append("<details class=\"older\"><summary>$CHEVRON_ICON${escapeHtml(page.s["earlier_workouts"])} · ${older.size}</summary>\n")
            appendSessions(page, kind, unit, older, showWeek)
            append("</details>\n")
        }
    }

    private fun StringBuilder.appendSession(page: Page, kind: ReportExerciseKind, unit: String, session: ExerciseSessionEntry, showWeek: Boolean) {
        val s = page.s
        val working = session.sets.filter { !it.isWarmup }
        if (working.isEmpty()) return

        val (totalLabel, total) = sessionTotal(page, kind, unit, working)
        append("<li class=\"session\">\n<div class=\"session-head\">")
        append("<time class=\"session-date\" datetime=\"${page.isoDay(session.date)}\">${escapeHtml(page.day(session.date))}</time>")
        // Inside a week of the program the week is already said above
        if (showWeek) session.week?.let { append("<span class=\"chip chip-week\">${escapeHtml(s["week_short"])} $it</span>") }
        append("<span class=\"session-total\"><span class=\"lbl\">${escapeHtml(totalLabel)}</span> ${escapeHtml(total)}</span>")
        append("</div>\n")

        session.adherence?.let { adherence ->
            val setsUnit = if (adherence.setsPlanned == 1) s["set_one"] else s["sets_unit"]
            append("<div class=\"adherence\">")
            append(adherenceChip("${adherence.setsDone}/${adherence.setsPlanned} $setsUnit", adherence.setsDone >= adherence.setsPlanned))
            adherence.repsPlanned?.let { append(adherenceChip("${adherence.repsDone}/$it ${s["reps_unit"]}", adherence.repsDone >= it)) }
            append("</div>\n")
        }

        append("<ol class=\"sets\">\n")
        working.forEach { appendSet(page, kind, unit, it) }
        append("</ol>\n</li>\n")
    }

    /** A total of the workout that means something: tonnage for weights, reps for intervals, time for holds and cardio. */
    private fun sessionTotal(page: Page, kind: ReportExerciseKind, unit: String, working: List<SetEntry>): Pair<String, String> {
        val s = page.s
        return when (kind) {
            ReportExerciseKind.STRENGTH -> {
                val volume = volumeOf(kind, working)
                // Bodyweight sets carry no load: their reps are the only total there is
                if (volume > 0f) s["volume"] to "${formatNumber(volume)} $unit"
                else s["total_reps"] to working.sumOf { it.reps }.toString()
            }
            ReportExerciseKind.TIME_AND_REPS -> s["total_reps"] to working.sumOf { it.reps }.toString()
            ReportExerciseKind.TIME_AND_WEIGHT -> s["total_time"] to formatSeconds(working.sumOf { it.seconds ?: 0 })
            ReportExerciseKind.CARDIO -> {
                val km = working.sumOf { (it.distanceKm ?: 0f).toDouble() }.toFloat()
                val time = formatSeconds(working.sumOf { it.seconds ?: 0 })
                s["total_time"] to if (km > 0f) "$time · ${distance(page, km)}" else time
            }
        }
    }

    private fun StringBuilder.appendSet(page: Page, kind: ReportExerciseKind, unit: String, set: SetEntry) {
        val s = page.s
        append("<li class=\"set\">")
        append("<span class=\"set-n\"><span class=\"sr\">${escapeHtml(s["set"])} </span>${set.setNumber}</span>")
        append("<div class=\"set-body\">")

        // The figure, what the set was planned and done as, and its RPE on one line that wraps when it must
        append("<div class=\"set-line\"><span class=\"fig\">${figure(page, kind, unit, set)}</span>")
        val goal = goalChip(page, set)
        if (goal != null || set.planned.isNotEmpty() || set.performed.isNotEmpty()) {
            append("<span class=\"chips\">")
            goal?.let { append(it) }
            set.planned.forEach { append("<span class=\"chip chip-plan\" dir=\"auto\">${programText(it)}</span>") }
            set.performed.forEach { append("<span class=\"chip chip-done\" dir=\"auto\">${programText(it)}</span>") }
            append("</span>")
        }
        set.rpe?.let { append("<span class=\"rpe\">RPE ${formatNumber(it)}</span>") }
        append("</div>")

        if (!set.note.isNullOrBlank()) {
            append("<p class=\"set-note\" role=\"note\">$NOTE_ICON<span class=\"txt\" dir=\"auto\">${escapeHtml(cleanNote(set.note))}</span></p>")
        }
        append("</div></li>\n")
    }

    /** What the set was, by how the exercise is measured: "100 kg × 5", "22 reps · 0:40", "1:15", "10:00 · 1.5 km". */
    private fun figure(page: Page, kind: ReportExerciseKind, unit: String, set: SetEntry): String {
        fun value(text: String) = "<b>${escapeHtml(text)}</b>"
        fun small(text: String) = "<span class=\"u\">${escapeHtml(text)}</span>"
        val sep = " <span class=\"sep\">·</span> "
        val time = set.seconds?.let { formatSeconds(it) } ?: "-"
        val weight = if (set.weight > 0f) value(formatNumber(set.weight)) + " " + small(unit) else null
        val reps = value(set.reps.toString()) + " " + small(page.s["reps_unit"])

        return when (kind) {
            ReportExerciseKind.STRENGTH ->
                if (weight != null) weight + " <span class=\"x\">×</span> " + value(set.reps.toString()) else reps
            ReportExerciseKind.TIME_AND_WEIGHT -> listOfNotNull(weight, value(time)).joinToString(sep)
            ReportExerciseKind.TIME_AND_REPS -> reps + sep + value(time)
            ReportExerciseKind.CARDIO -> {
                val km = set.distanceKm?.takeIf { it > 0f }?.let {
                    value(formatNumber(DistanceUnitConverter.toDisplay(it, page.distanceUnit))) + " " + small(DistanceUnitConverter.unitLabel(page.distanceUnit))
                }
                listOfNotNull(value(time), km).joinToString(sep)
            }
        }
    }

    private fun adherenceChip(text: String, reached: Boolean): String =
        "<span class=\"chip ${if (reached) "chip-ok" else "chip-miss"}\">${escapeHtml(text)}</span>"

    /** The reps the set was meant to reach when it fell short or went past them, e.g. "Goal 5 · −1"; nothing when it was on target. */
    private fun goalChip(page: Page, set: SetEntry): String? {
        val target = set.targetReps ?: return null
        val range = RepRange.parse(target)
        val (low, high) = when {
            range != null -> range.min to range.max
            else -> target.trim().toIntOrNull()?.let { it to it } ?: return null
        }
        val difference = when {
            set.reps < low -> set.reps - low
            set.reps > high -> set.reps - high
            else -> return null
        }
        val sign = if (difference < 0) "−" else "+"
        val text = "${page.s["goal"]} $target · $sign${abs(difference)}"
        return "<span class=\"chip ${if (difference < 0) "chip-miss" else "chip-over"}\">${escapeHtml(text)}</span>"
    }

    // ── What the sheet asks for, next to what was done ──────────────────────────────────────────────────────────

    /**
     * The sheet and the workouts of an exercise: week by week for a program with blocks (what was prescribed, then what
     * was done), the plain target and then the workouts otherwise.
     */
    private fun StringBuilder.appendProgramAndWorkouts(page: Page, report: PlanReport, exercise: ExerciseReport) {
        val s = page.s
        val program = exercise.program
        val workouts = exercise.sessions.filter { session -> session.sets.any { !it.isWarmup } }
        val unit = report.weightUnit

        if (program == null) {
            if (workouts.isEmpty()) {
                append("<p class=\"empty\">${escapeHtml(s["no_sets"])}</p>\n")
            } else {
                append("<h4 class=\"sub-title\">${escapeHtml(s["workouts"])}</h4>\n")
                appendRecentSessions(page, exercise.kind, unit, workouts)
            }
            return
        }

        append("<h4 class=\"sub-title\">${escapeHtml(s["program_vs_done"])}</h4>\n")
        if (program.weeks.isEmpty()) {
            program.plain?.let { plain ->
                append("<div class=\"sheet\"><span class=\"sheet-label\">${escapeHtml(s["prescribed"])}</span>")
                append("<p class=\"sheet-plain\">${escapeHtml(plainText(page, plain))}</p></div>\n")
            }
            if (workouts.isEmpty()) {
                append("<p class=\"empty\" style=\"margin-top:8px\">${escapeHtml(s["no_sets"])}</p>\n")
            } else {
                append("<span class=\"sheet-label done-label\">${escapeHtml(s["done"])}</span>\n")
                appendRecentSessions(page, exercise.kind, unit, workouts)
            }
            return
        }

        val periodized = report.weeksCount > 1
        val byWeek = workouts.groupBy { it.week }
        append("<ol class=\"weeks\">\n")
        program.weeks.forEach { week ->
            val done = if (periodized) byWeek[week.week].orEmpty() else workouts
            val css = if (periodized) weekClass(week.week, done.size, report.currentWeek, week.excluded) else ""
            append("<li class=\"week $css\">\n")
            if (periodized) {
                append("<div class=\"week-head\"><span class=\"week-n\">${escapeHtml(s["week_short"])} ${week.week}</span>")
                append(weekChips(page, week.week, done.size, report.currentWeek, week.excluded))
                week.repeatsWeek?.let { append("<span class=\"week-repeat\">${escapeHtml(s["same_as"])} ${escapeHtml(s["week_short"])} $it</span>") }
                append("</div>\n")
            }
            if (!week.excluded) appendSheet(page, week)
            if (done.isNotEmpty()) {
                append("<span class=\"sheet-label done-label\">${escapeHtml(s["done"])}</span>\n")
                // A program run several times gathers many workouts in each week: the last ones say where it stands
                if (periodized) appendRecentSessions(page, exercise.kind, unit, done, showWeek = false, recent = RECENT_WORKOUTS_OF_A_WEEK)
                else appendRecentSessions(page, exercise.kind, unit, done)
            }
            append("</li>\n")
        }
        append("</ol>\n")

        // Workouts that belong to no week of the program (done before it was periodized, or on a week since removed)
        val covered = program.weeks.map { it.week }.toSet()
        val others = if (periodized) workouts.filter { it.week !in covered } else emptyList()
        if (others.isNotEmpty()) {
            append("<h4 class=\"sub-title\">${escapeHtml(s["other_workouts"])}</h4>\n")
            appendRecentSessions(page, exercise.kind, unit, others)
        }
    }

    /** What the sheet says for one week: its blocks, each with the rest and the note written on it. */
    private fun StringBuilder.appendSheet(page: Page, week: ProgramWeek) {
        val s = page.s
        append("<div class=\"sheet\"><span class=\"sheet-label\">${escapeHtml(s["prescribed"])}</span>")
        if (week.blocks.isEmpty()) {
            append("<p class=\"sheet-plain\">-</p></div>\n")
            return
        }
        append("<ol class=\"blocks\">")
        week.blocks.forEachIndexed { index, block ->
            append("<li><span class=\"blk-n\">${index + 1}</span><span class=\"blk-t\" dir=\"auto\">${programText(block.text)}")
            block.restSeconds?.let { append("<span class=\"blk-meta\">${escapeHtml(s["rest"])} ${formatSeconds(it)}</span>") }
            block.note?.let { append("<span class=\"blk-note txt\" dir=\"auto\">${escapeHtml(cleanNote(it))}</span>") }
            append("</span></li>")
        }
        append("</ol></div>\n")
    }

    /** "3 × 8–10 · Rest 1:30", "3 × 0:45", "10:00 · 1.5 km": the target of an exercise without blocks. */
    private fun plainText(page: Page, target: PlainTarget): String {
        val s = page.s
        val reps = target.reps
        val amount = reps ?: target.seconds?.let { formatSeconds(it) }
        val parts = mutableListOf<String>()
        when {
            amount == null -> Unit
            // A list of reps per set ("8-6-4-2") already says how many sets there are
            reps != null && reps.contains('-') -> parts += "$reps ${s["reps_unit"]}"
            target.sets != null -> parts += "${target.sets} × $amount"
            else -> parts += amount
        }
        target.distanceKm?.let { parts += distance(page, it) }
        target.restSeconds?.let { parts += "${s["rest"]} ${formatSeconds(it)}" }
        return parts.joinToString(" · ").ifEmpty { "-" }
    }

    private fun weekClass(week: Int, workouts: Int, currentWeek: Int, excluded: Boolean): String = when {
        excluded -> "is-excluded"
        workouts == 0 && week < currentWeek -> "is-missed"
        week == currentWeek -> "is-current"
        workouts == 0 -> "is-upcoming"
        else -> ""
    }

    /** What became of a week: how many workouts it had, or that it was missed, is under way, still to come or skipped on the sheet. */
    private fun weekChips(page: Page, week: Int, workouts: Int, currentWeek: Int, excluded: Boolean): String = buildString {
        val s = page.s
        when {
            excluded -> append("<span class=\"chip\">${escapeHtml(s["week_excluded"])}</span>")
            workouts > 0 -> append("<span class=\"chip chip-ok\">${escapeHtml(count(s, workouts, "session_one", "sessions_unit"))}</span>")
            week < currentWeek -> append("<span class=\"chip chip-miss\">${escapeHtml(s["week_not_done"])}</span>")
            week > currentWeek -> append("<span class=\"chip\">${escapeHtml(s["week_upcoming"])}</span>")
        }
        if (week == currentWeek) append("<span class=\"chip chip-current\">${escapeHtml(s["week_current"])}</span>")
    }

    // ── Program changes and swaps ────────────────────────────────────────────────────────────────────────────────

    /** What was changed in the prescription of an exercise along the way: when, how, and what it was before and after. */
    private fun StringBuilder.appendProgramEdits(page: Page, edits: List<ProgramEditEvent>) {
        val s = page.s
        append("<h4 class=\"sub-title\">${escapeHtml(s["program_edits"])}</h4>\n<ol class=\"events\">\n")
        edits.forEach { edit ->
            val kind = if (edit.kind == ProgramEditKind.LOAD) s["program_edit_kind_load"] else s["program_edit_kind_program"]
            val isPlan = edit.scope == ProgramEditScope.PLAN
            val scope = if (isPlan) s["program_edit_plan"] else s["program_edit_workout"]

            append("<li class=\"event\">$EDIT_ICON<div class=\"event-body\">\n<div class=\"event-head\">")
            // The moment of the change: two workouts of the same day tell apart by it
            append("<time class=\"event-date\" datetime=\"${page.isoMoment(edit.createdAt)}\">${escapeHtml(page.moment(edit.createdAt))}</time>")
            append("<span class=\"chip chip-kind\">${escapeHtml(kind)}</span>")
            edit.week?.let { append("<span class=\"chip chip-week\">${escapeHtml(s["week_short"])} $it</span>") }
            append("<span class=\"chip ${if (isPlan) "chip-scope-plan" else "chip-scope-workout"}\">${escapeHtml(scope)}</span>")
            append("</div>\n")

            val comparison = buildString {
                append("<div class=\"compare\">\n")
                appendBlocks(s["program_edit_before"], edit.before)
                appendBlocks(s["program_edit_after"], edit.after)
                append("</div>\n")
            }
            if (edit.changes.isEmpty()) {
                // Only something the blocks do not show changed: the two programs are all there is to see
                append(comparison)
            } else {
                append("<ul class=\"changes\">")
                edit.changes.forEach { append("<li dir=\"auto\">${programText(it)}</li>") }
                append("</ul>\n")
                // The whole program before and after is long on a phone: the lines above already say what moved
                append("<details class=\"full-program\"><summary class=\"full-toggle\">")
                append("${escapeHtml(s["program_edit_before"])} / ${escapeHtml(s["program_edit_after"])}$CHEVRON_SMALL_ICON</summary>\n")
                append(comparison)
                append("</details>\n")
            }
            append("</div></li>\n")
        }
        append("</ol>\n")
    }

    private fun StringBuilder.appendBlocks(label: String, blocks: List<String>) {
        append("<div class=\"compare-col\"><div class=\"compare-label\">${escapeHtml(label)}</div><ol class=\"blocks\">")
        if (blocks.isEmpty()) {
            append("<li><span class=\"blk-t none\">-</span></li>")
        } else {
            blocks.forEachIndexed { index, block ->
                append("<li><span class=\"blk-n\">${index + 1}</span><span class=\"blk-t\" dir=\"auto\">${programText(block)}</span></li>")
            }
        }
        append("</ol></div>\n")
    }

    private fun StringBuilder.appendSwaps(page: Page, swaps: List<SwapEvent>) {
        append("<h4 class=\"sub-title\">${escapeHtml(page.s["swap_history"])}</h4>\n<ol class=\"events\">\n")
        swaps.sortedBy { it.sessionDate }.forEach { swap ->
            append("<li class=\"event event-swap\">$SWAP_ICON<div class=\"event-body\">\n")
            append("<div class=\"event-head\"><time class=\"event-date\" datetime=\"${page.isoDay(swap.sessionDate)}\">${escapeHtml(page.day(swap.sessionDate))}</time></div>\n")
            append("<div class=\"swap-line\"><span dir=\"auto\">${escapeHtml(swap.originalExerciseName)}</span>")
            append("<span class=\"swap-arrow\" aria-hidden=\"true\">→</span>")
            append("<span dir=\"auto\">${escapeHtml(swap.replacementExerciseName)}</span></div>\n")
            append("</div></li>\n")
        }
        append("</ol>\n")
    }

    // ── Workouts of the routine ──────────────────────────────────────────────────────────────────────────────────

    /** One workout of the routine: what was done in it, exercise by exercise, and what the lifter wrote about it. */
    private class Workout(
        val date: Long,
        val week: Int?,
        val items: List<Pair<ExerciseReport, List<SetEntry>>>,
        val journal: JournalEntry?
    )

    /** The workouts of a routine, newest first: the exercises' workouts put back together by their date, with the diary. */
    private fun workoutsOf(report: PlanReport): List<Workout> {
        val journalByDate = report.journal.associateBy { it.date }
        val sessions = report.exercises.flatMap { exercise ->
            exercise.sessions.mapNotNull { session ->
                val working = session.sets.filter { !it.isWarmup }
                if (working.isEmpty()) null else Triple(session.date, session.week, exercise to working)
            }
        }.groupBy { it.first }
        val dates = (sessions.keys + journalByDate.keys).sortedDescending()
        return dates.map { date ->
            val items = sessions[date].orEmpty()
            Workout(date, items.firstNotNullOfOrNull { it.second }, items.map { it.third }, journalByDate[date])
        }
    }

    private fun StringBuilder.appendWorkouts(page: Page, report: PlanReport, planId: String, workouts: List<Workout>) {
        val s = page.s
        append("<section class=\"sec\" id=\"$planId-workouts\" aria-labelledby=\"$planId-workouts-title\">\n")
        append("<div class=\"sec-head\"><h2 class=\"sec-title\" id=\"$planId-workouts-title\">${escapeHtml(s["workouts"])}</h2>")
        append("<span class=\"pill\">${escapeHtml(count(s, workouts.size, "session_one", "sessions_unit"))}</span></div>\n")

        var index = 0
        workouts.groupBy { page.monthKey(it.date) }.values.forEach { month ->
            val volume = month.sumOf { workoutVolume(it).toDouble() }.toFloat()
            val head = listOfNotNull(count(s, month.size, "session_one", "sessions_unit"), volume.takeIf { it > 0f }?.let { volumeText(it, report.weightUnit) })
            append("<div class=\"month\"><div class=\"month-head\"><h3>${escapeHtml(page.monthYear(month.first().date))}</h3>")
            append("<span>${escapeHtml(head.joinToString(" · "))}</span></div>\n<ol class=\"rows\">\n")
            month.forEach { workout -> appendWorkout(page, report, "$planId-wo-${++index}", workout) }
            append("</ol></div>\n")
        }
        append("</section>\n")
    }

    private fun workoutVolume(workout: Workout): Float = workout.items.sumOf { (exercise, sets) -> volumeOf(exercise.kind, sets).toDouble() }.toFloat()

    private fun StringBuilder.appendWorkout(page: Page, report: PlanReport, id: String, workout: Workout) {
        val s = page.s
        val title = workout.week?.let { "${s["week"]} $it" } ?: page.day(workout.date)
        val volume = workoutVolume(workout)
        val meta = listOfNotNull(
            count(s, workout.items.size, "exercise_one", "exercises_unit"),
            count(s, workout.items.sumOf { it.second.size }, "set_one", "sets_unit"),
            volume.takeIf { it > 0f }?.let { volumeText(it, report.weightUnit) }
        )

        append("<li class=\"wo\" id=\"$id\"><details><summary class=\"row\">")
        append("<time class=\"dt\" datetime=\"${page.isoDay(workout.date)}\" title=\"${escapeHtml(page.day(workout.date))}\">")
        append("<b>${page.dayOfMonth(workout.date)}</b><span>${escapeHtml(page.weekday(workout.date))}</span></time>")
        append("<span class=\"ex-main\"><span class=\"ex-name\" role=\"heading\" aria-level=\"3\">${escapeHtml(title)}</span>")
        append("<span class=\"ex-meta\">${escapeHtml(meta.joinToString(" · "))}</span>")
        val journal = workout.journal
        if (journal != null && (journal.rating != null || journal.tags.isNotEmpty())) {
            append("<span class=\"wo-j\">")
            journal.rating?.let { rating ->
                val label = escapeHtml("${s["rating"]}: $rating / 5")
                append("<span class=\"stars\" role=\"img\" aria-label=\"$label\">${"★".repeat(rating)}<span class=\"off\">${"★".repeat(5 - rating)}</span></span>")
            }
            journal.tags.forEach { append("<span class=\"tag\" dir=\"auto\">${escapeHtml(it)}</span>") }
            append("</span>")
        }
        append("</span>").append(CHEVRON_ICON).append("</summary>\n<div class=\"wo-body\">\n")

        journal?.note?.takeIf { it.isNotBlank() }?.let { note ->
            append("<div class=\"note\" role=\"note\">$NOTE_ICON<p class=\"note-body txt\" dir=\"auto\">${escapeHtml(cleanNote(note))}</p></div>\n")
        }
        if (workout.items.isNotEmpty()) {
            append("<ul class=\"wo-list\">\n")
            workout.items.forEach { (exercise, sets) ->
                append("<li><span class=\"wo-ex\" dir=\"auto\">${escapeHtml(exercise.exerciseName)}</span><span class=\"wo-sets\">")
                sets.forEach { append("<span class=\"sc\">${compactFigure(page, exercise.kind, report.weightUnit, it)}</span>") }
                append("</span></li>\n")
            }
            append("</ul>\n")
        }
        append("</div></details></li>\n")
    }

    /** A set in a few characters, for the list of a workout: "100kg × 5 @8", "12 reps", "1:15", "10:00 · 1.5km". */
    private fun compactFigure(page: Page, kind: ReportExerciseKind, unit: String, set: SetEntry): String {
        fun small(text: String) = "<span class=\"u\">${escapeHtml(text)}</span>"
        val x = "<span class=\"x\">×</span>"
        val dot = "<span class=\"x\">·</span>"
        val time = set.seconds?.let { escapeHtml(formatSeconds(it)) }
        val weight = if (set.weight > 0f) escapeHtml(formatNumber(set.weight)) + small(unit) else null
        val reps = "${set.reps}" + small(page.s["reps_unit"])
        val figure = when (kind) {
            ReportExerciseKind.STRENGTH -> if (weight != null) "$weight$x${set.reps}" else reps
            ReportExerciseKind.TIME_AND_WEIGHT -> listOfNotNull(weight, time).joinToString(dot)
            ReportExerciseKind.TIME_AND_REPS -> listOfNotNull(reps, time).joinToString(dot)
            ReportExerciseKind.CARDIO -> {
                val km = set.distanceKm?.takeIf { it > 0f }?.let {
                    escapeHtml(formatNumber(DistanceUnitConverter.toDisplay(it, page.distanceUnit))) + small(DistanceUnitConverter.unitLabel(page.distanceUnit))
                }
                listOfNotNull(time, km).joinToString(dot)
            }
        }
        return figure + (set.rpe?.let { "<span class=\"r\">@${escapeHtml(formatNumber(it))}</span>" } ?: "")
    }

    // ── Pieces ───────────────────────────────────────────────────────────────────────────────────────────────────

    private fun StringBuilder.appendNote(label: String, text: String) {
        append("<div class=\"note\" role=\"note\">$NOTE_ICON<div class=\"note-body\">")
        append("<div class=\"note-label\">${escapeHtml(label)}</div>")
        append("<p class=\"txt\" dir=\"auto\">${escapeHtml(cleanNote(text))}</p>")
        append("</div></div>\n")
    }

    /** The number of a row in the app's cookie shape. */
    private fun badge(number: Int): String =
        "<span class=\"badge\" aria-hidden=\"true\"><svg viewBox=\"0 0 40 40\"><use href=\"#cookie\"/></svg><span>$number</span></span>"

    /** Weight lifted: working sets with a load, weight × reps. */
    private fun volumeOf(kind: ReportExerciseKind, sets: List<SetEntry>): Float =
        if (kind != ReportExerciseKind.STRENGTH) 0f
        else sets.filter { !it.isWarmup }.sumOf { (it.weight * it.reps).toDouble() }.toFloat()

    /** "6.4 t" from a tonne up, as the app writes it; "850 kg" or "12450 lb" otherwise. */
    private fun volumeText(volume: Float, unit: String): String = when {
        unit == "kg" && volume >= 10_000f -> "${formatNumber((volume / 100f).roundToInt() / 10f)} t"
        unit == "kg" && volume >= 1_000f -> "${formatNumber((volume / 10f).roundToInt() / 100f)} t"
        else -> "${formatNumber(volume.roundToInt().toFloat())} $unit"
    }

    /** Text of a program (blocks, techniques): the numbers of a tempo ("3-1-1") stay together instead of breaking at the hyphens. */
    private fun programText(text: String): String =
        escapeHtml(text).replace(DIGITS_WITH_HYPHENS) { "<span class=\"nb\">${it.value}</span>" }

    /** "1 set", "8 sets": the word follows the number. */
    private fun count(s: ReportStrings, number: Int, one: String, many: String): String =
        "$number ${if (number == 1) s[one] else s[many]}"

    /** A note keeps its own line breaks (the style shows them) but not the blank runs around it. */
    private fun cleanNote(text: String): String =
        text.replace("\r\n", "\n").replace('\r', '\n').trim().replace(BLANK_LINES, "\n\n")

    /** "45s" for a short set, "12:30" or "1:05:00" for a longer one. */
    private fun formatSeconds(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val sec = seconds % 60
        return when {
            seconds < 60 -> "${seconds}s"
            h > 0 -> "$h:${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
            else -> "$m:${sec.toString().padStart(2, '0')}"
        }
    }

    /** 100 → "100", 62.5 → "62.5", the same way the prescriptions of the sets write their numbers. */
    private fun formatNumber(value: Float): String = PrescriptionFormatter.number(value)

    /** "1.5 km", or the same distance in miles when the user reads miles (stored values are always km). */
    private fun distance(page: Page, km: Float): String =
        "${formatNumber(DistanceUnitConverter.toDisplay(km, page.distanceUnit))} ${DistanceUnitConverter.unitLabel(page.distanceUnit)}"

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    companion object {
        /** The newest workouts an exercise shows before folding the older ones. */
        private const val RECENT_WORKOUTS = 4
        private const val RECENT_WORKOUTS_OF_A_WEEK = 2

        private val BLANK_LINES = Regex("\n{3,}")
        private val DIGITS_WITH_HYPHENS = Regex("""\d+(?:-\d+)+""")

        /**
         * The page in the app's own light or dark instead of the system's, for the preview inside the app (the shared
         * file keeps following whoever opens it).
         */
        fun withScheme(html: String, dark: Boolean): String =
            html.replaceFirst(Regex("<html lang=\"([^\"]*)\">"), "<html lang=\"$1\" data-scheme=\"${if (dark) "dark" else "light"}\">")

        /** Material 3 Expressive's nine-sided cookie, drawn once and reused by every numbered row. */
        private val COOKIE_PATH: String = buildString {
            append('M')
            val steps = 120
            for (k in 0 until steps) {
                val t = 2 * PI * k / steps
                val r = 19 * (1 - 0.075 + 0.075 * cos(9 * t))
                if (k > 0) append(" L")
                append(((20 + r * sin(t)) * 100).roundToInt() / 100.0).append(' ').append(((20 - r * cos(t)) * 100).roundToInt() / 100.0)
            }
            append('Z')
        }

        private val SHAPES = "<svg width=\"0\" height=\"0\" style=\"position:absolute\" aria-hidden=\"true\"><defs>" +
            "<symbol id=\"cookie\" viewBox=\"0 0 40 40\"><path d=\"$COOKIE_PATH\"/></symbol></defs></svg>\n"

        private const val ICON_ATTRS = "xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.2\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\""
        private const val NOTE_ICON = "<svg $ICON_ATTRS width=\"18\" height=\"18\"><path d=\"M4 5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v9l-6 6H6a2 2 0 0 1-2-2z\"/><path d=\"M14 20v-4a2 2 0 0 1 2-2h4\"/></svg>"
        private const val CHEVRON_ICON = "<svg class=\"chev\" $ICON_ATTRS width=\"20\" height=\"20\"><path d=\"m6 9 6 6 6-6\"/></svg>"
        private const val CHEVRON_SMALL_ICON = "<svg class=\"chev\" $ICON_ATTRS width=\"16\" height=\"16\"><path d=\"m6 9 6 6 6-6\"/></svg>"
        private const val ARROW_UP_ICON = "<svg $ICON_ATTRS width=\"18\" height=\"18\"><path d=\"M12 19V5M5 12l7-7 7 7\"/></svg>"
        private const val EDIT_ICON = "<svg $ICON_ATTRS width=\"18\" height=\"18\"><path d=\"M4 20h4L19 9l-4-4L4 16z\"/></svg>"
        private const val SWAP_ICON = "<svg $ICON_ATTRS width=\"18\" height=\"18\"><path d=\"M7 4 3 8l4 4M3 8h14M17 20l4-4-4-4M21 16H7\"/></svg>"

        /**
         * Optional: the page reads the same without it. Marks the part of the page in view in the tabs and the side
         * index, opens what a link points at, and opens everything before printing.
         */
        private const val SCRIPT = """
(function(){
  var groups=[].slice.call(document.querySelectorAll('[data-spy]'));
  function own(g){return [].slice.call(g.querySelectorAll('a[href^="#"]')).filter(function(a){return a.closest('[data-spy]')===g;});}
  function spy(){
    var line=window.innerHeight*0.3;
    groups.forEach(function(g){
      var links=own(g),current=-1;
      links.forEach(function(a,i){var t=document.getElementById(a.getAttribute('href').slice(1));if(t&&t.getBoundingClientRect().top<=line)current=i;});
      if(current<0&&g.closest('.tabs'))current=0;
      links.forEach(function(a,i){a.classList.toggle('on',i===current);});
    });
  }
  function open(id){var t=id&&document.getElementById(id);var d=t&&(t.tagName==='DETAILS'?t:t.querySelector(':scope > details'));if(d)d.open=true;}
  document.addEventListener('click',function(e){var a=e.target.closest&&e.target.closest('a[href^="#"]');if(a)open(a.getAttribute('href').slice(1));});
  window.addEventListener('scroll',spy,{passive:true});
  window.addEventListener('resize',spy);
  window.addEventListener('beforeprint',function(){[].forEach.call(document.querySelectorAll('details'),function(d){d.open=true;});});
  if(location.hash)open(location.hash.slice(1));
  spy();
})();
"""
    }
}
