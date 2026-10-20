package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary

/** The three ways RPE can be asked, as cards to pick from: icon, name, what it is and the values it offers. */
@Composable
internal fun RpeScaleOptions(selected: RpeScale, onSelect: (RpeScale) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RpeScale.entries.forEach { scale ->
            val (icon, title, description, example) = when (scale) {
                RpeScale.BASIC -> RpeScaleTexts(
                    Icons.Rounded.Spa,
                    stringResource(R.string.rpe_scale_basic),
                    stringResource(R.string.rpe_scale_basic_desc),
                    stringResource(R.string.rpe_scale_basic_example)
                )
                RpeScale.STANDARD -> RpeScaleTexts(
                    Icons.Rounded.Speed,
                    stringResource(R.string.rpe_scale_standard),
                    stringResource(R.string.rpe_scale_standard_desc),
                    stringResource(R.string.rpe_scale_standard_example)
                )
                RpeScale.ADVANCED -> RpeScaleTexts(
                    Icons.Rounded.Tune,
                    stringResource(R.string.rpe_scale_advanced),
                    stringResource(R.string.rpe_scale_advanced_desc),
                    stringResource(R.string.rpe_scale_advanced_example)
                )
            }
            RpeScaleCard(
                icon = icon,
                title = title,
                description = description,
                example = example,
                selected = scale == selected,
                onClick = { onSelect(scale) }
            )
        }
    }
}

private data class RpeScaleTexts(val icon: ImageVector, val title: String, val description: String, val example: String)

@Composable
private fun RpeScaleCard(
    icon: ImageVector,
    title: String,
    description: String,
    example: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Primary.copy(alpha = 0.08f) else OnSurface.copy(alpha = 0.04f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Primary else OnSurface.copy(alpha = 0.12f),
                shape = shape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            Text(
                text = example,
                style = MaterialTheme.typography.labelLarge,
                color = OnSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (selected) {
            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
        }
    }
}
