package com.emanuel5014.trainable.ui.screens.exercises

import com.emanuel5014.trainable.data.local.entity.CustomCategoryEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.repository.ExerciseMergeRepository
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.domain.exercise.ExerciseTraits
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import com.emanuel5014.trainable.data.repository.ExerciseMediaImport
import kotlinx.coroutines.launch

/**
 * Behind the exercises page: every exercise (hidden ones too), the two media display switches, the media of each exercise,
 * the "editable presets" switch, the custom categories and the creation of an exercise.
 */
class ExercisesState(
    private val exerciseRepository: ExerciseRepository,
    private val mergeRepository: ExerciseMergeRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    languageCodeSource: Flow<String>,
    private val scope: CoroutineScope
) {

    /** Every exercise, also the hidden ones; null until the first list arrives. */
    val exercises: StateFlow<List<ExerciseEntity>?> = exerciseRepository.getAllExercisesIncludingDeleted()
        .map<List<ExerciseEntity>, List<ExerciseEntity>?> { it }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

    /** The exercises as the form checks a name against them: the library plus the names of merged-away exercises. */
    val namesInUse: StateFlow<List<ExerciseEntity>> = mergeRepository.observeNamesInUse()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Every category the exercise form can offer, built-in and custom. */
    val categories: StateFlow<List<String>> = exerciseRepository.getCategories()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategories: StateFlow<List<CustomCategoryEntity>> = exerciseRepository.getCustomCategories()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaEnabled: StateFlow<Boolean> = userPreferencesRepository.exerciseMediaEnabled
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    val largePreview: StateFlow<Boolean> = userPreferencesRepository.exerciseMediaLarge
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), true)

    val editablePresetExercises: StateFlow<Boolean> = userPreferencesRepository.editablePresetExercises
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _messages = MutableSharedFlow<ExerciseMediaImport>(extraBufferCapacity = 1)
    val messages: SharedFlow<ExerciseMediaImport> = _messages.asSharedFlow()

    private val _created = Channel<Int>(Channel.BUFFERED)

    /** The id of each exercise [createExercise] has made or brought back; held until the page is there to take it. */
    val createdExerciseId: Flow<Int> = _created.receiveAsFlow()

    init {
        scope.launch {
            languageCodeSource.collect {
                _languageCode.value = it
            }
        }
    }

    fun setMediaEnabled(enabled: Boolean) {
        scope.launch { userPreferencesRepository.setExerciseMediaEnabled(enabled) }
    }

    fun setLargePreview(enabled: Boolean) {
        scope.launch { userPreferencesRepository.setExerciseMediaLarge(enabled) }
    }

    fun attach(exerciseId: Int, uri: String) {
        scope.launch {
            exerciseRepository.attachExerciseMedia(exerciseId, uri).takeUnless { it is ExerciseMediaImport.Saved }?.let { _messages.emit(it) }
        }
    }

    fun remove(exerciseId: Int) {
        scope.launch { exerciseRepository.removeExerciseMedia(exerciseId) }
    }

    fun removeAll() {
        scope.launch { exerciseRepository.removeAllExerciseMedia() }
    }

    fun setEditablePresetExercises(enabled: Boolean) {
        scope.launch { userPreferencesRepository.setEditablePresetExercises(enabled) }
    }

    fun resetPresetExerciseNames(onComplete: () -> Unit) {
        scope.launch {
            exerciseRepository.resetPresetExerciseNames()
            onComplete()
        }
    }

    fun addCategory(name: String) {
        scope.launch { exerciseRepository.addCustomCategory(name) }
    }

    fun updateCategory(category: CustomCategoryEntity) {
        scope.launch { exerciseRepository.updateCustomCategory(category) }
    }

    fun deleteCategory(category: CustomCategoryEntity) {
        scope.launch { exerciseRepository.deleteCustomCategory(category.id) }
    }

    /**
     * Creates an exercise (or brings back a hidden one with the same name, with its history) and announces its id on
     * [createdExerciseId]. It runs in the view model, so a rotation between the tap and the end does not lose it.
     * [description] is stored as it is; blank means none. [traits] are the muscles and equipment chosen in the form.
     */
    fun createExercise(name: String, category: String, description: String?, traits: ExerciseTraits = ExerciseTraits.EMPTY) {
        scope.launch {
            _created.send(
                exerciseRepository.addCustomExercise(name.trim(), category, description?.trim()?.ifEmpty { null }, traits)
            )
        }
    }
}
