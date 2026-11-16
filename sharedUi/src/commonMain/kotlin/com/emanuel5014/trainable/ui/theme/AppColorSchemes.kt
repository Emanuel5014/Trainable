package com.emanuel5014.trainable.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.emanuel5014.trainable.data.model.ColorSource
import com.emanuel5014.trainable.data.model.ThemeSettings
import com.materialkolor.dynamiccolor.ColorSpec.SpecVersion
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeFidelity
import com.materialkolor.scheme.SchemeFruitSalad
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeRainbow
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant

/**
 * The colour engine of both apps: the 2021 Material rules that Android's own Material library used before, so a
 * theme looks the same as it always did on either platform (checked against that library by
 * AppColorSchemesParityTest on Android and by the goldens of AppColorSchemesTest on iOS). Styles: 0 Tonal Spot,
 * 1 Vibrant, 2 Expressive, 3 Neutral, 4 Fruit Salad, 5 Content, 6 Fidelity, 7 Rainbow, 8 Monochrome.
 */
object AppColorSchemes {
    private val dc = MaterialDynamicColors()

    /** The app's scheme on a platform without wallpaper colours (iOS): every setting of [theme] applied. */
    fun scheme(theme: ThemeSettings, isDark: Boolean): ColorScheme =
        scheme(theme.colorSource(systemAvailable = false), isDark, theme.contrastLevel, theme.pureBlack)

    /** The scheme of any [source] but the platform's wallpaper one, which each platform paints itself. */
    fun scheme(source: ColorSource, isDark: Boolean, contrast: Double = 0.0, pureBlack: Boolean = false): ColorScheme {
        val scheme = when (source) {
            is ColorSource.Seed -> fromSeed(
                source.seed, source.style, isDark, contrast, source.secondaryHue, source.tertiaryHue, source.neutralTint
            )
            ColorSource.Monochrome -> if (isDark) MonochromeDarkColorScheme else MonochromeLightColorScheme
            ColorSource.Monolithic, is ColorSource.SystemDynamic ->
                if (isDark) MonolithicDarkColorScheme else MonolithicLightColorScheme
        }
        return if (isDark && pureBlack) withPureBlack(scheme) else scheme
    }

    fun fromSeed(
        seed: Int,
        style: Int,
        isDark: Boolean,
        contrast: Double = 0.0,
        secondaryHue: Int? = null,
        tertiaryHue: Int? = null,
        neutralTint: Int? = null
    ): ColorScheme = dynamicScheme(seed, style, isDark, contrast, secondaryHue, tertiaryHue, neutralTint).toColorScheme()

    /** Primary, secondary, tertiary and surface variant of a seed in a style: the dots of a style preview. */
    fun previews(seed: Int, style: Int, isDark: Boolean = true): List<Color> {
        val scheme = dynamicScheme(seed, style, isDark)
        return listOf(
            Color(dc.primary.getArgb(scheme)),
            Color(dc.secondary.getArgb(scheme)),
            Color(dc.tertiary.getArgb(scheme)),
            Color(dc.surfaceVariant.getArgb(scheme))
        )
    }

    /** The preview of a fixed palette: its primary only, or four greys for Monochrome. */
    fun palettePreview(index: Int, isDark: Boolean = true): List<Color> = when (index) {
        in 1..ThemeSettings.PALETTE_SEEDS.size ->
            listOf(Color(dc.primary.getArgb(dynamicScheme(ThemeSettings.PALETTE_SEEDS[index - 1], ThemeSettings.VIBRANT, isDark))))
        ThemeSettings.MONOCHROME_PALETTE -> if (isDark) {
            listOf(Color(0xFFE0E0E0), Color(0xFFB0B0B0), Color(0xFF909090), Color(0xFF555555))
        } else {
            listOf(Color(0xFF333333), Color(0xFF666666), Color(0xFF999999), Color(0xFFCCCCCC))
        }
        else -> listOf(MonolithicPrimary)
    }

