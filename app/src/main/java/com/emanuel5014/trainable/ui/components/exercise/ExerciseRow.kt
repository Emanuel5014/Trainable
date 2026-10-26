package com.emanuel5014.trainable.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.components.glass.glassPanelBackground

/**
 * An exercise as every list shows it, the exercises page and both pickers alike: the [thumbnail] (when the list has them),
 * the name, and under it the star of a favorite, the category and the pills of a custom or a hidden exercise, then
 * [badges]. What only some lists have goes in [trailing] (the info button of a picker). The badges sit under the name
 * rather than beside it so a long name keeps the whole width the thumbnail and the button leave it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseRow(
    name: String,
    category: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: (@Composable () -> Unit)? = null,
    isCustom: Boolean = false,
    isHidden: Boolean = false,
    isFavorite: Boolean = false,
    containerColor: Color = SurfaceContainerHigh,
    badges: @Composable () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.medium)
            .glassPanelBackground(containerColor, Shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        thumbnail?.invoke()
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
                if (isFavorite) FavoriteMarker()
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Lighter than the row, or the pill would be a bare label
                if (isCustom) PrescriptionPill(text = stringResource(R.string.custom), containerColor = SurfaceContainerHighest)
                if (isHidden) DeletedExercisePill(containerColor = SurfaceContainerHighest)
                badges()
            }
        }
        trailing?.invoke()
    }
}

/**
 * A row of the two pickers (the add / pick sheet and the swap sheet): the tap picks the exercise, the thumbnail opens its
 * media, and [onInfoClick] (none where the sheet only chooses) its detail. [isSelected] marks the exercise chosen before
 * coming back from the next step; [isCurrent] the one being replaced.
 */
@Composable
internal fun ExercisePickRow(
    exercise: ExerciseEntity,
    languageCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isCurrent: Boolean = false,
    showThumbnail: Boolean = false,
    onThumbnailClick: () -> Unit = {},
    onInfoClick: (() -> Unit)? = null
) {
    ExerciseRow(
        name = ExerciseTranslations.translate(exercise.nome, languageCode),
        category = ExerciseTranslations.translateCategory(exercise.categoria, languageCode),
        onClick = onClick,
        modifier = modifier,
        thumbnail = if (showThumbnail) {
            { ExerciseListThumbnail(fileName = exercise.mediaPath, onClick = onThumbnailClick) }
        } else null,
        isCustom = exercise.id >= ExerciseEntity.FIRST_CUSTOM_ID,
        isFavorite = exercise.isFavorite,
        containerColor = if (isSelected) Primary.copy(alpha = 0.1f) else SurfaceContainerHigh,
        badges = {
            when {
                isSelected -> PrescriptionPill(
                    text = stringResource(R.string.exercise_picked_badge).uppercase(),
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    emphasized = true
                )
                isCurrent -> PrescriptionPill(text = stringResource(R.string.current_badge).uppercase(), containerColor = Surface)
            }
        },
        trailing = onInfoClick?.let { { ExerciseInfoButton(onClick = it) } }
    )
}
