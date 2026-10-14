package com.emanuel5014.trainable.data.report

import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.ProgramEditKind
import com.emanuel5014.trainable.domain.prescription.ProgramEditScope
import com.emanuel5014.trainable.domain.prescription.RepRange
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * The report as one self-contained HTML page: no script, no font or image to download, semantic markup and a layout
 * that wraps instead of scrolling sideways, so it reads the same in the app's WebView, a phone browser, a desktop
 * browser or on paper. Several routines make one page, each with its own header and contents.
 */
class HtmlReportFormatter @Inject constructor() {

    fun format(report: PlanReport, languageCode: String = "en"): String = format(listOf(report), languageCode)

    fun format(reports: List<PlanReport>, languageCode: String = "en"): String {
        val page = Page(ReportStrings.forLanguage(languageCode), languageCode)
        val s = page.s
        val title = if (reports.size == 1) reports.first().planName else "Trainable"

        return buildString {
            append("<!DOCTYPE html>\n<html lang=\"${escapeHtml(s.languageTag)}\">\n<head>\n")
            append("<meta charset=\"UTF-8\">\n")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
            append("<meta name=\"color-scheme\" content=\"light dark\">\n")
            // Keeps a phone browser from turning "100 5" into a call link
            append("<meta name=\"format-detection\" content=\"telephone=no\">\n")
            append("<title>${escapeHtml(title)} - ${escapeHtml(s["report"])}</title>\n")
            append("<style>\n").append(ReportCss.STYLE).append("</style>\n")
            append("</head>\n<body id=\"top\">\n<main class=\"page\">\n")

            if (reports.size > 1) appendPlansIndex(page, reports)
            reports.forEachIndexed { index, report -> appendPlan(page, report, index) }
            if (reports.isEmpty()) append("<p class=\"empty\">${escapeHtml(s["no_data"])}</p>\n")

            append("<footer class=\"footer\">${escapeHtml(s["generated_by"])} ${escapeHtml(page.dateTime.format(Date()))}</footer>\n")
            append("</main>\n</body>\n</html>\n")
        }
    }

    /** Language, formats and words shared by every part of one page. Dates follow the language of the report, not the device. */
    private class Page(val s: ReportStrings, languageCode: String) {
        private val locale: Locale = Locale.forLanguageTag(languageCode)
        val date: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
        val dateTime: DateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        private val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        private val isoDateTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ROOT)

