package com.emanuel5014.trainable.domain.journal

import com.emanuel5014.trainable.domain.csvimport.HistoryCsvColumns
import java.util.Locale

/** Cuts the text to at most [max] characters without leaving half an emoji (a lone high surrogate) at the end. */
internal fun String.takeWholeChars(max: Int): String {
    if (length <= max) return this
    var end = max
    if (end > 0 && Character.isHighSurrogate(this[end - 1])) end--
    return substring(0, end)
}

/** The name of a workout tag: how it is cleaned when typed and what makes two names the same tag. */
object TagName {
    const val MAX_LENGTH = 24

    private val whitespace = Regex("\\s+")

    /** The name as it is stored, or null when nothing is left. `|` is dropped: it separates tags in the CSV. */
    fun clean(raw: String): String? =
        raw.replace("|", "")
            .replace(whitespace, " ")
            .trim()
            .takeWholeChars(MAX_LENGTH)
            .trimEnd()
            .ifEmpty { null }

    /**
     * What makes two names the same tag: case and accents do not count. A name made only of symbols folds to nothing,
     * so it keeps its own lower-cased text (otherwise every emoji tag would collide with the next).
     */
    fun key(name: String): String = HistoryCsvColumns.fold(name).ifEmpty { name.lowercase(Locale.ROOT) }
}
