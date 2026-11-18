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
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
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
fun ExerciseMediaSlot(fileName: String?, onOpen: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier,
                      size: Dp = exerciseMediaThumbSize(), playing: Boolean = true) =
    SharedExerciseMediaSlot(fileName, onOpen, onAdd, modifier, size, playing)

@Composable
fun ExerciseListThumbnail(fileName: String?, onClick: (() -> Unit)?, size: Dp = 56.dp) =
    SharedExerciseListThumbnail(fileName, onClick, size)

@Composable
fun ExerciseMediaPeek(exercise: ExerciseEntity?, languageCode: String, onDismiss: () -> Unit) =
    SharedExerciseMediaPeek(exercise, languageCode, onDismiss)

@Composable
fun ExerciseMediaCard(fileName: String, onOpen: () -> Unit, modifier: Modifier = Modifier) =
    SharedExerciseMediaCard(fileName, onOpen, modifier)

@Composable
fun ExerciseMediaViewer(fileName: String, exerciseName: String, onDismiss: () -> Unit,
                        onReplace: (() -> Unit)? = null, onRemove: (() -> Unit)? = null) =
    SharedExerciseMediaViewer(fileName, exerciseName, onDismiss, onReplace, onRemove)
