package com.emanuel5014.trainable.widget.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.ui.theme.getAppColorScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** The theme the user picked in the app (Personalization), so the widgets look like Trainable and not like the system. */
data class WidgetTheme(val dynamicColor: Boolean, val seedColor: Int?, val palette: Int, val style: Int) {

    fun scheme(context: Context, isDark: Boolean): ColorScheme =
        getAppColorScheme(context, dynamicColor, palette, seedColor, style, isDark)

    fun colorProviders(context: Context): ColorProviders =
        ColorProviders(light = scheme(context, isDark = false), dark = scheme(context, isDark = true))
}

fun widgetThemeFlow(prefs: UserPreferencesRepository): Flow<WidgetTheme> = combine(
    prefs.dynamicColor, prefs.dynamicColorSeed, prefs.themePalette, prefs.themeStyle
) { dynamic, seed, palette, style -> WidgetTheme(dynamic, seed, palette, style) }.distinctUntilChanged()

fun Context.isNightMode(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
