package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import com.emanuel5014.trainable.data.ai.remote.AiProfileList
import com.emanuel5014.trainable.data.ai.remote.AiProtocol
import com.emanuel5014.trainable.data.ai.remote.BaseUrlPolicy
import com.emanuel5014.trainable.data.ai.remote.KeyState
import com.emanuel5014.trainable.data.ai.remote.ProviderPreset
import com.emanuel5014.trainable.data.ai.remote.ProviderPresets
import com.emanuel5014.trainable.data.ai.remote.UrlCheck
import com.emanuel5014.trainable.data.ai.remote.UrlRejection
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.screens.routines.userText
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import kotlinx.coroutines.launch

/** Adds a provider ([editing] null) or edits a saved one; the API key is never shown again once saved. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProviderEditorSheet(
    editing: ProviderRow?,
    viewModel: AiProvidersViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = editing?.profile

    var name by remember { mutableStateOf(profile?.name.orEmpty()) }
    var protocol by remember { mutableStateOf(profile?.protocol ?: AiProtocol.OPENAI_COMPAT) }
    var baseUrl by remember { mutableStateOf(profile?.baseUrl ?: "https://") }
    var model by remember { mutableStateOf(profile?.model.orEmpty()) }
    var key by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var clearKey by remember { mutableStateOf(false) }
    var urlHint by remember { mutableStateOf<String?>(null) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val urlCheck = BaseUrlPolicy.check(baseUrl)
    val urlTyped = baseUrl.trim().removePrefix("https://").removePrefix("http://").isNotEmpty()
    val savedKeyExists = profile?.encryptedKey != null
    // The saved key only follows the profile to the host it was saved for
    val hostChanged = profile != null && !AiProfileList.keepsKey(profile, profile.copy(baseUrl = baseUrl))
    val savedKeyUsable = editing?.keyState == KeyState.PRESENT && !hostChanged && !clearKey
    val typedKey = AiProfileList.cleanKey(key)
    val keyInvalid = typedKey != null && !AiProfileList.isValidKey(typedKey)
    val keyMissing = protocol == AiProtocol.ANTHROPIC && !savedKeyUsable && typedKey == null
    val canSave = urlCheck is UrlCheck.Ok && model.isNotBlank() && !keyMissing && !keyInvalid

    fun draft() = ProviderDraft(
        id = profile?.id,
        name = name,
        protocol = protocol,
        baseUrl = baseUrl,
        model = model,
        // null keeps the saved key, blank removes it, anything else replaces it
        plainKey = when {
            key.isNotBlank() -> key
            clearKey -> ""
            else -> null
        }
    )

    fun describe(error: Throwable): String =
        (error as? ScanBackendException)?.userText(context) ?: error.message.orEmpty()

    fun applyPreset(preset: ProviderPreset) {
        if (name.isBlank()) name = preset.name
        protocol = preset.protocol
        baseUrl = preset.baseUrl
        urlHint = preset.hint
        models = emptyList()
        status = null
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Primary,
        unfocusedBorderColor = OnSurfaceVariant
    )
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = Primary.copy(alpha = 0.15f),
        selectedLabelColor = Primary
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        contentColor = OnSurface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(if (editing == null) R.string.ai_provider_editor_add_title else R.string.ai_provider_editor_edit_title),
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface,
                fontWeight = FontWeight.Black
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProviderPresets.all.forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = { applyPreset(preset) },
                        label = { Text(preset.name, maxLines = 1) },
                        colors = chipColors
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.ai_provider_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = protocol == AiProtocol.OPENAI_COMPAT,
                    onClick = { protocol = AiProtocol.OPENAI_COMPAT },
                    label = { Text(stringResource(R.string.ai_provider_protocol_openai), maxLines = 1) },
                    colors = chipColors
                )
                FilterChip(
                    selected = protocol == AiProtocol.ANTHROPIC,
                    onClick = { protocol = AiProtocol.ANTHROPIC },
                    label = { Text(stringResource(R.string.ai_provider_protocol_claude), maxLines = 1) },
                    colors = chipColors
                )
            }

            val rejection = (urlCheck as? UrlCheck.Rejected)?.reason
            OutlinedTextField(
                value = baseUrl,
                onValueChange = {
                    baseUrl = it
                    models = emptyList()
                    status = null
                },
                label = { Text(stringResource(R.string.ai_provider_url)) },
                isError = rejection != null && urlTyped,
                supportingText = if (rejection != null && urlTyped) {
                    {
                        Text(
                            stringResource(
                                when (rejection) {
                                    UrlRejection.CLEARTEXT_TO_PUBLIC_HOST -> R.string.ai_provider_url_cleartext
                                    UrlRejection.UNSUPPORTED_SCHEME -> R.string.ai_provider_url_scheme
                                    UrlRejection.INVALID -> R.string.ai_provider_url_invalid
                                }
                            )
                        )
                    }
                } else if (urlHint != null) {
                    { Text(stringResource(R.string.ai_provider_url_example, urlHint.orEmpty())) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors
            )

            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text(stringResource(R.string.ai_provider_key)) },
                placeholder = { Text(if (savedKeyUsable) "••••••" else "") },
                isError = keyInvalid,
                supportingText = {
                    Text(
                        stringResource(
                            when {
                                keyInvalid -> R.string.ai_provider_key_invalid
                                clearKey -> R.string.ai_provider_key_will_remove
                                savedKeyExists && hostChanged -> R.string.ai_provider_key_host_changed
                                savedKeyUsable -> R.string.ai_provider_key_keep
                                protocol == AiProtocol.ANTHROPIC -> R.string.ai_provider_key_required
                                else -> R.string.ai_provider_key_optional
                            }
                        )
                    )
                },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = OnSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors
            )

            if (savedKeyExists && !clearKey) {
                TextButton(onClick = { clearKey = true }) {
                    Text(stringResource(R.string.ai_provider_key_remove).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold)
                }
            }

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text(stringResource(R.string.ai_provider_model)) },
                supportingText = { Text(stringResource(R.string.ai_provider_model_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors
            )

            TextButton(
                onClick = {
                    scope.launch {
                        busy = true
                        status = null
                        viewModel.loadModels(draft()).fold(
                            onSuccess = { list ->
                                models = list
                                if (list.isEmpty()) status = false to context.getString(R.string.ai_provider_models_none)
                            },
                            onFailure = { status = false to describe(it) }
                        )
                        busy = false
                    }
                },
                enabled = !busy && urlCheck is UrlCheck.Ok && !keyInvalid
            ) {
                Text(stringResource(R.string.ai_provider_models_load).uppercase(), color = Primary, fontWeight = FontWeight.ExtraBold)
            }

            if (models.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    models.take(MAX_MODEL_CHIPS).forEach { id ->
                        FilterChip(
                            selected = id == model,
                            onClick = { model = id },
                            label = { Text(id, maxLines = 1) },
                            colors = chipColors
                        )
                    }
                }
            }

            status?.let { (ok, text) ->
                Text(text, style = MaterialTheme.typography.bodySmall, color = if (ok) Primary else Error)
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                GymButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = null
                            viewModel.test(draft()).fold(
                                onSuccess = { status = true to context.getString(R.string.ai_provider_test_ok) },
                                onFailure = { status = false to describe(it) }
                            )
                            busy = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = Primary.copy(alpha = 0.12f),
                    contentColor = Primary,
                    enabled = !busy && urlCheck is UrlCheck.Ok && model.isNotBlank() && !keyMissing && !keyInvalid
                ) {
                    Text(stringResource(R.string.ai_provider_test).uppercase(), fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
                Spacer(Modifier.width(12.dp))
                GymButton(
                    onClick = { viewModel.save(draft(), onSaved = onDismiss) },
                    modifier = Modifier.weight(1f),
                    enabled = canSave && !busy
                ) {
                    Text(stringResource(R.string.ai_provider_save).uppercase(), fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
        }
    }
}

private const val MAX_MODEL_CHIPS = 40
