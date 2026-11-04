package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock

/** What the user accepted from a program scan. */
data class ProgramScanApplication(
    /** Week number → blocks. In single-week mode there is only week 1. */
    val weeks: Map<Int, List<PrescriptionBlock>>,
    val oneRepMaxKg: Float?
)

/**
 * Everything the advanced-prescription editor needs from the rest of the app (1RMs, units, rounding, AI), so any
 * screen can show it without wiring them itself.
 */
interface PrescriptionEnvironment {
    val weightUnit: String
    val roundingIncrement: Float
    /** exerciseId → current 1RM in kg. */
    val oneRepMaxes: Map<Int, Float>
    /** exerciseId → Epley estimate from recent logs, in kg. */
    val estimatedOneRepMaxes: Map<Int, Float>
    /** The AI button is only offered when AI is enabled and the chosen place can read a photo. */
    val aiAvailable: Boolean

    fun saveOneRepMax(exerciseId: Int, weightKg: Float, source: String)

    /** "Scan the program page": photo → model → reviewable result, applied through [onApply]. */
    @Composable
    fun AiProgramScanButton(
        exerciseName: String?,
        singleWeek: Boolean,
        currentOneRepMaxKg: Float?,
        weightUnit: String,
        onApply: (ProgramScanApplication) -> Unit,
        modifier: Modifier,
        scanKey: String?
    )
}

/**
 * The environment of the editor being composed, from each platform (Android: a ViewModel of the screen showing the
 * editor). Called inside the editor, so whatever the platform remembers lives as long as that screen.
 */
val LocalPrescriptionEnvironment = staticCompositionLocalOf<@Composable () -> PrescriptionEnvironment> {
    { NoPrescriptionEnvironment }
}

private object NoPrescriptionEnvironment : PrescriptionEnvironment {
    override val weightUnit: String get() = "kg"
    override val roundingIncrement: Float get() = LoadCalculator.DEFAULT_INCREMENT_KG
    override val oneRepMaxes: Map<Int, Float> get() = emptyMap()
    override val estimatedOneRepMaxes: Map<Int, Float> get() = emptyMap()
    override val aiAvailable: Boolean get() = false
    override fun saveOneRepMax(exerciseId: Int, weightKg: Float, source: String) {}

    @Composable
    override fun AiProgramScanButton(
        exerciseName: String?,
        singleWeek: Boolean,
        currentOneRepMaxKg: Float?,
        weightUnit: String,
        onApply: (ProgramScanApplication) -> Unit,
        modifier: Modifier,
        scanKey: String?
    ) {}
}
