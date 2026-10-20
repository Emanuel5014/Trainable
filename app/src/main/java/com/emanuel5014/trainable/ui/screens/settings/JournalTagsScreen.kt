package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.relation.TagWithUsage
import com.emanuel5014.trainable.data.repository.RenameResult
import com.emanuel5014.trainable.domain.journal.TagName
import com.emanuel5014.trainable.domain.journal.TagPalette
import com.emanuel5014.trainable.ui.components.EmptyState
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.journal.TagColorPickerDialog
import com.emanuel5014.trainable.ui.components.journal.tagColor
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.launch

/** What the screen is asking about right now; at most one dialog is open. */
private sealed interface TagDialog {
    data object Create : TagDialog
    data class Rename(val tag: TagWithUsage, val invalid: Boolean = false) : TagDialog
    data class RenameConflict(val tag: TagWithUsage, val existingId: Int, val existingName: String) : TagDialog
    data class Color(val tag: TagWithUsage) : TagDialog
    data class CustomColor(val tag: TagWithUsage) : TagDialog
    data class PickMergeTarget(val tag: TagWithUsage) : TagDialog
    data class ConfirmMerge(val from: TagWithUsage, val into: TagWithUsage) : TagDialog
    data class Delete(val tag: TagWithUsage) : TagDialog
}

