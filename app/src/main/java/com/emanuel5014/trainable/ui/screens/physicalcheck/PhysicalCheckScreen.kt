package com.emanuel5014.trainable.ui.screens.physicalcheck

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.util.BiometricHelper
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhysicalCheckScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCompare: (Int, Int) -> Unit,
    viewModel: PhysicalCheckViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val checks by viewModel.checks.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val encryptionEnabled by viewModel.encryptionEnabled.collectAsState(initial = false)
    val biometricEnabled by viewModel.biometricEnabled.collectAsState(initial = false)
    val sessionActive by viewModel.sessionActive.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    var selectedForCompare by remember { mutableStateOf(setOf<Int>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var checkToDelete by remember { mutableStateOf<PhysicalCheckEntity?>(null) }

    var biometricRetry by remember { mutableIntStateOf(0) }

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val swipeActionsEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.SWIPE_ACTIONS_ENABLED] ?: true }
    }.collectAsState(initial = true)

    data class FullscreenState(val checkId: Int, val filenames: List<String>, val initialIndex: Int)
    var fullscreenState by remember { mutableStateOf<FullscreenState?>(null) }

    var addingPhotosForCheckId by remember { mutableStateOf<Int?>(null) }
    var showAddPhotoOptions by remember { mutableStateOf(false) }
    var checkToEdit by remember { mutableStateOf<PhysicalCheckEntity?>(null) }
    val photoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val addPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val checkId = addingPhotosForCheckId
        if (checkId == null) {
            viewModel.setPhotoCaptureCompleted()
            return@rememberLauncherForActivityResult
        }
        val contentResolver = context.contentResolver
        val bytesList = mutableListOf<ByteArray>()
        uris.forEach { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    bytesList.add(inputStream.readBytes())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (bytesList.isNotEmpty()) {
            viewModel.addPhotosToCheck(checkId, bytesList)
        }
        addingPhotosForCheckId = null
        viewModel.setPhotoCaptureCompleted()
    }

    val addPhotoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val checkId = addingPhotosForCheckId
            if (checkId == null) {
                viewModel.setPhotoCaptureCompleted()
                return@rememberLauncherForActivityResult
            }
            tempCameraUri?.let { uri ->
                val bytes = com.emanuel5014.trainable.util.ImageStorageUtils.readAndCompressImage(context, uri)
                if (bytes != null) {
                    viewModel.addPhotosToCheck(checkId, listOf(bytes))
                }
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        addingPhotosForCheckId = null
        showAddPhotoOptions = false
        viewModel.setPhotoCaptureCompleted()
    }

    val addPhotoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createCheckTempImageUri(context)
            tempCameraUri = uri
            addPhotoCameraLauncher.launch(uri)
        } else {
            viewModel.setPhotoCaptureCompleted()
            Toast.makeText(context, context.getString(R.string.physical_check_camera_denied), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(biometricEnabled, sessionActive, biometricRetry) {
        if (biometricEnabled && !sessionActive) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                BiometricHelper.checkAndShowBiometricPrompt(
                    activity = activity,
                    onSuccess = {
                        viewModel.setSessionActive(true)
                        viewModel.touch()
                    },
                    onError = { biometricRetry++ }
                )
            } else {
                viewModel.setSessionActive(true)
            }
        }
    }

    val isAutoUnlocking by viewModel.isAutoUnlocking.collectAsState()

    val showBiometricGate = biometricEnabled && !sessionActive
    val showPreparing = isAutoUnlocking && encryptionEnabled && !showBiometricGate
    val showPasswordGate = encryptionEnabled && !isUnlocked && !isAutoUnlocking

    when {
        showBiometricGate -> {
            // Schermata di blocco biometrico
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp)
                    .clickable { biometricRetry++ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = stringResource(R.string.physical_check_fingerprint_cd),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(R.string.physical_check_protected_access),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.physical_check_fingerprint_desc),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Button(
                            onClick = { biometricRetry++ },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.physical_check_auth_button))
                        }
                    }
                }
            }
        }
        showPreparing -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                GymLoadingIndicator()
            }
        }
        showPasswordGate -> {
            // Schermata di blocco / Inserimento password
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.physical_check_lock_cd),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(R.string.physical_check_encrypted_vault),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.physical_check_enter_password_desc),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                passwordError = false
                            },
                            label = { Text(stringResource(R.string.physical_check_password_label)) },
                            isError = passwordError,
                            singleLine = true,
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (passwordError) {
                            Text(
                                text = stringResource(R.string.physical_check_password_error),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.unlock(
                                    password = passwordInput,
                                    onSuccess = {
                                        passwordInput = ""
                                        viewModel.touch()
                                    },
                                    onError = {
                                        passwordError = true
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.physical_check_unlock))
                        }

                        TextButton(
                            onClick = { showResetConfirmDialog = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = stringResource(R.string.physical_check_forgot_password),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
        else -> {
            // Schermata principale timeline check fisici
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.physical_check_title), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                        },
                        actions = {
                            if (isSelectionMode && !swipeActionsEnabled) {
                                if (selectedForCompare.size == 1) {
                                    IconButton(onClick = {
                                        val checkId = selectedForCompare.first()
                                        checkToEdit = checks.find { it.id == checkId }
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                                    }
                                }
                                if (selectedForCompare.size == 1) {
                                    IconButton(onClick = {
                                        val checkId = selectedForCompare.first()
                                        checkToDelete = checks.find { it.id == checkId }
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            if (checks.isNotEmpty()) {
                                IconButton(onClick = {
                                    isSelectionMode = !isSelectionMode
                                    if (!isSelectionMode) selectedForCompare = emptySet()
                                }) {
                                    Icon(
                                        imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.Compare,
                                        contentDescription = stringResource(R.string.physical_check_compare_cd)
                                    )
                                }
                            }
                            IconButton(onClick = {
                                viewModel.touch()
                                onNavigateToSettings()
                            }) {
                                Icon(Icons.Default.Security, contentDescription = stringResource(R.string.physical_check_settings_cd))
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (isSelectionMode && selectedForCompare.size == 2) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                val list = selectedForCompare.toList()
                                onNavigateToCompare(list[0], list[1])
                                isSelectionMode = false
                                selectedForCompare = emptySet()
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Compare, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.physical_check_compare_count))
                        }
                    } else if (!isSelectionMode) {
                        FloatingActionButton(
                            onClick = {
                                viewModel.touch()
                                showAddDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = stringResource(R.string.physical_check_add_check_cd))
                        }
                    }
                }
            ) { paddingValues ->
                if (checks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(96.dp)
                            )
                            Text(
                                text = stringResource(R.string.physical_check_no_checks),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.physical_check_no_checks_desc),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(checks) { check ->
                            val dismissState = rememberSwipeToDismissBoxState()

                            LaunchedEffect(dismissState.targetValue) {
                                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                    if (swipeActionsEnabled && !isSelectionMode) {
                                        when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                checkToDelete = check
                                            }
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                checkToEdit = check
                                            }
                                            else -> {}
                                        }
                                    }
                                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                }
                            }

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = swipeActionsEnabled && !isSelectionMode,
                                enableDismissFromEndToStart = swipeActionsEnabled && !isSelectionMode,
                                backgroundContent = {
                                    val direction = dismissState.dismissDirection

                                    val color by animateColorAsState(
                                        when (direction) {
                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            else -> Color.Transparent
                                        },
                                        label = "bg_color"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(color)
                                            .padding(horizontal = 28.dp),
                                        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                    ) {
                                        if (direction == SwipeToDismissBoxValue.EndToStart) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.delete),
                                                tint = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        } else if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = stringResource(R.string.edit),
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            ) {
                                PhysicalCheckCard(
                                    check = check,
                                    viewModel = viewModel,
                                    isSelected = selectedForCompare.contains(check.id),
                                    isSelectionMode = isSelectionMode,
                                    onToggleSelection = {
                                        if (selectedForCompare.contains(check.id)) {
                                            selectedForCompare = selectedForCompare - check.id
                                        } else {
                                            if (selectedForCompare.size < 2) {
                                                selectedForCompare = selectedForCompare + check.id
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.physical_check_max_2), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode && !swipeActionsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isSelectionMode = true
                                            selectedForCompare = setOf(check.id)
                                        }
                                    },
                                    onPhotoClick = { filenames, index ->
                                        fullscreenState = FullscreenState(check.id, filenames, index)
                                    },
                                    onAddPhotoClick = {
                                        addingPhotosForCheckId = check.id
                                        showAddPhotoOptions = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(stringResource(R.string.physical_check_reset_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.physical_check_reset_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        viewModel.resetAllData {
                            Toast.makeText(context, context.getString(R.string.physical_check_encryption_disabled), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.physical_check_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddDialog) {
        AddPhysicalCheckDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { timestamp, peso, note, photos ->
                viewModel.addCheck(timestamp, peso, note, photos) {
                    showAddDialog = false
                }
            },
            viewModel = viewModel
        )
    }

    checkToDelete?.let { check ->
        AlertDialog(
            onDismissRequest = {
                checkToDelete = null
                isSelectionMode = false
                selectedForCompare = emptySet()
            },
            title = { Text(stringResource(R.string.physical_check_delete_title)) },
            text = { Text(stringResource(R.string.physical_check_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCheck(check)
                    checkToDelete = null
                    isSelectionMode = false
                    selectedForCompare = emptySet()
                }) {
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    checkToDelete = null
                    isSelectionMode = false
                    selectedForCompare = emptySet()
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    checkToEdit?.let { check ->
        EditPhysicalCheckDialog(
            check = check,
            viewModel = viewModel,
            onDismiss = {
                checkToEdit = null
                isSelectionMode = false
                selectedForCompare = emptySet()
            },
            onConfirm = { timestamp, peso, note ->
                viewModel.updateCheck(check.id, timestamp, peso, note) {
                    checkToEdit = null
                    isSelectionMode = false
                    selectedForCompare = emptySet()
                }
            }
        )
    }

    fullscreenState?.let { state ->
        Dialog(
            onDismissRequest = { fullscreenState = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PhysicalCheckFullscreenViewer(
                filenames = state.filenames,
                initialIndex = state.initialIndex,
                viewModel = viewModel,
                onDismiss = { fullscreenState = null },
                onDeletePhoto = { index ->
                    val filename = state.filenames[index]
                    viewModel.deletePhotoFromCheck(state.checkId, filename)
                    val remaining = state.filenames.toMutableList().apply { removeAt(index) }
                    if (remaining.isEmpty()) {
                        fullscreenState = null
                    } else {
                        fullscreenState = state.copy(filenames = remaining, initialIndex = index.coerceAtMost(remaining.size - 1))
                    }
                }
            )
        }
    }

    if (showAddPhotoOptions) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddPhotoOptions = false
                addingPhotosForCheckId = null
            },
            sheetState = photoSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.physical_check_add_photos),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OptionItem(
                        icon = Icons.Default.PhotoCamera,
                        label = stringResource(R.string.camera),
                        onClick = {
                            showAddPhotoOptions = false
                            viewModel.setPhotoCaptureStarted()
                            val permission = Manifest.permission.CAMERA
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                val uri = createCheckTempImageUri(context)
                                tempCameraUri = uri
                                addPhotoCameraLauncher.launch(uri)
                            } else {
                                addPhotoPermissionLauncher.launch(permission)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OptionItem(
                        icon = Icons.Default.PhotoLibrary,
                        label = stringResource(R.string.gallery),
                        onClick = {
                            showAddPhotoOptions = false
                            viewModel.setPhotoCaptureStarted()
                            addPhotoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhysicalCheckFullscreenViewer(
    filenames: List<String>,
    initialIndex: Int,
    viewModel: PhysicalCheckViewModel,
    onDismiss: () -> Unit,
    onDeletePhoto: ((Int) -> Unit)? = null
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { filenames.size }
    var isZoomed by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed,
            beyondViewportPageCount = 1
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ZoomableDecryptedImage(
                    filename = filenames[page],
                    viewModel = viewModel,
                    onZoomChanged = { zoomed ->
                        if (pagerState.currentPage == page) {
                            isZoomed = zoomed
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (filenames.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${filenames.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            } else {
                Spacer(modifier = Modifier)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDeletePhoto != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.physical_check_delete_photo_title)) },
            text = { Text(stringResource(R.string.physical_check_delete_photo_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val currentIndex = pagerState.currentPage
                    showDeleteConfirm = false
                    onDeletePhoto?.invoke(currentIndex)
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ZoomableDecryptedImage(
    filename: String,
    viewModel: PhysicalCheckViewModel,
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    val transformableState = rememberTransformableState { _, zoomChange, panChange, _ ->
        val oldScale = scale
        val newScale = (oldScale * zoomChange).coerceIn(1f, 5f)
        scale = newScale

        if (newScale <= 1f) {
            offsetX = 0f
            offsetY = 0f
            return@rememberTransformableState
        }

        val maxX = (imageSize.width * (newScale - 1f)) / 2f
        val maxY = (imageSize.height * (newScale - 1f)) / 2f

        val newOffsetX = offsetX + (panChange.x * newScale)
        val newOffsetY = offsetY + (panChange.y * newScale)

        offsetX = newOffsetX.coerceIn(-maxX, maxX)
        offsetY = newOffsetY.coerceIn(-maxY, maxY)
    }

    LaunchedEffect(scale) {
        onZoomChanged(scale > 1f)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        DecryptedImage(
            filename = filename,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .padding(16.dp)
                .onSizeChanged { imageSize = it }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .transformable(
                    state = transformableState,
                    canPan = { scale > 1f }
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun PhysicalCheckCard(
    check: PhysicalCheckEntity,
    viewModel: PhysicalCheckViewModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onLongClick: () -> Unit,
    onPhotoClick: (List<String>, Int) -> Unit,
    onAddPhotoClick: () -> Unit
) {
    val weightUnit by viewModel.weightUnit.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelection()
                },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = DateFormatter.format(check.timestamp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (check.peso != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = WeightUnitConverter.formatWithUnit(
                                    WeightUnitConverter.convertDisplay(check.peso, weightUnit),
                                    weightUnit
                                ),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (!check.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = check.note,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val filenames = remember(check.fotoFilenames) {
                if (check.fotoFilenames.isEmpty()) emptyList() else check.fotoFilenames.split(",")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayList = remember(filenames, isSelectionMode) {
                    buildList<String?> {
                        addAll(filenames)
                        if (!isSelectionMode) add(null)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = displayList.chunked(3)
                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                        ) {
                            for (name in row) {
                                if (name != null) {
                                    val index = filenames.indexOf(name)
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onPhotoClick(filenames, index) }
                                    ) {
                                        DecryptedImage(
                                            filename = name,
                                            viewModel = viewModel,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { onAddPhotoClick() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = stringResource(R.string.physical_check_add_photos),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (isSelectionMode) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DecryptedImage(
    filename: String,
    viewModel: PhysicalCheckViewModel,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var imageBytes by remember(filename) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(filename) {
        imageBytes = viewModel.getPhotoBytes(filename)
    }

    if (imageBytes != null) {
        val bitmap = remember(imageBytes) {
            val bytes = imageBytes!!
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = modifier,
                contentScale = contentScale
            )
        } else {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPhysicalCheckDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, Float?, String?, List<ByteArray>) -> Unit,
    viewModel: PhysicalCheckViewModel
) {
    val context = LocalContext.current
    val weightUnit by viewModel.weightUnit.collectAsState()
    var weightInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    val photoBytesList = remember { mutableStateListOf<ByteArray>() }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    var fullscreenPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var photoToDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val contentResolver = context.contentResolver
        uris.forEach { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    photoBytesList.add(bytes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModel.setPhotoCaptureCompleted()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { uri ->
                val bytes = com.emanuel5014.trainable.util.ImageStorageUtils.readAndCompressImage(context, uri)
                if (bytes != null) {
                    photoBytesList.add(bytes)
                }
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        showPhotoOptions = false
        viewModel.setPhotoCaptureCompleted()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createCheckTempImageUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            viewModel.setPhotoCaptureCompleted()
            Toast.makeText(context, context.getString(R.string.physical_check_camera_denied), Toast.LENGTH_SHORT).show()
        }
    }

    fun handleCameraClick() {
        viewModel.setPhotoCaptureStarted()
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val uri = createCheckTempImageUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.physical_check_new_check), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable { showDatePicker = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.physical_check_date),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DateFormatter.format(datePickerState.selectedDateMillis ?: System.currentTimeMillis()),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text(stringResource(R.string.physical_check_weight_label).replace(" (kg)", " ($weightUnit)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text(stringResource(R.string.physical_check_notes_label)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { showPhotoOptions = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.physical_check_add_photos))
                }

                if (photoBytesList.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.physical_check_photos_selected, photoBytesList.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(photoBytesList) { bytes ->
                            val itemIndex = photoBytesList.indexOf(bytes)
                            val bitmap = remember(bytes) {
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                            if (bitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { fullscreenPhotoIndex = itemIndex }
                                ) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(18.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .clickable { photoToDeleteIndex = itemIndex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.physical_check_remove_cd),
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (photoBytesList.isEmpty()) {
                        Toast.makeText(context, context.getString(R.string.physical_check_select_photo_toast), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    var peso = weightInput.replace(",", ".").toFloatOrNull()
                    if (peso != null) {
                        peso = WeightUnitConverter.convertStorage(peso, weightUnit)
                    }
                    onConfirm(
                        datePickerState.selectedDateMillis ?: System.currentTimeMillis(),
                        peso,
                        notesInput.takeIf { it.isNotBlank() },
                        photoBytesList.toList()
                    )
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showPhotoOptions) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoOptions = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.physical_check_select_source),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OptionItem(
                        icon = Icons.Default.PhotoCamera,
                        label = stringResource(R.string.camera),
                        onClick = {
                            showPhotoOptions = false
                            handleCameraClick()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OptionItem(
                        icon = Icons.Default.PhotoLibrary,
                        label = stringResource(R.string.gallery),
                        onClick = {
                            showPhotoOptions = false
                            viewModel.setPhotoCaptureStarted()
                            pickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    fullscreenPhotoIndex?.let { index ->
        val bytes = photoBytesList.getOrNull(index)
        if (bytes != null) {
            val bitmap = remember(bytes) {
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            Dialog(
                onDismissRequest = { fullscreenPhotoIndex = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    if (bitmap != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.75f),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (photoBytesList.size > 1) {
                            Text(
                                text = "${index + 1} / ${photoBytesList.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { photoToDeleteIndex = index },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            IconButton(
                                onClick = { fullscreenPhotoIndex = null },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    photoToDeleteIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { photoToDeleteIndex = null },
            title = { Text(stringResource(R.string.physical_check_delete_photo_title)) },
            text = { Text(stringResource(R.string.physical_check_delete_photo_message)) },
            confirmButton = {
                TextButton(onClick = {
                    photoBytesList.removeAt(index)
                    val wasViewingFullscreen = fullscreenPhotoIndex != null
                    if (wasViewingFullscreen) {
                        if (photoBytesList.isEmpty()) {
                            fullscreenPhotoIndex = null
                        } else {
                            fullscreenPhotoIndex = index.coerceAtMost(photoBytesList.lastIndex)
                        }
                    }
                    photoToDeleteIndex = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDeleteIndex = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun OptionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPhysicalCheckDialog(
    check: PhysicalCheckEntity,
    viewModel: PhysicalCheckViewModel,
    onDismiss: () -> Unit,
    onConfirm: (Long, Float?, String?) -> Unit
) {
    val context = LocalContext.current
    val weightUnit by viewModel.weightUnit.collectAsState()
    var weightInput by remember {
        mutableStateOf(
            check.peso?.let {
                WeightUnitConverter.format(WeightUnitConverter.convertDisplay(it, weightUnit))
            } ?: ""
        )
    }
    var notesInput by remember { mutableStateOf(check.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = check.timestamp)

    val existingFilenames = remember(check.id) {
        mutableStateListOf<String>().apply {
            if (check.fotoFilenames.isNotEmpty()) {
                addAll(check.fotoFilenames.split(","))
            }
        }
    }

    var fullscreenPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var photoToDelete by remember { mutableStateOf<String?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val contentResolver = context.contentResolver
        val bytesList = mutableListOf<ByteArray>()
        uris.forEach { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    bytesList.add(inputStream.readBytes())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (bytesList.isNotEmpty()) {
            viewModel.addPhotosToCheck(check.id, bytesList) { newFilenames ->
                existingFilenames.addAll(newFilenames)
            }
        }
        viewModel.setPhotoCaptureCompleted()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { uri ->
                val bytes = com.emanuel5014.trainable.util.ImageStorageUtils.readAndCompressImage(context, uri)
                if (bytes != null) {
                    viewModel.addPhotosToCheck(check.id, listOf(bytes)) { newFilenames ->
                        existingFilenames.addAll(newFilenames)
                    }
                }
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        showPhotoOptions = false
        viewModel.setPhotoCaptureCompleted()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createCheckTempImageUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            viewModel.setPhotoCaptureCompleted()
            Toast.makeText(context, context.getString(R.string.physical_check_camera_denied), Toast.LENGTH_SHORT).show()
        }
    }

    fun handleCameraClick() {
        viewModel.setPhotoCaptureStarted()
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val uri = createCheckTempImageUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.physical_check_edit_check), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable { showDatePicker = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.physical_check_date),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DateFormatter.format(datePickerState.selectedDateMillis ?: check.timestamp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text(stringResource(R.string.physical_check_weight_label).replace(" (kg)", " ($weightUnit)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text(stringResource(R.string.physical_check_notes_label)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (existingFilenames.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.physical_check_photos_count, existingFilenames.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(existingFilenames) { filename ->
                            val itemIndex = existingFilenames.indexOf(filename)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { fullscreenPhotoIndex = itemIndex }
                            ) {
                                DecryptedImage(
                                    filename = filename,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(18.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .clickable { photoToDelete = filename },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { showPhotoOptions = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.physical_check_add_photos))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                var peso = weightInput.replace(",", ".").toFloatOrNull()
                if (peso != null) {
                    peso = WeightUnitConverter.convertStorage(peso, weightUnit)
                }
                onConfirm(
                    datePickerState.selectedDateMillis ?: check.timestamp,
                    peso,
                    notesInput.takeIf { it.isNotBlank() }
                )
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showPhotoOptions) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoOptions = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.physical_check_select_source),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OptionItem(
                        icon = Icons.Default.PhotoCamera,
                        label = stringResource(R.string.camera),
                        onClick = {
                            showPhotoOptions = false
                            handleCameraClick()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OptionItem(
                        icon = Icons.Default.PhotoLibrary,
                        label = stringResource(R.string.gallery),
                        onClick = {
                            showPhotoOptions = false
                            viewModel.setPhotoCaptureStarted()
                            pickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    fullscreenPhotoIndex?.let { index ->
        Dialog(
            onDismissRequest = { fullscreenPhotoIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PhysicalCheckFullscreenViewer(
                filenames = existingFilenames,
                initialIndex = index,
                viewModel = viewModel,
                onDismiss = { fullscreenPhotoIndex = null },
                onDeletePhoto = { deletedIndex ->
                    val filenameToRemove = existingFilenames[deletedIndex]
                    existingFilenames.removeAt(deletedIndex)
                    viewModel.deletePhotoFromCheck(check.id, filenameToRemove)
                    if (existingFilenames.isEmpty()) {
                        fullscreenPhotoIndex = null
                    } else {
                        fullscreenPhotoIndex = deletedIndex.coerceAtMost(existingFilenames.lastIndex)
                    }
                }
            )
        }
    }

    photoToDelete?.let { filename ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text(stringResource(R.string.physical_check_delete_photo_title)) },
            text = { Text(stringResource(R.string.physical_check_delete_photo_message)) },
            confirmButton = {
                TextButton(onClick = {
                    existingFilenames.remove(filename)
                    viewModel.deletePhotoFromCheck(check.id, filename)
                    photoToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun createCheckTempImageUri(context: Context): Uri {
    val tempFile = File(context.cacheDir, "check_temp_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}
