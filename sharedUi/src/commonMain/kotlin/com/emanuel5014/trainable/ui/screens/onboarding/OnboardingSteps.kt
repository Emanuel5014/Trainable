package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon

/** The setup steps in the order the user meets them. */
enum class OnboardingStep { Welcome, You, Style, Tweaks, Look, Alerts, Ai, Data, Ready }

/** Steps of the setup. The AI step is always there: phones that can't run the on-device model offer providers. */
fun onboardingSteps(): List<OnboardingStep> = OnboardingStep.entries.toList()

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
