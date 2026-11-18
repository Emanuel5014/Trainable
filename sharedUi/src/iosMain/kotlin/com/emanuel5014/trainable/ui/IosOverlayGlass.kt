package com.emanuel5014.trainable.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.ui.theme.KyantPlatformGlass
import com.emanuel5014.trainable.ui.theme.LocalGlassSettings
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.UIView

/**
 * The shared Kyant Liquid Glass plus the blur behind sheets and dialogs on iOS (the Overlays part), the same idea
 * as Android's OverlayGlass.kt.
 */
internal object IosPlatformGlass : KyantPlatformGlass() {
    @Composable
    override fun NoOverlayBlur() {
        DisposableEffect(Unit) {
            IosOverlaySignal.suppressed++
            onDispose {
                // Given back a moment after the sheet is gone, as on Android, so the blur does not flash as it closes
                IosOverlaySignal.scope.launch { delay(300); IosOverlaySignal.suppressed-- }
            }
        }
    }

    @Composable
    override fun NotAnOverlay() {
        DisposableEffect(Unit) {
            IosOverlaySignal.popups++
            onDispose { IosOverlaySignal.popups-- }
        }
    }
}

private object IosOverlaySignal {
    /** Sheets or dialogs on screen that asked to open without the blur behind them. */
    var suppressed by mutableIntStateOf(0)
    /** Menus and tooltips on screen: Compose layers like sheets and dialogs, but without the blur. */
    var popups by mutableIntStateOf(0)
    val scope = MainScope()
}

/**
 * Blurs the app behind every sheet and dialog while the user has the overlays part on. Put it on the root of the app.
 *
 * Compose on iOS draws each dialog, sheet and popup in a layer: a UIView of its own over the app's view, removed when
 * it closes. The app's own content stays in the root view, so blurring the root leaves the layers sharp. There is no
 * callback for a layer coming or going, so the window is looked at a few times a second while the part is on; menus
 * and tooltips mark themselves with NotAnOverlay. The blur comes on only when two looks in a row agree, so a menu is
 * never caught in the moment between its layer appearing and its content marking it.
 */
@Composable
internal fun Modifier.iosOverlayGlassBlur(): Modifier {
    val glass = LocalGlassSettings.current
    val on = glass.isOn(GlassPart.Overlays)
    val controller = LocalUIViewController.current
    val radiusPx = with(LocalDensity.current) { lerp(16f, 40f, glass.intensity).dp.toPx() * glass.blurScale }
    var overlayOpen by remember { mutableStateOf(false) }
    // 0 with nothing in front, 1 with a sheet or dialog open; read in draw only, so the app does not recompose
    val amount = remember { Animatable(0f) }

    LaunchedEffect(amount) {
        // In along with the sheet, out almost at once
        snapshotFlow { overlayOpen && IosOverlaySignal.suppressed == 0 }
            .collect { open -> amount.animateTo(if (open) 1f else 0f, tween(if (open) 200 else 60)) }
    }
    LaunchedEffect(controller, on) {
        if (!on) {
            overlayOpen = false
            return@LaunchedEffect
        }
        var previous = false
        try {
            while (true) {
                val now = composeLayerCount(controller.view) > IosOverlaySignal.popups
                // On at the second look in a row, off at the first
                overlayOpen = now && previous
                previous = now
                delay(POLL_MS)
            }
        } finally {
            overlayOpen = false
        }
    }

    return graphicsLayer {
        val radius = radiusPx * amount.value
        renderEffect = if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Clamp) else null
    }
}

private const val POLL_MS = 60L
private const val LAYER_VIEW = "UIKitComposeSceneLayerView"

/** Compose's dialog, sheet and popup layers now in the app's window. */
private fun composeLayerCount(appView: UIView): Int {
    val window = appView.window ?: return 0
    return countLayers(window, depth = 0)
}

private fun countLayers(view: UIView, depth: Int): Int {
    // Kotlin/Native gives its UIView subclasses Objective-C names with a number after them
    if (view::class.simpleName?.startsWith(LAYER_VIEW) == true) return 1
    if (depth > 8) return 0
    var count = 0
    for (child in view.subviews) count += countLayers(child as UIView, depth + 1)
    return count
}
