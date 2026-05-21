package com.emanuel5014.trainable.ui.theme
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
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
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeNeutral
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant


private val MonolithicColorScheme = darkColorScheme(
    primary = MonolithicPrimary,
    onPrimary = MonolithicOnPrimary,
    primaryContainer = MonolithicPrimary,
    onPrimaryContainer = MonolithicOnPrimary,
    inversePrimary = MonolithicPrimary,
    secondary = MonolithicOnSecondaryContainer,
    onSecondary = MonolithicSecondaryContainer,
    secondaryContainer = MonolithicSecondaryContainer,
    onSecondaryContainer = MonolithicOnSecondaryContainer,
    tertiary = MonolithicTertiary,
    onTertiary = MonolithicOnTertiary,
    tertiaryContainer = MonolithicTertiaryContainer,
    onTertiaryContainer = MonolithicOnTertiaryContainer,
    background = MonolithicSurface,
    onBackground = MonolithicOnSurface,
    surface = MonolithicSurface,
    onSurface = MonolithicOnSurface,
    surfaceVariant = MonolithicSurfaceContainerLow,
    onSurfaceVariant = MonolithicOnSurfaceVariant,
    surfaceTint = MonolithicPrimary,
    inverseSurface = MonolithicOnSurface,
    inverseOnSurface = MonolithicSurface,
    error = MonolithicError,
    onError = MonolithicOnError,
    errorContainer = MonolithicError,
    onErrorContainer = MonolithicOnError,
    outline = MonolithicOutlineVariant,
    outlineVariant = MonolithicOutlineVariant,
    scrim = Color.Black,
    surfaceBright = MonolithicSurfaceContainerHigh,
    surfaceDim = MonolithicSurface,
    surfaceContainer = MonolithicSurfaceContainer,
    surfaceContainerHigh = MonolithicSurfaceContainerHigh,
    surfaceContainerHighest = MonolithicSurfaceContainerHighest,
    surfaceContainerLow = MonolithicSurfaceContainerLow,
    surfaceContainerLowest = MonolithicSurface,
)

@SuppressLint("RestrictedApi")
private fun createColorSchemeFromSeed(seed: Int): ColorScheme {
    val hct = Hct.fromInt(seed)
    val isDark = true
    val scheme = SchemeVibrant(hct, isDark, 0.0)
    val dc = MaterialDynamicColors()
    
    return ColorScheme(
        primary = Color(dc.primary().getArgb(scheme)),
        onPrimary = Color(dc.onPrimary().getArgb(scheme)),
        primaryContainer = Color(dc.primaryContainer().getArgb(scheme)),
        onPrimaryContainer = Color(dc.onPrimaryContainer().getArgb(scheme)),
        inversePrimary = Color(dc.inversePrimary().getArgb(scheme)),
        secondary = Color(dc.secondary().getArgb(scheme)),
        onSecondary = Color(dc.onSecondary().getArgb(scheme)),
        secondaryContainer = Color(dc.secondaryContainer().getArgb(scheme)),
        onSecondaryContainer = Color(dc.onSecondaryContainer().getArgb(scheme)),
        tertiary = Color(dc.tertiary().getArgb(scheme)),
        onTertiary = Color(dc.onTertiary().getArgb(scheme)),
        tertiaryContainer = Color(dc.tertiaryContainer().getArgb(scheme)),
        onTertiaryContainer = Color(dc.onTertiaryContainer().getArgb(scheme)),
        background = MonolithicSurface,
        onBackground = MonolithicOnSurface,
        surface = MonolithicSurface,
        onSurface = MonolithicOnSurface,
        surfaceVariant = MonolithicSurfaceContainerLow,
        onSurfaceVariant = MonolithicOnSurfaceVariant,
        surfaceTint = Color(dc.primary().getArgb(scheme)),
        inverseSurface = Color(dc.inverseSurface().getArgb(scheme)),
        inverseOnSurface = Color(dc.inverseOnSurface().getArgb(scheme)),
        error = MonolithicError,
        onError = MonolithicOnError,
        errorContainer = Color(dc.errorContainer().getArgb(scheme)),
        onErrorContainer = Color(dc.onErrorContainer().getArgb(scheme)),
        outline = Color(dc.outline().getArgb(scheme)),
        outlineVariant = MonolithicOutlineVariant,
        scrim = Color(dc.scrim().getArgb(scheme)),
        surfaceBright = Color(dc.surfaceBright().getArgb(scheme)),
        surfaceDim = Color(dc.surfaceDim().getArgb(scheme)),
        surfaceContainer = Color(dc.surfaceContainer().getArgb(scheme)),
        surfaceContainerHigh = Color(dc.surfaceContainerHigh().getArgb(scheme)),
        surfaceContainerHighest = Color(dc.surfaceContainerHighest().getArgb(scheme)),
        surfaceContainerLow = Color(dc.surfaceContainerLow().getArgb(scheme)),
        surfaceContainerLowest = Color(dc.surfaceContainerLowest().getArgb(scheme)),
        primaryFixed = Color(dc.primaryFixed().getArgb(scheme)),
        onPrimaryFixed = Color(dc.onPrimaryFixed().getArgb(scheme)),
        primaryFixedDim = Color(dc.primaryFixedDim().getArgb(scheme)),
        onPrimaryFixedVariant = Color(dc.onPrimaryFixedVariant().getArgb(scheme)),
        secondaryFixed = Color(dc.secondaryFixed().getArgb(scheme)),
        onSecondaryFixed = Color(dc.onSecondaryFixed().getArgb(scheme)),
        secondaryFixedDim = Color(dc.secondaryFixedDim().getArgb(scheme)),
        onSecondaryFixedVariant = Color(dc.onSecondaryFixedVariant().getArgb(scheme)),
        tertiaryFixed = Color(dc.tertiaryFixed().getArgb(scheme)),
        onTertiaryFixed = Color(dc.onTertiaryFixed().getArgb(scheme)),
        tertiaryFixedDim = Color(dc.tertiaryFixedDim().getArgb(scheme)),
        onTertiaryFixedVariant = Color(dc.onTertiaryFixedVariant().getArgb(scheme)),
    )
}

