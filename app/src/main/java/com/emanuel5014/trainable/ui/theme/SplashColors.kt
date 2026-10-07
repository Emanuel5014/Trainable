package com.emanuel5014.trainable.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import kotlinx.coroutines.flow.first

/** Surface and primary colour of the app theme, as the splash screen needs them. */
data class SplashColors(val background: Color, val logo: Color)

/**
 * The colours the app theme resolves to with the saved settings (mode, Material You, palette, seed, style).
 * The system splash screen is drawn before any of the app runs and can't know them, so the splash screen
 * overlay reads them here, ahead of the first frame.
 */
suspend fun loadSplashColors(context: Context): SplashColors {
    val prefs = context.dataStore.data.first()
    val isDark = when (prefs[UserPreferencesRepository.THEME_MODE] ?: 0) {
        1 -> false
        2 -> true
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }
    val scheme = getAppColorScheme(
        context = context,
        dynamicColor = prefs[UserPreferencesRepository.DYNAMIC_COLOR] ?: true,
        paletteIndex = prefs[UserPreferencesRepository.THEME_PALETTE] ?: 0,
        seedColor = prefs[UserPreferencesRepository.DYNAMIC_COLOR_SEED],
        themeStyle = prefs[UserPreferencesRepository.THEME_STYLE] ?: 0,
        isDark = isDark
    )
    return SplashColors(background = scheme.surface, logo = scheme.primary)
}
