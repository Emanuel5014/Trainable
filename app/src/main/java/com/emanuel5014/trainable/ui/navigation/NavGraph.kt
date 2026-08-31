package com.emanuel5014.trainable.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.emanuel5014.trainable.ui.screens.MainPagerScreen
import com.emanuel5014.trainable.ui.screens.compare.CompareSessionsScreen
import com.emanuel5014.trainable.ui.screens.history.EditWorkoutScreen
import com.emanuel5014.trainable.ui.screens.routines.RoutineDetailScreen
import com.emanuel5014.trainable.ui.screens.routines.ReportScreen
import com.emanuel5014.trainable.ui.screens.settings.SettingsScreen
import com.emanuel5014.trainable.ui.screens.settings.ExerciseCustomizationScreen
import com.emanuel5014.trainable.ui.screens.settings.WorkoutSettingsScreen
import com.emanuel5014.trainable.ui.screens.settings.DonorsScreen
import com.emanuel5014.trainable.ui.screens.workout.WorkoutExecutionScreen
import com.emanuel5014.trainable.ui.screens.physicalcheck.PhysicalCheckScreen
import com.emanuel5014.trainable.ui.screens.physicalcheck.PhysicalCheckSettingsScreen
import com.emanuel5014.trainable.ui.screens.physicalcheck.PhysicalCheckCompareScreen

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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWorkoutSettings = { navController.navigate(WorkoutSettings) },
                onNavigateToDonors = { navController.navigate(Donors) }
            )
        }
        composable<Donors> {
            DonorsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ExerciseCustomization> {
            ExerciseCustomizationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<WorkoutSettings> {
            WorkoutSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExerciseCustomization = { navController.navigate(ExerciseCustomization) }
            )
        }
        composable<WorkoutExecution> {
            WorkoutExecutionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRoutine = { planId ->
                    navController.navigate(RoutineDetail(planId = planId))
                }
            )
        }
        composable<EditWorkoutSession> {
            EditWorkoutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<CompareSessions> { backStackEntry ->
            val route = backStackEntry.toRoute<CompareSessions>()
            CompareSessionsScreen(
                sessionId1 = route.sessionId1,
                sessionId2 = route.sessionId2,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<RoutineDetail> { backStackEntry ->
            val detail = backStackEntry.toRoute<RoutineDetail>()
            RoutineDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartWorkout = { planId, sessionId ->
                    navController.navigate(WorkoutExecution(planId = planId, sessionId = sessionId))
                }
            )
        }
        composable<PhysicalCheck> {
            PhysicalCheckScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(PhysicalCheckSettings) },
                onNavigateToCompare = { id1, id2 ->
                    navController.navigate(PhysicalCheckCompare(id1 = id1, id2 = id2))
                }
            )
        }
        composable<PhysicalCheckSettings> {
            PhysicalCheckSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<PhysicalCheckCompare> { backStackEntry ->
            val route = backStackEntry.toRoute<PhysicalCheckCompare>()
            PhysicalCheckCompareScreen(
                checkId1 = route.id1,
                checkId2 = route.id2,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Report> {
            ReportScreen(
                onNavigateBack = { navController.popBackStack() }
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
