package com.emanuel5014.trainable.ui.components

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.SportsGymnastics
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.remote.GitHubRelease
import com.emanuel5014.trainable.data.remote.ReleaseNoteItem
import com.emanuel5014.trainable.data.remote.ReleaseNotes
import com.emanuel5014.trainable.data.remote.localized
import com.emanuel5014.trainable.ui.screens.routines.RowCorners
import com.emanuel5014.trainable.ui.screens.routines.routineRowCorners
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.util.UpdateError
import com.emanuel5014.trainable.util.UpdateFiles
import com.emanuel5014.trainable.util.UpdatePhase
import com.emanuel5014.trainable.util.isFinal
import com.emanuel5014.trainable.util.releaseDisplayVersion
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val ISSUES_URL = "https://github.com/Emanuel5014/Trainable/issues/"

/**
 * The update offer and everything after it, as a sheet: the release with its notes grouped as new, improved and
 * fixed (one line each, opened for the details), and at the bottom the action of the current phase (download,
 * progress, install, error). Closing it while downloading only hides it: the download goes on in its notification.
 *
 * [notes] are the release's notes in every language (null = show the release text as it is).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(
    phase: UpdatePhase,
    notes: ReleaseNotes?,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onCancelDownload: () -> Unit,
    onSkip: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val release = phase.releaseOrNull() ?: return
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val language = locale.language
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Buttons that close the sheet slide it away first, then tell the manager
    val closeThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }
    val padding = ResponsiveSize.horizontalPadding
    val apkSize = UpdateFiles.apkAsset(release)?.size ?: 0L
    val sizeText = if (apkSize > 0) Formatter.formatShortFileSize(context, apkSize) else null

    val items = notes?.items.orEmpty()
    val newItems = items.filter { it.type == ReleaseNoteItem.TYPE_NEW || it.type !in KNOWN_TYPES }
    val improvedItems = items.filter { it.type == ReleaseNoteItem.TYPE_IMPROVED }
    val fixedItems = items.filter { it.type == ReleaseNoteItem.TYPE_FIXED }
    val expanded = remember(release.tagName) { mutableStateOf(setOf<Int>()) }
    var fixesOpen by rememberSaveable(release.tagName) {
        mutableStateOf(newItems.isEmpty() && improvedItems.isEmpty())
    }
    val status = statusOf(phase)
    val toggle: (Int) -> Unit = { key ->
        expanded.value = if (key in expanded.value) expanded.value - key else expanded.value + key
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
            contentPadding = PaddingValues(start = padding, end = padding, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            item(key = "header") {
                SheetHeader(
                    release = release,
                    headline = notes?.headline?.localized(language).orEmpty(),
                    meta = listOfNotNull(releaseDate(release.publishedAt, locale), sizeText).joinToString(" · ")
                )
            }
            if (status != null) {
                item(key = "status") { StatusCard(status, Modifier.padding(bottom = 10.dp)) }
            }

            if (notes == null) {
                item(key = "raw") {
                    Text(
                        text = release.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            listOf(
                Triple("new", R.string.update_section_new, newItems),
                Triple("improved", R.string.update_section_improved, improvedItems)
            ).forEach { (key, label, list) ->
                if (list.isEmpty()) return@forEach
                item(key = "label-$key") { SectionLabel(stringResource(label), list.size) }
                itemsIndexed(list, key = { index, _ -> "$key-$index" }) { index, item ->
                    val rowKey = items.indexOf(item)
                    NoteRow(
                        item = item,
                        language = language,
                        corners = routineRowCorners(index, list.size),
                        expanded = rowKey in expanded.value,
                        onToggle = { toggle(rowKey) },
                        onIssue = { onOpenUrl(ISSUES_URL + it) }
                    )
                }
            }

            if (fixedItems.isNotEmpty()) {
                // The head row names the count itself ("3 fixes"), so the section needs no label of its own
                item(key = "fixes-head") {
                    FixesHead(
                        modifier = Modifier.padding(top = 14.dp),
                        count = fixedItems.size,
                        open = fixesOpen,
                        corners = if (fixesOpen) routineRowCorners(0, fixedItems.size + 1) else routineRowCorners(0, 1),
                        onToggle = { fixesOpen = !fixesOpen }
                    )
                }
                if (fixesOpen) {
                    itemsIndexed(fixedItems, key = { index, _ -> "fixed-$index" }) { index, item ->
                        val rowKey = items.indexOf(item)
                        NoteRow(
                            item = item,
                            language = language,
                            corners = routineRowCorners(index + 1, fixedItems.size + 1),
                            expanded = rowKey in expanded.value,
                            onToggle = { toggle(rowKey) },
                            onIssue = { onOpenUrl(ISSUES_URL + it) }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ActionBar(
            phase = phase,
            release = release,
            sizeText = sizeText,
            onDownload = onDownload,
            onInstall = onInstall,
            onCancelDownload = onCancelDownload,
            onSkip = { closeThen(onSkip) },
            onOpenUrl = onOpenUrl,
            onClose = { closeThen(onDismiss) },
            modifier = Modifier.padding(start = padding, end = padding, top = 12.dp, bottom = 8.dp)
        )
    }
}

private val KNOWN_TYPES = setOf(ReleaseNoteItem.TYPE_NEW, ReleaseNoteItem.TYPE_IMPROVED, ReleaseNoteItem.TYPE_FIXED)

private fun UpdatePhase.releaseOrNull(): GitHubRelease? = when (this) {
    UpdatePhase.Idle -> null
    is UpdatePhase.Available -> release
    is UpdatePhase.Downloading -> release
    is UpdatePhase.Verifying -> release
    is UpdatePhase.ReadyToInstall -> release
    is UpdatePhase.Installing -> release
    is UpdatePhase.Failed -> release
}

/** "2026-11-11T22:14:38Z" → "11 nov 2026" in the app's language; null when missing or unreadable. */
private fun releaseDate(iso: String, locale: Locale): String? = runCatching {
    OffsetDateTime.parse(iso).toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}.getOrNull()

