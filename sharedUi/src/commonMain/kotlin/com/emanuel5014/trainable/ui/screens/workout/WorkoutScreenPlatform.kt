package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** The exercise content recorded for the platform's existing workout-panel glass. */
interface WorkoutPanel {
    fun record(modifier: Modifier): Modifier
}

/** Platform services used by the shared workout screen; Android preserves its original implementations. */
interface WorkoutScreenPlatform {
    @Composable fun KeepScreenOn(enabled: Boolean) {}
    @Composable fun rememberPanel(): WorkoutPanel? = null
    @Composable fun PanelProvider(panel: WorkoutPanel?, content: @Composable () -> Unit) { content() }
    @Composable fun rememberProgramCompletedNotifier(): () -> Unit = {}
    fun formatTime(minutes: Int, seconds: Int, paddedMinutes: Boolean): String =
        (if (paddedMinutes) minutes.toString().padStart(2, '0') else minutes.toString()) + ":" + seconds.toString().padStart(2, '0')
}

private object DefaultWorkoutScreenPlatform : WorkoutScreenPlatform
val LocalWorkoutScreenPlatform = staticCompositionLocalOf<WorkoutScreenPlatform> { DefaultWorkoutScreenPlatform }

@Composable
fun workoutTime(minutes: Int, seconds: Int, paddedMinutes: Boolean = false): String =
    LocalWorkoutScreenPlatform.current.formatTime(minutes, seconds, paddedMinutes)
