package com.emanuel5014.trainable.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.ui.components.BottomBarManager
import com.emanuel5014.trainable.ui.components.BottomNavBar
import com.emanuel5014.trainable.ui.components.BottomNavBarExpressive
import com.emanuel5014.trainable.ui.components.BottomNavBarFlo
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.localizedNavItems
import com.emanuel5014.trainable.ui.navigation.MainTabs
import com.emanuel5014.trainable.ui.navigation.PhysicalCheck
import com.emanuel5014.trainable.ui.navigation.Report
import com.emanuel5014.trainable.ui.navigation.RoutineDetail
import com.emanuel5014.trainable.ui.navigation.Settings
import com.emanuel5014.trainable.ui.navigation.WorkoutExecution
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardContent
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardState
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardUiState
import com.emanuel5014.trainable.ui.screens.routines.RoutineListContent
import com.emanuel5014.trainable.ui.screens.routines.RoutinesState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * The iOS app's screens, with the same routes, tabs and navbars as the Android app (MainActivity + NavGraph). The
 * screens not shared yet open a stand-in page. Liquid Glass is Android only: the glass floating bar shows as the
 * blurred one.
 */
@Composable
internal fun IosNavGraph(
    dashboard: DashboardState,
    dashboardUi: DashboardUiState,
    routines: RoutinesState,
    navBarStyle: NavBarStyle,
    isDark: Boolean
) {
    val navController = rememberNavController()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val hazeState = rememberHazeState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(
            navController = navController,
            startDestination = MainTabs,
            modifier = Modifier.fillMaxSize().hazeSource(state = hazeState),
            enterTransition = { fadeIn() + slideInHorizontally { it } },
            exitTransition = { fadeOut() + slideOutHorizontally { -it } },
            popEnterTransition = { fadeIn() + slideInHorizontally { -it } },
            popExitTransition = { fadeOut() + slideOutHorizontally { it } }
        ) {
            composable<MainTabs> {
                IosMainTabs(navController, pagerState, dashboard, dashboardUi, routines)
            }
            composable<Settings> { NotOnIosYet("Settings", onBack = { navController.popBackStack() }) }
            composable<WorkoutExecution> { NotOnIosYet("Workout", onBack = { navController.popBackStack() }) }
            composable<PhysicalCheck> { NotOnIosYet("Physical check", onBack = { navController.popBackStack() }) }
            composable<RoutineDetail> { NotOnIosYet("Routine", onBack = { navController.popBackStack() }) }
            composable<Report> { NotOnIosYet("Report", onBack = { navController.popBackStack() }) }
        }

        AnimatedVisibility(
            visible = navBackStackEntry?.destination?.hasRoute(MainTabs::class) == true && BottomBarManager.isVisibleOverride,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            when (navBarStyle) {
                NavBarStyle.Floating -> BottomNavBarFlo(navController, pagerState, hazeState, isDark)
                NavBarStyle.Expressive -> BottomNavBarExpressive(navController, pagerState)
                else -> BottomNavBar(navController, pagerState)
            }
        }
    }
}

/** The four tabs, as MainPagerScreen on Android; Dashboard and Routines are shared so far. */
@Composable
private fun IosMainTabs(
    navController: NavHostController,
    pagerState: PagerState,
    dashboard: DashboardState,
    dashboardUi: DashboardUiState,
    routines: RoutinesState
) {
    val titles = localizedNavItems().map { it.title }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = !BottomBarManager.swipeLocked,
        beyondViewportPageCount = 3
    ) { page ->
        when (page) {
            0 -> DashboardContent(
                uiState = dashboardUi,
                actions = dashboard,
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
            1 -> RoutineListContent(
                state = routines,
                // The exercise categories in the phone's language until the app's own choice reaches iOS
                languageCode = Locale.current.language,
                // Sharing a routine file is not on iOS yet
                sharePlans = { _, _, _, _ -> },
                isVisible = pagerState.currentPage == 1,
                onNavigateToDetail = { planId -> navController.navigate(RoutineDetail(planId)) },
                onGenerateReport = { planIds -> navController.navigate(Report(planIds.joinToString(","))) }
            )
            else -> NotOnIosYet(titles[page], onBack = null)
        }
    }
}

/** Stand-in for a screen the iOS app does not have yet (development only, not translated). */
@Composable
private fun NotOnIosYet(title: String, onBack: (() -> Unit)?) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        if (onBack != null) {
            GymIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = onBack,
                description = "Back",
                modifier = Modifier.padding(16.dp)
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Not on iOS yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