@Composable
private fun SheetHeader(release: GitHubRelease, headline: String, meta: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (release.prerelease) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(
                        text = stringResource(R.string.update_beta_chip),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.update_sheet_title, releaseDisplayVersion(release.tagName)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (headline.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (release.prerelease) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.update_is_beta),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

private class SheetStatus(val icon: ImageVector, val text: String, val container: Color, val content: Color)

@Composable
private fun statusOf(phase: UpdatePhase): SheetStatus? {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    return when (phase) {
        is UpdatePhase.Failed -> if (phase.error == UpdateError.OTHER_SOURCE) {
            SheetStatus(Icons.Rounded.Info, updateErrorText(phase.error, phase.detail), colors.tertiaryContainer, colors.onTertiaryContainer)
        } else {
            SheetStatus(Icons.Rounded.ErrorOutline, updateErrorText(phase.error, phase.detail), colors.errorContainer, colors.onErrorContainer)
        }
        is UpdatePhase.ReadyToInstall ->
            SheetStatus(Icons.Rounded.CheckCircle, stringResource(R.string.update_ready_note), colors.primaryContainer, colors.onPrimaryContainer)
        is UpdatePhase.Installing ->
            SheetStatus(Icons.Rounded.SystemUpdate, stringResource(R.string.update_installing_note), colors.primaryContainer, colors.onPrimaryContainer)
        is UpdatePhase.Available -> {
            val metered = remember {
                context.getSystemService(android.net.ConnectivityManager::class.java)?.isActiveNetworkMetered == true
            }
            val size = UpdateFiles.apkAsset(phase.release)?.size ?: 0L
            if (metered && size > 0) {
                SheetStatus(
                    Icons.Rounded.SignalCellularAlt,
                    stringResource(R.string.update_metered_note, Formatter.formatShortFileSize(context, size)),
                    colors.secondaryContainer,
                    colors.onSecondaryContainer
                )
            } else {
                null
            }
        }
        else -> null
    }
}

@Composable
private fun StatusCard(status: SheetStatus, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(20.dp), color = status.container, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(status.icon, contentDescription = null, tint = status.content, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(text = status.text, style = MaterialTheme.typography.bodyMedium, color = status.content)
        }
    }
}

@Composable
private fun SectionLabel(label: String, count: Int) {
    Text(
        text = "$label · $count",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp)
    )
}

private fun RowCorners.shape() =
    RoundedCornerShape(topStart = top.dp, topEnd = top.dp, bottomEnd = bottom.dp, bottomStart = bottom.dp)

