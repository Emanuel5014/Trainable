package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    val isOnMainTabs = currentRoute?.contains("MainTabs") == true || currentRoute == null
    val selectedIndex = if (isOnMainTabs) pagerState.currentPage else 0
    val items = localizedNavItems()
    val hazeTintColor = SurfaceContainerHigh.copy(alpha = if (isDark) 0.65f else 0.95f)
    val barShape = MaterialTheme.shapes.extraLarge
    val itemHeight = 64.dp

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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                val totalWidth = maxWidth
                val itemWidth = totalWidth / items.size

                // Active indicator — pill that glides with an expressive spring
                val indicatorOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "indicatorOffset"
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(itemWidth)
                        .height(itemHeight)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .clip(ShapeFull)
                        .background(Primary.copy(alpha = 0.14f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = isOnMainTabs && selectedIndex == index
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        // Icon/label color morphs smoothly between onSurfaceVariant and primary
                        val itemTint by animateColorAsState(
                            targetValue = if (isSelected) Primary
                            else OnSurfaceVariant.copy(alpha = 0.55f),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "itemTint"
                        )

                        // Selected icon grows and lifts slightly (expressive emphasis)
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.18f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "iconScale"
                        )

                        val iconOffsetY by animateDpAsState(
                            targetValue = if (isSelected) (-3).dp else 0.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "iconOffsetY"
                        )

                        // Squeeze feedback on press, snapping back with a spring
                        val pressScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.93f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessHigh
                            ),
                            label = "pressScale"
                        )

                        // Label weight animates 600 -> 800 using the variable font
                        val labelWeight by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "labelWeight"
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
                                    if (!isSelected) {
                                        if (hapticEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        if (!isOnMainTabs) {
                                            navController.navigate(MainTabs) {
                                                launchSingleTop = true
                                            }
                                        }
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
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
                                    fontWeight = FontWeight(lerp(600, 800, labelWeight)),
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
