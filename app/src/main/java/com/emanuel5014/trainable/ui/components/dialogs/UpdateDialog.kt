package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import android.text.format.Formatter
import com.emanuel5014.trainable.util.UpdateError
import com.emanuel5014.trainable.util.UpdateFiles
import com.emanuel5014.trainable.util.UpdatePhase

/**
 * The update offer and everything after it: download progress, check, Android's installer, errors. One dialog for
 * every phase of [UpdateManager]; closing it while downloading only hides it (the download goes on in the
 * notification).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateDialog(
    phase: UpdatePhase,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val release = when (phase) {
        UpdatePhase.Idle -> return
        is UpdatePhase.Available -> phase.release
        is UpdatePhase.Downloading -> phase.release
        is UpdatePhase.Verifying -> phase.release
        is UpdatePhase.ReadyToInstall -> phase.release
        is UpdatePhase.Installing -> phase.release
        is UpdatePhase.Failed -> phase.release ?: return
    }
    val context = LocalContext.current
    val changelogElements = remember(release.body) {
        parseChangelog(release.body)
    }
    val apkSize = UpdateFiles.apkAsset(release)?.size ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (phase is UpdatePhase.Failed) Icons.Rounded.ErrorOutline else Icons.Rounded.NewReleases,
                contentDescription = null,
                tint = if (phase is UpdatePhase.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        },
        title = {
            Text(
                text = stringResource(if (phase is UpdatePhase.Failed) R.string.update_failed_title else R.string.update_available),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.version, release.tagName) +
                        if (apkSize > 0) " · " + Formatter.formatShortFileSize(context, apkSize) else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (release.prerelease) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.update_is_beta),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (phase is UpdatePhase.Failed) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = updateErrorText(phase.error, phase.detail),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.changelog),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable container bounded so the progress and the buttons stay on screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(end = 4.dp) // Spacing for scrollbar or visual edge
                ) {
                    if (changelogElements.isEmpty()) {
                        Text(
                            text = release.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        changelogElements.forEach { element ->
                            when (element) {
                                is ChangelogElement.Header -> {
                                    HeaderRow(title = element.title)
                                }
                                is ChangelogElement.ChangeItem -> {
                                    ChangeItemRow(item = element)
                                }
                                is ChangelogElement.BulletPoint -> {
                                    BulletPointRow(content = element.content)
                                }
                                is ChangelogElement.Text -> {
                                    TextRow(content = element.content)
                                }
                            }
                        }
                    }
                }

                when (phase) {
                    is UpdatePhase.Downloading, is UpdatePhase.Verifying -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val fraction = (phase as? UpdatePhase.Downloading)?.fraction
                            if (fraction != null) {
                                LinearWavyProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            Text(
                                text = when (phase) {
                                    is UpdatePhase.Downloading -> if (phase.total > 0) {
                                        stringResource(
                                            R.string.update_progress_of,
                                            Formatter.formatShortFileSize(context, phase.downloaded),
                                            Formatter.formatShortFileSize(context, phase.total)
                                        )
                                    } else {
                                        Formatter.formatShortFileSize(context, phase.downloaded)
                                    }
                                    else -> stringResource(R.string.update_verifying)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = stringResource(R.string.update_hide_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    is UpdatePhase.ReadyToInstall, is UpdatePhase.Installing -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(
                                if (phase is UpdatePhase.Installing) R.string.update_installing_note else R.string.update_ready_note
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            when (phase) {
                is UpdatePhase.Available -> Button(onClick = onDownload) { Text(stringResource(R.string.update_now)) }
                is UpdatePhase.Downloading, is UpdatePhase.Verifying ->
                    Button(onClick = onDismiss) { Text(stringResource(R.string.update_hide)) }
                is UpdatePhase.ReadyToInstall, is UpdatePhase.Installing ->
                    Button(onClick = onInstall) { Text(stringResource(R.string.update_install)) }
                is UpdatePhase.Failed -> Button(onClick = onDownload) { Text(stringResource(R.string.retry)) }
                UpdatePhase.Idle -> Unit
            }
        },
        dismissButton = {
            when (phase) {
                is UpdatePhase.Downloading ->
                    TextButton(onClick = onCancelDownload) { Text(stringResource(R.string.update_cancel_download)) }
                is UpdatePhase.Verifying -> Unit
                is UpdatePhase.Installing, is UpdatePhase.Failed ->
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                else -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.later)) }
            }
        }
    )
}

/** The message for an updater error, in the user's language. */
@Composable
fun updateErrorText(error: UpdateError, detail: String?): String = when (error) {
    UpdateError.NETWORK -> stringResource(R.string.update_error_network)
    UpdateError.RATE_LIMITED -> stringResource(R.string.update_error_rate_limited)
    UpdateError.SERVER -> stringResource(R.string.update_error_server)
    UpdateError.NO_APK -> stringResource(R.string.update_error_no_apk)
    UpdateError.STORAGE -> if (detail != null && detail.any { it.isDigit() } && detail.length < 16) {
        stringResource(R.string.update_error_storage, detail)
    } else {
        stringResource(R.string.update_error_storage_plain)
    }
    UpdateError.CORRUPT -> stringResource(R.string.update_error_corrupt)
    UpdateError.INSTALL_CONFLICT -> stringResource(R.string.update_error_install_conflict)
    UpdateError.INSTALL_STORAGE -> stringResource(R.string.update_error_install_storage)
    UpdateError.INSTALL_FAILED -> stringResource(R.string.update_error_install, detail.orEmpty()).trim()
}

