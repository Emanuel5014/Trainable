package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.back
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.physical_check_auth_button
import com.emanuel5014.trainable.sharedui.resources.physical_check_auth_reason
import com.emanuel5014.trainable.sharedui.resources.physical_check_encrypted_vault
import com.emanuel5014.trainable.sharedui.resources.physical_check_encryption_disabled
import com.emanuel5014.trainable.sharedui.resources.physical_check_enter_password_desc
import com.emanuel5014.trainable.sharedui.resources.physical_check_fingerprint_cd
import com.emanuel5014.trainable.sharedui.resources.physical_check_fingerprint_desc
import com.emanuel5014.trainable.sharedui.resources.physical_check_forgot_password
import com.emanuel5014.trainable.sharedui.resources.physical_check_lock_cd
import com.emanuel5014.trainable.sharedui.resources.physical_check_password_error
import com.emanuel5014.trainable.sharedui.resources.physical_check_password_label
import com.emanuel5014.trainable.sharedui.resources.physical_check_protected_access
import com.emanuel5014.trainable.sharedui.resources.physical_check_reset_confirm
import com.emanuel5014.trainable.sharedui.resources.physical_check_reset_message
import com.emanuel5014.trainable.sharedui.resources.physical_check_reset_title
import com.emanuel5014.trainable.sharedui.resources.physical_check_unlock
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymButtonText
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.screens.onboarding.MorphingLogoShape
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import org.jetbrains.compose.resources.stringResource

/**
 * A locked physical-check page: the icon on the Welcome's morphing Material shape, the title and the explanation,
 * then [content] (the buttons, the password field). Back stays in the corner so the lock is never a dead end.
 */
@Composable
internal fun PhysicalCheckLockScreen(
    icon: ImageVector,
    iconDescription: String,
    title: String,
    description: String,
    onNavigateBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .safeDrawingPadding()
            .imePadding()
    ) {
        IconButton(onClick = onNavigateBack, modifier = Modifier.padding(4.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back), tint = OnSurface)
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MorphingLogoShape(modifier = Modifier.size(168.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconDescription,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = OnSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

/** A password field with the show/hide eye, in the physical-check style. */
@Composable
internal fun PhysicalCheckPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null)
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = OutlineVariant,
            focusedLabelColor = Primary,
            cursorColor = Primary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * What stands in front of every physical-check screen (list, compare, security settings): the fingerprint/PIN when
 * that lock is on and this visit has not given it yet, a loader while the vault opens by itself, the password
 * when it cannot. [content] (with its sheets and dialogs) is composed only once all of them are passed, so nothing
 * of it stays on screen when the section locks again.
 */
@Composable
internal fun PhysicalCheckGate(model: PhysicalCheckModel, onNavigateBack: () -> Unit, content: @Composable () -> Unit) {
    // Null until read: nothing is shown before the locks are known
    val biometricEnabled by model.biometricEnabled.collectAsState(initial = null)
    val encryptionEnabled by model.encryptionEnabled.collectAsState(initial = null)
    val sessionActive by model.sessionActive.collectAsState()
    val isUnlocked by model.isUnlocked.collectAsState()
    val isAutoUnlocking by model.isAutoUnlocking.collectAsState()

    var biometricRetry by remember { mutableIntStateOf(0) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    val platform = LocalPhysicalCheckPlatform.current
    // Screenshots, screen recording and the app-switcher preview never show the section (lock screens included)
    platform.KeepPrivate()
    val biometricPrompt = platform.rememberBiometricPrompt(
        title = stringResource(Res.string.physical_check_protected_access),
        subtitle = stringResource(Res.string.physical_check_auth_reason)
    )
    val showMessage = LocalShowMessage.current
    val encryptionDisabledText = stringResource(Res.string.physical_check_encryption_disabled)

    LaunchedEffect(biometricEnabled, sessionActive, biometricRetry) {
        if (biometricEnabled == true && !sessionActive) {
            if (biometricPrompt != null) {
                biometricPrompt(
                    { model.setSessionActive(true) },
                    // Cancelled or failed: the lock screen stays with its button, no new prompt on its own
                    {}
                )
            } else {
                model.setSessionActive(true)
            }
        }
    }

    when {
        biometricEnabled == null || encryptionEnabled == null -> PhysicalCheckLoading()
        biometricEnabled == true && !sessionActive -> {
            PhysicalCheckLockScreen(
                icon = Icons.Rounded.Fingerprint,
                iconDescription = stringResource(Res.string.physical_check_fingerprint_cd),
                title = stringResource(Res.string.physical_check_protected_access),
                description = stringResource(Res.string.physical_check_fingerprint_desc),
                onNavigateBack = onNavigateBack
            ) {
                GymButton(onClick = { biometricRetry++ }, modifier = Modifier.fillMaxWidth()) {
                    GymButtonText(stringResource(Res.string.physical_check_auth_button), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        encryptionEnabled == true && isAutoUnlocking -> PhysicalCheckLoading()
        encryptionEnabled == true && !isUnlocked -> {
            val unlock = {
                model.unlock(
                    password = passwordInput,
                    onSuccess = { passwordInput = "" },
                    onError = { passwordError = true }
                )
            }
            PhysicalCheckLockScreen(
                icon = Icons.Rounded.Lock,
                iconDescription = stringResource(Res.string.physical_check_lock_cd),
                title = stringResource(Res.string.physical_check_encrypted_vault),
                description = stringResource(Res.string.physical_check_enter_password_desc),
                onNavigateBack = onNavigateBack
            ) {
                PhysicalCheckPasswordField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        passwordError = false
                    },
                    label = stringResource(Res.string.physical_check_password_label),
                    isError = passwordError
                )
                if (passwordError) {
                    Text(
                        text = stringResource(Res.string.physical_check_password_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                    )
                }
                GymButton(onClick = unlock, modifier = Modifier.fillMaxWidth()) {
                    GymButtonText(stringResource(Res.string.physical_check_unlock), fontWeight = FontWeight.ExtraBold)
                }
                TextButton(onClick = { showResetConfirmDialog = true }) {
                    Text(
                        text = stringResource(Res.string.physical_check_forgot_password),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        else -> content()
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
                        model.resetAllData { showMessage(encryptionDisabledText) }
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
}

@Composable
private fun PhysicalCheckLoading() {
    Box(modifier = Modifier.fillMaxSize().background(Surface), contentAlignment = Alignment.Center) {
        GymLoadingIndicator()
    }
}
