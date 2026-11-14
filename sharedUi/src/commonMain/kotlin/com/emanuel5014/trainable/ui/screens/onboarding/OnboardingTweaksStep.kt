package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.util.TimerAdjustment
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private fun TweakGroup.titleRes(): StringResource = when (this) {
    TweakGroup.Program -> Res.string.onboarding_group_program
    TweakGroup.Timers -> Res.string.onboarding_group_timers
    TweakGroup.ExercisesAndGestures -> Res.string.onboarding_group_exercises
    TweakGroup.Journal -> Res.string.onboarding_group_journal
}

/** A load step as the app writes it: "2.5 kg", "5 lb". */
fun roundingLabel(value: Float, unit: String): String {
    val number = if (value % 1f == 0f) value.toInt().toString() else value.toString()
    return "$number $unit"
}

/** "Le tue funzioni": every feature in four groups, already set by the chosen style, each row one preference. */
@Composable
fun TweaksStep(choices: OnboardingChoices, hero: @Composable () -> Unit) {
    val reduceMotion = rememberReduceMotion()
    val expanded = remember { mutableStateMapOf(TweakGroup.Program to true) }
    val style by choices.style

    OnboardingPage(underTopRow = true) {
        hero()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OnboardingTitle(stringResource(Res.string.onboarding_tweaks_title))
            Text(
                stringResource(Res.string.onboarding_tweaks_subtitle, stringResource(style.nameRes())),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TweakGroup.entries.forEach { group ->
                val open = expanded[group] == true
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GroupHeader(
                        title = stringResource(group.titleRes()),
                        badge = stringResource(Res.string.onboarding_group_active, choices.activeCount(group)),
                        expanded = open,
                        onToggle = { expanded[group] = !open },
                        reduceMotion = reduceMotion
                    )
                    AnimatedVisibility(
                        visible = open,
                        enter = if (reduceMotion) expandVertically(snap()) else expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                        exit = if (reduceMotion) shrinkVertically(snap()) else shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec())
                    ) {
                        JoinedColumn(groupRows(group, choices))
                    }
                }
            }
        }
    }
}

