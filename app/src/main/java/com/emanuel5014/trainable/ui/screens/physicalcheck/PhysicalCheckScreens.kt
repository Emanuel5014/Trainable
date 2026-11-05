package com.emanuel5014.trainable.ui.screens.physicalcheck

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.shared.ImageEditorDialog
import com.emanuel5014.trainable.util.BiometricHelper
import com.emanuel5014.trainable.util.ImageStorageUtils
import java.io.File

/** The Android physical checks: the shared [PhysicalCheckContent] on [PhysicalCheckViewModel]. */
@Composable
fun PhysicalCheckScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCompare: (Int, Int) -> Unit,
    viewModel: PhysicalCheckViewModel = hiltViewModel()
) {
    CompositionLocalProvider(LocalPhysicalCheckPlatform provides AndroidPhysicalCheckPlatform) {
        PhysicalCheckContent(onNavigateBack, onNavigateToSettings, onNavigateToCompare, viewModel.model)
    }
}

@Composable
fun PhysicalCheckCompareScreen(
    checkId1: Int,
    checkId2: Int,
    onNavigateBack: () -> Unit,
    viewModel: PhysicalCheckViewModel = hiltViewModel()
) {
    CompositionLocalProvider(LocalPhysicalCheckPlatform provides AndroidPhysicalCheckPlatform) {
        PhysicalCheckCompareContent(checkId1, checkId2, onNavigateBack, viewModel.model)
    }
}

@Composable
fun PhysicalCheckSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PhysicalCheckViewModel = hiltViewModel()
) {
    CompositionLocalProvider(LocalPhysicalCheckPlatform provides AndroidPhysicalCheckPlatform) {
        PhysicalCheckSettingsContent(onNavigateBack, viewModel.model)
    }
}

/** Camera (with its permission), the photo picker, BiometricPrompt and the crop editor, as before the move. */
object AndroidPhysicalCheckPlatform : PhysicalCheckPlatformUi {

    @Composable
    override fun rememberPhotoCapture(
        onPicked: (List<ByteArray>) -> Unit,
        onCameraResult: (success: Boolean, bytes: ByteArray?) -> Unit,
        onCameraDenied: () -> Unit
    ): PhotoCapture {
        val context = LocalContext.current
        var tempImageUri by remember { mutableStateOf<Uri?>(null) }

        val pickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia()
        ) { uris ->
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
            onPicked(bytesList)
        }

        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            val uri = tempImageUri
            if (success && uri != null) {
                val bytes = ImageStorageUtils.readAndCompressImage(context, uri)
                onCameraResult(true, bytes)
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                onCameraResult(success, null)
            }
        }

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                val uri = createCheckTempImageUri(context)
                tempImageUri = uri
                cameraLauncher.launch(uri)
            } else {
                onCameraDenied()
                Toast.makeText(context, context.getString(R.string.physical_check_camera_denied), Toast.LENGTH_SHORT).show()
            }
        }

        return PhotoCapture(
            takePhoto = {
                val permission = Manifest.permission.CAMERA
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    val uri = createCheckTempImageUri(context)
                    tempImageUri = uri
                    cameraLauncher.launch(uri)
                } else {
                    cameraPermissionLauncher.launch(permission)
                }
            },
            pickPhotos = {
                pickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        )
    }

    @Composable
    override fun rememberBiometricPrompt(): ((onSuccess: () -> Unit, onError: () -> Unit) -> Unit)? {
        val activity = LocalContext.current as? FragmentActivity ?: return null
        return remember(activity) {
            { onSuccess, onError ->
                BiometricHelper.checkAndShowBiometricPrompt(
                    activity = activity,
                    onSuccess = onSuccess,
                    onError = { onError() }
                )
            }
        }
    }

    @Composable
    override fun ImageEditor(imageBytes: ByteArray, onDismiss: () -> Unit, onConfirm: (ByteArray) -> Unit) {
        ImageEditorDialog(imageBytes = imageBytes, onDismiss = onDismiss, onConfirm = onConfirm)
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
