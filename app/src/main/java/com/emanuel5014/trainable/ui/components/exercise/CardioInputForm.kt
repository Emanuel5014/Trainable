package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

@Composable
fun CardioInputForm(
    categoria: String,
    onCategoriaChange: (String) -> Unit,
    distanza: String,
    onDistanzaChange: (String) -> Unit,
    durataOre: String,
    onDurataOreChange: (String) -> Unit,
    durataMinuti: String,
    onDurataMinutiChange: (String) -> Unit,
    durataSecondi: String,
    onDurataSecondiChange: (String) -> Unit,
    showCategory: Boolean = true,
) {
    val categories = if (showCategory) listOf(
        CardioCategory(stringResource(R.string.cardio_run), Icons.AutoMirrored.Rounded.DirectionsRun),
        CardioCategory(stringResource(R.string.cardio_bike), Icons.AutoMirrored.Rounded.DirectionsBike),
        CardioCategory(stringResource(R.string.cardio_walk), Icons.AutoMirrored.Rounded.DirectionsWalk),
    ) else emptyList()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Category Selection
        if (showCategory) {
            Column {
                Text(
                    text = stringResource(R.string.category).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = categoria.equals(cat.name, ignoreCase = true)
                        CardioCategoryChip(
                            category = cat,
                            isSelected = isSelected,
                            onClick = { onCategoriaChange(cat.name) }
                        )
                    }
                    item {
                        val isCustom = categories.none { it.name.equals(categoria, ignoreCase = true) } && categoria.isNotBlank()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCustom) Primary.copy(alpha = 0.1f) else SurfaceContainerHigh)
                                .border(
                                    width = 1.dp,
                                    color = if (isCustom) Primary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onCategoriaChange("") }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreHoriz,
                                    contentDescription = null,
                                    tint = if (isCustom) Primary else OnSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCustom) categoria else stringResource(R.string.cardio_other),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCustom) Primary else OnSurface,
                                    fontWeight = if (isCustom) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                if (categoria.isBlank() || categories.none { it.name.equals(categoria, ignoreCase = true) }) {
                    Spacer(modifier = Modifier.height(8.dp))
                    GymInputField(
                        value = categoria,
                        onValueChange = onCategoriaChange,
                        label = stringResource(R.string.activity_name),
                        placeholder = stringResource(R.string.activity_name_placeholder)
                    )
                }
            }
        }

        // Distance Input
        Column {
            Text(
                text = (stringResource(R.string.distance) + " (KM)").uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IncrementButton(
                    icon = Icons.Rounded.Remove,
                    onClick = {
                        val current = distanza.toFloatOrNull() ?: 0f
                        if (current > 0) onDistanzaChange((current - 0.5f).coerceAtLeast(0f).toString())
                    }
                )
                
                OutlinedTextField(
                    value = distanza,
                    onValueChange = { onDistanzaChange(it.replace(",", ".")) },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Black,
                        color = OnSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = Shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceContainerHigh,
                        unfocusedContainerColor = SurfaceContainerHigh,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )

                IncrementButton(
                    icon = Icons.Rounded.Add,
                    onClick = {
                        val current = distanza.toFloatOrNull() ?: 0f
                        onDistanzaChange((current + 0.5f).toString())
                    }
                )
            }
        }

        // Duration Input
        Column {
            Text(
                text = (stringResource(R.string.duration) + " (HH:MM:SS)").uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DurationUnitField(
                    value = durataOre,
                    onValueChange = onDurataOreChange,
                    label = "H",
                    modifier = Modifier.weight(1f)
                )
                Text(":", style = MaterialTheme.typography.headlineMedium, color = OnSurfaceVariant)
                DurationUnitField(
                    value = durataMinuti,
                    onValueChange = onDurataMinutiChange,
                    label = "M",
                    modifier = Modifier.weight(1f)
                )
                Text(":", style = MaterialTheme.typography.headlineMedium, color = OnSurfaceVariant)
                DurationUnitField(
                    value = durataSecondi,
                    onValueChange = onDurataSecondiChange,
                    label = "S",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CardioCategoryChip(
    category: CardioCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Primary.copy(alpha = 0.1f) else SurfaceContainerHigh)
            .border(
                width = 1.dp,
                color = if (isSelected) Primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = if (isSelected) Primary else OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Primary else OnSurface,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DurationUnitField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 2) onValueChange(it.filter { c -> c.isDigit() }) },
        modifier = modifier,
        textStyle = MaterialTheme.typography.titleLarge.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold,
            color = OnSurface
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceContainerHigh,
            unfocusedContainerColor = SurfaceContainerHigh,
            focusedBorderColor = Primary,
            unfocusedBorderColor = Color.Transparent
        ),
        singleLine = true,
        placeholder = { 
            Text(
                "00", 
                modifier = Modifier.fillMaxWidth(), 
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                color = OnSurfaceVariant.copy(alpha = 0.3f)
            ) 
        }
    )
}

@Composable
private fun IncrementButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(SurfaceContainerHigh)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = OnSurface)
    }
}

@Composable
fun AddCardioDialog(
    onDismiss: () -> Unit,
    onConfirm: (categoria: String, distanza: Float, durataSecondi: Int) -> Unit,
    initialCategoria: String? = null
) {
    var categoria by remember { mutableStateOf(initialCategoria ?: "") }
    var distanza by remember { mutableStateOf("") }
    var durataOre by remember { mutableStateOf("") }
    var durataMinuti by remember { mutableStateOf("") }
    var durataSecondi by remember { mutableStateOf("") }

    val isValid = categoria.isNotBlank() && (distanza.isNotBlank() || durataMinuti.isNotBlank() || durataOre.isNotBlank())

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                stringResource(R.string.add_cardio),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            ) 
        },
        text = {
            CardioInputForm(
                categoria = categoria,
                onCategoriaChange = { categoria = it },
                distanza = distanza,
                onDistanzaChange = { distanza = it },
                durataOre = durataOre,
                onDurataOreChange = { durataOre = it },
                durataMinuti = durataMinuti,
                onDurataMinutiChange = { durataMinuti = it },
                durataSecondi = durataSecondi,
                onDurataSecondiChange = { durataSecondi = it }
            )
        },
        confirmButton = {
            com.emanuel5014.trainable.ui.components.GymButton(
                onClick = {
                    val dist = distanza.toFloatOrNull() ?: 0f
                    val h = durataOre.toIntOrNull() ?: 0
                    val m = durataMinuti.toIntOrNull() ?: 0
                    val s = durataSecondi.toIntOrNull() ?: 0
                    val dur = h * 3600 + m * 60 + s
                    onConfirm(categoria, dist, dur)
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            com.emanuel5014.trainable.ui.components.GymButton(
                onClick = onDismiss,
                containerColor = Color.Transparent,
                contentColor = OnSurfaceVariant
            ) {
                Text(stringResource(R.string.cancel).uppercase())
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant,
        shape = Shapes.extraLarge
    )
}

private data class CardioCategory(
    val name: String,
    val icon: ImageVector
)
