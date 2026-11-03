package com.emanuel5014.trainable.ui.theme
import android.annotation.SuppressLint
import android.content.Context
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

private const val TAG = "TrainableTheme"


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
fun getAppColorScheme(
    context: Context,
    dynamicColor: Boolean,
    paletteIndex: Int,
    seedColor: Int?,
    themeStyle: Int,
    isDark: Boolean
): ColorScheme {
    return when {
        seedColor != null -> generateColorSchemeFromSeed(seedColor, themeStyle, isDark)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val baseScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (themeStyle == 0) baseScheme else generateColorSchemeFromSeed(baseScheme.primary.toArgb(), themeStyle, isDark)
        }
        else -> when (paletteIndex) {
            1 -> blueColorScheme(isDark)
            2 -> greenColorScheme(isDark)
            3 -> redColorScheme(isDark)
            4 -> purpleColorScheme(isDark)
            5 -> orangeColorScheme(isDark)
            6 -> pinkColorScheme(isDark)
            7 -> tealColorScheme(isDark)
            8 -> if (isDark) MonochromeDarkColorScheme else MonochromeLightColorScheme
            else -> if (isDark) MonolithicDarkColorScheme else MonolithicLightColorScheme
        }
    }
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
    val colorScheme = getAppColorScheme(
        context = context,
        dynamicColor = dynamicColor,
        paletteIndex = paletteIndex,
        seedColor = seedColor,
        themeStyle = themeStyle,
        isDark = darkTheme
    )

    // Save both dark and light color schemes for the web server
    SideEffect {
        val darkScheme = getAppColorScheme(context, dynamicColor, paletteIndex, seedColor, themeStyle, true)
        val lightScheme = getAppColorScheme(context, dynamicColor, paletteIndex, seedColor, themeStyle, false)

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