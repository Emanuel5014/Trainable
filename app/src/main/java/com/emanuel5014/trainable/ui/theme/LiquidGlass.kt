@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package com.emanuel5014.trainable.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import com.emanuel5014.trainable.data.model.AppBackground
import com.emanuel5014.trainable.util.WallpaperStorage
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.RefractionProfile
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource

const val DefaultGlassIntensity = 0.75f
val LocalLiquidGlassEnabled = staticCompositionLocalOf { false }
val LocalLiquidGlassPreference = staticCompositionLocalOf { false }
val LocalGlassIntensity = compositionLocalOf { DefaultGlassIntensity }
val LocalLightweightGlassPanels = staticCompositionLocalOf { false }
val LocalGlassBackdrop = staticCompositionLocalOf<HazeState?> { null }
val LocalAppBackground = compositionLocalOf { AppBackground() }
const val GlassHighlightStrength = .70f

/** Only screen backgrounds become clear; dialogs and sheets keep their opaque surface. */
val ScreenSurface: Color
    @Composable get() = if (LocalLiquidGlassEnabled.current || LocalAppBackground.current.isCustom) Color.Transparent else Surface

/** Standard screens keep their own tinted gradients; glass and custom backgrounds replace them with one uniform backdrop. */
val UsesStandardBackdrop: Boolean
    @Composable get() = !LocalLiquidGlassEnabled.current && !LocalAppBackground.current.isCustom

@Composable
fun LiquidGlassBackdrop(state: HazeState?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val appearance = LocalAppBackground.current
    val context = LocalContext.current
    val wallpaper = remember(context, appearance.wallpaperName) { WallpaperStorage.file(context, appearance.wallpaperName) }
    val fill = if (appearance.mode == AppBackground.COLOR) Color.Black.copy(alpha = appearance.dimming).compositeOver(Color(appearance.color)) else colors.background
    Box(modifier.fillMaxSize().then(if (state != null) Modifier.hazeSource(state) else Modifier).background(fill)) {
        if (appearance.mode == AppBackground.WALLPAPER && wallpaper != null) {
            AsyncImage(model = wallpaper, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = appearance.dimming)))
        }
    }
}

/** A draw-only reflection, shared with the optical light; all v5 halo alphas are scaled by 70%. */
fun Modifier.glassRim(shape: Shape, light: GlassLight, first: Float, middle: Float, last: Float): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val stroke = Stroke(.75.dp.toPx())
    onDrawWithContent {
        drawContent()
        val bias = light.bias
        val center = Offset(size.width / 2f, size.height / 2f)
        val direction = Offset(bias.x * size.width / 2f, bias.y * size.height / 2f)
        val safeDirection = if (direction.getDistanceSquared() < 1f) Offset(1f, 1f) else direction
        drawOutline(outline, Brush.linearGradient(listOf(Color.White.copy(alpha = first * GlassHighlightStrength),
            Color.White.copy(alpha = middle * GlassHighlightStrength), Color.White.copy(alpha = last * GlassHighlightStrength)),
            start = center + safeDirection, end = center - safeDirection), style = stroke)
    }
}

