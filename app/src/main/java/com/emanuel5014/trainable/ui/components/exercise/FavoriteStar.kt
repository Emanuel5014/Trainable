package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import com.emanuel5014.trainable.ui.components.glass.GlassIconToggleButton as IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.Primary

/** A small filled star that marks a favorite exercise in a row. It is a sign, not a button: the star is changed in the detail. */
@Composable
fun FavoriteMarker(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Rounded.Star,
        contentDescription = stringResource(R.string.exercise_favorite_marker),
        tint = Primary,
        modifier = modifier.size(16.dp)
    )
}

/** The star of the exercise detail: filled when [favorite], and [onFavoriteChange] gets the state the tap asks for. */
@Composable
fun FavoriteToggle(favorite: Boolean, onFavoriteChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    IconToggleButton(checked = favorite, onCheckedChange = onFavoriteChange, modifier = modifier) {
        Icon(
            imageVector = if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = stringResource(if (favorite) R.string.exercise_favorite_remove else R.string.exercise_favorite_add),
            tint = if (favorite) Primary else OnSurface
        )
    }
}
