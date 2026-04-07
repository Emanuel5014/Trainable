package com.example.gymtracking.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.gymtracking.ui.screens.MainPagerScreen
import com.example.gymtracking.ui.screens.routines.RoutineDetailScreen
import com.example.gymtracking.ui.screens.settings.SettingsScreen
import com.example.gymtracking.ui.screens.workout.WorkoutExecutionScreen

@Composable
fun MainNavGraph(
    navController: NavHostController,
    pagerState: PagerState,
    startDestination: Any = MainTabs,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn() + slideInHorizontally { it } },
        exitTransition = { fadeOut() + slideOutHorizontally { -it } },
        popEnterTransition = { fadeIn() + slideInHorizontally { -it } },
        popExitTransition = { fadeOut() + slideOutHorizontally { it } }
    ) {
        composable<MainTabs> {
            MainPagerScreen(
                navController = navController,
                pagerState = pagerState
            )
        }
        composable<Settings> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<WorkoutExecution> {
            WorkoutExecutionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<RoutineDetail> { backStackEntry ->
            val detail = backStackEntry.toRoute<RoutineDetail>()
            RoutineDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartWorkout = { planId ->
                    navController.navigate(WorkoutExecution(planId = planId))
                }
            )
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name)
    }
}
