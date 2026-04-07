package com.example.gymtracking.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtracking.R
import com.example.gymtracking.data.ExerciseTranslations
import com.example.gymtracking.ui.theme.*

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
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = previousName?.let { ExerciseTranslations.translate(it, languageCode) } ?: stringResource(R.string.previous_exercise_btn),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
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
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = nextName?.let { ExerciseTranslations.translate(it, languageCode) } ?: stringResource(R.string.next_exercise_btn),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
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
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
