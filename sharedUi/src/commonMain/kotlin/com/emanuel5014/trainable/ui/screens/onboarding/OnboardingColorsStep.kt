package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.data.model.ThemeSource
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.screens.settings.ThemeSourceContent
import com.emanuel5014.trainable.ui.screens.settings.ThemeStyleGrid
import com.emanuel5014.trainable.ui.screens.settings.customColorStart
import com.emanuel5014.trainable.ui.screens.settings.label
import com.emanuel5014.trainable.ui.screens.settings.themeSources
import com.emanuel5014.trainable.ui.screens.settings.withSource
import org.jetbrains.compose.resources.stringResource

/**
 * "I tuoi colori": where the colour comes from (wallpaper on Android 12+, a photo, a preset or a colour of one's own)
 * and the style, with the same pieces as Settings › Personalization. Accents, background tint and saved themes stay
 * in Settings; a line says so.
 */
@Composable
fun ColorsStep(model: OnboardingModel, systemAvailable: Boolean, wallpaperColors: List<Int>, hero: @Composable () -> Unit) {
    val stored by model.themeSettings.collectAsState(initial = null)
    val navBarStyle by model.navBarStyle.collectAsState(initial = NavBarStyle.Floating)

    OnboardingPage(underTopRow = true) {
        hero()
        OnboardingTitle(stringResource(Res.string.onboarding_colors_title))
        LookPreview(navBarStyle)

        val loaded = stored ?: return@OnboardingPage
        // A theme from before the Themes section is shown (and first written) in its terms, as in Settings
        val theme = remember(loaded, systemAvailable, wallpaperColors) { loaded.migrated(systemAvailable, wallpaperColors) }
        val customStart = customColorStart()

        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionLabel(stringResource(Res.string.theme_section_color))
                val sources = themeSources(systemAvailable)
                val names = sources.associateWith { stringResource(it.label()) }
                OptionGroup(
                    sources, theme.source ?: ThemeSource.Preset, { names.getValue(it) },
                    { model.setThemeSettings(theme.withSource(it, customStart)) }
                )
                ThemeSourceContent(theme, wallpaperColors, customStart, model::setThemeSettings, advanced = false)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionLabel(stringResource(Res.string.theme_section_style))
                ThemeStyleGrid(theme, systemAvailable, wallpaperColors, model::setThemeSettings)
            }

            Text(
                stringResource(Res.string.onboarding_colors_more),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
