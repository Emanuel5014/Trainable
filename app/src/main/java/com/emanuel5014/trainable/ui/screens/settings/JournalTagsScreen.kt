package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun JournalTagsScreen(onNavigateBack: () -> Unit, viewModel: JournalTagsViewModel = hiltViewModel()) =
    JournalTagsContent(onNavigateBack, viewModel.state)
