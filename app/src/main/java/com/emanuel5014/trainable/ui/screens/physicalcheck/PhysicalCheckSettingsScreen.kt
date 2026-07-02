package com.emanuel5014.trainable.ui.screens.physicalcheck

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalCheckSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PhysicalCheckViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val biometricEnabled by viewModel.biometricEnabled.collectAsState(initial = false)
    val encryptionEnabled by viewModel.encryptionEnabled.collectAsState(initial = false)

    var showEnableEncryptionDialog by remember { mutableStateOf(false) }
    var showDisableEncryptionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sicurezza Check Fisici", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                text = "Protezione e Privacy",
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
                            text = "Blocco Biometrico / PIN",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Richiedi l'impronta digitale o il PIN per accedere alla sezione.",
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
                            text = "Cifratura Dati e Foto",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cifra localmente tutte le foto usando una password portabile. Sicura nei backup.",
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

        AlertDialog(
            onDismissRequest = { showEnableEncryptionDialog = false },
            title = { Text("Attiva Cifratura", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Imposta una password. Ti servirà per accedere ai dati qualora ripristinassi il backup su un altro dispositivo.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorText = null },
                        label = { Text("Nuova Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorText = null },
                        label = { Text("Conferma Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
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
                            errorText = "La password deve essere di almeno 4 caratteri"
                            return@Button
                        }
                        if (password != confirmPassword) {
                            errorText = "Le password non coincidono"
                            return@Button
                        }

                        viewModel.enableEncryption(
                            password = password,
                            onSuccess = {
                                Toast.makeText(context, "Cifratura attivata con successo", Toast.LENGTH_SHORT).show()
                                showEnableEncryptionDialog = false
                            },
                            onError = {
                                errorText = "Errore durante l'attivazione della cifratura"
                            }
                        )
                    }
                ) {
                    Text("Attiva")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnableEncryptionDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    if (showDisableEncryptionDialog) {
        var passwordConfirm by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDisableEncryptionDialog = false },
            title = { Text("Disattiva Cifratura", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Inserisci la password per decifrare tutte le immagini correnti e salvarle in chiaro.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passwordConfirm,
                        onValueChange = { passwordConfirm = it; isError = false },
                        label = { Text("Password") },
                        singleLine = true,
                        isError = isError,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isError) {
                        Text(
                            text = "Password errata. Riprova.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
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
                                Toast.makeText(context, "Cifratura disattivata", Toast.LENGTH_SHORT).show()
                                showDisableEncryptionDialog = false
                            },
                            onError = {
                                isError = true
                            }
                        )
                    }
                ) {
                    Text("Disattiva")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableEncryptionDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}
