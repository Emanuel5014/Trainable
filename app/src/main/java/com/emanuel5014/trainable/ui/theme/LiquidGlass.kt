package com.emanuel5014.trainable.ui.theme

import android.os.Build
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.data.model.GlassSettings
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/** The user's Liquid Glass choices, provided once at the root of the app. */
val LocalGlassSettings = staticCompositionLocalOf { GlassSettings() }

/**
 * The app-wide copy of the screens, recorded behind the navigation (see MainActivity). Only what is
 * drawn outside the screens may bend it, such as the expressive navbar's action button; null when nothing uses it.
 */
val LocalAppGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** Below Android 12 there is no blur, so glass needs a fuller body to keep its content readable. */
val GlassCanBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** The swell and the lean after the finger of a touched piece of glass, as on iOS 26. */
private fun GraphicsLayerScope.followTouch(touch: GlassTouch) {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return
    val scale = lerp(1f, 1f + 4.dp.toPx() / height, touch.press)

    val maxOffset = size.minDimension
    val offset = touch.offset
    translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
    translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)

    val maxDragScale = 4.dp.toPx() / height
    val angle = atan2(offset.y, offset.x)
    scaleX = scale + maxDragScale * abs(cos(angle) * offset.x / size.maxDimension) * (width / height).fastCoerceAtMost(1f)
    scaleY = scale + maxDragScale * abs(sin(angle) * offset.y / size.maxDimension) * (height / width).fastCoerceAtMost(1f)
}

/**
 * Liquid Glass on a neutral round header button: a lit rim, a soft lift and a translucent body that
 * swells and lights up under the finger. Headers scroll with their screen, so there is only the screen
 * background beneath to show through. Returns the receiver unchanged when the user has this part off.
 */
@Composable
fun Modifier.headerGlass(): Modifier = surfaceGlass(GlassPart.HeaderButtons)

/**
 * The neutral glass of a control that scrolls with its screen (header buttons, search bar, tab track): a lit
 * rim, a soft lift and a translucent body over the screen background, that swells and lights up under the
 * finger. Returns the receiver unchanged when the user has [part] off.
 */
@Composable
fun Modifier.surfaceGlass(part: GlassPart): Modifier {
    val glass = LocalGlassSettings.current
    if (!glass.isOn(part)) return this
    val amount = glass.intensity
    val background = MaterialTheme.colorScheme.surface
    val dark = background.luminance() < 0.5f
    val body = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = lerp(0.9f, 0.35f, amount))
    val backdrop = rememberCanvasBackdrop { drawRect(background) }
    val scope = rememberCoroutineScope()
    val touch = remember(scope) { GlassTouch(scope) }
    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { Capsule() },
            effects = { vibrancy() },
            highlight = { Highlight.Default.copy(alpha = lerp(0.6f, 1f, amount)) },
            shadow = { Shadow(radius = 10.dp, color = Color.Black.copy(alpha = if (dark) 0.25f else 0.08f)) },
            layerBlock = { followTouch(touch) },
            onDrawSurface = { drawRect(body) }
        )
        .then(touch.light)
        .then(touch.gestures)
}

/** True when header buttons are glass: the places that group buttons then draw one capsule for the group. */
@Composable
fun headerGlassOn(): Boolean = LocalGlassSettings.current.isOn(GlassPart.HeaderButtons)

/**
 * The container of header buttons that belong together: one capsule of glass around them, as iOS 26 groups its
 * toolbar buttons, instead of one glass circle each. Put the buttons inside with no fill of their own.
 */
@Composable
fun Modifier.headerGroupGlass(): Modifier = surfaceGlass(GlassPart.HeaderButtons).clip(CircleShape)

/**
 * What a screen's floating buttons bend: a copy of the screen's own content, recorded by
 * [actionGlassSource] on the list beneath them. Null when the user has action glass off.
 *
 * The buttons cannot use the app-wide copy behind the navigation bar, because they are part of it:
 * glass drawing a copy of itself would loop.
 */
@Composable
fun rememberActionGlassSource(): LayerBackdrop? {
    if (!LocalGlassSettings.current.isOn(GlassPart.ActionButtons)) return null
    val background = MaterialTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(background)
        drawContent()
    }
}

