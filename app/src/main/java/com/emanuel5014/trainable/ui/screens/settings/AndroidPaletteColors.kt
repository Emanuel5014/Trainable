package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.ui.graphics.Color
import com.emanuel5014.trainable.ui.theme.getPalettePreviewColors
import com.emanuel5014.trainable.ui.theme.getSeedPreviewColors

object AndroidPaletteColors : PaletteColors {
    override fun palette(index: Int, isDark: Boolean): List<Color> = getPalettePreviewColors(index, isDark)
    override fun seed(seed: Int, style: Int, isDark: Boolean): List<Color> = getSeedPreviewColors(seed, style, isDark)
}
