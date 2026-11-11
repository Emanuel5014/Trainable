package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.emanuel5014.trainable.sharedui.resources.*
import org.jetbrains.compose.resources.stringResource

/** "Tutto pronto": the filled sun with its check and a summary; each row jumps back to its step. */
@Composable
fun ReadyStep(
    choices: OnboardingChoices,
    themeLabel: String,
    onJumpTo: (OnboardingStep) -> Unit,
    hero: @Composable () -> Unit
) {
    val name by choices.username
    val style by choices.style
    val localOn by choices.autoBackupEnabled
    val localDays by choices.autoBackupFrequency

    OnboardingPage(underTopRow = true) {
        hero()
        OnboardingTitle(
            if (name.isBlank()) stringResource(Res.string.onboarding_ready_title_no_name)
            else stringResource(Res.string.onboarding_ready_title, name.trim())
        )
        JoinedColumn(
            listOf(
                summary(Res.string.onboarding_ready_style, { stringResource(style.nameRes()) }) { onJumpTo(OnboardingStep.Style) },
                summary(Res.string.onboarding_ready_features, { choices.activeFeatureCount().toString() }) { onJumpTo(OnboardingStep.Tweaks) },
                summary(Res.string.onboarding_ready_theme, { themeLabel }) { onJumpTo(OnboardingStep.Look) },
                summary(Res.string.onboarding_ready_backup, {
                    if (localOn) stringResource(Res.string.backup_enabled, localDays) else stringResource(Res.string.onboarding_ready_backup_off)
                }) { onJumpTo(OnboardingStep.Data) }
            )
        )
    }
}

private fun summary(
    label: org.jetbrains.compose.resources.StringResource,
    value: @Composable () -> String,
    onClick: () -> Unit
): @Composable (JoinedPosition) -> Unit = { position ->
    JoinedRowSurface(position, Modifier.clickable(role = Role.Button, onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(value(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
