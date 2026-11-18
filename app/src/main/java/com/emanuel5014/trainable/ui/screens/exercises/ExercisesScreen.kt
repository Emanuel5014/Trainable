package com.emanuel5014.trainable.ui.screens.exercises

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun ExercisesScreen(onNavigateBack: () -> Unit, viewModel: ExercisesViewModel = hiltViewModel()) =
    ExercisesContent(onNavigateBack = onNavigateBack, controller = viewModel.controller)
