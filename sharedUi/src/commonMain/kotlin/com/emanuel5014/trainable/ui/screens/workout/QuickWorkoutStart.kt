package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.quick_exercise_count
import com.emanuel5014.trainable.sharedui.resources.quick_repeat_confirm
import com.emanuel5014.trainable.sharedui.resources.quick_repeat_message
import com.emanuel5014.trainable.sharedui.resources.quick_repeat_title
import com.emanuel5014.trainable.sharedui.resources.quick_set_count
import com.emanuel5014.trainable.sharedui.resources.quick_start_add
import com.emanuel5014.trainable.sharedui.resources.quick_start_repeat
import com.emanuel5014.trainable.sharedui.resources.quick_start_subtitle
import com.emanuel5014.trainable.sharedui.resources.quick_start_title
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.screens.history.displayText
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.util.formatDayMonth
import com.emanuel5014.trainable.ui.util.uppercaseUi
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * What a quick workout with no exercise yet shows: a button to add exercises and the last workouts done, any of which
 * can be repeated (its exercises are copied with that day's sets and weights, after a confirmation).
 */
@Composable
fun QuickWorkoutStart(
    workouts: List<RepeatableWorkout>?,
    languageCode: String,
    onAddExercises: () -> Unit,
    onRepeat: (RepeatableWorkout) -> Unit,
    modifier: Modifier = Modifier
) {
    var confirming by remember { mutableStateOf<RepeatableWorkout?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.quick_start_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = OnSurface
        )
        Text(
            text = stringResource(Res.string.quick_start_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        GymButton(onClick = onAddExercises, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.quick_start_add).uppercaseUi(), fontWeight = FontWeight.ExtraBold)
        }

        if (!workouts.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.quick_start_repeat).uppercaseUi(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurfaceVariant
            )
            workouts.forEach { workout ->
                RepeatWorkoutCard(workout, languageCode, onClick = { confirming = workout })
            }
        }
        // The list ends above the hub's place, not under it
        Spacer(modifier = Modifier.height(24.dp))
    }

    confirming?.let { workout ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(stringResource(Res.string.quick_repeat_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.quick_repeat_message), color = OnSurfaceVariant)
                    workout.exercises.forEach { exercise ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ExerciseTranslations.translate(exercise.name, languageCode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = pluralStringResource(Res.plurals.quick_set_count, exercise.sets, exercise.sets),
                                style = MaterialTheme.typography.labelLarge,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                GymButton(
                    onClick = {
                        confirming = null
                        onRepeat(workout)
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.quick_repeat_confirm).uppercaseUi(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { confirming = null },
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercaseUi(), fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}

/** A past workout: when, its name, its exercises in order, and how many exercises and sets it had. */
@Composable
private fun RepeatWorkoutCard(workout: RepeatableWorkout, languageCode: String, onClick: () -> Unit) {
    val names = workout.exercises.joinToString(", ") { ExerciseTranslations.translate(it.name, languageCode) }
    val sets = workout.exercises.filterNot { it.isCardio }.sumOf { it.sets }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerHigh, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = whenText(workout.timestamp).uppercaseUi() + " · " + workout.title.displayText(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = names,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(pluralStringResource(Res.plurals.quick_exercise_count, workout.exercises.size, workout.exercises.size))
                    if (sets > 0) append(" · ").append(pluralStringResource(Res.plurals.quick_set_count, sets, sets))
                },
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )
        }
        Icon(Icons.Rounded.Replay, contentDescription = null, tint = OnSurfaceVariant)
    }
}

/** "today", "yesterday" for the last two days, then the date ("22 nov"). */
@Composable
private fun whenText(timestamp: Long): String {
    val zone = kotlinx.datetime.TimeZone.currentSystemDefault()
    val days = com.emanuel5014.trainable.ui.screens.physicalcheck.daysBetween(timestamp, kotlin.time.Clock.System.now().toEpochMilliseconds(), zone)
    return if (days <= 1) daysAgoText(timestamp) else formatDayMonth(timestamp)
}
