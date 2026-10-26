@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.emanuel5014.trainable.ui.components.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.LocalLiquidGlassEnabled
import com.emanuel5014.trainable.ui.theme.ShapeFull
import com.emanuel5014.trainable.ui.theme.glassControlContentColor
import com.emanuel5014.trainable.ui.theme.liquidGlassSurface

// Keep Material behavior, semantics, touch targets and caller colors when glass is disabled.
// Only the container's rendering and on-fill foreground colors change for clear glass.
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.FilledTonalButton as MaterialFilledTonalButton
import androidx.compose.material3.ElevatedButton as MaterialElevatedButton
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.FilledIconButton as MaterialFilledIconButton
import androidx.compose.material3.FilledTonalIconButton as MaterialFilledTonalIconButton
import androidx.compose.material3.OutlinedIconButton as MaterialOutlinedIconButton
import androidx.compose.material3.IconToggleButton as MaterialIconToggleButton
import androidx.compose.material3.ToggleButton as MaterialToggleButton
import androidx.compose.material3.FloatingActionButton as MaterialFloatingActionButton
import androidx.compose.material3.ExtendedFloatingActionButton as MaterialExtendedFloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton as MaterialSmallFloatingActionButton
import androidx.compose.material3.LargeFloatingActionButton as MaterialLargeFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButton as MaterialToggleFloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenuItem as MaterialFloatingActionButtonMenuItem
import androidx.compose.material3.SegmentedButton as MaterialSegmentedButton
import androidx.compose.material3.RadioButton as MaterialRadioButton
import androidx.compose.material3.FilterChip as MaterialFilterChip
import androidx.compose.material3.AssistChip as MaterialAssistChip
import androidx.compose.material3.InputChip as MaterialInputChip
import androidx.compose.material3.SuggestionChip as MaterialSuggestionChip
import androidx.compose.material3.Surface as MaterialSurface
import androidx.compose.material3.Card as MaterialCard

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    quiet: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor, controlInteraction, enabled = enabled, control = true, quiet = quiet) else modifier
    MaterialButton(
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialButton(
        onClick = onClick,
        shapes = shapes,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialTextButton(
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassTextButton(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialTextButton(
        onClick = onClick,
        shapes = shapes,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassOutlinedButton(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialOutlinedButton(
        onClick = onClick,
        shapes = shapes,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialFilledTonalButton(
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassFilledTonalButton(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialFilledTonalButton(
        onClick = onClick,
        shapes = shapes,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.elevatedShape,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialElevatedButton(
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassElevatedButton(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialElevatedButton(
        onClick = onClick,
        shapes = shapes,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = IconButtonDefaults.standardShape,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialIconButton(
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        interactionSource = controlInteraction,
        shape = shape,
        content = content,
    )
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    shapes: IconButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialIconButton(
        onClick = onClick,
        shapes = shapes,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassFilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialFilledIconButton(
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassFilledIconButton(
    onClick: () -> Unit,
    shapes: IconButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialFilledIconButton(
        onClick = onClick,
        shapes = shapes,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassFilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialFilledTonalIconButton(
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassFilledTonalIconButton(
    onClick: () -> Unit,
    shapes: IconButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialFilledTonalIconButton(
        onClick = onClick,
        shapes = shapes,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassOutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.outlinedShape,
    colors: IconButtonColors = IconButtonDefaults.outlinedIconButtonColors(),
    border: BorderStroke? = IconButtonDefaults.outlinedIconButtonBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialOutlinedIconButton(
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        border = border,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassOutlinedIconButton(
    onClick: () -> Unit,
    shapes: IconButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.outlinedIconButtonColors(),
    border: BorderStroke? = IconButtonDefaults.outlinedIconButtonBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialOutlinedIconButton(
        onClick = onClick,
        shapes = shapes,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        border = border,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = IconButtonDefaults.standardShape,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, if (checked) colors.checkedContainerColor else colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor), checkedContainerColor = Color.Transparent, checkedContentColor = glassControlContentColor(colors.checkedContentColor)) else colors,
        interactionSource = controlInteraction,
        shape = shape,
        content = content,
    )
}

@Composable
fun GlassIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: IconToggleButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonVibrantColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, if (checked) colors.checkedContainerColor else colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = shapes,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor), checkedContainerColor = Color.Transparent, checkedContentColor = glassControlContentColor(colors.checkedContentColor)) else colors,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(ButtonDefaults.MinHeight),
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    quiet: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, if (checked) colors.checkedContainerColor else colors.containerColor, controlInteraction, enabled = enabled, control = true, quiet = quiet) else modifier
    val glassShapes = if (glass) shapes.copy(pressedShape = shapes.shape, checkedShape = shapes.shape) else shapes
    MaterialToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = glassModifier,
        enabled = enabled,
        shapes = glassShapes,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor), checkedContainerColor = Color.Transparent, checkedContentColor = glassControlContentColor(colors.checkedContentColor)) else colors,
        elevation = if (glass) null else elevation,
        border = if (glass && checked && enabled) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .60f)) else border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, containerColor, controlInteraction, enabled = true, control = true) else modifier
    MaterialFloatingActionButton(
        onClick = onClick,
        modifier = glassModifier,
        shape = shape,
        containerColor = if (glass) Color.Transparent else containerColor,
        contentColor = if (glass) glassControlContentColor(contentColor) else contentColor,
        elevation = if (glass) FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp) else elevation,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, containerColor, controlInteraction, enabled = true, control = true) else modifier
    MaterialExtendedFloatingActionButton(
        onClick = onClick,
        modifier = glassModifier,
        shape = shape,
        containerColor = if (glass) Color.Transparent else containerColor,
        contentColor = if (glass) glassControlContentColor(contentColor) else contentColor,
        elevation = if (glass) FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp) else elevation,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, containerColor, controlInteraction, enabled = true, control = true) else modifier
    MaterialExtendedFloatingActionButton(
        text = text,
        icon = icon,
        onClick = onClick,
        modifier = glassModifier,
        expanded = expanded,
        shape = shape,
        containerColor = if (glass) Color.Transparent else containerColor,
        contentColor = if (glass) glassControlContentColor(contentColor) else contentColor,
        elevation = if (glass) FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp) else elevation,
        interactionSource = controlInteraction,
    )
}

@Composable
fun GlassSmallFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.smallShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, containerColor, controlInteraction, enabled = true, control = true) else modifier
    MaterialSmallFloatingActionButton(
        onClick = onClick,
        modifier = glassModifier,
        shape = shape,
        containerColor = if (glass) Color.Transparent else containerColor,
        contentColor = if (glass) glassControlContentColor(contentColor) else contentColor,
        elevation = if (glass) FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp) else elevation,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassLargeFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.largeShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, containerColor, controlInteraction, enabled = true, control = true) else modifier
    MaterialLargeFloatingActionButton(
        onClick = onClick,
        modifier = glassModifier,
        shape = shape,
        containerColor = if (glass) Color.Transparent else containerColor,
        contentColor = if (glass) glassControlContentColor(contentColor) else contentColor,
        elevation = if (glass) FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp) else elevation,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassToggleFloatingActionButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: (Float) -> Color = ToggleFloatingActionButtonDefaults.containerColor(),
    contentAlignment: Alignment = Alignment.TopEnd,
    containerSize: (Float) -> Dp = ToggleFloatingActionButtonDefaults.containerSize(),
    containerCornerRadius: (Float) -> Dp =
        ToggleFloatingActionButtonDefaults.containerCornerRadius(),
    content: @Composable ToggleFloatingActionButtonScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val glassModifier = if (glass) modifier.liquidGlassSurface(CircleShape, containerColor(if (checked) 1f else 0f), null, enabled = true, control = true) else modifier
    MaterialToggleFloatingActionButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = glassModifier,
        containerColor = if (glass) { { Color.Transparent } } else containerColor,
        contentAlignment = contentAlignment,
        containerSize = containerSize,
        containerCornerRadius = containerCornerRadius,
        content = content,
    )
}

@Composable
fun FloatingActionButtonMenuScope.GlassFloatingActionButtonMenuItem(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = contentColorFor(containerColor),
) {
    val glass = LocalLiquidGlassEnabled.current
    val glassModifier = if (glass) modifier.liquidGlassSurface(ShapeFull, containerColor, null, enabled = true, control = true) else modifier
    MaterialFloatingActionButtonMenuItem(
        onClick = onClick,
        text = text,
        icon = icon,
        modifier = glassModifier,
        containerColor = if (glass) Color.Transparent else containerColor,
        contentColor = if (glass) glassControlContentColor(contentColor) else contentColor,
    )
}

@Composable
fun MultiChoiceSegmentedButtonRowScope.GlassSegmentedButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = SegmentedButtonDefaults.colors(),
    border: BorderStroke =
        SegmentedButtonDefaults.borderStroke((if (enabled) { if (checked) colors.activeBorderColor else colors.inactiveBorderColor } else { if (checked) colors.disabledActiveBorderColor else colors.disabledInactiveBorderColor })),
    contentPadding: PaddingValues = SegmentedButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable () -> Unit = { SegmentedButtonDefaults.Icon(checked) },
    label: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, if (checked) colors.activeContainerColor else colors.inactiveContainerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialSegmentedButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shape = shape,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(activeContainerColor = Color.Transparent, inactiveContainerColor = Color.Transparent, disabledActiveContainerColor = Color.Transparent, disabledInactiveContainerColor = Color.Transparent, activeContentColor = glassControlContentColor(colors.activeContentColor), inactiveContentColor = glassControlContentColor(colors.inactiveContentColor)) else colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        icon = icon,
        label = label,
    )
}

@Composable
fun SingleChoiceSegmentedButtonRowScope.GlassSegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = SegmentedButtonDefaults.colors(),
    border: BorderStroke =
        SegmentedButtonDefaults.borderStroke((if (enabled) { if (selected) colors.activeBorderColor else colors.inactiveBorderColor } else { if (selected) colors.disabledActiveBorderColor else colors.disabledInactiveBorderColor })),
    contentPadding: PaddingValues = SegmentedButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable () -> Unit = { SegmentedButtonDefaults.Icon(selected) },
    label: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, if (selected) colors.activeContainerColor else colors.inactiveContainerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialSegmentedButton(
        selected = selected,
        onClick = onClick,
        shape = shape,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors.copy(activeContainerColor = Color.Transparent, inactiveContainerColor = Color.Transparent, disabledActiveContainerColor = Color.Transparent, disabledInactiveContainerColor = Color.Transparent, activeContentColor = glassControlContentColor(colors.activeContentColor), inactiveContentColor = glassControlContentColor(colors.inactiveContentColor)) else colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
        icon = icon,
        label = label,
    )
}

