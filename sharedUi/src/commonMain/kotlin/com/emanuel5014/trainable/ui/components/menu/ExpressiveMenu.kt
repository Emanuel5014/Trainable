package com.emanuel5014.trainable.ui.components.menu

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import com.emanuel5014.trainable.ui.glass.LocalPlatformGlass
import com.emanuel5014.trainable.ui.screens.settings.SettingsSwitch
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSecondaryContainer
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.SecondaryContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest

/*
 * The app's menus in the M3 Expressive look: one rounded block (24 dp) holding joined rows (20 dp at the ends of a
 * run, 4 dp between), a small gap in the block's colour between runs, and the chosen or switched-on row as a filled
 * pill on the secondary container.
 *
 * Built on the classic DropdownMenu with a transparent container, not on the Expressive menu functions: those are
 * still alpha and differ between the Compose Multiplatform material3 sharedUi compiles against and the newer androidx
 * one the Android app runs with (DropdownMenuPopup lost a parameter: NoSuchMethodError on Android).
 */

private val BlockCorner = 24.dp
private val RowOuter = 20.dp
private val RowInner = 4.dp
private val PillCorner = 32.dp
private val RunGap = 6.dp

/** Where a row sits in its run of joined rows: it decides which corners are large. */
data class MenuRowPosition(val index: Int, val count: Int) {
    internal val shape: RoundedCornerShape
        get() {
            val top = if (index == 0) RowOuter else RowInner
            val bottom = if (index == count - 1) RowOuter else RowInner
            return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
        }

    companion object {
        val Single = MenuRowPosition(0, 1)
    }
}

/**
 * The menu: anchored like a [DropdownMenu] to the Box it sits in. [width] fixes the block's width (rows with long
 * text or a switch); without it the block takes the width of its widest row, at least 200 dp.
 */
@Composable
fun ExpressiveMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = RoundedCornerShape(BlockCorner),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        LocalPlatformGlass.current.NotAnOverlay()
        Surface(
            shape = RoundedCornerShape(BlockCorner),
            color = SurfaceContainerHigh,
            shadowElevation = 3.dp,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .then(if (width != null) Modifier.width(width) else Modifier.widthIn(min = 200.dp))
        ) {
            Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
        }
    }
}

/** The small gap between two runs of rows, in the block's colour (nothing of the screen shows through). */
@Composable
fun ExpressiveMenuGap() {
    Spacer(Modifier.size(RunGap - 2.dp))
}

/**
 * One row: a leading icon (or [leading] content, a numbered tile for example), the text and an optional trailing
 * part. [selected] makes it the filled pill with a check (a choice among several); [destructive] paints it in the
 * error colour.
 */
@Composable
fun ExpressiveMenuItem(
    text: String,
    onClick: () -> Unit,
    position: MenuRowPosition,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    selected: Boolean? = null,
    destructive: Boolean = false
) {
    val on = selected == true
    val fill by animateColorAsState(if (on) SecondaryContainer else SurfaceContainerHighest, WorkoutMotion.effects(), label = "menu_item_fill")
    val corner by animateDpAsState(if (on) PillCorner else RowOuter, WorkoutMotion.spatial(), label = "menu_item_corner")
    val ink = when {
        destructive -> Error
        else -> OnSurface
    }
    val iconInk = when {
        destructive -> Error
        on -> OnSecondaryContainer
        else -> OnSurfaceVariant
    }
    val shape = if (on) RoundedCornerShape(corner.coerceAtLeast(0.dp)) else position.shape
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(fill)
            .then(
                if (selected != null) Modifier.selectable(selected = on, role = Role.RadioButton, onClick = onClick)
                else Modifier.clickable(role = Role.Button, onClick = onClick)
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            leading != null -> leading()
            leadingIcon != null -> Icon(leadingIcon, contentDescription = null, tint = iconInk, modifier = Modifier.size(22.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        when {
            trailing != null -> trailing()
            on -> Icon(Icons.Rounded.Check, contentDescription = null, tint = OnSecondaryContainer, modifier = Modifier.size(20.dp))
        }
    }
}

/** A row with a switch: the whole row toggles, and when on it becomes the filled pill. */
@Composable
fun ExpressiveMenuSwitchItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    position: MenuRowPosition,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null
) {
    val fill by animateColorAsState(if (checked) SecondaryContainer else SurfaceContainerHighest, WorkoutMotion.effects(), label = "menu_switch_fill")
    val corner by animateDpAsState(if (checked) PillCorner else RowOuter, WorkoutMotion.spatial(), label = "menu_switch_corner")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(if (checked) RoundedCornerShape(corner.coerceAtLeast(0.dp)) else position.shape)
            .background(fill)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = if (checked) OnSecondaryContainer else OnSurfaceVariant, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = OnSurface)
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (checked) OnSecondaryContainer else OnSurfaceVariant
                )
            }
        }
        SettingsSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
