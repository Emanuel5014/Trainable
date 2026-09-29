package com.emanuel5014.trainable.ui.components

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ai.ScannedProgram
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.ui.screens.routines.AiScanningOverlay
import com.emanuel5014.trainable.ui.screens.routines.ProgramScanState
import com.emanuel5014.trainable.ui.screens.routines.ProgramScanViewModel
import com.emanuel5014.trainable.ui.screens.routines.ScanOptionItem
import com.emanuel5014.trainable.ui.screens.routines.createScanTempImageUri
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.flow.map

/** What the user accepted from a program scan. */
data class ProgramScanApplication(
    /** Week number → blocks. In single-week mode there is only week 1. */
    val weeks: Map<Int, List<PrescriptionBlock>>,
    val oneRepMaxKg: Float?
)

/**
 * "Scan the program page" button: camera / gallery → on-device model → reviewable result.
 * Renders nothing unless AI is enabled in settings and the model is downloaded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProgramScanButton(
    exerciseName: String?,
    /** In single-week contexts (a session) the user picks which scanned week to use. */
    singleWeek: Boolean,
    currentOneRepMaxKg: Float?,
    weightUnit: String,
    onApply: (ProgramScanApplication) -> Unit,
    modifier: Modifier = Modifier
) {
    val environment: PrescriptionEnvironmentViewModel = hiltViewModel()
    val available by environment.aiAvailable.collectAsState()
    if (!available) return

    val scanViewModel: ProgramScanViewModel = hiltViewModel()
    val state by scanViewModel.state.collectAsState()
    val stream by scanViewModel.stream.collectAsState()
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

    var showSource by remember { mutableStateOf(false) }
    var tempUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempUri?.let { scanViewModel.scan(it, exerciseName) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { scanViewModel.scan(it, exerciseName) }
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

    LaunchedEffect(state) {
        if (state is ProgramScanState.Failed) {
            Toast.makeText(context, context.getString(R.string.ai_program_scan_failed), Toast.LENGTH_LONG).show()
            scanViewModel.reset()
        }
    }

    GymButton(
        onClick = { showSource = true },
        modifier = modifier,
        containerColor = Primary.copy(alpha = 0.12f),
        contentColor = Primary,
        height = 48
    ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.ai_program_scan_button).uppercase(), fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }

    if (showSource) {
        ModalBottomSheet(
            onDismissRequest = { showSource = false },
            containerColor = Surface,
            contentColor = OnSurface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.ai_program_scan_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = stringResource(R.string.ai_program_scan_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ScanOptionItem(
                        icon = Icons.Rounded.PhotoCamera,
                        label = stringResource(R.string.camera),
                        onClick = {
                            showSource = false
                            openCamera()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ScanOptionItem(
                        icon = Icons.Rounded.PhotoLibrary,
                        label = stringResource(R.string.gallery),
                        onClick = {
                            showSource = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    (state as? ProgramScanState.Scanning)?.let { scanningState ->
        // Own window so the overlay also covers the bottom sheet that hosts the button
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            AiScanningOverlay(
                phase = scanningState.phase,
                stream = stream,
                showResourceAnalytics = analyticsEnabled,
                isDark = isDark,
                hazeState = null,
                onCancel = { scanViewModel.cancel() }
            )
        }
    }

    (state as? ProgramScanState.Result)?.let { result ->
        ProgramScanReviewDialog(
            program = result.program,
            singleWeek = singleWeek,
            currentOneRepMaxKg = currentOneRepMaxKg,
            weightUnit = weightUnit,
            onDismiss = { scanViewModel.reset() },
            onApply = {
                scanViewModel.reset()
                onApply(it)
            }
        )
    }
}

/** Shows what the model read so the user can pick what to import; everything stays editable afterwards. */
@Composable
private fun ProgramScanReviewDialog(
    program: ScannedProgram,
    singleWeek: Boolean,
    currentOneRepMaxKg: Float?,
    weightUnit: String,
    onDismiss: () -> Unit,
    onApply: (ProgramScanApplication) -> Unit
) {
    val labels = rememberPrescriptionLabels(weightUnit)
    val weeks = remember(program) { program.weeks.toSortedMap() }
    val selected = remember(program) {
        mutableStateMapOf<Int, Boolean>().apply { weeks.keys.forEachIndexed { i, week -> put(week, if (singleWeek) i == 0 else true) } }
    }
    val foundMax = program.oneRepMaxKg
    var saveMax by remember(program) { mutableStateOf(foundMax != null && foundMax != currentOneRepMaxKg) }
    val anySelected = selected.values.any { it }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = {
            Text(stringResource(R.string.ai_program_review_title), fontWeight = FontWeight.ExtraBold, color = OnSurface)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(if (singleWeek) R.string.ai_program_review_pick_week else R.string.ai_program_review_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
                weeks.forEach { (week, blocks) ->
                    val checked = selected[week] == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Shapes.medium)
                            .background(if (checked) Primary.copy(alpha = 0.08f) else SurfaceContainer)
                            .clickable {
                                if (singleWeek) weeks.keys.forEach { selected[it] = it == week }
                                else selected[week] = !checked
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (singleWeek) {
                            RadioButton(
                                selected = checked,
                                onClick = { weeks.keys.forEach { selected[it] = it == week } },
                                colors = RadioButtonDefaults.colors(selectedColor = Primary)
                            )
                        } else {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { selected[week] = it },
                                colors = CheckboxDefaults.colors(checkedColor = Primary)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.week_n, week),
                                style = MaterialTheme.typography.labelMedium,
                                color = Primary,
                                fontWeight = FontWeight.Black
                            )
                            PrescriptionBlocksSummary(blocks = blocks, labels = labels, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                if (foundMax != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { saveMax = !saveMax },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveMax,
                            onCheckedChange = { saveMax = it },
                            colors = CheckboxDefaults.colors(checkedColor = Primary)
                        )
                        Text(
                            text = stringResource(R.string.ai_program_save_one_rep_max, formatWeight(foundMax, weightUnit)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            GymButton(
                onClick = {
                    val chosen = weeks.filterKeys { selected[it] == true }
                    val result = if (singleWeek) mapOf(1 to chosen.values.first()) else chosen
                    onApply(ProgramScanApplication(result, foundMax.takeIf { saveMax }))
                },
                enabled = anySelected,
                containerColor = Primary,
                contentColor = OnPrimary,
                height = 48
            ) {
                Text(stringResource(R.string.ai_program_import).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}
