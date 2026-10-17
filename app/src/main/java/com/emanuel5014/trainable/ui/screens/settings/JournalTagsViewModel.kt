package com.emanuel5014.trainable.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.relation.TagWithUsage
import com.emanuel5014.trainable.data.repository.JournalRepository
import com.emanuel5014.trainable.data.repository.RenameResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Settings -> Workout -> Journal -> Manage tags: look after the tags the lifter has made. */
@HiltViewModel
class JournalTagsViewModel @Inject constructor(
    private val journalRepository: JournalRepository
) : ViewModel() {

    /** The most used tags first, then by name. */
    val tags: StateFlow<List<TagWithUsage>> = journalRepository.tags
        .map { list ->
            list.sortedWith(compareByDescending<TagWithUsage> { it.uses }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun create(name: String) {
        viewModelScope.launch { journalRepository.findOrCreateTag(name) }
    }

    /** [RenameResult.Conflict] means the name is another tag's: the screen offers to merge. */
    suspend fun rename(id: Int, name: String): RenameResult = journalRepository.renameTag(id, name)

    fun setColor(id: Int, colorIndex: Int) {
        viewModelScope.launch { journalRepository.setTagColor(id, colorIndex) }
    }

    fun merge(fromId: Int, intoId: Int) {
        viewModelScope.launch { journalRepository.mergeTags(fromId, intoId) }
    }

    fun delete(id: Int) {
        viewModelScope.launch { journalRepository.deleteTag(id) }
    }
}
