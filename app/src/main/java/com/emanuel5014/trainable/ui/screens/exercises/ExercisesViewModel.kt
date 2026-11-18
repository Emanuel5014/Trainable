package com.emanuel5014.trainable.ui.screens.exercises

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.CustomCategoryEntity
import com.emanuel5014.trainable.data.repository.*
import com.emanuel5014.trainable.domain.exercise.ExerciseTraits
import com.emanuel5014.trainable.util.AppLocaleManager
import com.emanuel5014.trainable.util.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    exerciseRepository: ExerciseRepository,
    mergeRepository: ExerciseMergeRepository,
    userPreferencesRepository: UserPreferencesRepository,
    localeManager: AppLocaleManager
) : ViewModel() {
    val controller = ExercisesState(exerciseRepository, mergeRepository, userPreferencesRepository,
        userPreferencesRepository.userLanguage.map { localeManager.getResolvedLanguage() }, viewModelScope)
    val exercises get() = controller.exercises
    val namesInUse get() = controller.namesInUse
    val categories get() = controller.categories
    val customCategories get() = controller.customCategories
    val mediaEnabled get() = controller.mediaEnabled
    val largePreview get() = controller.largePreview
    val editablePresetExercises get() = controller.editablePresetExercises
    val languageCode get() = controller.languageCode
    val createdExerciseId get() = controller.createdExerciseId
    val messages = controller.messages.mapNotNull { it.toMessage() }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))
    fun setMediaEnabled(enabled: Boolean) = controller.setMediaEnabled(enabled)
    fun setLargePreview(enabled: Boolean) = controller.setLargePreview(enabled)
    fun attach(exerciseId: Int, uri: Uri) = controller.attach(exerciseId, uri.toString())
    fun remove(exerciseId: Int) = controller.remove(exerciseId)
    fun removeAll() = controller.removeAll()
    fun setEditablePresetExercises(enabled: Boolean) = controller.setEditablePresetExercises(enabled)
    fun resetPresetExerciseNames(onComplete: () -> Unit) = controller.resetPresetExerciseNames(onComplete)
    fun addCategory(name: String) = controller.addCategory(name)
    fun updateCategory(category: CustomCategoryEntity) = controller.updateCategory(category)
    fun deleteCategory(category: CustomCategoryEntity) = controller.deleteCategory(category)
    fun createExercise(name: String, category: String, description: String?, traits: ExerciseTraits = ExerciseTraits.EMPTY) =
        controller.createExercise(name, category, description, traits)
}
