package com.emanuel5014.trainable

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.SystemClock
import android.view.animation.AccelerateInterpolator
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.data.remote.GitHubRelease
import com.emanuel5014.trainable.data.remote.dto.TrainablePlanParser
import com.emanuel5014.trainable.data.remote.dto.WorkoutPlanExportDto
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.ui.components.BottomBarManager
import com.emanuel5014.trainable.ui.components.BottomNavBar
import com.emanuel5014.trainable.ui.components.BottomNavBarExpressive
import com.emanuel5014.trainable.ui.components.BottomNavBarFlo
import com.emanuel5014.trainable.ui.components.ImportConfirmationDialog
import com.emanuel5014.trainable.ui.components.UpdateDialog
import com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming
import com.emanuel5014.trainable.ui.components.LocalNavBarStyle
import com.emanuel5014.trainable.ui.navigation.MainNavGraph
import com.emanuel5014.trainable.ui.navigation.MainTabs
import com.emanuel5014.trainable.ui.navigation.WorkoutExecution
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingScreen
import com.emanuel5014.trainable.ui.theme.GymTrackingTheme
import com.emanuel5014.trainable.ui.theme.SplashColors
import com.emanuel5014.trainable.ui.theme.loadSplashColors
import com.emanuel5014.trainable.util.AppLocaleManager
import com.emanuel5014.trainable.util.UpdateManager
import com.emanuel5014.trainable.util.notification.TimerNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var workoutIntentState by mutableStateOf<android.content.Intent?>(null)

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var updateManager: UpdateManager

    @Inject
    lateinit var workoutRepository: WorkoutRepository

    @Inject
    lateinit var localeManager: AppLocaleManager

    @Inject
    lateinit var timerNotificationHelper: TimerNotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash screen is drawn before any of the app runs, so it can only use the system's own
        // colours. The colours of the app theme are resolved off the main thread meanwhile and the splash screen
        // takes them on its way out.
        lifecycleScope.launch(Dispatchers.Default) {
            splashColors = runCatching { loadSplashColors(applicationContext) }.getOrNull()
        }
        installSplashScreen().setOnExitAnimationListener { splash ->
            // Let the logo finish its animation (Android 12+), then fade the splash out over the app
            val iconAnimationLeft = (splash.iconAnimationDurationMillis -
                (SystemClock.uptimeMillis() - splash.iconAnimationStartMillis)).coerceIn(0L, 700L)

            fun fadeOut() {
                splash.view.animate()
                    .alpha(0f)
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(280L)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction { splash.remove() }
                    .start()
            }

            val colors = splashColors
            if (colors == null) {
                splash.view.postDelayed(::fadeOut, iconAnimationLeft)
            } else {
                val argb = ArgbEvaluator()
                val view = splash.view as ViewGroup
                val icon = splash.iconView
                val fromBackground = (view.background as? ColorDrawable)?.color ?: getColor(R.color.splash_background)

                // Android 12+ draws the icon on a surface of its own that can't be recoloured, so the logo in the
                // colour of the theme is laid over it and cross-faded in. The system draws the 288dp icon behind a
                // 192dp mask, hence the 1.5 around the centre of the icon view. Before Android 12 the icon is an
                // ImageView and is simply tinted.
                val themedLogo = if (icon is ImageView) null else ImageView(this).apply {
                    setImageResource(R.drawable.ic_splash_logo)
                    imageTintList = ColorStateList.valueOf(colors.logo.toArgb())
                    scaleType = ImageView.ScaleType.FIT_XY
                    alpha = 0f
                    val iconLocation = IntArray(2).also(icon::getLocationInWindow)
                    val viewLocation = IntArray(2).also(view::getLocationInWindow)
                    val width = (icon.width * 1.5f).toInt()
                    val height = (icon.height * 1.5f).toInt()
                    layoutParams = FrameLayout.LayoutParams(width, height)
                    translationX = (iconLocation[0] - viewLocation[0]) - (width - icon.width) / 2f
                    translationY = (iconLocation[1] - viewLocation[1]) - (height - icon.height) / 2f
                }
                themedLogo?.let(view::addView)

                ValueAnimator.ofFloat(0f, 1f).apply {
                    startDelay = iconAnimationLeft
                    duration = 250L
                    addUpdateListener {
                        val fraction = it.animatedFraction
                        view.setBackgroundColor(argb.evaluate(fraction, fromBackground, colors.background.toArgb()) as Int)
                        if (themedLogo != null) {
                            themedLogo.alpha = fraction
                            icon.alpha = 1f - fraction
                        } else if (icon is ImageView) {
                            icon.imageTintList = ColorStateList.valueOf(
                                argb.evaluate(fraction, getColor(R.color.splash_logo), colors.logo.toArgb()) as Int
                            )
                        }
                    }
                    doOnEnd { fadeOut() }
                    start()
                }
            }
        }
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
            val advancedProgramming by userPreferencesRepository.advancedProgrammingEnabled.collectAsState(initial = false)

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

            // Handle Import Intent (also triggered again via onNewIntent)
            val pendingImportIntent = workoutIntentState
            LaunchedEffect(pendingImportIntent?.data) {
                pendingImportIntent?.data?.let { uri ->
                    if (uri.scheme == "content" || uri.scheme == "file") {
                        scope.launch {
                            try {
                                contentResolver.openInputStream(uri)?.use { inputStream ->
                                    val jsonData = inputStream.bufferedReader().use { it.readText() }
                                    val plans = TrainablePlanParser.decode(jsonData)
                                    if (plans.isEmpty()) {
                                        Toast.makeText(this@MainActivity, getString(R.string.import_failed), Toast.LENGTH_LONG).show()
                                        workoutIntentState = null
                                    } else {
                                        plansToImport = plans
                                        jsonDataToImport = jsonData
                                    }
                                } ?: run {
                                    Toast.makeText(this@MainActivity, getString(R.string.import_failed), Toast.LENGTH_LONG).show()
                                    workoutIntentState = null
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, getString(R.string.import_failed), Toast.LENGTH_LONG).show()
                                workoutIntentState = null
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

            val isDark = when (themeMode) {
                null -> true
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            // Null until read, so the first frame never shows the wrong navbar
            val navBarStyle by userPreferencesRepository.navBarStyle.collectAsState(initial = null)

            GymTrackingTheme(
                dynamicColor = dynamicColor,
                paletteIndex = themePalette,
                seedColor = dynamicColorSeed,
                themeStyle = themeStyle,
                darkTheme = isDark
            ) {
              CompositionLocalProvider(
                  LocalAdvancedProgramming provides advancedProgramming,
                  LocalNavBarStyle provides (navBarStyle ?: NavBarStyle.Floating)
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

                if (resolvedOnboardingState == null || navBarStyle == null) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    return@CompositionLocalProvider
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

                        AnimatedVisibility(
                            visible = showBottomBar,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            when (navBarStyle) {
                                NavBarStyle.Floating -> BottomNavBarFlo(
                                    navController = navController,
                                    pagerState = pagerState,
                                    hazeState = hazeState,
                                    isDark = isDark
                                )
                                NavBarStyle.Expressive -> BottomNavBarExpressive(
                                    navController = navController,
                                    pagerState = pagerState
                                )
                                else -> BottomNavBar(
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
                                        val imported = jsonDataToImport?.let {
                                            workoutRepository.importPlans(it)
                                        } ?: 0
                                        if (imported > 0) {
                                            Toast.makeText(this@MainActivity, getString(R.string.import_successful), Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(this@MainActivity, getString(R.string.import_failed), Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, getString(R.string.import_failed), Toast.LENGTH_LONG).show()
                                    } finally {
                                        plansToImport = null
                                        jsonDataToImport = null
                                        intent?.data = null
                                        workoutIntentState = null
                                    }
                                }
                            },
                            onDismiss = {
                                plansToImport = null
                                jsonDataToImport = null
                                intent?.data = null
                                workoutIntentState = null
                            }
                        )
                    }
                }
              }
            }
        }
    }

    private companion object {
        /** Colours of the app theme for the splash screen; null until resolved (or if that failed). */
        @Volatile
        var splashColors: SplashColors? = null
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        workoutIntentState = intent
    }

    override fun onResume() {
        super.onResume()
        if (::timerNotificationHelper.isInitialized) {
            timerNotificationHelper.cancelCustomVibration()
        }
        com.emanuel5014.trainable.widget.TrainableWidget.update(this)
        com.emanuel5014.trainable.widget.WeeklyGoalWidget.update(this)
    }
}
