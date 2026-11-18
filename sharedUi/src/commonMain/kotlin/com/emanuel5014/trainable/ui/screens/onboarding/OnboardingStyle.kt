package com.emanuel5014.trainable.ui.screens.onboarding

import com.emanuel5014.trainable.domain.rpe.RpeScale

/** How the user trains; picking one turns on the matching features, which stay editable on the Tweaks step. */
enum class OnboardingStyle { Simple, Hypertrophy, Strength }

/** The choices a style controls. Sub-options of a feature that is off keep the preference defaults. */
data class StyleFeatures(
    val rpeEnabled: Boolean,
    val rpeScale: RpeScale,
    val startingWeight: Boolean,
    val setPerformance: Boolean,
    val advancedProgramming: Boolean,
    val plateCalculator: Boolean,
    val journalEnabled: Boolean,
    val journalPromptAtFinish: Boolean,
    val journalRating: Boolean,
    val exerciseMedia: Boolean,
    val exerciseMediaLarge: Boolean,
    val aiScan: Boolean
)

fun OnboardingStyle.features(): StyleFeatures = when (this) {
    OnboardingStyle.Simple -> StyleFeatures(
        rpeEnabled = false, rpeScale = RpeScale.BASIC, startingWeight = false, setPerformance = false,
        advancedProgramming = false, plateCalculator = false, journalEnabled = false,
        journalPromptAtFinish = true, journalRating = true, exerciseMedia = false,
        exerciseMediaLarge = true, aiScan = false
    )
    OnboardingStyle.Hypertrophy -> StyleFeatures(
        rpeEnabled = true, rpeScale = RpeScale.STANDARD, startingWeight = true, setPerformance = true,
        advancedProgramming = false, plateCalculator = false, journalEnabled = true,
        journalPromptAtFinish = true, journalRating = true, exerciseMedia = true,
        exerciseMediaLarge = true, aiScan = false
    )
    OnboardingStyle.Strength -> StyleFeatures(
        rpeEnabled = true, rpeScale = RpeScale.STANDARD, startingWeight = true, setPerformance = true,
        advancedProgramming = true, plateCalculator = true, journalEnabled = true,
        journalPromptAtFinish = true, journalRating = true, exerciseMedia = false,
        exerciseMediaLarge = true, aiScan = true
    )
}

/** The style-controlled values as they are in [choices] right now. */
private fun OnboardingChoices.currentFeatures() = StyleFeatures(
    rpeEnabled = rpeEnabled.value,
    rpeScale = rpeScale.value,
    startingWeight = startingWeight.value,
    setPerformance = setPerformance.value,
    advancedProgramming = advancedProgramming.value,
    plateCalculator = plateCalculator.value,
    journalEnabled = journalEnabled.value,
    journalPromptAtFinish = journalPromptAtFinish.value,
    journalRating = journalRating.value,
    exerciseMedia = exerciseMedia.value,
    exerciseMediaLarge = exerciseMediaLarge.value,
    aiScan = aiScanEnabled.value
)

/** Writes this style's table into [choices]; every other choice is left alone. */
fun OnboardingStyle.applyTo(choices: OnboardingChoices) {
    val f = features()
    choices.style.value = this
    choices.rpeEnabled.value = f.rpeEnabled
    choices.rpeScale.value = f.rpeScale
    choices.startingWeight.value = f.startingWeight
    choices.setPerformance.value = f.setPerformance
    choices.advancedProgramming.value = f.advancedProgramming
    choices.plateCalculator.value = f.plateCalculator
    choices.journalEnabled.value = f.journalEnabled
    choices.journalPromptAtFinish.value = f.journalPromptAtFinish
    choices.journalRating.value = f.journalRating
    choices.exerciseMedia.value = f.exerciseMedia
    choices.exerciseMediaLarge.value = f.exerciseMediaLarge
    choices.aiScanEnabled.value = f.aiScan
}

/** True when the user changed any value this style controls (so switching style would lose those edits). */
fun OnboardingStyle.isTweaked(choices: OnboardingChoices): Boolean = choices.currentFeatures() != features()
