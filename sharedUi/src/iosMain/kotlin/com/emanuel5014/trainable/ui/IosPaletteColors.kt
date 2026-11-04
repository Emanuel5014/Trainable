package com.emanuel5014.trainable.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import com.emanuel5014.trainable.ui.screens.settings.PaletteColors
import com.emanuel5014.trainable.ui.theme.*
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.hct.Hct
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.dynamiccolor.ColorSpec.SpecVersion
import com.materialkolor.scheme.SchemeVibrant
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeFruitSalad

/** The 2021 Material color rules used by Android's existing engine, applied by iOS only. */
internal object IosPaletteColors : PaletteColors {
    override fun palette(index: Int, isDark: Boolean): List<Color> {
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
        val scheme = legacyHueBoundary(SchemeVibrant(hct, isDark, 0.0, SpecVersion.SPEC_2021), hct, 1)
        val dc = MaterialDynamicColors()
        return listOf(Color(dc.primary.getArgb(scheme)))
    }

    override fun seed(seed: Int, style: Int, isDark: Boolean): List<Color> {
        val hct = Hct.fromInt(seed)
        val scheme = when (style) {
            1 -> SchemeVibrant(hct, isDark, 0.0, SpecVersion.SPEC_2021)
            2 -> SchemeExpressive(hct, isDark, 0.0, SpecVersion.SPEC_2021)
            3 -> SchemeNeutral(hct, isDark, 0.0, SpecVersion.SPEC_2021)
            4 -> SchemeFruitSalad(hct, isDark, 0.0, SpecVersion.SPEC_2021)
            else -> SchemeTonalSpot(hct, isDark, 0.0, SpecVersion.SPEC_2021)
        }

        val compatibleScheme = legacyHueBoundary(scheme, hct, style)
        val dynamicColors = MaterialDynamicColors()
        return listOf(
            Color(dynamicColors.primary.getArgb(compatibleScheme)),
            Color(dynamicColors.secondary.getArgb(compatibleScheme)),
            Color(dynamicColors.tertiary.getArgb(compatibleScheme)),
            Color(dynamicColors.surfaceVariant.getArgb(compatibleScheme))
        )
    }

