package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.repository.ExerciseMediaImport
import com.emanuel5014.trainable.util.toMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import com.emanuel5014.trainable.ui.screens.exercise.ExerciseDetailDialog

/** The exercise media (ExerciseMedia.kt) and the exercise detail of the Android app, for the shared lists. */
object AndroidExercisePlatformUi : ExercisePlatformUi {
    override fun detailDialogProperties() = androidx.compose.ui.window.DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false)

    @Composable
    override fun rememberMediaPicker(onPicked: (Int, String) -> Unit): (Int) -> Unit =
        rememberExerciseMediaPicker { id, uri -> onPicked(id, uri.toString()) }

    @Composable
    override fun ImportMessages(messages: Flow<ExerciseMediaImport>) {
        val notes = remember(messages) { messages.mapNotNull { it.toMessage() } }
        ExerciseMediaMessageToasts(notes)
    }

    @Composable
    override fun MediaSlot(fileName: String?, onOpen: () -> Unit, onAdd: () -> Unit, modifier: Modifier, size: Dp, playing: Boolean) =
        ExerciseMediaSlot(fileName, onOpen, onAdd, modifier, size, playing)

    @Composable
    override fun MediaCard(fileName: String, onOpen: () -> Unit, modifier: Modifier) =
        ExerciseMediaCard(fileName, onOpen, modifier)

    @Composable
    override fun MediaViewer(fileName: String, exerciseName: String, onDismiss: () -> Unit, onReplace: (() -> Unit)?, onRemove: (() -> Unit)?) =
        ExerciseMediaViewer(fileName, exerciseName, onDismiss, onReplace, onRemove)

    @Composable
    override fun MediaImage(fileName: String, modifier: Modifier, crop: Boolean, playing: Boolean, maxPx: Int,
                            onLoaded: (Boolean, Float) -> Unit) =
        ExerciseMediaImage(fileName, modifier, crop, playing, maxPx, onLoaded)

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
