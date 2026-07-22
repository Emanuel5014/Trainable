package com.emanuel5014.trainable.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.BottomBarManager
import com.emanuel5014.trainable.ui.components.localizedNavItems
import com.emanuel5014.trainable.ui.navigation.RoutineDetail
import com.emanuel5014.trainable.ui.navigation.Settings
import com.emanuel5014.trainable.ui.navigation.WorkoutExecution
import com.emanuel5014.trainable.ui.navigation.PhysicalCheck
import com.emanuel5014.trainable.ui.navigation.Report
import com.emanuel5014.trainable.ui.screens.analytics.AnalyticsScreen
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardScreen
import com.emanuel5014.trainable.ui.screens.history.HistoryScreen
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
                onNavigateToPhysicalChecks = { navController.navigate(PhysicalCheck) }
            )
            1 -> RoutineListScreen(
                onNavigateToDetail = { planId ->
                    navController.navigate(RoutineDetail(planId))
                },
                onGenerateReport = { planIds ->
                    navController.navigate(Report(planIds.joinToString(",")))
                }
            )
            2 -> HistoryScreen(navController = navController)
            3 -> AnalyticsScreen()
        }
    }
}