    /** The dark scheme on true black for OLED screens; cards keep a step of their own above it. */
    fun withPureBlack(scheme: ColorScheme): ColorScheme = scheme.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = lerp(scheme.surfaceContainerLow, Color.Black, 0.5f)
    )

    private fun dynamicScheme(
        seed: Int,
        style: Int,
        isDark: Boolean,
        contrast: Double = 0.0,
        secondaryHue: Int? = null,
        tertiaryHue: Int? = null,
        neutralTint: Int? = null
    ): DynamicScheme {
        val hct = Hct.fromInt(seed)
        val spec = SpecVersion.SPEC_2021
        val base = when (style) {
            1 -> SchemeVibrant(hct, isDark, contrast, spec)
            2 -> SchemeExpressive(hct, isDark, contrast, spec)
            3 -> SchemeNeutral(hct, isDark, contrast, spec)
            4 -> SchemeFruitSalad(hct, isDark, contrast, spec)
            5 -> SchemeContent(hct, isDark, contrast, spec)
            6 -> SchemeFidelity(hct, isDark, contrast, spec)
            7 -> SchemeRainbow(hct, isDark, contrast, spec)
            8 -> SchemeMonochrome(hct, isDark, contrast, spec)
            else -> SchemeTonalSpot(hct, isDark, contrast, spec)
        }
        val scheme = legacyHueBoundary(base, hct, style)
        if (secondaryHue == null && tertiaryHue == null && neutralTint == null) return scheme
        return DynamicScheme(
            sourceColorHct = hct, variant = scheme.variant, isDark = isDark,
            contrastLevel = contrast, platform = scheme.platform, specVersion = spec,
            primaryPalette = scheme.primaryPalette,
            secondaryPalette = secondaryHue?.let { TonalPalette.fromHueAndChroma(it.toDouble(), 32.0) } ?: scheme.secondaryPalette,
            tertiaryPalette = tertiaryHue?.let { TonalPalette.fromHueAndChroma(it.toDouble(), 40.0) } ?: scheme.tertiaryPalette,
            neutralPalette = neutralTint?.let { TonalPalette.fromHueAndChroma(hct.hue, it.toDouble()) } ?: scheme.neutralPalette,
            neutralVariantPalette = neutralTint?.let { TonalPalette.fromHueAndChroma(hct.hue, it * 1.5 + 2.0) }
                ?: scheme.neutralVariantPalette,
            errorPalette = scheme.errorPalette
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

    private fun DynamicScheme.toColorScheme(): ColorScheme {
        val s = this
        fun c(color: com.materialkolor.dynamiccolor.DynamicColor) = Color(color.getArgb(s))
        return ColorScheme(
            primary = c(dc.primary),
            onPrimary = c(dc.onPrimary),
            primaryContainer = c(dc.primaryContainer),
            onPrimaryContainer = c(dc.onPrimaryContainer),
            inversePrimary = c(dc.inversePrimary),
            secondary = c(dc.secondary),
            onSecondary = c(dc.onSecondary),
            secondaryContainer = c(dc.secondaryContainer),
            onSecondaryContainer = c(dc.onSecondaryContainer),
            tertiary = c(dc.tertiary),
            onTertiary = c(dc.onTertiary),
            tertiaryContainer = c(dc.tertiaryContainer),
            onTertiaryContainer = c(dc.onTertiaryContainer),
            background = c(dc.background),
            onBackground = c(dc.onBackground),
            surface = c(dc.surface),
            onSurface = c(dc.onSurface),
            surfaceVariant = c(dc.surfaceVariant),
            onSurfaceVariant = c(dc.onSurfaceVariant),
            surfaceTint = c(dc.primary),
            inverseSurface = c(dc.inverseSurface),
            inverseOnSurface = c(dc.inverseOnSurface),
            error = c(dc.error),
            onError = c(dc.onError),
            errorContainer = c(dc.errorContainer),
            onErrorContainer = c(dc.onErrorContainer),
            outline = c(dc.outline),
            outlineVariant = c(dc.outlineVariant),
            scrim = c(dc.scrim),
            surfaceBright = c(dc.surfaceBright),
            surfaceDim = c(dc.surfaceDim),
            surfaceContainer = c(dc.surfaceContainer),
            surfaceContainerHigh = c(dc.surfaceContainerHigh),
            surfaceContainerHighest = c(dc.surfaceContainerHighest),
            surfaceContainerLow = c(dc.surfaceContainerLow),
            surfaceContainerLowest = c(dc.surfaceContainerLowest),
            primaryFixed = c(dc.primaryFixed),
            onPrimaryFixed = c(dc.onPrimaryFixed),
            primaryFixedDim = c(dc.primaryFixedDim),
            onPrimaryFixedVariant = c(dc.onPrimaryFixedVariant),
            secondaryFixed = c(dc.secondaryFixed),
            onSecondaryFixed = c(dc.onSecondaryFixed),
            secondaryFixedDim = c(dc.secondaryFixedDim),
            onSecondaryFixedVariant = c(dc.onSecondaryFixedVariant),
            tertiaryFixed = c(dc.tertiaryFixed),
            onTertiaryFixed = c(dc.onTertiaryFixed),
            tertiaryFixedDim = c(dc.tertiaryFixedDim),
            onTertiaryFixedVariant = c(dc.onTertiaryFixedVariant),
        )
    }
}
