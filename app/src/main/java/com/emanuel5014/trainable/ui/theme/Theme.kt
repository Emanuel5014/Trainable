package com.emanuel5014.trainable.ui.theme
import android.content.Context
import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.emanuel5014.trainable.data.model.ColorSource
import com.emanuel5014.trainable.data.model.ThemeSettings

/**
 * The app's colours for [theme]. Everything but the wallpaper scheme comes from the shared engine
 * ([AppColorSchemes]); the wallpaper scheme is Android's own, re-seeded from its primary for any other style.
 */
fun getAppColorScheme(context: Context, theme: ThemeSettings, isDark: Boolean): ColorScheme {
    val systemAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val contrast = theme.contrastLevel
    val pureBlack = isDark && theme.pureBlack
    return when (val source = theme.colorSource(systemAvailable)) {
        is ColorSource.SystemDynamic -> {
            val base = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            val scheme = if (source.style == 0 && contrast == 0.0) base
            else AppColorSchemes.fromSeed(base.primary.toArgb(), source.style, isDark, contrast)
            if (pureBlack) AppColorSchemes.withPureBlack(scheme) else scheme
        }
        else -> AppColorSchemes.scheme(source, isDark, contrast, pureBlack)
    }
}

@Composable
fun GymTrackingTheme(
    theme: ThemeSettings = ThemeSettings(),
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    rememberResponsiveSize()
    val context = LocalContext.current
    val colorScheme = remember(context, theme, darkTheme) { getAppColorScheme(context, theme, darkTheme) }

    // Save both dark and light color schemes for the web server
    SideEffect {
        val darkScheme = getAppColorScheme(context, theme, true)
        val lightScheme = getAppColorScheme(context, theme, false)

        fun Color.toHex(): String = "#${Integer.toHexString(toArgb()).padStart(8, '0').substring(2)}"

        ThemeColorStore.setColors(
            dark = mapOf(
                "primary" to darkScheme.primary.toHex(),
                "onPrimary" to darkScheme.onPrimary.toHex(),
                "primaryContainer" to darkScheme.primaryContainer.toHex(),
                "onPrimaryContainer" to darkScheme.onPrimaryContainer.toHex(),
                "secondary" to darkScheme.secondary.toHex(),
                "onSecondary" to darkScheme.onSecondary.toHex(),
                "secondaryContainer" to darkScheme.secondaryContainer.toHex(),
                "onSecondaryContainer" to darkScheme.onSecondaryContainer.toHex(),
                "tertiary" to darkScheme.tertiary.toHex(),
                "onTertiary" to darkScheme.onTertiary.toHex(),
                "tertiaryContainer" to darkScheme.tertiaryContainer.toHex(),
                "onTertiaryContainer" to darkScheme.onTertiaryContainer.toHex(),
                "surface" to darkScheme.surface.toHex(),
                "onSurface" to darkScheme.onSurface.toHex(),
                "surfaceVariant" to darkScheme.surfaceVariant.toHex(),
                "onSurfaceVariant" to darkScheme.onSurfaceVariant.toHex(),
                "surfaceContainer" to darkScheme.surfaceContainer.toHex(),
                "surfaceContainerHigh" to darkScheme.surfaceContainerHigh.toHex(),
                "surfaceContainerHighest" to darkScheme.surfaceContainerHighest.toHex(),
                "surfaceContainerLow" to darkScheme.surfaceContainerLow.toHex(),
                "outline" to darkScheme.outline.toHex(),
                "outlineVariant" to darkScheme.outlineVariant.toHex(),
                "error" to darkScheme.error.toHex(),
                "onError" to darkScheme.onError.toHex(),
                "errorContainer" to darkScheme.errorContainer.toHex(),
                "onErrorContainer" to darkScheme.onErrorContainer.toHex(),
                "background" to darkScheme.background.toHex(),
                "onBackground" to darkScheme.onBackground.toHex(),
            ),
            light = mapOf(
                "primary" to lightScheme.primary.toHex(),
                "onPrimary" to lightScheme.onPrimary.toHex(),
                "primaryContainer" to lightScheme.primaryContainer.toHex(),
                "onPrimaryContainer" to lightScheme.onPrimaryContainer.toHex(),
                "secondary" to lightScheme.secondary.toHex(),
                "onSecondary" to lightScheme.onSecondary.toHex(),
                "secondaryContainer" to lightScheme.secondaryContainer.toHex(),
                "onSecondaryContainer" to lightScheme.onSecondaryContainer.toHex(),
                "tertiary" to lightScheme.tertiary.toHex(),
                "onTertiary" to lightScheme.onTertiary.toHex(),
                "tertiaryContainer" to lightScheme.tertiaryContainer.toHex(),
                "onTertiaryContainer" to lightScheme.onTertiaryContainer.toHex(),
                "surface" to lightScheme.surface.toHex(),
                "onSurface" to lightScheme.onSurface.toHex(),
                "surfaceVariant" to lightScheme.surfaceVariant.toHex(),
                "onSurfaceVariant" to lightScheme.onSurfaceVariant.toHex(),
                "surfaceContainer" to lightScheme.surfaceContainer.toHex(),
                "surfaceContainerHigh" to lightScheme.surfaceContainerHigh.toHex(),
                "surfaceContainerHighest" to lightScheme.surfaceContainerHighest.toHex(),
                "surfaceContainerLow" to lightScheme.surfaceContainerLow.toHex(),
                "outline" to lightScheme.outline.toHex(),
                "outlineVariant" to lightScheme.outlineVariant.toHex(),
                "error" to lightScheme.error.toHex(),
                "onError" to lightScheme.onError.toHex(),
                "errorContainer" to lightScheme.errorContainer.toHex(),
                "onErrorContainer" to lightScheme.onErrorContainer.toHex(),
                "background" to lightScheme.background.toHex(),
                "onBackground" to lightScheme.onBackground.toHex(),
            )
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            run {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = Shapes,
        content = content
    )
}