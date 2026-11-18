package com.emanuel5014.trainable.domain.exercise

/**
 * The machine setup of an exercise (issue #67): free text the lifter types ("Seat 4, backrest 3"), shown above the sets
 * as one chip per item. Items are split on new lines, semicolons and commas, but not on a comma between two digits,
 * which is a decimal ("Seat 4,5" stays one item).
 */
object SetupNote {
    /** Longest text the editor accepts: a setup, not a diary. */
    const val MAX_LENGTH = 300

    /** The items to show as chips, in order, trimmed, empty ones dropped. */
    fun chips(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        val items = mutableListOf<String>()
        val current = StringBuilder()
        fun close() {
            current.toString().trim().takeIf { it.isNotEmpty() }?.let { items += it }
            current.clear()
        }
        text.forEachIndexed { i, c ->
            val splits = when (c) {
                '\n', '\r', ';' -> true
                ',' -> !(text.getOrNull(i - 1)?.isDigit() == true && text.getOrNull(i + 1)?.isDigit() == true)
                else -> false
            }
            if (splits) close() else current.append(c)
        }
        close()
        return items
    }

    /** The chips on one line, for the collapsed workout header. */
    fun oneLine(text: String?): String = chips(text).joinToString(" · ")

    /** What to store for what was typed: trimmed and cut to [MAX_LENGTH], null when nothing is left. */
    fun clean(text: String?): String? = text?.trim()?.take(MAX_LENGTH)?.trim()?.takeIf { it.isNotEmpty() }
}
