package com.emanuel5014.trainable.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity
import com.emanuel5014.trainable.util.ImageStorageUtils
import com.emanuel5014.trainable.util.UriMigrationHelper
import java.io.File

/** The routine photos on Android: Coil, the system gallery and camera, files in filesDir (the code of before the move). */
private class AndroidRoutinePhotoPlatform(private val context: Context) : RoutinePhotoPlatform {

    @Composable
    override fun Image(uri: String, modifier: Modifier, contentScale: ContentScale) {
        val context = LocalContext.current
        AsyncImage(
            model = UriMigrationHelper.fixPath(uri, context),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }

    @Composable
    override fun rememberSources(onAdded: (String) -> Unit, onFinished: () -> Unit): RoutinePhotoSources {
        val context = LocalContext.current
        val added by rememberUpdatedState(onAdded)
        val finished by rememberUpdatedState(onFinished)
        var tempImageUri by remember { mutableStateOf<Uri?>(null) }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val savedPath = ImageStorageUtils.compressAndSaveImage(context, it)
                if (savedPath != null) added(savedPath)
            }
            finished()
        }

        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                tempImageUri?.let { uri ->
                    val compressedPath = ImageStorageUtils.compressAndSaveImage(context, uri)
                    if (compressedPath != null) {
                        added(compressedPath)
                        // Delete the uncompressed temp file from cache
                        try {
                            context.contentResolver.delete(uri, null, null)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            finished()
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                val uri = createTempImageUri(context)
                tempImageUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, context.getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }

        return RoutinePhotoSources(
            takePhoto = {
                val permission = Manifest.permission.CAMERA
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    val uri = createTempImageUri(context)
                    tempImageUri = uri
                    cameraLauncher.launch(uri)
                } else {
                    permissionLauncher.launch(permission)
                }
            },
            pickFromGallery = { imagePickerLauncher.launch("image/*") }
        )
    }

    override fun deleteFile(uri: String) {
        val fixedPath = UriMigrationHelper.fixPath(uri, context)
        if (fixedPath?.startsWith("/") == true) {
            File(fixedPath).delete()
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val tempFile = File(context.cacheDir, "routine_temp_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

/** The routine photos of the shared routine screen (LocalRoutineImagePicker). */
val AndroidRoutineImagePicker: @Composable (
    images: List<WorkoutPlanImageEntity>,
    onImageAdd: (String) -> Unit,
    onImageRemove: (WorkoutPlanImageEntity) -> Unit,
    modifier: Modifier
) -> Unit = { images, onImageAdd, onImageRemove, modifier ->
    val context = LocalContext.current
    val platform = remember(context) { AndroidRoutinePhotoPlatform(context) }
    RoutineImagePicker(images, onImageAdd, onImageRemove, platform, modifier)
}
