package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.add_exercise
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.organize_exercises
import com.emanuel5014.trainable.sharedui.resources.organize_go_to
import com.emanuel5014.trainable.sharedui.resources.organize_hint
import com.emanuel5014.trainable.sharedui.resources.organize_link_next
import com.emanuel5014.trainable.sharedui.resources.organize_locked
import com.emanuel5014.trainable.sharedui.resources.organize_remove
import com.emanuel5014.trainable.sharedui.resources.organize_started
import com.emanuel5014.trainable.sharedui.resources.organize_to_do
import com.emanuel5014.trainable.sharedui.resources.organize_unlink
import com.emanuel5014.trainable.sharedui.resources.remove_exercise
import com.emanuel5014.trainable.sharedui.resources.remove_exercise_done_sets
import com.emanuel5014.trainable.sharedui.resources.remove_exercise_title
import com.emanuel5014.trainable.sharedui.resources.routine_exercises_count
import com.emanuel5014.trainable.sharedui.resources.routine_sets_count
import com.emanuel5014.trainable.sharedui.resources.superset
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.formatWeight
import com.emanuel5014.trainable.ui.components.menu.ExpressiveMenu
import com.emanuel5014.trainable.ui.components.menu.ExpressiveMenuGap
import com.emanuel5014.trainable.ui.components.menu.ExpressiveMenuItem
import com.emanuel5014.trainable.ui.components.menu.MenuRowPosition
import com.emanuel5014.trainable.ui.components.motion.rememberAnimatedShape
import com.emanuel5014.trainable.ui.components.rememberLazyListReorderState
import com.emanuel5014.trainable.ui.components.reorderGestures
import com.emanuel5014.trainable.ui.components.reorderableItem
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSecondaryContainer
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OnTertiary
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.SecondaryContainer
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.ui.util.uppercaseUi
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** One exercise of the list: its place in the workout and whether it was started (then it cannot be moved). */
private class OrganizeRow(val index: Int, val exercise: WorkoutExerciseState) {
    val key: String = "ex-${exercise.exercise.id}-${exercise.executionOrder}"
    val started: Boolean get() = exercise.hasExecutionRank
}

/** Size of the number badge of a card; the superset rail is centred on it, as in the routine screen. */
private val BadgeSize = 40.dp
private val CardGap = 12.dp

