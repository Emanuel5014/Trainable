package com.emanuel5014.trainable.ui.theme

import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.emanuel5014.trainable.data.model.GlassPart

/**
 * Blurs the app behind every sheet and dialog while the user has the overlays part on, as iOS does behind
 * its sheets. Put it on the root of the app.
 *
 * Sheets and dialogs are windows of their own, but the blur is drawn in the app's window, under them: asking
 * Android to blur behind those windows (FLAG_BLUR_BEHIND) was not dependable, the second sheet of a session
 * often opened without it. The app window loses focus exactly while a sheet or dialog is in front of it, which
 * is the signal. Android 12+ (RenderEffect); elsewhere nothing changes.
 */
@Composable
fun Modifier.overlayGlassBlur(): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    val glass = LocalGlassSettings.current
    val on = glass.isOn(GlassPart.Overlays)
    val view = LocalView.current
    val radiusPx = with(LocalDensity.current) { lerp(16f, 40f, glass.intensity).dp.toPx() }
    var overlayOpen by remember { mutableStateOf(false) }
    // 0 with nothing in front, 1 with a sheet or dialog open; read in draw only, so the app does not recompose
    val amount = remember { Animatable(0f) }

    LaunchedEffect(amount) {
        // in along with the sheet, out almost at once: the sheet is already on its way down
        snapshotFlow { overlayOpen }.collect { open -> amount.animateTo(if (open) 1f else 0f, tween(if (open) 220 else 80)) }
    }
    DisposableEffect(view, on) {
        if (!on) {
            overlayOpen = false
            return@DisposableEffect onDispose { }
        }
        val check = Runnable { overlayOpen = !view.hasWindowFocus() && hasOverlayWindow(view) }
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                overlayOpen = false
            } else {
                check.run()
                // the new window is listed by the time it takes focus; these are for the slow ones
                view.postDelayed(check, 120)
                view.postDelayed(check, 400)
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
            view.removeCallbacks(check)
            overlayOpen = false
        }
    }

    return graphicsLayer {
        val radius = radiusPx * amount.value
        renderEffect = if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Clamp) else null
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun hasOverlayWindow(appView: View): Boolean {
    val appRoot = appView.rootView
    return WindowInspector.getGlobalWindowViews().any { root ->
        // Dialogs and sheets are plain application windows; menus, tooltips and toasts are not
        root !== appRoot && root.isAttachedToWindow &&
            (root.layoutParams as? WindowManager.LayoutParams)?.type == WindowManager.LayoutParams.TYPE_APPLICATION
    }
}
