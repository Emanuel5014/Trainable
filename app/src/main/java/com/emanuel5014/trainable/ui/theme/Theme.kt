package com.emanuel5014.trainable.ui.theme
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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


private val MonochromeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF3A3A3A),
    onPrimaryContainer = Color(0xFFE0E0E0),
    inversePrimary = Color(0xFF2D2D2D),
    secondary = Color(0xFFB0B0B0),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFD0D0D0),
    tertiary = Color(0xFF909090),
    onTertiary = Color(0xFF1A1A1A),
    tertiaryContainer = Color(0xFF404040),
    onTertiaryContainer = Color(0xFFB0B0B0),
    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF0E0E0E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFA0A0A0),
    surfaceTint = Color(0xFFE0E0E0),
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF0E0E0E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFB4AB),
    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF333333),
    scrim = Color.Black,
    surfaceBright = Color(0xFF2A2A2A),
    surfaceDim = Color(0xFF0E0E0E),
    surfaceContainer = Color(0xFF151515),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF242424),
    surfaceContainerLow = Color(0xFF111111),
    surfaceContainerLowest = Color(0xFF0A0A0A),
)

private val MonochromeLightColorScheme = lightColorScheme(
    primary = Color(0xFF333333),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6D6D6),
    onPrimaryContainer = Color(0xFF1A1A1A),
    inversePrimary = Color(0xFFCCCCCC),
    secondary = Color(0xFF666666),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF2D2D2D),
    tertiary = Color(0xFF999999),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEBEBEB),
    onTertiaryContainer = Color(0xFF404040),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF555555),
    surfaceTint = Color(0xFF333333),
    inverseSurface = Color(0xFF333333),
    inverseOnSurface = Color(0xFFF5F5F5),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF999999),
    outlineVariant = Color(0xFFCCCCCC),
    scrim = Color.Black,
    surfaceBright = Color(0xFFF5F5F5),
    surfaceDim = Color(0xFFD9D9D9),
    surfaceContainer = Color(0xFFEDEDED),
    surfaceContainerHigh = Color(0xFFE3E3E3),
    surfaceContainerHighest = Color(0xFFD9D9D9),
    surfaceContainerLow = Color(0xFFF0F0F0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

private val MonolithicDarkColorScheme = darkColorScheme(
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

private val MonolithicLightColorScheme = lightColorScheme(
    primary = MonolithicPrimary,
    onPrimary = MonolithicOnPrimary,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF00164F),
    inversePrimary = Color(0xFFBCC6FF),
    secondary = Color(0xFF5A5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDEE1F9),
    onSecondaryContainer = Color(0xFF171A2C),
    tertiary = Color(0xFF75546F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F5),
    onTertiaryContainer = Color(0xFF2C1229),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE2E0EC),
    onSurfaceVariant = Color(0xFF45454F),
    surfaceTint = MonolithicPrimary,
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2EFF6),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC6C4D0),
    scrim = Color.Black,
    surfaceBright = Color(0xFFFBF8FF),
    surfaceDim = Color(0xFFDBD9E0),
    surfaceContainer = Color(0xFFEFECF4),
    surfaceContainerHigh = Color(0xFFE9E7EE),
    surfaceContainerHighest = Color(0xFFE2E0E8),
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

@SuppressLint("RestrictedApi")
private fun createColorSchemeFromSeed(seed: Int, isDark: Boolean): ColorScheme {
    val hct = Hct.fromInt(seed)
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
}

private fun blueColorScheme(isDark: Boolean) = createColorSchemeFromSeed(0xFF4285F4.toInt(), isDark)
private fun greenColorScheme(isDark: Boolean) = createColorSchemeFromSeed(0xFF34A853.toInt(), isDark)
private fun redColorScheme(isDark: Boolean) = createColorSchemeFromSeed(0xFFEA4335.toInt(), isDark)
private fun purpleColorScheme(isDark: Boolean) = createColorSchemeFromSeed(0xFFA142F4.toInt(), isDark)
private fun orangeColorScheme(isDark: Boolean) = createColorSchemeFromSeed(0xFFFBBC05.toInt(), isDark)
private fun pinkColorScheme(isDark: Boolean) = createColorSchemeFromSeed(0xFFE91E63.toInt(), isDark)
private fun tealColorScheme(isDark: Boolean) = createColorSchemeFromSeed(0xFF009688.toInt(), isDark)

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
        8 -> Color(0xFFE0E0E0)
        else -> MonolithicPrimary
    }
}

@SuppressLint("RestrictedApi")
fun getPalettePreviewColors(index: Int, isDark: Boolean = true): List<Color> {
    if (index == 0) return listOf(MonolithicPrimary)
    
    if (index == 8) {
        return if (isDark) listOf(
            Color(0xFFE0E0E0),
            Color(0xFFB0B0B0),
            Color(0xFF909090),
            Color(0xFF555555)
        ) else listOf(
            Color(0xFF333333),
            Color(0xFF666666),
            Color(0xFF999999),
            Color(0xFFCCCCCC)
        )
    }
    
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
    val scheme = SchemeVibrant(hct, isDark, 0.0)
    val dc = MaterialDynamicColors()
    return listOf(Color(dc.primary().getArgb(scheme)))
}

@SuppressLint("RestrictedApi")
fun getSeedPreviewColors(seed: Int, style: Int = 0, isDark: Boolean = true): List<Color> {
    val hct = Hct.fromInt(seed)
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
private fun generateColorSchemeFromSeed(seedColor: Int, themeStyle: Int = 0, isDark: Boolean = true): androidx.compose.material3.ColorScheme {
    val hct = Hct.fromInt(seedColor)
    val scheme = when (themeStyle) {
        1 -> SchemeVibrant(hct, isDark, 0.0)
        2 -> SchemeExpressive(hct, isDark, 0.0)
        3 -> SchemeNeutral(hct, isDark, 0.0)
        4 -> SchemeFruitSalad(hct, isDark, 0.0)
        else -> SchemeTonalSpot(hct, isDark, 0.0)
    }
    
    val dc = MaterialDynamicColors()
    return androidx.compose.material3.ColorScheme(
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
}

@SuppressLint("RestrictedApi")
@Composable
fun GymTrackingTheme(
    dynamicColor: Boolean = true,
    paletteIndex: Int = 0,
    seedColor: Int? = null,
    themeStyle: Int = 0,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    rememberResponsiveSize()
    val context = LocalContext.current
    val colorScheme = when {
        seedColor != null -> generateColorSchemeFromSeed(seedColor, themeStyle, darkTheme)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (themeStyle == 0) baseScheme else generateColorSchemeFromSeed(baseScheme.primary.toArgb(), themeStyle, darkTheme)
        }
        else -> when (paletteIndex) {
            1 -> blueColorScheme(darkTheme)
            2 -> greenColorScheme(darkTheme)
            3 -> redColorScheme(darkTheme)
            4 -> purpleColorScheme(darkTheme)
            5 -> orangeColorScheme(darkTheme)
            6 -> pinkColorScheme(darkTheme)
            7 -> tealColorScheme(darkTheme)
            8 -> if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme
            else -> if (darkTheme) MonolithicDarkColorScheme else MonolithicLightColorScheme
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