package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.repository.ExerciseMediaImport
import kotlinx.coroutines.flow.Flow

/**
 * What the shared exercise lists open or show through the platform: the user's media of an exercise (decoded and
 * played by each platform) and the exercise detail. The Android and iOS roots supply their own native decoder and picker.
 */
interface ExercisePlatformUi {
    fun detailDialogProperties(): androidx.compose.ui.window.DialogProperties =
        androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)

    @Composable
    fun rememberMediaPicker(onPicked: (Int, String) -> Unit): (Int) -> Unit = remember { {} }

    @Composable
    fun ImportMessages(messages: Flow<ExerciseMediaImport>) {}

    @Composable
    fun MediaSlot(fileName: String?, onOpen: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier,
                  size: Dp, playing: Boolean) { SharedExerciseMediaSlot(fileName, onOpen, onAdd, modifier, size, playing) }

    @Composable
    fun MediaCard(fileName: String, onOpen: () -> Unit, modifier: Modifier = Modifier) { SharedExerciseMediaCard(fileName, onOpen, modifier) }

    @Composable
    fun MediaViewer(fileName: String, exerciseName: String, onDismiss: () -> Unit,
                    onReplace: (() -> Unit)? = null, onRemove: (() -> Unit)? = null) { SharedExerciseMediaViewer(fileName, exerciseName, onDismiss, onReplace, onRemove) }

    @Composable
    fun MediaImage(fileName: String, modifier: Modifier = Modifier, crop: Boolean = true, playing: Boolean = true,
                   maxPx: Int = 512, onLoaded: (Boolean, Float) -> Unit = { _, _ -> }) {}

    /** The tile with the exercise's media next to a name; [onClick] null makes it only a picture. */
    @Composable
    fun ListThumbnail(fileName: String?, onClick: (() -> Unit)?, size: Dp?) { SharedExerciseListThumbnail(fileName, onClick, size ?: androidx.compose.ui.unit.Dp(56f)) }

    /** The exercise's media full screen, while [exercise] is not null. */
    @Composable
    fun MediaPeek(exercise: ExerciseEntity?, languageCode: String, onDismiss: () -> Unit) { SharedExerciseMediaPeek(exercise, languageCode, onDismiss) }

    /** Every exercise name in use, to warn about a duplicate in the exercise form; empty where not known yet. */
    @Composable
    fun namesInUse(): List<ExerciseEntity> = emptyList()

    /** Whether [ExerciseDetail] shows anything, so a list offers the way to it. */
    val hasExerciseDetail: Boolean get() = false

    /** The detail of an exercise as a dialog: history, records, media. */
    @Composable
    fun ExerciseDetail(exerciseId: Int, onDismiss: () -> Unit) {}
}

val LocalExercisePlatformUi = staticCompositionLocalOf<ExercisePlatformUi> { object : ExercisePlatformUi {} }

/**
 * Whether "Show in Workouts" is switched on on the exercises page of the settings. Provided once at the app root, like
 * [LocalAdvancedProgramming], so lists outside the workout screen (the exercise picker) follow the same switch.
 */
val LocalExerciseMediaEnabled = staticCompositionLocalOf { false }

/**
 * Whether a list where an exercise is picked draws the thumbnails of the exercises' media: it follows the "Show in
 * Workouts" switch and waits until at least one of the offered exercises has media, so the list is unchanged for
 * everyone else. From then on every row gets a tile ([ExerciseListThumbnail]), which keeps the names aligned.
 */
@Composable
fun rememberShowExerciseThumbnails(exercises: List<ExerciseEntity>, excludedIds: Set<Int> = emptySet()): Boolean {
    val enabled = LocalExerciseMediaEnabled.current
    return enabled && remember(exercises, excludedIds) { exercises.any { it.mediaPath != null && it.id !in excludedIds } }
}
