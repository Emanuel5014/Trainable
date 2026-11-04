package com.emanuel5014.trainable.ui.components.journal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.emanuel5014.trainable.data.local.relation.TagWithUsage

/** What the journal sheets need from the data layer: the tags to pick from and a way to make a new one. */
interface JournalData {
    /** Every tag, the most recently used first. */
    val tags: List<TagWithUsage>
    val ratingEnabled: Boolean

    /** Finds or creates the tag and hands back its id; a blank name does nothing. */
    fun createTag(name: String, onCreated: (Int) -> Unit)
}

/** The journal data of the sheet being composed, from each platform (Android: a ViewModel of the screen showing it). */
val LocalJournalData = staticCompositionLocalOf<@Composable () -> JournalData> { { NoJournalData } }

private object NoJournalData : JournalData {
    override val tags: List<TagWithUsage> get() = emptyList()
    override val ratingEnabled: Boolean get() = true
    override fun createTag(name: String, onCreated: (Int) -> Unit) {}
}
