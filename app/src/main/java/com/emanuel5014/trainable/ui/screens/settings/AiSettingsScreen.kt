package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ai.AiModelStatus
import com.emanuel5014.trainable.data.ai.AiModelVariant
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val aiScanEnabled by viewModel.aiScanEnabled.collectAsState()
    val aiResourceAnalyticsEnabled by viewModel.aiResourceAnalyticsEnabled.collectAsState()
    val aiModelVariant by viewModel.aiModelVariant.collectAsState()
    val aiModelStatus by viewModel.aiModelStatus.collectAsState()
    val aiDeviceSupported = viewModel.aiDeviceSupported

    var showDeleteModelDialog by remember { mutableStateOf(false) }

    if (showDeleteModelDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteModelDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.ai_model_delete_confirm_title),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    stringResource(R.string.ai_model_delete_confirm_desc),
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteModelDialog = false
                        viewModel.deleteAiModel()
                    }
                ) {
                    Text(stringResource(R.string.delete).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteModelDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = Primary)
                }
            }
        )
    }

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.ai_section_title),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = OnSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GymCard(containerColor = SurfaceContainerHigh) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.DocumentScanner, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(R.string.ai_scan_toggle), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    if (!aiDeviceSupported) stringResource(R.string.ai_device_unsupported)
                                    else stringResource(R.string.ai_scan_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = aiScanEnabled && aiDeviceSupported,
                            enabled = aiDeviceSupported,
                            onCheckedChange = { viewModel.setAiScanEnabled(it) }
                        )
                    }

                    if (aiDeviceSupported && aiScanEnabled) {
                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        val selectedVariant = AiModelVariant.fromId(aiModelVariant)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.ai_model_variant), fontWeight = FontWeight.ExtraBold, color = OnSurface)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerHighest)
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AiModelVariant.entries.forEach { variant ->
                                    Text(
                                        text = variant.displayName,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedVariant == variant) Primary else Color.Transparent)
                                            .clickable { viewModel.setAiModelVariant(variant.id) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = if (selectedVariant == variant) OnPrimary else OnSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.ai_model_requirements, selectedVariant.sizeLabel, selectedVariant.requiredRamGb),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }

                        when (val status = aiModelStatus) {
                            is AiModelStatus.Downloading -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            stringResource(R.string.ai_model_downloading, (status.progress * 100).toInt()),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Primary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        TextButton(onClick = { viewModel.cancelAiModelDownload() }) {
                                            Text(
                                                stringResource(R.string.ai_download_cancel).uppercase(),
                                                color = Error,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    LinearWavyProgressIndicator(
                                        progress = { status.progress },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            is AiModelStatus.Ready -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.ai_model_ready),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = OnSurface
                                        )
                                    }
                                    TextButton(onClick = { showDeleteModelDialog = true }) {
                                        Text(
                                            stringResource(R.string.ai_model_delete).uppercase(),
                                            color = Error,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                            is AiModelStatus.Error -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(status.message, style = MaterialTheme.typography.bodySmall, color = Error)
                                    GymButton(
                                        onClick = { viewModel.downloadAiModel() },
                                        containerColor = Primary.copy(alpha = 0.1f),
                                        contentColor = Primary,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(stringResource(R.string.ai_model_download_retry))
                                    }
                                }
                            }
                            AiModelStatus.NotDownloaded -> {
                                GymButton(
                                    onClick = { viewModel.downloadAiModel() },
                                    containerColor = Primary.copy(alpha = 0.1f),
                                    contentColor = Primary,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(stringResource(R.string.ai_model_download, selectedVariant.displayName))
                                }
                            }
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Rounded.Analytics,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.ai_resource_analytics_toggle),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = OnSurface,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        stringResource(R.string.ai_resource_analytics_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsSwitch(
                                checked = aiResourceAnalyticsEnabled,
                                onCheckedChange = { viewModel.setAiResourceAnalyticsEnabled(it) }
                            )
                        }
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceContainerHighest.copy(alpha = 0.5f))
                            .border(1.dp, OutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "BETA",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Primary
                                )
                            }
                            Text(
                                text = stringResource(R.string.ai_disclaimer_beta),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
