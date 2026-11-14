package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.local.entity.RoutineGroupEntity
import com.emanuel5014.trainable.domain.journal.TagContrast
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.custom_color
import com.emanuel5014.trainable.sharedui.resources.routine_group_archive
import com.emanuel5014.trainable.sharedui.resources.routine_group_archive_hint
import com.emanuel5014.trainable.sharedui.resources.routine_group_archived
import com.emanuel5014.trainable.sharedui.resources.routine_group_change_color
import com.emanuel5014.trainable.sharedui.resources.routine_group_collapse
import com.emanuel5014.trainable.sharedui.resources.routine_group_color
import com.emanuel5014.trainable.sharedui.resources.routine_group_color_hint
import com.emanuel5014.trainable.sharedui.resources.routine_group_create
import com.emanuel5014.trainable.sharedui.resources.routine_group_dissolve
import com.emanuel5014.trainable.sharedui.resources.routine_group_dissolve_hint
import com.emanuel5014.trainable.sharedui.resources.routine_group_expand
import com.emanuel5014.trainable.sharedui.resources.routine_group_move_down
import com.emanuel5014.trainable.sharedui.resources.routine_group_move_to
import com.emanuel5014.trainable.sharedui.resources.routine_group_move_up
import com.emanuel5014.trainable.sharedui.resources.routine_group_name
import com.emanuel5014.trainable.sharedui.resources.routine_group_new
import com.emanuel5014.trainable.sharedui.resources.routine_group_none
import com.emanuel5014.trainable.sharedui.resources.routine_group_options
import com.emanuel5014.trainable.sharedui.resources.routine_group_rename
import com.emanuel5014.trainable.sharedui.resources.routine_group_restore
import com.emanuel5014.trainable.sharedui.resources.routine_group_restore_hint
import com.emanuel5014.trainable.sharedui.resources.routine_group_routines
import com.emanuel5014.trainable.sharedui.resources.save
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.journal.TagColorPickerDialog
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val DarkInk = Color(0xFF1A1A1A)

/** Cover colour and its content colour for a group: its own colour when it has one, else its theme role. */
@Composable
internal fun routineGroupColors(group: RoutineGroupEntity): Pair<Color, Color> =
    routineGroupColors(group.colorRole, group.customColor)

@Composable
internal fun routineGroupColors(colorRole: Int, customColor: Int?): Pair<Color, Color> {
    if (customColor != null) {
        val ink = if (TagContrast.ratio(Color.White.toArgb(), customColor) >= TagContrast.ratio(DarkInk.toArgb(), customColor)) {
            Color.White
        } else DarkInk
        return Color(customColor) to ink
    }
    val scheme = MaterialTheme.colorScheme
    return when (colorRole) {
        // `secondary`, not `secondaryContainer`: in the dark Monolithic theme the container is the row's own grey
        RoutineGroupColor.SECONDARY -> scheme.secondary to scheme.onSecondary
        RoutineGroupColor.TERTIARY -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.primaryContainer to scheme.onPrimaryContainer
    }
}

/** Covers of the routines in no group while other routines are grouped: quiet, so the groups' colours stand out. */
@Composable
internal fun ungroupedCoverColors(): Pair<Color, Color> = SurfaceContainerHighest to OnSurfaceVariant

/** The small square (or the empty outline for "no group") that stands for a group next to its name. */
@Composable
internal fun GroupSwatch(color: Color?, size: Int = 14) {
    val shape = RoundedCornerShape((size * 0.36f).dp)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(shape)
            .then(
                if (color != null) Modifier.background(color)
                else Modifier.border(2.dp, OnSurfaceVariant.copy(alpha = 0.45f), shape)
            )
    )
}

/** Head of an open group in the list: its colour, name and a line under it; tapping folds it. */
@Composable
internal fun RoutineGroupHeader(
    title: String,
    subtitle: String,
    swatch: Color?,
    onToggle: () -> Unit,
    onMenu: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val collapseLabel = stringResource(Res.string.routine_group_collapse)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle)
            .semantics { contentDescription = "$title, $collapseLabel" }
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GroupSwatch(swatch)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (swatch != null) OnSurface else OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onMenu != null) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(Res.string.routine_group_options), tint = OnSurfaceVariant)
            }
        }
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, tint = OnSurfaceVariant)
        }
    }
}

/**
 * A folded group: one card with its covers stacked (up to three, numbered), the name, a line (period or count) and
 * the names of its routines. Tapping opens it; its ⋮ lives on the open group's head, so the text keeps the room.
 */
