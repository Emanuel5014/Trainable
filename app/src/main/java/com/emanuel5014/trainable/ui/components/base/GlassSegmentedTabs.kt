package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.surfaceGlass
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlin.math.abs
import kotlin.math.round

private val TabHeight = 44.dp
private val TabGap = 4.dp

/**
 * Tabs in Liquid Glass, like the segmented controls of iOS 26: a glass track, and a glass capsule on the
 * selected tab that shows the accent colour only inside itself. While it slides between tabs it becomes a
 * drop that magnifies the labels beneath, then settles again.
 *
 * [progress] is where the capsule is, in tabs (0 on the first, 1 on the second...), read in draw only;
 * [selectedIndex] is the tab the labels and accessibility treat as selected.
 */
@Composable
fun GlassSegmentedTabs(
    titles: List<String>,
    selectedIndex: Int,
    progress: () -> Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelSize: androidx.compose.ui.unit.TextUnit = MaterialTheme.typography.labelLarge.fontSize
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val trackBackdrop = rememberCanvasBackdrop { drawRect(track) }
    val accentLabels = rememberLayerBackdrop()
    val pillBackdrop = rememberCombinedBackdrop(trackBackdrop, accentLabels)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .surfaceGlass(GlassPart.Tabs)
            .clip(RoundedCornerShape(50))
            .padding(TabGap)
    ) {
        // What the user reads and taps: the labels in their neutral colour
        TabLabels(titles, selectedIndex, OnSurfaceVariant, labelSize, onSelect = onSelect)

        // The same labels in the accent colour, never drawn on screen: recorded for the capsule to show
        TabLabels(
            titles, selectedIndex, Primary, labelSize,
            modifier = Modifier
                .clearAndSetSemantics {}
                .alpha(0f)
                .layerBackdrop(accentLabels)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tabWidth: Dp = (maxWidth - TabGap * (titles.size - 1)) / titles.size
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val p = progress()
                        // 0 at rest on a tab, 1 half way: the capsule becomes a drop while it travels
                        val transit = (abs(p - round(p)) * 2f).coerceIn(0f, 1f)
                        translationX = p * (tabWidth + TabGap).toPx()
                        scaleX = 1f + 0.12f * transit
                        scaleY = 1f + 0.06f * transit
                    }
                    .drawBackdrop(
                        backdrop = pillBackdrop,
                        shape = { Capsule() },
                        effects = {
                            val p = progress()
                            val transit = (abs(p - round(p)) * 2f).coerceIn(0f, 1f)
                            lens(10.dp.toPx() * transit, 14.dp.toPx() * transit, chromaticAberration = true)
                        },
                        highlight = {
                            val p = progress()
                            Highlight.Default.copy(alpha = 0.5f + 0.5f * (abs(p - round(p)) * 2f).coerceIn(0f, 1f))
                        },
                        shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.12f)) },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.08f)) }
                    )
                    .width(tabWidth)
                    .height(TabHeight)
            )
        }
    }
}

@Composable
private fun TabLabels(
    titles: List<String>,
    selectedIndex: Int,
    color: Color,
    labelSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    onSelect: ((Int) -> Unit)? = null
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TabGap)) {
        titles.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(TabHeight)
                    .clip(RoundedCornerShape(50))
                    .then(
                        if (onSelect != null) {
                            Modifier.selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = labelSize),
                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                    color = color,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
