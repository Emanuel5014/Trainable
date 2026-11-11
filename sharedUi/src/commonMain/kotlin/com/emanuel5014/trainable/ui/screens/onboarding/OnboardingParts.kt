package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Position of a row inside a joined list: big outer corners, small corners between rows (as on Routines). */
enum class JoinedPosition { Single, First, Middle, Last }

fun joinedPosition(index: Int, count: Int): JoinedPosition = when {
    count <= 1 -> JoinedPosition.Single
    index == 0 -> JoinedPosition.First
    index == count - 1 -> JoinedPosition.Last
    else -> JoinedPosition.Middle
}

fun joinedShape(position: JoinedPosition): RoundedCornerShape {
    val outer = 18.dp
    val inner = 4.dp
    return when (position) {
        JoinedPosition.Single -> RoundedCornerShape(outer)
        JoinedPosition.First -> RoundedCornerShape(outer, outer, inner, inner)
        JoinedPosition.Middle -> RoundedCornerShape(inner)
        JoinedPosition.Last -> RoundedCornerShape(inner, inner, outer, outer)
    }
}

/** A column of joined rows; each child gets its position from [rows]. */
@Composable
fun JoinedColumn(rows: List<@Composable (JoinedPosition) -> Unit>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { index, row -> row(joinedPosition(index, rows.size)) }
    }
}

/** One joined row: background and shape by [position], content padded inside. */
@Composable
fun JoinedRowSurface(position: JoinedPosition, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(joinedShape(position))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

/**
 * A switch row with title and optional description; [options] appear under it only while it is on.
 * The whole row toggles (one touch target), the switch shows a check while on.
 */
@Composable
fun SwitchRow(
    position: JoinedPosition,
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    options: (@Composable ColumnScope.() -> Unit)? = null
) {
    JoinedRowSurface(position) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = if (checked) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null
            )
        }
        if (options != null) {
            AnimatedVisibility(visible = checked, enter = expandVertically(), exit = shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = options)
            }
        }
    }
}

/** A connected button group for short choices; the chosen button rounds fully. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> OptionGroup(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = option == selected,
                onCheckedChange = { onSelect(option) },
                modifier = Modifier.weight(1f),
                // Up to five choices share one row: little side padding, and labels shrink instead of being cut
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                shapes = when {
                    options.size == 1 -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(
                    label(option),
                    maxLines = 1,
                    softWrap = false,
                    fontWeight = FontWeight.Bold,
                    autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = LocalTextStyle.current.fontSize, stepSize = 0.5.sp)
                )
            }
        }
    }
}

/** A small label above an option group. */
@Composable
fun OptionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Header of an expandable group: name, "N attive" badge, chevron that turns. */
@Composable
fun GroupHeader(title: String, badge: String?, expanded: Boolean, onToggle: () -> Unit, reduceMotion: Boolean) {
    val turn by animateFloatAsState(
        if (expanded) 180f else 0f,
        animationSpec = if (reduceMotion) snap() else MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "groupChevron"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (badge != null) {
            Text(
                badge,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.rotate(turn), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
