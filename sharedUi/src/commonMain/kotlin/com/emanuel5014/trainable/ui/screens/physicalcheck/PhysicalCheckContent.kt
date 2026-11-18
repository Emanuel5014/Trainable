package com.emanuel5014.trainable.ui.screens.physicalcheck

import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.physical_check_max_2
import com.emanuel5014.trainable.sharedui.resources.physical_check_encryption_disabled
import com.emanuel5014.trainable.sharedui.resources.physical_check_select_photo_toast
import com.emanuel5014.trainable.sharedui.resources.back
import com.emanuel5014.trainable.sharedui.resources.camera
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.close
import com.emanuel5014.trainable.sharedui.resources.delete
import com.emanuel5014.trainable.sharedui.resources.edit
import com.emanuel5014.trainable.sharedui.resources.gallery
import com.emanuel5014.trainable.sharedui.resources.ok
import com.emanuel5014.trainable.sharedui.resources.physical_check_add_check_cd
import com.emanuel5014.trainable.sharedui.resources.physical_check_add_photos
import com.emanuel5014.trainable.sharedui.resources.physical_check_auth_button
import com.emanuel5014.trainable.sharedui.resources.physical_check_compare_cd
import com.emanuel5014.trainable.sharedui.resources.physical_check_compare_count
import com.emanuel5014.trainable.sharedui.resources.physical_check_date
import com.emanuel5014.trainable.sharedui.resources.physical_check_delete_message
import com.emanuel5014.trainable.sharedui.resources.physical_check_delete_photo_message
import com.emanuel5014.trainable.sharedui.resources.physical_check_delete_photo_title
import com.emanuel5014.trainable.sharedui.resources.physical_check_delete_title
import com.emanuel5014.trainable.sharedui.resources.physical_check_edit_check
import com.emanuel5014.trainable.sharedui.resources.physical_check_encrypted_vault
import com.emanuel5014.trainable.sharedui.resources.physical_check_enter_password_desc
import com.emanuel5014.trainable.sharedui.resources.physical_check_fingerprint_cd
import com.emanuel5014.trainable.sharedui.resources.physical_check_fingerprint_desc
import com.emanuel5014.trainable.sharedui.resources.physical_check_forgot_password
import com.emanuel5014.trainable.sharedui.resources.physical_check_lock_cd
import com.emanuel5014.trainable.sharedui.resources.physical_check_new_check
import com.emanuel5014.trainable.sharedui.resources.physical_check_no_checks
import com.emanuel5014.trainable.sharedui.resources.physical_check_no_checks_desc
import com.emanuel5014.trainable.sharedui.resources.physical_check_notes_label
import com.emanuel5014.trainable.sharedui.resources.physical_check_password_error
import com.emanuel5014.trainable.sharedui.resources.physical_check_password_label
import com.emanuel5014.trainable.sharedui.resources.physical_check_photos_count
import com.emanuel5014.trainable.sharedui.resources.physical_check_photos_selected
import com.emanuel5014.trainable.sharedui.resources.physical_check_protected_access
import com.emanuel5014.trainable.sharedui.resources.physical_check_remove_cd
import com.emanuel5014.trainable.sharedui.resources.physical_check_reset_confirm
import com.emanuel5014.trainable.sharedui.resources.physical_check_reset_message
import com.emanuel5014.trainable.sharedui.resources.physical_check_reset_title
import com.emanuel5014.trainable.sharedui.resources.physical_check_select_source
import com.emanuel5014.trainable.sharedui.resources.physical_check_settings_cd
import com.emanuel5014.trainable.sharedui.resources.physical_check_title
import com.emanuel5014.trainable.sharedui.resources.physical_check_unlock
import com.emanuel5014.trainable.sharedui.resources.physical_check_weight_label
import com.emanuel5014.trainable.sharedui.resources.save
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import kotlin.time.Clock

