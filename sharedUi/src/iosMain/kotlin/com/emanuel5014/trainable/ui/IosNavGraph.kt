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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.sp
import platform.Foundation.NSBundle
import com.emanuel5014.trainable.ui.screens.settings.AboutSettingsSection
import com.emanuel5014.trainable.ui.screens.settings.DonorsContent
import com.emanuel5014.trainable.ui.navigation.Donors
import com.emanuel5014.trainable.ui.navigation.ImportHistory
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
import androidx.navigation.toRoute
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.emanuel5014.trainable.ui.screens.routines.RoutineDetailContent
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.ui.components.BottomBarManager
import com.emanuel5014.trainable.ui.components.BottomNavBar
import com.emanuel5014.trainable.ui.components.BottomNavBarExpressive
import com.emanuel5014.trainable.ui.components.BottomNavBarFlo
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.localizedNavItems
import com.emanuel5014.trainable.ui.navigation.MainTabs
import com.emanuel5014.trainable.ui.navigation.PhysicalCheck
import com.emanuel5014.trainable.ui.navigation.PhysicalCheckCompare
import com.emanuel5014.trainable.ui.navigation.PhysicalCheckSettings
import com.emanuel5014.trainable.ui.navigation.Report
import com.emanuel5014.trainable.ui.navigation.EditWorkoutSession
import com.emanuel5014.trainable.ui.navigation.CompareSessions
import com.emanuel5014.trainable.ui.screens.settings.PersonalizationSettingsContent
import com.emanuel5014.trainable.ui.screens.settings.NotificationSettingsContent
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.ai_settings_desc_ios
import com.emanuel5014.trainable.sharedui.resources.wallpaper_colors_unavailable
import com.emanuel5014.trainable.sharedui.resources.ai_local_not_on_ios
import com.emanuel5014.trainable.ui.screens.settings.AiProvidersModel
import com.emanuel5014.trainable.ui.screens.settings.AiSettingsContent
import com.emanuel5014.trainable.sharedui.resources.glass_part_overlays_desc_ios
import com.emanuel5014.trainable.ui.screens.settings.SettingsContent
import com.emanuel5014.trainable.ui.screens.settings.SettingsState
import com.emanuel5014.trainable.ui.screens.settings.WorkoutSettingsContent
import com.emanuel5014.trainable.ui.screens.settings.JournalTagsContent
import com.emanuel5014.trainable.ui.screens.settings.JournalTagsState
import com.emanuel5014.trainable.ui.screens.onerepmax.OneRepMaxContent
import com.emanuel5014.trainable.ui.screens.onerepmax.OneRepMaxState
import com.emanuel5014.trainable.ui.navigation.WorkoutSettings
import com.emanuel5014.trainable.ui.navigation.JournalTags
import com.emanuel5014.trainable.ui.navigation.Exercises
import com.emanuel5014.trainable.ui.navigation.AiSettings
import com.emanuel5014.trainable.ui.navigation.PersonalizationSettings
import com.emanuel5014.trainable.ui.navigation.NotificationSettings
import kotlinx.coroutines.flow.flowOf
import com.emanuel5014.trainable.ui.screens.analytics.AnalyticsContent
import com.emanuel5014.trainable.ui.screens.analytics.AnalyticsState
import com.emanuel5014.trainable.ui.navigation.OneRepMaxes
import com.emanuel5014.trainable.ui.screens.history.HistoryContent
import com.emanuel5014.trainable.ui.screens.history.HistoryState
import com.emanuel5014.trainable.ui.navigation.RoutineDetail
import com.emanuel5014.trainable.ui.navigation.Settings
import com.emanuel5014.trainable.ui.navigation.WorkoutExecution
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardContent
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardState
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardUiState
import com.emanuel5014.trainable.ui.screens.routines.RoutineListContent
import com.emanuel5014.trainable.ui.screens.routines.RoutinesState
import com.kyant.backdrop.backdrops.layerBackdrop
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
    history: HistoryState,
    analytics: AnalyticsState,
    navBarStyle: NavBarStyle,
    isDark: Boolean
) {
    val navController = rememberNavController()
    val uriHandler = LocalUriHandler.current
    // The app font has no emoji glyph on iOS; keep the original text accessible and draw its heart inline.
    val authorHeart = remember {
        InlineTextContent(Placeholder(12.sp, 12.sp, PlaceholderVerticalAlign.TextCenter)) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color(0xFFE53935),
                modifier = Modifier.fillMaxSize())
        }
    }
    val versionName = remember { NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "" }
    val pagerState = rememberPagerState(pageCount = { 4 })
    val hazeState = rememberHazeState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // As MainActivity: the floating bar turns to glass when the user wants glass on the navigation, and the
    // expressive navbar's action button bends the same copy of the screens
    val glassSettings = com.emanuel5014.trainable.ui.theme.LocalGlassSettings.current
    val glassNavBar = navBarStyle == NavBarStyle.Floating && glassSettings.isOn(com.emanuel5014.trainable.data.model.GlassPart.NavBar)
    val glassNavAction = navBarStyle == NavBarStyle.Expressive && glassSettings.isOn(com.emanuel5014.trainable.data.model.GlassPart.ActionButtons)
    val recordScreens = glassNavBar || glassNavAction
    val backgroundColor = MaterialTheme.colorScheme.background
    val glassBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(
            navController = navController,
            startDestination = MainTabs,
            modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)
                .then(if (recordScreens) Modifier.layerBackdrop(glassBackdrop) else Modifier),
            enterTransition = { fadeIn() + slideInHorizontally { it } },
            exitTransition = { fadeOut() + slideOutHorizontally { -it } },
            popEnterTransition = { fadeIn() + slideInHorizontally { -it } },
            popExitTransition = { fadeOut() + slideOutHorizontally { it } }
        ) {
            composable<MainTabs> {
                IosMainTabs(navController, pagerState, dashboard, dashboardUi, routines, history, analytics)
            }
            composable<OneRepMaxes> {
                val scope = rememberCoroutineScope()
                val language = com.emanuel5014.trainable.platform.IosAppLocale.language()
                val state = remember(language) { OneRepMaxState(IosAppGraph.oneRepMaxes, IosAppGraph.exercises,
                    IosAppGraph.preferences, flowOf(language), scope) }
                OneRepMaxContent(onNavigateBack = { navController.popBackStack() }, controller = state)
            }
            composable<Settings> {
                val scope = rememberCoroutineScope()
                val state = remember { SettingsState(IosAppGraph.users, IosAppGraph.preferences, scope) }
                SettingsContent(
                    state = state,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWorkoutSettings = { navController.navigate(WorkoutSettings) },
                    onNavigateToExercises = { navController.navigate(Exercises) },
                    onNavigateToAiSettings = { navController.navigate(AiSettings) },
                    onNavigateToPersonalizationSettings = { navController.navigate(PersonalizationSettings) },
                    onNavigateToNotificationSettings = { navController.navigate(NotificationSettings) },
                    languageUi = { IosLanguageSetting() },
                    aiDescription = Res.string.ai_settings_desc_ios,
                    platformSections = { IosDataSection(onNavigateToImportHistory = { navController.navigate(ImportHistory) }) },
                    aboutSection = {
                        AboutSettingsSection(
                            versionName = versionName,
                            authorHeart = authorHeart,
                            onOpenSource = { uriHandler.openUri("https://github.com/Emanuel5014/Trainable") },
                            onSupport = { uriHandler.openUri("https://ko-fi.com/emanuel5014") },
                            onNavigateToDonors = { navController.navigate(Donors) },
                            onAuthorEasterEgg = { uriHandler.openUri("https://www.youtube.com/watch?v=5jc_R9_im9w") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                )
            }
            composable<WorkoutSettings> {
                val scope = rememberCoroutineScope()
                val state = remember { SettingsState(IosAppGraph.users, IosAppGraph.preferences, scope) }
                WorkoutSettingsContent(onNavigateBack = { navController.popBackStack() },
                    onNavigateToOneRepMaxes = { navController.navigate(OneRepMaxes) },
                    onNavigateToJournalTags = { navController.navigate(JournalTags) }, state = state)
            }
            composable<JournalTags> {
                val scope = rememberCoroutineScope()
                val state = remember { JournalTagsState(IosAppGraph.journal, scope) }
                JournalTagsContent(onNavigateBack = { navController.popBackStack() }, state = state)
            }
            composable<Donors> {
                DonorsContent(
                    onNavigateBack = { navController.popBackStack() },
                    onSupport = { uriHandler.openUri("https://ko-fi.com/emanuel5014") }
                )
            }
            composable<Exercises> {
                val scope = rememberCoroutineScope()
                val language = com.emanuel5014.trainable.platform.IosAppLocale.language()
                val controller = remember(language) {
                    com.emanuel5014.trainable.ui.screens.exercises.ExercisesState(
                        IosAppGraph.exercises, IosAppGraph.merges, IosAppGraph.preferences, flowOf(language), scope)
                }
                com.emanuel5014.trainable.ui.screens.exercises.ExercisesContent(
                    onNavigateBack = { navController.popBackStack() }, controller = controller)
            }
            composable<ImportHistory> { IosHistoryImportScreen(onNavigateBack = { navController.popBackStack() }) }
            composable<AiSettings> {
                // Remote providers only: the on-device model is Android-only for now (A5)
                val scope = rememberCoroutineScope()
                val providers = remember { AiProvidersModel(IosAppGraph.aiProfiles, IosAppGraph.aiHttp, scope) }
                val scanEnabled by IosAppGraph.preferences.aiScanEnabled.collectAsState(initial = false)
                AiSettingsContent(
                    onNavigateBack = { navController.popBackStack() },
                    scanEnabled = scanEnabled,
                    onScanEnabledChange = { enabled -> scope.launch { IosAppGraph.preferences.setAiScanEnabled(enabled) } },
                    providers = providers,
                    deviceSupported = false,
                    localUnavailableText = Res.string.ai_local_not_on_ios
                )
            }
            composable<PersonalizationSettings> {
                val scope = rememberCoroutineScope()
                val state = remember { SettingsState(IosAppGraph.users, IosAppGraph.preferences, scope) }
                PersonalizationSettingsContent(onNavigateBack = { navController.popBackStack() }, state = state,
                    dynamicColorUnsupportedText = Res.string.wallpaper_colors_unavailable,
                    glassSupported = true,
                    // Every part, Overlays included (blur behind sheets and dialogs, IosOverlayGlass.kt)
                    glassParts = com.emanuel5014.trainable.data.model.GlassPart.entries.toSet(),
                    overlaysDescription = Res.string.glass_part_overlays_desc_ios)
            }
            composable<NotificationSettings> {
                val scope = rememberCoroutineScope()
                val state = remember { SettingsState(IosAppGraph.users, IosAppGraph.preferences, scope) }
                IosNotificationSettings(state, onNavigateBack = { navController.popBackStack() })
            }
            composable<WorkoutExecution> { entry ->
                val route = entry.toRoute<WorkoutExecution>()
                IosWorkoutScreen(entry, route.planId, route.sessionId, route.quickStart, route.workoutName,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRoutine = { id -> navController.navigate(RoutineDetail(id)) })
            }
            composable<PhysicalCheck> { entry ->
                IosPhysicalCheckScreen(entry) { model ->
                    com.emanuel5014.trainable.ui.screens.physicalcheck.PhysicalCheckContent(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSettings = { navController.navigate(PhysicalCheckSettings) },
                        onNavigateToCompare = { id1, id2 -> navController.navigate(PhysicalCheckCompare(id1 = id1, id2 = id2)) },
                        viewModel = model
                    )
                }
            }
            composable<PhysicalCheckSettings> { entry ->
                IosPhysicalCheckScreen(entry) { model ->
                    com.emanuel5014.trainable.ui.screens.physicalcheck.PhysicalCheckSettingsContent(
                        onNavigateBack = { navController.popBackStack() }, viewModel = model)
                }
            }
            composable<PhysicalCheckCompare> { entry ->
                val route = entry.toRoute<PhysicalCheckCompare>()
                IosPhysicalCheckScreen(entry) { model ->
                    com.emanuel5014.trainable.ui.screens.physicalcheck.PhysicalCheckCompareContent(
                        checkId1 = route.id1, checkId2 = route.id2,
                        onNavigateBack = { navController.popBackStack() }, viewModel = model)
                }
            }
            composable<RoutineDetail> { entry ->
                val planId = entry.toRoute<RoutineDetail>().planId
                val scope = rememberCoroutineScope()
                val detail = remember(planId) { IosAppGraph.routineDetail(planId, scope) }
                val language = com.emanuel5014.trainable.platform.IosAppLocale.language()
                RoutineDetailContent(
                    state = detail,
                    languageCode = language,
                    scan = rememberIosRoutineScanUi(planId, detail, language),
                    onNavigateBack = { navController.popBackStack() },
                    onStartWorkout = { id, sessionId -> navController.navigate(WorkoutExecution(planId = id, sessionId = sessionId)) }
                )
            }
            composable<Report> { entry ->
                IosReportScreen(entry.toRoute<Report>().planIdsString, onNavigateBack = { navController.popBackStack() })
            }
            composable<EditWorkoutSession> { entry ->
                val sessionId = entry.toRoute<EditWorkoutSession>().sessionId
                val scope = rememberCoroutineScope()
                val language = com.emanuel5014.trainable.platform.IosAppLocale.language()
                val model = remember(sessionId) {
                    com.emanuel5014.trainable.ui.screens.history.EditWorkoutModel(
                        sessionId, IosAppGraph.workouts, IosAppGraph.exercises, IosAppGraph.preferences,
                        IosAppGraph.oneRepMaxes, IosAppGraph.journal, flowOf(language), scope
                    )
                }
                com.emanuel5014.trainable.ui.screens.history.EditWorkoutContent(
                    onNavigateBack = { navController.popBackStack() }, model = model)
            }
            composable<CompareSessions> { entry ->
                val route = entry.toRoute<CompareSessions>()
                val scope = rememberCoroutineScope()
                val language = com.emanuel5014.trainable.platform.IosAppLocale.language()
                val model = remember {
                    com.emanuel5014.trainable.ui.screens.compare.CompareSessionsModel(
                        IosAppGraph.workouts, IosAppGraph.preferences, flowOf(language), scope)
                }
                com.emanuel5014.trainable.ui.screens.compare.CompareSessionsContent(
                    route.sessionId1, route.sessionId2, { navController.popBackStack() }, model)
            }
        }

        AnimatedVisibility(
            visible = navBackStackEntry?.destination?.hasRoute(MainTabs::class) == true && BottomBarManager.isVisibleOverride,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            when (navBarStyle) {
                NavBarStyle.Floating -> if (glassNavBar) {
                    com.emanuel5014.trainable.ui.components.BottomNavBarGlass(
                        navController = navController,
                        pagerState = pagerState,
                        backdrop = glassBackdrop,
                        isDark = isDark,
                        intensity = glassSettings.intensity
                    )
                } else {
                    BottomNavBarFlo(navController, pagerState, hazeState, isDark)
                }
                NavBarStyle.Expressive -> androidx.compose.runtime.CompositionLocalProvider(
                    com.emanuel5014.trainable.ui.theme.LocalAppGlassBackdrop provides (if (glassNavAction) glassBackdrop else null)
                ) { BottomNavBarExpressive(navController, pagerState) }
                else -> BottomNavBar(navController, pagerState)
            }
        }
    }
}

/** The same four shared tabs as MainPagerScreen on Android. */
@Composable
private fun IosMainTabs(
    navController: NavHostController,
    pagerState: PagerState,
    dashboard: DashboardState,
    dashboardUi: DashboardUiState,
    routines: RoutinesState,
    history: HistoryState,
    analytics: AnalyticsState
) {
    val hapticEnabled by IosAppGraph.preferences.hapticEnabled.collectAsState(initial = true)
    val physicalCheckBiometric by IosAppGraph.preferences.physicalCheckBiometricEnabled.collectAsState(initial = false)
    val biometricPrompt = rememberIosBiometricPrompt()
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
                onNavigateToPhysicalChecks = {
                    // As on Android: the biometric/passcode check comes before the section opens
                    if (physicalCheckBiometric) biometricPrompt({ navController.navigate(PhysicalCheck) }, {})
                    else navController.navigate(PhysicalCheck)
                }
            )
            1 -> RoutineListContent(
                state = routines,
                languageCode = com.emanuel5014.trainable.platform.IosAppLocale.language(),
                sharePlans = rememberIosRoutineSharing(),
                isVisible = pagerState.currentPage == 1,
                onNavigateToDetail = { planId -> navController.navigate(RoutineDetail(planId)) },
                onGenerateReport = { planIds -> navController.navigate(Report(planIds.joinToString(","))) }
            )
            2 -> HistoryContent(
                state = history,
                languageCode = com.emanuel5014.trainable.platform.IosAppLocale.language(),
                share = rememberIosHistoryShareUi(),
                onEditSession = { sessionId -> navController.navigate(EditWorkoutSession(sessionId)) },
                onCompareSessions = { first, second -> navController.navigate(CompareSessions(first, second)) },
                isVisible = pagerState.currentPage == 2
            )
            3 -> AnalyticsContent(
                state = analytics,
                hapticEnabled = hapticEnabled,
                oneRepMaxesEntry = { modifier ->
                    IosOneRepMaxesEntry(modifier = modifier, onClick = { navController.navigate(OneRepMaxes) })
                }
            )
        }
    }
}
