package com.emanuel5014.trainable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.ComposeUIViewController
import com.emanuel5014.trainable.data.local.GymDatabaseInstance
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.repository.UserDefaultsStore
import com.emanuel5014.trainable.data.repository.ExerciseMergeRepository
import com.emanuel5014.trainable.ui.screens.analytics.AnalyticsState
import kotlinx.coroutines.flow.flowOf
import androidx.compose.ui.text.intl.Locale
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.PlanRepository
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.IosExerciseMediaFiles
import com.emanuel5014.trainable.data.repository.IosWorkoutPlatform
import com.emanuel5014.trainable.data.repository.IosTransactionRunner
import com.emanuel5014.trainable.data.repository.JournalRepository
import com.emanuel5014.trainable.data.repository.HistoryCsvImporter
import com.emanuel5014.trainable.ui.screens.history.HistoryState
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.domain.rpe.RpeSettings
import com.emanuel5014.trainable.ui.components.LocalPrescriptionEnvironment
import com.emanuel5014.trainable.ui.components.LocalPrescriptionText
import com.emanuel5014.trainable.ui.components.LocalRpeSettings
import com.emanuel5014.trainable.ui.components.PrescriptionEnvironment
import com.emanuel5014.trainable.ui.components.ProgramScanApplication
import com.emanuel5014.trainable.ui.screens.routines.RoutineDetailState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.createSettingsDataStore
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.google_sans_flex
import com.emanuel5014.trainable.ui.screens.settings.LocalPaletteColors
import com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming
import com.emanuel5014.trainable.ui.components.LocalHapticsSetting
import com.emanuel5014.trainable.ui.components.LocalNavBarStyle
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardData
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardState
import com.emanuel5014.trainable.ui.screens.routines.RoutinesState
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.rememberResponsiveSize
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.Font
import platform.UIKit.UIViewController

/** The iOS app's root, shown by the Xcode project: the app's navigation on the device's own data. */
fun MainViewController(): UIViewController = ComposeUIViewController { IosBackupStartup { TrainableIosApp() } }

/** One database and one settings file for the whole process, and the repositories over them. */
internal object IosAppGraph {
    val database by lazy { GymDatabaseInstance.getDatabase() }
    val settingsStore by lazy { createSettingsDataStore() }
    val preferences by lazy { UserPreferencesRepository(settingsStore) }
    val users by lazy { UserRepository(database.userDao(), database.weightLogDao()) }
    val oneRepMaxes by lazy { OneRepMaxRepository(database.oneRepMaxDao()) }
    val workouts by lazy {
        WorkoutRepository(
            database.workoutDao(), database.userDao(), database.exerciseDao(), database.oneRepMaxDao(), preferences,
            IosWorkoutPlatform()
        )
    }
    val exercises by lazy { ExerciseRepository(database.exerciseDao(), IosExerciseMediaFiles()) }
    val analyticsPreferences by lazy { UserDefaultsStore("analytics_prefs") }
    val merges by lazy { ExerciseMergeRepository(database.exerciseMergeDao(), database.exerciseDao(), analyticsPreferences) }
    val analytics by lazy { AnalyticsRepository(database.analyticsDao(), database.weightLogDao()) }
    val journal by lazy { JournalRepository(database.journalDao(), IosTransactionRunner(database)) }
    val historyImporter by lazy {
        HistoryCsvImporter(
            IosTransactionRunner(database), database.workoutDao(), database.exerciseDao(), database.userDao(), journal,
            IosWorkoutPlatform()
        )
    }

    val physicalChecks by lazy {
        com.emanuel5014.trainable.data.repository.PhysicalCheckRepository(
            database.physicalCheckDao(), com.emanuel5014.trainable.data.repository.IosPhysicalCheckCrypto(),
            com.emanuel5014.trainable.data.repository.IosPhysicalCheckFiles(), preferences
        )
    }

    fun routineDetail(planId: Int, scope: CoroutineScope) =
        RoutineDetailState(planId, workouts, exercises, preferences, oneRepMaxes, scope)
}

