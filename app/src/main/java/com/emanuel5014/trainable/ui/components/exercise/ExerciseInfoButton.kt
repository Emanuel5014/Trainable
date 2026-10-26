package com.emanuel5014.trainable.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import com.emanuel5014.trainable.ui.components.glass.GlassIconButton as IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.LocalLiquidGlassEnabled
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant

/** Opens the detail screen of an exercise: its media, records and history. The same everywhere an exercise is shown. */
@Composable
fun ExerciseInfoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (LocalLiquidGlassEnabled.current) {
        GymIconButton(onClick = onClick, modifier = modifier, icon = Icons.Rounded.Info,
            description = stringResource(R.string.exercise_detail_open), contentColor = OnSurfaceVariant,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), quiet = true)
    } else {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = stringResource(R.string.exercise_detail_open),
                tint = OnSurfaceVariant
            )
        }
    }
}
