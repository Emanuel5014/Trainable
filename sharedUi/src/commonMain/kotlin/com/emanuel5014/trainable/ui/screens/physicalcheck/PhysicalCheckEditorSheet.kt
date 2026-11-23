package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.camera
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.close
import com.emanuel5014.trainable.sharedui.resources.delete
import com.emanuel5014.trainable.sharedui.resources.gallery
import com.emanuel5014.trainable.sharedui.resources.ok
import com.emanuel5014.trainable.sharedui.resources.physical_check_date
import com.emanuel5014.trainable.sharedui.resources.physical_check_delete_photo_message
import com.emanuel5014.trainable.sharedui.resources.physical_check_delete_photo_title
import com.emanuel5014.trainable.sharedui.resources.physical_check_edit_check
import com.emanuel5014.trainable.sharedui.resources.physical_check_new_check
import com.emanuel5014.trainable.sharedui.resources.physical_check_notes_label
import com.emanuel5014.trainable.sharedui.resources.physical_check_remove_cd
import com.emanuel5014.trainable.sharedui.resources.physical_check_select_photo_toast
import com.emanuel5014.trainable.sharedui.resources.physical_check_weight
import com.emanuel5014.trainable.sharedui.resources.save
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.components.GymButtonText
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.util.fullDate
import com.emanuel5014.trainable.util.WeightUnitConverter
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/** A photo in the editor: just taken or picked (a new check keeps them until saved), or already stored. */
private sealed interface EditorPhoto {
    class New(val bytes: ByteArray) : EditorPhoto
    class Stored(val filename: String) : EditorPhoto
}

private val ThumbShape = RoundedCornerShape(20.dp)

