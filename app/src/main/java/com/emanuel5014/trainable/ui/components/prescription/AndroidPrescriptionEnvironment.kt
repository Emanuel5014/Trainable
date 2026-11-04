package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/** The advanced editor's environment on Android: the PrescriptionEnvironmentViewModel of the screen showing it. */
val AndroidPrescriptionEnvironment: @Composable () -> PrescriptionEnvironment = {
    val viewModel: PrescriptionEnvironmentViewModel = hiltViewModel()
    val weightUnit by viewModel.weightUnit.collectAsState()
    val roundingIncrement by viewModel.roundingIncrement.collectAsState()
    val oneRepMaxes by viewModel.oneRepMaxes.collectAsState()
    val estimatedOneRepMaxes by viewModel.estimatedOneRepMaxes.collectAsState()
    val aiAvailable by viewModel.aiAvailable.collectAsState()
    object : PrescriptionEnvironment {
        override val weightUnit: String get() = weightUnit
        override val roundingIncrement: Float get() = roundingIncrement
        override val oneRepMaxes: Map<Int, Float> get() = oneRepMaxes
        override val estimatedOneRepMaxes: Map<Int, Float> get() = estimatedOneRepMaxes
        override val aiAvailable: Boolean get() = aiAvailable

        override fun saveOneRepMax(exerciseId: Int, weightKg: Float, source: String) =
            viewModel.saveOneRepMax(exerciseId, weightKg, source)

        @Composable
        override fun AiProgramScanButton(
            exerciseName: String?,
            singleWeek: Boolean,
            currentOneRepMaxKg: Float?,
            weightUnit: String,
            onApply: (ProgramScanApplication) -> Unit,
            modifier: Modifier,
            scanKey: String?
        ) = com.emanuel5014.trainable.ui.components.AiProgramScanButton(
            exerciseName, singleWeek, currentOneRepMaxKg, weightUnit, onApply, modifier, scanKey
        )
    }
}
