package com.emanuel5014.trainable.ui.theme

import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.emanuel5014.trainable.data.model.GlassPart

/**
 * Blurs the screen behind every sheet and dialog while the user has the overlays part on, as iOS does
 * behind its sheets. Sheets and dialogs are windows of their own, so this asks Android to blur across
 * windows (Android 12+, and only where the device allows it); elsewhere nothing changes.
 *
 * One listener for the whole app: when the main window loses focus, a sheet or dialog has just opened,
 * so the new application windows get the blur. Put it once at the root of the app.
 */
@Composable
fun OverlayGlassBlur() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val glass = LocalGlassSettings.current
    val radiusPx = with(LocalDensity.current) { lerp(16f, 40f, glass.intensity).dp.roundToPx() }
    val view = LocalView.current
    val on = glass.isOn(GlassPart.Overlays)
    DisposableEffect(view, on, radiusPx) {
        if (!on) return@DisposableEffect onDispose { }
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (!hasFocus) view.post { blurOpenOverlays(view, radiusPx) }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun blurOpenOverlays(appView: View, radiusPx: Int) {
    val windowManager = appView.context.getSystemService(WindowManager::class.java) ?: return
    if (!windowManager.isCrossWindowBlurEnabled) return
    val appRoot = appView.rootView
    for (root in WindowInspector.getGlobalWindowViews()) {
        if (root === appRoot || !root.isAttachedToWindow) continue
        val params = root.layoutParams as? WindowManager.LayoutParams ?: continue
        // Dialogs and sheets are plain application windows; menus, tooltips and toasts are not
        if (params.type != WindowManager.LayoutParams.TYPE_APPLICATION) continue
        if (params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND != 0 && params.blurBehindRadius == radiusPx) continue
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        params.blurBehindRadius = radiusPx
        runCatching { windowManager.updateViewLayout(root, params) }
    }
}
