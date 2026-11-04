package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.ui.screens.exercise.ExerciseDetailDialog

/** The exercise media (ExerciseMedia.kt) and the exercise detail of the Android app, for the shared lists. */
object AndroidExercisePlatformUi : ExercisePlatformUi {
    @Composable
    override fun ListThumbnail(fileName: String?, onClick: (() -> Unit)?, size: Dp?) {
        if (size == null) ExerciseListThumbnail(fileName, onClick) else ExerciseListThumbnail(fileName, onClick, size)
    }

    @Composable
    override fun MediaPeek(exercise: ExerciseEntity?, languageCode: String, onDismiss: () -> Unit) =
        ExerciseMediaPeek(exercise, languageCode, onDismiss)

    @Composable
    override fun namesInUse(): List<ExerciseEntity> = rememberNamesInUse()

    override val hasExerciseDetail: Boolean get() = true

    @Composable
    override fun ExerciseDetail(exerciseId: Int, onDismiss: () -> Unit) =
        ExerciseDetailDialog(exerciseId = exerciseId, onDismiss = onDismiss)
}
