package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.semantics.Role
import com.emanuel5014.trainable.data.repository.PhysicalCheckRepository
import com.emanuel5014.trainable.sharedui.resources.physical_check_change_password
import com.emanuel5014.trainable.sharedui.resources.physical_check_change_password_desc
import com.emanuel5014.trainable.sharedui.resources.physical_check_current_password
import com.emanuel5014.trainable.sharedui.resources.physical_check_password_changed
import com.emanuel5014.trainable.sharedui.resources.save
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.back
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.physical_check_biometric_desc
import com.emanuel5014.trainable.sharedui.resources.physical_check_biometric_lock
import com.emanuel5014.trainable.sharedui.resources.physical_check_confirm_password
import com.emanuel5014.trainable.sharedui.resources.physical_check_disable
import com.emanuel5014.trainable.sharedui.resources.physical_check_disable_encryption
import com.emanuel5014.trainable.sharedui.resources.physical_check_disable_encryption_desc
import com.emanuel5014.trainable.sharedui.resources.physical_check_enable
import com.emanuel5014.trainable.sharedui.resources.physical_check_enable_encryption
import com.emanuel5014.trainable.sharedui.resources.physical_check_enable_encryption_desc
import com.emanuel5014.trainable.sharedui.resources.physical_check_encryption
import com.emanuel5014.trainable.sharedui.resources.physical_check_encryption_desc
import com.emanuel5014.trainable.sharedui.resources.physical_check_encryption_disabled
import com.emanuel5014.trainable.sharedui.resources.physical_check_encryption_enabled
import com.emanuel5014.trainable.sharedui.resources.physical_check_encryption_error
import com.emanuel5014.trainable.sharedui.resources.physical_check_forgot_password
import com.emanuel5014.trainable.sharedui.resources.physical_check_health_section
import com.emanuel5014.trainable.sharedui.resources.physical_check_new_password
import com.emanuel5014.trainable.sharedui.resources.physical_check_password_error
import com.emanuel5014.trainable.sharedui.resources.physical_check_password_label
import com.emanuel5014.trainable.sharedui.resources.physical_check_password_min_length
import com.emanuel5014.trainable.sharedui.resources.physical_check_passwords_dont_match
import com.emanuel5014.trainable.sharedui.resources.physical_check_reset_confirm
import com.emanuel5014.trainable.sharedui.resources.physical_check_reset_message
import com.emanuel5014.trainable.sharedui.resources.physical_check_reset_title
import com.emanuel5014.trainable.sharedui.resources.physical_check_settings_title
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.screens.settings.SettingsSection
import com.emanuel5014.trainable.ui.screens.settings.SettingsSwitch
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.util.bodyPadding
import com.emanuel5014.trainable.ui.util.scrollEndInset
import org.jetbrains.compose.resources.stringResource

