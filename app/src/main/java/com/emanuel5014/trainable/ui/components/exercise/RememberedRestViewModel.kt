package com.emanuel5014.trainable.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The rest the add / swap exercise sheet was confirmed with last time, so its slider does not start from 2 minutes again. */
@HiltViewModel
class RememberedRestViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository
) : ViewModel() {
    /** Null until the stored value is read, which happens long before the lifter has picked an exercise. */
    val lastRestSeconds: StateFlow<Int?> = preferences.lastAddedExerciseRestSeconds
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun remember(seconds: Int) {
        viewModelScope.launch { preferences.setLastAddedExerciseRestSeconds(seconds) }
    }
}
