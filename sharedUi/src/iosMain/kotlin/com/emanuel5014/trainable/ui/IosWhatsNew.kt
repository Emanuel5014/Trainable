package com.emanuel5014.trainable.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.remote.ReleaseNoteItem
import com.emanuel5014.trainable.data.remote.ReleaseNotes
import com.emanuel5014.trainable.platform.IosAppLocale
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.whats_new_settings
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.WhatsNewSheet
import com.emanuel5014.trainable.ui.components.bundledReleaseNotes
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.util.releaseCoreVersion
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

/**
 * AltStore shows a release's notes in a small box, so the app shows them itself: once, on the first launch of a new
 * build, from the `changelog.json` it was built with. The build seen last is kept in the user defaults; a fresh
 * install marks its own build as seen during the first setup, so only updates show the sheet.
 */
internal object IosWhatsNew {
    private const val SEEN_KEY = "whatsnew.seen_build"

    val versionName: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""

    private val build: String
        get() = "$versionName(${NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: ""})"

    fun isUnseen(): Boolean = NSUserDefaults.standardUserDefaults.stringForKey(SEEN_KEY) != build

    fun markSeen() = NSUserDefaults.standardUserDefaults.setObject(build, forKey = SEEN_KEY)

    /** The bundled notes when they belong to this version (a file left from an older version is never shown). */
    suspend fun notes(): ReleaseNotes? = bundledReleaseNotes(ReleaseNoteItem.PLATFORM_IOS)
        ?.takeIf { releaseCoreVersion(it.version) == releaseCoreVersion(versionName) }
}

/** The sheet on the first launch after an update; nothing when this build was seen or has no notes. */
@Composable
internal fun IosWhatsNewAfterUpdate() {
    var notes by remember { mutableStateOf<ReleaseNotes?>(null) }
    LaunchedEffect(Unit) {
        if (!IosWhatsNew.isUnseen()) return@LaunchedEffect
        val bundled = IosWhatsNew.notes()
        if (bundled == null) IosWhatsNew.markSeen() else notes = bundled
    }
    notes?.let { shown ->
        IosWhatsNewSheet(shown, onDismiss = {
            IosWhatsNew.markSeen()
            notes = null
        })
    }
}

/** Settings > About: opens the notes of the installed version again; hidden when the build has none. */
@Composable
internal fun IosWhatsNewButton() {
    var available by remember { mutableStateOf<ReleaseNotes?>(null) }
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { available = IosWhatsNew.notes() }
    val notes = available ?: return
    GymButton(
        onClick = { open = true },
        containerColor = Primary.copy(alpha = 0.1f),
        contentColor = Primary,
        height = 48,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(
            text = stringResource(Res.string.whats_new_settings),
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelLarge
        )
    }
    if (open) IosWhatsNewSheet(notes, onDismiss = { open = false })
}

@Composable
private fun IosWhatsNewSheet(notes: ReleaseNotes, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    WhatsNewSheet(
        notes = notes,
        language = IosAppLocale.language(),
        onOpenUrl = { uriHandler.openUri(it) },
        onDismiss = onDismiss
    )
}