/** The advanced editor's environment on iOS: units, rounding and 1RMs of the device; no AI yet. */
private val IosPrescriptionEnvironment: @Composable () -> PrescriptionEnvironment = {
    val preferences = IosAppGraph.preferences
    val maxes = IosAppGraph.oneRepMaxes
    val scope = rememberCoroutineScope()
    val weightUnit by preferences.weightUnit.collectAsState(initial = "kg")
    val rounding by preferences.loadRoundingIncrement.collectAsState(initial = LoadCalculator.DEFAULT_INCREMENT_KG)
    val current by remember { maxes.currentByExercise().map { all -> all.mapValues { it.value.weightKg } } }
        .collectAsState(initial = emptyMap())
    val estimated by remember { maxes.estimatedByExercise() }.collectAsState(initial = emptyMap())
    object : PrescriptionEnvironment {
        override val weightUnit: String get() = weightUnit
        override val roundingIncrement: Float get() = rounding
        override val oneRepMaxes: Map<Int, Float> get() = current
        override val estimatedOneRepMaxes: Map<Int, Float> get() = estimated
        override val aiAvailable: Boolean get() = false

        override fun saveOneRepMax(exerciseId: Int, weightKg: Float, source: String) {
            scope.launch { maxes.add(exerciseId = exerciseId, weightKg = weightKg, source = source) }
        }

        @Composable
        override fun AiProgramScanButton(
            exerciseName: String?,
            singleWeek: Boolean,
            currentOneRepMaxKg: Float?,
            weightUnit: String,
            onApply: (ProgramScanApplication) -> Unit,
            modifier: Modifier,
            scanKey: String?
        ) {}
    }
}

@Composable
private fun TrainableIosApp() {
    val preferences = IosAppGraph.preferences
    IosNotificationEffects(preferences)
    val scope = rememberCoroutineScope()
    val state = remember {
        val db = IosAppGraph.database
        DashboardState(
            data = object : DashboardData {
                override fun activePlans(): Flow<List<WorkoutPlanEntity>> = db.workoutDao().getActivePlans()
                override fun allSessions(): Flow<List<WorkoutSessionEntity>> = db.workoutDao().getAllSessions()
                override fun unfinishedSessions(): Flow<List<SessionWithPlanName>> =
                    db.workoutDao().getUnfinishedSessionsWithPlanName()
                override fun cardioSessionCountSince(sinceTimestamp: Long): Flow<Int> =
                    db.workoutDao().getCardioSessionCountSince(sinceTimestamp)
                override suspend fun deleteSession(sessionId: Int) = db.workoutDao().deleteSession(sessionId)
            },
            analyticsRepository = AnalyticsRepository(db.analyticsDao(), db.weightLogDao()),
            userRepository = UserRepository(db.userDao(), db.weightLogDao()),
            userPrefsRepository = preferences,
            scope = scope
        )
    }
    val routines = remember {
        val db = IosAppGraph.database
        RoutinesState(PlanRepository(db.workoutDao(), db.userDao(), db.oneRepMaxDao()), preferences, scope)
    }
    val history = remember {
        HistoryState(IosAppGraph.workouts, IosAppGraph.exercises, preferences, IosAppGraph.journal, scope)
    }
    val analyticsText = rememberIosAnalyticsText()
    val language = Locale.current.language
    val analytics = remember(analyticsText, language) {
        AnalyticsState(IosAppGraph.analytics, IosAppGraph.workouts, preferences, IosAppGraph.merges,
            IosAppGraph.analyticsPreferences, analyticsText, flowOf(language), scope)
    }
    val uiState by state.uiState.collectAsState()
    val seedColor by preferences.dynamicColorSeed.collectAsState(initial = null)
    val themeStyle by preferences.themeStyle.collectAsState(initial = 0)
    val themeMode by preferences.themeMode.collectAsState(initial = null)
    val exerciseMediaEnabled by preferences.exerciseMediaEnabled.collectAsState(initial = false)
    val advancedProgramming by preferences.advancedProgrammingEnabled.collectAsState(initial = false)
    val navBarStyle by preferences.navBarStyle.collectAsState(initial = null)
    val rpeEnabled by preferences.rpeEnabled.collectAsState(initial = false)
    val rpeScale by preferences.rpeScale.collectAsState(initial = RpeScale.BASIC)
    val rpeSettings = remember(rpeEnabled, rpeScale) { RpeSettings(rpeEnabled, rpeScale) }
    // Same choice as the Android app: dark unless the user picked light or the system's
    val isDark = when (themeMode) {
        null -> true
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    IosTheme(paletteIndex = uiState.themePalette, seedColor = seedColor, themeStyle = themeStyle, darkTheme = isDark) {
        // As on Android, nothing until the navbar style is read, so the bar never swaps once drawn
        val style = navBarStyle
        if (style == null) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            return@IosTheme
        }
        CompositionLocalProvider(
            com.emanuel5014.trainable.ui.components.LocalExercisePlatformUi provides IosExercisePlatformUi,
            com.emanuel5014.trainable.ui.components.LocalExerciseAddMemory provides IosExerciseAddMemory,
            com.emanuel5014.trainable.ui.components.LocalExerciseMediaEnabled provides exerciseMediaEnabled,
            com.emanuel5014.trainable.ui.components.LocalShowMessage provides rememberIosShowMessage(),
            LocalPaletteColors provides IosPaletteColors,
            LocalHapticsSetting provides preferences.hapticEnabled,
            LocalAdvancedProgramming provides advancedProgramming,
            LocalNavBarStyle provides style,
            LocalRpeSettings provides rpeSettings,
            LocalPrescriptionText provides rememberIosPrescriptionText(),
            LocalPrescriptionEnvironment provides IosPrescriptionEnvironment
        ) {
            IosNavGraph(dashboard = state, dashboardUi = uiState, routines = routines, history = history, analytics = analytics, navBarStyle = style, isDark = isDark)
            IosBackupDialogs()
        }
    }
}

