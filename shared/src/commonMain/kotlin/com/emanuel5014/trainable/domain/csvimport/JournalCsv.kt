package com.emanuel5014.trainable.domain.csvimport

import com.emanuel5014.trainable.domain.journal.TagName

/**
 * The workout journal in the history CSV: three cells after the others (rating, tags, note), repeated on every row of
 * the workout like its duration. The export writes them ([cells]) and the import reads them back ([splitTags]).
 */
object JournalCsv {
    /** Between the tags of one workout; a tag name never holds a `|` (see [TagName.clean]). */
    const val TAG_SEPARATOR = " | "

    /**
     * ",rating,tags,note": each cell quoted when it needs to be, newlines of the note flattened to spaces (a row is one
     * line in this file). The cells are empty for what the workout does not have.
     */
    fun cells(rating: Int?, tagNames: List<String>, note: String?): String =
        "," + (rating?.toString() ?: "") +
            "," + escape(tagNames.joinToString(TAG_SEPARATOR)) +
            "," + escape(note?.takeIf { it.isNotBlank() } ?: "")

    /** The tags of a cell: split on `|` or `;`, each cleaned, blanks dropped, the same tag (by key) once. */
    fun splitTags(text: String): List<String> =
        text.split('|', ';')
            .mapNotNull { TagName.clean(it) }
            .distinctBy { TagName.key(it) }

    private fun escape(value: String): String {
        val flat = value.replace("\r", " ").replace("\n", " ")
        return if (flat.contains(',') || flat.contains('"')) "\"" + flat.replace("\"", "\"\"") + "\"" else flat
    }
}
