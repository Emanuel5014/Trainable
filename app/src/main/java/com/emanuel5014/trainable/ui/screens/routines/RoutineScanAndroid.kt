package com.emanuel5014.trainable.ui.screens.routines

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.util.ImageStorageUtils
import dev.chrisbanes.haze.HazeState

@Composable
fun rememberAndroidRoutineScanUi(viewModel: RoutineDetailViewModel): RoutineScanUi {
    val context = LocalContext.current
    val languageCode by viewModel.languageCode.collectAsState()
    val aiResourceAnalyticsEnabled by viewModel.aiResourceAnalyticsEnabled.collectAsState()

    var scanTempImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val scanCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) scanTempImageUri?.let { viewModel.scan.scanRoutineSheet(it.toString()) }
    }
    val scanGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.scan.scanRoutineSheet(it.toString()) } }
    val scanPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createScanTempImageUri(context)
            scanTempImageUri = uri
            scanCameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, context.getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    fun handleScanCameraClick() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val uri = createScanTempImageUri(context)
            scanTempImageUri = uri
            scanCameraLauncher.launch(uri)
        } else {
            scanPermissionLauncher.launch(permission)
        }
    }

    val scan = rememberRoutineScanUi(
        model = viewModel.scan,
        languageCode = languageCode,
        photo = AndroidScanPhoto,
        takePhoto = ::handleScanCameraClick,
        pickPhoto = { scanGalleryLauncher.launch("image/*") },
        savePhoto = { ImageStorageUtils.compressAndSaveImage(context, android.net.Uri.parse(it)) },
        showError = { state ->
            Toast.makeText(
                context,
                state.failure?.userText(context)
                    ?: if (state.message == null) context.getString(R.string.ai_scan_no_exercises)
                    else context.getString(R.string.ai_scan_failed),
                Toast.LENGTH_LONG
            ).show()
        },
        resourceAnalytics = { metrics, isDark ->
            if (aiResourceAnalyticsEnabled) DeviceResourceAnalyticsCard(metrics, isDark)
        }
    )

    val isScanActive = scan.isScanning
    // AI scan (LLM inference) can take a while: keep the screen on for its whole duration.
    // Always on, no setting. Released when scanning ends or the screen is left.
    val scanWindow = (context as? Activity)?.window
    DisposableEffect(isScanActive) {
        if (isScanActive) {
            scanWindow?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            scanWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            scanWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    return scan
}

internal fun createScanTempImageUri(context: android.content.Context): android.net.Uri {
    val tempFile = java.io.File(context.cacheDir, "ai_scan_temp_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

@Composable
internal fun AiScanningOverlay(
    phase: com.emanuel5014.trainable.data.ai.ScanPhase,
    stream: AiScanStreamState,
    showResourceAnalytics: Boolean = false,
    isDark: Boolean,
    hazeState: HazeState?,
    onCancel: () -> Unit
) {
    ScanProgressOverlay(phase, stream, isDark, hazeState, onCancel) {
        if (showResourceAnalytics) stream.metrics?.let { DeviceResourceAnalyticsCard(it, isDark) }
    }
}

@Composable
private fun DeviceResourceAnalyticsCard(
    metrics: com.emanuel5014.trainable.data.ai.DeviceResourceMetrics,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceContainerHighest.copy(alpha = if (isDark) 0.6f else 0.75f),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Analytics,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.ai_analytics_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                }

                // Backend / Model tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = metrics.inferenceBackend,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // CPU Usage Section
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${stringResource(R.string.ai_analytics_cpu)} (${metrics.cpuCores} Core)",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${metrics.cpuUsagePercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                LinearProgressIndicator(
                    progress = { (metrics.cpuUsagePercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (metrics.cpuUsagePercent > 85) Error else Primary,
                    trackColor = SurfaceContainer
                )
            }

            // RAM Section
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Analytics,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.ai_analytics_ram),
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${metrics.appRamUsedMb} MB App · ${"%.1f".format(metrics.systemRamUsedGb)}/${"%.1f".format(metrics.systemRamTotalGb)} GB (${metrics.systemRamPercent}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                LinearProgressIndicator(
                    progress = { (metrics.systemRamPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (metrics.systemRamPercent > 85) Error else Primary,
                    trackColor = SurfaceContainer
                )
            }

            // 2-Column Metrics: Speed / Throughput & Battery / Thermal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speed Chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceContainer.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.ai_analytics_speed),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = if (metrics.throughputTokPerSec > 0f) {
                                    "${"%.1f".format(metrics.throughputTokPerSec)} tok/s"
                                } else {
                                    "0.0 tok/s"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnSurface
                            )
                        }
                    }
                }

                // Battery & Temp Chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (metrics.isThermalThrottling) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f) else SurfaceContainer.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (metrics.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                            contentDescription = null,
                            tint = if (metrics.isThermalThrottling) MaterialTheme.colorScheme.onErrorContainer else Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.ai_analytics_battery_temp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (metrics.isThermalThrottling) MaterialTheme.colorScheme.onErrorContainer else OnSurfaceVariant
                            )
                            Text(
                                text = buildString {
                                    if (metrics.batteryPercent != null) append("${metrics.batteryPercent}%")
                                    if (metrics.batteryTemperatureC != null) {
                                        if (isNotEmpty()) append(" · ")
                                        append("${"%.1f".format(metrics.batteryTemperatureC)}°C")
                                    }
                                    if (isEmpty()) append("--")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (metrics.isThermalThrottling) MaterialTheme.colorScheme.onErrorContainer else OnSurface
                            )
                        }
                    }
                }
            }

            // Thermal Status Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Android Runtime",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = OnSurfaceVariant
                )
                Text(
                    text = "${stringResource(R.string.ai_analytics_thermal)}: ${metrics.thermalStatus}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (metrics.isThermalThrottling) Error else OnSurfaceVariant
                )
            }
        }
    }
}
