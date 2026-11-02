package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt

private val TabHeight = 44.dp
private val TabGap = 4.dp

/**
 * Tabs in Liquid Glass, like the segmented controls of iOS 26: a glass track, and a glass capsule on the
 * selected tab that shows the accent colour only inside itself. While it slides between tabs it becomes a
 * drop that magnifies the labels beneath, then settles again.
 *
 * [progress] is where the capsule is, in tabs (0 on the first, 1 on the second...), read in draw only;
 * [selectedIndex] is the tab the labels and accessibility treat as selected.
 *
 * The capsule can also be held and dragged, like the floating nav bar's pill: [onScrub] receives where the
 * finger has put it (in tabs, so 0.4 is a bit past the first) and is expected to move whatever [progress]
 * reads; [onScrubEnd] gets the tab it was dropped on. The drag is consumed here, so a pager around the tabs
 * does not take it as a swipe between its own pages. [onTick] fires each time the drop crosses to another tab.
 */
@Composable
fun GlassSegmentedTabs(
    titles: List<String>,
    selectedIndex: Int,
    progress: () -> Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelSize: androidx.compose.ui.unit.TextUnit = MaterialTheme.typography.labelLarge.fontSize,
    onScrub: ((position: Float) -> Unit)? = null,
    onScrubEnd: ((index: Int) -> Unit)? = null,
    onTick: () -> Unit = {}
) {
    val scrub by rememberUpdatedState(onScrub)
    val scrubEnd by rememberUpdatedState(onScrubEnd)
    val tick by rememberUpdatedState(onTick)
    // A finger is on the tabs: the capsule swells into a drop even if it is not between two tabs
    var held by remember { mutableStateOf(false) }
    val press = animateFloatAsState(
        targetValue = if (held) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tabsPress"
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val trackBackdrop = rememberCanvasBackdrop { drawRect(track) }
    val accentLabels = rememberLayerBackdrop()
    val pillBackdrop = rememberCombinedBackdrop(trackBackdrop, accentLabels)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .surfaceGlass(GlassPart.Tabs)
            .clip(RoundedCornerShape(50))
            .pointerInput(titles.size) {
                val gap = TabGap.toPx()
                val lastIndex = (titles.size - 1).toFloat()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (scrub == null || scrubEnd == null) return@awaitEachGesture
                    held = true
                    val tabWidth = (size.width - gap * 2 - gap * lastIndex) / titles.size
                    fun fingerPosition(x: Float) =
                        ((x - gap - tabWidth / 2f) / (tabWidth + gap)).coerceIn(0f, lastIndex)

                    val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    if (start == null) {
                        // a tap: the tab's own click handles it
                        held = false
                        return@awaitEachGesture
                    }
                    var position = fingerPosition(start.position.x)
                    var tabIndex = position.roundToInt()
                    scrub?.invoke(position)
                    horizontalDrag(start.id) { change ->
                        position = fingerPosition(change.position.x)
                        scrub?.invoke(position)
                        change.consume()
                        val index = position.roundToInt()
                        if (index != tabIndex) {
                            tabIndex = index
                            tick()
                        }
                    }
                    held = false
                    scrubEnd?.invoke(position.roundToInt().coerceIn(0, titles.size - 1))
                }
            }
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
                        // 0 at rest on a tab, 1 half way or held: the capsule becomes a drop while it travels
                        val transit = max((abs(p - round(p)) * 2f).coerceIn(0f, 1f), press.value)
                        translationX = p * (tabWidth + TabGap).toPx()
                        scaleX = 1f + 0.12f * transit
                        scaleY = 1f + 0.06f * transit
                    }
                    .drawBackdrop(
                        backdrop = pillBackdrop,
                        shape = { Capsule() },
                        effects = {
                            val p = progress()
                            val transit = max((abs(p - round(p)) * 2f).coerceIn(0f, 1f), press.value)
                            lens(10.dp.toPx() * transit, 14.dp.toPx() * transit, chromaticAberration = true)
                        },
                        highlight = {
                            val p = progress()
                            Highlight.Default.copy(
                                alpha = 0.5f + 0.5f * max((abs(p - round(p)) * 2f).coerceIn(0f, 1f), press.value)
                            )
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
