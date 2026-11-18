package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.*
import org.jetbrains.compose.resources.stringResource

/** "Proteggi i tuoi dati": the local folder and Nextcloud on one step, each in a card that opens. */
@Composable
fun DataStep(
    localOn: Boolean,
    nextcloudConnected: Boolean,
    local: @Composable () -> Unit,
    nextcloud: @Composable () -> Unit,
    hero: @Composable () -> Unit
) {
    val reduceMotion = rememberReduceMotion()
    var localOpen by rememberSaveable { mutableStateOf(true) }
    // A connected account shows its automatic-upload switch, so nothing runs that the user hasn't seen
    var cloudOpen by rememberSaveable(nextcloudConnected) { mutableStateOf(nextcloudConnected) }

    OnboardingPage(underTopRow = true) {
        hero()
        OnboardingTitle(stringResource(Res.string.onboarding_backup_title))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DataCard(
                title = stringResource(Res.string.onboarding_data_local),
                badge = if (localOn) stringResource(Res.string.backup_enabled_short) else null,
                open = localOpen,
                onToggle = { localOpen = !localOpen },
                reduceMotion = reduceMotion,
                content = local
            )
            DataCard(
                title = stringResource(Res.string.nextcloud_title),
                badge = if (nextcloudConnected) stringResource(Res.string.backup_enabled_short) else stringResource(Res.string.onboarding_connect),
                open = cloudOpen,
                onToggle = { cloudOpen = !cloudOpen },
                reduceMotion = reduceMotion,
                content = nextcloud
            )
        }
    }
}

@Composable
private fun DataCard(
    title: String,
    badge: String?,
    open: Boolean,
    onToggle: () -> Unit,
    reduceMotion: Boolean,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupHeader(title, badge, open, onToggle, reduceMotion)
        AnimatedVisibility(
            visible = open,
            enter = if (reduceMotion) expandVertically(snap()) else expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = if (reduceMotion) shrinkVertically(snap()) else shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec())
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}
