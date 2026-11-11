package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.created_on
import com.emanuel5014.trainable.sharedui.resources.expires
import com.emanuel5014.trainable.sharedui.resources.routine_exercises_count
import com.emanuel5014.trainable.sharedui.resources.today
import com.emanuel5014.trainable.sharedui.resources.week_short
import com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming
import com.emanuel5014.trainable.ui.components.LocalPlanImage
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.ui.util.narrowDayName
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/** Dark ink for the white "today" badge. */
private val TintInk = Color(0xFF1A1A1A)

/** Container colour and its content colour of a cover, from the app theme (see [routineCoverRole]). */
@Composable
internal fun coverColors(role: Int): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (role) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        else -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
}

/** The row's own shape for its place in the group (see [routineRowCorners]). */
internal fun RowCorners.toShape(): RoundedCornerShape =
    RoundedCornerShape(topStart = top.dp, topEnd = top.dp, bottomEnd = bottom.dp, bottomStart = bottom.dp)

/** Space between the cover and the row's edge; the cover's corners are the row's minus this, so they stay concentric. */
private const val COVER_INSET = 6f

/** Today's weekday index, 0 = Monday … 6 = Sunday. */
internal fun todayDayIndex(): Int =
    Clock.System.todayIn(TimeZone.currentSystemDefault()).dayOfWeek.isoDayNumber - 1

/**
 * One routine of the list: a cover on the left (its photo, or its tint with the initial) and the name, the days and
 * the muscles or expiry on the right. [position] (0-based) and [count] place it in the joined group and give its
 * number and cover colour.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RoutineRow(
    planWithDetails: PlanWithDetails,
    position: Int,
    count: Int,
    /** True when the week card sits on top of the group (the first row then joins it). */
    hasHead: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isArchived: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    languageCode: String = "en"
) {
    val plan = planWithDetails.plan
    val planImage = LocalPlanImage.current
    val imageUri = (planWithDetails.images.firstOrNull()?.imageUri ?: plan.imageUri).takeIf { planImage != null }
    val days = remember(plan.giorniSettimana) { scheduledDayIndexes(plan.giorniSettimana) }
    val isToday = !isArchived && todayDayIndex() in days
    val muscles = remember(planWithDetails.exercises) {
        planWithDetails.exercises.map { it.exercise.categoria }.distinct().filter { it.isNotBlank() }
    }
    val advancedProgramming = LocalAdvancedProgramming.current
    val (coverColor, coverInk) = coverColors(routineCoverRole(position))
    val corners = routineRowCorners(position, count, hasHead)
    val rowShape = corners.toShape()
    val coverShape = corners.inset(COVER_INSET).toShape()
    val number = "${position + 1}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(if (isSelected) Primary.copy(alpha = 0.1f).compositeOver(SurfaceContainerHigh) else SurfaceContainerHigh)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
      // The cover takes the row's height (no intrinsic measuring: the photo loader would not allow it)
      Box(modifier = Modifier.matchParentSize()) {
        // Cover: check when selected, else photo, else tint + faint shape + initial
        Box(
            modifier = Modifier
                .padding(COVER_INSET.dp)
                .width(80.dp)
                .fillMaxHeight()
                .clip(coverShape)
                .background(if (isSelected) Primary else coverColor),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = OnPrimary, modifier = Modifier.size(28.dp))
            } else {
                // Colour, shape and number always drawn: they show through when the photo is missing or broken
                // (e.g. a backup restored on another device without its photo files)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 22.dp, y = (-22).dp)
                        .size(72.dp)
                        .rotate(20f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(coverInk.copy(alpha = 0.08f))
                )
                Text(
                    text = number,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = coverInk.copy(alpha = 0.92f),
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 6.dp)
                )
                if (imageUri != null && planImage != null) {
                    planImage(imageUri, Modifier.fillMaxSize())
                    // The routine's colour + number on the photo, so it matches its box in the week card
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(coverColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = number,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = coverInk
                        )
                    }
                }
            }
            if (isToday && !isSelected) {
                Text(
                    text = stringResource(Res.string.today).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = TintInk,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
      }

      Row(
          modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(start = 86.dp),
          verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
            modifier = Modifier.weight(1f).padding(start = 6.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = plan.nome,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) Primary else OnSurface,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (advancedProgramming && plan.weeksCount > 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    PrescriptionPill(
                        text = stringResource(Res.string.week_short, plan.currentWeek) + "/" + plan.weeksCount,
                        containerColor = Primary.copy(alpha = 0.12f),
                        contentColor = Primary,
                        emphasized = true
                    )
                }
            }
            if (days.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    DayOfWeek.entries.forEachIndexed { index, day ->
                        val scheduled = index in days
                        Box(
                            // Shrinks on narrow rows (e.g. 360 dp phones with the reorder arrows) instead of
                            // pushing Saturday and Sunday out
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .widthIn(max = 18.dp)
                                .aspectRatio(1f)
                                .clip(if (scheduled) CircleShape else RoundedCornerShape(5.dp))
                                .then(
                                    if (scheduled) Modifier.background(Primary)
                                    else Modifier.border(1.dp, OnSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = narrowDayName(day),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (scheduled) OnPrimary else OnSurfaceVariant
                            )
                        }
                    }
                }
            }
            val expiry = plan.dataFine
            val (line, lineColor) = when {
                expiry != null -> (stringResource(Res.string.expires) + " " + DateFormatter.format(expiry)) to
                    (if (expiry < Clock.System.now().toEpochMilliseconds()) Error else Primary)
                isArchived -> (stringResource(Res.string.created_on) + " " + DateFormatter.format(plan.dataInizio)) to OnSurfaceVariant
                else -> {
                    val count = planWithDetails.exercises.size
                    val countText = pluralStringResource(Res.plurals.routine_exercises_count, count, count)
                    val muscleText = muscles.joinToString(" · ") { ExerciseTranslations.translateCategory(it, languageCode) }
                    (if (muscleText.isBlank()) countText else "$muscleText — $countText") to OnSurfaceVariant
                }
            }
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = lineColor,
                fontWeight = if (expiry != null) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelectionMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move Up", tint = OnSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move Down", tint = OnSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
      }
    }
}
