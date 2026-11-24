package com.emanuel5014.trainable.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.BottomBarManager
import com.emanuel5014.trainable.ui.components.LocalMainPagerState
import com.emanuel5014.trainable.ui.components.localizedNavItems
import com.emanuel5014.trainable.ui.navigation.RoutineDetail
import com.emanuel5014.trainable.ui.navigation.Settings
import com.emanuel5014.trainable.ui.navigation.WorkoutExecution
import com.emanuel5014.trainable.ui.navigation.PhysicalCheck
import com.emanuel5014.trainable.ui.navigation.Report
import com.emanuel5014.trainable.ui.screens.analytics.AnalyticsScreen
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardScreen
import com.emanuel5014.trainable.ui.screens.history.HistoryScreen
import com.emanuel5014.trainable.ui.screens.routines.RoutineFromWorkoutHost
import com.emanuel5014.trainable.ui.screens.routines.RoutineFromWorkoutViewModel
import com.emanuel5014.trainable.ui.screens.routines.RoutineListScreen
import kotlinx.coroutines.launch

@Composable
fun MainPagerScreen(
    navController: NavHostController,
    pagerState: PagerState
) {
    val navItemsList = localizedNavItems()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val backPressTime = remember { mutableStateOf(0L) }
    val backPressMessage = stringResource(R.string.press_back_again)

    BackHandler {
        if (pagerState.currentPage != 0) {
            scope.launch { pagerState.animateScrollToPage(0) }
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressTime.value < 2000L) {
                (context as? Activity)?.finishAffinity()
            } else {
                backPressTime.value = currentTime
                Toast.makeText(context, backPressMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val routineFromWorkout: RoutineFromWorkoutViewModel = hiltViewModel()
    val routineLanguage by routineFromWorkout.languageCode.collectAsState()

    // Rows with quick swipe actions hand fast flicks back to this pager
    CompositionLocalProvider(LocalMainPagerState provides pagerState) {
    Box(modifier = Modifier.fillMaxSize()) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = !BottomBarManager.swipeLocked,
        beyondViewportPageCount = 3
    ) { page ->
        when (page) {
            0 -> DashboardScreen(
                isActive = pagerState.currentPage == 0,
                onNavigateToSettings = { navController.navigate(Settings) },
                onNavigateToWorkout = { planId, sessionId ->
                    navController.navigate(WorkoutExecution(planId = planId, sessionId = sessionId))
                },
                onNavigateToQuickWorkout = { name ->
                    navController.navigate(WorkoutExecution(quickStart = true, workoutName = name))
                },
                onNavigateToPhysicalChecks = { navController.navigate(PhysicalCheck) },
                onLogSet = { planId, sessionId, token ->
                    navController.navigate(WorkoutExecution(planId = planId, sessionId = sessionId, logSet = token))
                }
            )
            1 -> RoutineListScreen(
                isVisible = pagerState.currentPage == 1,
                onNavigateToDetail = { planId ->
                    navController.navigate(RoutineDetail(planId))
                },
                onGenerateReport = { planIds ->
                    navController.navigate(Report(planIds.joinToString(",")))
                }
            )
            2 -> HistoryScreen(
                navController = navController,
                isVisible = pagerState.currentPage == 2
            )
            3 -> AnalyticsScreen(
                onNavigateToOneRepMaxes = { navController.navigate(com.emanuel5014.trainable.ui.navigation.OneRepMaxes) }
            )
        }
    }
    // "Save as routine": the offer after a quick workout and the sheets the tabs ask for
    RoutineFromWorkoutHost(
        state = routineFromWorkout.state,
        languageCode = routineLanguage,
        onOpenRoutine = { planId -> navController.navigate(RoutineDetail(planId)) }
    )
    }
    }
}
