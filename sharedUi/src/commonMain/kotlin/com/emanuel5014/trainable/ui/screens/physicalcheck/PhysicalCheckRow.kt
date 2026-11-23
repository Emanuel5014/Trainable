package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.physical_check_count
import com.emanuel5014.trainable.sharedui.resources.physical_check_photo_count
import com.emanuel5014.trainable.ui.screens.routines.RowCorners
import com.emanuel5014.trainable.ui.screens.routines.toShape
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.SurfaceContainerLow
import com.emanuel5014.trainable.ui.util.fullDate
import com.emanuel5014.trainable.ui.util.monthName
import com.emanuel5014.trainable.ui.util.shortDayName
import com.emanuel5014.trainable.ui.util.shortMonthName
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import kotlin.time.Instant

/** Space between the cover photo and the row's edge; the cover's corners are the row's minus this. */
private const val COVER_INSET = 6f

/** The colour a weight change is written in: a loss in primary, a gain or no change in the plain variant colour. */
@Composable
internal fun weightChangeColor(kg: Float): Color = if (kg < -0.05f) Primary else OnSurfaceVariant

/**
 * One check of the list, joined to its month's other checks by [corners]: the first photo as a portrait cover on the
 * left, then the date, the weight with its change since the check before, and the photo count with the note.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PhysicalCheckRow(
    check: PhysicalCheckEntity,
    weightChangeKg: Float?,
    corners: RowCorners,
    weightUnit: String,
    languageCode: String,
    model: PhysicalCheckModel,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = corners.toShape()
    val coverShape = corners.inset(COVER_INSET).toShape()
    val photos = remember(check.fotoFilenames) { check.photoFilenames() }
    val date = remember(check.timestamp) {
        Instant.fromEpochMilliseconds(check.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    }
    val dayName = remember(date.dayOfWeek, languageCode) { shortDayName(date.dayOfWeek) }
    val monthShort = remember(date.month, languageCode) { shortMonthName(date.month.ordinal + 1) }
    val dateDescription = remember(check.timestamp, languageCode) { fullDate(check.timestamp) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) Primary.copy(alpha = 0.1f).compositeOver(SurfaceContainerLow) else SurfaceContainerLow)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(COVER_INSET.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhysicalCheckCover(
                filename = photos.firstOrNull(),
                model = model,
                shape = coverShape,
                modifier = Modifier.size(width = 60.dp, height = 80.dp)
            )

            Column(
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${date.day} $monthShort · $dayName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface,
                    maxLines = 1,
                    modifier = Modifier.semantics { contentDescription = dateDescription }
                )
                val peso = check.peso
                if (peso != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = formatBodyWeight(peso, weightUnit),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            maxLines = 1
                        )
                        if (weightChangeKg != null) {
                            Text(
                                text = formatWeightChange(weightChangeKg, weightUnit),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = weightChangeColor(weightChangeKg),
                                maxLines = 1
                            )
                        }
                    }
                }
                val details = buildList {
                    if (photos.isNotEmpty()) add(pluralStringResource(Res.plurals.physical_check_photo_count, photos.size, photos.size))
                    check.note?.takeIf { it.isNotBlank() }?.let { add(it.replace('\n', ' ')) }
                }
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelectionMode) {
                SelectionMark(isSelected, Modifier.padding(end = 8.dp))
            }
        }
    }
}

/** The first photo of a check, cropped to [shape]; an icon on a tonal tile when the check has no photo. */
@Composable
internal fun PhysicalCheckCover(filename: String?, model: PhysicalCheckModel, shape: Shape, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(shape).background(SurfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (filename != null) {
            DecryptedImage(filename = filename, viewModel = model, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(22.dp))
        }
    }
}

/** The round tick of selection mode. */
@Composable
internal fun SelectionMark(isSelected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (isSelected) Primary else Color.Transparent, CircleShape)
            .border(
                width = 2.dp,
                color = if (isSelected) Primary else OnSurfaceVariant.copy(alpha = 0.6f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

/** The head of a month: its name (with the year when not this year) and, on the right, its checks and weight change. */
@Composable
internal fun PhysicalCheckMonthHeader(month: PhysicalCheckMonth, weightUnit: String, currentYear: Int, languageCode: String) {
    val name = remember(month.month, languageCode) { monthName(month.month) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() }
            .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name + if (month.year != currentYear) " ${month.year}" else "",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        val count = month.checks.size
        Text(
            text = pluralStringResource(Res.plurals.physical_check_count, count, count),
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceVariant,
            maxLines = 1
        )
        month.weightChangeKg?.let { change ->
            Text(
                text = "· " + formatWeightChange(change, weightUnit),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = weightChangeColor(change),
                maxLines = 1
            )
        }
    }
}

/** A plain rounded shape, used where a photo sits on its own (sheet pager, editor strip). */
internal val PhotoTileShape = RoundedCornerShape(16.dp)
