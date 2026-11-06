package com.emanuel5014.trainable.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.platform.IosAppLocale
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.language
import com.emanuel5014.trainable.sharedui.resources.language_english
import com.emanuel5014.trainable.sharedui.resources.language_french
import com.emanuel5014.trainable.sharedui.resources.language_german
import com.emanuel5014.trainable.sharedui.resources.language_italian
import com.emanuel5014.trainable.sharedui.resources.language_portuguese
import com.emanuel5014.trainable.sharedui.resources.language_spanish
import com.emanuel5014.trainable.sharedui.resources.language_system_default
import com.emanuel5014.trainable.ui.screens.settings.LanguageOption
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val LOADING = "\u0000loading"

/**
 * Rebuilds the app in the chosen language. Android restarts its activity for this; here the whole tree is keyed on
 * the language, so every screen, model and string is created again (navigation starts over at the dashboard).
 * Until the preference has loaded, the language applied last time is kept (it survives relaunches).
 */
@Composable
internal fun IosLanguageRoot(content: @Composable () -> Unit) {
    val preference by IosAppGraph.preferences.userLanguage.collectAsState(initial = LOADING)
    val language = remember(preference) {
        if (preference != LOADING) IosAppLocale.apply(preference)
        IosAppLocale.language()
    }
    key(language) { content() }
}

/** The Settings row and dialog, the same as Android's; the change applies at once instead of restarting. */
@Composable
internal fun IosLanguageSetting() {
    val preferences = IosAppGraph.preferences
    val scope = rememberCoroutineScope()
    val current by preferences.userLanguage.collectAsState(initial = null)
    val selected = current ?: "system"
    var showDialog by remember { mutableStateOf(false) }
    val options = listOf(
        "system" to stringResource(Res.string.language_system_default),
        "en" to stringResource(Res.string.language_english),
        "it" to stringResource(Res.string.language_italian),
        "es" to stringResource(Res.string.language_spanish),
        "fr" to stringResource(Res.string.language_french),
        "de" to stringResource(Res.string.language_german),
        "pt" to stringResource(Res.string.language_portuguese)
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = SurfaceContainerHigh,
            title = { Text(stringResource(Res.string.language), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
            text = {
                Column {
                    options.forEach { (code, title) ->
                        LanguageOption(title = title, isSelected = selected == code, onClick = {
                            showDialog = false
                            scope.launch { preferences.setUserLanguage(code) }
                        })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.Language, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(stringResource(Res.string.language), style = MaterialTheme.typography.titleMedium, color = OnSurface,
                    fontWeight = FontWeight.ExtraBold)
                Text(options.firstOrNull { it.first == selected }?.second ?: options.first().second,
                    style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
    }
    HorizontalDivider(color = Surface.copy(alpha = 0.5f))
}
