package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.add_photo
import com.emanuel5014.trainable.sharedui.resources.camera
import com.emanuel5014.trainable.sharedui.resources.gallery
import com.emanuel5014.trainable.sharedui.resources.remove_photo
import com.emanuel5014.trainable.sharedui.resources.select_photo_source
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.util.fullscreenDialogProperties
import com.emanuel5014.trainable.ui.util.fullscreenDialogTopInset
import org.jetbrains.compose.resources.stringResource

/** The camera and gallery of one routine photo strip, from [RoutinePhotoPlatform.rememberSources]. */
class RoutinePhotoSources(val takePhoto: () -> Unit, val pickFromGallery: () -> Unit)

/** What the routine photos need from each platform: drawing a stored photo, adding one, deleting its file. */
interface RoutinePhotoPlatform {
    /** Draws the photo stored at [uri] (a path as saved in the database). */
    @Composable
    fun Image(uri: String, modifier: Modifier, contentScale: ContentScale)

    /**
     * Camera and gallery for one strip. [onAdded] gets the stored path of a new photo; [onFinished] runs when a
     * picker or the camera closes (also without a photo), not when the camera permission is refused.
     */
    @Composable
    fun rememberSources(onAdded: (String) -> Unit, onFinished: () -> Unit): RoutinePhotoSources

    /** Removes the photo file at [uri] when it is one of the app's own files. */
    fun deleteFile(uri: String)
}

@Composable
fun RoutineImagePicker(
    images: List<WorkoutPlanImageEntity>,
    onImageAdd: (String) -> Unit,
    onImageRemove: (WorkoutPlanImageEntity) -> Unit,
    platform: RoutinePhotoPlatform,
    modifier: Modifier = Modifier
) {
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

    // Read once: on iOS the dialog's content recomposes once more while it closes, after the index became null
    val openIndex = selectedImageIndex
    if (openIndex != null) {
        Dialog(
            onDismissRequest = { selectedImageIndex = null },
            properties = fullscreenDialogProperties()
        ) {
            FullscreenImageViewerContent(
                images = images,
                initialIndex = openIndex,
                platform = platform,
                onDismiss = { selectedImageIndex = null }
            )
        }
    }

    ImagePickerContent(
        images = images,
        onImageAdd = onImageAdd,
        onImageRemove = onImageRemove,
        onImageClick = { clickedImage ->
            selectedImageIndex = images.indexOf(clickedImage).takeIf { it != -1 }
        },
        platform = platform,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagePickerContent(
    images: List<WorkoutPlanImageEntity>,
    onImageAdd: (String) -> Unit,
    onImageRemove: (WorkoutPlanImageEntity) -> Unit,
    onImageClick: (WorkoutPlanImageEntity) -> Unit,
    platform: RoutinePhotoPlatform,
    modifier: Modifier = Modifier
) {
    var showOptions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sources = platform.rememberSources(onAdded = onImageAdd, onFinished = { showOptions = false })

    if (showOptions) {
        ModalBottomSheet(
            onDismissRequest = { showOptions = false },
            sheetState = sheetState,
            containerColor = Surface,
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(OnSurfaceVariant.copy(alpha = 0.4f))
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
                    text = stringResource(Res.string.select_photo_source),
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurface,
                    fontWeight = FontWeight.Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OptionItem(
                        icon = Icons.Rounded.PhotoCamera,
                        label = stringResource(Res.string.camera),
                        onClick = { sources.takePhoto() },
                        modifier = Modifier.weight(1f)
                    )
                    OptionItem(
                        icon = Icons.Rounded.PhotoLibrary,
                        label = stringResource(Res.string.gallery),
                        onClick = { sources.pickFromGallery() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Nothing to show yet: a slim button instead of a big empty tile
    if (images.isEmpty()) {
        Box(modifier = modifier) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { showOptions = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(Res.string.add_photo),
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        return
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(images, key = { it.id }) { image ->
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onImageClick(image) }
            ) {
                platform.Image(
                    uri = image.imageUri,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Surface.copy(alpha = 0.8f))
                        .clickable {
                            onImageRemove(image)
                            platform.deleteFile(image.imageUri)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(Res.string.remove_photo),
                        tint = OnSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { showOptions = true },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = stringResource(Res.string.add_photo),
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = stringResource(Res.string.add_photo),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
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
    Column(
        modifier = modifier
            .clip(Shapes.large)
            .background(SurfaceContainerHigh)
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = OnSurface,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun FullscreenImageViewerContent(
    images: List<WorkoutPlanImageEntity>,
    initialIndex: Int,
    platform: RoutinePhotoPlatform,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
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
            val image = images[page]

            ZoomableImage(
                imageUri = image.imageUri,
                platform = platform,
                onZoomChanged = { zoomed ->
                    if (pagerState.currentPage == page) {
                        isZoomed = zoomed
                    }
                }
            )
        }

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fullscreenDialogTopInset()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${pagerState.currentPage + 1} / ${images.size}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    imageUri: String,
    platform: RoutinePhotoPlatform,
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
        platform.Image(
            uri = imageUri,
            modifier = Modifier
                .fillMaxSize()
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
