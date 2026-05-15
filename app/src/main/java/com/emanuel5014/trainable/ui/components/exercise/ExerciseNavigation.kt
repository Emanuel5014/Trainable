package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary

@Composable
fun ExerciseNavigation(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean,
    previousName: String? = null,
    nextName: String? = null,
    modifier: Modifier = Modifier,
    languageCode: String = "en"
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button
        if (hasPrevious) {
            TextButton(
                onClick = onPrevious,
                colors = ButtonDefaults.textButtonColors(contentColor = OnSurface)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_exercise_btn)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.previous_exercise_btn).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = previousName?.let { ExerciseTranslations.translate(it, languageCode) } ?: stringResource(R.string.previous_exercise_btn),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Next Button
        if (hasNext) {
            TextButton(
                onClick = onNext,
                colors = ButtonDefaults.textButtonColors(contentColor = OnSurface)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.next_exercise_btn).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = nextName?.let { ExerciseTranslations.translate(it, languageCode) } ?: stringResource(R.string.next_exercise_btn),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_exercise_btn)
                )
            }
        } else {
            // Finish Button or placeholder if no next
            FilledTonalButton(
                onClick = onNext,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.finish),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
