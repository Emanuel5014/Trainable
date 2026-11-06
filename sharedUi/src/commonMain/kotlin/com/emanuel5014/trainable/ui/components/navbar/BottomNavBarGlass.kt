package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.emanuel5014.trainable.ui.navigation.MainTabs
import com.emanuel5014.trainable.ui.theme.LocalGlassSettings
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

// Same measures as BottomNavBarFlo, so the screens' floating buttons keep their distance from the bar
private val GlassItemHeight = 64.dp
private val GlassBarPadding = 6.dp
private val GlassPillInset = 3.dp
private val GlassBarHeight = GlassItemHeight + GlassBarPadding * 2
private val GlassPillHeight = GlassBarHeight - GlassPillInset * 2

/** Below Android 12 there is no blur, so the glass needs a fuller body to keep the labels readable. */
private val canBlur = com.emanuel5014.trainable.ui.theme.glassCanBlur

/**
 * The floating navigation in Liquid Glass, modelled on the iOS 26 tab bar.
 *
 * The bar bends and blurs [backdrop] (the screen content). The selected tab sits in a capsule that only
 * shows the accent colour inside itself; while a finger holds or drags it, the capsule swells into a drop
 * of glass that magnifies the tabs beneath. Gestures, haptics and the pager link are those of
 * [BottomNavBarFlo], through [NavPillState].
 *
 * [intensity] (0..1, set by the user) goes from a nearly solid bar with a faint lens to clear, strongly
 * bending glass.
 *
 * The layering (an invisible accent copy of the tabs seen only through the capsule) follows the
 * LiquidBottomTabs example of Kyant0/AndroidLiquidGlass (Apache-2.0).
 */
@Composable
fun BottomNavBarGlass(
    navController: NavHostController,
    pagerState: androidx.compose.foundation.pager.PagerState,
    backdrop: Backdrop,
    isDark: Boolean,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current
    val hapticEnabled by LocalHapticsSetting.current.collectAsState(initial = true)

    val isOnMainTabs = currentRoute?.contains("MainTabs") == true || currentRoute == null
    val items = localizedNavItems()
    val pillState = rememberNavPillState(pagerState, items.size)
    val interactionSources = remember(items.size) { List(items.size) { MutableInteractionSource() } }
    val anyPressed = interactionSources.map { it.collectIsPressedAsState() }.any { it.value }

    val amount = intensity.coerceIn(0f, 1f)
    val blurScale = LocalGlassSettings.current.blurScale
    val containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(
        alpha = if (canBlur) lerp(0.85f, 0.2f, amount) else lerp(0.96f, 0.85f, amount)
    )
    val lensHeight = lerp(8f, 28f, amount).dp
    val lensAmount = lerp(8f, 32f, amount).dp
    val restingPillColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)

    // 0 at rest, 1 while a finger holds the bar: drives the drop, the bar swell and the tab zoom
    val press = animateFloatAsState(
        targetValue = if (pillState.isDragging || anyPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "glassPress"
    )
    val stretch = LocalGlassSettings.current.stretch
    val tabScale = { if (stretch) lerp(1f, 1.2f, press.value) else 1f }

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = if (ResponsiveSize.isCompact) 12.dp else 24.dp)
            .padding(bottom = if (ResponsiveSize.isCompact) 12.dp else 20.dp)
            .fillMaxWidth()
            .pointerInput(pillState) {
                val barPadding = GlassBarPadding.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val cellWidth = (size.width - 2 * barPadding) / pillState.count
                    fun fingerPosition(x: Float) =
                        NavPillMath.positionForFinger(x - barPadding, cellWidth, pillState.count)

                    // The drop forms under the finger, not on the tab that was selected
                    pillState.beginDrag(fingerPosition(down.position.x), snap = true)
                    val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                        change.consume()
                    }
                    if (dragStart == null) {
                        // A tap settles through the tab's own click; a finger lifted off the bar, or a stolen gesture, goes back
                        val last = currentEvent.changes.firstOrNull { it.id == down.id }
                        val liftedOnBar = last != null && last.changedToUpIgnoreConsumed() &&
                            last.position.x in 0f..size.width.toFloat() &&
                            last.position.y in 0f..size.height.toFloat()
                        pillState.endDrag(cancelled = !liftedOnBar)
                        return@awaitEachGesture
                    }

                    var tickIndex = NavPillMath.nearestIndex(fingerPosition(dragStart.position.x), pillState.count)
                    pillState.dragTo(fingerPosition(dragStart.position.x))
                    val finished = horizontalDrag(dragStart.id) { change ->
                        val position = fingerPosition(change.position.x)
                        pillState.dragTo(position)
                        change.consume()

                        val index = NavPillMath.closestIndexWithHysteresis(tickIndex, position, pillState.count)
                        if (index != tickIndex) {
                            tickIndex = index
                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                    pillState.endDrag(cancelled = !finished)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val cellWidth = (maxWidth - GlassBarPadding * 2) / items.size

        // The bar: the tabs in their neutral colour on a lens of glass
        Row(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx() * blurScale)
                        lens(lensHeight.toPx(), lensAmount.toPx())
                    },
                    layerBlock = {
                        val scale = if (stretch) lerp(1f, 1f + 16.dp.toPx() / size.width, press.value) else 1f
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .height(GlassBarHeight)
                .fillMaxWidth()
                .padding(GlassBarPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                GlassTab(
                    item = item,
                    tint = OnSurface.copy(alpha = 0.85f),
                    scale = tabScale,
                    modifier = Modifier.clickable(
                        interactionSource = interactionSources[index],
                        indication = null
                    ) {
                        if (!isOnMainTabs || index != pagerState.targetPage) {
                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!isOnMainTabs) {
                                navController.navigate(MainTabs) { launchSingleTop = true }
                            }
                            pillState.settleOn(index)
                        }
                    }
                )
            }
        }

        // The same tabs in the accent colour, never drawn on screen: recorded for the capsule to show
        Row(
            modifier = Modifier
                .clearAndSetSemantics {}
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx() * blurScale)
                        lens(lensHeight.toPx() * press.value, lensAmount.toPx() * press.value)
                    },
                    highlight = { Highlight.Default.copy(alpha = press.value) },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .height(GlassPillHeight)
                .fillMaxWidth()
                .padding(horizontal = GlassBarPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item -> GlassTab(item = item, tint = Primary, scale = tabScale) }
        }

        // The capsule on the selected tab, as large as the floating bar's pill; it becomes a drop of glass while held
        Box(
            modifier = Modifier
                .padding(start = GlassPillInset)
                .graphicsLayer { translationX = pillState.position * cellWidth.toPx() }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        lens(
                            10.dp.toPx() * press.value,
                            14.dp.toPx() * press.value,
                            chromaticAberration = true
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = press.value) },
                    shadow = { Shadow(alpha = press.value) },
                    innerShadow = { InnerShadow(radius = 8.dp * press.value, alpha = press.value) },
                    layerBlock = {
                        val scale = lerp(1f, 1.3f, press.value)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = {
                        drawRect(restingPillColor, alpha = 1f - press.value)
                        drawRect(Color.Black.copy(alpha = 0.03f * press.value))
                    }
                )
                .height(GlassPillHeight)
                .width(cellWidth + (GlassBarPadding - GlassPillInset) * 2)
        )
    }
}

@Composable
private fun RowScope.GlassTab(
    item: LocalizedNavItem,
    tint: Color,
    scale: () -> Float,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            }
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = tint,
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight(650),
                letterSpacing = 0.1.sp
            ),
            color = tint,
            maxLines = 1
        )
    }
}
