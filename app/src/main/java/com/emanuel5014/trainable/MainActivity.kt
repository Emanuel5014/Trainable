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
import com.emanuel5014.trainable.ui.components.BottomBarManager
import com.emanuel5014.trainable.ui.components.BottomNavBar
import com.emanuel5014.trainable.ui.components.BottomNavBarFlo
import com.emanuel5014.trainable.ui.components.ImportConfirmationDialog
import com.emanuel5014.trainable.ui.components.UpdateDialog
import com.emanuel5014.trainable.ui.navigation.MainNavGraph
import com.emanuel5014.trainable.ui.navigation.MainTabs
import com.emanuel5014.trainable.ui.navigation.WorkoutExecution
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingScreen
import com.emanuel5014.trainable.ui.theme.GymTrackingTheme
import com.emanuel5014.trainable.util.AppLocaleManager
import com.emanuel5014.trainable.util.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var workoutIntentState by mutableStateOf<android.content.Intent?>(null)

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var updateManager: UpdateManager

    @Inject
    lateinit var workoutRepository: WorkoutRepository

    @Inject
    lateinit var localeManager: AppLocaleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        workoutIntentState = intent

        // For Android < 13, apply the stored language before setContent
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            runBlocking {
                localeManager.applyStoredLanguage()
            }
        }

        enableEdgeToEdge()
        setContent {
            val userLanguage by userPreferencesRepository.userLanguage.collectAsState(initial = "system")
            val dynamicColor by userPreferencesRepository.dynamicColor.collectAsState(initial = true)
            val dynamicColorSeed by userPreferencesRepository.dynamicColorSeed.collectAsState(initial = null)
            val themePalette by userPreferencesRepository.themePalette.collectAsState(initial = 0)
            val themeStyle by userPreferencesRepository.themeStyle.collectAsState(initial = 0)
            val themeMode by userPreferencesRepository.themeMode.collectAsState(initial = null)

            val context = androidx.compose.ui.platform.LocalContext.current
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current

            val locale = remember(userLanguage) {
                if (userLanguage == null || userLanguage == "system") {
                    Locale.getDefault()
                } else {
                    Locale.forLanguageTag(userLanguage!!)
                }
            }

            LaunchedEffect(locale) {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                    configuration.setLocale(locale)
                    @Suppress("DEPRECATION")
                    context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
                }
            }

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
                    try {
                        val jsonData = withContext(Dispatchers.IO) {
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                inputStream.bufferedReader().use { it.readText() }
                            }
                        } ?: return@let
                        val json = Json { ignoreUnknownKeys = true }
                        val plans = json.decodeFromString<List<WorkoutPlanExportDto>>(jsonData)
                        plansToImport = plans
                        jsonDataToImport = jsonData
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, getString(R.string.import_failed), Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
                }
            }

            val isDark = when (themeMode) {
                null -> true
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            GymTrackingTheme(
                dynamicColor = dynamicColor,
                paletteIndex = themePalette,
                seedColor = dynamicColorSeed,
                themeStyle = themeStyle,
                darkTheme = isDark
            ) {
                val hasCompletedOnboarding by userPreferencesRepository.hasCompletedOnboarding.collectAsState(initial = null)
                val onboardingCompletedOverride = remember { mutableStateOf<Boolean?>(null) }
                val navController = rememberNavController()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentWorkoutIntent = workoutIntentState
                LaunchedEffect(currentWorkoutIntent, navBackStackEntry) {
                    if (currentWorkoutIntent != null && navBackStackEntry != null) {
                        val intent = currentWorkoutIntent
                        if (intent.hasExtra("workout_plan_id") || intent.hasExtra("workout_session_id") || intent.getBooleanExtra("quick_start", false)) {
                            val planId = intent.getIntExtra("workout_plan_id", -1).takeIf { id -> id != -1 }
                            val sessionId = intent.getIntExtra("workout_session_id", -1).takeIf { id -> id != -1 }
                            val quickStart = intent.getBooleanExtra("quick_start", false)
                            val workoutName = intent.getStringExtra("workout_name")

                            // Clear intent extras to prevent multiple navigation triggers
                            intent.removeExtra("workout_plan_id")
                            intent.removeExtra("workout_session_id")
                            intent.removeExtra("quick_start")
                            intent.removeExtra("workout_name")
                            workoutIntentState = null

                            navController.navigate(WorkoutExecution(
                                planId = planId,
                                sessionId = sessionId,
                                quickStart = quickStart,
                                workoutName = workoutName
                            ))
                        }
                    }
                }

                val currentDestination = navBackStackEntry?.destination

                val resolvedOnboardingState = onboardingCompletedOverride.value ?: hasCompletedOnboarding

                if (resolvedOnboardingState == null) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    return@GymTrackingTheme
                }

                val pagerState = rememberPagerState(pageCount = { 4 })

                val hazeState = rememberHazeState()

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
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(state = hazeState)
                        )

                        val showBottomBar = (currentDestination?.hasRoute(MainTabs::class) == true || 
                            currentDestination?.route?.startsWith("MainTabs") == true) && 
                            currentDestination.hasRoute(WorkoutExecution::class) == false &&
                            BottomBarManager.isVisibleOverride

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
                                    pagerState = pagerState,
                                    hazeState = hazeState,
                                    isDark = isDark
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        workoutIntentState = intent
    }

    override fun onResume() {
        super.onResume()
        com.emanuel5014.trainable.widget.TrainableWidget.update(this)
    }
}
