package com.emanuel5014.trainable.ui.screens.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Android's [CompareSessionsModel], with the language [AppLocaleManager] resolves for Compose. */
@HiltViewModel
class CompareSessionsViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    userPreferencesRepository: UserPreferencesRepository,
    localeManager: AppLocaleManager
) : ViewModel() {
    val model = CompareSessionsModel(
        workoutRepository,
        userPreferencesRepository,
        userPreferencesRepository.userLanguage.map { localeManager.resolveLanguageForCompose(it) },
        viewModelScope
    )
}