@Composable
private fun IconBubble(icon: ImageVector, container: Color, content: Color) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteRow(
    item: ReleaseNoteItem,
    language: String,
    corners: RowCorners,
    expanded: Boolean,
    onToggle: () -> Unit,
    onIssue: (Int) -> Unit
) {
    val details = item.details.localized(language)
    val opens = details.isNotEmpty() || item.issues.isNotEmpty()
    val fixed = item.type == ReleaseNoteItem.TYPE_FIXED
    val colors = MaterialTheme.colorScheme
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Surface(
        shape = corners.shape(),
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .then(if (opens) Modifier.clickable(onClick = onToggle) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    icon = areaIcon(item.area),
                    container = if (fixed) colors.secondaryContainer else colors.primaryContainer,
                    content = if (fixed) colors.onSecondaryContainer else colors.onPrimaryContainer
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.localized(language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    val summary = item.summary.localized(language)
                    if (summary.isNotEmpty()) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                if (opens) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(R.string.update_show_details),
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.rotate(chevron)
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded && opens,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 54.dp, top = 8.dp)) {
                    if (details.isNotEmpty()) {
                        Text(text = details, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    }
                    if (item.issues.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            item.issues.forEach { issue ->
                                Surface(
                                    shape = CircleShape,
                                    color = colors.surfaceContainerHighest,
                                    modifier = Modifier.clip(CircleShape).clickable { onIssue(issue) }
                                ) {
                                    Text(
                                        text = "#$issue",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FixesHead(count: Int, open: Boolean, corners: RowCorners, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val chevron by animateFloatAsState(if (open) 180f else 0f, label = "fixes chevron")
    Surface(shape = corners.shape(), color = colors.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(Icons.Rounded.Build, colors.secondaryContainer, colors.onSecondaryContainer)
            Spacer(Modifier.width(14.dp))
            Text(
                text = pluralStringResource(R.plurals.update_fixes_count, count, count),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = stringResource(R.string.update_show_details),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.rotate(chevron)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionBar(
    phase: UpdatePhase,
    release: GitHubRelease,
    sizeText: String?,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onCancelDownload: () -> Unit,
    onSkip: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainButton = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    Column(modifier = modifier.fillMaxWidth()) {
        when (phase) {
            is UpdatePhase.Available -> {
                Button(onClick = onDownload, modifier = mainButton) {
                    Text(
                        text = sizeText?.let { stringResource(R.string.update_download_size, it) }
                            ?: stringResource(R.string.update_now),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.update_skip_version)) }
                    TextButton(onClick = onClose) { Text(stringResource(R.string.later)) }
                }
            }
            is UpdatePhase.Downloading, is UpdatePhase.Verifying -> {
                val downloading = phase as? UpdatePhase.Downloading
                val fraction = downloading?.fraction
                if (fraction != null) {
                    LinearWavyProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = when {
                        downloading == null -> stringResource(R.string.update_verifying)
                        downloading.total > 0 -> stringResource(
                            R.string.update_progress_of,
                            Formatter.formatShortFileSize(context, downloading.downloaded),
                            Formatter.formatShortFileSize(context, downloading.total)
                        )
                        else -> Formatter.formatShortFileSize(context, downloading.downloaded)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = stringResource(R.string.update_hide_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (downloading != null) {
                        TextButton(onClick = onCancelDownload) { Text(stringResource(R.string.update_cancel_download)) }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    FilledTonalButton(onClick = onClose) { Text(stringResource(R.string.update_hide)) }
                }
            }
            is UpdatePhase.ReadyToInstall, is UpdatePhase.Installing -> {
                Button(onClick = onInstall, modifier = mainButton) {
                    Text(stringResource(R.string.update_install), style = MaterialTheme.typography.titleSmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) {
                        Text(stringResource(if (phase is UpdatePhase.Installing) R.string.close else R.string.later))
                    }
                }
            }
            is UpdatePhase.Failed -> {
                when {
                    !phase.error.isFinal -> Button(onClick = onDownload, modifier = mainButton) {
                        Text(stringResource(R.string.retry), style = MaterialTheme.typography.titleSmall)
                    }
                    release.htmlUrl.isNotEmpty() -> Button(onClick = { onOpenUrl(release.htmlUrl) }, modifier = mainButton) {
                        Text(stringResource(R.string.update_open_release), style = MaterialTheme.typography.titleSmall)
                    }
                    else -> Unit
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
                }
            }
            UpdatePhase.Idle -> Unit
        }
    }
}

/** The icon of a part of the app named in the notes (see tools/release_notes.py for the list). */
fun areaIcon(area: String): ImageVector = when (area) {
    "updates" -> Icons.Rounded.SystemUpdate
    "dashboard" -> Icons.Rounded.SpaceDashboard
    "routines" -> Icons.Rounded.ViewAgenda
    "history" -> Icons.Rounded.CalendarMonth
    "analytics" -> Icons.Rounded.Insights
    "workouts" -> Icons.Rounded.FitnessCenter
    "exercises" -> Icons.Rounded.SportsGymnastics
    "timers" -> Icons.Rounded.Timer
    "onboarding" -> Icons.Rounded.WavingHand
    "notifications" -> Icons.Rounded.Notifications
    "appearance" -> Icons.Rounded.Palette
    "liquid_glass" -> Icons.Rounded.BubbleChart
    "backup" -> Icons.Rounded.CloudSync
    "import_export" -> Icons.Rounded.ImportExport
    "ai" -> Icons.Rounded.AutoAwesome
    "widgets" -> Icons.Rounded.Widgets
    "journal" -> Icons.Rounded.EditNote
    "settings" -> Icons.Rounded.Tune
    else -> Icons.Rounded.NewReleases
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
    UpdateError.OTHER_SOURCE -> stringResource(R.string.update_error_other_source)
    UpdateError.WRONG_APP -> stringResource(R.string.update_error_wrong_app)
    UpdateError.INSTALL_CONFLICT -> stringResource(R.string.update_error_install_conflict)
    UpdateError.INSTALL_STORAGE -> stringResource(R.string.update_error_install_storage)
    UpdateError.INSTALL_FAILED -> stringResource(R.string.update_error_install, detail.orEmpty()).trim()
}