    private fun generateColorSchemeFromSeed(seedColor: Int, themeStyle: Int = 0, isDark: Boolean = true): ColorScheme {
        val hct = Hct.fromInt(seedColor)
        val scheme = when (themeStyle) {
            1 -> SchemeVibrant(hct, isDark, 0.0, SpecVersion.SPEC_2021)
            2 -> SchemeExpressive(hct, isDark, 0.0, SpecVersion.SPEC_2021)
            3 -> SchemeNeutral(hct, isDark, 0.0, SpecVersion.SPEC_2021)
            4 -> SchemeFruitSalad(hct, isDark, 0.0, SpecVersion.SPEC_2021)
            else -> SchemeTonalSpot(hct, isDark, 0.0, SpecVersion.SPEC_2021)
        }

        val compatibleScheme = legacyHueBoundary(scheme, hct, themeStyle)
        val dc = MaterialDynamicColors()
        return ColorScheme(
            primary = Color(dc.primary.getArgb(compatibleScheme)),
            onPrimary = Color(dc.onPrimary.getArgb(compatibleScheme)),
            primaryContainer = Color(dc.primaryContainer.getArgb(compatibleScheme)),
            onPrimaryContainer = Color(dc.onPrimaryContainer.getArgb(compatibleScheme)),
            inversePrimary = Color(dc.inversePrimary.getArgb(compatibleScheme)),
            secondary = Color(dc.secondary.getArgb(compatibleScheme)),
            onSecondary = Color(dc.onSecondary.getArgb(compatibleScheme)),
            secondaryContainer = Color(dc.secondaryContainer.getArgb(compatibleScheme)),
            onSecondaryContainer = Color(dc.onSecondaryContainer.getArgb(compatibleScheme)),
            tertiary = Color(dc.tertiary.getArgb(compatibleScheme)),
            onTertiary = Color(dc.onTertiary.getArgb(compatibleScheme)),
            tertiaryContainer = Color(dc.tertiaryContainer.getArgb(compatibleScheme)),
            onTertiaryContainer = Color(dc.onTertiaryContainer.getArgb(compatibleScheme)),
            background = Color(dc.background.getArgb(compatibleScheme)),
            onBackground = Color(dc.onBackground.getArgb(compatibleScheme)),
            surface = Color(dc.surface.getArgb(compatibleScheme)),
            onSurface = Color(dc.onSurface.getArgb(compatibleScheme)),
            surfaceVariant = Color(dc.surfaceVariant.getArgb(compatibleScheme)),
            onSurfaceVariant = Color(dc.onSurfaceVariant.getArgb(compatibleScheme)),
            surfaceTint = Color(dc.primary.getArgb(compatibleScheme)),
            inverseSurface = Color(dc.inverseSurface.getArgb(compatibleScheme)),
            inverseOnSurface = Color(dc.inverseOnSurface.getArgb(compatibleScheme)),
            error = Color(dc.error.getArgb(compatibleScheme)),
            onError = Color(dc.onError.getArgb(compatibleScheme)),
            errorContainer = Color(dc.errorContainer.getArgb(compatibleScheme)),
            onErrorContainer = Color(dc.onErrorContainer.getArgb(compatibleScheme)),
            outline = Color(dc.outline.getArgb(compatibleScheme)),
            outlineVariant = Color(dc.outlineVariant.getArgb(compatibleScheme)),
            scrim = Color(dc.scrim.getArgb(compatibleScheme)),
            surfaceBright = Color(dc.surfaceBright.getArgb(compatibleScheme)),
            surfaceDim = Color(dc.surfaceDim.getArgb(compatibleScheme)),
            surfaceContainer = Color(dc.surfaceContainer.getArgb(compatibleScheme)),
            surfaceContainerHigh = Color(dc.surfaceContainerHigh.getArgb(compatibleScheme)),
            surfaceContainerHighest = Color(dc.surfaceContainerHighest.getArgb(compatibleScheme)),
            surfaceContainerLow = Color(dc.surfaceContainerLow.getArgb(compatibleScheme)),
            surfaceContainerLowest = Color(dc.surfaceContainerLowest.getArgb(compatibleScheme)),
            primaryFixed = Color(dc.primaryFixed.getArgb(compatibleScheme)),
            onPrimaryFixed = Color(dc.onPrimaryFixed.getArgb(compatibleScheme)),
            primaryFixedDim = Color(dc.primaryFixedDim.getArgb(compatibleScheme)),
            onPrimaryFixedVariant = Color(dc.onPrimaryFixedVariant.getArgb(compatibleScheme)),
            secondaryFixed = Color(dc.secondaryFixed.getArgb(compatibleScheme)),
            onSecondaryFixed = Color(dc.onSecondaryFixed.getArgb(compatibleScheme)),
            secondaryFixedDim = Color(dc.secondaryFixedDim.getArgb(compatibleScheme)),
            onSecondaryFixedVariant = Color(dc.onSecondaryFixedVariant.getArgb(compatibleScheme)),
            tertiaryFixed = Color(dc.tertiaryFixed.getArgb(compatibleScheme)),
            onTertiaryFixed = Color(dc.onTertiaryFixed.getArgb(compatibleScheme)),
            tertiaryFixedDim = Color(dc.tertiaryFixedDim.getArgb(compatibleScheme)),
            onTertiaryFixedVariant = Color(dc.onTertiaryFixedVariant.getArgb(compatibleScheme)),
        )
    }

    /** Match the legacy strict lower bound in Android's DynamicScheme.getRotatedHue for black seeds. */
    private fun legacyHueBoundary(scheme: DynamicScheme, source: Hct, style: Int): DynamicScheme {
        if (source.hue != 0.0 || style !in 1..2) return scheme
        return DynamicScheme(
            sourceColorHct = source, variant = scheme.variant, isDark = scheme.isDark,
            contrastLevel = scheme.contrastLevel, platform = scheme.platform, specVersion = SpecVersion.SPEC_2021,
            primaryPalette = scheme.primaryPalette,
            secondaryPalette = TonalPalette.fromHueAndChroma(source.hue, 24.0),
            tertiaryPalette = TonalPalette.fromHueAndChroma(source.hue, 32.0),
            neutralPalette = scheme.neutralPalette, neutralVariantPalette = scheme.neutralVariantPalette,
            errorPalette = scheme.errorPalette
        )
    }

    fun colorScheme(paletteIndex: Int, seedColor: Int?, style: Int, isDark: Boolean): ColorScheme {
        if (seedColor != null) return generateColorSchemeFromSeed(seedColor, style, isDark)
        val paletteSeed = when (paletteIndex) {
            1 -> 0xFF4285F4.toInt()
            2 -> 0xFF34A853.toInt()
            3 -> 0xFFEA4335.toInt()
            4 -> 0xFFA142F4.toInt()
            5 -> 0xFFFBBC05.toInt()
            6 -> 0xFFE91E63.toInt()
            7 -> 0xFF009688.toInt()
            else -> null
        }
        return when {
            paletteSeed != null -> generateColorSchemeFromSeed(paletteSeed, 1, isDark)
            paletteIndex == 8 -> if (isDark) MonochromeDarkColorScheme else MonochromeLightColorScheme
            else -> if (isDark) MonolithicDarkColorScheme else MonolithicLightColorScheme
        }
    }
}