/**
 * Settings -> Workout -> Journal -> Manage tags: every tag with how many workouts use it, and what can be done to
 * it: rename (a name that is already another tag's offers to merge), recolour, merge into another tag, delete.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JournalTagsScreen(
    onNavigateBack: () -> Unit,
    viewModel: JournalTagsViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsState()
    var dialog by remember { mutableStateOf<TagDialog?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.journal_manage_tags),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = OnSurface
                )
            )
        }
    ) { paddingValues ->
        if (tags.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Sell,
                title = stringResource(R.string.journal_tags),
                description = stringResource(R.string.journal_tags_empty),
                modifier = Modifier.padding(paddingValues),
                action = {
                    GymButton(onClick = { dialog = TagDialog.Create }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.journal_new_tag).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "new") {
                    GymButton(onClick = { dialog = TagDialog.Create }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.journal_new_tag).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                }
                items(tags, key = { it.id }) { tag ->
                    TagRow(
                        tag = tag,
                        onRename = { dialog = TagDialog.Rename(tag) },
                        onColor = { dialog = TagDialog.Color(tag) },
                        onMerge = { dialog = TagDialog.PickMergeTarget(tag) },
                        onDelete = { dialog = TagDialog.Delete(tag) }
                    )
                }
            }
        }
    }

    when (val open = dialog) {
        null -> Unit

        TagDialog.Create -> NameDialog(
            title = stringResource(R.string.journal_new_tag),
            initial = "",
            invalid = false,
            onConfirm = { name ->
                viewModel.create(name)
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        is TagDialog.Rename -> NameDialog(
            title = stringResource(R.string.journal_tag_rename_title),
            initial = open.tag.name,
            invalid = open.invalid,
            onConfirm = { name ->
                scope.launch {
                    dialog = when (val result = viewModel.rename(open.tag.id, name)) {
                        RenameResult.Renamed -> null
                        RenameResult.Invalid -> open.copy(invalid = true)
                        is RenameResult.Conflict -> TagDialog.RenameConflict(open.tag, result.existing.id, result.existing.name)
                    }
                }
            },
            onDismiss = { dialog = null }
        )

        is TagDialog.RenameConflict -> ConfirmDialog(
            title = stringResource(R.string.journal_tag_merge_title),
            message = stringResource(R.string.journal_tag_exists, open.existingName),
            confirmLabel = stringResource(R.string.journal_merge_action),
            onConfirm = {
                viewModel.merge(open.tag.id, open.existingId)
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        is TagDialog.Color -> AlertDialog(
            onDismissRequest = { dialog = null },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(stringResource(R.string.journal_tag_color), fontWeight = FontWeight.ExtraBold, color = OnSurface)
            },
            text = {
                // Rows of four: seven swatches in one row do not fit in a dialog on a narrow screen
                FlowRow(
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (slot in 0 until TagPalette.SIZE) {
                        val color = tagColor(slot)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    viewModel.setColor(open.tag.id, slot)
                                    dialog = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (open.tag.customColor == null && slot == open.tag.colorIndex) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Surface, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                    CustomColorSwatch(
                        customColor = open.tag.customColor,
                        onClick = { dialog = TagDialog.CustomColor(open.tag) }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { dialog = null }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = Primary)
                }
            }
        )

        is TagDialog.CustomColor -> TagColorPickerDialog(
            initialColor = tagColor(open.tag.colorIndex, open.tag.customColor),
            onDismiss = { dialog = null },
            onApply = { argb ->
                viewModel.setCustomColor(open.tag.id, argb)
                dialog = null
            }
        )

        is TagDialog.PickMergeTarget -> AlertDialog(
            onDismissRequest = { dialog = null },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(stringResource(R.string.journal_tag_merge), fontWeight = FontWeight.ExtraBold, color = OnSurface)
            },
            text = {
                val others = tags.filter { it.id != open.tag.id }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(others, key = { it.id }) { target ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { dialog = TagDialog.ConfirmMerge(open.tag, target) }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ColorDot(target.colorIndex, target.customColor)
                            Text(target.name, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { dialog = null }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = Primary)
                }
            }
        )

        is TagDialog.ConfirmMerge -> ConfirmDialog(
            title = stringResource(R.string.journal_tag_merge_title),
            message = stringResource(R.string.journal_tag_merge_message, open.from.name, open.into.name),
            confirmLabel = stringResource(R.string.journal_merge_action),
            onConfirm = {
                viewModel.merge(open.from.id, open.into.id)
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        is TagDialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.journal_tag_delete_title),
            message = stringResource(R.string.journal_tag_delete_message, open.tag.uses),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                viewModel.delete(open.tag.id)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
    }
}

@Composable
private fun ColorDot(colorIndex: Int, customColor: Int?, size: Int = 14) {
    Box(modifier = Modifier.size(size.dp).clip(CircleShape).background(tagColor(colorIndex, customColor)))
}

/** The last swatch of the colour dialog: the tag's own colour when it has one (checked), else a rainbow that opens the picker. */
@Composable
private fun CustomColorSwatch(customColor: Int?, onClick: () -> Unit) {
    val label = stringResource(R.string.custom_color)
    val fill = if (customColor != null) {
        Modifier.background(Color(customColor))
    } else {
        Modifier.background(
            Brush.sweepGradient(
                listOf(Color(0xFFFF4D4D), Color(0xFFFFC233), Color(0xFF5BD66B), Color(0xFF33B5FF), Color(0xFFB35CFF), Color(0xFFFF4D4D))
            )
        )
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(fill)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (customColor != null) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Surface, modifier = Modifier.size(22.dp))
        } else {
            Icon(Icons.Rounded.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun TagRow(
    tag: TagWithUsage,
    onRename: () -> Unit,
    onColor: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerHigh)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ColorDot(tag.colorIndex, tag.customColor, size = 18)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pluralStringResource(R.plurals.journal_tag_uses, tag.uses, tag.uses),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = tag.name,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.semantics { contentDescription = tag.name }
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.journal_tag_rename)) },
                    onClick = { menuOpen = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.journal_tag_color)) },
                    onClick = { menuOpen = false; onColor() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.journal_tag_merge)) },
                    onClick = { menuOpen = false; onMerge() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = Error) },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

/** A name to type: for a new tag or a rename. [invalid] marks a name that is empty once cleaned. */
@Composable
private fun NameDialog(
    title: String,
    initial: String,
    invalid: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = { Text(title, fontWeight = FontWeight.ExtraBold, color = OnSurface) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.replace("|", "").take(TagName.MAX_LENGTH) },
                label = { Text(stringResource(R.string.journal_tag_name)) },
                singleLine = true,
                isError = invalid,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save).uppercase(), color = Primary, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = { Text(title, fontWeight = FontWeight.ExtraBold, color = OnSurface) },
        text = { Text(message, color = OnSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel.uppercase(),
                    color = if (destructive) Error else Primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}
