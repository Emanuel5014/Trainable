package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface as SheetBackground
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.SurfaceContainerLow
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.util.UriMigrationHelper

/**
 * The "add workout" sheet of the history: an empty workout on top, then the routines the way they are meant to be
 * picked: the ones in use first, with what each contains and when it was last done, and the archived ones in a
 * section of their own that stays closed until asked for (or until a search finds something in it).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddWorkoutSheetContent(
    plans: List<WorkoutPlanEntity>,
    summaries: Map<Int, PlanSummary>,
    lastDoneByPlan: Map<Int, Long>,
    languageCode: String,
    onPlanSelected: (Int) -> Unit,
    onEmptyWorkoutClick: () -> Unit
) {
    val items = remember(plans, summaries, lastDoneByPlan, languageCode) {
        plans.map { plan ->
            val summary = summaries[plan.id]
            AddWorkoutPlan(
                plan = plan,
                summary = summary,
                muscleLabels = summary?.muscleGroups.orEmpty().map { ExerciseTranslations.translateCategory(it, languageCode) },
                lastDone = lastDoneByPlan[plan.id]
            )
        }
    }
    var query by remember { mutableStateOf("") }
    var archivedOpen by remember { mutableStateOf(false) }

    val routineCount = remember(items) { AddWorkoutPlans.routineCount(items) }
    val showSearch = routineCount > AddWorkoutPlans.SEARCH_MIN_ROUTINES
    val activeQuery = if (showSearch) query.trim() else ""
    val groups = remember(items, activeQuery) { AddWorkoutPlans.group(items, activeQuery) }
    // A search shows what it found in the archive without making the user open it first
    val archivedVisible = archivedOpen || activeQuery.isNotEmpty()

    val listState = rememberLazyListState()
    // The search stays pinned under the title and the empty-workout row: typing brings the results up under it
    val searchIndex = 2
    LaunchedEffect(activeQuery) {
        if (activeQuery.isNotEmpty()) listState.scrollToItem(searchIndex)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "title") {
            Column(
                modifier = Modifier.padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_workout),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = OnSurface
                )
                Text(
                    text = stringResource(R.string.select_plan_to_add),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }

        item(key = "empty-workout") { EmptyWorkoutRow(onClick = onEmptyWorkoutClick) }

        if (showSearch) {
            stickyHeader(key = "search") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SheetBackground)
                        .padding(vertical = 6.dp)
                ) {
                    GymInputField(
                        value = query,
                        onValueChange = { query = it },
                        label = stringResource(R.string.search_routines),
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { query = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                            }
                        } else null,
                        containerColor = SurfaceContainerHighest
                    )
                }
            }
        }

        when {
            routineCount == 0 -> item(key = "none") {
                EmptyMessage(stringResource(R.string.no_active_routines_message))
            }
            groups.isEmpty -> item(key = "no-results") {
                EmptyMessage(stringResource(R.string.no_results_filters))
            }
            else -> {
                if (groups.active.isNotEmpty() || activeQuery.isEmpty()) {
                    item(key = "active-header") {
                        SectionHeader(label = stringResource(R.string.active_routines), count = groups.active.size)
                    }
                    if (groups.active.isEmpty()) {
                        item(key = "active-empty") {
                            EmptyMessage(stringResource(R.string.no_active_routines))
                        }
                    }
                    items(groups.active, key = { "active-${it.plan.id}" }) { item ->
                        PlanRow(item = item, archived = false, onClick = { onPlanSelected(item.plan.id) })
                    }
                }
                if (groups.archived.isNotEmpty()) {
                    item(key = "archived-header") {
                        SectionHeader(
                            label = stringResource(R.string.archived_routines_header),
                            count = groups.archived.size,
                            icon = Icons.Rounded.Archive,
                            // While a search is on, the section is open and there is nothing to toggle
                            expanded = if (activeQuery.isEmpty()) archivedVisible else null,
                            onToggle = { archivedOpen = !archivedOpen }
                        )
                    }
                    if (archivedVisible) {
                        items(groups.archived, key = { "archived-${it.plan.id}" }) { item ->
                            PlanRow(item = item, archived = true, onClick = { onPlanSelected(item.plan.id) })
                        }
                    }
                }
            }
        }
    }
}

/** An empty workout to fill in by hand: an action, so it looks different from the routines listed under it. */
@Composable
private fun EmptyWorkoutRow(onClick: () -> Unit) {
    TappableCard(
        onClick = onClick,
        containerColor = Primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AddBox, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.add_empty_workout),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
                Text(
                    text = stringResource(R.string.custom_workout),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

/**
 * Caption of a group of routines with how many there are. With [expanded] set it is a button that opens and
 * closes the group (a chevron says which); without it, it is just a label.
 */
@Composable
private fun SectionHeader(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    expanded: Boolean? = null,
    onToggle: () -> Unit = {}
) {
    val chevron by animateFloatAsState(targetValue = if (expanded == true) 180f else 0f, label = "archive_chevron")
    val toggleLabel = stringResource(if (expanded == true) R.string.hide_archived_routines else R.string.show_archived_routines)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(
                if (expanded != null) Modifier.clickable(onClickLabel = toggleLabel, role = Role.Button, onClick = onToggle)
                else Modifier
            )
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceVariant,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = Primary,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
        if (expanded != null) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(chevron)
            )
        }
    }
}

@Composable
private fun PlanRow(item: AddWorkoutPlan, archived: Boolean, onClick: () -> Unit) {
    val plan = item.plan
    TappableCard(
        onClick = onClick,
        // The routines in use stand out a little more than the archived ones
        containerColor = if (archived) SurfaceContainerLow else SurfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (archived) SurfaceContainerHighest else Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (plan.imageUri != null) {
                    val context = LocalContext.current
                    val model = remember(plan.imageUri) { UriMigrationHelper.fixPath(plan.imageUri, context) ?: plan.imageUri }
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        if (archived) Icons.Rounded.Archive else Icons.AutoMirrored.Rounded.Notes,
                        contentDescription = null,
                        tint = if (archived) OnSurfaceVariant else Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = plan.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val details = planDetails(item)
                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                item.lastDone?.let { lastDone ->
                    Text(
                        text = stringResource(R.string.add_workout_last_done, DateFormatter.formatShort(lastDone)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (archived) OnSurfaceVariant else Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = OnSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/** "5 exercises · Legs, Arms": what the routine holds, empty when nothing is known about it. */
@Composable
private fun planDetails(item: AddWorkoutPlan): String {
    val count = item.summary?.exerciseCount ?: return ""
    val parts = buildList {
        add(pluralStringResource(R.plurals.add_workout_exercise_count, count, count))
        if (item.muscleLabels.isNotEmpty()) add(item.muscleLabels.joinToString(", "))
    }
    return parts.joinToString(" · ")
}

/** A full-width rounded card that is one big button; compact on purpose so several routines fit on screen. */
@Composable
private fun TappableCard(
    onClick: () -> Unit,
    containerColor: Color,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = border
    ) {
        Box(modifier = Modifier.padding(12.dp)) { content() }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = OnSurfaceVariant
    )
}
