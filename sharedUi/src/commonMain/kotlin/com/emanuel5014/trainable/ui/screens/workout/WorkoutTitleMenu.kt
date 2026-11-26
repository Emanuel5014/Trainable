package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.deload_workout
import com.emanuel5014.trainable.sharedui.resources.deload_workout_desc
import com.emanuel5014.trainable.sharedui.resources.open_routine
import com.emanuel5014.trainable.sharedui.resources.organize_exercises
import com.emanuel5014.trainable.sharedui.resources.rename_workout
import com.emanuel5014.trainable.ui.components.menu.ExpressiveMenu
import com.emanuel5014.trainable.ui.components.menu.ExpressiveMenuGap
import com.emanuel5014.trainable.ui.components.menu.ExpressiveMenuItem
import com.emanuel5014.trainable.ui.components.menu.ExpressiveMenuSwitchItem
import com.emanuel5014.trainable.ui.components.menu.MenuRowPosition
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import org.jetbrains.compose.resources.stringResource

/**
 * The menu under the workout's name ([ExpressiveMenu]): what to do with the workout (rename, open its routine) as
 * joined rows, then the deload switch, which turns into a filled pill when on.
 */
@Composable
internal fun WorkoutTitleMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    canRename: Boolean,
    canOpenRoutine: Boolean,
    isDeload: Boolean,
    /** "Organise exercises", offered in a quick workout that has some. */
    canOrganize: Boolean = false,
    onOrganize: () -> Unit = {},
    onRename: () -> Unit,
    onOpenRoutine: () -> Unit,
    onSetDeload: (Boolean) -> Unit
) {
    ExpressiveMenu(expanded = expanded, onDismissRequest = onDismiss, width = 296.dp) {
        val actions = buildList {
            if (canRename) add(TitleAction.RENAME)
            if (canOrganize) add(TitleAction.ORGANIZE)
            if (canOpenRoutine) add(TitleAction.OPEN_ROUTINE)
        }
        actions.forEachIndexed { i, action ->
            ExpressiveMenuItem(
                text = stringResource(
                    when (action) {
                        TitleAction.RENAME -> Res.string.rename_workout
                        TitleAction.ORGANIZE -> Res.string.organize_exercises
                        TitleAction.OPEN_ROUTINE -> Res.string.open_routine
                    }
                ),
                leadingIcon = when (action) {
                    TitleAction.RENAME -> Icons.Rounded.Edit
                    TitleAction.ORGANIZE -> Icons.Rounded.SwapVert
                    TitleAction.OPEN_ROUTINE -> Icons.AutoMirrored.Rounded.ListAlt
                },
                position = MenuRowPosition(i, actions.size),
                onClick = {
                    onDismiss()
                    when (action) {
                        TitleAction.RENAME -> onRename()
                        TitleAction.ORGANIZE -> onOrganize()
                        TitleAction.OPEN_ROUTINE -> onOpenRoutine()
                    }
                },
                // Opening the routine leaves the workout screen: the arrow says so
                trailing = if (action != TitleAction.OPEN_ROUTINE) null else {
                    { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = OnSurfaceVariant) }
                }
            )
        }
        if (actions.isNotEmpty()) ExpressiveMenuGap()
        // A deload workout stays in the history, out of stats, records and the next workout's values (DeloadRules)
        ExpressiveMenuSwitchItem(
            text = stringResource(Res.string.deload_workout),
            supportingText = stringResource(Res.string.deload_workout_desc),
            leadingIcon = Icons.AutoMirrored.Rounded.TrendingDown,
            checked = isDeload,
            onCheckedChange = onSetDeload,
            position = MenuRowPosition.Single
        )
    }
}

private enum class TitleAction { RENAME, ORGANIZE, OPEN_ROUTINE }

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
