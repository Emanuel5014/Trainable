package com.emanuel5014.trainable.ui.components.journal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.data.local.relation.TagWithUsage

/** The journal sheets' data on Android: the JournalViewModel of the screen showing them. */
val AndroidJournalData: @Composable () -> JournalData = {
    val viewModel: JournalViewModel = hiltViewModel()
    val tags by viewModel.tags.collectAsState()
    val ratingEnabled by viewModel.ratingEnabled.collectAsState()
    object : JournalData {
        override val tags: List<TagWithUsage> get() = tags
        override val ratingEnabled: Boolean get() = ratingEnabled
        override fun createTag(name: String, onCreated: (Int) -> Unit) = viewModel.createTag(name, onCreated)
    }
}
