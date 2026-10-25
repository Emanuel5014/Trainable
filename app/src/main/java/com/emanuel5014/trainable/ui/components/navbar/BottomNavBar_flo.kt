package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.navigation.MainTabs
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.ShapeFull
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.flow.map

private val BarPadding = 6.dp
private val PillInset = 3.dp

@Composable
fun BottomNavBarFlo(
    navController: NavHostController,
    pagerState: androidx.compose.foundation.pager.PagerState,
    hazeState: HazeState,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)

    val isOnMainTabs = currentRoute?.contains("MainTabs") == true || currentRoute == null
    val items = localizedNavItems()
    val pillState = rememberNavPillState(pagerState, items.size)
    val hazeTintColor = SurfaceContainerHigh.copy(alpha = if (isDark) 0.65f else 0.95f)
    val barShape = MaterialTheme.shapes.extraLarge
    val itemHeight = 64.dp
    val pillColor = Primary

    // The pill swells a little while a finger holds it. Kept as State so only the draw phase reads it.
    val grab = animateFloatAsState(
        targetValue = if (pillState.isDragging) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillGrab"
    )

    Surface(
        modifier = modifier
            .padding(horizontal = if (ResponsiveSize.isCompact) 12.dp else 24.dp)
            .padding(bottom = if (ResponsiveSize.isCompact) 12.dp else 20.dp),
        shape = barShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = if (isDark) 10.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(barShape)
                .hazeEffect(state = hazeState) {
                    blurRadius = 32.dp
                    tints = listOf(HazeTint(hazeTintColor))
                    noiseFactor = 0.1f
                }
                .border(
                    width = 1.dp,
                    color = OutlineVariant.copy(alpha = if (isDark) 0.35f else 0.3f),
                    shape = barShape
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Both of these see the whole bar (before the padding): the finger and the pill
                    // share one coordinate space. Taps never reach this gesture, only a horizontal drag does.
                    .pointerInput(pillState) {
                        val barPadding = BarPadding.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                                change.consume()
                            } ?: return@awaitEachGesture

                            val cellWidth = (size.width - 2 * barPadding) / pillState.count
                            fun fingerPosition(x: Float) =
                                NavPillMath.positionForFinger(x - barPadding, cellWidth, pillState.count)

                            var tickIndex = NavPillMath.nearestIndex(
                                fingerPosition(dragStart.position.x), pillState.count
                            )
                            pillState.beginDrag(fingerPosition(dragStart.position.x))
                            val finished = horizontalDrag(dragStart.id) { change ->
                                val position = fingerPosition(change.position.x)
                                pillState.dragTo(position)
                                change.consume()

                                val index = NavPillMath.closestIndexWithHysteresis(
                                    tickIndex, position, pillState.count
                                )
                                if (index != tickIndex) {
                                    tickIndex = index
                                    if (hapticEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                            pillState.endDrag(cancelled = !finished)
                        }
                    }
                    // Active indicator: a capsule nearly as tall as the bar that follows the finger,
                    // the pager or its settle spring. Drawn here so moving it never recomposes anything.
                    .drawBehind {
                        val barPadding = BarPadding.toPx()
                        val inset = PillInset.toPx()
                        val cellWidth = (size.width - 2 * barPadding) / pillState.count
                        val pillSize = Size(
                            width = cellWidth + 2 * (barPadding - inset),
                            height = size.height - 2 * inset
                        )
                        val topLeft = Offset(inset + pillState.position * cellWidth, inset)
                        val swell = 1f + 0.03f * grab.value
                        scale(swell, swell, pivot = topLeft + Offset(pillSize.width / 2, pillSize.height / 2)) {
                            drawRoundRect(
                                color = pillColor.copy(alpha = lerp(0.14f, 0.22f, grab.value)),
                                topLeft = topLeft,
                                size = pillSize,
                                cornerRadius = CornerRadius(pillSize.height / 2)
                            )
                        }
                    }
                    .padding(horizontal = BarPadding, vertical = BarPadding)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        // 1 with the pill right on this item, fading to 0 one item away: tint, weight and
                        // icon lift all follow it, so the item lights up as the pill passes
                        val proximity by remember(index) {
                            derivedStateOf { NavPillMath.proximity(pillState.position, index) }
                        }
                        val itemTint = lerpColor(
                            OnSurfaceVariant.copy(alpha = 0.55f), Primary, proximity
                        )
                        val iconScale = lerp(1f, 1.18f, proximity)
                        val iconOffsetY = (-3).dp * proximity

                        // Squeeze feedback on press, snapping back with a spring
                        val pressScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.93f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessHigh
                            ),
                            label = "pressScale"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(itemHeight)
                                .clip(ShapeFull)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    if (!isOnMainTabs || index != pagerState.targetPage) {
                                        if (hapticEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        if (!isOnMainTabs) {
                                            navController.navigate(MainTabs) {
                                                launchSingleTop = true
                                            }
                                        }
                                        pillState.settleOn(index)
                                    }
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .offset(y = iconOffsetY)
                                    .scale(pressScale * iconScale)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = itemTint,
                                    modifier = Modifier.size(23.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    // label weight 600 -> 800 using the variable font
                                    fontWeight = FontWeight(lerp(600, 800, proximity)),
                                    letterSpacing = 0.1.sp
                                ),
                                color = itemTint,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
