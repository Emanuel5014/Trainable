package com.emanuel5014.trainable.ui.components

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.animation.core.animateFloatAsState
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.util.ExerciseMediaMessage
import com.emanuel5014.trainable.util.ExerciseMediaStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Side of the square exercise thumbnail shown next to the exercise name. It grows with the screen width,
 * from the 44dp touch-target minimum on the narrowest phones (where it costs a one-line name no extra height)
 * to 76dp on tablets, and stays at the minimum on a phone in landscape, which is short.
 */
@Composable
fun exerciseMediaThumbSize(): Dp {
    val configuration = LocalConfiguration.current
    if (configuration.screenHeightDp < 480) return 44.dp
    return (configuration.screenWidthDp * 0.145f).dp.coerceIn(44.dp, 76.dp)
}

/**
 * Opens the system Photo Picker (no permission needed) for one exercise at a time. Call the returned function with
 * the exercise id; [onPicked] gets that id back with the chosen file. The target survives rotation and recreation.
 */
@Composable
fun rememberExerciseMediaPicker(onPicked: (exerciseId: Int, uri: Uri) -> Unit): (Int) -> Unit {
    var targetId by rememberSaveable { mutableStateOf<Int?>(null) }
    val currentOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = targetId
        if (uri != null && target != null) currentOnPicked(target, uri)
        targetId = null
    }
    return remember(launcher) {
        { exerciseId: Int ->
            targetId = exerciseId
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

/** Shows each message from [messages] as a toast while this is in the composition. */
@Composable
fun ExerciseMediaMessageToasts(messages: Flow<ExerciseMediaMessage>) {
    val context = LocalContext.current
    LaunchedEffect(messages) {
        messages.collect { message ->
            val text = if (message.arg != null) context.getString(message.resId, message.arg) else context.getString(message.resId)
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * The user's exercise image or GIF. Animated files loop; they pause while the app is in the background
 * or when [playing] is false.
 *
 * Decoded with the platform `ImageDecoder`, which understands GIF and animated WebP natively (minSdk 28),
 * so no image library is needed for this.
 */
@Composable
fun ExerciseMediaImage(
    fileName: String,
    modifier: Modifier = Modifier,
    crop: Boolean = true,
    playing: Boolean = true,
    maxPx: Int = 512,
    onLoaded: (animated: Boolean, aspectRatio: Float) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val drawable by produceState<Drawable?>(initialValue = null, fileName, maxPx) {
        value = withContext(Dispatchers.IO) { ExerciseMediaStorage.decode(context, fileName, maxPx) }
    }

    var appVisible by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> appVisible = true
                Lifecycle.Event.ON_PAUSE -> appVisible = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ImageView(ctx).apply { importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        },
        update = { view ->
            view.scaleType = if (crop) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
            val current = drawable
            if (view.drawable !== current) {
                view.setImageDrawable(current)
                if (current != null) {
                    val aspect = if (current.intrinsicWidth > 0 && current.intrinsicHeight > 0) {
                        current.intrinsicWidth.toFloat() / current.intrinsicHeight
                    } else 1f
                    onLoaded(current is AnimatedImageDrawable, aspect)
                }
            }
            (current as? AnimatedImageDrawable)?.let { animation ->
                animation.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                if (playing && appVisible) {
                    if (!animation.isRunning) animation.start()
                } else {
                    animation.stop()
                }
            }
        },
        onRelease = { view ->
            (view.drawable as? AnimatedImageDrawable)?.stop()
            view.setImageDrawable(null)
        }
    )
}

/**
 * The exercise's thumbnail, or, when it has no media yet, a dashed slot that invites adding one.
 * Always [size] square so the surrounding layout does not shift between the two states.
 */
@Composable
fun ExerciseMediaSlot(
    fileName: String?,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = exerciseMediaThumbSize(),
    playing: Boolean = true
) {
    val shape = RoundedCornerShape(if (size < 56.dp) 12.dp else 16.dp)
    if (fileName != null) {
        val description = stringResource(R.string.exercise_media_open)
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(SurfaceContainerHigh)
                .clickable(role = Role.Button, onClick = onOpen)
                .semantics { contentDescription = description }
        ) {
            ExerciseMediaImage(fileName = fileName, modifier = Modifier.fillMaxSize(), playing = playing, maxPx = 256)
        }
    } else {
        val description = stringResource(R.string.exercise_media_add)
        val outline = OnSurfaceVariant.copy(alpha = 0.55f)
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .clickable(role = Role.Button, onClick = onAdd)
                .drawBehind {
                    val stroke = 1.5.dp.toPx()
                    drawRoundRect(
                        color = outline,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke),
                        cornerRadius = CornerRadius(
                            if (size < 56.dp) 12.dp.toPx() else 16.dp.toPx()
                        ),
                        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                    )
                }
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AddPhotoAlternate,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.size(if (size < 56.dp) 20.dp else 26.dp)
            )
        }
    }
}

