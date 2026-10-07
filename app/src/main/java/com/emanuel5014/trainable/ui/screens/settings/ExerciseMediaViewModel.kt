package com.emanuel5014.trainable.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import com.emanuel5014.trainable.util.ExerciseMediaMessage
import com.emanuel5014.trainable.util.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Behind the "Exercise Media" settings screen: the two display switches and the image/GIF of every exercise. */
@HiltViewModel
class ExerciseMediaViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager
) : ViewModel() {

    val exercises: StateFlow<List<ExerciseEntity>> = exerciseRepository.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaEnabled: StateFlow<Boolean> = userPreferencesRepository.exerciseMediaEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val largePreview: StateFlow<Boolean> = userPreferencesRepository.exerciseMediaLarge
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _messages = MutableSharedFlow<ExerciseMediaMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<ExerciseMediaMessage> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userLanguage.collect {
                _languageCode.value = localeManager.getResolvedLanguage()
            }
        }
    }

    fun setMediaEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setExerciseMediaEnabled(enabled) }
    }

    fun setLargePreview(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setExerciseMediaLarge(enabled) }
    }

    fun attach(exerciseId: Int, uri: Uri) {
        viewModelScope.launch {
            exerciseRepository.attachExerciseMedia(exerciseId, uri).toMessage()?.let { _messages.emit(it) }
        }
    }

    fun remove(exerciseId: Int) {
        viewModelScope.launch { exerciseRepository.removeExerciseMedia(exerciseId) }
    }

    fun removeAll() {
        viewModelScope.launch { exerciseRepository.removeAllExerciseMedia() }
    }
}
