package com.example.gymtracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gymtracking.data.remote.GitHubRelease
import com.example.gymtracking.data.repository.UserPreferencesRepository
import com.example.gymtracking.ui.components.BottomNavBar
import com.example.gymtracking.ui.components.BottomNavBarFlo
import com.example.gymtracking.ui.components.UpdateDialog
import com.example.gymtracking.ui.navigation.MainNavGraph
import com.example.gymtracking.ui.navigation.MainTabs
import com.example.gymtracking.ui.navigation.WorkoutExecution
import com.example.gymtracking.ui.screens.onboarding.OnboardingScreen
import com.example.gymtracking.ui.theme.GymTrackingTheme
import com.example.gymtracking.util.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dynamicColor by userPreferencesRepository.dynamicColor.collectAsState(initial = true)

            var showUpdateDialog by remember { mutableStateOf(false) }
            var latestRelease by remember { mutableStateOf<GitHubRelease?>(null) }
            var isDownloading by remember { mutableStateOf(false) }
            var downloadProgress by remember { mutableFloatStateOf(0f) }
            val scope = rememberCoroutineScope()

            androidx.compose.runtime.LaunchedEffect(Unit) {
                latestRelease = updateManager.checkForUpdates()
                if (latestRelease != null) {
                    showUpdateDialog = true
                }
            }

            GymTrackingTheme(dynamicColor = dynamicColor) {
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

                        val floatingNavBar by userPreferencesRepository.floatingNavBar.collectAsState(initial = false)

                        AnimatedVisibility(
                            visible = showBottomBar,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            if (floatingNavBar) {
                                BottomNavBarFlo(
                                    navController = navController,
                                    pagerState = pagerState
                                )
                            } else {
                                BottomNavBar(
                                    navController = navController,
                                    pagerState = pagerState
                                )
                            }
                        }
                    } else {
                        OnboardingScreen(
                            onFinished = {
                                onboardingCompletedOverride.value = true
                            }
                        )
                    }

                    if (showUpdateDialog && latestRelease != null) {
                        UpdateDialog(
                            release = latestRelease!!,
                            onDismiss = { showUpdateDialog = false },
                            onConfirm = {
                                isDownloading = true
                                scope.launch {
                                    updateManager.downloadAndInstall(latestRelease!!) { progress ->
                                        downloadProgress = progress
                                    }.onSuccess {
                                        showUpdateDialog = false
                                        isDownloading = false
                                    }.onFailure {
                                        isDownloading = false
                                        // TODO: Show error
                                    }
                                }
                            },
                            isDownloading = isDownloading,
                            downloadProgress = downloadProgress
                        )
                    }
                }
            }
        }
    }
}