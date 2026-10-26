@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.components.glass.GlassToggleButton
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Seven accessible targets; compact displays scroll rather than overlapping adjacent hit areas. */
@Composable
fun RoutineDayPicker(selectedDays: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val spacing = ButtonGroupDefaults.ConnectedSpaceBetween
        val dayWidth = ((maxWidth - spacing * 6) / 7).coerceAtLeast(48.dp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing)) {
            DayOfWeek.entries.forEachIndexed { index, day ->
                val name = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                GlassToggleButton(
                    checked = day in selectedDays,
                    onCheckedChange = { onToggle(day) },
                    modifier = Modifier.width(dayWidth).heightIn(min = 48.dp).semantics { contentDescription = name },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        6 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
