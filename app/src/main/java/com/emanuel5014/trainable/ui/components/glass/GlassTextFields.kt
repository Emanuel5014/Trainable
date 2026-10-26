@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.emanuel5014.trainable.ui.components.glass

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.LocalLiquidGlassEnabled
import com.emanuel5014.trainable.ui.theme.liquidGlassSurface
import androidx.compose.material3.OutlinedTextField as MaterialOutlinedTextField
import androidx.compose.material3.TextField as MaterialTextField

// Glass belongs to the decoration container, below the floating label and above supporting text.
// BasicTextField retains native selection, IME, password transformation and editing semantics.

@Composable
fun GlassOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = RoundedCornerShape(18.dp),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    if (!LocalLiquidGlassEnabled.current) {
        MaterialOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            shape = shape,
            colors = colors,
        )
        return
    }
    val fieldInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val focused by fieldInteraction.collectIsFocusedAsState()
    val textColor = textStyle.color.takeOrElse {
        when { !enabled -> colors.disabledTextColor; isError -> colors.errorTextColor
            focused -> colors.focusedTextColor; else -> colors.unfocusedTextColor }
    }
    val errorLabel = stringResource(R.string.error)
    val clearColors = colors.copy(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent, errorContainerColor = Color.Transparent)
    val labelPadding = if (label == null) 0.dp else with(LocalDensity.current) { MaterialTheme.typography.bodySmall.lineHeight.toDp() / 2 }

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value, onValueChange = onValueChange, modifier = modifier.semantics(mergeDescendants = true) { if (isError) error(errorLabel) }.padding(top = labelPadding).defaultMinSize(minWidth = OutlinedTextFieldDefaults.MinWidth, minHeight = OutlinedTextFieldDefaults.MinHeight),
            enabled = enabled, readOnly = readOnly, textStyle = textStyle.merge(TextStyle(color = textColor)),
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions, singleLine = singleLine, maxLines = maxLines, minLines = minLines,
            interactionSource = fieldInteraction,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value, innerTextField = innerTextField, visualTransformation = visualTransformation,
                    enabled = enabled, singleLine = singleLine, isError = isError, interactionSource = fieldInteraction,
                    label = label, placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
                    prefix = prefix, suffix = suffix, supportingText = supportingText, colors = clearColors,
                    container = {
                        Box(Modifier.fillMaxSize().liquidGlassSurface(shape, MaterialTheme.colorScheme.surfaceContainerHigh,
                            fieldInteraction, enabled = enabled, control = true, quiet = true)) {
                            OutlinedTextFieldDefaults.Container(enabled = enabled, isError = isError, interactionSource = fieldInteraction,
                                modifier = Modifier.fillMaxSize(), colors = clearColors, shape = shape,
                                focusedBorderThickness = 1.5.dp, unfocusedBorderThickness = .75.dp)
                        }
                    }
                )
            }
        )
    }
}

