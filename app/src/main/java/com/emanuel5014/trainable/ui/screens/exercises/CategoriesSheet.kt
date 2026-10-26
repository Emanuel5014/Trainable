package com.emanuel5014.trainable.ui.screens.exercises
import com.emanuel5014.trainable.ui.components.glass.glassPanelBackground

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import com.emanuel5014.trainable.ui.components.glass.GlassAlertDialog as AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.emanuel5014.trainable.ui.components.glass.GlassIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import com.emanuel5014.trainable.ui.components.glass.GlassModalBottomSheet as ModalBottomSheet
import androidx.compose.material3.Text
import com.emanuel5014.trainable.ui.components.glass.GlassTextButton as TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.CustomCategoryEntity
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/**
 * The custom categories: add, rename, delete. The rules are those of the old customization screen (a blank or an already
 * used name is refused with a message); the labels of the exercises that use a category are not touched by a rename or a
 * delete, as before.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesSheet(
    categories: List<CustomCategoryEntity>,
    onAdd: (String) -> Unit,
    onEdit: (CustomCategoryEntity) -> Unit,
    onDelete: (CustomCategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var toEdit by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    var toDelete by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    val existingNames = categories.map { it.name }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Category, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        stringResource(R.string.custom_categories),
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        stringResource(R.string.custom_categories_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    CategoryRow(
                        name = category.name,
                        onEdit = { toEdit = category },
                        onDelete = { toDelete = category }
                    )
                }
            }

            GymButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth(), containerColor = SurfaceContainerHigh, contentColor = OnSurface) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.add_category), fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    if (showAdd) {
        CategoryNameDialog(
            title = stringResource(R.string.add_category_title),
            initialName = "",
            takenNames = existingNames,
            onConfirm = {
                onAdd(it)
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }

    toEdit?.let { category ->
        CategoryNameDialog(
            title = stringResource(R.string.edit_category_title),
            initialName = category.name,
            // Keeping its own name is fine
            takenNames = existingNames - category.name,
            onConfirm = {
                onEdit(category.copy(name = it))
                toEdit = null
            },
            onDismiss = { toEdit = null }
        )
    }

    toDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = SurfaceContainerHigh,
            shape = Shapes.extraLarge,
            title = { Text(stringResource(R.string.delete_category_title), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
            text = { Text(stringResource(R.string.delete_category_message, category.name), color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(category)
                    toDelete = null
                }) {
                    Text(stringResource(R.string.delete).uppercase(), color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = Primary)
                }
            }
        )
    }
}

/**
 * One category: the name and the edit and delete buttons. Beside the name when it keeps enough room, below it (buttons
 * on the right) on a small screen or with a large font, where the two 48dp buttons would squeeze it to a few letters a line.
 */
@Composable
internal fun CategoryRow(name: String, onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.medium)
            .glassPanelBackground(SurfaceContainerHigh, Shapes.medium)
    ) {
        val stacked = maxWidth - CategoryButtonsWidth - CategoryRowInsets < CategoryNameMinWidth
        val nameText = @Composable { modifier: Modifier ->
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier
            )
        }
        val buttons = @Composable {
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_category_title), tint = Primary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete), tint = Error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
        if (stacked) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
                nameText(Modifier.padding(end = 12.dp))
                Row(modifier = Modifier.align(Alignment.End)) { buttons() }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                nameText(Modifier.weight(1f))
                buttons()
            }
        }
    }
}

private val CategoryButtonsWidth = 96.dp
private val CategoryRowInsets = 28.dp
private val CategoryNameMinWidth = 140.dp

/** Asks for a category name; a blank or already used one is refused with the message of the old screen. */
@Composable
private fun CategoryNameDialog(
    title: String,
    initialName: String,
    takenNames: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val context = LocalContext.current
    val alreadyExists = stringResource(R.string.category_already_exists)
    val nameEmpty = stringResource(R.string.category_name_empty)

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        shape = Shapes.extraLarge,
        title = { Text(title, fontWeight = FontWeight.ExtraBold, color = OnSurface) },
        text = {
            GymInputField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.category_name),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            GymButton(
                onClick = {
                    val trimmed = name.trim()
                    when {
                        trimmed.isEmpty() -> Toast.makeText(context, nameEmpty, Toast.LENGTH_SHORT).show()
                        trimmed in takenNames -> Toast.makeText(context, alreadyExists, Toast.LENGTH_SHORT).show()
                        else -> onConfirm(trimmed)
                    }
                },
                containerColor = Primary,
                contentColor = OnPrimary
            ) {
                Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}
