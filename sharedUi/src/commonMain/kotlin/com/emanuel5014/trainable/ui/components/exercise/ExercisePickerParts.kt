package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.util.uppercaseUi

/** The handle on top of a picker sheet (and of its filter sheet). */
@Composable
fun PickerDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(OnSurfaceVariant.copy(alpha = 0.4f))
    )
}

/** Gap between the joined rows of one group of a picker. */
val PickerRowGap = 2.dp

/** Room left under a picker's list so its last row can scroll out from under the floating create button. */
val PickerFabClearance = 88.dp

/**
 * The insets a picker sheet keeps for itself: only the top. At the bottom its list runs to the edge of the screen,
 * under the navigation bar, and keeps that room inside the list instead ([pickerBottomInset]); a sheet that kept it
 * as padding left an empty band under the last row.
 */
val PickerSheetInsets: @Composable () -> WindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Top) }

/** What covers the bottom of a picker sheet: the navigation bar, or the keyboard while it is up. */
@Composable
fun pickerBottomInset(): Dp = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()

/**
 * The top of an exercise picker: a [eyebrow] in capitals, a short [title] and, when there is one, a [hint] under it.
 * Smaller than a screen title, so the list starts higher.
 */
@Composable
fun ExercisePickerHeader(eyebrow: String, title: String, hint: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)) {
        Text(
            text = eyebrow.uppercaseUi(),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = ResponsiveSize.labelLargeSize),
            color = Primary,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Room between a picker's filters and the top of its list. */
val PickerListTop = 12.dp

/**
 * The label over a group of rows in a picker ("RECENTI", "TUTTI GLI ESERCIZI"); the [first] one of the list sits right
 * under the filters, the others leave room after the group above.
 */
@Composable
fun ExercisePickerSectionLabel(text: String, modifier: Modifier = Modifier, first: Boolean = false) {
    Text(
        text = text.uppercaseUi(),
        style = MaterialTheme.typography.labelMedium,
        color = OnSurfaceVariant,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier.padding(start = 4.dp, top = if (first) 0.dp else Spacing.medium, bottom = Spacing.small - PickerRowGap)
    )
}

/** The shape of row [index] of [count] in a joined group: big corners at the ends of the group, small between rows. */
fun pickerRowShape(index: Int, count: Int): Shape {
    val corners = SegmentedShapes.corners(index, count, isActive = false)
    return RoundedCornerShape(
        topStart = corners.topDp.dp,
        topEnd = corners.topDp.dp,
        bottomStart = corners.bottomDp.dp,
        bottomEnd = corners.bottomDp.dp
    )
}

/**
 * [items] as one joined group of a picker list, each row drawn by [row] with its place in the group. The [keyPrefix]
 * keeps the keys apart when the same exercise is in two groups (recent and all).
 */
fun <T> LazyListScope.pickerGroup(
    items: List<T>,
    keyPrefix: String,
    key: (T) -> Any,
    row: @Composable (item: T, shape: Shape) -> Unit
) {
    itemsIndexed(items, key = { _, item -> "$keyPrefix-${key(item)}" }) { index, item ->
        row(item, pickerRowShape(index, items.size))
    }
}

/**
 * The shape of an exercise's muscle group, so a list without pictures can still be read at a glance: each built-in
 * category has its own (a heart for cardio), a category of the lifter's own a six-sided cookie.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun categoryShape(category: String): Shape = when (category.lowercase()) {
    "chest" -> MaterialShapes.Cookie9Sided
    "back" -> MaterialShapes.Clover4Leaf
    "legs" -> MaterialShapes.Flower
    "shoulders" -> MaterialShapes.Sunny
    "arms" -> MaterialShapes.Pill
    "core" -> MaterialShapes.Pentagon
    "cardio" -> MaterialShapes.Heart
    else -> MaterialShapes.Cookie6Sided
}.toShape()

/**
 * Where a picker row has no picture: the shape of the exercise's [category] (the stored name), tinted, with the first
 * letter of its [label] (the translated name) inside.
 */
@Composable
fun CategoryAvatar(category: String, label: String, size: Dp = 48.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(categoryShape(category))
            .background(Primary.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.take(1).uppercaseUi(),
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
            fontWeight = FontWeight.Black
        )
    }
}

/** The place of an exercise among the ones picked together, in the spot of its picture: a filled cookie. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SelectionNumberBadge(number: Int, size: Dp = 48.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(Primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = OnPrimary,
            fontWeight = FontWeight.Black
        )
    }
}

/** A picked exercise of a picker that keeps no order: a filled cookie with a check, in the spot of its picture. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SelectedCheckBadge(size: Dp = 48.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(Primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = OnPrimary)
    }
}

/**
 * The button of a picker that creates a new exercise, floating over the end of the list like the one of the exercises
 * page: it shrinks to its icon once the list has scrolled, and gives way to the keyboard.
 */
@Composable
fun PickerCreateFab(
    label: String,
    visible: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            expanded = expanded,
            // Shrunk to its icon, the button still says what it does
            icon = { Icon(Icons.Rounded.Add, contentDescription = if (expanded) null else label) },
            text = { Text(label.uppercaseUi(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold) },
            containerColor = Primary,
            contentColor = OnPrimary,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.semantics { if (expanded) contentDescription = label }
        )
    }
}