private fun groupRows(group: TweakGroup, c: OnboardingChoices): List<@Composable (JoinedPosition) -> Unit> = when (group) {
    TweakGroup.Program -> listOf(
        rowOf(c.rpeEnabled, Res.string.rpe_setting, Res.string.rpe_setting_desc) {
            OptionLabel(stringResource(Res.string.onboarding_rpe_scale))
            val basic = stringResource(Res.string.rpe_scale_basic)
            val standard = stringResource(Res.string.rpe_scale_standard)
            val advanced = stringResource(Res.string.rpe_scale_advanced)
            OptionGroup(
                RpeScale.entries, c.rpeScale.value,
                { when (it) { RpeScale.BASIC -> basic; RpeScale.STANDARD -> standard; RpeScale.ADVANCED -> advanced } },
                { c.rpeScale.value = it }
            )
        },
        rowOf(c.startingWeight, Res.string.starting_weight, Res.string.starting_weight_desc),
        rowOf(c.setPerformance, Res.string.set_performance_setting, Res.string.set_performance_setting_desc),
        rowOf(c.advancedProgramming, Res.string.advanced_programming, Res.string.advanced_programming_desc) {
            OptionLabel(stringResource(Res.string.load_rounding))
            val unit = c.weightUnit.value
            OptionGroup(
                if (unit == "lb") LoadCalculator.INCREMENTS_LB else LoadCalculator.INCREMENTS_KG,
                c.loadRounding.floatValue,
                { roundingLabel(it, unit) },
                { c.pickRounding(it) }
            )
        },
        rowOf(c.inlineModifications, Res.string.inline_exercise_modifications, Res.string.inline_exercise_modifications_desc)
    )
    TweakGroup.Timers -> listOf(
        rowOf(c.restTimerEnabled, Res.string.rest_timer_setting, Res.string.rest_timer_setting_desc),
        rowOf(c.warmupTimerEnabled, Res.string.warmup_timer, Res.string.warmup_timer_settings_desc),
        rowOf(c.workoutTimerEnabled, Res.string.workout_timer, Res.string.workout_timer_desc),
        rowOf(c.timerAddEnabled, Res.string.timer_add_time, null) {
            OptionLabel(stringResource(Res.string.onboarding_step_seconds))
            OptionGroup(TimerAdjustment.ADD_OPTIONS, c.timerAddSeconds.intValue, { "+${it}s" }, { c.timerAddSeconds.intValue = it })
        },
        rowOf(c.timerSubtractEnabled, Res.string.timer_subtract_time, null) {
            OptionLabel(stringResource(Res.string.onboarding_step_seconds))
            OptionGroup(TimerAdjustment.SUBTRACT_OPTIONS, c.timerSubtractSeconds.intValue, { "−${it}s" }, { c.timerSubtractSeconds.intValue = it })
        },
        rowOf(c.timerShowNextSetButton, Res.string.timer_next_set_button, Res.string.timer_next_set_button_desc),
        rowOf(c.keepScreenOnSetTimer, Res.string.keep_screen_on_set_timer, Res.string.keep_screen_on_set_timer_desc),
        rowOf(c.keepScreenOnCardioTimer, Res.string.keep_screen_on_cardio_timer, Res.string.keep_screen_on_cardio_timer_desc),
        rowOf(c.autoStopCardioAtTarget, Res.string.auto_stop_cardio_at_target, Res.string.auto_stop_cardio_at_target_desc),
        rowOf(c.autoStopTimeWeightAtTarget, Res.string.auto_stop_time_weight_at_target, Res.string.auto_stop_time_weight_at_target_desc) {
            InnerSwitch(c.autoAdvanceTimedSets, Res.string.auto_advance_timed_sets)
        }
    )
    TweakGroup.ExercisesAndGestures -> listOf(
        rowOf(c.exerciseMedia, Res.string.exercise_media_show, Res.string.exercise_media_desc) {
            val small = stringResource(Res.string.onboarding_media_size_small)
            val large = stringResource(Res.string.onboarding_media_size_large)
            OptionLabel(stringResource(Res.string.exercise_media_large))
            OptionGroup(listOf(false, true), c.exerciseMediaLarge.value, { if (it) large else small }, { c.exerciseMediaLarge.value = it })
        },
        rowOf(c.plateCalculator, Res.string.plate_calculator, Res.string.plate_calculator_desc),
        rowOf(c.editablePresets, Res.string.editable_preset_exercises, Res.string.editable_preset_exercises_desc),
        rowOf(c.swipeActionsEnabled, Res.string.swipe_actions, Res.string.swipe_actions_desc),
        rowOf(c.hapticEnabled, Res.string.tactile_feedback, Res.string.tactile_feedback_desc)
    )
    TweakGroup.Journal -> listOf(
        rowOf(c.journalEnabled, Res.string.journal_title, Res.string.journal_desc) {
            InnerSwitch(c.journalPromptAtFinish, Res.string.journal_prompt_finish)
            InnerSwitch(c.journalRating, Res.string.journal_rating_setting)
        }
    )
}

private fun rowOf(
    state: MutableState<Boolean>,
    title: StringResource,
    desc: StringResource?,
    options: (@Composable () -> Unit)? = null
): @Composable (JoinedPosition) -> Unit = { position ->
    var checked by state
    SwitchRow(
        position = position,
        title = stringResource(title),
        description = desc?.let { stringResource(it) },
        checked = checked,
        onCheckedChange = { checked = it },
        options = options?.let { content -> { content() } }
    )
}

/** A sub-switch inside a row's options (journal prompt, stars). */
@Composable
private fun InnerSwitch(state: MutableState<Boolean>, title: StringResource) {
    var checked by state
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { checked = it })
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(title), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = null)
    }
}
