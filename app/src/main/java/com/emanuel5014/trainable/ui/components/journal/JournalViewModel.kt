package com.emanuel5014.trainable.ui.components.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.relation.TagWithUsage
import com.emanuel5014.trainable.data.repository.JournalRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the journal sheets need from the data layer: the tags to pick from and a way to make a new one. */
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /** Every tag, the most recently used first. */
    val tags: StateFlow<List<TagWithUsage>> = journalRepository.tags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val ratingEnabled: StateFlow<Boolean> = userPreferencesRepository.journalRatingEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    /** Finds or creates the tag and hands back its id (on the main thread); a blank name does nothing. */
    fun createTag(name: String, onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            journalRepository.findOrCreateTag(name)?.let { onCreated(it.tag.id) }
        }
    }
}
