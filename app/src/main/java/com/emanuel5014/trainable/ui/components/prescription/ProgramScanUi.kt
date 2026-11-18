package com.emanuel5014.trainable.ui.components

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.screens.routines.DeviceResourceAnalyticsCard
import com.emanuel5014.trainable.ui.screens.routines.ProgramScanState
import com.emanuel5014.trainable.ui.screens.routines.ProgramScanViewModel
import com.emanuel5014.trainable.ui.screens.routines.createScanTempImageUri
import com.emanuel5014.trainable.ui.screens.routines.userText
import kotlinx.coroutines.flow.map

/** Android sources and activity lifetime for the shared program scanner. */
@Composable
fun AiProgramScanButton(
    exerciseName: String?,
    /** In single-week contexts (a session) the user picks which scanned week to use. */
    singleWeek: Boolean,
    currentOneRepMaxKg: Float?,
    weightUnit: String,
    onApply: (ProgramScanApplication) -> Unit,
    modifier: Modifier = Modifier,
    /** Distinguishes several buttons on one screen (one per scanned exercise) so each keeps its own scan state. */
    scanKey: String? = null
) {
    val environment: PrescriptionEnvironmentViewModel = hiltViewModel()
    val available by environment.aiAvailable.collectAsState()
    if (!available) return

    val scanViewModel: ProgramScanViewModel = hiltViewModel(key = scanKey)
    val state by scanViewModel.model.state.collectAsState()
    val analyticsEnabled by scanViewModel.resourceAnalyticsEnabled.collectAsState(initial = false)

    val context = LocalContext.current
    val themeMode by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.THEME_MODE] ?: 0 }
    }.collectAsState(initial = 0)
    val isDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    var tempUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempUri?.let { scanViewModel.model.scan(it.toString(), exerciseName) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { scanViewModel.model.scan(it.toString(), exerciseName) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createScanTempImageUri(context)
            tempUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, context.getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = createScanTempImageUri(context)
            tempUri = uri
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // The model can take a while: keep the screen on for the whole scan
    val window = (context as? Activity)?.window
    val scanning = state is ProgramScanState.Scanning
    DisposableEffect(scanning) {
        if (scanning) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val roundingIncrement by environment.roundingIncrement.collectAsState()
    ProgramScanButton(
        model = scanViewModel.model,
        exerciseName = exerciseName,
        singleWeek = singleWeek,
        currentOneRepMaxKg = currentOneRepMaxKg,
        weightUnit = weightUnit,
        roundingIncrement = roundingIncrement,
        isDark = isDark,
        takePhoto = ::openCamera,
        pickPhoto = { galleryLauncher.launch("image/*") },
        showError = { failed ->
            Toast.makeText(
                context,
                failed.failure?.userText(context) ?: context.getString(R.string.ai_program_scan_failed),
                Toast.LENGTH_LONG
            ).show()
        },
        onApply = onApply,
        modifier = modifier
    ) { metrics ->
        if (analyticsEnabled) DeviceResourceAnalyticsCard(metrics, isDark)
    }
}
