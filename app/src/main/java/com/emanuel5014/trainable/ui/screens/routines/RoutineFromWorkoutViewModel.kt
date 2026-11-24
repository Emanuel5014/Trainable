package com.emanuel5014.trainable.ui.screens.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The shared [RoutineFromWorkoutState] over the tabs, plus the app's language, which only Android resolves. */
@HiltViewModel
class RoutineFromWorkoutViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager
) : ViewModel() {

    val state = RoutineFromWorkoutState(workoutRepository, viewModelScope)

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userLanguage.collect { userLang ->
                _languageCode.value = localeManager.resolveLanguageForCompose(userLang)
            }
        }
    }
}
