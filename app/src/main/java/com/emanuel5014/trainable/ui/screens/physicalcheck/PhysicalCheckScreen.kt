package com.emanuel5014.trainable.ui.screens.physicalcheck

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.ui.util.DateFormatter
import java.io.File
import java.util.Calendar

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

    var showAddDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    var selectedForCompare by remember { mutableStateOf(setOf<Int>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    data class FullscreenState(val checkId: Int, val filenames: List<String>, val initialIndex: Int)
    var fullscreenState by remember { mutableStateOf<FullscreenState?>(null) }

    var addingPhotosForCheckId by remember { mutableStateOf<Int?>(null) }
    var showAddPhotoOptions by remember { mutableStateOf(false) }
    val photoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val addPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val checkId = addingPhotosForCheckId ?: return@rememberLauncherForActivityResult
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
    }

    val addPhotoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val checkId = addingPhotosForCheckId ?: return@rememberLauncherForActivityResult
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
    }

    val addPhotoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createCheckTempImageUri(context)
            tempCameraUri = uri
            addPhotoCameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permesso fotocamera negato", Toast.LENGTH_SHORT).show()
        }
    }

    if (encryptionEnabled && !isUnlocked) {
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
                        contentDescription = "Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Cassaforte Cifrata",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Inserisci la password per decifrare i tuoi check fisici.",
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
                        label = { Text("Password") },
                        isError = passwordError,
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (passwordError) {
                        Text(
                            text = "Password errata. Riprova.",
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
                                },
                                onError = {
                                    passwordError = true
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sblocca")
                    }
                }
            }
        }
    } else {
        // Schermata principale timeline check fisici
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Check Fisici", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (checks.isNotEmpty()) {
                            IconButton(onClick = {
                                isSelectionMode = !isSelectionMode
                                if (!isSelectionMode) selectedForCompare = emptySet()
                            }) {
                                Icon(
                                    imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.Compare,
                                    contentDescription = "Compare"
                                )
                            }
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Security, contentDescription = "Settings")
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
                        Text("Confronta (2)")
                    }
                } else if (!isSelectionMode) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Aggiungi Check")
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
                            text = "Nessun check registrato",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Premi il pulsante in basso per aggiungere le tue prime foto di check fisici.",
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
                                        Toast.makeText(context, "Puoi selezionare massimo 2 check", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDelete = { viewModel.deleteCheck(check) },
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

    if (showAddDialog) {
        AddPhysicalCheckDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { timestamp, peso, note, photos ->
                viewModel.addCheck(timestamp, peso, note, photos) {
                    showAddDialog = false
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
                onDismiss = { fullscreenState = null }
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
                    text = "Aggiungi foto",
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
                        label = "Fotocamera",
                        onClick = {
                            showAddPhotoOptions = false
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
                        label = "Galleria",
                        onClick = {
                            showAddPhotoOptions = false
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
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { filenames.size }
    var isZoomed by remember { mutableStateOf(false) }

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
            IconButton(
                onClick = onDismiss,
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
    onDelete: () -> Unit,
    onPhotoClick: (List<String>, Int) -> Unit,
    onAddPhotoClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isSelectionMode) { onToggleSelection() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    if (isSelectionMode) {
                        Checkbox(checked = isSelected, onCheckedChange = { onToggleSelection() })
                    }
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
                                text = "${check.peso} kg",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (!isSelectionMode) {
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Elimina Check") },
                            text = { Text("Sei sicuro di voler eliminare questo check fisico? Le foto collegate verranno rimosse permanentemente.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    onDelete()
                                    showDeleteConfirm = false
                                }) {
                                    Text("Elimina", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Annulla")
                                }
                            }
                        )
                    }
                }
            }

            if (!check.note.isNullOrBlank()) {
                Text(
                    text = check.note,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val filenames = remember(check.fotoFilenames) {
                if (check.fotoFilenames.isEmpty()) emptyList() else check.fotoFilenames.split(",")
            }

            if (filenames.isNotEmpty() || !isSelectionMode) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filenames.withIndex().toList()) { (index, filename) ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPhotoClick(filenames, index) }
                        ) {
                            DecryptedImage(
                                filename = filename,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    if (!isSelectionMode) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onAddPhotoClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Aggiungi foto",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
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
    onConfirm: (Long, Float?, String?, List<ByteArray>) -> Unit
) {
    val context = LocalContext.current
    var weightInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    val photoBytesList = remember { mutableStateListOf<ByteArray>() }

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
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createCheckTempImageUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permesso fotocamera negato", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleCameraClick() {
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
        title = { Text("Nuovo Check Fisico", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Peso corporeo (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Note / Sensazioni") },
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
                    Text("Aggiungi Foto")
                }

                if (photoBytesList.isNotEmpty()) {
                    Text(
                        text = "Foto selezionate: ${photoBytesList.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(photoBytesList) { bytes ->
                            val bitmap = remember(bytes) {
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                            if (bitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { photoBytesList.remove(bytes) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
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
                        Toast.makeText(context, "Seleziona almeno una foto", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val peso = weightInput.replace(",", ".").toFloatOrNull()
                    onConfirm(
                        System.currentTimeMillis(),
                        peso,
                        notesInput.takeIf { it.isNotBlank() },
                        photoBytesList.toList()
                    )
                }
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )

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
                    text = "Seleziona origine",
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
                        label = "Fotocamera",
                        onClick = {
                            showPhotoOptions = false
                            handleCameraClick()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OptionItem(
                        icon = Icons.Default.PhotoLibrary,
                        label = "Galleria",
                        onClick = {
                            showPhotoOptions = false
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

private fun createCheckTempImageUri(context: Context): Uri {
    val tempFile = File(context.cacheDir, "check_temp_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}
