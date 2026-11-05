package com.emanuel5014.trainable.ui.screens.workout

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.glass.LocalPanelGlass
import com.emanuel5014.trainable.ui.theme.AndroidPanelGlass
import com.emanuel5014.trainable.ui.theme.LocalPanelGlassSource
import com.emanuel5014.trainable.ui.theme.actionGlassSource
import com.emanuel5014.trainable.ui.theme.rememberPanelGlassSource
import com.kyant.backdrop.backdrops.LayerBackdrop

/** The previous Android screen's window flags, glass recording, toast duration and time formatter. */
object AndroidWorkoutScreenPlatform : WorkoutScreenPlatform {
    @Composable
    override fun KeepScreenOn(enabled: Boolean) {
        val context = LocalContext.current
        val activityWindow = (context as? Activity)?.window
        DisposableEffect(enabled) {
            if (enabled) activityWindow?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else activityWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { activityWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
    }

    @Composable
    override fun rememberPanel(): WorkoutPanel? {
        val source = rememberPanelGlassSource()
        return remember(source) { source?.let { AndroidWorkoutPanel(it) } }
    }

    @Composable
    override fun PanelProvider(panel: WorkoutPanel?, content: @Composable () -> Unit) {
        val source = (panel as? AndroidWorkoutPanel)?.source
        CompositionLocalProvider(
            LocalPanelGlassSource provides source,
            LocalPanelGlass provides remember(source) { source?.let { AndroidPanelGlass(it) } }
        ) { content() }
    }

    @Composable
    override fun rememberProgramCompletedNotifier(): () -> Unit {
        val context = LocalContext.current
        return remember(context) {
            { Toast.makeText(context, context.getString(R.string.program_completed), Toast.LENGTH_LONG).show() }
        }
    }

    override fun formatTime(minutes: Int, seconds: Int, paddedMinutes: Boolean): String =
        String.format(if (paddedMinutes) "%02d:%02d" else "%d:%02d", minutes, seconds)
}

private class AndroidWorkoutPanel(val source: LayerBackdrop) : WorkoutPanel {
    override fun record(modifier: Modifier): Modifier = modifier.actionGlassSource(source)
}