/**
 * Adds a check ([existing] null) or edits one, in a bottom sheet: the photos first, with the camera and the gallery
 * right next to them, then the date, the weight and the note. A new check needs at least one photo and saves it all
 * at once; on an existing check, photos are added and removed straight away (as before), the rest on save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhysicalCheckEditorSheet(
    existing: PhysicalCheckEntity?,
    model: PhysicalCheckModel,
    onDismiss: () -> Unit,
    onSave: (timestamp: Long, pesoKg: Float?, note: String?, newPhotos: List<ByteArray>) -> Unit
) {
    val weightUnit by model.weightUnit.collectAsState()
    val showMessage = LocalShowMessage.current
    val selectPhotoText = stringResource(Res.string.physical_check_select_photo_toast)
    val platform = LocalPhysicalCheckPlatform.current

    var weightInput by remember {
        mutableStateOf(existing?.peso?.let { WeightUnitConverter.format(WeightUnitConverter.convertDisplay(it, weightUnit)) } ?: "")
    }
    var notesInput by remember { mutableStateOf(existing?.note ?: "") }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = existing?.timestamp ?: Clock.System.now().toEpochMilliseconds()
    )
    var showDatePicker by remember { mutableStateOf(false) }
    val photos = remember {
        mutableStateListOf<EditorPhoto>().apply { existing?.photoFilenames()?.forEach { add(EditorPhoto.Stored(it)) } }
    }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    var photoToRemove by remember { mutableStateOf<EditorPhoto?>(null) }

    // Every taken or picked photo goes through the crop/rotate editor first, one after the other
    val editQueue = remember { mutableStateListOf<ByteArray>() }
    val editedBatch = remember { mutableListOf<ByteArray>() }
    fun startEditing(list: List<ByteArray>) {
        if (list.isEmpty()) return
        editedBatch.clear()
        editQueue.clear()
        editQueue.addAll(list)
    }
    fun onBatchEdited() {
        val batch = editedBatch.toList()
        editedBatch.clear()
        if (existing == null) {
            batch.forEach { photos.add(EditorPhoto.New(it)) }
        } else {
            model.addPhotosToCheck(existing.id, batch) { names -> names.forEach { photos.add(EditorPhoto.Stored(it)) } }
        }
    }
    val capture = platform.rememberPhotoCapture(
        onPicked = { list ->
            startEditing(list)
            model.setPhotoCaptureCompleted()
        },
        onCameraResult = { success, bytes ->
            if (success && bytes != null) startEditing(listOf(bytes))
            model.setPhotoCaptureCompleted()
        },
        onCameraDenied = { model.setPhotoCaptureCompleted() }
    )

    fun removePhoto(photo: EditorPhoto) {
        photos.remove(photo)
        if (photo is EditorPhoto.Stored && existing != null) model.deletePhotoFromCheck(existing.id, photo.filename)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        contentColor = OnSurface,
        tonalElevation = 0.dp,
        dragHandle = { PhysicalCheckSheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val sidePadding = ResponsiveSize.horizontalPadding
            Text(
                text = stringResource(if (existing == null) Res.string.physical_check_new_check else Res.string.physical_check_edit_check),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = OnSurface,
                modifier = Modifier.padding(horizontal = sidePadding)
            )

            // The photos, then the camera and the gallery
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = sidePadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(photos, key = { _, p -> p.hashCode() }) { index, photo ->
                    EditorThumb(photo = photo, model = model, onOpen = { previewIndex = index }, onRemove = { photoToRemove = photo })
                }
                item(key = "camera") {
                    SourceTile(Icons.Rounded.PhotoCamera, stringResource(Res.string.camera)) {
                        model.setPhotoCaptureStarted()
                        capture.takePhoto()
                    }
                }
                item(key = "gallery") {
                    SourceTile(Icons.Rounded.PhotoLibrary, stringResource(Res.string.gallery)) {
                        model.setPhotoCaptureStarted()
                        capture.pickPhotos()
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = sidePadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerHigh)
                        .clickable(role = Role.Button) { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = Primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.physical_check_date),
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = fullDate(datePickerState.selectedDateMillis ?: existing?.timestamp ?: Clock.System.now().toEpochMilliseconds()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                }

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OutlineVariant,
                    focusedLabelColor = Primary,
                    cursorColor = Primary
                )
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text(stringResource(Res.string.physical_check_weight)) },
                    suffix = { Text(weightUnit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text(stringResource(Res.string.physical_check_notes_label)) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                GymButton(
                    onClick = {
                        val newPhotos = photos.filterIsInstance<EditorPhoto.New>().map { it.bytes }
                        if (existing == null && newPhotos.isEmpty()) {
                            showMessage(selectPhotoText)
                            return@GymButton
                        }
                        val peso = weightInput.replace(",", ".").toFloatOrNull()?.let { WeightUnitConverter.convertStorage(it, weightUnit) }
                        onSave(
                            datePickerState.selectedDateMillis ?: existing?.timestamp ?: Clock.System.now().toEpochMilliseconds(),
                            peso,
                            notesInput.takeIf { it.isNotBlank() },
                            newPhotos
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    GymButtonText(stringResource(Res.string.save), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(Res.string.ok)) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(Res.string.cancel)) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    previewIndex?.let { index ->
        val stored = photos.filterIsInstance<EditorPhoto.Stored>()
        val photo = photos.getOrNull(index)
        Dialog(onDismissRequest = { previewIndex = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            when (photo) {
                is EditorPhoto.Stored -> PhysicalCheckFullscreenViewer(
                    filenames = stored.map { it.filename },
                    initialIndex = stored.indexOf(photo).coerceAtLeast(0),
                    viewModel = model,
                    onDismiss = { previewIndex = null },
                    onDeletePhoto = { storedIndex ->
                        stored.getOrNull(storedIndex)?.let { removePhoto(it) }
                        previewIndex = null
                    }
                )
                is EditorPhoto.New -> NewPhotoPreview(
                    bytes = photo.bytes,
                    onDismiss = { previewIndex = null },
                    onDelete = {
                        previewIndex = null
                        photoToRemove = photo
                    }
                )
                null -> previewIndex = null
            }
        }
    }

    photoToRemove?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToRemove = null },
            title = { Text(stringResource(Res.string.physical_check_delete_photo_title)) },
            text = { Text(stringResource(Res.string.physical_check_delete_photo_message)) },
            confirmButton = {
                TextButton(onClick = {
                    removePhoto(photo)
                    photoToRemove = null
                }) {
                    Text(stringResource(Res.string.delete), color = Error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = { TextButton(onClick = { photoToRemove = null }) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    if (editQueue.isNotEmpty()) {
        platform.ImageEditor(
            imageBytes = editQueue.first(),
            onDismiss = {
                // Cancelling one photo drops the rest of the batch, as before
                editQueue.clear()
                editedBatch.clear()
            },
            onConfirm = { edited ->
                editQueue.removeAt(0)
                editedBatch.add(edited)
                if (editQueue.isEmpty()) onBatchEdited()
            }
        )
    }
}

/** A photo of the editor with its remove button. */
@Composable
private fun EditorThumb(photo: EditorPhoto, model: PhysicalCheckModel, onOpen: () -> Unit, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 128.dp)
            .clip(ThumbShape)
            .background(SurfaceContainerHigh)
            .clickable(role = Role.Image, onClick = onOpen)
    ) {
        when (photo) {
            is EditorPhoto.Stored -> DecryptedImage(photo.filename, model, Modifier.fillMaxSize(), ContentScale.Crop)
            is EditorPhoto.New -> {
                val bitmap = remember(photo) { decodePhotoBitmap(photo.bytes) }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(role = Role.Button, onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(Res.string.physical_check_remove_cd),
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Camera or gallery, as tall as a photo so the row reads as one strip. */
@Composable
private fun SourceTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(width = 96.dp, height = 128.dp)
            .clip(ThumbShape)
            .background(Primary.copy(alpha = 0.12f))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Primary, maxLines = 1)
    }
}

/** A just-taken photo on black, with delete and close. */
@Composable
private fun NewPhotoPreview(bytes: ByteArray, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val bitmap = remember(bytes) { decodePhotoBitmap(bytes) }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onDelete, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(Res.string.delete), tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.close), tint = Color.White)
            }
        }
    }
}
