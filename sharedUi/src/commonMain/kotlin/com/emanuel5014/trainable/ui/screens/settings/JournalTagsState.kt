package com.emanuel5014.trainable.ui.screens.settings

import com.emanuel5014.trainable.data.local.relation.TagWithUsage
import com.emanuel5014.trainable.data.repository.JournalRepository
import com.emanuel5014.trainable.data.repository.RenameResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.CoroutineScope

/** The journal's tag management on the device database. */
class JournalTagsState(private val journalRepository: JournalRepository, private val scope: CoroutineScope) {
    /** The most used tags first, then by name. */
    val tags: StateFlow<List<TagWithUsage>> = journalRepository.tags
        .map { list ->
            list.sortedWith(compareByDescending<TagWithUsage> { it.uses }.thenComparator { first, second -> first.name.compareTo(second.name, ignoreCase = true) })
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun create(name: String) {
        scope.launch { journalRepository.findOrCreateTag(name) }
    }

    /** [RenameResult.Conflict] means the name is another tag's: the screen offers to merge. */
    suspend fun rename(id: Int, name: String): RenameResult = journalRepository.renameTag(id, name)

    fun setColor(id: Int, colorIndex: Int) {
        scope.launch { journalRepository.setTagColor(id, colorIndex) }
    }

    fun setCustomColor(id: Int, argb: Int) {
        scope.launch { journalRepository.setTagCustomColor(id, argb) }
    }

    fun merge(fromId: Int, intoId: Int) {
        scope.launch { journalRepository.mergeTags(fromId, intoId) }
    }

    fun delete(id: Int) {
        scope.launch { journalRepository.deleteTag(id) }
    }
}
