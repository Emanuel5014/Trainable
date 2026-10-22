package com.emanuel5014.trainable.ui.screens.exercises

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.DeletedExercisePill
import com.emanuel5014.trainable.ui.components.ExerciseListThumbnail
import com.emanuel5014.trainable.ui.components.ExerciseMediaSlot
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/**
 * A row of the exercises page: the thumbnail (its own click: open the media or add one), the name and category, and a
 * pill for a custom or a hidden exercise. The rest of the row opens the detail. No button sits beside the text, so a long
 * name keeps its room on a small screen. With [mediaAction] false the thumbnail is only a picture, for a list where the
 * whole row opens the detail (the hidden exercises).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun ExerciseListRow(
    name: String,
    category: String,
    mediaFile: String?,
    isCustom: Boolean,
    isHidden: Boolean,
    onThumbnailClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mediaAction: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.medium)
            .background(SurfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Still images only: a long list of GIFs would animate all at once
        if (mediaAction) {
            ExerciseMediaSlot(
                fileName = mediaFile,
                onOpen = onThumbnailClick,
                onAdd = onThumbnailClick,
                size = 56.dp,
                playing = false
            )
        } else {
            ExerciseListThumbnail(fileName = mediaFile, onClick = null, size = 56.dp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isCustom) PrescriptionPill(text = stringResource(R.string.custom))
                if (isHidden) DeletedExercisePill()
            }
        }
    }
}
