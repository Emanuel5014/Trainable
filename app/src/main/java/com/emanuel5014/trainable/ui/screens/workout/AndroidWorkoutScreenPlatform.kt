package com.emanuel5014.trainable.ui.screens.workout

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.KyantPanelProvider
import com.emanuel5014.trainable.ui.theme.KyantWorkoutPanel
import com.emanuel5014.trainable.ui.theme.rememberPanelGlassSource

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
        return remember(source) { source?.let { KyantWorkoutPanel(it) } }
    }

    @Composable
    override fun PanelProvider(panel: WorkoutPanel?, content: @Composable () -> Unit) = KyantPanelProvider(panel, content)

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

