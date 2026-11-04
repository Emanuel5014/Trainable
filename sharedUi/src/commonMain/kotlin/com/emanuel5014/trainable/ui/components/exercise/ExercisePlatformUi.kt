package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity

/**
 * What the shared exercise lists open or show through the platform: the user's media of an exercise (decoded and
 * played by Android) and the exercise detail. Without a provider, as on iOS for now, there is no media and no detail.
 */
interface ExercisePlatformUi {
    /** The tile with the exercise's media next to a name; [onClick] null makes it only a picture. */
    @Composable
    fun ListThumbnail(fileName: String?, onClick: (() -> Unit)?, size: Dp?) {}

    /** The exercise's media full screen, while [exercise] is not null. */
    @Composable
    fun MediaPeek(exercise: ExerciseEntity?, languageCode: String, onDismiss: () -> Unit) {}

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
