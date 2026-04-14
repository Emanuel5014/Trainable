package com.example.gymtracking.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.gymtracking.data.repository.UserPreferencesRepository
import com.example.gymtracking.data.repository.dataStore
import com.example.gymtracking.ui.navigation.MainTabs
import com.example.gymtracking.ui.theme.OnSurfaceVariant
import com.example.gymtracking.ui.theme.Primary
import com.example.gymtracking.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun BottomNavBar(
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

    NavigationBar(
        modifier = modifier.navigationBarsPadding(),
        containerColor = SurfaceContainerHigh,
        contentColor = Primary,
        tonalElevation = 0.dp
    ) {
        localizedNavItems().forEachIndexed { index, item ->
            val isSelected = isOnMainTabs && selectedIndex == index
            
            NavigationBarItem(
                selected = isSelected,
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
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(text = item.title)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    unselectedIconColor = OnSurfaceVariant,
                    unselectedTextColor = OnSurfaceVariant,
                    indicatorColor = Primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}
