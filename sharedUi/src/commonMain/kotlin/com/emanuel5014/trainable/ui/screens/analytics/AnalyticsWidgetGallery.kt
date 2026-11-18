package com.emanuel5014.trainable.ui.screens.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.analytics_add_1rm_graphic
import com.emanuel5014.trainable.sharedui.resources.analytics_add_widget
import com.emanuel5014.trainable.sharedui.resources.analytics_already_added
import com.emanuel5014.trainable.sharedui.resources.analytics_empty_hint_v2
import com.emanuel5014.trainable.sharedui.resources.analytics_body_weight
import com.emanuel5014.trainable.sharedui.resources.analytics_category_volume_widget
import com.emanuel5014.trainable.sharedui.resources.analytics_gallery_desc_body_weight
import com.emanuel5014.trainable.sharedui.resources.analytics_gallery_desc_calendar
import com.emanuel5014.trainable.sharedui.resources.analytics_gallery_desc_category_volume
import com.emanuel5014.trainable.sharedui.resources.analytics_gallery_desc_comparison
import com.emanuel5014.trainable.sharedui.resources.analytics_gallery_desc_exercise_chart
import com.emanuel5014.trainable.sharedui.resources.analytics_gallery_desc_progress_cards
import com.emanuel5014.trainable.sharedui.resources.analytics_gallery_desc_volume
import com.emanuel5014.trainable.sharedui.resources.analytics_progress_cards
import com.emanuel5014.trainable.sharedui.resources.analytics_time_period_comparison
import com.emanuel5014.trainable.sharedui.resources.analytics_total_volume
import com.emanuel5014.trainable.sharedui.resources.analytics_workout_calendar
import com.emanuel5014.trainable.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private class GalleryLook(val name: StringResource, val description: StringResource, val icon: ImageVector)

private fun GalleryWidget.look(): GalleryLook = when (this) {
    GalleryWidget.Calendar -> GalleryLook(Res.string.analytics_workout_calendar, Res.string.analytics_gallery_desc_calendar, Icons.Rounded.DateRange)
    GalleryWidget.BodyWeight -> GalleryLook(Res.string.analytics_body_weight, Res.string.analytics_gallery_desc_body_weight, Icons.Rounded.MonitorWeight)
    GalleryWidget.CategoryVolume -> GalleryLook(Res.string.analytics_category_volume_widget, Res.string.analytics_gallery_desc_category_volume, Icons.Rounded.BarChart)
    GalleryWidget.Volume -> GalleryLook(Res.string.analytics_total_volume, Res.string.analytics_gallery_desc_volume, Icons.Rounded.FitnessCenter)
    GalleryWidget.ProgressCards -> GalleryLook(Res.string.analytics_progress_cards, Res.string.analytics_gallery_desc_progress_cards, Icons.Rounded.EmojiEvents)
    GalleryWidget.ExerciseChart -> GalleryLook(Res.string.analytics_add_1rm_graphic, Res.string.analytics_gallery_desc_exercise_chart, Icons.AutoMirrored.Rounded.ShowChart)
    GalleryWidget.PeriodComparison -> GalleryLook(Res.string.analytics_time_period_comparison, Res.string.analytics_gallery_desc_comparison, Icons.AutoMirrored.Rounded.CompareArrows)
}

/** "Aggiungi widget" sheet: one tile per widget type, two per row, the last one full width. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsWidgetGallery(
    entries: List<GalleryEntry>,
    onPick: (GalleryWidget) -> Unit,
    onShowPresent: (key: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.emanuel5014.trainable.ui.theme.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.analytics_add_widget),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            entries.chunked(2).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(IntrinsicSize.Min)
                ) {
                    row.forEach { entry ->
                        GalleryTile(
                            entry = entry,
                            onClick = { entry.presentKey?.let(onShowPresent) ?: onPick(entry.type) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryTile(entry: GalleryEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val look = entry.type.look()
    val colors = MaterialTheme.colorScheme
    val present = entry.presentKey != null
    val alreadyAdded = stringResource(Res.string.analytics_already_added)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerHigh,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { if (present) stateDescription = alreadyAdded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(look.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(look.name),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            if (present) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = alreadyAdded,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = stringResource(look.description),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Dashed "+ Aggiungi widget" tile at the end of the analytics list. */
@Composable
fun AddWidgetTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val outline = colors.outlineVariant
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = outline,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                    )
                )
            }
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(Res.string.analytics_add_widget),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
    }
}

/**
 * Round "+" in the analytics header; opens the gallery. Same 44 dp as the old button so the big title
 * keeps its width (a "+ Widget" pill wrapped "STATISTICHE" on a Pixel 10); the words live on the end tile.
 */
@Composable
fun AddWidgetHeaderButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier.size(44.dp)
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = stringResource(Res.string.analytics_add_widget),
            modifier = Modifier.size(22.dp)
        )
    }
}

/** Shown under the header when there is no widget: icon, hint and an "Aggiungi widget" button. */
@Composable
fun AnalyticsEmptyState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Insights,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = colors.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(Spacing.medium))
        Text(
            text = stringResource(Res.string.analytics_empty_hint_v2),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = colors.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(Spacing.medium))
        FilledTonalButton(onClick = onAdd) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(Res.string.analytics_add_widget))
        }
    }
}