@Composable
fun GlassRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(CircleShape, MaterialTheme.colorScheme.surfaceContainerHigh, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialRadioButton(
        selected = selected,
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        colors = if (glass) colors else colors,
        interactionSource = controlInteraction,
    )
}

@Composable
fun GlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
    horizontalArrangement: Arrangement.Horizontal = FilterChipDefaults.horizontalArrangement(),
    contentPadding: PaddingValues = FilterChipDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, if (selected) colors.selectedContainerColor else colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialFilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = if (glass) glassModifier.heightIn(min = 48.dp) else glassModifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, selectedContainerColor = Color.Transparent, disabledSelectedContainerColor = Color.Transparent, labelColor = glassControlContentColor(colors.labelColor), selectedLabelColor = glassControlContentColor(colors.selectedLabelColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
    )
}

@Composable
fun GlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    shapes: ChipShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: SelectableChipColors = FilterChipDefaults.tonalFilterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
    horizontalArrangement: Arrangement.Horizontal =
        FilterChipDefaults.horizontalArrangement(leadingIcon != null, trailingIcon != null),
    contentPadding: PaddingValues = FilterChipDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, if (selected) colors.selectedContainerColor else colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialFilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        shapes = shapes,
        modifier = if (glass) glassModifier.heightIn(min = 48.dp) else glassModifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, selectedContainerColor = Color.Transparent, disabledSelectedContainerColor = Color.Transparent, labelColor = glassControlContentColor(colors.labelColor), selectedLabelColor = glassControlContentColor(colors.selectedLabelColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
    )
}

