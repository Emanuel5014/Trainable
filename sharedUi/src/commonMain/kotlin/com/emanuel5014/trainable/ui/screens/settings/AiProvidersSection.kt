package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.ai_backend_local
import com.emanuel5014.trainable.sharedui.resources.ai_provider_add
import com.emanuel5014.trainable.sharedui.resources.ai_provider_cloud_subtitle
import com.emanuel5014.trainable.sharedui.resources.ai_provider_delete
import com.emanuel5014.trainable.sharedui.resources.ai_provider_delete_body
import com.emanuel5014.trainable.sharedui.resources.ai_provider_delete_title
import com.emanuel5014.trainable.sharedui.resources.ai_provider_edit
import com.emanuel5014.trainable.sharedui.resources.ai_provider_hint_weak
import com.emanuel5014.trainable.sharedui.resources.ai_provider_key_reenter
import com.emanuel5014.trainable.sharedui.resources.ai_provider_pc_subtitle
import com.emanuel5014.trainable.sharedui.resources.ai_where_title
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.delete
import com.emanuel5014.trainable.data.ai.remote.AiProfileList
import com.emanuel5014.trainable.data.ai.remote.HostKind
import com.emanuel5014.trainable.data.ai.remote.KeyState
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

private class EditorTarget(val row: ProviderRow?)

/** "Where the AI runs": the phone or one of the saved providers, plus adding, editing and deleting them. */
@Composable
internal fun AiProvidersSection(
    rows: List<ProviderRow>,
    activeId: String,
    activeIsLocal: Boolean,
    deviceSupported: Boolean,
    localUnavailableText: StringResource,
    viewModel: AiProvidersModel,
    /** False leaves only the providers (the onboarding on a phone that can't run the on-device model). */
    showLocal: Boolean = true
) {
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<ProviderRow?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(Res.string.ai_where_title), fontWeight = FontWeight.ExtraBold, color = OnSurface)

        if (showLocal) ChoiceRow(
            selected = activeIsLocal,
            enabled = deviceSupported,
            title = stringResource(Res.string.ai_backend_local),
            subtitle = if (deviceSupported) null else stringResource(localUnavailableText),
            subtitleColor = OnSurfaceVariant,
            onClick = { viewModel.select(AiProfileList.LOCAL) }
        )

        rows.forEach { row ->
            val subtitle: String
            val subtitleColor: Color
            if (row.keyState == KeyState.UNREADABLE) {
                subtitle = stringResource(Res.string.ai_provider_key_reenter)
                subtitleColor = Error
            } else {
                subtitle = stringResource(
                    if (row.kind == HostKind.PRIVATE) Res.string.ai_provider_pc_subtitle else Res.string.ai_provider_cloud_subtitle,
                    row.host.orEmpty()
                )
                subtitleColor = OnSurfaceVariant
            }
            ChoiceRow(
                selected = row.profile.id == activeId,
                enabled = true,
                title = row.profile.name,
                subtitle = subtitle,
                subtitleColor = subtitleColor,
                onClick = { viewModel.select(row.profile.id) },
                trailing = {
                    RowMenu(
                        onEdit = { editor = EditorTarget(row) },
                        onDelete = { deleteTarget = row }
                    )
                }
            )
        }

        // Without the phone row the hint ("add one below") goes above the button it points to
        if (!showLocal && !deviceSupported && activeIsLocal) {
            Text(
                stringResource(Res.string.ai_provider_hint_weak),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }

        GymButton(
            onClick = { editor = EditorTarget(null) },
            modifier = Modifier.fillMaxWidth(),
            containerColor = Primary.copy(alpha = 0.1f),
            contentColor = Primary
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(Res.string.ai_provider_add))
        }

        if (showLocal && !deviceSupported && activeIsLocal) {
            Text(
                stringResource(Res.string.ai_provider_hint_weak),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
    }

    editor?.let { target ->
        ProviderEditorSheet(editing = target.row, viewModel = viewModel, onDismiss = { editor = null })
    }

    deleteTarget?.let { row ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(stringResource(Res.string.ai_provider_delete_title), fontWeight = FontWeight.ExtraBold, color = OnSurface)
            },
            text = {
                Text(stringResource(Res.string.ai_provider_delete_body, row.profile.name), color = OnSurfaceVariant)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        viewModel.delete(row.profile.id)
                    }
                ) {
                    Text(stringResource(Res.string.delete).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = Primary)
                }
            }
        )
    }
}

@Composable
private fun ChoiceRow(
    selected: Boolean,
    enabled: Boolean,
    title: String,
    subtitle: String?,
    subtitleColor: Color,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(selectedColor = Primary)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) OnSurface else OnSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun RowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = OnSurfaceVariant)
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.ai_provider_edit)) },
            onClick = {
                open = false
                onEdit()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.ai_provider_delete), color = Error) },
            onClick = {
                open = false
                onDelete()
            }
        )
    }
}
