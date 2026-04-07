package com.example.gymtracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gymtracking.data.repository.UserPreferencesRepository
import com.example.gymtracking.ui.components.BottomNavBar
import com.example.gymtracking.ui.navigation.MainTabs
import com.example.gymtracking.ui.navigation.MainNavGraph
import com.example.gymtracking.ui.navigation.WorkoutExecution
import com.example.gymtracking.ui.screens.onboarding.OnboardingScreen
import com.example.gymtracking.ui.theme.GymTrackingTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymTrackingTheme {
                val hasCompletedOnboarding by userPreferencesRepository.hasCompletedOnboarding.collectAsState(initial = null)
                val onboardingCompletedOverride = remember { mutableStateOf<Boolean?>(null) }
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val resolvedOnboardingState = onboardingCompletedOverride.value ?: hasCompletedOnboarding

                if (resolvedOnboardingState == null) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    return@GymTrackingTheme
                }

                val pagerState = rememberPagerState(pageCount = { 4 })

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (resolvedOnboardingState == true) {
                        MainNavGraph(
                            navController = navController,
                            pagerState = pagerState,
                            startDestination = MainTabs,
                            modifier = Modifier.fillMaxSize()
                        )

                        val showBottomBar = (currentDestination?.hasRoute(MainTabs::class) == true || 
                            currentDestination?.route?.startsWith("MainTabs") == true) && 
                            currentDestination?.hasRoute(WorkoutExecution::class) == false

                        AnimatedVisibility(
                            visible = showBottomBar,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            BottomNavBar(
                                navController = navController,
                                pagerState = pagerState
                            )
                        }
                    } else {
                        OnboardingScreen(
                            onFinished = {
                                onboardingCompletedOverride.value = true
                            }
                        )
                    }
                }
            }
        }
    }
}