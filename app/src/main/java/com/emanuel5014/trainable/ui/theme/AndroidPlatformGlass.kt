package com.emanuel5014.trainable.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.emanuel5014.trainable.ui.components.GlassSegmentedTabs
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.ui.glass.GlassSource
import com.emanuel5014.trainable.ui.glass.PlatformGlass
import com.kyant.backdrop.backdrops.LayerBackdrop

/** The Liquid Glass of LiquidGlass.kt for the screens of the shared module, provided at the root of the app. */
object AndroidPlatformGlass : PlatformGlass {
    @Composable
    override fun header(modifier: Modifier): Modifier = modifier.headerGlass()

    @Composable
    override fun rememberActionSource(): GlassSource? {
        val backdrop = rememberActionGlassSource() ?: return null
        return remember(backdrop) { BackdropGlassSource(backdrop) }
    }

    @Composable
    override fun rememberNavBarActionSource(): GlassSource? {
        val backdrop = if (LocalGlassSettings.current.isOn(GlassPart.ActionButtons)) LocalAppGlassBackdrop.current else null
        return remember(backdrop) { backdrop?.let { BackdropGlassSource(it) } }
    }

    override val hasSegmentedTabs: Boolean get() = true

    @Composable
    override fun SegmentedTabs(
        titles: List<String>,
        selectedIndex: Int,
        progress: () -> Float,
        onSelect: (Int) -> Unit,
        modifier: Modifier,
        labelSize: TextUnit,
        onScrub: ((position: Float) -> Unit)?,
        onScrubEnd: ((index: Int) -> Unit)?,
        onTick: () -> Unit
    ) = GlassSegmentedTabs(titles, selectedIndex, progress, onSelect, modifier, labelSize, onScrub, onScrubEnd, onTick)

    @Composable
    override fun NoOverlayBlur() = com.emanuel5014.trainable.ui.theme.NoOverlayBlur()
}

private class BackdropGlassSource(private val backdrop: LayerBackdrop) : GlassSource {
    override fun record(modifier: Modifier): Modifier = modifier.actionGlassSource(backdrop)

    @Composable
    override fun bend(modifier: Modifier, tint: Color, cornerRadius: Dp?): Modifier =
        modifier.actionGlass(backdrop, tint, cornerRadius)
}