/**
 * The exercise's media shown large inside the free space [modifier] gives it (the caller sets the height).
 * The media is fitted, never cropped, so a tall or wide demo keeps all of its frame; the surface stays
 * hidden until the file is decoded and its proportions are known, so it does not flash a square first.
 */
@Composable
fun ExerciseMediaCard(
    fileName: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var aspect by remember(fileName) { mutableFloatStateOf(1f) }
    var loaded by remember(fileName) { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (loaded) 1f else 0f, label = "exerciseMediaCardAlpha")
    val description = stringResource(R.string.exercise_media_open)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .aspectRatio(aspect)
                .alpha(alpha)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerHigh)
                .clickable(role = Role.Button, onClick = onOpen)
                .semantics { contentDescription = description }
        ) {
            ExerciseMediaImage(
                fileName = fileName,
                modifier = Modifier.fillMaxSize(),
                crop = false,
                maxPx = 1024,
                onLoaded = { _, ratio ->
                    aspect = ratio
                    loaded = true
                }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenInFull,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Full-screen look at an exercise's media: pinch to zoom, double-tap to reset, tap to play or pause an
 * animation. Replace and remove live in the top bar rather than in a bottom row so a phone in landscape
 * keeps all of its height for the image.
 */
@Composable
fun ExerciseMediaViewer(
    fileName: String,
    exerciseName: String,
    onDismiss: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit
) {
    var showRemoveConfirm by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var animated by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.exercise_media_remove_title),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Text(stringResource(R.string.exercise_media_remove_message), color = OnSurfaceVariant)
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveConfirm = false
                    onRemove()
                }) {
                    Text(stringResource(R.string.exercise_media_remove).uppercase(), color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = Primary)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .systemBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                }
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                IconButton(onClick = onReplace) {
                    Icon(
                        Icons.Rounded.SwapHoriz,
                        contentDescription = stringResource(R.string.exercise_media_replace),
                        tint = Color.White
                    )
                }
                IconButton(onClick = { showRemoveConfirm = true }) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.exercise_media_remove),
                        tint = Error
                    )
                }
            }

            val playPauseDescription = stringResource(R.string.exercise_media_play_pause)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(0.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                ) {
                    ExerciseMediaImage(
                        fileName = fileName,
                        modifier = Modifier.fillMaxSize(),
                        crop = false,
                        playing = playing,
                        maxPx = 1600,
                        onLoaded = { isAnimated, _ -> animated = isAnimated }
                    )
                }

                // Gestures sit on a transparent layer above the image: the platform view underneath would
                // otherwise swallow the touches before Compose can read them.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(animated) {
                            detectTapGestures(
                                onTap = { if (animated) playing = !playing },
                                onDoubleTap = {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = if (scale <= 1f) Offset.Zero else offset + pan
                            }
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription = playPauseDescription
                            role = Role.Button
                            onClick {
                                if (animated) playing = !playing
                                animated
                            }
                        }
                )

                if (animated && !playing) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHighest.copy(alpha = 0.85f))
                            .clearAndSetSemantics { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}
