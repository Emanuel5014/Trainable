package com.emanuel5014.trainable.ui.screens.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.JournalRepository
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Android's [EditWorkoutModel]: the workout from the route, and the app's language from [AppLocaleManager]. */
@HiltViewModel
class EditWorkoutViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    userPreferencesRepository: UserPreferencesRepository,
    localeManager: AppLocaleManager,
    oneRepMaxRepository: OneRepMaxRepository,
    journalRepository: JournalRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val model = EditWorkoutModel(
        sessionId = savedStateHandle.get<Int>("sessionId") ?: 0,
        workoutRepository = workoutRepository,
        exerciseRepository = exerciseRepository,
        userPreferencesRepository = userPreferencesRepository,
        oneRepMaxRepository = oneRepMaxRepository,
        journalRepository = journalRepository,
        languageCodes = userPreferencesRepository.userLanguage.map { localeManager.getResolvedLanguage() },
        scope = viewModelScope
    )
}
