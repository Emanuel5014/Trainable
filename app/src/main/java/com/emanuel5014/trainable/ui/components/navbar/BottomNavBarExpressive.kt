package com.emanuel5014.trainable.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.navigation.MainTabs
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ShapeFull
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val ToolbarPadding = 8.dp
private val TabSize = 48.dp
private val LabelGap = 8.dp

/** Medium FAB: deliberately taller than the 64dp toolbar so the main action stands out. */
private val FabSize = 80.dp
private val FabGap = 8.dp
private val ScreenEdge = 12.dp

/** Lift above the system navigation area. */
private val BottomLift = 16.dp

/** Height the bar takes above the navigation area, including its lift. */
internal val ExpressiveNavBarHeight = FabSize + BottomLift

/**
 * Material 3 Expressive navigation: a floating toolbar with the four tabs, where the selected one
 * grows into a labelled pill. The main action of the tab on screen (see [NavBarActionEffect]) sits
 * as a button next to the toolbar, and slides in and out as the tab changes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomNavBarExpressive(
    navController: NavHostController,
    pagerState: PagerState,
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
    val motion = remember { MotionScheme.expressive() }

    val action = NavBarActionManager.actionFor(selectedIndex)
    // The last action stays around so the button can animate out still showing its icon
    var retainedAction by remember { mutableStateOf(action) }
    LaunchedEffect(action) { if (action != null) retainedAction = action }
    val displayedAction = action ?: retainedAction

    var menuExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(selectedIndex) { menuExpanded = false }
    val menuOpen = menuExpanded && action is NavBarAction.Menu
    BackHandler(menuOpen) { menuExpanded = false }

    val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val selectedTitle = items.getOrNull(selectedIndex)?.title.orEmpty()
    val labelWidth = remember(selectedTitle, labelStyle, density) {
        with(density) { textMeasurer.measure(selectedTitle, labelStyle, maxLines = 1).size.width.toDp() }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = ScreenEdge, end = ScreenEdge, bottom = BottomLift)
    ) {
        // The selected tab only shows its label when it fits next to the action button
        val actionSpace = if (action != null) FabSize + FabGap else 0.dp
        val tabsWithLabel = ToolbarPadding * 2 + TabSize * items.size + LabelGap + labelWidth
        val showLabel = maxWidth - actionSpace >= tabsWithLabel

        FabMenu(
            visible = menuOpen,
            items = (displayedAction as? NavBarAction.Menu)?.items.orEmpty(),
            onItemClick = { item ->
                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                menuExpanded = false
                item.onClick()
            },
            motion = motion
        )

        Row(
            // As tall as the button even on tabs without one, so the toolbar never shifts between tabs
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FabSize)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                        toolbarContainerColor = SurfaceContainerHigh,
                        toolbarContentColor = OnSurfaceVariant
                    ),
                    expandedShadowElevation = 6.dp
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = isOnMainTabs && selectedIndex == index
                        NavBarTab(
                            title = item.title,
                            icon = item.icon,
                            selected = isSelected,
                            showLabel = showLabel,
                            labelStyle = labelStyle,
                            motion = motion,
                            onClick = {
                                if (!isSelected) {
                                    if (hapticEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    if (!isOnMainTabs) {
                                        navController.navigate(MainTabs) {
                                            launchSingleTop = true
                                        }
                                    }
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = action != null,
                enter = fadeIn(motion.fastEffectsSpec()) +
                    scaleIn(motion.fastSpatialSpec(), initialScale = 0.4f) +
                    expandHorizontally(motion.fastSpatialSpec(), expandFrom = Alignment.End),
                exit = fadeOut(motion.fastEffectsSpec()) +
                    scaleOut(motion.fastSpatialSpec(), targetScale = 0.4f) +
                    shrinkHorizontally(motion.fastSpatialSpec(), shrinkTowards = Alignment.End)
            ) {
                displayedAction?.let { shown ->
                    NavBarFab(
                        action = shown,
                        menuOpen = menuOpen,
                        motion = motion,
                        modifier = Modifier.padding(start = FabGap),
                        onClick = {
                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            when (shown) {
                                is NavBarAction.Click -> shown.onClick()
                                is NavBarAction.Menu -> menuExpanded = !menuExpanded
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavBarTab(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    showLabel: Boolean,
    labelStyle: TextStyle,
    motion: MotionScheme,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val labelVisible = selected && showLabel

    val container by animateColorAsState(
        targetValue = if (selected) Primary else Color.Transparent,
        animationSpec = motion.defaultEffectsSpec(),
        label = "tabContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) OnPrimary else OnSurfaceVariant,
        animationSpec = motion.defaultEffectsSpec(),
        label = "tabContent"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "tabPress"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "tabIcon"
    )

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .height(TabSize)
            .clip(ShapeFull)
            .background(container)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title.takeIf { !labelVisible },
            tint = content,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        AnimatedVisibility(
            visible = labelVisible,
            enter = fadeIn(motion.fastEffectsSpec()) +
                expandHorizontally(motion.defaultSpatialSpec(), expandFrom = Alignment.Start),
            exit = fadeOut(motion.fastEffectsSpec()) +
                shrinkHorizontally(motion.defaultSpatialSpec(), shrinkTowards = Alignment.Start)
        ) {
            Text(
                text = title,
                style = labelStyle,
                color = content,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = LabelGap)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavBarFab(
    action: NavBarAction,
    menuOpen: Boolean,
    motion: MotionScheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "fabPress"
    )
    // Rounded square at rest, rounding off to a circle while pressed or when its menu is open
    val corner by animateDpAsState(
        targetValue = if (isPressed || menuOpen) FabSize / 2 else 28.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "fabCorner"
    )
    val rotation by animateFloatAsState(
        targetValue = if (menuOpen) 45f else 0f,
        animationSpec = motion.fastSpatialSpec(),
        label = "fabRotation"
    )

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(action.label) } },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .size(FabSize)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
            shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            interactionSource = interactionSource
        ) {
            AnimatedContent(
                targetState = action.icon,
                transitionSpec = {
                    (fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.fastSpatialSpec(), initialScale = 0.5f))
                        .togetherWith(
                            fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec(), targetScale = 0.5f)
                        )
                },
                label = "fabIcon"
            ) { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = action.label,
                    modifier = Modifier.size(32.dp).rotate(rotation)
                )
            }
        }
    }
}

/** Choices that open upwards from the action button, as in a speed dial. */
@Composable
private fun BoxScope.FabMenu(
    visible: Boolean,
    items: List<NavBarMenuItem>,
    onItemClick: (NavBarMenuItem) -> Unit,
    motion: MotionScheme
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomEnd),
        enter = fadeIn(motion.fastEffectsSpec()) +
            scaleIn(motion.fastSpatialSpec(), initialScale = 0.6f, transformOrigin = TransformOrigin(1f, 1f)),
        exit = fadeOut(motion.fastEffectsSpec()) +
            scaleOut(motion.fastSpatialSpec(), targetScale = 0.6f, transformOrigin = TransformOrigin(1f, 1f))
    ) {
        Column(
            modifier = Modifier.padding(bottom = FabSize + FabGap),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                ExtendedFloatingActionButton(
                    onClick = { onItemClick(item) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = ShapeFull
                ) {
                    Icon(imageVector = item.icon, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = item.label, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
