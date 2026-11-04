package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.rounded.FitnessCenter
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
import androidx.compose.runtime.staticCompositionLocalOf
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
import org.jetbrains.compose.resources.stringResource
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.close
import com.emanuel5014.trainable.sharedui.resources.exercise_media_add
import com.emanuel5014.trainable.sharedui.resources.exercise_media_open
import com.emanuel5014.trainable.sharedui.resources.exercise_media_play_pause
import com.emanuel5014.trainable.sharedui.resources.exercise_media_remove
import com.emanuel5014.trainable.sharedui.resources.exercise_media_remove_message
import com.emanuel5014.trainable.sharedui.resources.exercise_media_remove_title
import com.emanuel5014.trainable.sharedui.resources.exercise_media_replace

@Composable
fun SharedExerciseMediaSlot(
    fileName: String?,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    playing: Boolean = true
) {
    val shape = RoundedCornerShape(if (size < 56.dp) 12.dp else 16.dp)
    if (fileName != null) {
        val description = stringResource(Res.string.exercise_media_open)
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(SurfaceContainerHigh)
                .clickable(role = Role.Button, onClick = onOpen)
                .semantics { contentDescription = description }
        ) {
            LocalExercisePlatformUi.current.MediaImage(fileName = fileName, modifier = Modifier.fillMaxSize(), playing = playing, maxPx = 256)
        }
    } else {
        val description = stringResource(Res.string.exercise_media_add)
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
 * The user's image for the exercise, which opens full screen when tapped, or a neutral tile when it has none. Still
 * on purpose (like the list on the exercises page): a GIF is shown by its first frame, so a long list does not animate
 * all at once; the full-screen look plays it. Tapping anywhere else on the row picks the exercise. With a null [onClick]
 * the tile is only a picture: the row around it has the one action.
 */
@Composable
fun SharedExerciseListThumbnail(fileName: String?, onClick: (() -> Unit)?, size: Dp = ExerciseListThumbnailSize) {
    val shape = RoundedCornerShape(if (size < 56.dp) 12.dp else 16.dp)
    if (fileName != null) {
        val description = stringResource(Res.string.exercise_media_open)
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(SurfaceContainerHighest)
                .then(
                    if (onClick != null) {
                        Modifier
                            .clickable(role = Role.Button, onClick = onClick)
                            .semantics { contentDescription = description }
                    } else {
                        Modifier.clearAndSetSemantics { }
                    }
                )
        ) {
            LocalExercisePlatformUi.current.MediaImage(
                fileName = fileName,
                modifier = Modifier.fillMaxSize(),
                playing = false,
                maxPx = 192
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(SurfaceContainerHighest)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.FitnessCenter,
                contentDescription = null,
                tint = OnSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private val ExerciseListThumbnailSize = 56.dp

/** The full-screen, look-only viewer of [exercise]'s media; nothing while [exercise] is null or has no media. */
@Composable
fun SharedExerciseMediaPeek(exercise: ExerciseEntity?, languageCode: String, onDismiss: () -> Unit) {
    val fileName = exercise?.mediaPath ?: return
    SharedExerciseMediaViewer(
        fileName = fileName,
        exerciseName = ExerciseTranslations.translate(exercise.nome, languageCode),
        onDismiss = onDismiss
    )
}

/**
 * The exercise's media shown large inside the free space [modifier] gives it (the caller sets the height).
 * The media is fitted, never cropped, so a tall or wide demo keeps all of its frame; the surface stays
 * hidden until the file is decoded and its proportions are known, so it does not flash a square first.
 */
@Composable
fun SharedExerciseMediaCard(
    fileName: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var aspect by remember(fileName) { mutableFloatStateOf(1f) }
    var loaded by remember(fileName) { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (loaded) 1f else 0f, label = "exerciseMediaCardAlpha")
    val description = stringResource(Res.string.exercise_media_open)
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
            LocalExercisePlatformUi.current.MediaImage(
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
 * keeps all of its height for the image. Leave [onReplace] and [onRemove] null for a look-only viewer.
 */
@Composable
fun SharedExerciseMediaViewer(
    fileName: String,
    exerciseName: String,
    onDismiss: () -> Unit,
    onReplace: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    var showRemoveConfirm by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var animated by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    if (showRemoveConfirm && onRemove != null) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    stringResource(Res.string.exercise_media_remove_title),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Text(stringResource(Res.string.exercise_media_remove_message), color = OnSurfaceVariant)
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveConfirm = false
                    onRemove()
                }) {
                    Text(stringResource(Res.string.exercise_media_remove).uppercase(), color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = Primary)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = LocalExercisePlatformUi.current.detailDialogProperties()
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
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.close), tint = Color.White)
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
                if (onReplace != null) {
                    IconButton(onClick = onReplace) {
                        Icon(
                            Icons.Rounded.SwapHoriz,
                            contentDescription = stringResource(Res.string.exercise_media_replace),
                            tint = Color.White
                        )
                    }
                }
                if (onRemove != null) {
                    IconButton(onClick = { showRemoveConfirm = true }) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = stringResource(Res.string.exercise_media_remove),
                            tint = Error
                        )
                    }
                }
            }

            val playPauseDescription = stringResource(Res.string.exercise_media_play_pause)
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
                    LocalExercisePlatformUi.current.MediaImage(
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