import com.emanuel5014.trainable.ui.glass.actionGlass
import com.emanuel5014.trainable.ui.glass.actionGlassContainer
import com.emanuel5014.trainable.ui.glass.actionGlassElevation
import com.emanuel5014.trainable.ui.glass.actionGlassSource
import com.emanuel5014.trainable.ui.glass.rememberActionGlassSource
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhysicalCheckContent(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCompare: (Int, Int) -> Unit,
    viewModel: PhysicalCheckModel
) {
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

    val swipeActionsEnabled by viewModel.swipeActionsEnabled.collectAsState(initial = true)

    data class FullscreenState(val checkId: Int, val filenames: List<String>, val initialIndex: Int)
    var fullscreenState by remember { mutableStateOf<FullscreenState?>(null) }

    var addingPhotosForCheckId by remember { mutableStateOf<Int?>(null) }
    var showAddPhotoOptions by remember { mutableStateOf(false) }
    var checkToEdit by remember { mutableStateOf<PhysicalCheckEntity?>(null) }
    val photoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val topPhotoEditQueue = remember { mutableStateListOf<ByteArray>() }
    var topOnEditFinished by remember { mutableStateOf<((ByteArray) -> Unit)?>(null) }

    val platform = LocalPhysicalCheckPlatform.current
    val addPhotoCapture = platform.rememberPhotoCapture(
        onPicked = { bytesList ->
            val checkId = addingPhotosForCheckId
            if (checkId == null) {
                viewModel.setPhotoCaptureCompleted()
                return@rememberPhotoCapture
            }
            if (bytesList.isNotEmpty()) {
                val editedList = mutableListOf<ByteArray>()
                topPhotoEditQueue.clear()
                topPhotoEditQueue.addAll(bytesList)
                topOnEditFinished = { editedBytes ->
                    editedList.add(editedBytes)
                    if (topPhotoEditQueue.isEmpty()) {
                        viewModel.addPhotosToCheck(checkId, editedList)
                        topOnEditFinished = null
                    }
                }
            }
            addingPhotosForCheckId = null
            viewModel.setPhotoCaptureCompleted()
        },
        onCameraResult = { success, bytes ->
            if (success) {
                val checkId = addingPhotosForCheckId
                if (checkId == null) {
                    viewModel.setPhotoCaptureCompleted()
                    return@rememberPhotoCapture
                }
                if (bytes != null) {
                    val editedList = mutableListOf<ByteArray>()
                    topPhotoEditQueue.clear()
                    topPhotoEditQueue.add(bytes)
                    topOnEditFinished = { editedBytes ->
                        editedList.add(editedBytes)
                        if (topPhotoEditQueue.isEmpty()) {
                            viewModel.addPhotosToCheck(checkId, editedList)
                            topOnEditFinished = null
                        }
                    }
                }
            }
            addingPhotosForCheckId = null
            showAddPhotoOptions = false
            viewModel.setPhotoCaptureCompleted()
        },
        onCameraDenied = { viewModel.setPhotoCaptureCompleted() }
    )
    val biometricPrompt = platform.rememberBiometricPrompt()
    val showMessage = LocalShowMessage.current
    val maxTwoText = stringResource(Res.string.physical_check_max_2)
    val encryptionDisabledText = stringResource(Res.string.physical_check_encryption_disabled)

    LaunchedEffect(biometricEnabled, sessionActive, biometricRetry) {
        if (biometricEnabled && !sessionActive) {
            if (biometricPrompt != null) {
                biometricPrompt(
                    {
                        viewModel.setSessionActive(true)
                        viewModel.touch()
                    },
                    { biometricRetry++ }
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
                            contentDescription = stringResource(Res.string.physical_check_fingerprint_cd),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(Res.string.physical_check_protected_access),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(Res.string.physical_check_fingerprint_desc),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Button(
                            onClick = { biometricRetry++ },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(Res.string.physical_check_auth_button))
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
                    .imePadding()
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
                            contentDescription = stringResource(Res.string.physical_check_lock_cd),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(Res.string.physical_check_encrypted_vault),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(Res.string.physical_check_enter_password_desc),
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
                            label = { Text(stringResource(Res.string.physical_check_password_label)) },
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
                                text = stringResource(Res.string.physical_check_password_error),
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
                            Text(stringResource(Res.string.physical_check_unlock))
                        }

                        TextButton(
                            onClick = { showResetConfirmDialog = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = stringResource(Res.string.physical_check_forgot_password),
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
            val actionGlass = rememberActionGlassSource()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(Res.string.physical_check_title), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                            }
                        },
                        actions = {
                            if (isSelectionMode && !swipeActionsEnabled) {
                                if (selectedForCompare.size == 1) {
                                    IconButton(onClick = {
                                        val checkId = selectedForCompare.first()
                                        checkToEdit = checks.find { it.id == checkId }
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
                                    }
                                }
                                if (selectedForCompare.size == 1) {
                                    IconButton(onClick = {
                                        val checkId = selectedForCompare.first()
                                        checkToDelete = checks.find { it.id == checkId }
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(Res.string.delete),
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
                                        contentDescription = stringResource(Res.string.physical_check_compare_cd)
                                    )
                                }
                            }
                            IconButton(onClick = {
                                viewModel.touch()
                                onNavigateToSettings()
                            }) {
                                Icon(Icons.Default.Security, contentDescription = stringResource(Res.string.physical_check_settings_cd))
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
                            containerColor = actionGlassContainer(actionGlass, MaterialTheme.colorScheme.primary),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = actionGlassElevation(actionGlass),
                            modifier = Modifier.actionGlass(actionGlass, MaterialTheme.colorScheme.primary, cornerRadius = 16.dp)
                        ) {
                            Icon(Icons.Default.Compare, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.physical_check_compare_count))
                        }
                    } else if (!isSelectionMode) {
                        FloatingActionButton(
                            onClick = {
                                viewModel.touch()
                                showAddDialog = true
                            },
                            containerColor = actionGlassContainer(actionGlass, MaterialTheme.colorScheme.primary),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = actionGlassElevation(actionGlass),
                            modifier = Modifier.actionGlass(actionGlass, MaterialTheme.colorScheme.primary, cornerRadius = 16.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = stringResource(Res.string.physical_check_add_check_cd))
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
                                text = stringResource(Res.string.physical_check_no_checks),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(Res.string.physical_check_no_checks_desc),
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
                            .actionGlassSource(actionGlass)
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
                                                contentDescription = stringResource(Res.string.delete),
                                                tint = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        } else if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = stringResource(Res.string.edit),
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
                                                showMessage(maxTwoText)
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
            title = { Text(stringResource(Res.string.physical_check_delete_title)) },
            text = { Text(stringResource(Res.string.physical_check_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCheck(check)
                    checkToDelete = null
                    isSelectionMode = false
                    selectedForCompare = emptySet()
                }) {
                    Text(stringResource(Res.string.delete), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    checkToDelete = null
                    isSelectionMode = false
                    selectedForCompare = emptySet()
                }) {
                    Text(stringResource(Res.string.cancel))
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
                    text = stringResource(Res.string.physical_check_add_photos),
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
                        label = stringResource(Res.string.camera),
                        onClick = {
                            showAddPhotoOptions = false
                            viewModel.setPhotoCaptureStarted()
                            addPhotoCapture.takePhoto()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OptionItem(
                        icon = Icons.Default.PhotoLibrary,
                        label = stringResource(Res.string.gallery),
                        onClick = {
                            showAddPhotoOptions = false
                            viewModel.setPhotoCaptureStarted()
                            addPhotoCapture.pickPhotos()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (topPhotoEditQueue.isNotEmpty() && topOnEditFinished != null) {
        platform.ImageEditor(
            imageBytes = topPhotoEditQueue.first(),
            onDismiss = {
                topPhotoEditQueue.clear()
                topOnEditFinished = null
            },
            onConfirm = { editedBytes ->
                val callback = topOnEditFinished
                topPhotoEditQueue.removeAt(0)
                callback?.invoke(editedBytes)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhysicalCheckFullscreenViewer(
    filenames: List<String>,
    initialIndex: Int,
    viewModel: PhysicalCheckModel,
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
                            contentDescription = stringResource(Res.string.delete),
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
                        contentDescription = stringResource(Res.string.close),
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.physical_check_delete_photo_title)) },
            text = { Text(stringResource(Res.string.physical_check_delete_photo_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val currentIndex = pagerState.currentPage
                    showDeleteConfirm = false
                    onDeletePhoto?.invoke(currentIndex)
                }) {
                    Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ZoomableDecryptedImage(
    filename: String,
    viewModel: PhysicalCheckModel,
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
    viewModel: PhysicalCheckModel,
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
                                    WeightUnitConverter.convertDisplay(check.peso!!, weightUnit),
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
                    text = check.note.orEmpty(),
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
                                            contentDescription = stringResource(Res.string.physical_check_add_photos),
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
    viewModel: PhysicalCheckModel,
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
            decodePhotoBitmap(bytes)
        }
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
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
    viewModel: PhysicalCheckModel
) {
    val weightUnit by viewModel.weightUnit.collectAsState()
    var weightInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    val photoBytesList = remember { mutableStateListOf<ByteArray>() }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds())

    var fullscreenPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var photoToDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dialogPhotoEditQueue = remember { mutableStateListOf<ByteArray>() }
    var dialogOnEditFinished by remember { mutableStateOf<((ByteArray) -> Unit)?>(null) }

    val photoCapture = LocalPhysicalCheckPlatform.current.rememberPhotoCapture(
        onPicked = { bytesList ->
            if (bytesList.isNotEmpty()) {
                dialogPhotoEditQueue.clear()
                dialogPhotoEditQueue.addAll(bytesList)
                dialogOnEditFinished = { editedBytes ->
                    photoBytesList.add(editedBytes)
                    if (dialogPhotoEditQueue.isEmpty()) {
                        dialogOnEditFinished = null
                    }
                }
            }
            viewModel.setPhotoCaptureCompleted()
        },
        onCameraResult = { success, bytes ->
            if (success) {
                if (bytes != null) {
                    dialogPhotoEditQueue.clear()
                    dialogPhotoEditQueue.add(bytes)
                    dialogOnEditFinished = { editedBytes ->
                        photoBytesList.add(editedBytes)
                        if (dialogPhotoEditQueue.isEmpty()) {
                            dialogOnEditFinished = null
                        }
                    }
                }
            }
            showPhotoOptions = false
            viewModel.setPhotoCaptureCompleted()
        },
        onCameraDenied = { viewModel.setPhotoCaptureCompleted() }
    )

    fun handleCameraClick() {
        viewModel.setPhotoCaptureStarted()
        photoCapture.takePhoto()
    }

    val showMessage = LocalShowMessage.current
    val selectPhotoText = stringResource(Res.string.physical_check_select_photo_toast)

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.physical_check_new_check), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
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
                            text = stringResource(Res.string.physical_check_date),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DateFormatter.format(datePickerState.selectedDateMillis ?: Clock.System.now().toEpochMilliseconds()),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text(stringResource(Res.string.physical_check_weight_label).replace(" (kg)", " ($weightUnit)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text(stringResource(Res.string.physical_check_notes_label)) },
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
                    Text(stringResource(Res.string.physical_check_add_photos))
                }

                if (photoBytesList.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.physical_check_photos_selected, photoBytesList.size),
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
                                decodePhotoBitmap(bytes)
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
                                        bitmap = bitmap,
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
                                            contentDescription = stringResource(Res.string.physical_check_remove_cd),
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
                        showMessage(selectPhotoText)
                        return@Button
                    }
                    var peso = weightInput.replace(",", ".").toFloatOrNull()
                    if (peso != null) {
                        peso = WeightUnitConverter.convertStorage(peso, weightUnit)
                    }
                    onConfirm(
                        datePickerState.selectedDateMillis ?: Clock.System.now().toEpochMilliseconds(),
                        peso,
                        notesInput.takeIf { it.isNotBlank() },
                        photoBytesList.toList()
                    )
                }
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.cancel))
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
                    text = stringResource(Res.string.physical_check_select_source),
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
                        label = stringResource(Res.string.camera),
                        onClick = {
                            showPhotoOptions = false
                            handleCameraClick()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OptionItem(
                        icon = Icons.Default.PhotoLibrary,
                        label = stringResource(Res.string.gallery),
                        onClick = {
                            showPhotoOptions = false
                            viewModel.setPhotoCaptureStarted()
                            photoCapture.pickPhotos()
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
                decodePhotoBitmap(bytes)
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
                                bitmap = bitmap,
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
            title = { Text(stringResource(Res.string.physical_check_delete_photo_title)) },
            text = { Text(stringResource(Res.string.physical_check_delete_photo_message)) },
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
                    Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDeleteIndex = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (dialogPhotoEditQueue.isNotEmpty() && dialogOnEditFinished != null) {
        LocalPhysicalCheckPlatform.current.ImageEditor(
            imageBytes = dialogPhotoEditQueue.first(),
            onDismiss = {
                dialogPhotoEditQueue.clear()
                dialogOnEditFinished = null
            },
            onConfirm = { editedBytes ->
                val callback = dialogOnEditFinished
                dialogPhotoEditQueue.removeAt(0)
                callback?.invoke(editedBytes)
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
    viewModel: PhysicalCheckModel,
    onDismiss: () -> Unit,
    onConfirm: (Long, Float?, String?) -> Unit
) {
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
    val editPhotoEditQueue = remember { mutableStateListOf<ByteArray>() }
    var editOnEditFinished by remember { mutableStateOf<((ByteArray) -> Unit)?>(null) }

    val photoCapture = LocalPhysicalCheckPlatform.current.rememberPhotoCapture(
        onPicked = { bytesList ->
            if (bytesList.isNotEmpty()) {
                val editedList = mutableListOf<ByteArray>()
                editPhotoEditQueue.clear()
                editPhotoEditQueue.addAll(bytesList)
                editOnEditFinished = { editedBytes ->
                    editedList.add(editedBytes)
                    if (editPhotoEditQueue.isEmpty()) {
                        viewModel.addPhotosToCheck(check.id, editedList) { newFilenames ->
                            existingFilenames.addAll(newFilenames)
                        }
                        editOnEditFinished = null
                    }
                }
            }
            viewModel.setPhotoCaptureCompleted()
        },
        onCameraResult = { success, bytes ->
            if (success) {
                if (bytes != null) {
                    val editedList = mutableListOf<ByteArray>()
                    editPhotoEditQueue.clear()
                    editPhotoEditQueue.add(bytes)
                    editOnEditFinished = { editedBytes ->
                        editedList.add(editedBytes)
                        if (editPhotoEditQueue.isEmpty()) {
                            viewModel.addPhotosToCheck(check.id, editedList) { newFilenames ->
                                existingFilenames.addAll(newFilenames)
                            }
                            editOnEditFinished = null
                        }
                    }
                }
            }
            showPhotoOptions = false
            viewModel.setPhotoCaptureCompleted()
        },
        onCameraDenied = { viewModel.setPhotoCaptureCompleted() }
    )

    fun handleCameraClick() {
        viewModel.setPhotoCaptureStarted()
        photoCapture.takePhoto()
    }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.physical_check_edit_check), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
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
                            text = stringResource(Res.string.physical_check_date),
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
                    label = { Text(stringResource(Res.string.physical_check_weight_label).replace(" (kg)", " ($weightUnit)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text(stringResource(Res.string.physical_check_notes_label)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (existingFilenames.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.physical_check_photos_count, existingFilenames.size),
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
                    Text(stringResource(Res.string.physical_check_add_photos))
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
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.cancel))
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
                    text = stringResource(Res.string.physical_check_select_source),
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
                        label = stringResource(Res.string.camera),
                        onClick = {
                            showPhotoOptions = false
                            handleCameraClick()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OptionItem(
                        icon = Icons.Default.PhotoLibrary,
                        label = stringResource(Res.string.gallery),
                        onClick = {
                            showPhotoOptions = false
                            viewModel.setPhotoCaptureStarted()
                            photoCapture.pickPhotos()
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
            title = { Text(stringResource(Res.string.physical_check_delete_photo_title)) },
            text = { Text(stringResource(Res.string.physical_check_delete_photo_message)) },
            confirmButton = {
                TextButton(onClick = {
                    existingFilenames.remove(filename)
                    viewModel.deletePhotoFromCheck(check.id, filename)
                    photoToDelete = null
                }) {
                    Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (editPhotoEditQueue.isNotEmpty() && editOnEditFinished != null) {
        LocalPhysicalCheckPlatform.current.ImageEditor(
            imageBytes = editPhotoEditQueue.first(),
            onDismiss = {
                editPhotoEditQueue.clear()
                editOnEditFinished = null
            },
            onConfirm = { editedBytes ->
                val callback = editOnEditFinished
                editPhotoEditQueue.removeAt(0)
                callback?.invoke(editedBytes)
            }
        )
    }
}
