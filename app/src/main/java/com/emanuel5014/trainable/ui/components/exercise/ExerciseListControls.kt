package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary

/** The search box of every exercise list: a magnifier, and a cross that clears the text once there is some. */
@Composable
fun ExerciseSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    GymInputField(
        value = query,
        onValueChange = onQueryChange,
        label = stringResource(R.string.search_exercises),
        modifier = modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = OnSurfaceVariant) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close), tint = OnSurfaceVariant)
                }
            }
        } else null
    )
}

/** One button of an [ExerciseFilterBar]: it is [checked] when its filter is on, and [onClick] asks to switch it. */
class ExerciseFilterOption(val label: String, val checked: Boolean, val onClick: () -> Unit)

/**
 * The filters of an exercise list as a connected group of toggle buttons that scrolls sideways when the labels do not fit.
 * The bar only draws them: the page keeps one on at a time, the pickers let a category combine with the favorites.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExerciseFilterBar(options: List<ExerciseFilterOption>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = option.checked,
                onCheckedChange = { option.onClick() },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(option.label, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/** What a list of exercises says when the search and the filters leave nothing. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExerciseEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(Primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = Primary, modifier = Modifier.size(32.dp))
        }
        Text(
            text = stringResource(R.string.no_exercises_found),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
