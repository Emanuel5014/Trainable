package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.deload_workout
import com.emanuel5014.trainable.sharedui.resources.deload_workout_desc
import com.emanuel5014.trainable.sharedui.resources.open_routine
import com.emanuel5014.trainable.sharedui.resources.rename_workout
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import com.emanuel5014.trainable.ui.glass.LocalPlatformGlass
import com.emanuel5014.trainable.ui.screens.settings.SettingsSwitch
import com.emanuel5014.trainable.ui.theme.OnSecondaryContainer
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.SecondaryContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import org.jetbrains.compose.resources.stringResource

/**
 * The menu under the workout's name in the M3 Expressive look: what to do with the workout (rename, open its
 * routine) in one rounded group, the deload switch in its own; a deload workout fills its item with the secondary
 * container, like the deload badges.
 *
 * Built on the classic [DropdownMenu] (transparent, the groups draw themselves) and not on the Expressive menu
 * functions: those are still alpha and differ between the Compose Multiplatform material3 sharedUi compiles against
 * and the newer androidx one the Android app runs with (DropdownMenuPopup lost a parameter: NoSuchMethodError).
 */
@Composable
internal fun WorkoutTitleMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    canRename: Boolean,
    canOpenRoutine: Boolean,
    isDeload: Boolean,
    onRename: () -> Unit,
    onOpenRoutine: () -> Unit,
    onSetDeload: (Boolean) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(GroupOuter),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        LocalPlatformGlass.current.NotAnOverlay()
        val actions = buildList {
            if (canRename) add(TitleAction.RENAME)
            if (canOpenRoutine) add(TitleAction.OPEN_ROUTINE)
        }
        // One rounded container: the actions as joined rows, then the deload row; the gap between them stays the
        // container's colour, so nothing of the screen shows through
        MenuContainer {
            if (actions.isNotEmpty()) {
                actions.forEachIndexed { i, action ->
                    val rename = action == TitleAction.RENAME
                    MenuRow(
                        icon = if (rename) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.ListAlt,
                        title = stringResource(if (rename) Res.string.rename_workout else Res.string.open_routine),
                        shape = itemShape(i, actions.size),
                        onClick = {
                            onDismiss()
                            if (rename) onRename() else onOpenRoutine()
                        },
                        // Opening the routine leaves the workout screen: the arrow says so
                        trailing = if (rename) null else {
                            { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = OnSurfaceVariant) }
                        }
                    )
                }
                Spacer(Modifier.size(SectionGap))
            }
            // A deload workout stays in the history, out of stats, records and the next workout's values (DeloadRules)
            DeloadMenuRow(checked = isDeload, onCheckedChange = onSetDeload)
        }
    }
}

private enum class TitleAction { RENAME, OPEN_ROUTINE }

private val GroupOuter = 24.dp
private val ItemOuter = 20.dp
private val ItemInner = 4.dp
private val SectionGap = 6.dp
private val MenuWidth = 296.dp

/** The menu's one rounded surface; its rows sit inside it with concentric corners, like the set list. */
@Composable
private fun MenuContainer(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(GroupOuter),
        color = SurfaceContainerHigh,
        shadowElevation = 3.dp,
        modifier = Modifier.padding(horizontal = 8.dp).width(MenuWidth)
    ) {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
    }
}

private fun itemShape(index: Int, count: Int): RoundedCornerShape {
    val top: Dp = if (index == 0) ItemOuter else ItemInner
    val bottom: Dp = if (index == count - 1) ItemOuter else ItemInner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .widthIn(min = MenuWidth - 8.dp)
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(SurfaceContainerHighest)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(PaddingValues(horizontal = 12.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(22.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = OnSurface,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** The deload item: the whole row toggles; when on it turns into a filled pill, the M3 Expressive selected item. */
@Composable
private fun DeloadMenuRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val fill by animateColorAsState(if (checked) SecondaryContainer else SurfaceContainerHighest, WorkoutMotion.effects(), label = "deload_item_fill")
    val corner by animateDpAsState(if (checked) 32.dp else ItemOuter, WorkoutMotion.spatial(), label = "deload_item_corner")
    Row(
        modifier = Modifier
            .widthIn(min = MenuWidth - 8.dp)
            .clip(RoundedCornerShape(corner.coerceAtLeast(0.dp)))
            .background(fill)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.TrendingDown,
            contentDescription = null,
            tint = if (checked) OnSecondaryContainer else OnSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(Res.string.deload_workout),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface
            )
            Text(
                text = stringResource(Res.string.deload_workout_desc),
                style = MaterialTheme.typography.bodySmall,
                color = if (checked) OnSecondaryContainer else OnSurfaceVariant
            )
        }
        SettingsSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** The small round arrow after the workout's name: the name opens a menu; it turns over while the menu is open. */
@Composable
internal fun TitleMenuChevron(open: Boolean, modifier: Modifier = Modifier) {
    val turn by animateFloatAsState(if (open) 180f else 0f, WorkoutMotion.spatial(), label = "title_menu_chevron")
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(SurfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = OnSurface,
            modifier = Modifier.size(18.dp).rotate(turn)
        )
    }
}
