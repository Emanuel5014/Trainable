package com.emanuel5014.trainable.ui.screens.settings

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun PersonalizationSettingsScreen(onNavigateBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val wallpaperColors by viewModel.wallpaperColors.collectAsState()
    PersonalizationSettingsContent(onNavigateBack = onNavigateBack, state = viewModel.state,
        wallpaperColors = wallpaperColors, dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        glassSupported = true)
}
