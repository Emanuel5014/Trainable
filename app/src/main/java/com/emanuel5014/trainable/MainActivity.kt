package com.emanuel5014.trainable

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.emanuel5014.trainable.data.remote.GitHubRelease
import com.emanuel5014.trainable.data.remote.dto.WorkoutPlanExportDto
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.ui.components.BottomNavBar
import com.emanuel5014.trainable.ui.components.BottomNavBarFlo
import com.emanuel5014.trainable.ui.components.ImportConfirmationDialog
import com.emanuel5014.trainable.ui.components.UpdateDialog
import com.emanuel5014.trainable.ui.navigation.MainNavGraph
import com.emanuel5014.trainable.ui.navigation.MainTabs
import com.emanuel5014.trainable.ui.navigation.WorkoutExecution
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingScreen
import com.emanuel5014.trainable.ui.theme.GymTrackingTheme
import com.emanuel5014.trainable.util.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var updateManager: UpdateManager

    @Inject
    lateinit var workoutRepository: WorkoutRepository

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

            var plansToImport by remember { mutableStateOf<List<WorkoutPlanExportDto>?>(null) }
            var jsonDataToImport by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                latestRelease = updateManager.checkForUpdates()
                if (latestRelease != null) {
                    showUpdateDialog = true
                }
            }

            // Handle Import Intent
            LaunchedEffect(intent?.data) {
                intent?.data?.let { uri ->
                    scope.launch {
                        try {
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                val jsonData = inputStream.bufferedReader().use { it.readText() }
                                val json = Json { ignoreUnknownKeys = true }
                                val plans = json.decodeFromString<List<WorkoutPlanExportDto>>(jsonData)
                                plansToImport = plans
                                jsonDataToImport = jsonData
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, getString(R.string.import_failed), Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    }
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

                    if (plansToImport != null) {
                        ImportConfirmationDialog(
                            plans = plansToImport!!,
                            onConfirm = {
                                scope.launch {
                                    try {
                                        jsonDataToImport?.let { 
                                            workoutRepository.importPlans(it)
                                            Toast.makeText(this@MainActivity, getString(R.string.import_successful), Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, getString(R.string.import_failed), Toast.LENGTH_LONG).show()
                                    } finally {
                                        plansToImport = null
                                        jsonDataToImport = null
                                        intent?.data = null
                                    }
                                }
                            },
                            onDismiss = {
                                plansToImport = null
                                jsonDataToImport = null
                                intent?.data = null
                            }
                        )
                    }
                }
            }
        }
    }
}
