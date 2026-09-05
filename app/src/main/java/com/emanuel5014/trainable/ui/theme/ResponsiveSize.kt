package com.emanuel5014.trainable.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Central responsive system.
 *
 * Old version used a plain `var screenWidthDp` on a singleton: reads did NOT
 * trigger recomposition, so changing Display size / DPI / resolution / font
 * scale / split-screen / foldable posture left stale paddings + font sizes
 * on screen (= "glitch").
 *
 * This version backs every dimension with Snapshot state, tracks width +
 * height + fontScale + density, and exposes tablet/landscape helpers so the
 * whole app recomposes atomically on any configuration change.
 */
@Stable
object ResponsiveSize {
    var screenWidthDp: Int by mutableIntStateOf(360)
        private set
    var screenHeightDp: Int by mutableIntStateOf(700)
        private set
    var fontScale: Float by mutableFloatStateOf(1f)
        private set
    var densityDpi: Int by mutableIntStateOf(420)
        private set

    // ---- Window classes (M3-adjacent breakpoints) ----
    val isCompactWidth: Boolean get() = screenWidthDp < 600
    val isMediumWidth: Boolean get() = screenWidthDp in 600..839
    val isExpandedWidth: Boolean get() = screenWidthDp >= 840

    /** Legacy: phone narrower than baseline. Kept for compat. */
    val isCompact: Boolean get() = screenWidthDp < 360
    val isNarrow: Boolean get() = screenWidthDp < 400

    val isTablet: Boolean get() = screenWidthDp >= 600
    val isLargeTablet: Boolean get() = screenWidthDp >= 840
    val isLandscape: Boolean get() = screenWidthDp > screenHeightDp
    val isShortHeight: Boolean get() = screenHeightDp < 500
    val isLargeFontScale: Boolean get() = fontScale > 1.3f
    val isHugeFontScale: Boolean get() = fontScale > 1.6f

    /** Max content width centered on tablets / large screens: avoids stretched rows. */
    val maxContentWidth: Dp get() = when {
        screenWidthDp >= 840 -> 800.dp
        screenWidthDp >= 600 -> 600.dp
        else -> Dp.Infinity
    }

    val horizontalPadding: Dp get() = when {
        screenWidthDp < 340 -> 12.dp
        screenWidthDp < 360 -> 16.dp
        screenWidthDp < 400 -> 20.dp
        screenWidthDp >= 840 -> 32.dp
        else -> 24.dp
    }

    val cardPadding: Dp get() = when {
        screenWidthDp < 340 -> 12.dp
        screenWidthDp < 360 -> 16.dp
        screenWidthDp >= 840 -> 28.dp
        else -> 24.dp
    }

    /** Grid columns for adaptive grids (images, cards). */
    val gridColumns: Int get() = when {
        screenWidthDp >= 840 -> 4
        screenWidthDp >= 600 -> 3
        screenWidthDp < 340 -> 2
        else -> 2
    }

    /** Dialog / sheet max width so they never bleed off small screens. */
    val dialogMaxWidth: Dp get() = when {
        screenWidthDp >= 840 -> 560.dp
        else -> screenWidthDp.dp.minus(horizontalPadding * 2).coerceAtLeast(280.dp)
    }

    val screenHeaderLineHeightSp: TextUnit get() = when {
        screenWidthDp < 360 -> 28.sp
        screenWidthDp < 400 -> 34.sp
        else -> 40.sp
    }

    val displaySmallSize: TextUnit get() = when {
        screenWidthDp < 360 -> 24.sp
        screenWidthDp < 400 -> 30.sp
        else -> 36.sp
    }

    val headlineLargeSize: TextUnit get() = when {
        screenWidthDp < 360 -> 18.sp
        screenWidthDp < 400 -> 22.sp
        else -> 28.sp
    }

    val headlineMediumSize: TextUnit get() = when {
        screenWidthDp < 360 -> 20.sp
        screenWidthDp < 400 -> 24.sp
        else -> 28.sp
    }

    val titleLargeSize: TextUnit get() = when {
        screenWidthDp < 360 -> 16.sp
        screenWidthDp < 400 -> 18.sp
        else -> 22.sp
    }

    val titleMediumSize: TextUnit get() = when {
        screenWidthDp < 360 -> 14.sp
        screenWidthDp < 400 -> 16.sp
        else -> 16.sp
    }

    val labelLargeSize: TextUnit get() = when {
        screenWidthDp < 360 -> 12.sp
        screenWidthDp < 400 -> 13.sp
        else -> 14.sp
    }

    fun update(widthDp: Int, heightDp: Int, fontScale: Float, densityDpi: Int) {
        var changed = false
        if (screenWidthDp != widthDp) { screenWidthDp = widthDp; changed = true }
        if (screenHeightDp != heightDp) { screenHeightDp = heightDp; changed = true }
        if (this.fontScale != fontScale) { this.fontScale = fontScale; changed = true }
        if (this.densityDpi != densityDpi) { this.densityDpi = densityDpi; changed = true }
        @Suppress("UNUSED_VARIABLE")
        val ignored = changed
    }

    /** Legacy overload kept for binary-compat with old call sites. */
    fun update(widthDp: Int) {
        if (screenWidthDp != widthDp) screenWidthDp = widthDp
    }

    /**
     * Maps the app's canonical title sizes to the responsive scale.
     * Falls back to proportional scaling (instead of returning the original)
     * so unknown sizes still shrink on very small screens instead of overflowing.
     */
    fun responsiveFontSize(original: TextUnit): TextUnit {
        if (original == TextUnit.Unspecified) return original
        val mapped = when {
            original == 36.sp -> displaySmallSize
            original == 32.sp -> headlineLargeSize
            original == 28.sp -> headlineMediumSize
            original == 22.sp -> titleLargeSize
            original == 16.sp -> titleMediumSize
            original == 14.sp -> labelLargeSize
            else -> null
        }
        if (mapped != null) return mapped
        // Proportional fallback: scale relative to 400dp baseline, clamped.
        val factor = when {
            screenWidthDp < 340 -> 0.85f
            screenWidthDp < 360 -> 0.9f
            screenWidthDp < 400 -> 0.95f
            screenWidthDp >= 840 -> 1.05f
            else -> 1f
        }
        return (original.value * factor).sp
    }

    /** Content padding shared by screens: horizontal adapts, bottom reserves nav-bar space. */
    fun contentPadding(bottom: Dp = 100.dp): PaddingValues =
        PaddingValues(start = horizontalPadding, end = horizontalPadding, bottom = bottom)
}

/**
 * Must be called once at the top of the theme (already wired in GymTrackingTheme).
 * Observes width + height + fontScale + density so ANY display change recomposes.
 */
@Composable
fun rememberResponsiveSize() {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    ResponsiveSize.update(
        widthDp = config.screenWidthDp,
        heightDp = config.screenHeightDp,
        fontScale = density.fontScale,
        densityDpi = config.densityDpi
    )
}

/**
 * Constrains wide layouts to [ResponsiveSize.maxContentWidth] and centers them.
 * On phones it's a no-op (fillMaxWidth). Use for top-level screen columns.
 */
@Composable
fun responsiveCenteredModifier(): Modifier {
    val max = ResponsiveSize.maxContentWidth
    return if (max == Dp.Infinity) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().widthIn(max = max)
    }
}
