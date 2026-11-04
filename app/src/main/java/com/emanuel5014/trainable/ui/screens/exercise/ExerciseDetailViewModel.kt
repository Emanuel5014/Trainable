package com.emanuel5014.trainable.ui.screens.exercise

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.*
import com.emanuel5014.trainable.domain.exercise.ExerciseForm
import com.emanuel5014.trainable.util.AppLocaleManager
import com.emanuel5014.trainable.util.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository,
    mergeRepository: ExerciseMergeRepository,
    userPreferencesRepository: UserPreferencesRepository,
    localeManager: AppLocaleManager
) : ViewModel() {
    val controller = ExerciseDetailState(exerciseRepository, workoutRepository, mergeRepository, userPreferencesRepository,
        userPreferencesRepository.userLanguage.map { localeManager.getResolvedLanguage() }, viewModelScope)
    val state get() = controller.state
    val categories get() = controller.categories
    val namesInUse get() = controller.namesInUse
    val messages get() = controller.messages
    val mergeCandidates get() = controller.mergeCandidates
    val mediaMessages = controller.mediaMessages.mapNotNull { it.toMessage() }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))
    fun load(exerciseId: Int) = controller.load(exerciseId)
    suspend fun previewMerge(winnerId: Int) = controller.previewMerge(winnerId)
    fun merge(winnerId: Int, onMerged: (Int) -> Unit) = controller.merge(winnerId, onMerged)
    fun save(form: ExerciseForm) = controller.save(form)
    fun setFavorite(favorite: Boolean) = controller.setFavorite(favorite)
    fun attachMedia(exerciseId: Int, uri: Uri) = controller.attachMedia(exerciseId, uri.toString())
    fun removeMedia() = controller.removeMedia()
    fun undoMerge(mergeId: Int) = controller.undoMerge(mergeId)
    suspend fun willBeDeletedForGood() = controller.willBeDeletedForGood()
    fun remove() = controller.remove()
    fun restore() = controller.restore()
}
