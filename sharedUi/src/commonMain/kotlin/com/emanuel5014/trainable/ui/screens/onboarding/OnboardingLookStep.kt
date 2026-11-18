package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.sharedui.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * "Il tuo stile": light or dark (and pure black), contrast, navigation bar and Liquid Glass, written at once with a
 * live preview. The colours have their own step just before ([ColorsStep]).
 */
@Composable
fun LookStep(model: OnboardingModel, hero: @Composable () -> Unit) {
    val theme by model.themeSettings.collectAsState(initial = null)
    val themeMode by model.themeMode.collectAsState(initial = 0)
    val navBarStyle by model.navBarStyle.collectAsState(initial = NavBarStyle.Floating)
    val glass by model.liquidGlassEnabled.collectAsState(initial = false)

    OnboardingPage(underTopRow = true) {
        hero()
        OnboardingTitle(stringResource(Res.string.onboarding_look_title))
        LookPreview(navBarStyle)

        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionLabel(stringResource(Res.string.theme_mode))
                val light = stringResource(Res.string.theme_mode_light)
                val dark = stringResource(Res.string.theme_mode_dark)
                val system = stringResource(Res.string.theme_source_system)
                // Mode ids as stored: 0 system, 1 light, 2 dark
                OptionGroup(listOf(1, 2, 0), themeMode, { when (it) { 1 -> light; 2 -> dark; else -> system } }, { model.setThemeMode(it) })
            }

            theme?.let { current ->
                // Pure black only shows in the dark theme, so it sits under the mode it belongs to
                if (themeMode != 1) {
                    SwitchRow(
                        position = JoinedPosition.Single,
                        title = stringResource(Res.string.theme_pure_black),
                        description = stringResource(Res.string.theme_pure_black_desc),
                        checked = current.pureBlack,
                        onCheckedChange = { model.setThemeSettings(current.copy(pureBlack = it)) }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionLabel(stringResource(Res.string.theme_section_contrast))
                    val names = listOf(
                        stringResource(Res.string.theme_contrast_standard),
                        stringResource(Res.string.theme_contrast_medium),
                        stringResource(Res.string.theme_contrast_high)
                    )
                    OptionGroup(listOf(0, 1, 2), current.contrast, { names[it] }, { model.setThemeSettings(current.copy(contrast = it)) })
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionLabel(stringResource(Res.string.nav_bar_style))
                val names = mapOf(
                    NavBarStyle.Classic to stringResource(Res.string.nav_bar_style_classic),
                    NavBarStyle.Floating to stringResource(Res.string.nav_bar_style_floating),
                    NavBarStyle.Expressive to stringResource(Res.string.nav_bar_style_expressive)
                )
                OptionGroup(names.keys.toList(), navBarStyle, { names.getValue(it) }, { model.setNavBarStyle(it) })
            }

            SwitchRow(
                position = JoinedPosition.Single,
                title = stringResource(Res.string.liquid_glass),
                description = stringResource(Res.string.liquid_glass_desc),
                checked = glass,
                onCheckedChange = { model.setLiquidGlassEnabled(it) }
            )
        }
    }
}

/** A tiny dashboard drawn with the live theme: two cards and the chosen navigation bar. */
@Composable
internal fun LookPreview(style: NavBarStyle) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(16.dp)).background(scheme.primaryContainer))
            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(16.dp)).background(scheme.secondaryContainer))
        }
        Box(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(14.dp)).background(scheme.surfaceContainerHigh))
        Spacer(Modifier.height(4.dp))
        val dots: @Composable () -> Unit = {
            repeat(4) { i ->
                Box(Modifier.size(if (i == 0) 18.dp else 12.dp).clip(CircleShape).background(if (i == 0) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.5f)))
            }
        }
        when (style) {
            NavBarStyle.Classic -> Row(
                modifier = Modifier.fillMaxWidth().height(34.dp).background(scheme.surfaceContainer),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) { dots() }
            NavBarStyle.Floating -> Row(
                modifier = Modifier.padding(horizontal = 28.dp).fillMaxWidth().height(34.dp).clip(CircleShape).background(scheme.surfaceContainerHighest),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) { dots() }
            else -> Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f).height(34.dp).clip(CircleShape).background(scheme.surfaceContainerHighest),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) { dots() }
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(scheme.primaryContainer))
            }
        }
        Spacer(Modifier.width(1.dp))
    }
}
