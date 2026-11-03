package com.emanuel5014.trainable.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.domain.exercise.ExerciseTypeMemory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The type each exercise was last added with, so the add / swap exercise sheet opens on it instead of weight × reps
 * (see [ExerciseTypeMemory]).
 */
@HiltViewModel
class RememberedExerciseTypesViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository
) : ViewModel() {
    /** Empty until the stored value is read, which happens long before the lifter has picked an exercise. */
    val types: StateFlow<Map<Int, String>> = preferences.exerciseTypes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun remember(exerciseId: Int, type: String) {
        viewModelScope.launch { preferences.rememberExerciseType(exerciseId, type) }
    }
}
