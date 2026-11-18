package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun WorkoutSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOneRepMaxes: () -> Unit = {},
    onNavigateToJournalTags: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) = WorkoutSettingsContent(onNavigateBack, onNavigateToOneRepMaxes, onNavigateToJournalTags, viewModel.state)
