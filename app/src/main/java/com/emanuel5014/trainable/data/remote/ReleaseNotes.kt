package com.emanuel5014.trainable.data.remote

import kotlinx.serialization.Serializable

/**
 * The notes of a release, as the `changelog.json` attached to it (written once, see tools/release_notes.py, which
 * also turns it into the GitHub release text). Every text is a map from language code ("en", "it", "de", "es",
 * "fr", "pt") to the text; English is the fallback. Releases without the file are read from their markdown table
 * (see [com.emanuel5014.trainable.util.ReleaseNotesParser]).
 */
@Serializable
data class ReleaseNotes(
    val version: String = "",
    /** One sentence on what the release is about, under the title. */
    val headline: Map<String, String> = emptyMap(),
    val items: List<ReleaseNoteItem> = emptyList(),
    /** Only for the GitHub page (such as installing a beta by hand): never shown in the app. */
    val notice: Map<String, String> = emptyMap()
)

@Serializable
data class ReleaseNoteItem(
    /** [TYPE_NEW], [TYPE_IMPROVED] or [TYPE_FIXED]: the section it goes in. */
    val type: String = TYPE_NEW,
    /** The part of the app, which picks the icon (see ReleaseNotesUi.areaIcon); unknown areas get a generic one. */
    val area: String = "other",
    val title: Map<String, String> = emptyMap(),
    /** One line, always shown. */
    val summary: Map<String, String> = emptyMap(),
    /** The full text, shown when the row is opened; optional. */
    val details: Map<String, String> = emptyMap(),
    /** GitHub issue numbers. */
    val issues: List<Int> = emptyList()
) {
    companion object {
        const val TYPE_NEW = "new"
        const val TYPE_IMPROVED = "improved"
        const val TYPE_FIXED = "fixed"
    }
}

/** The text in [language], else English, else any; empty when there is none. */
fun Map<String, String>.localized(language: String): String =
    this[language]?.takeIf { it.isNotBlank() } ?: this["en"]?.takeIf { it.isNotBlank() } ?: values.firstOrNull { it.isNotBlank() }.orEmpty()
