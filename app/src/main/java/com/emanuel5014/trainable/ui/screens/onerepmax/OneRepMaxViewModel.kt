package com.emanuel5014.trainable.ui.screens.onerepmax

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class OneRepMaxViewModel @Inject constructor(
    oneRepMaxRepository: OneRepMaxRepository,
    exerciseRepository: ExerciseRepository,
    userPreferencesRepository: UserPreferencesRepository,
    localeManager: AppLocaleManager
) : ViewModel() {
    val state = OneRepMaxState(oneRepMaxRepository, exerciseRepository, userPreferencesRepository,
        localeManager.userSelectedLanguage.map { localeManager.getResolvedLanguage() }, viewModelScope)
    val uiState = state.uiState
    val languageCode = state.languageCode
}
