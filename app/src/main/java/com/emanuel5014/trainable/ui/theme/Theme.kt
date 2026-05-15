package com.emanuel5014.trainable.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MonolithicColorScheme = darkColorScheme(
    primary = MonolithicPrimary,
    onPrimary = MonolithicOnPrimary,
    primaryContainer = MonolithicPrimary,
    secondaryContainer = MonolithicSecondaryContainer,
    onSecondaryContainer = MonolithicOnSecondaryContainer,
    tertiary = MonolithicTertiary,
    tertiaryContainer = MonolithicTertiaryContainer,
    onTertiaryContainer = MonolithicOnTertiaryContainer,
    background = MonolithicSurface,
    surface = MonolithicSurface,
    surfaceVariant = MonolithicSurfaceContainerLow,
    onSurface = MonolithicOnSurface,
    onSurfaceVariant = MonolithicOnSurfaceVariant,
    outlineVariant = MonolithicOutlineVariant,
    error = MonolithicError,
    onError = MonolithicOnError,
)

@Composable
fun GymTrackingTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    rememberResponsiveSize()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(context)
        }
        else -> MonolithicColorScheme
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = Shapes,
        content = content
    )
}