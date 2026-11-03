package com.emanuel5014.trainable.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.ComposeUIViewController
import com.emanuel5014.trainable.data.local.GymDatabaseInstance
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.createSettingsDataStore
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.google_sans_flex
import com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming
import com.emanuel5014.trainable.ui.components.LocalHapticsSetting
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardContent
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardData
import com.emanuel5014.trainable.ui.screens.dashboard.DashboardState
import com.emanuel5014.trainable.ui.theme.MonochromeDarkColorScheme
import com.emanuel5014.trainable.ui.theme.MonochromeLightColorScheme
import com.emanuel5014.trainable.ui.theme.MonolithicDarkColorScheme
import com.emanuel5014.trainable.ui.theme.MonolithicLightColorScheme
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.rememberResponsiveSize
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.Font
import platform.UIKit.UIViewController

/** The iOS app's root, shown by the Xcode project. For now it is the Dashboard on the device's own data. */
fun MainViewController(): UIViewController = ComposeUIViewController { TrainableIosApp() }

/** One database and one settings file for the whole process. */
private object IosAppGraph {
    val database by lazy { GymDatabaseInstance.getDatabase() }
    val preferences by lazy { UserPreferencesRepository(createSettingsDataStore()) }
}

@Composable
private fun TrainableIosApp() {
    val preferences = IosAppGraph.preferences
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
    val uiState by state.uiState.collectAsState()
    val themeMode by preferences.themeMode.collectAsState(initial = null)
    val advancedProgramming by preferences.advancedProgrammingEnabled.collectAsState(initial = false)
    // Same choice as the Android app: dark unless the user picked light or the system's
    val isDark = when (themeMode) {
        null -> true
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    IosTheme(paletteIndex = uiState.themePalette, darkTheme = isDark) {
        CompositionLocalProvider(
            LocalHapticsSetting provides preferences.hapticEnabled,
            LocalAdvancedProgramming provides advancedProgramming
        ) {
            DashboardContent(
                uiState = uiState,
                actions = state,
                onNavigateToSettings = {},
                onNavigateToWorkout = { _, _ -> },
                onNavigateToQuickWorkout = {},
                onNavigateToPhysicalChecks = {}
            )
        }
    }
}

/**
 * The theme of the Android app on iOS. The palettes built from a seed colour need the Android colour library, so on
 * iOS every palette but Monochrome shows as Monolithic for now; there is no dynamic colour (wallpaper) on iOS.
 */
@Composable
private fun IosTheme(paletteIndex: Int, darkTheme: Boolean, content: @Composable () -> Unit) {
    rememberResponsiveSize()
    val colorScheme = when {
        paletteIndex == 8 -> if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme
        else -> if (darkTheme) MonolithicDarkColorScheme else MonolithicLightColorScheme
    }
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
