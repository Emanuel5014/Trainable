package com.emanuel5014.trainable.ui.screens.exercise

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun ExerciseDetailDialog(exerciseId: Int, onDismiss: () -> Unit, viewModel: ExerciseDetailViewModel = hiltViewModel()) =
    ExerciseDetailDialogContent(exerciseId = exerciseId, onDismiss = onDismiss, controller = viewModel.controller)
