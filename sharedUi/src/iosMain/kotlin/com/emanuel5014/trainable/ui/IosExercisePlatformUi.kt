@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.interop.LocalUIViewController
import com.emanuel5014.trainable.ui.components.ExercisePlatformUi
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.screens.exercise.ExerciseDetailDialogContent
import com.emanuel5014.trainable.ui.screens.exercise.ExerciseDetailState
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.repository.ExerciseMediaImport
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.exercise_media_too_large
import com.emanuel5014.trainable.sharedui.resources.exercise_media_unsupported
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.getString
import platform.Foundation.*
import platform.PhotosUI.*
import platform.UIKit.*
import platform.darwin.*

internal object IosExercisePlatformUi : ExercisePlatformUi {
    override val hasExerciseDetail: Boolean get() = true

    @Composable
    override fun namesInUse(): List<ExerciseEntity> {
        val names by remember { IosAppGraph.merges.observeNamesInUse() }.collectAsState(emptyList())
        return names
    }

    @Composable
    override fun ExerciseDetail(exerciseId: Int, onDismiss: () -> Unit) {
        val scope = rememberCoroutineScope()
        val language = com.emanuel5014.trainable.platform.IosAppLocale.language()
        val controller = remember(language) {
            ExerciseDetailState(IosAppGraph.exercises, IosAppGraph.workouts, IosAppGraph.merges,
                IosAppGraph.preferences, flowOf(language), scope)
        }
        ExerciseDetailDialogContent(exerciseId, onDismiss, controller)
    }

    @Composable
    override fun MediaImage(fileName: String, modifier: Modifier, crop: Boolean, playing: Boolean, maxPx: Int,
                            onLoaded: (Boolean, Float) -> Unit) =
        IosExerciseMediaImage(fileName, modifier, crop, playing, maxPx, onLoaded)

    @Composable
    override fun ImportMessages(messages: Flow<ExerciseMediaImport>) {
        val show = LocalShowMessage.current
        LaunchedEffect(messages) {
            messages.collect { result ->
                when (result) {
                    ExerciseMediaImport.TooLarge -> show(getString(Res.string.exercise_media_too_large, 15))
                    ExerciseMediaImport.Unsupported -> show(getString(Res.string.exercise_media_unsupported))
                    is ExerciseMediaImport.Saved -> Unit
                }
            }
        }
    }

    @Composable
    override fun rememberMediaPicker(onPicked: (Int, String) -> Unit): (Int) -> Unit {
        val presenter = LocalUIViewController.current
        val currentCallback by rememberUpdatedState(onPicked)
        var targetId by remember { mutableStateOf<Int?>(null) }
        var active by remember { mutableStateOf<PHPickerViewController?>(null) }
        var alive by remember { mutableStateOf(true) }
        val show = LocalShowMessage.current
        val unsupported = org.jetbrains.compose.resources.stringResource(Res.string.exercise_media_unsupported)
        val delegate = remember {
            object : NSObject(), PHPickerViewControllerDelegateProtocol {
                override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                    val target = targetId
                    targetId = null
                    val result = didFinishPicking.firstOrNull() as? PHPickerResult
                    picker.dismissViewControllerAnimated(true) {
                        active = null
                        if (target != null && result != null && alive) {
                            result.itemProvider.loadFileRepresentationForTypeIdentifier("public.image") { url, error ->
                                // Apple's temporary URL is valid only during this callback. Copy before returning.
                                val destination = NSTemporaryDirectory() + "TrainableExercisePick_" + NSUUID().UUIDString
                                val copied = url != null && error == null && NSFileManager.defaultManager.copyItemAtURL(
                                    url, NSURL.fileURLWithPath(destination), null)
                                dispatch_async(dispatch_get_main_queue()) {
                                    when {
                                        copied && alive -> currentCallback(target, destination)
                                        copied -> NSFileManager.defaultManager.removeItemAtPath(destination, null)
                                        alive -> show(unsupported)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        DisposableEffect(presenter) {
            onDispose { alive = false; active?.dismissViewControllerAnimated(false, null); active = null }
        }
        return remember(presenter, delegate) {
            { id: Int ->
                if (active == null) {
                    val configuration = PHPickerConfiguration().apply {
                        filter = PHPickerFilter.imagesFilter
                        selectionLimit = 1
                        preferredAssetRepresentationMode = PHPickerConfigurationAssetRepresentationModeCurrent
                    }
                    val picker = PHPickerViewController(configuration).apply { this.delegate = delegate }
                    targetId = id
                    active = picker
                    topController(presenter).presentViewController(picker, animated = true, completion = null)
                }
            }
        }
    }
}

private fun topController(root: UIViewController): UIViewController {
    var top = root
    while (top.presentedViewController != null) top = top.presentedViewController!!
    return top
}

/** Validation/import messages are visible over both Compose sheets and full-screen exercise dialogs. */
@Composable
internal fun rememberIosShowMessage(): (String) -> Unit {
    val controller = LocalUIViewController.current
    return remember(controller) {
        { text ->
            dispatch_async(dispatch_get_main_queue()) {
                val top = topController(controller)
                if (top !is UIAlertController) {
                    val alert = UIAlertController.alertControllerWithTitle(null, text, UIAlertControllerStyleAlert)
                    alert.addAction(UIAlertAction.actionWithTitle("OK", UIAlertActionStyleDefault, null))
                    top.presentViewController(alert, true, null)
                }
            }
        }
    }
}
