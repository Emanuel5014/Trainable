package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.ui.glass.LocalPanelGlass
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastAny
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Velocity
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.lerp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.hub_panel_collapse
import com.emanuel5014.trainable.sharedui.resources.hub_panel_expand
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.SurfaceContainerLow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val GRABBER_HEIGHT = 32.dp
private val PANEL_SHAPE = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/**
 * As glass the hub floats like an iOS 26 sheet: inset from the screen sides and from the bottom, rounded on
 * every corner. Flush against the edges its lit rim would run off the screen, and where it ends depends on
 * the device's own corner radius; inset by this much it looks the same on every phone. The screen's own
 * padding already keeps the hub above the system bars.
 */
private val GLASS_PANEL_INSET = 8.dp

/**
 * Where the bottom hub of the workout screen is: [expansion] is 1 when it is open and 0 when it is a thin strip,
 * and in between while the lifter drags it or it settles. [detent] is the step the lifter chose; while the hub
 * is [locked] (the lifter has to type something) it stays open without forgetting that choice.
 */
@Stable
class HubSheetState internal constructor(
    private val scope: CoroutineScope,
    initialDetent: Float,
    private val fallbackOpenPx: Int,
    private val fallbackStripPx: Int,
    private val minTravelPx: Float,
    private val flingVelocityPx: Float
) {
    var expansion by mutableFloatStateOf(initialDetent)
        private set
    var detent by mutableFloatStateOf(initialDetent)
        private set
    var locked by mutableStateOf(false)
        private set

    private var openPx = 0
    private var stripPx = 0
    private var job: Job? = null

    /** How far the finger has to move to go from one step to the other, from the heights measured so far. */
    private val travelPx: Float
        get() = HubMotion.travelPx(
            openPx = openPx.takeIf { it > 0 } ?: fallbackOpenPx,
            stripPx = stripPx.takeIf { it > 0 } ?: fallbackStripPx,
            minPx = minTravelPx
        )

    internal fun dragStarted() {
        job?.cancel()
    }

    internal fun dragBy(deltaPx: Float) {
        job?.cancel()
        expansion = HubMotion.dragExpansion(expansion, deltaPx, travelPx, locked)
    }

    /** Whether the finger pulled a locked hub down from open, so the rest of the gesture belongs to the hub. */
    internal val pulledWhileLocked: Boolean
        get() = locked && expansion < HubMotion.OPEN

    /**
     * The finger lifted at [velocityPxPerSec] (positive is down). A locked hub pulled down far enough, or flicked
     * down, calls [onDismiss], which is expected to unlock it; it goes back open meanwhile.
     */
    internal fun settle(velocityPxPerSec: Float, onDismiss: (() -> Unit)? = null) {
        if (locked && onDismiss != null && HubMotion.lockedReleaseDismisses(expansion, velocityPxPerSec, flingVelocityPx)) {
            onDismiss()
        }
        val target = HubMotion.releaseTarget(expansion, velocityPxPerSec, flingVelocityPx, locked)
        if (!locked) detent = target
        animateTo(target, initialVelocity = -velocityPxPerSec / travelPx)
    }

    /** The pill was tapped. */
    fun toggle() {
        if (locked) return
        detent = HubMotion.tapTarget(detent, locked)
        animateTo(detent)
    }

    internal fun setLocked(value: Boolean) {
        if (locked == value) return
        locked = value
        animateTo(HubMotion.restingTarget(detent, value))
    }

    /** Learns the two heights of the hub, so a drag can follow the finger closely. */
    internal fun onMeasured(heightPx: Int) {
        when {
            expansion >= 1f - MEASURE_TOLERANCE -> openPx = heightPx
            expansion <= MEASURE_TOLERANCE -> stripPx = heightPx
        }
    }

    private fun animateTo(target: Float, initialVelocity: Float = 0f) {
        job?.cancel()
        job = scope.launch {
            animate(
                initialValue = expansion,
                targetValue = target,
                initialVelocity = initialVelocity,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            ) { value, _ -> expansion = value.coerceIn(HubMotion.STRIP, HubMotion.OPEN) }
        }
    }

    private companion object {
        const val MEASURE_TOLERANCE = 0.02f
    }
}

@Composable
fun rememberHubSheetState(): HubSheetState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // The step the lifter chose also survives a rotation of the phone
    return rememberSaveable(
        saver = Saver<HubSheetState, Float>(
            save = { it.detent },
            restore = { createHubSheetState(scope, it, density) }
        )
    ) { createHubSheetState(scope, HubMotion.OPEN, density) }
}

