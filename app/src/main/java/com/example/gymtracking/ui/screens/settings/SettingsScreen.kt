package com.example.gymtracking.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracking.MainActivity
import com.example.gymtracking.R
import com.example.gymtracking.ui.components.GymButton
import com.example.gymtracking.ui.components.GymIconButton
import com.example.gymtracking.ui.components.GymCard
import com.example.gymtracking.ui.theme.*
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val weeklyGoal by viewModel.weeklyGoal.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val autoBackupFrequency by viewModel.autoBackupFrequency.collectAsState()
    val autoBackupFolderUri by viewModel.autoBackupFolderUri.collectAsState()
    val autoBackupMaxCount by viewModel.autoBackupMaxCount.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showBackupSetupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(backupStatus) {
        backupStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportDatabase(it) }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { outputUri ->
            viewModel.getCsvContent()?.let { csv ->
                try {
                    context.contentResolver.openOutputStream(outputUri)?.use { output ->
                        output.write(csv.toByteArray())
                    }
                    Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error writing file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { 
            viewModel.importDatabase(it) {
                Toast.makeText(context, "Database imported. Restarting app...", Toast.LENGTH_LONG).show()
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                exitProcess(0)
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.setAutoBackupFolder(it) }
    }

    if (showBackupSetupDialog) {
        var tempFrequency by remember { mutableIntStateOf(autoBackupFrequency) }
        var tempMaxCount by remember { mutableIntStateOf(autoBackupMaxCount) }
        var useCustomFolder by remember { mutableStateOf(autoBackupFolderUri != null) }

        AlertDialog(
            onDismissRequest = { showBackupSetupDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text("Auto Backup Setup", fontWeight = FontWeight.Bold, color = OnSurface)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("Configure your automatic backup settings", color = OnSurfaceVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Frequency", fontWeight = FontWeight.Bold, color = OnSurface)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            IconButton(onClick = { if (tempFrequency > 1) tempFrequency-- }) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                            }
                            Text("$tempFrequency day(s)", style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
                            IconButton(onClick = { if (tempFrequency < 7) tempFrequency++ }) {
                                Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Keep last", fontWeight = FontWeight.Bold, color = OnSurface)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            IconButton(onClick = { if (tempMaxCount > 1) tempMaxCount-- }) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                            }
                            Text("$tempMaxCount backup(s)", style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
                            IconButton(onClick = { if (tempMaxCount < 10) tempMaxCount++ }) {
                                Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Save to custom folder", color = OnSurface)
                        Switch(
                            checked = useCustomFolder,
                            onCheckedChange = { useCustomFolder = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnPrimary,
                                checkedTrackColor = Primary,
                                uncheckedThumbColor = OnSurfaceVariant,
                                uncheckedTrackColor = SurfaceContainerHigh
                            )
                        )
                    }

                    if (useCustomFolder) {
                        TextButton(onClick = { folderPickerLauncher.launch(null) }) {
                            Icon(Icons.Rounded.Folder, contentDescription = "Folder", tint = OnSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (autoBackupFolderUri != null) stringResource(R.string.folder_selected) else stringResource(R.string.choose_folder))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setAutoBackupFrequency(tempFrequency)
                        viewModel.setAutoBackupMaxCount(tempMaxCount)
                        if (!useCustomFolder) {
                            viewModel.setAutoBackupFolder(android.net.Uri.EMPTY)
                        }
                        viewModel.setAutoBackupEnabled(true)
                        showBackupSetupDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save).uppercase(), color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupSetupDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    "Reset App?",
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    "This will delete all your data including workouts, routines, and settings. This action cannot be undone.",
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetApp {
                            Toast.makeText(context, "Restarting app...", Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            exitProcess(0)
                        }
                    }
                ) {
                    Text("RESET", color = Tertiary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("CANCEL", color = Primary)
                }
            }
        )
    }

    Scaffold(
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "SETTINGS", 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        GymIconButton(
                            icon = Icons.Rounded.ArrowBack,
                            onClick = onNavigateBack,
                            containerColor = SurfaceContainerHigh,
                            contentColor = OnSurface,
                            description = "Back"
                        )
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
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            SettingsSection(title = "PROFILE") {
                GymCard(containerColor = SurfaceContainerHigh) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column {
                            Text(
                                text = currentUser?.username ?: "Athlete",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.member_since, currentUser?.dataIscrizione?.let { com.example.gymtracking.ui.util.DateFormatter.format(it) } ?: stringResource(R.string.today)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.preferences)) {
                GymCard(containerColor = SurfaceContainerHigh) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Flag, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.weekly_goal), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.target_workouts), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (weeklyGoal > 1) viewModel.setWeeklyGoal(weeklyGoal - 1) }) {
                                    Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                                }
                                Text(
                                    text = weeklyGoal.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Primary,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { if (weeklyGoal < 7) viewModel.setWeeklyGoal(weeklyGoal + 1) }) {
                                    Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                                }
                            }
                        }

                        Divider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Vibration, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Tactile Feedback", style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.Bold)
                                    Text("Vibration on interactions", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = hapticEnabled,
                                onCheckedChange = { viewModel.setHapticEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnPrimary,
                                    checkedTrackColor = Primary,
                                    uncheckedThumbColor = OnSurfaceVariant,
                                    uncheckedTrackColor = SurfaceContainerHigh,
                                    uncheckedBorderColor = OnSurfaceVariant
                                )
                            )
                        }

                        Divider(color = Surface.copy(alpha = 0.5f))

                        var showLanguageDialog by remember { mutableStateOf(false) }
                        val currentLanguage by viewModel.userLanguage.collectAsState()
                        val context = LocalContext.current

                        if (showLanguageDialog) {
                            AlertDialog(
                                onDismissRequest = { showLanguageDialog = false },
                                containerColor = SurfaceContainerHigh,
                                title = { Text("Language", fontWeight = FontWeight.Bold, color = OnSurface) },
                                text = {
                                    Column {
                                        LanguageOption(
                                            title = "System Default",
                                            isSelected = currentLanguage == "system",
                                            onClick = {
                                                viewModel.setLanguage("system") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                        LanguageOption(
                                            title = "English",
                                            isSelected = currentLanguage == "en",
                                            onClick = {
                                                viewModel.setLanguage("en") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                        LanguageOption(
                                            title = "Italiano",
                                            isSelected = currentLanguage == "it",
                                            onClick = {
                                                viewModel.setLanguage("it") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showLanguageDialog = false }) {
                                        Text("CANCEL", color = OnSurfaceVariant)
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.Bold)
                                    Text(
                                        when (currentLanguage) {
                                            "en" -> stringResource(R.string.language_english)
                                            "it" -> stringResource(R.string.language_italian)
                                            else -> stringResource(R.string.language_system_default)
                                        },
                                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.data_backup)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GymCard(containerColor = SurfaceContainerHigh) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Backup, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(stringResource(R.string.auto_backup), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (autoBackupEnabled) stringResource(R.string.backup_enabled, autoBackupFrequency) else stringResource(R.string.backup_disabled),
                                            style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = autoBackupEnabled,
                                    onCheckedChange = { 
                                        if (it) {
                                            showBackupSetupDialog = true
                                        } else {
                                            viewModel.setAutoBackupEnabled(false)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OnPrimary,
                                        checkedTrackColor = Primary,
                                        uncheckedThumbColor = OnSurfaceVariant,
                                        uncheckedTrackColor = SurfaceContainerHigh,
                                        uncheckedBorderColor = OnSurfaceVariant
                                    )
                                )
                            }

                            if (autoBackupEnabled) {
                                Divider(color = Surface.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(stringResource(R.string.storage_location), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.Bold)
                                            Text(
                                                if (autoBackupFolderUri != null) stringResource(R.string.custom_folder) else stringResource(R.string.app_internal),
                                                style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                            )
                                        }
                                    }
                                    TextButton(onClick = { showBackupSetupDialog = true }) {
                                        Text(stringResource(R.string.edit_backup).uppercase(), color = Primary)
                                    }
                                }
                            }
                        }
                    }

                    GymButton(
                        onClick = { exportLauncher.launch("Trainable_Backup.zip") },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurface
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.export_database), fontWeight = FontWeight.Bold)
                    }

                    GymButton(
                        onClick = { 
                            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                            csvExportLauncher.launch("Workouts_$timestamp.csv")
                            viewModel.exportWorkoutsToCsv()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurface
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("EXPORT CSV", fontWeight = FontWeight.Bold)
                    }

                    GymButton(
                        onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurface
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("IMPORT DATABASE", fontWeight = FontWeight.Bold)
                    }

                    GymButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = SurfaceContainerHigh.copy(alpha = 0.5f),
                        contentColor = Tertiary
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = Tertiary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("RESET APP", fontWeight = FontWeight.Bold, color = Tertiary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "Trainable v1.4",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = Primary,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
private fun LanguageOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurface
        )
        if (isSelected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Primary)
        }
    }
}