/** Records this content for the screen's floating buttons; put it on the list under them, never on the buttons. */
fun Modifier.actionGlassSource(source: LayerBackdrop?): Modifier =
    if (source == null) this else layerBackdrop(source)

/**
 * Tinted Liquid Glass for a floating action button, like the primary actions of iOS: [tint] stays the
 * button's colour, now translucent over the content it bends, and the glass swells and lights up under
 * the finger. Use together with [actionGlassContainer] and [actionGlassElevation] so the button's own fill
 * and shadow give way to the glass. [cornerRadius] null means a pill or a circle.
 */
@Composable
fun Modifier.actionGlass(source: Backdrop?, tint: Color, cornerRadius: Dp? = null): Modifier {
    if (source == null) return this
    val amount = LocalGlassSettings.current.intensity
    val blurScale = LocalGlassSettings.current.blurScale
    // The label colours are the button's own (on-primary, on-container), picked against its solid tint: the veil
    // stays dense enough that the glass never drifts far from it, whatever slides underneath
    val veil = tint.copy(alpha = if (GlassCanBlur) lerp(0.97f, 0.86f, amount) else 0.97f)
    val background = MaterialTheme.colorScheme.surface
    val scope = rememberCoroutineScope()
    val touch = remember(scope) { GlassTouch(scope) }
    return this
        .drawBackdrop(
            backdrop = source,
            shape = { if (cornerRadius == null) Capsule() else RoundedRectangle(cornerRadius) },
            effects = {
                vibrancy()
                blur(lerp(2f, 6f, amount).dp.toPx() * blurScale)
                lens(lerp(6f, 16f, amount).dp.toPx(), lerp(8f, 26f, amount).dp.toPx())
            },
            shadow = { Shadow(radius = 16.dp, color = Color.Black.copy(alpha = 0.18f)) },
            layerBlock = { followTouch(touch) },
            // A button that sticks out of the recorded list would otherwise show a hole there
            onDrawBehind = { drawRect(background) },
            onDrawSurface = {
                // The tint colours what is beneath (hue), then a veil of it keeps the label readable
                drawRect(tint, blendMode = BlendMode.Hue)
                drawRect(veil)
            }
        )
        .then(touch.light)
        .then(touch.gestures)
}

/** The button's own fill: clear when the glass draws the body instead. */
fun actionGlassContainer(source: Backdrop?, color: Color): Color =
    if (source == null) color else Color.Transparent

/** The button's own shadow: none when the glass casts its own. */
@Composable
fun actionGlassElevation(
    source: Backdrop?,
    default: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation()
): FloatingActionButtonElevation =
    if (source == null) default else FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)

/** What the workout panel frosts: the exercise content beside it, recorded with [actionGlassSource]. */
val LocalPanelGlassSource = staticCompositionLocalOf<LayerBackdrop?> { null }

/** A copy of the workout's exercise content for its bottom panel; null when the user has this part off. */
@Composable
fun rememberPanelGlassSource(): LayerBackdrop? {
    if (!LocalGlassSettings.current.isOn(GlassPart.WorkoutPanel)) return null
    val background = MaterialTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(background)
        drawContent()
    }
}

/**
 * Frosted Liquid Glass for a large panel, as iOS and Telegram do for their big surfaces: a strong blur
 * with vibrancy and a lit rim, no lens, so the numbers on it stay calm and readable. Make the panel's own
 * fill and shadow clear when [source] is not null.
 */
@Composable
fun Modifier.panelGlass(source: Backdrop?, shape: Shape, tint: Color): Modifier {
    if (source == null) return this
    val amount = LocalGlassSettings.current.intensity
    val blurScale = LocalGlassSettings.current.blurScale
    val body = tint.copy(alpha = if (GlassCanBlur) lerp(0.96f, 0.7f, amount) else 0.97f)
    val background = MaterialTheme.colorScheme.surface
    return drawBackdrop(
        backdrop = source,
        shape = { shape },
        effects = {
            vibrancy()
            blur(lerp(12f, 24f, amount).dp.toPx() * blurScale)
        },
        highlight = { Highlight.Default.copy(alpha = 0.8f) },
        shadow = { Shadow(radius = 24.dp, color = Color.Black.copy(alpha = 0.25f)) },
        onDrawBehind = { drawRect(background) },
        onDrawSurface = { drawRect(body) }
    )
}
