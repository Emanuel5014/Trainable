package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/** How loud the way to the next exercise is: quiet while sets are left to do, the main action once the exercise is done. */
internal enum class HubNavEmphasis { Tonal, Filled }

/**
 * The way to the previous and next exercise (or to finishing the workout after the last one) in the bottom hub: the
 * one component behind both the quiet row under the logging card and the main row of an exercise that is done.
 * [previousName] and [nextName] are the exercises' own names, translated here.
 */
@Composable
internal fun HubNav(
    emphasis: HubNavEmphasis,
    previousName: String?,
    nextName: String?,
    isLast: Boolean,
    languageCode: String,
    expansion: () -> Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    // On the strip the exercise buttons are a little lower
    val navHeight = hubNavHeight(expansion())
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (previousName != null) {
            HubPreviousButton(
                previousName = ExerciseTranslations.translate(previousName, languageCode),
                expansion = expansion,
                onClick = onPrevious,
                nameStyle = MaterialTheme.typography.bodyMedium,
                nameWeight = FontWeight.Black,
                height = navHeight
            )
        } else if (emphasis == HubNavEmphasis.Tonal) {
            // Next stays on the right even with nothing on the left
            Spacer(modifier = Modifier.weight(1f))
        }
        when {
            !isLast && nextName != null -> {
                val translatedNext = ExerciseTranslations.translate(nextName, languageCode)
                if (emphasis == HubNavEmphasis.Filled) {
                    GymButton(onClick = onNext, modifier = Modifier.weight(1f), height = navHeight, pressMorph = true) {
                        NextLabel(translatedNext, labelColor = OnPrimary.copy(alpha = 0.75f), nameColor = OnPrimary, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                    }
                } else {
                    Surface(
                        onClick = onNext,
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceContainerHigh,
                        modifier = Modifier.weight(1f).height(navHeight.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NextLabel(translatedNext, labelColor = OnSurfaceVariant, nameColor = OnSurface, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.next_exercise_btn),
                                tint = OnSurface
                            )
                        }
                    }
                }
            }
            isLast && emphasis == HubNavEmphasis.Tonal -> {
                // Sets are left on the last exercise: finishing stays one tap away, quietly
                Surface(
                    onClick = onFinish,
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceContainerHigh,
                    modifier = Modifier.weight(1f).height(navHeight.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.finish).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            isLast && emphasis == HubNavEmphasis.Filled -> {
                GymButton(onClick = onFinish, modifier = Modifier.weight(1f), height = navHeight, pressMorph = true) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.finish_workout).uppercase(),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun NextLabel(name: String, labelColor: androidx.compose.ui.graphics.Color, nameColor: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.End, modifier = modifier) {
        Text(
            text = stringResource(R.string.next_exercise_btn).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = nameColor,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