/**
 * "Organise exercises" of a quick workout, drawn like the exercise list of a routine. The started exercises sit on top
 * in the order they were done, as one segmented block with a lock: they keep their place. The others are the routine's
 * cards (number badge, ⋮⋮ handle, a superset tinted, outlined and joined by a rail) to drag into a new order, a superset
 * moving as a block. A tap on an exercise opens its menu: go to it, make or leave a superset, take it out of the workout
 * (asking first when sets of it are done). Every change is saved at once; while a timer or a chain follows an exercise by
 * its place, only "go to" is offered and the sheet says why.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeExercisesSheet(
    state: WorkoutState,
    languageCode: String,
    onReorder: (List<Int>) -> Unit,
    onRemove: (Int) -> Unit,
    onLinkWithNext: (index: Int, linked: Boolean) -> Unit,
    onOpen: (Int) -> Unit,
    onAddExercise: () -> Unit,
    onDismiss: () -> Unit
) {
    val editable = state.canOrganizeExercises
    val all = state.exercises.mapIndexed { i, ex -> OrganizeRow(i, ex) }
    val started = all.filter { it.started }.sortedBy { it.exercise.executionOrder }
    val pending = all.filterNot { it.started }
    val shown = started + pending
    val listState = rememberLazyListState()
    var confirmRemove by remember { mutableStateOf<OrganizeRow?>(null) }

    val byKey = pending.associateBy { it.key }
    val reorder = rememberLazyListReorderState(
        lazyListState = listState,
        keys = { if (editable) pending.map { it.key } else emptyList() },
        groupOf = { key -> byKey[key]?.exercise?.supersetId },
        hapticEnabled = state.hapticEnabled,
        onCommit = { keys ->
            val moved = keys.mapNotNull { byKey[it as String] }
            onReorder(started.map { it.index } + moved.map { it.index })
        }
    )
    val pendingShown = reorder.ordered(pending) { it.key }

    // An exercise started out of turn is listed with the started ones: the workout takes the order shown, as it would
    // when reopened, so that "superset with the next one" joins the two exercises the list shows together
    LaunchedEffect(shown.map { it.index }, editable) {
        if (editable && shown.map { it.index } != state.exercises.indices.toList()) onReorder(shown.map { it.index })
    }

    fun askRemove(row: OrganizeRow) {
        if (row.exercise.setsDone() > 0) confirmRemove = row else onRemove(row.index)
    }

    /** The menu of an exercise: go to it; with changes allowed, its superset and taking it out. */
    @Composable
    fun RowMenu(row: OrganizeRow, open: Boolean, onClose: () -> Unit) {
        val exercises = state.exercises
        val sid = row.exercise.supersetId
        val linkedPrev = sid != null && exercises.getOrNull(row.index - 1)?.supersetId == sid
        val linkedNext = sid != null && exercises.getOrNull(row.index + 1)?.supersetId == sid
        val hasNext = row.index < exercises.lastIndex
        val linkAction = editable && (linkedPrev || linkedNext || hasNext)
        val firstGroup = if (linkAction) 2 else 1
        ExpressiveMenu(expanded = open, onDismissRequest = onClose, width = 296.dp) {
            ExpressiveMenuItem(
                text = stringResource(Res.string.organize_go_to),
                leadingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                position = MenuRowPosition(0, firstGroup),
                onClick = {
                    onClose()
                    onOpen(row.index)
                }
            )
            if (linkAction) {
                val leave = linkedPrev || linkedNext
                ExpressiveMenuItem(
                    text = stringResource(if (leave) Res.string.organize_unlink else Res.string.organize_link_next),
                    leadingIcon = if (leave) Icons.Rounded.LinkOff else Icons.Rounded.Link,
                    position = MenuRowPosition(1, firstGroup),
                    onClick = {
                        onClose()
                        when {
                            // Out of the group on both sides (the middle of three or more)
                            linkedPrev && linkedNext -> {
                                onLinkWithNext(row.index - 1, false)
                                onLinkWithNext(row.index, false)
                            }
                            linkedNext -> onLinkWithNext(row.index, false)
                            linkedPrev -> onLinkWithNext(row.index - 1, false)
                            else -> onLinkWithNext(row.index, true)
                        }
                    }
                )
            }
            if (editable) {
                ExpressiveMenuGap()
                ExpressiveMenuItem(
                    text = stringResource(Res.string.organize_remove),
                    leadingIcon = Icons.Rounded.DeleteOutline,
                    position = MenuRowPosition.Single,
                    destructive = true,
                    onClick = {
                        onClose()
                        askRemove(row)
                    }
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(OnSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Text(
                text = stringResource(Res.string.organize_exercises),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = OnSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            val totalSets = state.exercises.sumOf { it.setsTotal() }
            Text(
                text = pluralStringResource(Res.plurals.routine_exercises_count, state.exercises.size, state.exercises.size) +
                    " · " + pluralStringResource(Res.plurals.routine_sets_count, totalSets, totalSets),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = OnSecondaryContainer,
                modifier = Modifier.clip(CircleShape).background(SecondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(if (editable) Res.string.organize_hint else Res.string.organize_locked),
                style = MaterialTheme.typography.bodyMedium,
                color = if (editable) OnSurfaceVariant else Error
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).reorderGestures(reorder)
            ) {
                if (started.isNotEmpty()) {
                    item(key = "started-header") { SectionLabel(stringResource(Res.string.organize_started)) }
                    items(started, key = { it.key }) { row ->
                        val position = started.indexOf(row)
                        var menuOpen by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.padding(bottom = 2.dp)) {
                            StartedRow(
                                row = row,
                                number = position + 1,
                                position = position,
                                count = started.size,
                                isCurrent = row.index == state.currentExerciseIndex,
                                weightUnit = state.weightUnit,
                                languageCode = languageCode,
                                onClick = { menuOpen = true }
                            )
                            RowMenu(row, menuOpen) { menuOpen = false }
                        }
                    }
                    if (pending.isNotEmpty()) item(key = "pending-header") { SectionLabel(stringResource(Res.string.organize_to_do)) }
                } else {
                    item(key = "top-space") { Spacer(modifier = Modifier.height(8.dp)) }
                }
                items(pendingShown, key = { it.key }) { row ->
                    val i = pendingShown.indexOf(row)
                    val sid = row.exercise.supersetId
                    val isStart = sid != null && pendingShown.getOrNull(i - 1)?.exercise?.supersetId != sid
                    val isEnd = sid != null && pendingShown.getOrNull(i + 1)?.exercise?.supersetId != sid
                    val dragged = reorder.isDragged(row.key)
                    var menuOpen by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.then(reorderableItem(reorder, row.key, shape = RoundedCornerShape(40.dp)))) {
                        if (isStart) SupersetLabel()
                        Box(
                            modifier = Modifier.then(
                                if (sid != null && !reorder.isDragging) Modifier.supersetRail(isStart, isEnd) else Modifier
                            )
                        ) {
                            PendingCard(
                                row = row,
                                number = started.size + i + 1,
                                isSuperset = sid != null,
                                isDragging = dragged,
                                editable = editable,
                                weightUnit = state.weightUnit,
                                languageCode = languageCode,
                                onClick = { menuOpen = true }
                            )
                            RowMenu(row, menuOpen) { menuOpen = false }
                        }
                        Spacer(modifier = Modifier.height(CardGap))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            GymButton(
                onClick = onAddExercise,
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceContainerHigh,
                contentColor = Primary
            ) {
                Icon(Icons.Rounded.FitnessCenter, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.add_exercise).uppercaseUi(), fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    confirmRemove?.let { row ->
        val name = ExerciseTranslations.translate(row.exercise.exercise.nome, languageCode)
        val done = row.exercise.setsDone()
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(Res.string.remove_exercise_title, name)) },
            text = { Text(pluralStringResource(Res.plurals.remove_exercise_done_sets, done, done)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        confirmRemove = null
                        onRemove(row.index)
                    },
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.remove_exercise).uppercaseUi(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { confirmRemove = null },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercaseUi(), fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}

/** "ALREADY STARTED" / "TO DO" above the two parts of the list. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercaseUi(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
        fontWeight = FontWeight.Black,
        color = OnSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
    )
}

/** The "🔗 SUPERSET" line above the first card of a superset, as in the routine screen. */
@Composable
private fun SupersetLabel() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)) {
        Icon(Icons.Rounded.Link, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(Res.string.superset),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = Primary,
            fontWeight = FontWeight.Black
        )
    }
}

/** The rail joining the badges of a superset, behind the cards: up to the card before, down to the card after. */
@Composable
private fun Modifier.supersetRail(isStart: Boolean, isEnd: Boolean): Modifier {
    val rail = Primary
    val badgeCenter = ResponsiveSize.cardPadding + BadgeSize / 2
    return drawBehind {
    val x = badgeCenter.toPx()
    val centerY = size.height / 2f
    val half = CardGap.toPx() / 2f
    val width = 4.dp.toPx()
    if (!isStart) drawLine(rail, Offset(x, centerY), Offset(x, -CardGap.toPx()), strokeWidth = width)
    if (!isEnd) drawLine(rail, Offset(x, centerY), Offset(x, size.height + half), strokeWidth = width)
    }
}

/** What an exercise is about to do: its next set ("62.5 kg × 7", "45 s"), or the minutes of a cardio one. */
@Composable
private fun nextLoadText(exercise: WorkoutExerciseState, weightUnit: String): String {
    if (exercise.isCardio) {
        val minutes = (exercise.cardioDurataTargetSeconds ?: 0) / 60
        return if (minutes > 0) "$minutes min" else ""
    }
    val set = exercise.sets.firstOrNull { !it.isCompleted } ?: exercise.sets.lastOrNull() ?: return ""
    val seconds = set.timeSeconds
    return when {
        set.isTimeAndReps && seconds != null -> "$seconds s"
        seconds != null && set.weight > 0f -> "${formatWeight(set.weight, weightUnit)} × $seconds s"
        seconds != null -> "$seconds s"
        else -> "${formatWeight(set.weight, weightUnit)} × ${set.reps}"
    }
}

/**
 * A started exercise: one row of a segmented block (round at its ends, tight between rows), its badge a teal circle
 * with a tick once every set is done, else the cookie; a ring marks the exercise on screen, a lock says it stays put.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StartedRow(
    row: OrganizeRow,
    number: Int,
    position: Int,
    count: Int,
    isCurrent: Boolean,
    weightUnit: String,
    languageCode: String,
    onClick: () -> Unit
) {
    val exercise = row.exercise
    val done = exercise.setsDone()
    val total = exercise.setsTotal()
    val finished = total > 0 && done >= total
    val top = if (position == 0) 20.dp else 4.dp
    val bottom = if (position == count - 1) 20.dp else 4.dp
    val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    val ring = Primary.copy(alpha = 0.3f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (isCurrent) SurfaceContainerHigh else SurfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .then(
                if (isCurrent) Modifier.drawBehind {
                    drawRoundRect(
                        color = ring,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(top.toPx())
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (finished) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Tertiary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = OnTertiary, modifier = Modifier.size(18.dp))
            }
        } else {
            Box(
                modifier = Modifier.size(32.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text("$number", style = MaterialTheme.typography.labelLarge, color = OnPrimary, fontWeight = FontWeight.ExtraBold)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = ExerciseTranslations.translate(exercise.exercise.nome, languageCode),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val load = nextLoadText(exercise, weightUnit)
            Text(
                text = if (load.isEmpty()) "$done/$total" else "$done/$total · $load",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = OnSurfaceVariant.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
    }
}

/**
 * An exercise still to do, as the routine screen draws it: the cookie badge with its number, the name, the sets and
 * the next load, the ⋮⋮ handle. A superset is tinted and outlined in the brand colour. While dragged the card squares
 * off and the badge turns into a rounded square, like the routine's.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PendingCard(
    row: OrganizeRow,
    number: Int,
    isSuperset: Boolean,
    isDragging: Boolean,
    editable: Boolean,
    weightUnit: String,
    languageCode: String,
    onClick: () -> Unit
) {
    val exercise = row.exercise
    val lift by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "organize_card_lift"
    )
    val lifted = lift.coerceIn(0f, 1f)
    val cardShape = RoundedCornerShape(lerp(40.dp, 16.dp, lift).coerceAtLeast(0.dp))
    val badgeMorph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Square) }
    val badgeShape = rememberAnimatedShape(badgeMorph, lift)
    val restingColor = if (isSuperset) Primary.copy(alpha = 0.10f) else SurfaceContainer
    val borderAlpha = androidx.compose.ui.util.lerp(if (isSuperset) 0.45f else 0f, 0.8f, lifted)
    val badgeColor = lerp(if (isSuperset) Primary else SecondaryContainer, Primary, lifted)
    val badgeText = lerp(if (isSuperset) OnPrimary else OnSecondaryContainer, OnPrimary, lifted)
    GymCard(
        modifier = Modifier.fillMaxWidth().clip(cardShape).clickable(role = Role.Button, onClick = onClick),
        containerColor = lerp(restingColor, lerp(SurfaceContainer, Primary, 0.2f), lifted),
        border = if (borderAlpha > 0.01f) BorderStroke(1.5.dp, Primary.copy(alpha = borderAlpha)) else null,
        shape = cardShape
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(BadgeSize).clip(badgeShape).background(badgeColor), contentAlignment = Alignment.Center) {
                Text("$number", style = MaterialTheme.typography.labelLarge, color = badgeText, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ExerciseTranslations.translate(exercise.exercise.nome, languageCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val total = exercise.setsTotal()
                    Text(
                        text = pluralStringResource(Res.plurals.routine_sets_count, total, total),
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.background(SurfaceContainerHigh, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                    val load = nextLoadText(exercise, weightUnit)
                    if (load.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = load, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.DragIndicator,
                contentDescription = null,
                tint = OnSurfaceVariant.copy(alpha = if (editable) 0.7f else 0.3f)
            )
        }
    }
}
