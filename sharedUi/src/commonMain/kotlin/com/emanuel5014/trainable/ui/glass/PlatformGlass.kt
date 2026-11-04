package com.emanuel5014.trainable.ui.glass

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SliderColors
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.emanuel5014.trainable.ui.theme.LocalGlassSettings
import com.emanuel5014.trainable.data.model.GlassPart
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass as the shared screens ask for it. The Android app draws it (LiquidGlass.kt, provided at its root);
 * without a provider, as on iOS for now, every effect leaves the element as it is: the Classic look.
 */
interface PlatformGlass {
    /** The glass of the round header buttons, or [modifier] unchanged. */
    @Composable
    fun header(modifier: Modifier): Modifier

    /** The recording the floating buttons of a screen bend; null while action glass is off. */
    @Composable
    fun rememberActionSource(): GlassSource?

    /** The glass of the navbar's action button, which sits outside the screens and bends a copy of all of them. */
    @Composable
    fun rememberNavBarActionSource(): GlassSource? = null

    /** Whether this platform draws [SegmentedTabs]; without it the screens draw their classic tabs. */
    val hasSegmentedTabs: Boolean get() = false

    /** Glass tabs whose capsule follows [progress] (0 = first tab) and can be dragged ([onScrub], [onScrubEnd]). */
    @Composable
    fun SegmentedTabs(
        titles: List<String>,
        selectedIndex: Int,
        progress: () -> Float,
        onSelect: (Int) -> Unit,
        modifier: Modifier,
        labelSize: TextUnit,
        onScrub: ((position: Float) -> Unit)?,
        onScrubEnd: ((index: Int) -> Unit)?,
        onTick: () -> Unit
    ) {}

    /** Whether this platform draws [surface]; without it the controls that would wear it keep their classic look. */
    val hasSurfaceGlass: Boolean get() = false

    /** The neutral glass of a control that scrolls with its screen (search bar, tab track), or [modifier] unchanged. */
    @Composable
    fun surface(modifier: Modifier, part: GlassPart): Modifier = modifier

    /** Whether this platform draws [GlassSlider]; without it sliders keep the Material thumb. */
    val hasGlassSlider: Boolean get() = false

    /** The Material slider with its thumb in glass; same parameters as the shared Slider. */
    @Composable
    fun GlassSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        onValueChangeFinished: (() -> Unit)?,
        colors: SliderColors,
        interactionSource: MutableInteractionSource
    ) {}

    /** Put inside a sheet or dialog that should open without the blur behind it. */
    @Composable
    fun NoOverlayBlur() {}
}

/** Content recorded for the floating buttons above it; only the platform drawing the glass knows what is inside. */
interface GlassSource {
    /** Records the content of [modifier]; put it on the list under the buttons, never on the buttons. */
    fun record(modifier: Modifier): Modifier

    /** Tinted glass on a floating button over the recorded content. [cornerRadius] null means a pill or a circle. */
    @Composable
    fun bend(modifier: Modifier, tint: Color, cornerRadius: Dp?): Modifier
}

private object ClassicGlass : PlatformGlass {
    @Composable
    override fun header(modifier: Modifier): Modifier = modifier

    @Composable
    override fun rememberActionSource(): GlassSource? = null
}

val LocalPlatformGlass = staticCompositionLocalOf<PlatformGlass> { ClassicGlass }

@Composable
fun Modifier.headerGlass(): Modifier = LocalPlatformGlass.current.header(this)

@Composable
fun Modifier.surfaceGlass(part: GlassPart): Modifier = LocalPlatformGlass.current.surface(this, part)

/** Whether header buttons wear glass: the user has the part on and the platform draws it. */
@Composable
fun headerGlassOn(): Boolean = LocalGlassSettings.current.isOn(GlassPart.HeaderButtons) && LocalPlatformGlass.current.hasSurfaceGlass

/**
 * The container of header buttons that belong together: one capsule of glass around them, as iOS 26 groups its
 * toolbar buttons, instead of one glass circle each. Put the buttons inside with no fill of their own.
 */
@Composable
fun Modifier.headerGroupGlass(): Modifier = surfaceGlass(GlassPart.HeaderButtons).clip(CircleShape)

@Composable
fun rememberActionGlassSource(): GlassSource? = LocalPlatformGlass.current.rememberActionSource()

fun Modifier.actionGlassSource(source: GlassSource?): Modifier = source?.record(this) ?: this

@Composable
fun Modifier.actionGlass(source: GlassSource?, tint: Color, cornerRadius: Dp? = null): Modifier =
    source?.bend(this, tint, cornerRadius) ?: this

/** The button's own fill: clear when the glass draws the body instead. */
fun actionGlassContainer(source: GlassSource?, color: Color): Color =
    if (source == null) color else Color.Transparent

/** The button's own shadow: none when the glass casts its own. */
@Composable
fun actionGlassElevation(
    source: GlassSource?,
    default: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation()
): FloatingActionButtonElevation =
    if (source == null) default else FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
