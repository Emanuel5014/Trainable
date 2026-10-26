package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.navigation.MainTabs
import com.emanuel5014.trainable.ui.theme.*
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.math.roundToInt

private val BarPadding = 6.dp
private val PillInset = 3.dp

@Stable
private class WaterTouchState {
    var down by mutableStateOf(false)
    var yFraction by mutableFloatStateOf(0.5f)
    var stretch by mutableFloatStateOf(0f)
}

@Composable
fun BottomNavBarFlo(
    navController: NavHostController,
    pagerState: androidx.compose.foundation.pager.PagerState,
    hazeState: HazeState,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val isOnMainTabs = route?.contains("MainTabs") == true || route == null
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)
    val items = localizedNavItems()
    val pill = rememberNavPillState(pagerState, items.size)
    val touch = remember { WaterTouchState() }
    val glass = LocalLiquidGlassEnabled.current
    val barShape = if (glass) ShapeFull else MaterialTheme.shapes.extraLarge
    val neutral = OnSurfaceVariant
    val primary = if (glass && isDark) androidx.compose.ui.graphics.lerp(Primary, OnSurface, 0.35f) else Primary
    val tint = SurfaceContainerHigh.copy(alpha = if (isDark) 0.65f else 0.95f)
    val blurStyle = remember(tint) {
        HazeBlurStyle {
            blurRadius(32.dp)
            colorEffects(listOf(HazeColorEffect.tint(tint)))
            noiseFactor(0.025f)
        }
    }
    val blurInput = remember(hazeState) { HazeInput.Sources(hazeState) }
    val grab = animateFloatAsState(
        if (touch.down) 1f else 0f,
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "waterDropGrab"
    )
    val density = LocalDensity.current
    val itemHeight = 64.dp
    val barHeight = itemHeight + BarPadding * 2

    Surface(
        modifier = modifier
            .padding(horizontal = if (ResponsiveSize.isCompact) 12.dp else 24.dp)
            .padding(bottom = if (ResponsiveSize.isCompact) 12.dp else 20.dp),
        shape = barShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = if (glass) 4.dp else if (isDark) 10.dp else 0.dp
    ) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(barHeight).clip(barShape)
                .then(if (glass) Modifier.navigationGlass(hazeState, isDark)
                    else Modifier.hazeBlur(blurInput, blurStyle))
                .border(0.5.dp, Color.White.copy(alpha = if (isDark) 0.16f else 0.55f), barShape)
                .testTag("liquid_navigation")
                .pointerInput(pill, hapticEnabled) {
                    val padding = BarPadding.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        touch.down = true
                        touch.stretch = 0f
                        touch.yFraction = (down.position.y / size.height).coerceIn(0f, 1f)
                        try {
                            val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                                ?: return@awaitEachGesture
                            val cell = (size.width - padding * 2) / pill.count
                            fun position(x: Float) = NavPillMath.positionForFinger(x - padding, cell, pill.count)
                            var previousX = start.position.x
                            var tick = NavPillMath.nearestIndex(position(previousX), pill.count)
                            pill.beginDrag(position(previousX))
                            val finished = horizontalDrag(start.id) { change ->
                                touch.yFraction = (change.position.y / size.height).coerceIn(0f, 1f)
                                val delta = abs(change.position.x - previousX) / cell
                                touch.stretch = (touch.stretch * 0.55f + delta * 0.45f).coerceIn(0f, 0.14f)
                                previousX = change.position.x
                                val next = position(previousX)
                                pill.dragTo(next)
                                change.consume()
                                val index = NavPillMath.closestIndexWithHysteresis(tick, next, pill.count)
                                if (index != tick) {
                                    tick = index
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            pill.endDrag(cancelled = !finished)
                        } finally {
                            touch.down = false
                            // Handles cancellation by another gesture or detaching the navbar.
                            if (pill.isDragging) pill.endDrag(cancelled = true)
                        }
                    }
                }
        ) {
            val cellWidth = (maxWidth - BarPadding * 2) / items.size
            val cellPx = with(density) { cellWidth.toPx() }
            val insetPx = with(density) { PillInset.toPx() }
            if (glass) {
                Box(
                    Modifier.offset { IntOffset((insetPx + pill.position * cellPx).roundToInt(), insetPx.roundToInt()) }
                        .width(cellWidth + (BarPadding - PillInset) * 2)
                        .height(barHeight - PillInset * 2)
                        .graphicsLayer {
                            val held = grab.value
                            scaleX = 1f + held * (0.045f + touch.stretch)
                            scaleY = 1f + held * (0.025f - touch.stretch * 0.35f)
                            translationY = (touch.yFraction - 0.5f) * size.height * 0.12f * held
                        }
                        .navigationGlass(hazeState, isDark, droplet = true)
                        .drawWithContent {
                            drawContent()
                            // The restrained glint follows the finger; it never changes the
                            // brightness of the whole bar or creates another blur pass.
                            drawRoundRect(
                                Brush.radialGradient(
                                    listOf(Color.White.copy(alpha = 0.07f + grab.value * 0.07f), Color.Transparent),
                                    Offset(size.width * 0.3f, size.height * touch.yFraction),
                                    size.width * 0.9f
                                ),
                                cornerRadius = CornerRadius(size.height / 2)
                            )
                        }
                )
            } else {
                Box(Modifier.fillMaxSize().drawBehind {
                    drawRoundRect(
                        primary.copy(alpha = 0.14f),
                        topLeft = Offset(insetPx + pill.position * cellPx, insetPx),
                        size = Size(cellPx + (BarPadding - PillInset).toPx() * 2, size.height - insetPx * 2),
                        cornerRadius = CornerRadius(size.height / 2)
                    )
                })
            }
            Row(Modifier.fillMaxSize().padding(BarPadding), verticalAlignment = Alignment.CenterVertically) {
                items.forEachIndexed { index, item ->
                    val interaction = remember { MutableInteractionSource() }
                    val selected by remember(index, pagerState) { derivedStateOf { pagerState.targetPage == index } }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(ShapeFull)
                            .testTag("nav_tab_$index")
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                interactionSource = interaction,
                                indication = null
                            ) {
                                if (!isOnMainTabs || index != pagerState.targetPage) {
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!isOnMainTabs) navController.navigate(MainTabs) { launchSingleTop = true }
                                    pill.settleOn(index)
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = if (selected) primary else neutral.copy(alpha = if (glass) 0.9f else 0.65f),
                            modifier = Modifier.size(23.dp).graphicsLayer {
                                val proximity = NavPillMath.proximity(pill.position, index)
                                scaleX = 1f + proximity * 0.10f
                                scaleY = scaleX
                                translationY = -2.dp.toPx() * proximity
                            }
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            item.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp
                            ),
                            color = if (selected) primary else neutral.copy(alpha = 0.9f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