/** The selected fixed/custom palette on iOS; wallpaper colors remain an Android capability. */
@Composable
private fun IosTheme(paletteIndex: Int, seedColor: Int?, themeStyle: Int, darkTheme: Boolean, content: @Composable () -> Unit) {
    rememberResponsiveSize()
    val colorScheme = remember(paletteIndex, seedColor, themeStyle, darkTheme) {
        IosPaletteColors.colorScheme(paletteIndex, seedColor, themeStyle, darkTheme)
    }
    // The Live Activity card is always dark, so it takes the dark scheme's primary whatever the app's mode.
    val liveAccent = remember(paletteIndex, seedColor, themeStyle) {
        IosPaletteColors.colorScheme(paletteIndex, seedColor, themeStyle, true).primary.toArgb().toLong() and 0xFFFFFFFFL
    }
    SideEffect { IosLiveActivityStyle.accentArgb = liveAccent }
    MaterialTheme(colorScheme = colorScheme, typography = appTypography(), shapes = Shapes, content = content)
}

/** Google Sans Flex at the weights and grade of the Android app's typography (Type.kt). */
@Composable
private fun appTypography(): Typography {
    fun FontWeight.settings() = FontVariation.Settings(
        FontVariation.Setting("wght", weight.toFloat()),
        FontVariation.Setting("GRAD", 80f)
    )
    val weights = listOf(
        FontWeight.Thin, FontWeight.ExtraLight, FontWeight.Light, FontWeight.Normal, FontWeight.Medium,
        FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black
    )
    val family = FontFamily(weights.map { Font(Res.font.google_sans_flex, it, FontStyle.Normal, it.settings()) })
    return remember(family) {
        Typography().run {
            copy(
                displayLarge = displayLarge.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
                displayMedium = displayMedium.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
                displaySmall = displaySmall.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
                headlineLarge = headlineLarge.copy(fontFamily = family, fontWeight = FontWeight.Bold),
                headlineMedium = headlineMedium.copy(fontFamily = family, fontWeight = FontWeight.Bold),
                headlineSmall = headlineSmall.copy(fontFamily = family, fontWeight = FontWeight.Bold),
                titleLarge = titleLarge.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
                titleMedium = titleMedium.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
                titleSmall = titleSmall.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
                bodyLarge = bodyLarge.copy(fontFamily = family, fontWeight = FontWeight.Medium),
                bodyMedium = bodyMedium.copy(fontFamily = family, fontWeight = FontWeight.Medium),
                bodySmall = bodySmall.copy(fontFamily = family, fontWeight = FontWeight.Medium),
                labelLarge = labelLarge.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
                labelMedium = labelMedium.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
                labelSmall = labelSmall.copy(fontFamily = family, fontWeight = FontWeight.Medium),
            )
        }
    }
}

/** The remembered rest and types on iOS, in the same settings file as Android's. */
private val IosExerciseAddMemory: @Composable () -> com.emanuel5014.trainable.ui.components.ExerciseAddMemory = {
    val preferences = IosAppGraph.preferences
    val scope = rememberCoroutineScope()
    remember(scope) {
        object : com.emanuel5014.trainable.ui.components.ExerciseAddMemory {
            override val lastRestSeconds: kotlinx.coroutines.flow.StateFlow<Int?> = preferences.lastAddedExerciseRestSeconds
                .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)
            override fun rememberRest(seconds: Int) {
                scope.launch { preferences.setLastAddedExerciseRestSeconds(seconds) }
            }
            override val types: kotlinx.coroutines.flow.StateFlow<Map<Int, String>> = preferences.exerciseTypes
                .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyMap())
            override fun rememberType(exerciseId: Int, type: String) {
                scope.launch { preferences.rememberExerciseType(exerciseId, type) }
            }
        }
    }
}