private fun createHubSheetState(scope: CoroutineScope, detent: Float, density: Density) = HubSheetState(
    scope = scope,
    initialDetent = detent,
    fallbackOpenPx = with(density) { 230.dp.roundToPx() },
    fallbackStripPx = with(density) { 100.dp.roundToPx() },
    minTravelPx = with(density) { 60.dp.toPx() },
    flingVelocityPx = with(density) { 600.dp.toPx() }
)

/**
 * The bottom hub: a sheet with a pill on top that the lifter drags between open and a thin strip. [content] gets
 * the live expansion (read it inside a layout or a small scope, it changes on every frame of a drag) and uses it
 * to reshape itself. [locked] opens the hub and holds it there, e.g. while the lifter types a weight.
 * [onDismissInput] closes what a locked hub asks for: the lifter swipes down on the hub, on its pill or on its content.
 */
@Composable
fun HubPanel(
    sheet: HubSheetState,
    visible: Boolean,
    locked: Boolean,
    hapticEnabled: Boolean,
    horizontalPadding: Dp,
    onHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onDismissInput: (() -> Unit)? = null,
    content: @Composable ColumnScope.(expansion: () -> Float) -> Unit
) {
    LaunchedEffect(locked) { sheet.setLocked(locked) }
    val expansion: () -> Float = remember(sheet) { { sheet.expansion } }
    val focusManager = LocalFocusManager.current
    val currentDismiss by rememberUpdatedState(onDismissInput)
    val closeEditor: () -> Unit = remember {
        {
            // The keyboard of a field in the editor goes with it
            focusManager.clearFocus()
            currentDismiss?.invoke()
        }
    }
    val dismiss = if (onDismissInput == null) null else closeEditor
    val wheelTouch = remember { HubWheelTouch() }
    val pullToDismiss = remember(sheet) { HubPullToDismiss(sheet, wheelTouch) }
    SideEffect { pullToDismiss.onDismiss = dismiss }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        modifier = modifier
    ) {
        val glass = LocalPanelGlass.current
        val shape = glass?.shape ?: PANEL_SHAPE
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hub_panel")
                .onSizeChanged {
                    sheet.onMeasured(it.height)
                    onHeightChanged(it.height)
                }
                .then(
                    if (glass == null) Modifier
                    else Modifier.padding(horizontal = GLASS_PANEL_INSET).padding(bottom = GLASS_PANEL_INSET)
                )
                .then(if (glass == null) Modifier else glass.frost(Modifier, shape, SurfaceContainerLow)),
            color = if (glass == null) SurfaceContainerLow else Color.Transparent,
            shape = shape,
            tonalElevation = if (glass == null) 8.dp else 0.dp,
            shadowElevation = if (glass == null) 16.dp else 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().then(if (glass == null) Modifier.navigationBarsPadding() else Modifier)) {
                HubGrabber(sheet = sheet, hapticEnabled = hapticEnabled, onDismiss = dismiss)
                // The open hub takes the room it needs; only when that is more than the screen has does its content scroll under the pill
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        // A swipe down on the content of a locked hub pulls the hub, once the content is at its top
                        .nestedScroll(pullToDismiss)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = if (glass == null) horizontalPadding else (horizontalPadding - GLASS_PANEL_INSET).coerceAtLeast(8.dp))
                ) {
                    CompositionLocalProvider(LocalHubWheelTouch provides wheelTouch) { content(expansion) }
                    Spacer(modifier = Modifier.hubSpace(expansion, closed = 8.dp, open = 16.dp))
                }
            }
        }
    }
}

/**
 * Whether a finger is on one of the number wheels inside the hub. A wheel is turned with the same vertical drag
 * that pulls the hub, so while one is touched the hub leaves the whole gesture to it, however hard the lifter
 * flicks; only the pill still moves the hub.
 */
internal class HubWheelTouch {
    private var fingers = 0
    val active: Boolean get() = fingers > 0

    fun down() { fingers++ }
    fun up() { fingers = (fingers - 1).coerceAtLeast(0) }
}

internal val LocalHubWheelTouch = staticCompositionLocalOf<HubWheelTouch?> { null }

/**
 * Marks a control of the hub that turns under a vertical drag, like a number wheel: from the moment a finger lands
 * on it until the last one lifts, a drag on the content never pulls the hub. Outside a hub it does nothing.
 */
@Composable
internal fun Modifier.keepsHubStill(): Modifier {
    val touch = LocalHubWheelTouch.current ?: return this
    return pointerInput(touch) {
        awaitEachGesture {
            // The Initial pass sees the finger before the wheel and the hub do anything with it
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            touch.down()
            try {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                } while (event.changes.fastAny { it.pressed })
            } finally {
                // Also when the wheel leaves the screen in the middle of the gesture
                touch.up()
            }
        }
    }
}

