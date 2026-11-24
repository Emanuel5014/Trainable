package com.emanuel5014.trainable.data.report

/**
 * The look the report takes from the app: the colours of the user's theme in light and dark (the page follows the
 * system of whoever opens it) and the app's font, embedded so a shared file reads the same everywhere.
 */
data class ReportTheme(
    val light: ReportPalette = ReportPalette.DEFAULT_LIGHT,
    val dark: ReportPalette = ReportPalette.DEFAULT_DARK,
    /** The app's font as a WOFF2 file in base64; null leaves the page on the system font. */
    val fontWoff2Base64: String? = null
)

/** The Material roles the page uses, each a colour as "#RRGGBB". */
data class ReportPalette(
    val surface: String,
    val surfaceContainerLow: String,
    val surfaceContainer: String,
    val surfaceContainerHigh: String,
    val surfaceContainerHighest: String,
    val onSurface: String,
    val onSurfaceVariant: String,
    val outlineVariant: String,
    val primary: String,
    val onPrimary: String,
    val primaryContainer: String,
    val onPrimaryContainer: String,
    val secondaryContainer: String,
    val onSecondaryContainer: String,
    val tertiaryContainer: String,
    val onTertiaryContainer: String,
    val errorContainer: String,
    val onErrorContainer: String
) {
    /** The palette as the page's CSS variables; a value that is not a colour falls back to the default one. */
    internal fun cssVariables(fallback: ReportPalette): String = listOf(
        "surface" to (surface to fallback.surface),
        "c-low" to (surfaceContainerLow to fallback.surfaceContainerLow),
        "c" to (surfaceContainer to fallback.surfaceContainer),
        "c-high" to (surfaceContainerHigh to fallback.surfaceContainerHigh),
        "c-highest" to (surfaceContainerHighest to fallback.surfaceContainerHighest),
        "on" to (onSurface to fallback.onSurface),
        "on-var" to (onSurfaceVariant to fallback.onSurfaceVariant),
        "outline" to (outlineVariant to fallback.outlineVariant),
        "primary" to (primary to fallback.primary),
        "on-primary" to (onPrimary to fallback.onPrimary),
        "p-cont" to (primaryContainer to fallback.primaryContainer),
        "on-p-cont" to (onPrimaryContainer to fallback.onPrimaryContainer),
        "s-cont" to (secondaryContainer to fallback.secondaryContainer),
        "on-s-cont" to (onSecondaryContainer to fallback.onSecondaryContainer),
        "t-cont" to (tertiaryContainer to fallback.tertiaryContainer),
        "on-t-cont" to (onTertiaryContainer to fallback.onTertiaryContainer),
        "e-cont" to (errorContainer to fallback.errorContainer),
        "on-e-cont" to (onErrorContainer to fallback.onErrorContainer)
    ).joinToString("") { (name, values) ->
        val (value, default) = values
        "--$name:${if (HEX.matches(value)) value else default};"
    }

    companion object {
        private val HEX = Regex("#[0-9A-Fa-f]{6}")

        /** The app's default blue theme. */
        val DEFAULT_LIGHT = ReportPalette(
            surface = "#FBF8FF", surfaceContainerLow = "#F5F2FA", surfaceContainer = "#EFEDF4",
            surfaceContainerHigh = "#E9E7EF", surfaceContainerHighest = "#E3E1E9",
            onSurface = "#1B1B21", onSurfaceVariant = "#46464F", outlineVariant = "#C6C5D0",
            primary = "#3A59D1", onPrimary = "#FFFFFF", primaryContainer = "#DDE1FF", onPrimaryContainer = "#0F2A8F",
            secondaryContainer = "#E1E0F9", onSecondaryContainer = "#45455B",
            tertiaryContainer = "#B6ECF4", onTertiaryContainer = "#004F58",
            errorContainer = "#FFDAD6", onErrorContainer = "#93000A"
        )
        val DEFAULT_DARK = ReportPalette(
            surface = "#0F0F12", surfaceContainerLow = "#16161A", surfaceContainer = "#1D1C21",
            surfaceContainerHigh = "#26252B", surfaceContainerHighest = "#302F36",
            onSurface = "#E6E1EA", onSurfaceVariant = "#ADA9B3", outlineVariant = "#47464E",
            primary = "#4F6CE6", onPrimary = "#FFFFFF", primaryContainer = "#24337A", onPrimaryContainer = "#DDE1FF",
            secondaryContainer = "#2A2734", onSecondaryContainer = "#CBC3DC",
            tertiaryContainer = "#1C4D54", onTertiaryContainer = "#A6EEF9",
            errorContainer = "#5C1A17", onErrorContainer = "#FFDAD6"
        )
    }
}
