package com.emanuel5014.trainable.util

import com.emanuel5014.trainable.data.remote.ReleaseNoteItem
import com.emanuel5014.trainable.data.remote.ReleaseNotes

/**
 * Reads the notes of releases published before `changelog.json`: their GitHub text is a markdown table
 * `| Category | Feature / Fix | Description |`, in English, with an emoji in the category. Each row becomes an item:
 * the first sentence of the description is the summary, the whole description the details. Text outside the table
 * (such as how to install a beta by hand) was meant for the GitHub page and is left out.
 */
object ReleaseNotesParser {

    /** The notes in [body], or null when it has no table (the sheet then shows the text as it is). */
    fun fromMarkdown(body: String): ReleaseNotes? {
        val items = body.lines().mapNotNull { line -> rowItem(line.trim()) }
        return if (items.isEmpty()) null else ReleaseNotes(items = items)
    }

    private fun rowItem(line: String): ReleaseNoteItem? {
        if (!line.startsWith("|")) return null
        val cells = line.trim('|').split("|").map { it.trim() }
        if (cells.size < 3 || cells.any { it.startsWith(":--") || it.startsWith("---") }) return null
        val category = clean(cells[0])
        if (category.equals("Category", ignoreCase = true)) return null
        val title = clean(cells[1])
        val (description, issues) = splitIssues(clean(cells.drop(2).joinToString(" | ")))
        if (title.isEmpty() && description.isEmpty()) return null
        val summary = firstSentence(description)
        return ReleaseNoteItem(
            type = if (isFix(category)) ReleaseNoteItem.TYPE_FIXED else ReleaseNoteItem.TYPE_NEW,
            area = areaOf(category),
            title = mapOf("en" to title.ifEmpty { summary }),
            summary = mapOf("en" to summary),
            details = if (description.length > summary.length) mapOf("en" to description) else emptyMap(),
            issues = issues
        )
    }

    private fun isFix(category: String): Boolean =
        category.contains("fix", ignoreCase = true) || category.contains("bug", ignoreCase = true)

    /** The area for an old category label such as "🏠 Dashboard" or "🫧 Liquid Glass". */
    fun areaOf(category: String): String {
        val text = category.lowercase()
        return AREA_KEYWORDS.firstOrNull { (keyword, _) -> keyword in text }?.second ?: "other"
    }

    /**
     * The first sentence (up to ". "), or the whole text when it is one sentence. A long one is cut at its first
     * colon or semicolon, which in these notes separates the gist from the particulars.
     */
    fun firstSentence(text: String): String {
        val end = Regex("""[.!?](\s|$)""").find(text)?.range?.first
        val sentence = (if (end == null) text else text.substring(0, end + 1)).trim().removeSuffix(".")
        if (sentence.length <= LONG_SUMMARY) return sentence
        val cut = sentence.indexOfAny(charArrayOf(':', ';'))
        return if (cut >= MIN_SUMMARY) sentence.substring(0, cut).trim() else sentence
    }

    private const val LONG_SUMMARY = 140
    private const val MIN_SUMMARY = 40

    /** "… the totals #58 #59" → ("… the totals", [58, 59]). */
    private fun splitIssues(text: String): Pair<String, List<Int>> {
        val issues = Regex("""(?<![\w/])#(\d+)\b""").findAll(text).map { it.groupValues[1].toInt() }.toList()
        val clean = text.replace(Regex("""\s*(?<![\w/])#\d+\b"""), "").trim()
        return clean to issues
    }

    /** Drops markdown bold and the emoji at the start of a category. */
    private fun clean(cell: String): String =
        cell.replace("**", "").replace("__", "").trim()
            .dropWhile { !it.isLetterOrDigit() && it != '"' && it != '(' }
            .trim()

    private val AREA_KEYWORDS = listOf(
        "update" to "updates",
        "dashboard" to "dashboard",
        "routine" to "routines",
        "history" to "history",
        "analytic" to "analytics",
        "onboarding" to "onboarding",
        "notification" to "notifications",
        "glass" to "liquid_glass",
        "workout" to "workouts",
        "exercise" to "exercises",
        "csv" to "import_export",
        "import" to "import_export",
        "export" to "import_export",
        "backup" to "backup",
        "nextcloud" to "backup",
        "timer" to "timers",
        "journal" to "journal",
        "widget" to "widgets",
        " ai" to "ai",
        "scan" to "ai",
        "theme" to "appearance",
        "personali" to "appearance",
        "appearance" to "appearance",
        "setting" to "settings",
    )
}
