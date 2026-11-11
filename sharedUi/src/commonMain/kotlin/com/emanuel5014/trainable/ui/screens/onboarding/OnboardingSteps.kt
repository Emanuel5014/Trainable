package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon

/** The setup steps in the order the user meets them. */
enum class OnboardingStep { Welcome, You, Style, Tweaks, Look, Alerts, Ai, Data, Ready }

/**
 * Steps shown on this device. The AI step comes after Tweaks, where the switch that brings it in lives, so it can only
 * appear or vanish ahead of the user.
 */
fun onboardingSteps(aiAvailable: Boolean): List<OnboardingStep> =
    if (aiAvailable) OnboardingStep.entries.toList() else OnboardingStep.entries - OnboardingStep.Ai

/** The AI step is offered when the phone can run the on-device model and the user writes programs (advanced). */
fun aiStepAvailable(deviceSupportsModel: Boolean, advancedProgramming: Boolean): Boolean =
    deviceSupportsModel && advancedProgramming

/** The hero shape of each step: one shape that morphs as the user moves forward and ends on the filled sun. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun heroShapeFor(stepIndex: Int): RoundedPolygon {
    val shapes = listOf(
        MaterialShapes.Circle, MaterialShapes.Square, MaterialShapes.Cookie9Sided, MaterialShapes.Sunny,
        MaterialShapes.Gem, MaterialShapes.Clover4Leaf, MaterialShapes.Pill, MaterialShapes.Cookie4Sided,
        MaterialShapes.Sunny
    )
    return shapes[stepIndex.coerceIn(0, shapes.lastIndex)]
}