        fun day(millis: Long): String = date.format(Date(millis))
        fun isoDay(millis: Long): String = isoDate.format(Date(millis))
        fun isoMoment(millis: Long): String = isoDateTime.format(Date(millis))
    }

    // ── Page and routines ────────────────────────────────────────────────────────────────────────────────────────

    private fun StringBuilder.appendPlansIndex(page: Page, reports: List<PlanReport>) {
        val s = page.s
        append("<nav class=\"plans-nav\" aria-labelledby=\"plans-title\">\n")
        append("<p class=\"eyebrow\">Trainable</p>\n")
        append("<h2 class=\"section-title\" id=\"plans-title\">${escapeHtml(s["routines"])}</h2>\n")
        append("<ol class=\"toc-list\">\n")
        reports.forEachIndexed { index, report ->
            append("<li><a class=\"toc-link\" href=\"#plan-${index + 1}\">")
            append("<span class=\"toc-n\">${index + 1}</span>")
            append("<span class=\"toc-name\" dir=\"auto\">${escapeHtml(report.planName)}</span>")
            append("<span class=\"toc-meta\">${escapeHtml(count(s, report.totalSessions, "session_one", "sessions_unit"))}</span>")
            append("</a></li>\n")
        }
        append("</ol>\n</nav>\n")
    }

    private fun StringBuilder.appendPlan(page: Page, report: PlanReport, index: Int) {
        val s = page.s
        val id = "plan-${index + 1}"
        append("<section class=\"plan\" id=\"$id\" aria-labelledby=\"$id-title\">\n")

        append("<header>\n")
        append("<p class=\"eyebrow\">${escapeHtml(s["report"])}</p>\n")
        append("<h1 class=\"title\" id=\"$id-title\" dir=\"auto\">${escapeHtml(report.planName)}</h1>\n")
        if (!report.planNote.isNullOrBlank()) appendNote(s["routine_note"], report.planNote)

        val first = report.periodFirstSession
        val last = report.periodLastSession
        val period = when {
            first == null || last == null -> "-"
            page.day(first) == page.day(last) -> page.day(first)
            else -> "${page.day(first)} – ${page.day(last)}"
        }
        append("<div class=\"facts\">\n")
        appendFact(report.totalSessions.toString(), s["total_sessions"])
        appendFact(report.exercises.size.toString(), s["exercises"])
        // Where the athlete is in a periodized program
        if (report.weeksCount > 1) appendFact("${report.currentWeek} / ${report.weeksCount}", s["current_week"], wide = true)
        appendFact(period, s["period"], wide = true)
        append("</div>\n</header>\n")

        if (report.weeks.isNotEmpty()) appendWeeksOverview(page, report, id)

        if (report.exercises.isEmpty()) {
            append("<p class=\"empty\">${escapeHtml(s["no_data"])}</p>\n")
        } else {
            appendContents(page, report, id)
            append("<section aria-labelledby=\"$id-history\">\n")
            append("<h2 class=\"section-title\" id=\"$id-history\">${escapeHtml(s["exercises_history"])}</h2>\n")
            report.exercises.forEachIndexed { exerciseIndex, exercise ->
                appendExercise(page, report, id, exerciseIndex, exercise)
            }
            append("</section>\n")
        }
        append("</section>\n")
    }

    private fun StringBuilder.appendFact(value: String, label: String, wide: Boolean = false) {
        append("<div class=\"fact${if (wide) " fact-wide" else ""}\">")
        append("<span class=\"fact-value\">${escapeHtml(value)}</span>")
        append("<span class=\"fact-label\">${escapeHtml(label)}</span>")
        append("</div>\n")
    }

    /** Where to jump to: one tappable row per exercise, so a long routine can be crossed with a thumb. */
    private fun StringBuilder.appendContents(page: Page, report: PlanReport, planId: String) {
        val s = page.s
        append("<nav id=\"$planId-contents\" aria-labelledby=\"$planId-contents-title\">\n")
        append("<h2 class=\"section-title\" id=\"$planId-contents-title\">${escapeHtml(s["contents"])}</h2>\n")
        append("<ol class=\"toc-list\">\n")
        report.exercises.forEachIndexed { index, exercise ->
            append("<li><a class=\"toc-link\" href=\"#$planId-ex-${index + 1}\">")
            append("<span class=\"toc-n\">${index + 1}</span>")
            append("<span class=\"toc-name\" dir=\"auto\">${escapeHtml(exercise.exerciseName)}</span>")
            append("<span class=\"toc-meta\">${escapeHtml(count(s, exercise.summary.totalSets, "set_one", "sets_unit"))}</span>")
            append("</a></li>\n")
        }
        append("</ol>\n</nav>\n")
    }

    // ── Exercise ─────────────────────────────────────────────────────────────────────────────────────────────────

    private fun StringBuilder.appendExercise(page: Page, report: PlanReport, planId: String, index: Int, exercise: ExerciseReport) {
        val s = page.s
        val id = "$planId-ex-${index + 1}"
        val hasWorkingSets = exercise.sessions.any { session -> session.sets.any { !it.isWarmup } }

        append("<article class=\"exercise${if (exercise.isCurrentlyInPlan) "" else " is-removed"}\" id=\"$id\">\n")
        append("<details open>\n")
        // The row inside the summary does the flex layout: a summary that is a flex box itself is not the same in every browser
        append("<summary class=\"ex-head\"><span class=\"ex-row\">")
        append("<span class=\"ex-num\">${index + 1}</span>")
        append("<span class=\"ex-id\">")
        // A heading made of spans: a summary may hold phrasing content and nothing more
        append("<span class=\"ex-name\" role=\"heading\" aria-level=\"3\" dir=\"auto\">${escapeHtml(exercise.exerciseName)}</span>")
        append("<span class=\"ex-tags\">")
        if (exercise.muscleGroup.isNotBlank()) append("<span class=\"tag\">${escapeHtml(exercise.muscleGroup)}</span>")
        if (!exercise.isCurrentlyInPlan) append("<span class=\"tag tag-removed\">${escapeHtml(s["removed"])}</span>")
        append("</span></span>")
        append(CHEVRON_ICON)
        append("</span></summary>\n")

        append("<div class=\"ex-body\">\n")
        if (hasWorkingSets) {
            appendMetrics(page, exercise, report.weightUnit)
        }
        if (exercise.programEdits.isNotEmpty()) appendProgramEdits(page, exercise.programEdits)
        if (exercise.swapEvents.isNotEmpty()) appendSwaps(page, exercise.swapEvents)

        appendProgramAndWorkouts(page, report, exercise)

        append("<a class=\"back-link\" href=\"#$planId-contents\">$ARROW_UP_ICON${escapeHtml(s["to_contents"])}</a>\n")
        append("</div>\n</details>\n</article>\n")
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
                    Metric(s["max_volume"], "${formatNumber(summary.maxVolume)} $unit", dateHint(summary.maxVolumeDate)),
                    Metric(s["total_sets"], summary.totalSets.toString()),
                    summary.bestEstimatedOneRM?.let { Metric(s["estimated_1rm"], "${formatNumber(it)} $unit") }
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
                summary.totalKm.takeIf { it > 0f }?.let { Metric(s["total_distance"], "${formatNumber(it)} km") },
                Metric(s["longest_set"], formatSeconds(summary.bestSeconds)),
                Metric(s["total_sets"], summary.totalSets.toString())
            )
        }
    }

    // ── Workouts and sets ────────────────────────────────────────────────────────────────────────────────────────

    private fun StringBuilder.appendSessions(page: Page, kind: ReportExerciseKind, unit: String, sessions: List<ExerciseSessionEntry>, showWeek: Boolean) {
        append("<ol class=\"sessions\">\n")
        sessions.forEach { appendSession(page, kind, unit, it, showWeek) }
        append("</ol>\n")
    }

    private fun StringBuilder.appendSession(page: Page, kind: ReportExerciseKind, unit: String, session: ExerciseSessionEntry, showWeek: Boolean) {
        val s = page.s
        val working = session.sets.filter { !it.isWarmup }
        if (working.isEmpty()) return

        val (totalLabel, total) = sessionTotal(page, kind, unit, working)
        append("<li class=\"session\">\n")
        append("<div class=\"session-head\">")
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
                val volume = working.sumOf { (it.weight * it.reps).toDouble() }.toFloat()
                // Bodyweight sets carry no load: their reps are the only total there is
                if (volume > 0f) s["volume"] to "${formatNumber(volume)} $unit"
                else s["total_reps"] to working.sumOf { it.reps }.toString()
            }
            ReportExerciseKind.TIME_AND_REPS -> s["total_reps"] to working.sumOf { it.reps }.toString()
            ReportExerciseKind.TIME_AND_WEIGHT -> s["total_time"] to formatSeconds(working.sumOf { it.seconds ?: 0 })
            ReportExerciseKind.CARDIO -> {
                val km = working.sumOf { (it.distanceKm ?: 0f).toDouble() }.toFloat()
                val time = formatSeconds(working.sumOf { it.seconds ?: 0 })
                s["total_time"] to if (km > 0f) "$time · ${formatNumber(km)} km" else time
            }
        }
    }

    private fun StringBuilder.appendSet(page: Page, kind: ReportExerciseKind, unit: String, set: SetEntry) {
        val s = page.s
        append("<li class=\"set\">\n")
        append("<span class=\"set-n\"><span class=\"sr\">${escapeHtml(s["set"])} </span>${set.setNumber}</span>\n")
        append("<div class=\"set-body\">\n")

        append("<div class=\"set-line\"><span class=\"fig\">${figure(page, kind, unit, set)}</span>")
        set.rpe?.let { append("<span class=\"chip chip-rpe\">RPE ${formatNumber(it)}</span>") }
        append("</div>\n")

        val goal = goalChip(page, set)
        if (goal != null || set.planned.isNotEmpty() || set.performed.isNotEmpty()) {
            append("<div class=\"chips\">")
            goal?.let { append(it) }
            set.planned.forEach { append("<span class=\"chip chip-plan\" dir=\"auto\">${programText(it)}</span>") }
            set.performed.forEach { append("<span class=\"chip chip-done\" dir=\"auto\">${programText(it)}</span>") }
            append("</div>\n")
        }

        if (!set.note.isNullOrBlank()) {
            append("<p class=\"set-note\" role=\"note\">$NOTE_ICON<span class=\"txt\" dir=\"auto\">${escapeHtml(cleanNote(set.note))}</span></p>\n")
        }
        append("</div>\n</li>\n")
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
                val km = set.distanceKm?.takeIf { it > 0f }?.let { value(formatNumber(it)) + " " + small("km") }
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
        val text = "${page.s["goal"]} $target · $sign${kotlin.math.abs(difference)}"
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
                appendSessions(page, exercise.kind, unit, workouts, showWeek = true)
            }
            return
        }

        append("<h4 class=\"sub-title\">${escapeHtml(s["program_vs_done"])}</h4>\n")
        if (program.weeks.isEmpty()) {
            program.plain?.let { plain ->
                append("<div class=\"sheet\"><div class=\"sheet-label\">${escapeHtml(s["prescribed"])}</div>")
                append("<p class=\"sheet-plain\">${escapeHtml(plainText(page, plain))}</p></div>\n")
            }
            if (workouts.isEmpty()) {
                append("<p class=\"empty\">${escapeHtml(s["no_sets"])}</p>\n")
            } else {
                append("<div class=\"sheet-label done-label\">${escapeHtml(s["done"])}</div>\n")
                appendSessions(page, exercise.kind, unit, workouts, showWeek = true)
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
                append("<div class=\"sheet-label done-label\">${escapeHtml(s["done"])}</div>\n")
                appendSessions(page, exercise.kind, unit, done, showWeek = false)
            }
            append("</li>\n")
        }
        append("</ol>\n")

        // Workouts that belong to no week of the program (done before it was periodized, or on a week since removed)
        val covered = program.weeks.map { it.week }.toSet()
        val others = if (periodized) workouts.filter { it.week !in covered } else emptyList()
        if (others.isNotEmpty()) {
            append("<h4 class=\"sub-title\">${escapeHtml(s["other_workouts"])}</h4>\n")
            appendSessions(page, exercise.kind, unit, others, showWeek = true)
        }
    }

    /** What the sheet says for one week: its blocks, each with the rest and the note written on it. */
    private fun StringBuilder.appendSheet(page: Page, week: ProgramWeek) {
        val s = page.s
        append("<div class=\"sheet\"><div class=\"sheet-label\">${escapeHtml(s["prescribed"])}</div>")
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
        target.distanceKm?.let { parts += "${formatNumber(it)} km" }
        target.restSeconds?.let { parts += "${s["rest"]} ${formatSeconds(it)}" }
        return parts.joinToString(" · ").ifEmpty { "-" }
    }

    // ── Weeks of a program ───────────────────────────────────────────────────────────────────────────────────────

    /** The weeks of the program at a glance: which were trained, which were missed, which are still to come. */
    private fun StringBuilder.appendWeeksOverview(page: Page, report: PlanReport, planId: String) {
        val s = page.s
        append("<section aria-labelledby=\"$planId-weeks-title\">\n")
        append("<h2 class=\"section-title\" id=\"$planId-weeks-title\">${escapeHtml(s["program_weeks"])}</h2>\n")
        append("<ol class=\"week-rows\">\n")
        report.weeks.forEach { week ->
            val first = week.firstDate
            val last = week.lastDate
            val dates = when {
                first == null || last == null -> "-"
                page.day(first) == page.day(last) -> page.day(first)
                else -> "${page.day(first)} – ${page.day(last)}"
            }
            append("<li class=\"week-row ${weekClass(week.week, week.workouts, report.currentWeek, excluded = false)}\">")
            append("<span class=\"wr-n\">${escapeHtml(s["week_short"])} ${week.week}</span>")
            append("<span class=\"wr-dates\">${escapeHtml(dates)}</span>")
            append(weekChips(page, week.week, week.workouts, report.currentWeek, excluded = false))
            append("</li>\n")
        }
        append("</ol>\n</section>\n")
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
        if (week == currentWeek) append("<span class=\"chip chip-week\">${escapeHtml(s["week_current"])}</span>")
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

            append("<li class=\"event\">\n<div class=\"event-head\">")
            // The moment of the change: two workouts of the same day tell apart by it
            append("<time class=\"event-date\" datetime=\"${page.isoMoment(edit.createdAt)}\">${escapeHtml(page.dateTime.format(Date(edit.createdAt)))}</time>")
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
                append("<details class=\"full-program\"><summary class=\"full-toggle\">$CHEVRON_SMALL_ICON")
                append("${escapeHtml(s["program_edit_before"])} / ${escapeHtml(s["program_edit_after"])}</summary>\n")
                append(comparison)
                append("</details>\n")
            }
            append("</li>\n")
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
            append("<li class=\"event event-swap\">\n")
            append("<div class=\"event-head\"><time class=\"event-date\" datetime=\"${page.isoDay(swap.sessionDate)}\">${escapeHtml(page.day(swap.sessionDate))}</time></div>\n")
            append("<div class=\"swap-line\"><span dir=\"auto\">${escapeHtml(swap.originalExerciseName)}</span>")
            append("<span class=\"swap-arrow\" aria-hidden=\"true\">→</span>")
            append("<span dir=\"auto\">${escapeHtml(swap.replacementExerciseName)}</span></div>\n")
            append("</li>\n")
        }
        append("</ol>\n")
    }

    // ── Pieces ───────────────────────────────────────────────────────────────────────────────────────────────────

    private fun StringBuilder.appendNote(label: String, text: String) {
        append("<div class=\"note\" role=\"note\">$NOTE_ICON<div class=\"note-body\">")
        append("<div class=\"note-label\">${escapeHtml(label)}</div>")
        append("<p class=\"txt\" dir=\"auto\">${escapeHtml(cleanNote(text))}</p>")
        append("</div></div>\n")
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
            h > 0 -> String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec)
            else -> String.format(Locale.ROOT, "%d:%02d", m, sec)
        }
    }

    /** 100 → "100", 62.5 → "62.5", the same way the prescriptions of the sets write their numbers. */
    private fun formatNumber(value: Float): String = PrescriptionFormatter.number(value)

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private companion object {
        val BLANK_LINES = Regex("\n{3,}")
        val DIGITS_WITH_HYPHENS = Regex("""\d+(?:-\d+)+""")

        private const val ICON_ATTRS = "xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\""
        const val NOTE_ICON = "<svg $ICON_ATTRS width=\"16\" height=\"16\"><path d=\"M4 5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v9l-6 6H6a2 2 0 0 1-2-2z\"/><path d=\"M14 20v-4a2 2 0 0 1 2-2h4\"/></svg>"
        const val CHEVRON_ICON = "<svg class=\"ex-chevron\" $ICON_ATTRS width=\"20\" height=\"20\"><polyline points=\"6 9 12 15 18 9\"/></svg>"
        const val CHEVRON_SMALL_ICON = "<svg class=\"tg-chev\" $ICON_ATTRS width=\"16\" height=\"16\"><polyline points=\"6 9 12 15 18 9\"/></svg>"
        const val ARROW_UP_ICON = "<svg $ICON_ATTRS width=\"14\" height=\"14\"><path d=\"M12 19V5M5 12l7-7 7 7\"/></svg>"
    }
}
