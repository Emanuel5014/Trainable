package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConnectionResult
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.components.GymButton
import org.jetbrains.compose.resources.stringResource

/** What the Welcome restore sheet needs from the host: the file restore and the Nextcloud connection and list. */
data class OnboardingRestore(
    val fromFile: () -> Unit,
    val form: NextcloudForm,
    val connection: NextcloudConnection,
    /** Shows the host's NextcloudRestoreDialog (the list of backups on the server). */
    val openNextcloudBackups: () -> Unit
)

/** First step: logo, one line, language, "Inizia" and "Ho già un backup". No back button or progress. */
@Composable
fun WelcomeStep(
    logo: @Composable (Modifier, Color) -> Unit,
    languageLabel: String,
    onPickLanguage: () -> Unit,
    onStart: () -> Unit,
    restore: OnboardingRestore,
    /** Nextcloud got connected from this sheet, only to restore: the setup keeps its automatic uploads off. */
    onConnectedForRestore: () -> Unit = {}
) {
    var showRestore by remember { mutableStateOf(false) }

    OnboardingPage(centered = true, horizontalAlignment = Alignment.CenterHorizontally) {
        OnboardingHeroShape(stepIndex = 0, active = false, done = false, modifier = Modifier.size(112.dp)) {
            logo(Modifier.size(56.dp), MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.onboarding_welcome_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                fontSize = OnboardingMetrics.welcomeTitleSize,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(Res.string.onboarding_welcome_line),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        AssistChip(
            onClick = onPickLanguage,
            label = { Text(languageLabel) },
            leadingIcon = { Icon(Icons.Rounded.Language, null, Modifier.size(18.dp)) }
        )
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GymButton(onClick = onStart, pressMorph = true, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.onboarding_start), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
            OutlinedButton(
                onClick = { showRestore = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Icon(Icons.Rounded.Restore, null, Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.onboarding_have_backup), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showRestore) RestoreSheet(restore, onConnectedForRestore, onDismiss = { showRestore = false })
}

/** "Da un file" or "Da Nextcloud"; the second swaps the sheet to the connection form, then to the backup list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestoreSheet(restore: OnboardingRestore, onConnectedForRestore: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nextcloud by remember { mutableStateOf(false) }
    LaunchedEffect(nextcloud, restore.connection.state) {
        if (nextcloud && restore.connection.state is NextcloudConnectionResult.Success) onConnectedForRestore()
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(if (nextcloud) Res.string.onboarding_restore_nextcloud else Res.string.onboarding_restore_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            if (!nextcloud) {
                JoinedColumn(
                    listOf(
                        { position ->
                            RestoreOption(position, Icons.Rounded.FolderOpen, Res.string.onboarding_restore_file, Res.string.onboarding_restore_file_desc) {
                                onDismiss()
                                restore.fromFile()
                            }
                        },
                        { position ->
                            RestoreOption(position, Icons.Rounded.Cloud, Res.string.onboarding_restore_nextcloud, Res.string.onboarding_restore_nextcloud_desc) {
                                nextcloud = true
                            }
                        }
                    )
                )
            } else if (restore.connection.connectedAs == null || restore.connection.state is NextcloudConnectionResult.Error) {
                NextcloudConnectFields(restore.form, restore.connection)
            } else {
                Text(restore.connection.connectedAs.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                GymButton(onClick = { onDismiss(); restore.openNextcloudBackups() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.onboarding_restore_pick), fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RestoreOption(
    position: JoinedPosition,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: org.jetbrains.compose.resources.StringResource,
    desc: org.jetbrains.compose.resources.StringResource,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clip(joinedShape(position)).clickable(role = Role.Button, onClick = onClick),
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(stringResource(title), fontWeight = FontWeight.Bold) },
        supportingContent = { Text(stringResource(desc)) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}
