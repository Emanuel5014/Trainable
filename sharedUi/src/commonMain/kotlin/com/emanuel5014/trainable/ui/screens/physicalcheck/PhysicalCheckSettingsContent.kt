package com.emanuel5014.trainable.ui.screens.physicalcheck

import org.jetbrains.compose.resources.stringResource
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
import com.emanuel5014.trainable.ui.components.LocalShowMessage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalCheckSettingsContent(
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.physical_check_settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.physical_check_health_section),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Switch Biometria
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.physical_check_biometric_lock),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.physical_check_biometric_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) },
                        thumbContent = if (biometricEnabled) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }

            // Switch Cifratura Backup
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.physical_check_encryption),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.physical_check_encryption_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = encryptionEnabled,
                        onCheckedChange = { active ->
                            if (active) {
                                showEnableEncryptionDialog = true
                            } else {
                                showDisableEncryptionDialog = true
                            }
                        },
                        thumbContent = if (encryptionEnabled) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    if (showEnableEncryptionDialog) {
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }
        var passwordVisible by remember { mutableStateOf(false) }
        var confirmPasswordVisible by remember { mutableStateOf(false) }

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
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorText = null },
                        label = { Text(stringResource(Res.string.physical_check_new_password)) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorText = null },
                        label = { Text(stringResource(Res.string.physical_check_confirm_password)) },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (password.length < 4) {
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
                            onError = {
                                errorText = encryptionErrorText
                            }
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

    var showResetConfirmDialog by remember { mutableStateOf(false) }

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
        var passwordConfirmVisible by remember { mutableStateOf(false) }

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
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passwordConfirm,
                        onValueChange = { passwordConfirm = it; isError = false },
                        label = { Text(stringResource(Res.string.physical_check_password_label)) },
                        singleLine = true,
                        isError = isError,
                        visualTransformation = if (passwordConfirmVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordConfirmVisible = !passwordConfirmVisible }) {
                                Icon(
                                    imageVector = if (passwordConfirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordConfirmVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isError) {
                        Text(
                            text = stringResource(Res.string.physical_check_password_error),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
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
                            fontSize = 13.sp
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
                            onError = {
                                isError = true
                            }
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
