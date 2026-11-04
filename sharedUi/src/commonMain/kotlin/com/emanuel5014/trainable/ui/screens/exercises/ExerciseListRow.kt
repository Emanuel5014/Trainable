package com.emanuel5014.trainable.ui.screens.exercises

import com.emanuel5014.trainable.sharedui.resources.Res

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.components.ExerciseRow

/**
 * A row of the exercises page: the thumbnail (its own click: open the media or add one), the name and category, and a
 * pill for a custom or a hidden exercise. The rest of the row opens the detail. No button sits beside the text, so a long
 * name keeps its room on a small screen. With [mediaAction] false the thumbnail is only a picture, for a list where the
 * whole row opens the detail (the hidden exercises). A favorite shows a small star before the category: a sign, not a
 * button, since the star is changed in the detail. The look is [ExerciseRow]'s, shared with the exercise pickers.
 */
@Composable
fun ExerciseListRow(
    name: String,
    category: String,
    mediaFile: String?,
    isCustom: Boolean,
    isHidden: Boolean,
    onThumbnailClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mediaAction: Boolean = true,
    isFavorite: Boolean = false
) {
    val mediaUi = com.emanuel5014.trainable.ui.components.LocalExercisePlatformUi.current
    ExerciseRow(
        name = name,
        category = category,
        onClick = onClick,
        modifier = modifier,
        thumbnail = {
            // Still images only: a long list of GIFs would animate all at once
            if (mediaAction) {
                mediaUi.MediaSlot(
                    fileName = mediaFile,
                    onOpen = onThumbnailClick,
                    onAdd = onThumbnailClick,
                    size = 56.dp,
                    playing = false
                )
            } else {
                mediaUi.ListThumbnail(fileName = mediaFile, onClick = null, size = 56.dp)
            }
        },
        isCustom = isCustom,
        isHidden = isHidden,
        isFavorite = isFavorite
    )
}
