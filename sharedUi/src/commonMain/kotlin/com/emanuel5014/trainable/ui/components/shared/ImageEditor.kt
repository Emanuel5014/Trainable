package com.emanuel5014.trainable.ui.components.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material3.*
import com.emanuel5014.trainable.ui.components.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.crop_aspect_16_9
import com.emanuel5014.trainable.sharedui.resources.crop_aspect_1_1
import com.emanuel5014.trainable.sharedui.resources.crop_aspect_4_3
import com.emanuel5014.trainable.sharedui.resources.crop_aspect_free
import com.emanuel5014.trainable.sharedui.resources.image_editor_rotate_cd
import com.emanuel5014.trainable.sharedui.resources.image_editor_title
import com.emanuel5014.trainable.sharedui.resources.reset
import com.emanuel5014.trainable.sharedui.resources.save
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class CropAspectRatio(val ratio: Float?, val labelRes: StringResource) {
    FREE(null, Res.string.crop_aspect_free),
    ONE_ONE(1f, Res.string.crop_aspect_1_1),
    FOUR_THREE(4f/3f, Res.string.crop_aspect_4_3),
    SIXTEEN_NINE(16f/9f, Res.string.crop_aspect_16_9)
}

@Composable
fun ImageEditorDialog(
    imageBytes: ByteArray,
    onDismiss: () -> Unit,
    onConfirm: (ByteArray) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Keyed on the photo: when several photos are picked, the same dialog edits them one after the other and each
    // must start fresh (before, the spinner of the first save stayed on for the second photo).
    var bitmap by remember(imageBytes) { mutableStateOf<ImageBitmap?>(null) }
    var isProcessing by remember(imageBytes) { mutableStateOf(false) }

    LaunchedEffect(imageBytes) {
        withContext(Dispatchers.Default) {
            bitmap = decodeEditorImage(imageBytes)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = imageEditorDialogProperties()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (bitmap == null || isProcessing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else key(imageBytes) {
                ImageEditorContent(
                    bitmap = bitmap!!,
                    onCancel = onDismiss,
                    onSave = { scale, offset, rotation, aspect, cropRect, containerSize ->
                        isProcessing = true
                        scope.launch(Dispatchers.Default) {
                            try {
                                val editedBytes = cropAndRotateImage(
                                    originalBitmap = bitmap!!,
                                    scale = scale,
                                    offsetX = offset.x,
                                    offsetY = offset.y,
                                    rotationAngle = rotation,
                                    cropRect = cropRect,
                                    containerSize = containerSize
                                )
                                withContext(Dispatchers.Main) {
                                    onConfirm(editedBytes)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ImageEditorContent(
    bitmap: ImageBitmap,
    onCancel: () -> Unit,
    onSave: (scale: Float, offset: Offset, rotation: Float, aspect: CropAspectRatio, cropRect: Rect, containerSize: Size) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var selectedAspect by remember { mutableStateOf(CropAspectRatio.FREE) }

    var containerSize by remember { mutableStateOf(Size.Zero) }
    var isTransforming by remember { mutableStateOf(false) }

    val originalWidth = bitmap.width
    val originalHeight = bitmap.height

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color.Black)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.cancel), tint = Color.White)
            }

            Text(
                text = stringResource(Res.string.image_editor_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                        rotationAngle = 0f
                        selectedAspect = CropAspectRatio.FREE
                    },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.reset), tint = Color.White)
                }

                IconButton(
                    onClick = {
                        val cropRect = getCropRectForAspect(selectedAspect, containerSize)
                        onSave(scale, offset, rotationAngle, selectedAspect, cropRect, containerSize)
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.Default.Done, contentDescription = stringResource(Res.string.save), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        // Image Canvas / Editor Workspace (constrained between top bar and bottom controls)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer { clip = true }
                .onGloballyPositioned { coordinates ->
                    containerSize = Size(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat()
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        isTransforming = true
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offset = offset + pan
                        rotationAngle = (rotationAngle + rotation)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (containerSize != Size.Zero) {
                // Calculate display size for the image (fit inside container)
                val imageAspect = originalWidth.toFloat() / originalHeight
                val containerAspect = containerSize.width / containerSize.height

                val baseWidth: Float
                val baseHeight: Float
                if (imageAspect > containerAspect) {
                    baseWidth = containerSize.width
                    baseHeight = containerSize.width / imageAspect
                } else {
                    baseHeight = containerSize.height
                    baseWidth = containerSize.height * imageAspect
                }

                // Render image with transformations
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(
                            width = with(LocalDensity.current) { baseWidth.toDp() },
                            height = with(LocalDensity.current) { baseHeight.toDp() }
                        )
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                            rotationZ = rotationAngle
                        }
                )

                // Crop Overlay Window
                val cropRect = getCropRectForAspect(selectedAspect, containerSize)
                CropOverlay(
                    cropRect = cropRect,
                    containerSize = containerSize,
                    showGrid = isTransforming
                )
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rotation Control Row (Slider and exact rotate)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = {
                        rotationAngle = (rotationAngle - 90f)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Rotate90DegreesCcw,
                        contentDescription = stringResource(Res.string.image_editor_rotate_cd),
                        tint = Color.White
                    )
                }

                Slider(
                    value = rotationAngle.coerceIn(-180f, 180f),
                    onValueChange = {
                        isTransforming = true
                        rotationAngle = it
                    },
                    onValueChangeFinished = {
                        isTransforming = false
                    },
                    valueRange = -180f..180f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )

                Text(
                    text = "${rotationAngle.toInt()}°",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.width(44.dp)
                )
            }

            // Aspect Ratio Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CropAspectRatio.values().forEach { aspect ->
                    val isSelected = selectedAspect == aspect
                    Button(
                        onClick = { selectedAspect = aspect },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = stringResource(aspect.labelRes), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Reset isTransforming overlay state when gestures stop
    LaunchedEffect(scale, offset, rotationAngle) {
        kotlinx.coroutines.delay(800)
        isTransforming = false
    }
}

@Composable
fun CropOverlay(
    cropRect: Rect,
    containerSize: Size,
    showGrid: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw the dim background
        val path = Path().apply {
            // Add entire container bounds
            addRect(Rect(0f, 0f, containerSize.width, containerSize.height))
            // Cut out the cropRect viewport
            val cutoutPath = Path().apply {
                addRect(cropRect)
            }
            op(this, cutoutPath, androidx.compose.ui.graphics.PathOperation.Difference)
        }
        drawPath(path = path, color = Color.Black.copy(alpha = 0.6f))

        // Draw crop frame border
        drawRect(
            color = Color.White,
            topLeft = cropRect.topLeft,
            size = cropRect.size,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw Rule of Thirds grid guidelines if user is transforming the image
        if (showGrid) {
            val oneThirdW = cropRect.width / 3f
            val oneThirdH = cropRect.height / 3f

            // Vertical grid lines
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(cropRect.left + oneThirdW, cropRect.top),
                end = Offset(cropRect.left + oneThirdW, cropRect.bottom),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(cropRect.left + 2 * oneThirdW, cropRect.top),
                end = Offset(cropRect.left + 2 * oneThirdW, cropRect.bottom),
                strokeWidth = 1.dp.toPx()
            )

            // Horizontal grid lines
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(cropRect.left, cropRect.top + oneThirdH),
                end = Offset(cropRect.right, cropRect.top + oneThirdH),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(cropRect.left, cropRect.top + 2 * oneThirdH),
                end = Offset(cropRect.right, cropRect.top + 2 * oneThirdH),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

private fun getCropRectForAspect(aspect: CropAspectRatio, containerSize: Size): Rect {
    val margin = 32.dp.value // safe margin from container border
    val maxW = containerSize.width - 2 * margin
    val maxH = containerSize.height - 2 * margin

    val ratio = aspect.ratio ?: (maxW / maxH) // default to container ratio for FREE

    val w: Float
    val h: Float
    if (maxW / ratio <= maxH) {
        w = maxW
        h = maxW / ratio
    } else {
        h = maxH
        w = maxH * ratio
    }

    val left = (containerSize.width - w) / 2f
    val top = (containerSize.height - h) / 2f
    return Rect(left, top, left + w, top + h)
}