private val BlueColorScheme = createColorSchemeFromSeed(0xFF4285F4.toInt())
private val GreenColorScheme = createColorSchemeFromSeed(0xFF34A853.toInt())
private val RedColorScheme = createColorSchemeFromSeed(0xFFEA4335.toInt())
private val PurpleColorScheme = createColorSchemeFromSeed(0xFFA142F4.toInt())
private val OrangeColorScheme = createColorSchemeFromSeed(0xFFFBBC05.toInt())
private val PinkColorScheme = createColorSchemeFromSeed(0xFFE91E63.toInt())
private val TealColorScheme = createColorSchemeFromSeed(0xFF009688.toInt())

fun getPaletteColor(index: Int): Color {
    return when (index) {
        0 -> MonolithicPrimary
        1 -> Color(0xFF4285F4)
        2 -> Color(0xFF34A853)
        3 -> Color(0xFFEA4335)
        4 -> Color(0xFFA142F4)
        5 -> Color(0xFFFBBC05)
        6 -> Color(0xFFE91E63)
        7 -> Color(0xFF009688)
        else -> MonolithicPrimary
    }
}

@SuppressLint("RestrictedApi")
fun getPalettePreviewColors(index: Int): List<Color> {
    if (index == 0) return listOf(MonolithicPrimary)
    
    val seed = when (index) {
        1 -> 0xFF4285F4.toInt()
        2 -> 0xFF34A853.toInt()
        3 -> 0xFFEA4335.toInt()
        4 -> 0xFFA142F4.toInt()
        5 -> 0xFFFBBC05.toInt()
        6 -> 0xFFE91E63.toInt()
        7 -> 0xFF009688.toInt()
        else -> return listOf(MonolithicPrimary)
    }
    
    val hct = Hct.fromInt(seed)
    val scheme = SchemeVibrant(hct, true, 0.0)
    val dc = MaterialDynamicColors()
    return listOf(Color(dc.primary().getArgb(scheme)))
}

@SuppressLint("RestrictedApi")
fun getSeedPreviewColors(seed: Int, style: Int = 0): List<Color> {
    val hct = Hct.fromInt(seed)
    val isDark = true
    val scheme = when (style) {
        1 -> SchemeVibrant(hct, isDark, 0.0)
        2 -> SchemeExpressive(hct, isDark, 0.0)
        3 -> SchemeNeutral(hct, isDark, 0.0)
        4 -> SchemeFruitSalad(hct, isDark, 0.0)
        else -> SchemeTonalSpot(hct, isDark, 0.0)
    }
    
    val dynamicColors = MaterialDynamicColors()
    return listOf(
        Color(dynamicColors.primary().getArgb(scheme)),
        Color(dynamicColors.secondary().getArgb(scheme)),
        Color(dynamicColors.tertiary().getArgb(scheme)),
        Color(dynamicColors.surfaceVariant().getArgb(scheme))
    )
}

