package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

private val languageCodes = listOf("system", "en", "it", "es", "fr", "de", "pt")

/** The name of a language code as the language picker shows it ("system" = follow the phone). */
@Composable
fun languageName(code: String?): String = when (code) {
    "en" -> stringResource(R.string.language_english)
    "it" -> stringResource(R.string.language_italian)
    "es" -> stringResource(R.string.language_spanish)
    "fr" -> stringResource(R.string.language_french)
    "de" -> stringResource(R.string.language_german)
    "pt" -> stringResource(R.string.language_portuguese)
    else -> stringResource(R.string.language_system_default)
}

/** The language picker of Settings, shared with the onboarding Welcome step. */
@Composable
fun LanguageDialog(currentLanguage: String?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = { Text(stringResource(R.string.language), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
        text = {
            Column {
                languageCodes.forEach { code ->
                    LanguageOption(
                        title = languageName(code),
                        isSelected = (currentLanguage ?: "system") == code,
                        onClick = { onPick(code) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}