@Composable
fun GlassAssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = AssistChipDefaults.shape,
    colors: ChipColors = AssistChipDefaults.assistChipColors(),
    elevation: ChipElevation? = AssistChipDefaults.assistChipElevation(),
    border: BorderStroke? = AssistChipDefaults.assistChipBorder(enabled),
    horizontalArrangement: Arrangement.Horizontal = AssistChipDefaults.horizontalArrangement(),
    contentPadding: PaddingValues = AssistChipDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialAssistChip(
        onClick = onClick,
        label = label,
        modifier = if (glass) glassModifier.heightIn(min = 48.dp) else glassModifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, labelColor = glassControlContentColor(colors.labelColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
    )
}

@Composable
fun GlassInputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    avatar: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = InputChipDefaults.shape,
    colors: SelectableChipColors = InputChipDefaults.inputChipColors(),
    elevation: SelectableChipElevation? = InputChipDefaults.inputChipElevation(),
    border: BorderStroke? = InputChipDefaults.inputChipBorder(enabled, selected),
    horizontalArrangement: Arrangement.Horizontal = InputChipDefaults.horizontalArrangement(),
    contentPadding: PaddingValues =
        InputChipDefaults.contentPadding(avatar != null, leadingIcon != null, trailingIcon != null),
    interactionSource: MutableInteractionSource? = null,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, if (selected) colors.selectedContainerColor else colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialInputChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = if (glass) glassModifier.heightIn(min = 48.dp) else glassModifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        avatar = avatar,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, selectedContainerColor = Color.Transparent, disabledSelectedContainerColor = Color.Transparent, labelColor = glassControlContentColor(colors.labelColor), selectedLabelColor = glassControlContentColor(colors.selectedLabelColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
    )
}

