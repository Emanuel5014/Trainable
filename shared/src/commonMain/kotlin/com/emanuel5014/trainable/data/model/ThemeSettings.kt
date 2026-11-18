package com.emanuel5014.trainable.data.model

/** Where the app's colours come from, as picked in the Themes section. */
enum class ThemeSource(val id: Int) {
    /** The phone's wallpaper colours (Android 12+ only). */
    System(0),
    /** Colours found in a photo the user picked. */
    Photo(1),
    /** One of the fixed palettes (Monolithic, the seven seeds, Monochrome). */
    Preset(2),
    /** A colour the user built with the sliders or a hex code. */
    Custom(3);

    companion object {
        fun fromId(id: Int?): ThemeSource? = entries.firstOrNull { it.id == id }
    }
}

/** A custom theme kept by name in the Themes section, with its accents and background tint. */
data class SavedTheme(
    val name: String,
    val seed: Int,
    val style: Int,
    val secondaryHue: Int? = null,
    val tertiaryHue: Int? = null,
    val neutralTint: Int? = null
)

/** What actually paints the app once a [ThemeSettings] is read. */
sealed interface ColorSource {
    /** The platform's own wallpaper scheme; [style] 0 is the system scheme as it is. */
    data class SystemDynamic(val style: Int) : ColorSource
    data class Seed(
        val seed: Int,
        val style: Int,
        val secondaryHue: Int? = null,
        val tertiaryHue: Int? = null,
        val neutralTint: Int? = null
    ) : ColorSource
    data object Monolithic : ColorSource
    data object Monochrome : ColorSource
}

/**
 * Every theme preference in one value. [source] is null for a theme chosen before the Themes redesign: those
 * keep the old reading of [dynamicColor], [dynamicColorSeed], [palette] and [style] exactly, so nobody's colours
 * change until they pick something in the new section. With a [source], [dynamicColorSeed] is the wallpaper colour
 * picked under System (null = the system scheme), and the photo and custom seeds live in their own keys.
 */
data class ThemeSettings(
    val source: ThemeSource? = null,
    val dynamicColor: Boolean = true,
    val dynamicColorSeed: Int? = null,
    val palette: Int = 0,
    val style: Int = 0,
    val photoSeed: Int? = null,
    val photoColors: List<Int> = emptyList(),
    val customSeed: Int? = null,
    val secondaryHue: Int? = null,
    val tertiaryHue: Int? = null,
    val neutralTint: Int? = null,
    /** 0 standard, 1 medium, 2 high. */
    val contrast: Int = 0,
    val pureBlack: Boolean = false,
    val saved: List<SavedTheme> = emptyList()
) {
    val contrastLevel: Double get() = when (contrast) { 1 -> 0.5; 2 -> 1.0; else -> 0.0 }

    /** The colours to paint with. [systemAvailable] is false on iOS and on Android before 12. */
    fun colorSource(systemAvailable: Boolean): ColorSource = when (source) {
        null -> when {
            dynamicColorSeed != null -> ColorSource.Seed(dynamicColorSeed, style)
            dynamicColor && systemAvailable -> ColorSource.SystemDynamic(style)
            else -> presetSource(VIBRANT)
        }
        ThemeSource.System -> when {
            !systemAvailable -> presetSource(style)
            dynamicColorSeed != null -> ColorSource.Seed(dynamicColorSeed, style)
            else -> ColorSource.SystemDynamic(style)
        }
        ThemeSource.Photo -> photoSeed?.let { ColorSource.Seed(it, style) } ?: ColorSource.Monolithic
        ThemeSource.Preset -> presetSource(style)
        ThemeSource.Custom -> customSeed?.let { ColorSource.Seed(it, style, secondaryHue, tertiaryHue, neutralTint) }
            ?: ColorSource.Monolithic
    }

    private fun presetSource(presetStyle: Int): ColorSource = when (palette) {
        in 1..PALETTE_SEEDS.size -> ColorSource.Seed(PALETTE_SEEDS[palette - 1], presetStyle)
        MONOCHROME_PALETTE -> ColorSource.Monochrome
        else -> ColorSource.Monolithic
    }

    /**
     * The same theme said in the new section's terms, for a theme chosen before it: a seed that is one of
     * [wallpaperColors] stays under System, any other seed becomes a custom colour, a palette becomes a preset
     * in the Vibrant style the seeded palettes were always drawn with. Already-new settings come back unchanged.
     */
    fun migrated(systemAvailable: Boolean, wallpaperColors: List<Int>): ThemeSettings {
        if (source != null) return this
        return when {
            dynamicColorSeed != null && dynamicColor && systemAvailable && dynamicColorSeed in wallpaperColors ->
                copy(source = ThemeSource.System)
            dynamicColorSeed != null -> copy(source = ThemeSource.Custom, customSeed = dynamicColorSeed, dynamicColorSeed = null)
            dynamicColor && systemAvailable -> copy(source = ThemeSource.System)
            // Fixed palettes ignored the style: Vibrant, how the seeded ones were drawn, is where the others start
            else -> copy(source = ThemeSource.Preset, style = VIBRANT)
        }
    }

    /** A saved theme put back: its colour, style, accents and tint, as a custom colour. */
    fun applying(saved: SavedTheme): ThemeSettings = copy(
        source = ThemeSource.Custom, customSeed = saved.seed, style = saved.style,
        secondaryHue = saved.secondaryHue, tertiaryHue = saved.tertiaryHue, neutralTint = saved.neutralTint
    )

    /** True unless the app is in its own fixed Monolithic look; the dashboard logo takes the theme colour then. */
    fun tintsLogo(systemAvailable: Boolean): Boolean =
        if (source == null) dynamicColor || palette != 0
        else colorSource(systemAvailable) != ColorSource.Monolithic

    companion object {
        const val VIBRANT = 1
        const val STYLE_COUNT = 9
        const val MONOCHROME_PALETTE = 8
        /** Blue, green, red, purple, orange, pink, teal: palettes 1 to 7. */
        val PALETTE_SEEDS: List<Int> = listOf(
            0xFF4285F4.toInt(), 0xFF34A853.toInt(), 0xFFEA4335.toInt(), 0xFFA142F4.toInt(),
            0xFFFBBC05.toInt(), 0xFFE91E63.toInt(), 0xFF009688.toInt()
        )

        fun encodeColors(colors: List<Int>): String = colors.joinToString(",")
        fun decodeColors(text: String?): List<Int> =
            text?.split(',')?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()

        /**
         * One theme per line: seed, style, secondary hue, tertiary hue, tint (empty when automatic), then the name,
         * last so it may hold anything but a newline.
         */
        fun encodeSaved(themes: List<SavedTheme>): String = themes.joinToString("\n") {
            listOf(it.seed, it.style, it.secondaryHue, it.tertiaryHue, it.neutralTint).joinToString("\t") { v -> v?.toString().orEmpty() } +
                "\t" + it.name.replace('\n', ' ')
        }
        fun decodeSaved(text: String?): List<SavedTheme> =
            text?.lineSequence()?.mapNotNull { line ->
                val parts = line.split('\t', limit = 6)
                if (parts.size < 6) return@mapNotNull null
                val seed = parts[0].toIntOrNull() ?: return@mapNotNull null
                val style = parts[1].toIntOrNull() ?: return@mapNotNull null
                SavedTheme(parts[5], seed, style, parts[2].toIntOrNull(), parts[3].toIntOrNull(), parts[4].toIntOrNull())
            }?.toList() ?: emptyList()
    }
}
