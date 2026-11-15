package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.superset
import com.emanuel5014.trainable.sharedui.resources.workout_overview_summary
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.data.ExerciseTranslations
import org.jetbrains.compose.resources.stringResource

/** How long the finger rests on the exercise buttons before the list of the workout opens. */
private const val HOLD_MILLIS = 500L

/**
 * The list of every exercise of the workout, opened by holding a finger on the previous/next buttons of the hub.
 * It is only to look at: it stays while the finger stays and closes when the finger lifts.
 */
@Stable
class ExerciseOverviewState {
    /** Where the pressed buttons are (centre, root coordinates) while the list is open, null when it is closed. */
    var anchor by mutableStateOf<Offset?>(null)
        private set
    /** The last [anchor], kept so the list can shrink back into the buttons after it closes. */
    var lastAnchor by mutableStateOf(Offset.Zero)
        private set
    var hapticEnabled = true

    fun open(at: Offset) {
        lastAnchor = at
        anchor = at
    }

    fun close() {
        anchor = null
    }
}

val LocalExerciseOverview = staticCompositionLocalOf<ExerciseOverviewState?> { null }

/**
 * Holding a finger still on these buttons for [HOLD_MILLIS] opens the exercise list; lifting it closes the list.
 * A tap stays a tap and a drag (the hub, a swipe) stays a drag: the hold only counts while the finger has not moved.
 * Once the list is open the touch belongs to it, so lifting the finger never also presses the button under it.
 */
@Composable
fun Modifier.holdForExerciseOverview(): Modifier {
    val overview = LocalExerciseOverview.current ?: return this
    val haptic = LocalHapticFeedback.current
    val bounds = remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { bounds.value = it.boundsInRoot().center }
        .pointerInput(overview) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val slop = viewConfiguration.touchSlop
                // Non-null: the touch ended or became something else before the hold time
                val endedEarly = withTimeoutOrNull(HOLD_MILLIS) {
                    var ended = false
                    while (!ended) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        ended = change == null || !change.pressed || event.changes.size > 1 ||
                            (change.position - down.position).getDistance() > slop
                    }
                    true
                }
                if (endedEarly != null) return@awaitEachGesture

                if (overview.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                overview.open(bounds.value)
                try {
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                } finally {
                    overview.close()
                }
            }
        }
}

/** One line of the list: an exercise, or the exercises of a superset done together. */
private class OverviewGroup(val supersetId: String?, val items: List<IndexedValue<WorkoutExerciseState>>)

private fun groupsOf(exercises: List<WorkoutExerciseState>): List<OverviewGroup> {
    val groups = mutableListOf<OverviewGroup>()
    var current = mutableListOf<IndexedValue<WorkoutExerciseState>>()
    exercises.withIndex().forEach { item ->
        val id = item.value.supersetId
        val sameSuperset = id != null && current.isNotEmpty() && current.last().value.supersetId == id
        if (!sameSuperset && current.isNotEmpty()) {
            groups += OverviewGroup(current.first().value.supersetId.takeIf { current.size > 1 }, current)
            current = mutableListOf()
        }
        current += item
    }
    if (current.isNotEmpty()) groups += OverviewGroup(current.first().value.supersetId.takeIf { current.size > 1 }, current)
    return groups
}

private fun WorkoutExerciseState.setsDone(): Int =
    if (isCardio) cardioSets.count { it.isCompleted } else sets.count { it.isCompleted }

private fun WorkoutExerciseState.setsTotal(): Int = if (isCardio) cardioSets.size else sets.size

/**
 * The exercise list over the whole screen. It grows out of the buttons that were held into a card in the middle of
 * the screen, its lines coming in one after the other, and shrinks back into them when the finger lifts.
 */
