package com.example.gymtracking.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.gymtracking.ui.components.localizedNavItems
import com.example.gymtracking.ui.navigation.Analytics
import com.example.gymtracking.ui.navigation.Dashboard
import com.example.gymtracking.ui.navigation.History
import com.example.gymtracking.ui.navigation.RoutineDetail
import com.example.gymtracking.ui.navigation.Routines
import com.example.gymtracking.ui.navigation.Settings
import com.example.gymtracking.ui.navigation.WorkoutExecution
import com.example.gymtracking.ui.screens.analytics.AnalyticsScreen
import com.example.gymtracking.ui.screens.dashboard.DashboardScreen
import com.example.gymtracking.ui.screens.history.HistoryScreen
import com.example.gymtracking.ui.screens.routines.RoutineListScreen

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
