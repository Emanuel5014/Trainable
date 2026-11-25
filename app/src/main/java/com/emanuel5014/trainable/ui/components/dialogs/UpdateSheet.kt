package com.emanuel5014.trainable.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SystemUpdate
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.remote.GitHubRelease
import com.emanuel5014.trainable.data.remote.ReleaseNotes
import com.emanuel5014.trainable.data.remote.localized
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
    val expanded = remember(release.tagName) { mutableStateOf(setOf<Int>()) }
    var fixesOpen by rememberSaveable(release.tagName) { mutableStateOf(items.onlyFixes()) }
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

            releaseNoteItems(
                items = items,
                language = language,
                expanded = expanded.value,
                onToggle = toggle,
                fixesOpen = fixesOpen,
                onToggleFixes = { fixesOpen = !fixesOpen },
                onIssue = { onOpenUrl(RELEASE_ISSUES_URL + it) }
            )
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
