@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.emanuel5014.trainable.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.emanuel5014.trainable.ui.theme.LocalGlassIntensity
import com.emanuel5014.trainable.ui.theme.LocalLightweightGlassPanels
import com.emanuel5014.trainable.ui.theme.LocalLiquidGlassEnabled
import com.emanuel5014.trainable.ui.theme.liquidGlassSurface
import com.emanuel5014.trainable.ui.theme.glassRim
import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.compose.material3.ModalBottomSheet as MaterialModalBottomSheet

/** Raised glass remains legible over content and avoids recapturing an animated popup window. */
@Composable
internal fun Modifier.raisedGlass(shape: Shape, tint: Color): Modifier {
    if (!LocalLiquidGlassEnabled.current) return this
    val amount = LocalGlassIntensity.current.coerceIn(0f, 1f)
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < .5f
    val base = lerp(scheme.surface, tint, .25f)
    val sheen = lerp(base, scheme.primary, .04f + .035f * amount)
    return clip(shape)
        .background(sheen.copy(alpha = 1f - .06f * amount))
        .glassRim(shape, com.emanuel5014.trainable.ui.theme.LocalGlassLight.current,
            if (dark) .16f + .16f * amount else .65f, .04f, if (dark) .08f else .35f)
}

/** For hand-built panels that previously painted an opaque Material container. */
@Composable
fun Modifier.glassPanelBackground(color: Color, shape: Shape = MaterialTheme.shapes.large): Modifier =
    if (LocalLiquidGlassEnabled.current) liquidGlassSurface(shape, color) else background(color, shape)

@Composable
fun GlassModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalLiquidGlassEnabled.current
    val sheetColor = if (glass) lerp(containerColor, MaterialTheme.colorScheme.primary, .025f)
        .copy(alpha = 1f - LocalGlassIntensity.current.coerceIn(0f, 1f) * .04f) else containerColor
    CompositionLocalProvider(LocalLightweightGlassPanels provides true) {
        MaterialModalBottomSheet(
            // BottomSheet applies draggableAnchors AFTER its caller modifier. Keep all
            // rendering in its own moving Surface, otherwise decoration clips the footer
            // and stays at the origin as the sheet moves.
            onDismissRequest, modifier,
            sheetState, sheetMaxWidth, sheetGesturesEnabled, shape,
            sheetColor, contentColor,
            if (glass) 0.dp else tonalElevation, scrimColor,
            if (dragHandle == null || !glass) dragHandle else {
                { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { dragHandle() } }
            }, contentWindowInsets, properties, content
        )
    }
}

@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties()
) {
    val glass = LocalLiquidGlassEnabled.current
    CompositionLocalProvider(LocalLightweightGlassPanels provides true) {
        MaterialAlertDialog(onDismissRequest, confirmButton,
            if (glass) modifier.raisedGlass(shape, containerColor) else modifier,
            dismissButton, icon, title, text, shape, if (glass) Color.Transparent else containerColor,
            iconContentColor, titleContentColor, textContentColor, if (glass) 0.dp else tonalElevation, properties)
    }
}
