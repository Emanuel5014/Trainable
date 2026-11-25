package com.emanuel5014.trainable.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.remote.ReleaseNotes
import com.emanuel5014.trainable.data.remote.forPlatform
import com.emanuel5014.trainable.data.remote.localized
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.update_beta_chip
import com.emanuel5014.trainable.sharedui.resources.update_sheet_title
import com.emanuel5014.trainable.sharedui.resources.whats_new_done
import com.emanuel5014.trainable.sharedui.resources.whats_new_title
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.util.isPreReleaseVersion
import com.emanuel5014.trainable.util.releaseDisplayVersion
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

/** Where the app carries the `changelog.json` of its own version (copied there before a build, see HANDOFF). */
const val BUNDLED_CHANGELOG = "files/changelog.json"

private val notesJson = Json { ignoreUnknownKeys = true }

/**
 * The notes the app was built with, only the items meant for [platform]; null when the file is missing, cannot be
 * read or has nothing for this app.
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun bundledReleaseNotes(platform: String): ReleaseNotes? = runCatching {
    notesJson.decodeFromString<ReleaseNotes>(Res.readBytes(BUNDLED_CHANGELOG).decodeToString()).forPlatform(platform)
}.getOrNull()?.takeIf { it.items.isNotEmpty() }

/**
 * What changed in the installed version: the same rows as the Android update sheet, under "What's new" and the
 * version, with one button that closes it. Shown once after an update and again from Settings > About.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(
    notes: ReleaseNotes,
    language: String,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val padding = ResponsiveSize.horizontalPadding
    val items = notes.items
    var expanded by remember(notes.version) { mutableStateOf(setOf<Int>()) }
    var fixesOpen by remember(notes.version) { mutableStateOf(items.onlyFixes()) }

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
                WhatsNewHeader(
                    version = notes.version,
                    headline = notes.headline.localized(language)
                )
            }
            releaseNoteItems(
                items = items,
                language = language,
                expanded = expanded,
                onToggle = { key -> expanded = if (key in expanded) expanded - key else expanded + key },
                fixesOpen = fixesOpen,
                onToggleFixes = { fixesOpen = !fixesOpen },
                onIssue = { onOpenUrl(RELEASE_ISSUES_URL + it) }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Button(
            onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
            modifier = Modifier
                .padding(start = padding, end = padding, top = 12.dp, bottom = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text(stringResource(Res.string.whats_new_done), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun WhatsNewHeader(version: String, headline: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isPreReleaseVersion(version)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(
                        text = stringResource(Res.string.update_beta_chip),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = stringResource(Res.string.whats_new_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.update_sheet_title, releaseDisplayVersion(version)),
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
    }
}
