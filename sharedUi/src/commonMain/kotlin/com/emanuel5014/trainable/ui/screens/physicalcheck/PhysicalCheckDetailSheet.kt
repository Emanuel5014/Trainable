package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.delete
import com.emanuel5014.trainable.sharedui.resources.edit
import com.emanuel5014.trainable.sharedui.resources.physical_check_add_photos
import com.emanuel5014.trainable.sharedui.resources.physical_check_compare_with
import com.emanuel5014.trainable.sharedui.resources.physical_check_photos
import com.emanuel5014.trainable.sharedui.resources.physical_check_since_last
import com.emanuel5014.trainable.sharedui.resources.physical_check_weight
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymButtonText
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerLow
import com.emanuel5014.trainable.ui.util.fullDate
import org.jetbrains.compose.resources.stringResource

/** The grab handle of the physical-check sheets, the same as the history sheet. */
@Composable
internal fun PhysicalCheckSheetHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(CircleShape)
            .background(OnSurfaceVariant.copy(alpha = 0.4f))
    )
}

/**
 * One check opened from the list: the full date with edit and delete (only when swipe actions are off, as in the
 * history), the photos in a pager that peeks at the next one and ends with an "add photos" tile, the numbers, the
 * note and the way into comparing it with another check.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhysicalCheckDetailSheet(
    check: PhysicalCheckEntity,
    weightChangeKg: Float?,
    weightUnit: String,
    model: PhysicalCheckModel,
    showEditDelete: Boolean,
    canCompare: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCompare: () -> Unit,
    onAddPhotos: () -> Unit,
    onOpenPhoto: (filenames: List<String>, index: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val photos = remember(check.fotoFilenames) { check.photoFilenames() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        dragHandle = { PhysicalCheckSheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ResponsiveSize.horizontalPadding)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            // Header: the date and, without swipe actions, edit and delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // The photo count is in its own tile below
                Text(
                    text = fullDate(check.timestamp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (showEditDelete) {
                    GymIconButton(
                        icon = Icons.Rounded.Edit,
                        onClick = onEdit,
                        description = stringResource(Res.string.edit),
                        containerColor = SurfaceContainerHigh,
                        contentColor = Primary
                    )
                    GymIconButton(
                        icon = Icons.Rounded.Delete,
                        onClick = onDelete,
                        description = stringResource(Res.string.delete),
                        containerColor = Error.copy(alpha = 0.12f),
                        contentColor = Error
                    )
                }
            }

            PhotoPager(photos = photos, model = model, onOpenPhoto = onOpenPhoto, onAddPhotos = onAddPhotos)

            // The numbers: weight and change only when the check has a weight
            val peso = check.peso
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (peso != null) {
                    StatTile(formatBodyWeight(peso, weightUnit), stringResource(Res.string.physical_check_weight), Modifier.weight(1f))
                    if (weightChangeKg != null) {
                        StatTile(
                            value = formatWeightChange(weightChangeKg, weightUnit),
                            label = stringResource(Res.string.physical_check_since_last),
                            valueColor = weightChangeColor(weightChangeKg),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                StatTile(photos.size.toString(), stringResource(Res.string.physical_check_photos), Modifier.weight(1f))
            }

            check.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLow)
                        .padding(16.dp)
                )
            }

            if (canCompare) {
                GymButton(
                    onClick = onCompare,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Primary.copy(alpha = 0.12f),
                    contentColor = Primary
                ) {
                    Icon(Icons.Rounded.Compare, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    GymButtonText(stringResource(Res.string.physical_check_compare_with), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

/** The photos in portrait pages, the next one peeking in; the last page adds photos. */
@Composable
private fun PhotoPager(
    photos: List<String>,
    model: PhysicalCheckModel,
    onOpenPhoto: (List<String>, Int) -> Unit,
    onAddPhotos: () -> Unit
) {
    if (photos.isEmpty()) {
        AddPhotoTile(onClick = onAddPhotos, modifier = Modifier.fillMaxWidth().height(160.dp))
        return
    }
    val pagerState = rememberPagerState { photos.size + 1 }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(end = 56.dp),
        pageSpacing = 8.dp,
        beyondViewportPageCount = 1
    ) { page ->
        val tile = Modifier.fillMaxWidth().aspectRatio(0.75f)
        if (page < photos.size) {
            Box(
                modifier = tile
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(role = Role.Image) { onOpenPhoto(photos, page) }
            ) {
                DecryptedImage(filename = photos[page], viewModel = model, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        } else {
            AddPhotoTile(onClick = onAddPhotos, modifier = tile, shape = RoundedCornerShape(28.dp))
        }
    }
}

/** A dashed tile that adds photos. */
@Composable
internal fun AddPhotoTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    showLabel: Boolean = true
) {
    val label = stringResource(Res.string.physical_check_add_photos)
    Column(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(1.5.dp, OutlineVariant), shape)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AddAPhoto, contentDescription = if (showLabel) null else label, tint = Primary, modifier = Modifier.size(24.dp))
        }
        if (showLabel) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = OnSurface) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
