package com.emanuel5014.trainable.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.emanuel5014.trainable.ui.components.localizedNavItems
import com.emanuel5014.trainable.ui.navigation.RoutineDetail
import com.emanuel5014.trainable.ui.navigation.Settings
import com.emanuel5014.trainable.ui.navigation.WorkoutExecution
import com.emanuel5014.trainable.ui.screens.analytics.AnalyticsScreen
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardScreen
import com.emanuel5014.trainable.ui.screens.history.HistoryScreen
import com.emanuel5014.trainable.ui.screens.routines.RoutineListScreen

@Composable
fun MainPagerScreen(
    navController: NavHostController,
    pagerState: PagerState
) {
    val navItemsList = localizedNavItems()
    
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 3
    ) { page ->
        when (page) {
            0 -> DashboardScreen(
                onNavigateToSettings = { navController.navigate(Settings) },
                onNavigateToWorkout = { planId, sessionId ->
                    navController.navigate(WorkoutExecution(planId = planId, sessionId = sessionId))
                },
                onNavigateToQuickWorkout = { name ->
                    navController.navigate(WorkoutExecution(quickStart = true, workoutName = name))
                }
            )
            1 -> RoutineListScreen(
                onNavigateToDetail = { planId ->
                    navController.navigate(RoutineDetail(planId))
                }
            )
            2 -> HistoryScreen(navController = navController)
            3 -> AnalyticsScreen()
        }
    }
}