/** Real optical glass: refraction, a shallow blur, edge folds and directional lighting. */
@Composable
fun Modifier.liquidGlassSurface(
    shape: Shape,
    tint: Color,
    interactionSource: InteractionSource? = null,
    backdrop: HazeState? = LocalGlassBackdrop.current,
    enabled: Boolean = true,
    control: Boolean = false,
    quiet: Boolean = false
): Modifier {
    val amount = LocalGlassIntensity.current.coerceIn(0f, 1f)
    if (!LocalLiquidGlassEnabled.current || amount == 0f) return this
    val colors = MaterialTheme.colorScheme
    val light = LocalGlassLight.current
    val dark = colors.background.luminance() < 0.5f
    val base = if (quiet) colors.surfaceContainerHigh else colors.surface
    val rounded = shape as? RoundedCornerShape ?: RoundedCornerShape(24.dp)
    // A caller may intentionally use a subtle tint (supersets, journal chips).
    // Preserve that strength instead of promoting a 10% accent to a full-color fill.
    val tintColor = if (tint.alpha == 0f) colors.surfaceContainerHigh
        else lerp(colors.surfaceContainerHigh, tint.copy(alpha = 1f), tint.alpha)
    val opacity = if (quiet) .42f - amount * .10f else (if (control) 0.45f else 0.62f) - amount * (if (control) 0.31f else 0.38f)
    if (!control && LocalLightweightGlassPanels.current) {
        // A settings card can be several screens tall. Keep its transparent material
        // in the normal draw pass rather than allocating/refracting a tall texture
        // on every scroll frame. Small controls retain their optical glass.
        return clip(shape).background(tintColor.copy(alpha = opacity))
            .glassRim(shape, light, if (dark) .30f else .80f, .04f, if (dark) .12f else .42f)
    }
    val style = remember(rounded, tintColor, base, dark, amount, enabled, control, light, quiet) {
        GlassStyle.clear.then {
            shape(rounded)
            backgroundColor(base)
            tint(tintColor.copy(alpha = if (enabled) opacity else opacity * 0.5f))
            optics(
                refractionStrength = amount * 0.95f,
                refractionDisplacement = (if (quiet) 3f + 8f * amount else 8f + 34f * amount).dp,
                refractionHeightFraction = 0.35f,
                refractionProfile = RefractionProfile.Edge((8f + 16f * amount).dp),
                refractionFoldStrength = amount * 0.14f,
                refractionDetailIntensity = 0f,
                // These surfaces sample a static backdrop. A sharp sample avoids an extra
                // blur texture for every button and for tall, scrolling settings panels.
                blurRadius = 0.dp,
                depth = 0f
            )
            specularIntensity((if (quiet) .12f + amount * .12f else .20f + amount * .70f) * (if (enabled) 1f else 0.4f) * GlassHighlightStrength)
            ambientResponse((if (quiet) .08f else if (dark) .10f + amount * .12f else .18f + amount * .22f) * GlassHighlightStrength)
            edgeSoftness((1f + amount * 2f).dp)
            edgeShadow(if (quiet) Color.Transparent else Color.Black.copy(alpha = if (dark) .20f else .09f))
            chromaticAberrationStrength(0f)
            whitePoint(if (quiet) 0f else if (dark) -.18f else .17f)
            contrast(if (quiet) .02f else if (dark) .12f else .08f)
            lightPosition(light)
            // Material and Gym controls already provide ripple/scale feedback. Do not
            // abruptly change the whole glass layer's white point on press/release.
        }
    }
    if (backdrop == null) return clip(shape).background(tintColor.copy(alpha = opacity))
    val input = remember(backdrop) { HazeInput.Sources(backdrop) }
    return clip(shape).hazeGlass(
        input = input,
        style = style,
        interactionSource = interactionSource,
        performanceMode = if (control) HazePerformanceMode.Performance else HazePerformanceMode.Balanced
    )
}

/** Two retained lenses: a thin navigation shell and the water drop moving under the labels. */
@Composable
fun Modifier.navigationGlass(backdrop: HazeState, dark: Boolean, droplet: Boolean = false): Modifier {
    val amount = LocalGlassIntensity.current.coerceIn(0f, 1f)
    val colors = MaterialTheme.colorScheme
    val tint = if (droplet) colors.primary else colors.surface
    val background = colors.background
    val light = LocalGlassLight.current
    val style = remember(amount, dark, droplet, tint, background, light) {
        GlassStyle.clear.then {
            shape(ShapeFull)
            backgroundColor(background)
            tint(tint.copy(alpha = if (droplet) 0.035f + amount * 0.055f else if (dark) 0.16f else 0.27f))
            optics(
                refractionStrength = if (droplet) 0.65f + amount * 0.35f else amount * 0.45f,
                refractionDisplacement = (if (droplet) 24f + amount * 30f else 12f + amount * 8f).dp,
                refractionProfile = RefractionProfile.Edge((if (droplet) 20f else 10f).dp),
                refractionHeightFraction = if (droplet) 0.45f else 0.22f,
                refractionFoldStrength = if (droplet) amount * 0.12f else 0f,
                refractionDetailIntensity = 0f,
                blurRadius = 0.dp,
                depth = 0f
            )
            specularIntensity((if (droplet) 0.90f else 0.48f) * GlassHighlightStrength)
            ambientResponse((if (dark) 0.12f else 0.24f) * GlassHighlightStrength)
            edgeSoftness(if (droplet) 1.5.dp else 1.dp)
            edgeShadow(Color.Black.copy(alpha = if (droplet) 0.10f else 0.05f))
            whitePoint(if (dark) -0.10f else 0.08f)
            contrast(0.05f)
            chromaticAberrationStrength(0f)
            lightPosition(light)
        }
    }
    val input = remember(backdrop) { HazeInput.Sources(backdrop) }
    return clip(ShapeFull).hazeGlass(input, style, performanceMode = HazePerformanceMode.Balanced)
}

/** Keep custom button foregrounds, but replace an on-fill color when its fill becomes clear. */
@Composable
fun glassControlContentColor(original: Color): Color {
    if (!LocalLiquidGlassEnabled.current) return original
    val colors = MaterialTheme.colorScheme
    return when (original) {
        colors.onPrimary, colors.onSecondary, colors.onTertiary -> colors.onSurface
        colors.onError, colors.onErrorContainer -> colors.error
        else -> original
    }
}
