package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.SportsGymnastics
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.remote.ReleaseNoteItem
import com.emanuel5014.trainable.data.remote.localized
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.update_fixes_count
import com.emanuel5014.trainable.sharedui.resources.update_section_improved
import com.emanuel5014.trainable.sharedui.resources.update_section_new
import com.emanuel5014.trainable.sharedui.resources.update_show_details
import com.emanuel5014.trainable.ui.screens.routines.RowCorners
import com.emanuel5014.trainable.ui.screens.routines.routineRowCorners
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Where a release's issue numbers open. */
const val RELEASE_ISSUES_URL = "https://github.com/Emanuel5014/Trainable/issues/"

private val KNOWN_TYPES = setOf(ReleaseNoteItem.TYPE_NEW, ReleaseNoteItem.TYPE_IMPROVED, ReleaseNoteItem.TYPE_FIXED)

/**
 * The notes of a release as rows of a LazyColumn, shared by the Android update sheet and the iOS "What's new"
 * sheet: New and Improved as labelled groups of joined rows, the fixes behind one row that names their count.
 * A row with details or issues opens on tap. [expanded] holds the index in [items] of every open row.
 */
fun LazyListScope.releaseNoteItems(
    items: List<ReleaseNoteItem>,
    language: String,
    expanded: Set<Int>,
    onToggle: (Int) -> Unit,
    fixesOpen: Boolean,
    onToggleFixes: () -> Unit,
    onIssue: (Int) -> Unit
) {
    val newItems = items.filter { it.type == ReleaseNoteItem.TYPE_NEW || it.type !in KNOWN_TYPES }
    val improvedItems = items.filter { it.type == ReleaseNoteItem.TYPE_IMPROVED }
    val fixedItems = items.filter { it.type == ReleaseNoteItem.TYPE_FIXED }

    listOf("new" to newItems, "improved" to improvedItems).forEach { (key, list) ->
        if (list.isEmpty()) return@forEach
        item(key = "label-$key") {
            val label = if (key == "new") Res.string.update_section_new else Res.string.update_section_improved
            SectionLabel(stringResource(label), list.size)
        }
        itemsIndexed(list, key = { index, _ -> "$key-$index" }) { index, item ->
            val rowKey = items.indexOf(item)
            NoteRow(
                item = item,
                language = language,
                corners = routineRowCorners(index, list.size),
                expanded = rowKey in expanded,
                onToggle = { onToggle(rowKey) },
                onIssue = onIssue
            )
        }
    }

    if (fixedItems.isNotEmpty()) {
        // The head row names the count itself ("3 fixes"), so the section needs no label of its own
        item(key = "fixes-head") {
            FixesHead(
                modifier = Modifier.padding(top = 14.dp),
                count = fixedItems.size,
                open = fixesOpen,
                corners = if (fixesOpen) routineRowCorners(0, fixedItems.size + 1) else routineRowCorners(0, 1),
                onToggle = onToggleFixes
            )
        }
        if (fixesOpen) {
            itemsIndexed(fixedItems, key = { index, _ -> "fixed-$index" }) { index, item ->
                val rowKey = items.indexOf(item)
                NoteRow(
                    item = item,
                    language = language,
                    corners = routineRowCorners(index + 1, fixedItems.size + 1),
                    expanded = rowKey in expanded,
                    onToggle = { onToggle(rowKey) },
                    onIssue = onIssue
                )
            }
        }
    }
}

/** True when the notes have only fixes: their list then starts open. */
fun List<ReleaseNoteItem>.onlyFixes(): Boolean = all { it.type == ReleaseNoteItem.TYPE_FIXED }

@Composable
private fun SectionLabel(label: String, count: Int) {
    Text(
        text = "$label · $count",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp)
    )
}

private fun RowCorners.shape() =
    RoundedCornerShape(topStart = top.dp, topEnd = top.dp, bottomEnd = bottom.dp, bottomStart = bottom.dp)

@Composable
private fun IconBubble(icon: ImageVector, container: Color, content: Color) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteRow(
    item: ReleaseNoteItem,
    language: String,
    corners: RowCorners,
    expanded: Boolean,
    onToggle: () -> Unit,
    onIssue: (Int) -> Unit
) {
    val details = item.details.localized(language)
    val opens = details.isNotEmpty() || item.issues.isNotEmpty()
    val fixed = item.type == ReleaseNoteItem.TYPE_FIXED
    val colors = MaterialTheme.colorScheme
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Surface(
        shape = corners.shape(),
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .then(if (opens) Modifier.clickable(onClick = onToggle) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    icon = areaIcon(item.area),
                    container = if (fixed) colors.secondaryContainer else colors.primaryContainer,
                    content = if (fixed) colors.onSecondaryContainer else colors.onPrimaryContainer
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.localized(language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    val summary = item.summary.localized(language)
                    if (summary.isNotEmpty()) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                if (opens) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(Res.string.update_show_details),
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.rotate(chevron)
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded && opens,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 54.dp, top = 8.dp)) {
                    if (details.isNotEmpty()) {
                        Text(text = details, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    }
                    if (item.issues.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            item.issues.forEach { issue ->
                                Surface(
                                    shape = CircleShape,
                                    color = colors.surfaceContainerHighest,
                                    modifier = Modifier.clip(CircleShape).clickable { onIssue(issue) }
                                ) {
                                    Text(
                                        text = "#$issue",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FixesHead(count: Int, open: Boolean, corners: RowCorners, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val chevron by animateFloatAsState(if (open) 180f else 0f, label = "fixes chevron")
    Surface(shape = corners.shape(), color = colors.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(Icons.Rounded.Build, colors.secondaryContainer, colors.onSecondaryContainer)
            Spacer(Modifier.width(14.dp))
            Text(
                text = pluralStringResource(Res.plurals.update_fixes_count, count, count),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = stringResource(Res.string.update_show_details),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.rotate(chevron)
            )
        }
    }
}

/** The icon of a part of the app named in the notes (see tools/release_notes.py for the list). */
fun areaIcon(area: String): ImageVector = when (area) {
    "updates" -> Icons.Rounded.SystemUpdate
    "dashboard" -> Icons.Rounded.SpaceDashboard
    "routines" -> Icons.Rounded.ViewAgenda
    "history" -> Icons.Rounded.CalendarMonth
    "analytics" -> Icons.Rounded.Insights
    "workouts" -> Icons.Rounded.FitnessCenter
    "exercises" -> Icons.Rounded.SportsGymnastics
    "timers" -> Icons.Rounded.Timer
    "onboarding" -> Icons.Rounded.WavingHand
    "notifications" -> Icons.Rounded.Notifications
    "appearance" -> Icons.Rounded.Palette
    "liquid_glass" -> Icons.Rounded.BubbleChart
    "backup" -> Icons.Rounded.CloudSync
    "import_export" -> Icons.Rounded.ImportExport
    "ai" -> Icons.Rounded.AutoAwesome
    "widgets" -> Icons.Rounded.Widgets
    "journal" -> Icons.Rounded.EditNote
    "settings" -> Icons.Rounded.Tune
    "physical_check" -> Icons.Rounded.MonitorWeight
    "report" -> Icons.Rounded.Description
    else -> Icons.Rounded.NewReleases
}