// --- Structured Changelog Models & Parser ---

sealed interface ChangelogElement {
    data class Header(val title: String) : ChangelogElement
    data class Text(val content: String) : ChangelogElement
    data class BulletPoint(val content: String) : ChangelogElement
    data class ChangeItem(
        val category: String,
        val feature: String,
        val description: String
    ) : ChangelogElement
}

private fun parseChangelog(body: String): List<ChangelogElement> {
    val lines = body.lines()
    val elements = mutableListOf<ChangelogElement>()
    
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        
        // Check for Markdown Table Row (e.g. | Category | Feature | Description |)
        if (trimmed.startsWith("|")) {
            val parts = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            // Skip separators (like | :--- | :--- |)
            if (parts.any { it.contains("---") }) {
                continue
            }
            // Skip headers (like | Category | Feature | Description |)
            if (parts.size >= 2 && parts[0].equals("Category", ignoreCase = true)) {
                continue
            }
            
            if (parts.size >= 3) {
                val category = cleanMarkdown(parts[0])
                val feature = cleanMarkdown(parts[1])
                val description = cleanMarkdown(parts[2])
                elements.add(ChangelogElement.ChangeItem(category, feature, description))
            } else if (parts.size == 2) {
                val category = cleanMarkdown(parts[0])
                val description = cleanMarkdown(parts[1])
                elements.add(ChangelogElement.ChangeItem(category, "", description))
            } else if (parts.size == 1) {
                elements.add(ChangelogElement.Text(cleanMarkdown(parts[0])))
            }
            continue
        }
        
        // Check for Section Headers (e.g. ### Header)
        if (trimmed.startsWith("#")) {
            val headerText = trimmed.replace(Regex("^#+\\s*"), "")
            elements.add(ChangelogElement.Header(cleanMarkdown(headerText)))
            continue
        }
        
        // Check for Bullet Points (e.g. - Bullet or * Bullet)
        if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
            val bulletText = trimmed.substring(1).trim()
            elements.add(ChangelogElement.BulletPoint(cleanMarkdown(bulletText)))
            continue
        }
        
        // Default plain text row
        elements.add(ChangelogElement.Text(cleanMarkdown(trimmed)))
    }
    
    return elements
}

private fun cleanMarkdown(text: String): String {
    // Strips markdown bold markers (**text** or __text__)
    return text.replace(Regex("\\*\\*|__"), "").trim()
}

@Composable
private fun getCategoryColors(category: String): Pair<Color, Color> {
    return when {
        category.contains("bug", ignoreCase = true) || category.contains("🛠️") || category.contains("fix", ignoreCase = true) -> {
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        }
        category.contains("ui", ignoreCase = true) || category.contains("🎨") || category.contains("polish", ignoreCase = true) || category.contains("visual", ignoreCase = true) -> {
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        }
        category.contains("feature", ignoreCase = true) || category.contains("new", ignoreCase = true) || category.contains("✨") || category.contains("new features", ignoreCase = true) -> {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        }
        else -> {
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
}

@Composable
private fun ChangeItemRow(item: ChangelogElement.ChangeItem) {
    val (containerColor, contentColor) = getCategoryColors(item.category)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(containerColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            
            if (item.feature.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                // Feature Title
                Text(
                    text = item.feature,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Description
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}

@Composable
private fun HeaderRow(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            thickness = 2.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BulletPointRow(content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TextRow(content: String) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
}
