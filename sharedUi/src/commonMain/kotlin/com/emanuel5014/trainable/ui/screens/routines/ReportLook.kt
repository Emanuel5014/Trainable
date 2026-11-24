package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.emanuel5014.trainable.data.report.ReportPalette
import com.emanuel5014.trainable.data.report.ReportTheme
import com.emanuel5014.trainable.sharedui.resources.Res
import kotlin.io.encoding.Base64

/** The report drawn like the app: the user's theme in light and dark, and the app's font. */
object ReportLook {

    /**
     * The look of the report for the theme whose schemes are [light] and [dark]. The font is Google Sans Flex cut down
     * to Latin text with the app's axes (weight 100-900, grade 80), about 50 KB, so a shared file needs nothing else.
     */
    suspend fun theme(light: ColorScheme, dark: ColorScheme): ReportTheme =
        ReportTheme(light = palette(light), dark = palette(dark), fontWoff2Base64 = font())

    private suspend fun font(): String? = runCatching { Base64.encode(Res.readBytes("files/report_font.woff2")) }.getOrNull()

    fun palette(scheme: ColorScheme): ReportPalette = ReportPalette(
        surface = scheme.surface.hex(),
        surfaceContainerLow = scheme.surfaceContainerLow.hex(),
        surfaceContainer = scheme.surfaceContainer.hex(),
        surfaceContainerHigh = scheme.surfaceContainerHigh.hex(),
        surfaceContainerHighest = scheme.surfaceContainerHighest.hex(),
        onSurface = scheme.onSurface.hex(),
        onSurfaceVariant = scheme.onSurfaceVariant.hex(),
        outlineVariant = scheme.outlineVariant.hex(),
        primary = scheme.primary.hex(),
        onPrimary = scheme.onPrimary.hex(),
        primaryContainer = scheme.primaryContainer.hex(),
        onPrimaryContainer = scheme.onPrimaryContainer.hex(),
        secondaryContainer = scheme.secondaryContainer.hex(),
        onSecondaryContainer = scheme.onSecondaryContainer.hex(),
        tertiaryContainer = scheme.tertiaryContainer.hex(),
        onTertiaryContainer = scheme.onTertiaryContainer.hex(),
        errorContainer = scheme.errorContainer.hex(),
        onErrorContainer = scheme.onErrorContainer.hex()
    )

    /** "#3A59D1": the colour without its alpha. */
    private fun Color.hex(): String = "#" + (toArgb() and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
}
