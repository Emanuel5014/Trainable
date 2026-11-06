package com.emanuel5014.trainable.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity
import com.emanuel5014.trainable.data.repository.deleteIosRoutineImage
import com.emanuel5014.trainable.data.repository.iosDocumentsPath
import com.emanuel5014.trainable.data.repository.readIosRoutineImage
import com.emanuel5014.trainable.data.repository.saveIosRoutineImage
import com.emanuel5014.trainable.data.repository.toByteArray
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.camera_permission_denied
import com.emanuel5014.trainable.ui.components.RoutineImagePicker
import com.emanuel5014.trainable.ui.components.RoutinePhotoPlatform
import com.emanuel5014.trainable.ui.components.RoutinePhotoSources
import com.emanuel5014.trainable.ui.screens.physicalcheck.decodePhotoBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * Routine photos on iOS: files in Documents/routine_images (also those restored from an Android backup or
 * imported with a routine file), added from the camera or the photo library like Android (one photo at a time).
 */
internal object IosRoutinePhotoPlatform : RoutinePhotoPlatform {

    /** The last decoded photos, so lists and the viewer do not decode the same file again (main thread only). */
    private val cache = LinkedHashMap<String, ImageBitmap>()
    private const val CACHE_SIZE = 24

    @Composable
    override fun Image(uri: String, modifier: Modifier, contentScale: ContentScale) {
        val bitmap by produceState(cache[uri], uri) {
            if (value == null) {
                value = withContext(Dispatchers.Default) { decode(uri) }?.also { keep(uri, it) }
            }
        }
        val shown = bitmap
        if (shown != null) {
            androidx.compose.foundation.Image(bitmap = shown, contentDescription = null, modifier = modifier, contentScale = contentScale)
        } else {
            Box(modifier)
        }
    }

    @Composable
    override fun rememberSources(onAdded: (String) -> Unit, onFinished: () -> Unit): RoutinePhotoSources {
        val added by rememberUpdatedState(onAdded)
        val finished by rememberUpdatedState(onFinished)
        fun store(bytes: ByteArray?) {
            bytes?.let { saveIosRoutineImage(it) }?.let { added(it) }
        }
        val capture = rememberIosPhotoCapture(
            selectionLimit = 1,
            deniedText = stringResource(Res.string.camera_permission_denied),
            onPicked = { photos -> photos.firstOrNull()?.let { store(it) }; finished() },
            onCameraResult = { success, bytes -> if (success) store(bytes); finished() },
            // As on Android, the source sheet stays open after a refused permission
            onCameraDenied = {}
        )
        return remember(capture) { RoutinePhotoSources(takePhoto = capture.takePhoto, pickFromGallery = capture.pickPhotos) }
    }

    override fun deleteFile(uri: String) {
        cache.remove(uri)
        deleteIosRoutineImage(uri)
    }

    private fun decode(uri: String): ImageBitmap? {
        val documents = iosDocumentsPath() ?: return null
        return readIosRoutineImage(uri, documents)?.toByteArray()?.let { decodePhotoBitmap(it) }
    }

    private fun keep(uri: String, bitmap: ImageBitmap) {
        cache.remove(uri)
        cache[uri] = bitmap
        while (cache.size > CACHE_SIZE) cache.remove(cache.keys.first())
    }
}

/** Shown in the routine list cards and the add-workout sheet (LocalPlanImage). */
internal val IosPlanImage: @Composable (uri: String, modifier: Modifier) -> Unit = { uri, modifier ->
    IosRoutinePhotoPlatform.Image(uri, modifier, ContentScale.Crop)
}

/** The routine screen's photo strip (LocalRoutineImagePicker). */
internal val IosRoutineImagePicker: @Composable (
    images: List<WorkoutPlanImageEntity>,
    onImageAdd: (String) -> Unit,
    onImageRemove: (WorkoutPlanImageEntity) -> Unit,
    modifier: Modifier
) -> Unit = { images, onImageAdd, onImageRemove, modifier ->
    RoutineImagePicker(images, onImageAdd, onImageRemove, IosRoutinePhotoPlatform, modifier)
}
