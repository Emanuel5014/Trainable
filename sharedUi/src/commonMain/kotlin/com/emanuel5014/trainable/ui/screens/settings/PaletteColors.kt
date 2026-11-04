package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Color previews from the engine used by the platform's actual app theme. */
interface PaletteColors {
    fun palette(index: Int, isDark: Boolean = true): List<Color>
    fun seed(seed: Int, style: Int = 0, isDark: Boolean = true): List<Color>
}

val LocalPaletteColors = staticCompositionLocalOf<PaletteColors> { error("Palette colors must be provided by the app") }