@Composable
fun GlassOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = RoundedCornerShape(18.dp),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    if (!LocalLiquidGlassEnabled.current) {
        MaterialOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            shape = shape,
            colors = colors,
        )
        return
    }
    val fieldInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val focused by fieldInteraction.collectIsFocusedAsState()
    val textColor = textStyle.color.takeOrElse {
        when { !enabled -> colors.disabledTextColor; isError -> colors.errorTextColor
            focused -> colors.focusedTextColor; else -> colors.unfocusedTextColor }
    }
    val errorLabel = stringResource(R.string.error)
    val clearColors = colors.copy(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent, errorContainerColor = Color.Transparent)
    val labelPadding = if (label == null) 0.dp else with(LocalDensity.current) { MaterialTheme.typography.bodySmall.lineHeight.toDp() / 2 }

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value, onValueChange = onValueChange, modifier = modifier.semantics(mergeDescendants = true) { if (isError) error(errorLabel) }.padding(top = labelPadding).defaultMinSize(minWidth = OutlinedTextFieldDefaults.MinWidth, minHeight = OutlinedTextFieldDefaults.MinHeight),
            enabled = enabled, readOnly = readOnly, textStyle = textStyle.merge(TextStyle(color = textColor)),
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions, singleLine = singleLine, maxLines = maxLines, minLines = minLines,
            interactionSource = fieldInteraction,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value.text, innerTextField = innerTextField, visualTransformation = visualTransformation,
                    enabled = enabled, singleLine = singleLine, isError = isError, interactionSource = fieldInteraction,
                    label = label, placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
                    prefix = prefix, suffix = suffix, supportingText = supportingText, colors = clearColors,
                    container = {
                        Box(Modifier.fillMaxSize().liquidGlassSurface(shape, MaterialTheme.colorScheme.surfaceContainerHigh,
                            fieldInteraction, enabled = enabled, control = true, quiet = true)) {
                            OutlinedTextFieldDefaults.Container(enabled = enabled, isError = isError, interactionSource = fieldInteraction,
                                modifier = Modifier.fillMaxSize(), colors = clearColors, shape = shape,
                                focusedBorderThickness = 1.5.dp, unfocusedBorderThickness = .75.dp)
                        }
                    }
                )
            }
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = RoundedCornerShape(18.dp),
    colors: TextFieldColors = TextFieldDefaults.colors(),
) {
    if (!LocalLiquidGlassEnabled.current) {
        MaterialTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            shape = shape,
            colors = colors,
        )
        return
    }
    val fieldInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val focused by fieldInteraction.collectIsFocusedAsState()
    val textColor = textStyle.color.takeOrElse {
        when { !enabled -> colors.disabledTextColor; isError -> colors.errorTextColor
            focused -> colors.focusedTextColor; else -> colors.unfocusedTextColor }
    }
    val errorLabel = stringResource(R.string.error)
    val clearColors = colors.copy(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent, errorContainerColor = Color.Transparent)
    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value, onValueChange = onValueChange, modifier = modifier.semantics(mergeDescendants = true) { if (isError) error(errorLabel) }.defaultMinSize(minWidth = TextFieldDefaults.MinWidth, minHeight = TextFieldDefaults.MinHeight),
            enabled = enabled, readOnly = readOnly, textStyle = textStyle.merge(TextStyle(color = textColor)),
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions, singleLine = singleLine, maxLines = maxLines, minLines = minLines,
            interactionSource = fieldInteraction,
            decorationBox = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = value, innerTextField = innerTextField, visualTransformation = visualTransformation,
                    enabled = enabled, singleLine = singleLine, isError = isError, interactionSource = fieldInteraction,
                    label = label, placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
                    prefix = prefix, suffix = suffix, supportingText = supportingText, colors = clearColors,
                    container = {
                        Box(Modifier.fillMaxSize().liquidGlassSurface(shape, MaterialTheme.colorScheme.surfaceContainerHigh,
                            fieldInteraction, enabled = enabled, control = true, quiet = true)) {
                            TextFieldDefaults.Container(enabled = enabled, isError = isError, interactionSource = fieldInteraction,
                                modifier = Modifier.fillMaxSize(), colors = clearColors, shape = shape,
                                focusedIndicatorLineThickness = 1.5.dp, unfocusedIndicatorLineThickness = .75.dp)
                        }
                    }
                )
            }
        )
    }
}

@Composable
fun GlassTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = RoundedCornerShape(18.dp),
    colors: TextFieldColors = TextFieldDefaults.colors(),
) {
    if (!LocalLiquidGlassEnabled.current) {
        MaterialTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            shape = shape,
            colors = colors,
        )
        return
    }
    val fieldInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val focused by fieldInteraction.collectIsFocusedAsState()
    val textColor = textStyle.color.takeOrElse {
        when { !enabled -> colors.disabledTextColor; isError -> colors.errorTextColor
            focused -> colors.focusedTextColor; else -> colors.unfocusedTextColor }
    }
    val errorLabel = stringResource(R.string.error)
    val clearColors = colors.copy(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent, errorContainerColor = Color.Transparent)
    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value, onValueChange = onValueChange, modifier = modifier.semantics(mergeDescendants = true) { if (isError) error(errorLabel) }.defaultMinSize(minWidth = TextFieldDefaults.MinWidth, minHeight = TextFieldDefaults.MinHeight),
            enabled = enabled, readOnly = readOnly, textStyle = textStyle.merge(TextStyle(color = textColor)),
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions, singleLine = singleLine, maxLines = maxLines, minLines = minLines,
            interactionSource = fieldInteraction,
            decorationBox = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = value.text, innerTextField = innerTextField, visualTransformation = visualTransformation,
                    enabled = enabled, singleLine = singleLine, isError = isError, interactionSource = fieldInteraction,
                    label = label, placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
                    prefix = prefix, suffix = suffix, supportingText = supportingText, colors = clearColors,
                    container = {
                        Box(Modifier.fillMaxSize().liquidGlassSurface(shape, MaterialTheme.colorScheme.surfaceContainerHigh,
                            fieldInteraction, enabled = enabled, control = true, quiet = true)) {
                            TextFieldDefaults.Container(enabled = enabled, isError = isError, interactionSource = fieldInteraction,
                                modifier = Modifier.fillMaxSize(), colors = clearColors, shape = shape,
                                focusedIndicatorLineThickness = 1.5.dp, unfocusedIndicatorLineThickness = .75.dp)
                        }
                    }
                )
            }
        )
    }
}