/** The lock and the encryption of the physical checks, in one card like the other settings screens. */
@Composable
fun PhysicalCheckSettingsContent(
    onNavigateBack: () -> Unit,
    viewModel: PhysicalCheckModel
) {
    // Turning the locks off needs the locks passed first
    PhysicalCheckGate(viewModel, onNavigateBack) {
        SettingsBody(onNavigateBack, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBody(
    onNavigateBack: () -> Unit,
    viewModel: PhysicalCheckModel
) {
    val showMessage = LocalShowMessage.current
    val passwordMinLengthText = stringResource(Res.string.physical_check_password_min_length)
    val passwordsDontMatchText = stringResource(Res.string.physical_check_passwords_dont_match)
    val encryptionEnabledText = stringResource(Res.string.physical_check_encryption_enabled)
    val encryptionErrorText = stringResource(Res.string.physical_check_encryption_error)
    val encryptionDisabledText = stringResource(Res.string.physical_check_encryption_disabled)
    val biometricEnabled by viewModel.biometricEnabled.collectAsState(initial = false)
    val encryptionEnabled by viewModel.encryptionEnabled.collectAsState(initial = false)

    var showEnableEncryptionDialog by remember { mutableStateOf(false) }
    var showDisableEncryptionDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    val passwordChangedText = stringResource(Res.string.physical_check_password_changed)
    val wrongPasswordText = stringResource(Res.string.physical_check_password_error)
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.physical_check_settings_title),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back), tint = OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = OnSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues.bodyPadding())
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .padding(bottom = paddingValues.scrollEndInset()),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            SettingsSection(title = stringResource(Res.string.physical_check_health_section)) {
                GymCard(containerColor = SurfaceContainerHigh) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LockSwitchRow(
                            icon = Icons.Rounded.Fingerprint,
                            title = stringResource(Res.string.physical_check_biometric_lock),
                            description = stringResource(Res.string.physical_check_biometric_desc),
                            checked = biometricEnabled,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) }
                        )
                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))
                        LockSwitchRow(
                            icon = Icons.Rounded.EnhancedEncryption,
                            title = stringResource(Res.string.physical_check_encryption),
                            description = stringResource(Res.string.physical_check_encryption_desc),
                            checked = encryptionEnabled,
                            onCheckedChange = { on ->
                                if (on) showEnableEncryptionDialog = true else showDisableEncryptionDialog = true
                            }
                        )
                        if (encryptionEnabled) {
                            HorizontalDivider(color = Surface.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) { showChangePasswordDialog = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Password, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(Res.string.physical_check_change_password),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = OnSurface,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        stringResource(Res.string.physical_check_change_password_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChangePasswordDialog) {
        var current by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }
        var working by remember { mutableStateOf(false) }

        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { if (!working) showChangePasswordDialog = false },
            title = { Text(stringResource(Res.string.physical_check_change_password), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PhysicalCheckPasswordField(
                        value = current,
                        onValueChange = { current = it; errorText = null },
                        label = stringResource(Res.string.physical_check_current_password)
                    )
                    PhysicalCheckPasswordField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorText = null },
                        label = stringResource(Res.string.physical_check_new_password)
                    )
                    PhysicalCheckPasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorText = null },
                        label = stringResource(Res.string.physical_check_confirm_password),
                        isError = errorText != null
                    )
                    errorText?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (working) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !working,
                    onClick = {
                        if (newPassword.length < PhysicalCheckRepository.MIN_PASSWORD_LENGTH) {
                            errorText = passwordMinLengthText
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            errorText = passwordsDontMatchText
                            return@Button
                        }
                        // Every photo is encrypted again: it can take a moment with many photos
                        working = true
                        viewModel.changePassword(
                            oldPassword = current,
                            newPassword = newPassword,
                            onSuccess = {
                                working = false
                                showMessage(passwordChangedText)
                                showChangePasswordDialog = false
                            },
                            onError = {
                                working = false
                                errorText = wrongPasswordText
                            }
                        )
                    }
                ) {
                    Text(stringResource(Res.string.save))
                }
            },
            dismissButton = {
                TextButton(enabled = !working, onClick = { showChangePasswordDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (showEnableEncryptionDialog) {
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { showEnableEncryptionDialog = false },
            title = { Text(stringResource(Res.string.physical_check_enable_encryption), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.physical_check_enable_encryption_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    PhysicalCheckPasswordField(
                        value = password,
                        onValueChange = { password = it; errorText = null },
                        label = stringResource(Res.string.physical_check_new_password)
                    )
                    PhysicalCheckPasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorText = null },
                        label = stringResource(Res.string.physical_check_confirm_password),
                        isError = errorText != null
                    )
                    errorText?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (password.length < PhysicalCheckRepository.MIN_PASSWORD_LENGTH) {
                            errorText = passwordMinLengthText
                            return@Button
                        }
                        if (password != confirmPassword) {
                            errorText = passwordsDontMatchText
                            return@Button
                        }
                        viewModel.enableEncryption(
                            password = password,
                            onSuccess = {
                                showMessage(encryptionEnabledText)
                                showEnableEncryptionDialog = false
                            },
                            onError = { errorText = encryptionErrorText }
                        )
                    }
                ) {
                    Text(stringResource(Res.string.physical_check_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnableEncryptionDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(stringResource(Res.string.physical_check_reset_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.physical_check_reset_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        viewModel.resetAllData {
                            showMessage(encryptionDisabledText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.physical_check_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (showDisableEncryptionDialog) {
        var passwordConfirm by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { showDisableEncryptionDialog = false },
            title = { Text(stringResource(Res.string.physical_check_disable_encryption), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.physical_check_disable_encryption_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    PhysicalCheckPasswordField(
                        value = passwordConfirm,
                        onValueChange = { passwordConfirm = it; isError = false },
                        label = stringResource(Res.string.physical_check_password_label),
                        isError = isError
                    )
                    if (isError) {
                        Text(
                            text = stringResource(Res.string.physical_check_password_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(
                        onClick = {
                            showDisableEncryptionDialog = false
                            showResetConfirmDialog = true
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = stringResource(Res.string.physical_check_forgot_password),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.disableEncryption(
                            password = passwordConfirm,
                            onSuccess = {
                                showMessage(encryptionDisabledText)
                                showDisableEncryptionDialog = false
                            },
                            onError = { isError = true }
                        )
                    }
                ) {
                    Text(stringResource(Res.string.physical_check_disable))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableEncryptionDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

/** An icon, a title with its explanation and a switch, as in Workout settings. */
@Composable
private fun LockSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        SettingsSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