@SuppressLint("RestrictedApi")
@Composable
fun GymTrackingTheme(
    dynamicColor: Boolean = true,
    paletteIndex: Int = 0,
    seedColor: Int? = null,
    themeStyle: Int = 0,
    content: @Composable () -> Unit
) {
    rememberResponsiveSize()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (seedColor != null) {
                val hct = Hct.fromInt(seedColor)
                val isDark = true
                val scheme = when (themeStyle) {
                    1 -> SchemeVibrant(hct, isDark, 0.0)
                    2 -> SchemeExpressive(hct, isDark, 0.0)
                    3 -> SchemeNeutral(hct, isDark, 0.0)
                    4 -> SchemeFruitSalad(hct, isDark, 0.0)
                    else -> SchemeTonalSpot(hct, isDark, 0.0)
                }
                
                val dc = MaterialDynamicColors()
                androidx.compose.material3.ColorScheme(
                    primary = Color(dc.primary().getArgb(scheme)),
                    onPrimary = Color(dc.onPrimary().getArgb(scheme)),
                    primaryContainer = Color(dc.primaryContainer().getArgb(scheme)),
                    onPrimaryContainer = Color(dc.onPrimaryContainer().getArgb(scheme)),
                    inversePrimary = Color(dc.inversePrimary().getArgb(scheme)),
                    secondary = Color(dc.secondary().getArgb(scheme)),
                    onSecondary = Color(dc.onSecondary().getArgb(scheme)),
                    secondaryContainer = Color(dc.secondaryContainer().getArgb(scheme)),
                    onSecondaryContainer = Color(dc.onSecondaryContainer().getArgb(scheme)),
                    tertiary = Color(dc.tertiary().getArgb(scheme)),
                    onTertiary = Color(dc.onTertiary().getArgb(scheme)),
                    tertiaryContainer = Color(dc.tertiaryContainer().getArgb(scheme)),
                    onTertiaryContainer = Color(dc.onTertiaryContainer().getArgb(scheme)),
                    background = Color(dc.background().getArgb(scheme)),
                    onBackground = Color(dc.onBackground().getArgb(scheme)),
                    surface = Color(dc.surface().getArgb(scheme)),
                    onSurface = Color(dc.onSurface().getArgb(scheme)),
                    surfaceVariant = Color(dc.surfaceVariant().getArgb(scheme)),
                    onSurfaceVariant = Color(dc.onSurfaceVariant().getArgb(scheme)),
                    surfaceTint = Color(dc.primary().getArgb(scheme)),
                    inverseSurface = Color(dc.inverseSurface().getArgb(scheme)),
                    inverseOnSurface = Color(dc.inverseOnSurface().getArgb(scheme)),
                    error = Color(dc.error().getArgb(scheme)),
                    onError = Color(dc.onError().getArgb(scheme)),
                    errorContainer = Color(dc.errorContainer().getArgb(scheme)),
                    onErrorContainer = Color(dc.onErrorContainer().getArgb(scheme)),
                    outline = Color(dc.outline().getArgb(scheme)),
                    outlineVariant = Color(dc.outlineVariant().getArgb(scheme)),
                    scrim = Color(dc.scrim().getArgb(scheme)),
                    surfaceBright = Color(dc.surfaceBright().getArgb(scheme)),
                    surfaceDim = Color(dc.surfaceDim().getArgb(scheme)),
                    surfaceContainer = Color(dc.surfaceContainer().getArgb(scheme)),
                    surfaceContainerHigh = Color(dc.surfaceContainerHigh().getArgb(scheme)),
                    surfaceContainerHighest = Color(dc.surfaceContainerHighest().getArgb(scheme)),
                    surfaceContainerLow = Color(dc.surfaceContainerLow().getArgb(scheme)),
                    surfaceContainerLowest = Color(dc.surfaceContainerLowest().getArgb(scheme)),
                    primaryFixed = Color(dc.primaryFixed().getArgb(scheme)),
                    onPrimaryFixed = Color(dc.onPrimaryFixed().getArgb(scheme)),
                    primaryFixedDim = Color(dc.primaryFixedDim().getArgb(scheme)),
                    onPrimaryFixedVariant = Color(dc.onPrimaryFixedVariant().getArgb(scheme)),
                    secondaryFixed = Color(dc.secondaryFixed().getArgb(scheme)),
                    onSecondaryFixed = Color(dc.onSecondaryFixed().getArgb(scheme)),
                    secondaryFixedDim = Color(dc.secondaryFixedDim().getArgb(scheme)),
                    onSecondaryFixedVariant = Color(dc.onSecondaryFixedVariant().getArgb(scheme)),
                    tertiaryFixed = Color(dc.tertiaryFixed().getArgb(scheme)),
                    onTertiaryFixed = Color(dc.onTertiaryFixed().getArgb(scheme)),
                    tertiaryFixedDim = Color(dc.tertiaryFixedDim().getArgb(scheme)),
                    onTertiaryFixedVariant = Color(dc.onTertiaryFixedVariant().getArgb(scheme)),
                )
            } else {
                dynamicDarkColorScheme(context)
            }
        }
        else -> when (paletteIndex) {
            1 -> BlueColorScheme
            2 -> GreenColorScheme
            3 -> RedColorScheme
            4 -> PurpleColorScheme
            5 -> OrangeColorScheme
            6 -> PinkColorScheme
            7 -> TealColorScheme
            else -> MonolithicColorScheme
        }
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