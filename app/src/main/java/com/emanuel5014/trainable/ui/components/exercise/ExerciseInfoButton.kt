package com.emanuel5014.trainable.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant

/** Opens the detail screen of an exercise: its media, records and history. The same everywhere an exercise is shown. */
@Composable
fun ExerciseInfoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = stringResource(R.string.exercise_detail_open),
            tint = OnSurfaceVariant
        )
    }
}