@Composable
fun ExerciseOverview(state: WorkoutState, overview: ExerciseOverviewState, languageCode: String) {
    val open = overview.anchor != null
    val progress by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = if (open) spring(dampingRatio = 0.72f, stiffness = 420f) else spring(dampingRatio = 1f, stiffness = 900f),
        label = "overview"
    )
    val linesProgress by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(durationMillis = if (open) 480 else 120, easing = FastOutSlowInEasing),
        label = "overview_lines"
    )
    if (!open && progress <= 0.001f) return

    val groups = remember(state.exercises) { groupsOf(state.exercises) }
    val setsDone = remember(state.exercises) { state.exercises.sumOf { it.setsDone() } }
    val setsTotal = remember(state.exercises) { state.exercises.sumOf { it.setsTotal() } }
    val currentGroup = groups.indexOfFirst { group -> group.items.any { it.index == state.currentExerciseIndex } }.coerceAtLeast(0)
    // The list cannot be scrolled with the finger held elsewhere: a long workout opens around the current exercise
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentGroup - 2).coerceAtLeast(0))
    var center by remember { mutableStateOf(Offset.Zero) }
    val anchor = overview.lastAnchor
    val p = progress
    val stagger = 0.1f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { center = it.boundsInRoot().center }
            .background(Color.Black.copy(alpha = 0.6f * p.coerceIn(0f, 1f))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.72f)
                .graphicsLayer {
                    // From the held buttons to the middle of the screen
                    translationX = (anchor.x - center.x) * (1f - p)
                    translationY = (anchor.y - center.y) * (1f - p)
                    val scale = 0.25f + 0.75f * p
                    scaleX = scale
                    scaleY = scale
                    alpha = (p * 1.6f).coerceIn(0f, 1f)
                    shape = RoundedCornerShape(28.dp)
                    clip = true
                }
                .background(SurfaceContainerHigh)
                .padding(20.dp)
        ) {
            Text(
                text = state.planName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(Res.string.workout_overview_summary, state.completedExercises, state.totalExercises, setsDone, setsTotal),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            LazyColumn(
                state = listState,
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val lastStart = (groups.size - 1).coerceAtLeast(1) * stagger
                itemsIndexed(groups) { index, group ->
                    val local = ((linesProgress * (1f + lastStart)) - index * stagger).coerceIn(0f, 1f)
                    val density = LocalDensity.current
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = local
                            translationY = with(density) { 20.dp.toPx() } * (1f - local)
                        }
                    ) {
                        OverviewGroupView(group, state.currentExerciseIndex, languageCode)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewGroupView(group: OverviewGroup, currentIndex: Int, languageCode: String) {
    if (group.supersetId == null) {
        group.items.forEach { OverviewLine(it.value, it.index == currentIndex, languageCode) }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, OutlineVariant, RoundedCornerShape(18.dp))
            .padding(4.dp)
    ) {
        Text(
            text = stringResource(Res.string.superset),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Primary,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 2.dp)
        )
        group.items.forEach { OverviewLine(it.value, it.index == currentIndex, languageCode) }
    }
}

@Composable
private fun OverviewLine(exercise: WorkoutExerciseState, isCurrent: Boolean, languageCode: String) {
    val done = exercise.setsDone()
    val total = exercise.setsTotal()
    val isDone = total > 0 && done >= total
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(if (isCurrent) Primary.copy(alpha = 0.14f) else Color.Transparent, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusMark(isDone = isDone, isCurrent = isCurrent)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = ExerciseTranslations.translate(exercise.exercise.nome, languageCode),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
            color = if (isDone && !isCurrent) OnSurfaceVariant else OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$done/$total",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (isCurrent || isDone) Primary else OnSurfaceVariant
        )
    }
}

/** Done: a filled circle with a tick. Being done now: a ring with a dot. Still to do: a faint ring. */
@Composable
private fun StatusMark(isDone: Boolean, isCurrent: Boolean) {
    when {
        isDone -> Box(
            modifier = Modifier.size(24.dp).background(Primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(16.dp))
        }
        isCurrent -> Box(
            modifier = Modifier.size(24.dp).border(2.dp, Primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(10.dp).background(Primary, CircleShape))
        }
        else -> Box(modifier = Modifier.size(24.dp).border(2.dp, OnSurfaceVariant.copy(alpha = 0.45f), CircleShape))
    }
}
