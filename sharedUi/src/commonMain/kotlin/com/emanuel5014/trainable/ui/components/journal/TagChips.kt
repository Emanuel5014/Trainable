package com.emanuel5014.trainable.ui.components.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.domain.journal.TagContrast
import com.emanuel5014.trainable.domain.journal.TagPalette
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSecondaryContainer
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OnTertiaryContainer
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary

/**
 * The colour of a tag: the one the lifter picked freely ([customColor], lightened or darkened only as far as it needs
 * to read on a card) or else the palette slot, taken from the theme so dynamic colour and the dark theme keep working.
 */
@Composable
fun tagColor(colorIndex: Int, customColor: Int? = null): Color {
    if (customColor != null) return Color(TagContrast.readableOn(customColor, SurfaceContainerHigh.toArgb()))
    return when (colorIndex.coerceIn(0, TagPalette.SIZE - 1)) {
        0 -> Primary
        1 -> Tertiary
        2 -> Error
        3 -> OnSecondaryContainer
        4 -> OnSurfaceVariant
        else -> OnTertiaryContainer
    }
}

/** A tag that can be switched on and off: a dot of its colour while off, a check mark and a tinted container while on. */
@Composable
fun TagChip(
    name: String,
    colorIndex: Int,
    customColor: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = tagColor(colorIndex, customColor)
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp)) },
        leadingIcon = {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
            } else {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.18f),
            selectedLabelColor = color,
            selectedLeadingIconColor = color
        )
    )
}

/** A tag as a small read-only pill, for lists and cards. */
@Composable
fun TagBadge(name: String, colorIndex: Int, customColor: Int?, modifier: Modifier = Modifier) {
    val color = tagColor(colorIndex, customColor)
    Text(
        text = name,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .widthIn(max = 140.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}
