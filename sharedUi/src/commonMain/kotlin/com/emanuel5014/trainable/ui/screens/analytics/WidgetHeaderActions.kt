package com.emanuel5014.trainable.ui.screens.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.analytics_more_options
import com.emanuel5014.trainable.sharedui.resources.analytics_period
import com.emanuel5014.trainable.sharedui.resources.analytics_remove
import com.emanuel5014.trainable.sharedui.resources.edit
import com.emanuel5014.trainable.ui.glass.LocalPlatformGlass
import org.jetbrains.compose.resources.stringResource

/** One entry of a widget's ⋮ menu. */
data class WidgetMenuAction(
    val label: String,
    val icon: ImageVector,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

/** The usual ⋮ entries: "Modifica" (when [onEdit] is given) then "Rimuovi" (when [onRemove] is given). */
@Composable
fun widgetMenuActions(onEdit: (() -> Unit)?, onRemove: (() -> Unit)?): List<WidgetMenuAction> {
    val editLabel = stringResource(Res.string.edit)
    val removeLabel = stringResource(Res.string.analytics_remove)
    return buildList {
        if (onEdit != null) add(WidgetMenuAction(editLabel, Icons.Rounded.Edit, onClick = onEdit))
        if (onRemove != null) add(WidgetMenuAction(removeLabel, Icons.Rounded.Delete, isDestructive = true, onClick = onRemove))
    }
}

/**
 * Right side of a widget header: an optional period chip (changes the period at once) and the ⋮ menu.
 * Replaces the old always-visible pencil + red X.
 */
@Composable
fun WidgetHeaderActions(
    period: AnalyticsTimeRange?,
    onPeriodChange: (AnalyticsTimeRange) -> Unit,
    menuActions: List<WidgetMenuAction>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (period != null) PeriodChip(period, onPeriodChange)
        if (menuActions.isNotEmpty()) OverflowMenu(menuActions)
    }
}

@Composable
private fun PeriodChip(period: AnalyticsTimeRange, onPeriodChange: (AnalyticsTimeRange) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = stringResource(period.labelResId)
    val description = stringResource(Res.string.analytics_period, label)
    val colors = MaterialTheme.colorScheme
    Box {
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clickable(role = Role.Button) { open = true }
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center
        ) {
            Surface(shape = CircleShape, color = colors.surfaceContainerHighest) {
                Row(
                    modifier = Modifier.height(28.dp).padding(start = 10.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LocalPlatformGlass.current.NotAnOverlay()
            AnalyticsTimeRange.entries.forEach { range ->
                DropdownMenuItem(
                    text = { Text(stringResource(range.labelResId)) },
                    trailingIcon = if (range == period) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.primary) }
                    } else null,
                    onClick = {
                        open = false
                        if (range != period) onPeriodChange(range)
                    }
                )
            }
        }
    }
}

@Composable
private fun OverflowMenu(actions: List<WidgetMenuAction>) {
    var open by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = stringResource(Res.string.analytics_more_options),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LocalPlatformGlass.current.NotAnOverlay()
            actions.forEach { action ->
                val tint = if (action.isDestructive) colors.error else colors.onSurface
                DropdownMenuItem(
                    text = { Text(action.label) },
                    leadingIcon = { Icon(action.icon, contentDescription = null) },
                    colors = MenuDefaults.itemColors(textColor = tint, leadingIconColor = tint),
                    onClick = {
                        open = false
                        action.onClick()
                    }
                )
            }
        }
    }
}
