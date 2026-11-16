package com.emanuel5014.trainable.widget.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import com.emanuel5014.trainable.data.model.ThemeSettings
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.ui.theme.getAppColorScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

/** The theme the user picked in the app (Personalization), so the widgets look like Trainable and not like the system. */
data class WidgetTheme(val theme: ThemeSettings) {

    fun scheme(context: Context, isDark: Boolean): ColorScheme = getAppColorScheme(context, theme, isDark)

    fun colorProviders(context: Context): ColorProviders =
        ColorProviders(light = scheme(context, isDark = false), dark = scheme(context, isDark = true))
}

fun widgetThemeFlow(prefs: UserPreferencesRepository): Flow<WidgetTheme> =
    prefs.themeSettings.map(::WidgetTheme).distinctUntilChanged()

fun Context.isNightMode(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
