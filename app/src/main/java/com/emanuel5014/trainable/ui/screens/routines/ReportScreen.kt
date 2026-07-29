package com.emanuel5014.trainable.ui.screens.routines

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        uri?.let { viewModel.saveReport(it) }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, context.getString(R.string.export_successful), Toast.LENGTH_SHORT).show()
            viewModel.clearSaveSuccess()
        }
    }

    Scaffold(
        containerColor = Surface,
        floatingActionButton = {
            if (uiState.html != null) {
                Column(horizontalAlignment = Alignment.End) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            val planName = uiState.reports.firstOrNull()?.planName ?: "report"
                            val safeName = planName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            saveLauncher.launch("trainable_${safeName}_${System.currentTimeMillis()}.html")
                        },
                        containerColor = SurfaceContainerHigh,
                        contentColor = Primary,
                        shape = Shapes.large
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = stringResource(R.string.save))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(modifier = Modifier.width(Spacing.small))

                    ExtendedFloatingActionButton(
                        onClick = { viewModel.shareReport() },
                        containerColor = Primary,
                        contentColor = OnPrimary,
                        shape = Shapes.large
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.share))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.share).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScreenHeader(
                title = stringResource(R.string.report),
                subtitle = stringResource(R.string.report_preview).uppercase(),
                navigationIcon = {
                    GymIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onClick = onNavigateBack,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = OnSurface,
                        description = "Back"
                    )
                },
                titleInRow = true
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GymLoadingIndicator()
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = OnSurfaceVariant
                    )
                }
            } else if (uiState.html != null) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = false
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            loadDataWithBaseURL(null, uiState.html!!, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
