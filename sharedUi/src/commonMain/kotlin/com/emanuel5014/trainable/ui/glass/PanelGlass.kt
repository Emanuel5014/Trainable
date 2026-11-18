package com.emanuel5014.trainable.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * The frosted glass of the workout's bottom panel, over a copy of the exercise content the screen records. Provided
 * by the workout screen while the user has this part of the glass on; null otherwise (and on iOS).
 */
interface PanelGlass {
    /** The panel's outline as glass (Android: a continuous-corner rectangle, inset from the screen). */
    val shape: Shape

    /** The frosted panel body; the panel's own fill and shadow are clear meanwhile. */
    @Composable
    fun frost(modifier: Modifier, shape: Shape, tint: Color): Modifier
}

val LocalPanelGlass = staticCompositionLocalOf<PanelGlass?> { null }
