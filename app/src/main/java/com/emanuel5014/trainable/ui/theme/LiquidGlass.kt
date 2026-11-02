package com.emanuel5014.trainable.ui.theme

import android.os.Build
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.data.model.GlassSettings
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

/** The user's Liquid Glass choices, provided once at the root of the app. */
val LocalGlassSettings = staticCompositionLocalOf { GlassSettings() }

/** Below Android 12 there is no blur, so glass needs a fuller body to keep its content readable. */
val GlassCanBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Liquid Glass on a neutral round header button: a lit rim, a soft lift and a translucent body.
 * Headers scroll with their screen, so there is only the screen background beneath to show through.
 * Returns the receiver unchanged when the user has this part off.
 */
@Composable
fun Modifier.headerGlass(): Modifier {
    val glass = LocalGlassSettings.current
    if (!glass.isOn(GlassPart.HeaderButtons)) return this
    val amount = glass.intensity
    val background = MaterialTheme.colorScheme.surface
    val dark = background.luminance() < 0.5f
    val body = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = lerp(0.9f, 0.35f, amount))
    val backdrop = rememberCanvasBackdrop { drawRect(background) }
    return drawBackdrop(
        backdrop = backdrop,
        shape = { Capsule() },
        effects = { vibrancy() },
        highlight = { Highlight.Default.copy(alpha = lerp(0.6f, 1f, amount)) },
        shadow = { Shadow(radius = 10.dp, color = Color.Black.copy(alpha = if (dark) 0.25f else 0.08f)) },
        onDrawSurface = { drawRect(body) }
    )
}

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
 * button's colour, now translucent over the content it bends. Use together with [actionGlassContainer]
 * and [actionGlassElevation] so the button's own fill and shadow give way to the glass.
 * [cornerRadius] null means a pill or a circle.
 */
@Composable
fun Modifier.actionGlass(source: LayerBackdrop?, tint: Color, cornerRadius: Dp? = null): Modifier {
    if (source == null) return this
    val amount = LocalGlassSettings.current.intensity
    val body = tint.copy(alpha = if (GlassCanBlur) lerp(0.96f, 0.62f, amount) else 0.96f)
    return drawBackdrop(
        backdrop = source,
        shape = { if (cornerRadius == null) Capsule() else RoundedRectangle(cornerRadius) },
        effects = {
            vibrancy()
            blur(lerp(2f, 6f, amount).dp.toPx())
            lens(lerp(6f, 16f, amount).dp.toPx(), lerp(8f, 26f, amount).dp.toPx())
        },
        shadow = { Shadow(radius = 16.dp, color = Color.Black.copy(alpha = 0.18f)) },
        onDrawSurface = { drawRect(body) }
    )
}

/** The button's own fill: clear when the glass draws the body instead. */
fun actionGlassContainer(source: LayerBackdrop?, color: Color): Color =
    if (source == null) color else Color.Transparent

/** The button's own shadow: none when the glass casts its own. */
@Composable
fun actionGlassElevation(
    source: LayerBackdrop?,
    default: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation()
): FloatingActionButtonElevation =
    if (source == null) default else FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
