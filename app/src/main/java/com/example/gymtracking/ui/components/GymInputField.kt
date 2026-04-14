package com.example.gymtracking.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.gymtracking.ui.theme.OnSurface
import com.example.gymtracking.ui.theme.OnSurfaceVariant
import com.example.gymtracking.ui.theme.Primary
import com.example.gymtracking.ui.theme.Shapes

@Composable
fun GymInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        readOnly = readOnly,
        trailingIcon = trailingIcon,
        shape = Shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface,
            focusedBorderColor = Primary,
            unfocusedBorderColor = OnSurfaceVariant.copy(alpha = 0.5f),
            focusedLabelColor = Primary,
            unfocusedLabelColor = OnSurfaceVariant,
            cursorColor = Primary
        ),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}