/**
 * Lets a swipe down on the content of a locked hub pull the hub as the pill does, and close its editor when the
 * finger lifts far enough down ([HubSheetState.settle]). An open hub, or one without [onDismiss], leaves every
 * gesture to its content, and so does a hub whose number wheel is being turned ([wheelTouch]).
 */
private class HubPullToDismiss(
    private val sheet: HubSheetState,
    private val wheelTouch: HubWheelTouch
) : NestedScrollConnection {
    var onDismiss: (() -> Unit)? = null

    /**
     * Whether this gesture is pulling the hub. It is not enough that a locked hub is below open: it is also there
     * while it opens, and a fling of a wheel at that moment must not close the editor.
     */
    private var pulling = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Back up first: the hub the finger pulled down returns before the content scrolls again
        if (source != NestedScrollSource.UserInput || !pulling || !sheet.pulledWhileLocked || available.y >= 0f) return Offset.Zero
        sheet.dragBy(available.y)
        return Offset(0f, available.y)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || wheelTouch.active || onDismiss == null || !sheet.locked || available.y <= 0f) {
            return Offset.Zero
        }
        if (!pulling) sheet.dragStarted()
        pulling = true
        sheet.dragBy(available.y)
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!pulling) return Velocity.Zero
        pulling = false
        sheet.settle(available.y, onDismiss)
        return available
    }
}

/** The pill on top of the hub: drag it, or tap it to switch step. A locked hub pulled down closes its editor ([onDismiss]). */
@Composable
private fun HubGrabber(sheet: HubSheetState, hapticEnabled: Boolean, onDismiss: (() -> Unit)?) {
    val haptic = LocalHapticFeedback.current
    val hapticOn by rememberUpdatedState(hapticEnabled)
    val dragState = rememberDraggableState { delta ->
        val before = sheet.expansion
        sheet.dragBy(delta)
        // A tick as the hub passes the point where letting go would switch step
        if (hapticOn && !sheet.locked && (before < 0.5f) != (sheet.expansion < 0.5f)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val label = stringResource(if (sheet.detent >= 0.5f) Res.string.hub_panel_collapse else Res.string.hub_panel_expand)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(GRABBER_HEIGHT)
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStarted = { sheet.dragStarted() },
                onDragStopped = { velocity -> sheet.settle(velocity, onDismiss) }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !sheet.locked,
                onClickLabel = label,
                role = Role.Button,
                onClick = sheet::toggle
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(OnSurfaceVariant.copy(alpha = 0.4f))
        )
    }
}

/**
 * Part of the hub that leaves as it shrinks: fully there from [to] up, gone at [from] and below, and in
 * between it collapses and fades with the finger. [gap] and [gapBelow] are the space above and under it, which
 * go with it (nothing is left over when the content itself is empty).
 */
@Composable
fun HubSlot(
    expansion: () -> Float,
    from: Float,
    to: Float,
    modifier: Modifier = Modifier,
    gap: Dp = 0.dp,
    gapBelow: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    // Once it is gone it is not composed at all, so nothing invisible can be reached by touch or by a screen reader
    val gone by remember(expansion, from) { derivedStateOf { expansion() <= from } }
    if (gone) return
    Box(modifier = modifier.hubSlotLayout(expansion, from, to, gap, gapBelow)) { content() }
}

private fun Modifier.hubSlotLayout(expansion: () -> Float, from: Float, to: Float, gap: Dp, gapBelow: Dp): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val t = HubMotion.window(expansion(), from, to)
        val gapPx = if (placeable.height > 0) gap.roundToPx() else 0
        val gapBelowPx = if (placeable.height > 0) gapBelow.roundToPx() else 0
        layout(placeable.width, ((placeable.height + gapPx + gapBelowPx) * t).roundToInt()) {
            placeable.placeWithLayer(0, gapPx) { alpha = t }
        }
    }.clipToBounds()

/** This style at [factor] of its size, for text that shrinks with the hub. */
fun TextStyle.hubScaled(factor: Float): TextStyle = copy(
    fontSize = if (fontSize.isSpecified) fontSize * factor else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * factor else lineHeight
)

private fun Modifier.hubSpace(expansion: () -> Float, closed: Dp, open: Dp): Modifier =
    layout { _, constraints ->
        layout(constraints.minWidth, lerp(closed, open, expansion()).roundToPx()) {}
    }
