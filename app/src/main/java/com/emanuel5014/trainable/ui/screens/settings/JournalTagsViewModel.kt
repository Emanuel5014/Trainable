package com.emanuel5014.trainable.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class JournalTagsViewModel @Inject constructor(journalRepository: JournalRepository) : ViewModel() {
    val state = JournalTagsState(journalRepository, viewModelScope)
}