@Composable
fun GlassInputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    shapes: ChipShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    avatar: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: SelectableChipColors = InputChipDefaults.tonalInputChipColors(),
    elevation: SelectableChipElevation? = InputChipDefaults.inputChipElevation(),
    border: BorderStroke? = InputChipDefaults.inputChipBorder(enabled, selected),
    horizontalArrangement: Arrangement.Horizontal =
        InputChipDefaults.horizontalArrangement(
            avatar != null,
            leadingIcon != null,
            trailingIcon != null,
        ),
    contentPadding: PaddingValues =
        InputChipDefaults.contentPadding(avatar != null, leadingIcon != null, trailingIcon != null),
    interactionSource: MutableInteractionSource? = null,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shapes.shape, if (selected) colors.selectedContainerColor else colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialInputChip(
        selected = selected,
        onClick = onClick,
        label = label,
        shapes = shapes,
        modifier = if (glass) glassModifier.heightIn(min = 48.dp) else glassModifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        avatar = avatar,
        trailingIcon = trailingIcon,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, selectedContainerColor = Color.Transparent, disabledSelectedContainerColor = Color.Transparent, labelColor = glassControlContentColor(colors.labelColor), selectedLabelColor = glassControlContentColor(colors.selectedLabelColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
    )
}

@Composable
fun GlassSuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shape: Shape = SuggestionChipDefaults.shape,
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors(),
    elevation: ChipElevation? = SuggestionChipDefaults.suggestionChipElevation(),
    border: BorderStroke? = SuggestionChipDefaults.suggestionChipBorder(enabled),
    horizontalArrangement: Arrangement.Horizontal = SuggestionChipDefaults.horizontalArrangement(),
    contentPadding: PaddingValues = SuggestionChipDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialSuggestionChip(
        onClick = onClick,
        label = label,
        modifier = if (glass) glassModifier.heightIn(min = 48.dp) else glassModifier,
        enabled = enabled,
        icon = icon,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, labelColor = glassControlContentColor(colors.labelColor)) else colors,
        elevation = if (glass) null else elevation,
        border = border,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = controlInteraction,
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    // Structural full-screen surfaces and media canvases retain their own backgrounds.
    val glass = LocalLiquidGlassEnabled.current && shape != RectangleShape && isGlassPanelColor(color)
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, color) else modifier
    MaterialSurface(
        modifier = glassModifier,
        shape = shape,
        color = if (glass) Color.Transparent else color,
        contentColor = if (glass) glassControlContentColor(contentColor) else contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = if (glass) 0.dp else shadowElevation,
        border = border,
        content = content,
    )
}

@Composable
fun GlassSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, color, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialSurface(
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        shape = shape,
        color = if (glass) Color.Transparent else color,
        contentColor = if (glass) glassControlContentColor(contentColor) else contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = if (glass) 0.dp else shadowElevation,
        border = border,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassSurface(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, color, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialSurface(
        selected = selected,
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        shape = shape,
        color = if (glass) Color.Transparent else color,
        contentColor = if (glass) glassControlContentColor(contentColor) else contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = if (glass) 0.dp else shadowElevation,
        border = border,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassSurface(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, color, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialSurface(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = glassModifier,
        enabled = enabled,
        shape = shape,
        color = if (glass) Color.Transparent else color,
        contentColor = if (glass) glassControlContentColor(contentColor) else contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = if (glass) 0.dp else shadowElevation,
        border = border,
        interactionSource = controlInteraction,
        content = content,
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current && isGlassPanelColor(colors.containerColor)
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor) else modifier
    MaterialCard(
        modifier = glassModifier,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        elevation = if (glass) CardDefaults.cardElevation(0.dp) else elevation,
        border = border,
        content = content,
    )
}

@Composable
private fun isGlassPanelColor(color: Color): Boolean {
    val scheme = MaterialTheme.colorScheme
    if (color.alpha == 0f) return false
    return color.copy(alpha = 1f) in listOf(scheme.surface, scheme.surfaceVariant, scheme.surfaceContainer, scheme.surfaceContainerLow,
        scheme.surfaceContainerHigh, scheme.surfaceContainerHighest, scheme.primaryContainer,
        scheme.secondaryContainer, scheme.tertiaryContainer)
}

@Composable
fun GlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val controlInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val glassModifier = if (glass) modifier.liquidGlassSurface(shape, colors.containerColor, controlInteraction, enabled = enabled, control = true) else modifier
    MaterialCard(
        onClick = onClick,
        modifier = glassModifier,
        enabled = enabled,
        shape = shape,
        colors = if (glass) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f), contentColor = glassControlContentColor(colors.contentColor)) else colors,
        elevation = if (glass) CardDefaults.cardElevation(0.dp) else elevation,
        border = border,
        interactionSource = controlInteraction,
        content = content,
    )
}
