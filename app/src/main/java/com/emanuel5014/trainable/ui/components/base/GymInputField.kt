package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import com.emanuel5014.trainable.ui.components.glass.GlassOutlinedTextField as OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.LocalLiquidGlassEnabled
import com.emanuel5014.trainable.ui.theme.LocalGlassIntensity

@Composable
fun GymInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    containerColor: Color = Color.Transparent,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Start
) {
    val glass = LocalLiquidGlassEnabled.current
    val amount = LocalGlassIntensity.current
    val fieldFill = if (glass && containerColor == Color.Transparent) {
        lerp(MaterialTheme.colorScheme.surfaceContainerHigh, Primary, .045f)
            .copy(alpha = .26f - .10f * amount)
    } else containerColor
    val outline = if (glass) MaterialTheme.colorScheme.outline.copy(alpha = .45f)
        else OnSurfaceVariant.copy(alpha = .5f)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        readOnly = readOnly,
        enabled = enabled,
        isError = isError,
        shape = if (glass) androidx.compose.foundation.shape.RoundedCornerShape(18.dp) else Shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface,
            disabledTextColor = OnSurface,
            focusedBorderColor = Primary,
            unfocusedBorderColor = outline,
            disabledBorderColor = outline,
            focusedLabelColor = Primary,
            unfocusedLabelColor = OnSurfaceVariant,
            disabledLabelColor = OnSurfaceVariant,
            cursorColor = Primary,
            focusedContainerColor = fieldFill,
            unfocusedContainerColor = fieldFill,
            disabledContainerColor = fieldFill,
            disabledLeadingIconColor = OnSurfaceVariant,
            disabledTrailingIconColor = OnSurfaceVariant
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = textAlign)
    )
}
