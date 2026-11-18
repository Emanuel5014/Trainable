package com.emanuel5014.trainable.ui.screens.routines

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/** The report on Android: the shared [ReportContent] with a WebView preview and the system's document picker. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        uri?.let { viewModel.saveReport(it) }
    }

    ReportContent(
        state = viewModel.model,
        onNavigateBack = onNavigateBack,
        onSave = { saveLauncher.launch(ReportModel.fileName(viewModel.model.uiState.value, System.currentTimeMillis())) },
        onShare = viewModel::shareReport,
        preview = { html, modifier, _ ->
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = false
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    }
                },
                modifier = modifier
            )
        }
    )
}
