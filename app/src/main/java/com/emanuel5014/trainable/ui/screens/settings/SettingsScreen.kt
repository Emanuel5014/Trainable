package com.emanuel5014.trainable.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.BuildConfig
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.UpdateDialog
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.fromHSV
import com.emanuel5014.trainable.ui.theme.getPalettePreviewColors
import com.emanuel5014.trainable.ui.theme.getSeedPreviewColors
import com.emanuel5014.trainable.ui.theme.toHSV
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
    val autoBackupIncludeImages by viewModel.autoBackupIncludeImages.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val floatingNavBar by viewModel.floatingNavBar.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val dynamicColorSeed by viewModel.dynamicColorSeed.collectAsState()
    val wallpaperColors by viewModel.wallpaperColors.collectAsState()
    val themePalette by viewModel.themePalette.collectAsState()
    val themeStyle by viewModel.themeStyle.collectAsState()
    val timerNotificationsEnabled by viewModel.timerNotificationsEnabled.collectAsState()
    val gymMembershipExpiryNotificationsEnabled by viewModel.gymMembershipExpiryNotificationsEnabled.collectAsState()
    val gymMembershipExpiryNotificationDaysBefore by viewModel.gymMembershipExpiryNotificationDaysBefore.collectAsState()
    val swipeActionsEnabled by viewModel.swipeActionsEnabled.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()
    
    val latestRelease by viewModel.latestRelease.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }
    var easterEggClicks by remember { mutableIntStateOf(0) }
    var showBackupSetupDialog by remember { mutableStateOf(false) }
    var showIncludeImagesDialog by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }
    var savedSeedForRestore by remember { mutableStateOf<Int?>(null) }
    var savedStyleForRestore by remember { mutableIntStateOf(0) }
    var includeImagesChoice by remember { mutableStateOf(false) }
    var pendingNotificationType by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            when (pendingNotificationType) {
                "timer" -> viewModel.setTimerNotificationsEnabled(true)
                "membership" -> viewModel.setGymMembershipExpiryNotificationsEnabled(true)
            }
        } else {
            Toast.makeText(context, "Permission denied for notifications", Toast.LENGTH_SHORT).show()
        }
        pendingNotificationType = null
    }

    LaunchedEffect(backupStatus) {
        backupStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted && timerNotificationsEnabled) {
                viewModel.setTimerNotificationsEnabled(false)
            }
        }
    }

    if (latestRelease != null) {
        UpdateDialog(
            release = latestRelease!!,
            onDismiss = { viewModel.clearUpdate() },
            onConfirm = { viewModel.downloadAndInstall(latestRelease!!) },
            isDownloading = isDownloading,
            downloadProgress = downloadProgress
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportDatabase(it, includeImagesChoice) }
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
        var tempIncludeImages by remember { mutableStateOf(autoBackupIncludeImages) }

        AlertDialog(
            onDismissRequest = { showBackupSetupDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(stringResource(R.string.auto_backup_setup), fontWeight = FontWeight.ExtraBold, color = OnSurface)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(stringResource(R.string.configure_backup), color = OnSurfaceVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.frequency), fontWeight = FontWeight.ExtraBold, color = OnSurface)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            IconButton(onClick = { if (tempFrequency > 1) tempFrequency-- }) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                            }
                            Text(stringResource(R.string.day_s, tempFrequency), style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
                            IconButton(onClick = { if (tempFrequency < 7) tempFrequency++ }) {
                                Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.keep_last), fontWeight = FontWeight.ExtraBold, color = OnSurface)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            IconButton(onClick = { if (tempMaxCount > 1) tempMaxCount-- }) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                            }
                            Text(stringResource(R.string.backup_s, tempMaxCount), style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
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
                        Text(stringResource(R.string.include_images), color = OnSurface)
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = tempIncludeImages,
                            onCheckedChange = { tempIncludeImages = it }
                        )
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.storage_location), fontWeight = FontWeight.ExtraBold, color = OnSurface)
                        GymButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            containerColor = if (autoBackupFolderUri == null) Primary.copy(alpha = 0.1f) else SurfaceContainerHighest,
                            contentColor = if (autoBackupFolderUri == null) Primary else OnSurface
                        ) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (autoBackupFolderUri != null) 
                                    viewModel.getFolderDisplayPath(autoBackupFolderUri) 
                                else 
                                    stringResource(R.string.choose_folder)
                            )
                        }
                        if (autoBackupFolderUri == null) {
                            Text(
                                stringResource(R.string.folder_required_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = Error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = autoBackupFolderUri != null,
                    onClick = {
                        viewModel.setAutoBackupFrequency(tempFrequency)
                        viewModel.setAutoBackupMaxCount(tempMaxCount)
                        viewModel.setAutoBackupIncludeImages(tempIncludeImages)
                        viewModel.setAutoBackupEnabled(true)
                        showBackupSetupDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save).uppercase(), color = if (autoBackupFolderUri != null) Primary else OnSurfaceVariant)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupSetupDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            }
        )
    }

    if (showIncludeImagesDialog) {
        AlertDialog(
            onDismissRequest = { showIncludeImagesDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.include_images),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    stringResource(R.string.include_images_description),
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        includeImagesChoice = true
                        showIncludeImagesDialog = false
                        exportLauncher.launch("Trainable_Backup_Full.zip")
                    }
                ) {
                    Text(stringResource(R.string.yes), color = Primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        includeImagesChoice = false
                        showIncludeImagesDialog = false
                        exportLauncher.launch("Trainable_Backup.zip")
                    }
                ) {
                    Text(stringResource(R.string.no), color = OnSurfaceVariant)
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
                        stringResource(R.string.reset_app_dialog),
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
            },
            text = {
                Text(
                    stringResource(R.string.reset_app_message),
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                val restartingText = stringResource(R.string.restarting_app)
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetApp {
                            Toast.makeText(context, restartingText, Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            exitProcess(0)
                        }
                    }
                ) {
                    Text(stringResource(R.string.reset_app_button).uppercase(), color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = Primary)
                }
            }
        )
    }

    if (showCustomColorDialog) {
        CustomColorPickerDialog(
            initialColor = savedSeedForRestore?.let { Color(it) },
            initialStyle = savedStyleForRestore,
            onDismiss = {
                if (savedSeedForRestore != null) {
                    viewModel.setDynamicColorSeed(savedSeedForRestore)
                    viewModel.setThemeStyle(savedStyleForRestore)
                }
                showCustomColorDialog = false
            },
            onApply = { colorSeed, style ->
                viewModel.setDynamicColorSeed(colorSeed)
                viewModel.setThemeStyle(style)
                showCustomColorDialog = false
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
                        stringResource(R.string.settings_title), 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        GymIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = onNavigateBack,
                            containerColor = Color.Transparent,
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
            SettingsSection(title = stringResource(R.string.profile)) {
                var showEditUsernameDialog by remember { mutableStateOf(false) }

                if (showEditUsernameDialog) {
                    var newUsername by remember { mutableStateOf(currentUser?.username ?: "") }
                    AlertDialog(
                        onDismissRequest = { showEditUsernameDialog = false },
                        containerColor = SurfaceContainerHigh,
                        title = { Text(stringResource(R.string.edit_username), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
                        text = {
                            OutlinedTextField(
                                value = newUsername,
                                onValueChange = { newUsername = it },
                                label = { Text(stringResource(R.string.username)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = OnSurfaceVariant
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (newUsername.isNotBlank()) {
                                        viewModel.updateUsername(newUsername)
                                        showEditUsernameDialog = false
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.save).uppercase(), color = Primary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditUsernameDialog = false }) {
                                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
                            }
                        }
                    )
                }

                GymCard(
                    containerColor = SurfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.username ?: "Athlete",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = stringResource(R.string.member_since, currentUser?.dataIscrizione?.let { com.emanuel5014.trainable.ui.util.DateFormatter.format(it) } ?: stringResource(R.string.today)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showEditUsernameDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit), tint = OnSurfaceVariant)
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
                                Icon(Icons.Rounded.Scale, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.weight_unit), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.weight_unit_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerHighest)
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "kg",
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (weightUnit == "kg") Primary else Color.Transparent)
                                        .clickable { viewModel.setWeightUnit("kg") }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = if (weightUnit == "kg") OnPrimary else OnSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "lb",
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (weightUnit == "lb") Primary else Color.Transparent)
                                        .clickable { viewModel.setWeightUnit("lb") }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = if (weightUnit == "lb") OnPrimary else OnSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Flag, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.weekly_goal), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
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

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Vibration, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.tactile_feedback), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.tactile_feedback_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsSwitch(
                                checked = hapticEnabled,
                                onCheckedChange = { viewModel.setHapticEnabled(it) }
                            )
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        var showLanguageDialog by remember { mutableStateOf(false) }
                        val currentLanguage by viewModel.userLanguage.collectAsState()
                        val context = LocalContext.current

                        if (showLanguageDialog) {
                            AlertDialog(
                                onDismissRequest = { showLanguageDialog = false },
                                containerColor = SurfaceContainerHigh,
                                title = { Text(stringResource(R.string.language), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
                                text = {
                                    Column {
                                        LanguageOption(
                                            title = stringResource(R.string.language_system_default),
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
                                            title = stringResource(R.string.language_english),
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
                                            title = stringResource(R.string.language_italian),
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
                                        LanguageOption(
                                            title = stringResource(R.string.language_spanish),
                                            isSelected = currentLanguage == "es",
                                            onClick = {
                                                viewModel.setLanguage("es") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                        LanguageOption(
                                            title = stringResource(R.string.language_french),
                                            isSelected = currentLanguage == "fr",
                                            onClick = {
                                                viewModel.setLanguage("fr") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                        LanguageOption(
                                            title = stringResource(R.string.language_german),
                                            isSelected = currentLanguage == "de",
                                            onClick = {
                                                viewModel.setLanguage("de") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                        LanguageOption(
                                            title = stringResource(R.string.language_portuguese),
                                            isSelected = currentLanguage == "pt",
                                            onClick = {
                                                viewModel.setLanguage("pt") {
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
                                        Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
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
                                    Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        when (currentLanguage) {
                                            "en" -> stringResource(R.string.language_english)
                                            "it" -> stringResource(R.string.language_italian)
                                            "es" -> stringResource(R.string.language_spanish)
                                            "fr" -> stringResource(R.string.language_french)
                                            "de" -> stringResource(R.string.language_german)
                                            "pt" -> stringResource(R.string.language_portuguese)
                                            else -> stringResource(R.string.language_system_default)
                                        },
                                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }
                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Dashboard, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.floating_nav_bar), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.floating_nav_bar_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsSwitch(
                                checked = !floatingNavBar,
                                onCheckedChange = { viewModel.setFloatingNavBar(!it) }
                            )
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt, 
                                    contentDescription = null, 
                                    tint = Primary, 
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.swipe_actions), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.swipe_actions_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsSwitch(
                                checked = swipeActionsEnabled,
                                onCheckedChange = { viewModel.setSwipeActionsEnabled(it) }
                            )
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.personalization)) {
                GymCard(containerColor = SurfaceContainerHigh) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Palette, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    val isDynamicColorSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                                    Text(stringResource(R.string.dynamic_color), style = MaterialTheme.typography.titleMedium, color = if (isDynamicColorSupported) OnSurface else OnSurfaceVariant, fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        if (isDynamicColorSupported) stringResource(R.string.dynamic_color_desc) else stringResource(R.string.dynamic_color_not_supported),
                                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsSwitch(
                                checked = dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S,
                                onCheckedChange = { viewModel.setDynamicColor(it) },
                                enabled = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                            )
                        }

                        if (dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && wallpaperColors.isNotEmpty()) {
                            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(stringResource(R.string.app_palette), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                        Text(stringResource(R.string.app_palette_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(SurfaceContainerHighest)
                                                .clickable { 
                                                    viewModel.setDynamicColorSeed(null)
                                                    viewModel.setThemeStyle(0)
                                                }
                                                .padding(if (dynamicColorSeed == null) 4.dp else 0.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (dynamicColorSeed == null) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().background(Primary.copy(alpha = 0.2f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.RestartAlt,
                                                        contentDescription = "System Default",
                                                        tint = Primary,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    Icons.Rounded.RestartAlt,
                                                    contentDescription = "System Default",
                                                    tint = OnSurfaceVariant,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Text("Default", style = MaterialTheme.typography.labelSmall, color = if (dynamicColorSeed == null) Primary else OnSurfaceVariant, maxLines = 1)
                                    }

                                    val primarySeed = wallpaperColors.first()
                                    val styleNames = listOf("Tonal Spot", "Vibrant", "Expressive", "Neutral", "Fruit Salad")
                                    (0..4).forEach { styleIndex ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            PalettePreviewCircle(
                                                colors = getSeedPreviewColors(primarySeed, styleIndex),
                                                isSelected = dynamicColorSeed == primarySeed && themeStyle == styleIndex,
                                                onClick = { 
                                                    viewModel.setDynamicColorSeed(primarySeed)
                                                    viewModel.setThemeStyle(styleIndex)
                                                }
                                            )
                                            Text(styleNames[styleIndex], style = MaterialTheme.typography.labelSmall, color = if (dynamicColorSeed == primarySeed && themeStyle == styleIndex) Primary else OnSurfaceVariant, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        if (!dynamicColor || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(stringResource(R.string.app_palette), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                        Text(stringResource(R.string.app_palette_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val paletteNames = listOf("Default", "Blue", "Green", "Red", "Purple", "Orange", "Pink", "Teal")
                                    (0..7).forEach { index ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            PalettePreviewCircle(
                                                colors = getPalettePreviewColors(index),
                                                isSelected = themePalette == index,
                                                onClick = { viewModel.setThemePalette(index) }
                                            )
                                            Text(paletteNames[index], style = MaterialTheme.typography.labelSmall, color = if (themePalette == index) Primary else OnSurfaceVariant, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    savedSeedForRestore = dynamicColorSeed
                                    savedStyleForRestore = themeStyle
                                    viewModel.setDynamicColorSeed(null)
                                    viewModel.setThemeStyle(0)
                                    showCustomColorDialog = true
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Custom Color", style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    if (dynamicColorSeed != null) {
                                        val styleName = when (themeStyle) {
                                            1 -> "Vibrant"
                                            2 -> "Expressive"
                                            3 -> "Neutral"
                                            4 -> "Fruit Salad"
                                            else -> "Tonal Spot"
                                        }
                                        Text("Custom \u2022 $styleName", style = MaterialTheme.typography.bodySmall, color = Primary)
                                    } else {
                                        Text("Pick your own theme color", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            if (dynamicColorSeed != null) {
                                val previewColors = getSeedPreviewColors(dynamicColorSeed!!, themeStyle)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    previewColors.forEach { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(color)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = {
                                            viewModel.setDynamicColorSeed(null)
                                            viewModel.setThemeStyle(0)
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text("Reset", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            } else {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                            }
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.notifications)) {
                GymCard(containerColor = SurfaceContainerHigh) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications, 
                                    contentDescription = null, 
                                    tint = Primary, 
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.timer_notifications), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.timer_notifications_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsSwitch(
                                checked = timerNotificationsEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        pendingNotificationType = "timer"
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setTimerNotificationsEnabled(enabled)
                                    }
                                }
                            )
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.CreditCard, 
                                    contentDescription = null, 
                                    tint = Primary, 
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.gym_membership_notifications), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.gym_membership_notifications_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsSwitch(
                                checked = gymMembershipExpiryNotificationsEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        pendingNotificationType = "membership"
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setGymMembershipExpiryNotificationsEnabled(enabled)
                                    }
                                }
                            )
                        }

                        if (gymMembershipExpiryNotificationsEnabled) {
                            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Flag, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(stringResource(R.string.notify_days_before), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                        Text(stringResource(R.string.notify_days_before_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (gymMembershipExpiryNotificationDaysBefore > 1) viewModel.setGymMembershipExpiryNotificationDaysBefore(gymMembershipExpiryNotificationDaysBefore - 1) }) {
                                        Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                                    }
                                    Text(
                                        text = gymMembershipExpiryNotificationDaysBefore.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Primary,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(onClick = { if (gymMembershipExpiryNotificationDaysBefore < 30) viewModel.setGymMembershipExpiryNotificationDaysBefore(gymMembershipExpiryNotificationDaysBefore + 1) }) {
                                        Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                                    }
                                }
                            }
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
                                        Text(stringResource(R.string.auto_backup), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                        Text(
                                            if (autoBackupEnabled) stringResource(R.string.backup_enabled, autoBackupFrequency) else stringResource(R.string.backup_disabled),
                                            style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                        )
                                    }
                                }
                                SettingsSwitch(
                                    checked = autoBackupEnabled,
                                    onCheckedChange = { 
                                        if (it) {
                                            showBackupSetupDialog = true
                                        } else {
                                            viewModel.setAutoBackupEnabled(false)
                                        }
                                    }
                                )
                            }

                            if (autoBackupEnabled) {
                                HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(stringResource(R.string.storage_location), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                            Text(
                                                viewModel.getFolderDisplayPath(autoBackupFolderUri),
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


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GymButton(
                            onClick = { showIncludeImagesDialog = true },
                            modifier = Modifier.weight(1f),
                            containerColor = SurfaceContainerHigh,
                            contentColor = OnSurface
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, tint = Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.export_database), fontWeight = FontWeight.ExtraBold)
                        }

                        GymButton(
                            onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                            modifier = Modifier.weight(1f),
                            containerColor = SurfaceContainerHigh,
                            contentColor = OnSurface
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.import_database), fontWeight = FontWeight.ExtraBold)
                        }
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
                        Icon(Icons.Rounded.TableChart, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.export_csv), fontWeight = FontWeight.ExtraBold)
                    }

                    GymButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Error.copy(alpha = 0.15f),
                        contentColor = Error
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = Error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.reset_app_button), fontWeight = FontWeight.ExtraBold, color = Error)
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.about)) {
                GymCard(containerColor = SurfaceContainerHigh) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Trainable v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Made with ❤️ by Emanuel5014",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        easterEggClicks++
                                        if (easterEggClicks >= 3) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=fHLTWJ8X7iQ"))
                                            context.startActivity(intent)
                                            easterEggClicks = 0
                                        }
                                    }
                                )
                            }
                            GymButton(
                                onClick = { viewModel.checkForUpdates() },
                                containerColor = Primary.copy(alpha = 0.1f),
                                contentColor = Primary,
                                height = 48,
                                contentPadding = PaddingValues(horizontal = 24.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.check_for_updates),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = Surface.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GymButton(
                                onClick = { 
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Emanuel5014/Trainable"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                containerColor = Surface.copy(alpha = 0.5f),
                                contentColor = OnSurface
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_github), 
                                    contentDescription = null, 
                                    tint = OnSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.star_on_github), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                            }

                            GymButton(
                                onClick = { 
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/emanuel5014"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                containerColor = Surface.copy(alpha = 0.5f),
                                contentColor = OnSurface
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_kofi), 
                                    contentDescription = null, 
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.buy_me_a_coffee), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
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
            fontWeight = FontWeight.Black,
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

@Composable
private fun SettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                    tint = Primary
                )
            }
        } else {
            null
        }
    )
}

@Composable
private fun PalettePreviewCircle(
    colors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (isSelected) Primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (colors.size == 1) {
                    drawCircle(color = colors[0])
                } else {
                    val primary = colors.getOrElse(0) { Color.Gray }
                    val secondary = colors.getOrElse(1) { primary.copy(alpha = 0.7f) }
                    val tertiary = colors.getOrElse(2) { primary.copy(alpha = 0.5f) }
                    val neutral = colors.getOrElse(3) { primary.copy(alpha = 0.3f) }

                    // Top-left quadrant
                    drawArc(
                        color = primary,
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    // Top-right quadrant
                    drawArc(
                        color = secondary,
                        startAngle = 270f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    // Bottom-right quadrant
                    drawArc(
                        color = tertiary,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    // Bottom-left quadrant
                    drawArc(
                        color = neutral,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomColorPickerDialog(
    initialColor: Color?,
    initialStyle: Int,
    onDismiss: () -> Unit,
    onApply: (colorSeed: Int, style: Int) -> Unit
) {
    val defaultHue = initialColor?.toHSV()?.get(0) ?: 220f
    var hue by remember { mutableFloatStateOf(defaultHue) }
    var selectedStyle by remember { mutableIntStateOf(initialStyle) }

    val currentColor = remember(hue) { Color.fromHSV(hue, 0.8f, 0.9f) }
    val seedArgb = remember(currentColor) { currentColor.toArgb() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = {
            Text("Custom Color", fontWeight = FontWeight.ExtraBold, color = OnSurface)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(currentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${seedArgb.toString(16).padStart(8, '0').substring(2).uppercase()}",
                        color = if (currentColor.let { c -> c.red * 0.299f + c.green * 0.587f + c.blue * 0.114f > 0.5f } ) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                HueSlider(
                    hue = hue,
                    onHueChange = { hue = it },
                    currentColor = currentColor
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme Preview", style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val previewColors = getSeedPreviewColors(seedArgb, selectedStyle)
                        previewColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Style", style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        (0..4).forEach { styleIndex ->
                            PalettePreviewCircle(
                                colors = getSeedPreviewColors(seedArgb, styleIndex),
                                isSelected = selectedStyle == styleIndex,
                                onClick = { selectedStyle = styleIndex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(seedArgb, selectedStyle) }) {
                Text("Apply".uppercase(), color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel".uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    currentColor: Color
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Hue", style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant)
            Text("${hue.toInt()}°", style = MaterialTheme.typography.labelLarge, color = OnSurface)
        }

        val rainbowColors = remember {
            listOf(
                Color(0xFFFF0000),
                Color(0xFFFF8800),
                Color(0xFFFFFF00),
                Color(0xFF00FF00),
                Color(0xFF00CBFF),
                Color(0xFF0055FF),
                Color(0xFF8800FF),
                Color(0xFFFF00FF),
                Color(0xFFFF0000),
            )
        }

        val rainbowStops = remember {
            listOf(0f, 30f / 360f, 60f / 360f, 120f / 360f, 180f / 360f, 240f / 360f, 280f / 360f, 300f / 360f, 1f)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = rainbowColors,
                            startX = 0f,
                            endX = Float.POSITIVE_INFINITY
                        )
                    )
                    .align(Alignment.Center)
            )

            Slider(
                value = hue,
                onValueChange = onHueChange,
                valueRange = 0f..360f,
                modifier = Modifier.fillMaxSize(),
                colors = SliderDefaults.colors(
                    thumbColor = currentColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                )
            )
        }
    }
}