@Composable
internal fun RoutineGroupFoldedCard(
    title: String,
    subtitle: String,
    names: String,
    count: Int,
    colors: Pair<Color, Color>,
    named: Boolean,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expandLabel = stringResource(Res.string.routine_group_expand)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ROUTINE_GROUP_END_RADIUS.dp))
            .background(SurfaceContainerHigh)
            .clickable(onClick = onExpand)
            .semantics { contentDescription = "$title, $expandLabel" }
            .heightIn(min = 88.dp)
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val shown = count.coerceIn(1, 3)
        Box(modifier = Modifier.width((52 + (shown - 1) * 16).dp).height(68.dp)) {
            for (i in shown - 1 downTo 0) {
                Box(
                    modifier = Modifier
                        .offset(x = (i * 16).dp, y = (8 - i * 4).dp)
                        .size(width = 52.dp, height = 56.dp)
                        .alpha(1f - i * 0.18f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.first)
                        .border(2.dp, SurfaceContainerHigh, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.BottomStart
                ) {
                    // Only the front cover carries a number: the others peek out behind it
                    if (i == 0) Text(
                        text = "${i + 1}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = colors.second,
                        modifier = Modifier.padding(start = 9.dp, bottom = 2.dp)
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (named) OnSurface else OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = names,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = OnSurfaceVariant)
        }
    }
}

/** The three theme colours and a free one; [onPick] gets the role and the free colour (null for a theme one). */
@Composable
internal fun GroupColorPicker(
    colorRole: Int,
    customColor: Int?,
    onPick: (colorRole: Int, customColor: Int?) -> Unit
) {
    var picking by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (role in 0 until RoutineGroupColor.THEME_ROLES) {
            val (fill, ink) = routineGroupColors(role, null)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .clickable { onPick(role, null) },
                contentAlignment = Alignment.Center
            ) {
                if (customColor == null && role == colorRole) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
                }
            }
        }
        Box(modifier = Modifier.width(1.dp).height(28.dp).background(OnSurfaceVariant.copy(alpha = 0.3f)))
        val customLabel = stringResource(Res.string.custom_color)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (customColor != null) Modifier.background(Color(customColor))
                    else Modifier.background(
                        Brush.sweepGradient(
                            listOf(Color(0xFFFF4D4D), Color(0xFFFFC233), Color(0xFF5BD66B), Color(0xFF33B5FF), Color(0xFFB35CFF), Color(0xFFFF4D4D))
                        )
                    )
                )
                .clickable { picking = true }
                .semantics { contentDescription = customLabel },
            contentAlignment = Alignment.Center
        ) {
            if (customColor != null) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = routineGroupColors(0, customColor).second, modifier = Modifier.size(22.dp))
            } else {
                Icon(Icons.Rounded.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
    if (picking) {
        TagColorPickerDialog(
            initialColor = routineGroupColors(colorRole, customColor).first,
            onDismiss = { picking = false },
            onApply = { argb ->
                picking = false
                onPick(colorRole, argb)
            }
        )
    }
}

/** A group the selected routines can move to, with what the sheet says about it. */
internal data class GroupChoice(val group: RoutineGroupEntity, val count: Int, val archived: Boolean)

/**
 * "Group": a new group from the selected routines (name and colour), or moving them into a group that exists, or out
 * of their groups.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutineGroupSheet(
    selectedNames: List<String>,
    choices: List<GroupChoice>,
    canUngroup: Boolean,
    defaultColorRole: Int,
    onCreate: (name: String, colorRole: Int, customColor: Int?) -> Unit,
    onMoveTo: (groupId: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var colorRole by remember { mutableStateOf(defaultColorRole) }
    var customColor by remember { mutableStateOf<Int?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        tonalElevation = 0.dp,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ResponsiveSize.horizontalPadding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.routine_group_new),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineMedium.fontSize)),
                    color = OnSurface,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = selectedNames.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            GymInputField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(Res.string.routine_group_name),
                modifier = Modifier.fillMaxWidth()
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(Res.string.routine_group_color))
                GroupColorPicker(colorRole, customColor) { role, custom ->
                    colorRole = role
                    customColor = custom
                }
                Text(
                    text = stringResource(Res.string.routine_group_color_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            GymButton(
                onClick = { onCreate(name, colorRole, customColor) },
                enabled = name.isNotBlank(),
                containerColor = Primary,
                contentColor = OnPrimary,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(Res.string.routine_group_create).uppercase(), fontWeight = FontWeight.ExtraBold)
            }

            if (choices.isNotEmpty() || canUngroup) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    SectionLabel(stringResource(Res.string.routine_group_move_to))
                    Spacer(modifier = Modifier.height(5.dp))
                    val rows = choices.size + if (canUngroup) 1 else 0
                    choices.forEachIndexed { index, choice ->
                        val countText = pluralStringResource(Res.plurals.routine_group_routines, choice.count, choice.count)
                        ChoiceRow(
                            swatch = routineGroupColors(choice.group).first,
                            title = choice.group.name,
                            trailing = if (choice.archived) stringResource(Res.string.routine_group_archived) else countText,
                            index = index,
                            count = rows,
                            onClick = { onMoveTo(choice.group.id) }
                        )
                    }
                    if (canUngroup) {
                        ChoiceRow(
                            swatch = null,
                            title = stringResource(Res.string.routine_group_none),
                            trailing = "",
                            index = rows - 1,
                            count = rows,
                            onClick = { onMoveTo(null) }
                        )
                    }
                }
            }
        }
    }
}

/** What can be done to a group from its ⋮ button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutineGroupMenuSheet(
    group: RoutineGroupEntity,
    names: String,
    archivedTab: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRename: () -> Unit,
    onChangeColor: () -> Unit,
    onMove: (up: Boolean) -> Unit,
    onArchive: () -> Unit,
    onDissolve: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        tonalElevation = 0.dp,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ResponsiveSize.horizontalPadding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GroupSwatch(routineGroupColors(group).first, size = 40)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = names, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            val edits = buildList {
                add(Triple(Icons.Rounded.Edit, Res.string.routine_group_rename, onRename))
                add(Triple(Icons.Rounded.Palette, Res.string.routine_group_change_color, onChangeColor))
                if (canMoveUp) add(Triple(Icons.Rounded.KeyboardArrowUp, Res.string.routine_group_move_up, { onMove(true) }))
                if (canMoveDown) add(Triple(Icons.Rounded.KeyboardArrowDown, Res.string.routine_group_move_down, { onMove(false) }))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                edits.forEachIndexed { index, (icon, label, action) ->
                    MenuRow(icon, stringResource(label), null, index, edits.size, action)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MenuRow(
                    icon = if (archivedTab) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                    title = stringResource(if (archivedTab) Res.string.routine_group_restore else Res.string.routine_group_archive),
                    hint = stringResource(if (archivedTab) Res.string.routine_group_restore_hint else Res.string.routine_group_archive_hint),
                    index = 0,
                    count = 2,
                    onClick = onArchive
                )
                MenuRow(
                    icon = Icons.Rounded.FolderOff,
                    title = stringResource(Res.string.routine_group_dissolve),
                    hint = stringResource(Res.string.routine_group_dissolve_hint),
                    index = 1,
                    count = 2,
                    onClick = onDissolve
                )
            }
        }
    }
}

@Composable
internal fun RoutineGroupRenameDialog(initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = { Text(stringResource(Res.string.routine_group_rename), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
        text = {
            GymInputField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(Res.string.routine_group_name),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(Res.string.save).uppercase(), color = Primary, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}

@Composable
internal fun RoutineGroupColorDialog(group: RoutineGroupEntity, onDismiss: () -> Unit, onPick: (Int, Int?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = { Text(stringResource(Res.string.routine_group_change_color), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GroupColorPicker(group.colorRole, group.customColor, onPick)
                Text(stringResource(Res.string.routine_group_color_hint), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel).uppercase(), color = Primary)
            }
        }
    )
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(CircleShape)
            .background(OnSurfaceVariant.copy(alpha = 0.4f))
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black,
        color = OnSurfaceVariant
    )
}

/** Corners of a row in a short joined list: round at its ends, tight where rows touch. */
private fun joinedShape(index: Int, count: Int): RoundedCornerShape {
    val top = if (index == 0) 24.dp else 8.dp
    val bottom = if (index == count - 1) 24.dp else 8.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun ChoiceRow(swatch: Color?, title: String, trailing: String, index: Int, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(joinedShape(index, count))
            .background(SurfaceContainerHigh)
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GroupSwatch(swatch)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (trailing.isNotEmpty()) {
            Text(text = trailing, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, hint: String?, index: Int, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(joinedShape(index, count))
            .background(SurfaceContainerHigh)
            .clickable(onClick = onClick)
            .heightIn(min = if (hint != null) 64.dp else 56.dp)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface)
            if (hint != null) {
                Text(text = hint, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
    }
}
