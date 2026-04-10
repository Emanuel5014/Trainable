package com.example.gymtracking.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.gymtracking.R
import com.example.gymtracking.ui.navigation.MainTabs
import com.example.gymtracking.ui.theme.OnPrimary
import com.example.gymtracking.ui.theme.OnSurfaceVariant
import com.example.gymtracking.ui.theme.Primary
import com.example.gymtracking.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.launch

@Composable
fun localizedNavItems(): List<LocalizedNavItem> = listOf(
    LocalizedNavItem(stringResource(R.string.nav_dashboard), Icons.Rounded.Home),
    LocalizedNavItem(stringResource(R.string.nav_routines), Icons.Rounded.FitnessCenter),
    LocalizedNavItem(stringResource(R.string.nav_history), Icons.Rounded.History),
    LocalizedNavItem(stringResource(R.string.nav_analytics), Icons.Rounded.Insights)
)

data class LocalizedNavItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun BottomNavBar(
    navController: NavHostController,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val navItemsList = localizedNavItems()

    val isOnMainTabs = currentRoute?.contains("MainTabs") == true || currentRoute == null
    val currentPage = pagerState.currentPage

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .wrapContentSize()
                .clip(RoundedCornerShape(32.dp))
                .background(SurfaceContainerHigh.copy(alpha = 0.95f))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItemsList.forEachIndexed { index, item ->
                val isSelected = isOnMainTabs && currentPage == index

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) Primary else Color.Transparent,
                    label = "item_container_color"
                )
                
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) OnPrimary else OnSurfaceVariant,
                    label = "item_content_color"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
