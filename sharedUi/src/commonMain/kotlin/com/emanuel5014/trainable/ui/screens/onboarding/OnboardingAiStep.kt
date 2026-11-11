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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ai.AiModelStatus
import com.emanuel5014.trainable.data.ai.AiModelVariant
import com.emanuel5014.trainable.data.ai.backend.ScanReadiness
import com.emanuel5014.trainable.ui.screens.settings.AiProvidersModel
import com.emanuel5014.trainable.ui.screens.settings.AiProvidersSection
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.components.GymButton
import org.jetbrains.compose.resources.stringResource

/** The on-device model as the platform has it: Android offers E2B and E4B, iOS only E2B. */
class OnboardingLocalAi(
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

/** Where the scan can run: the saved providers (cloud key or PC server) and, when the phone can, the on-device model. */
class OnboardingAi(
    val providers: AiProvidersModel,
    /** Null when this phone can't run the on-device model: only providers are offered. */
    val local: OnboardingLocalAi?,
    /** RAM the on-device model needs on this platform (Android 6 GB, iOS 8 GB), told to phones without it. */
    val localRequiredRamGb: Int
)

/** "Scansiona la scheda": the scan switch and, when on, the model to download or import. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiStep(choices: OnboardingChoices, ai: OnboardingAi, hero: @Composable () -> Unit) {
    var scan by choices.aiScanEnabled

    OnboardingPage(underTopRow = true) {
        hero()
        OnboardingTitle(stringResource(Res.string.onboarding_ai_scan_title))
        if (ai.local == null) LocalUnsupportedCard(ai.localRequiredRamGb)
        SwitchRow(
            position = JoinedPosition.Single,
            title = stringResource(Res.string.onboarding_ai_toggle_title),
            description = stringResource(Res.string.onboarding_ai_scan_desc),
            checked = scan,
            onCheckedChange = { scan = it },
            options = {
                val rows by ai.providers.rows.collectAsState()
                val activeId by ai.providers.activeId.collectAsState()
                // A profile that no longer exists leaves the phone as the place the scan runs
                val activeIsLocal = ScanReadiness.resolveActive(activeId, rows.map { it.profile }) == null
                AiProvidersSection(
                    rows = rows,
                    activeId = activeId,
                    activeIsLocal = activeIsLocal,
                    deviceSupported = ai.local != null,
                    localUnavailableText = Res.string.ai_device_unsupported,
                    viewModel = ai.providers,
                    showLocal = ai.local != null
                )
                val local = ai.local
                if (local != null && activeIsLocal) LocalModelBlock(local)
                Text(
                    stringResource(Res.string.ai_disclaimer_beta),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LocalModelBlock(ai: OnboardingLocalAi) {
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
}

/** Tells a phone without the RAM for the on-device model that scanning still works through a provider. */
@Composable
private fun LocalUnsupportedCard(requiredRamGb: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(Res.string.onboarding_ai_local_unsupported_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                stringResource(Res.string.onboarding_ai_local_unsupported_desc, requiredRamGb),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
