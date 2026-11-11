package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ai.AiModelStatus
import com.emanuel5014.trainable.data.ai.AiModelVariant
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.components.GymButton
import org.jetbrains.compose.resources.stringResource

/** The on-device model as the platform has it: Android offers E2B and E4B, iOS only E2B. */
class OnboardingAi(
    val variants: List<AiModelVariant>,
    val selected: AiModelVariant,
    val onSelect: (AiModelVariant) -> Unit,
    val status: AiModelStatus,
    val onDownload: () -> Unit,
    val onCancel: () -> Unit,
    /** RAM the platform asks for (Android: the variant's own, iOS: 8 GB). */
    val requiredRamGb: (AiModelVariant) -> Int = { it.requiredRamGb },
    /** "Import a model you already have", the platform's own picker. */
    val importOption: @Composable () -> Unit
)

/** "Scansiona la scheda": the scan switch and, when on, the model to download or import. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiStep(choices: OnboardingChoices, ai: OnboardingAi, hero: @Composable () -> Unit) {
    var scan by choices.aiScanEnabled

    OnboardingPage(underTopRow = true) {
        hero()
        OnboardingTitle(stringResource(Res.string.onboarding_ai_title))
        SwitchRow(
            position = JoinedPosition.Single,
            title = stringResource(Res.string.onboarding_ai_toggle_title),
            description = stringResource(Res.string.onboarding_ai_toggle_desc),
            checked = scan,
            onCheckedChange = { scan = it },
            options = {
                OptionLabel(stringResource(Res.string.ai_model_variant))
                OptionGroup(ai.variants, ai.selected, { it.displayName }, ai.onSelect)
                Text(
                    stringResource(Res.string.ai_model_requirements, ai.selected.sizeLabel, ai.requiredRamGb(ai.selected)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                when (val status = ai.status) {
                    is AiModelStatus.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(
                                    if (status.importing) Res.string.ai_model_importing else Res.string.ai_model_downloading,
                                    (status.progress * 100).toInt()
                                ).replace("%%", "%"),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = ai.onCancel) {
                                Text(stringResource(Res.string.ai_download_cancel), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        LinearWavyProgressIndicator(progress = { status.progress }, modifier = Modifier.fillMaxWidth())
                    }
                    is AiModelStatus.Ready -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(Res.string.ai_model_ready), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    else -> {
                        GymButton(
                            onClick = ai.onDownload,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(Icons.Rounded.CloudDownload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.ai_model_download, ai.selected.displayName), fontWeight = FontWeight.Bold)
                        }
                        Text(
                            stringResource(Res.string.onboarding_ai_download_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ai.importOption()
                    }
                }
                Text(
                    stringResource(Res.string.ai_disclaimer_beta),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}
