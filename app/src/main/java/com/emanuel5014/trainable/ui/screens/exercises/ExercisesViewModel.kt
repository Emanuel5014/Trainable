package com.emanuel5014.trainable.ui.screens.exercises

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.CustomCategoryEntity
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Behind the exercises page: every exercise (hidden ones too), the two media display switches, the media of each exercise,
 * the "editable presets" switch, the custom categories and the creation of an exercise.
 */
@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager
) : ViewModel() {

    /** Every exercise, also the hidden ones; null until the first list arrives. */
    val exercises: StateFlow<List<ExerciseEntity>?> = exerciseRepository.getAllExercisesIncludingDeleted()
        .map<List<ExerciseEntity>, List<ExerciseEntity>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Every category the exercise form can offer, built-in and custom. */
    val categories: StateFlow<List<String>> = exerciseRepository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategories: StateFlow<List<CustomCategoryEntity>> = exerciseRepository.getCustomCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaEnabled: StateFlow<Boolean> = userPreferencesRepository.exerciseMediaEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val largePreview: StateFlow<Boolean> = userPreferencesRepository.exerciseMediaLarge
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val editablePresetExercises: StateFlow<Boolean> = userPreferencesRepository.editablePresetExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    fun setEditablePresetExercises(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setEditablePresetExercises(enabled) }
    }

    fun resetPresetExerciseNames(onComplete: () -> Unit) {
        viewModelScope.launch {
            exerciseRepository.resetPresetExerciseNames()
            onComplete()
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch { exerciseRepository.addCustomCategory(name) }
    }

    fun updateCategory(category: CustomCategoryEntity) {
        viewModelScope.launch { exerciseRepository.updateCustomCategory(category) }
    }

    fun deleteCategory(category: CustomCategoryEntity) {
        viewModelScope.launch { exerciseRepository.deleteCustomCategory(category.id) }
    }

    /**
     * Creates an exercise (or brings back a hidden one with the same name, with its history) and returns its id.
     * [description] is stored as it is; blank means none.
     */
    suspend fun createExercise(name: String, category: String, description: String?): Int =
        exerciseRepository.addCustomExercise(name.trim(), category, description?.trim()?.ifEmpty { null })
}
