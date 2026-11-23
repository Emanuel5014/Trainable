package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.back
import com.emanuel5014.trainable.sharedui.resources.physical_check_after
import com.emanuel5014.trainable.sharedui.resources.physical_check_before
import com.emanuel5014.trainable.sharedui.resources.physical_check_compare_title
import com.emanuel5014.trainable.sharedui.resources.physical_check_in_days
import com.emanuel5014.trainable.sharedui.resources.physical_check_same_day
import com.emanuel5014.trainable.sharedui.resources.physical_check_side_by_side
import com.emanuel5014.trainable.sharedui.resources.physical_check_slider_mode
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.SurfaceContainerLow
import com.emanuel5014.trainable.ui.util.shortMonthName
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

private val StageShape = RoundedCornerShape(28.dp)

/**
 * Two checks side by side in time: the photos on a dark stage (a slider over each other, or next to each other),
 * then what changed (weight and days) and, when a check has several photos, the strips to pick which ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalCheckCompareContent(
    checkId1: Int,
    checkId2: Int,
    onNavigateBack: () -> Unit,
    viewModel: PhysicalCheckModel
) {
    val checks by viewModel.checks.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()

    val check1 = remember(checks, checkId1) { checks.find { it.id == checkId1 } }
    val check2 = remember(checks, checkId2) { checks.find { it.id == checkId2 } }

    if (check1 == null || check2 == null) {
        Box(modifier = Modifier.fillMaxSize().background(Surface), contentAlignment = Alignment.Center) {
            GymLoadingIndicator()
        }
        return
    }

    val (older, newer) = remember(check1, check2) {
        if (check1.timestamp <= check2.timestamp) check1 to check2 else check2 to check1
    }
    val olderPhotos = remember(older.fotoFilenames) { older.photoFilenames() }
    val newerPhotos = remember(newer.fotoFilenames) { newer.photoFilenames() }
    var olderPhoto by remember(olderPhotos) { mutableStateOf(olderPhotos.firstOrNull()) }
    var newerPhoto by remember(newerPhotos) { mutableStateOf(newerPhotos.firstOrNull()) }
    var useSlider by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.physical_check_compare_title),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back), tint = OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface, titleContentColor = OnSurface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .navigationBarsPadding()
                .padding(horizontal = ResponsiveSize.horizontalPadding)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ViewToggle(useSlider = useSlider, onChange = { useSlider = it })

            // The stage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(StageShape)
                    .background(Color.Black)
            ) {
                val beforeLabel = stringResource(Res.string.physical_check_before)
                val afterLabel = stringResource(Res.string.physical_check_after)
                if (useSlider && olderPhoto != null && newerPhoto != null) {
                    BeforeAfterSlider(
                        before = { StagePhoto(olderPhoto, viewModel) },
                        after = { StagePhoto(newerPhoto, viewModel) },
                        beforeLabel = { StageLabel(beforeLabel) },
                        afterLabel = { StageLabel(afterLabel) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            StagePhoto(olderPhoto, viewModel)
                            StageLabel(beforeLabel, Modifier.align(Alignment.TopStart).padding(12.dp))
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            StagePhoto(newerPhoto, viewModel)
                            StageLabel(afterLabel, Modifier.align(Alignment.TopEnd).padding(12.dp))
                        }
                    }
                }
            }

            ChangeSummary(older, newer, weightUnit)

            if (olderPhotos.size > 1 || newerPhotos.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PhotoStrip(olderPhotos, olderPhoto, viewModel, Modifier.weight(1f)) { olderPhoto = it }
                    PhotoStrip(newerPhotos, newerPhoto, viewModel, Modifier.weight(1f)) { newerPhoto = it }
                }
            }
        }
    }
}

@Composable
private fun ViewToggle(useSlider: Boolean, onChange: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        listOf(true to Res.string.physical_check_slider_mode, false to Res.string.physical_check_side_by_side)
            .forEachIndexed { index, (slider, label) ->
                SegmentedButton(
                    selected = useSlider == slider,
                    onClick = { onChange(slider) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Primary.copy(alpha = 0.15f),
                        activeContentColor = Primary,
                        activeBorderColor = Primary.copy(alpha = 0.3f),
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = OnSurfaceVariant,
                        inactiveBorderColor = SurfaceContainerHighest
                    ),
                    label = { Text(stringResource(label), maxLines = 1) }
                )
            }
    }
}

@Composable
private fun BoxScope.StagePhoto(filename: String?, model: PhysicalCheckModel) {
    if (filename != null) {
        DecryptedImage(filename = filename, viewModel = model, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun StageLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** Before and after with their dates and weights, and in the middle how much changed in how many days. */
@Composable
private fun ChangeSummary(older: PhysicalCheckEntity, newer: PhysicalCheckEntity, weightUnit: String) {
    val days = remember(older.timestamp, newer.timestamp) { daysBetween(older.timestamp, newer.timestamp, TimeZone.currentSystemDefault()) }
    val change = if (older.peso != null && newer.peso != null) newer.peso!! - older.peso!! else null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckSide(stringResource(Res.string.physical_check_before), older, weightUnit, Alignment.Start, Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
            if (change != null) {
                Text(
                    text = formatWeightChange(change, weightUnit),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = weightChangeColor(change),
                    maxLines = 1
                )
            }
            Text(
                text = if (days == 0) stringResource(Res.string.physical_check_same_day)
                else pluralStringResource(Res.plurals.physical_check_in_days, days, days),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
        CheckSide(stringResource(Res.string.physical_check_after), newer, weightUnit, Alignment.End, Modifier.weight(1f))
    }
}

@Composable
private fun CheckSide(label: String, check: PhysicalCheckEntity, weightUnit: String, alignment: Alignment.Horizontal, modifier: Modifier) {
    val date = remember(check.timestamp) {
        Instant.fromEpochMilliseconds(check.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    }
    val dateText = "${date.day} ${shortMonthName(date.month.ordinal + 1)} ${date.year}"
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant, maxLines = 1)
        Text(dateText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = OnSurface, maxLines = 1)
        check.peso?.let {
            Text(formatBodyWeight(it, weightUnit), style = MaterialTheme.typography.bodyMedium, color = OnSurface, maxLines = 1)
        }
    }
}

/** The photos of one check, to pick the one on the stage; empty space when it has only one. */
@Composable
private fun PhotoStrip(
    photos: List<String>,
    selected: String?,
    model: PhysicalCheckModel,
    modifier: Modifier,
    onSelect: (String) -> Unit
) {
    Box(modifier = modifier) {
        if (photos.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(0.dp)) {
                items(photos) { photo ->
                    val isSelected = photo == selected
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .then(if (isSelected) Modifier.border(3.dp, Primary, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable(role = Role.Image) { onSelect(photo) }
                    ) {
                        DecryptedImage(filename = photo, viewModel = model, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
            }
        }
    }
}
