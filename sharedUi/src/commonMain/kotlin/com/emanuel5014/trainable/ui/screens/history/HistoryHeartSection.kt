package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.emanuel5014.trainable.data.local.entity.SessionHeartDataEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.repository.HeartSyncResult
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.heart_allow
import com.emanuel5014.trainable.sharedui.resources.heart_no_data
import com.emanuel5014.trainable.sharedui.resources.heart_no_duration
import com.emanuel5014.trainable.sharedui.resources.heart_no_permission
import com.emanuel5014.trainable.sharedui.resources.heart_reading
import com.emanuel5014.trainable.sharedui.resources.heart_refresh
import com.emanuel5014.trainable.ui.components.heart.HeartCardMessage
import com.emanuel5014.trainable.ui.components.heart.HeartRateCard
import com.emanuel5014.trainable.ui.components.heart.rememberHealthAccess
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/** A workout that ended this long ago still waits for the band to sync: older ones without data show nothing. */
private const val RECENT_MS = 7L * 24 * 3_600_000

/** Saved data, or [Loading] until the database answered (so "no data" does not flash first). */
private sealed interface SavedHeart {
    data object Loading : SavedHeart
    data class Loaded(val data: SessionHeartDataEntity?) : SavedHeart
}

/**
 * The heart-rate card of an opened workout. With the switch on (Android) it reads the workout's span from Health
 * Connect when nothing is saved for it yet; saved data is shown on a platform without a store too (iOS).
 */
@Composable
internal fun HistoryHeartSection(
    state: HistoryState,
    session: WorkoutSessionEntity,
    enabled: Boolean,
    maxHeartRate: Int,
    modifier: Modifier = Modifier
) {
    val access = rememberHealthAccess()
    val canRead = enabled && state.canSyncHeart && access.available && !access.needsInstall
    val saved by remember(session.id) { state.heartData(session.id).map<SessionHeartDataEntity?, SavedHeart> { SavedHeart.Loaded(it) } }
        .collectAsState(initial = SavedHeart.Loading)
    var syncing by remember(session.id) { mutableStateOf(false) }
    var result by remember(session.id) { mutableStateOf<HeartSyncResult?>(null) }
    val scope = rememberCoroutineScope()

    fun sync(force: Boolean) {
        if (syncing) return
        scope.launch {
            syncing = true
            result = state.syncHeart(session, force)
            syncing = false
        }
    }

    LaunchedEffect(session.id, session.timestamp, session.durationMs, canRead) {
        if (canRead) sync(force = false)
    }

    val loaded = saved as? SavedHeart.Loaded ?: return
    val data = loaded.data
    // Android follows the switch; where there is no store (iOS) saved data still shows
    if (data != null && !(enabled || !access.available)) return

    val endMs = session.timestamp + (session.durationMs ?: 0L)
    val recent = Clock.System.now().toEpochMilliseconds() - endMs < RECENT_MS
    val message = when {
        data != null || !canRead -> null
        result == HeartSyncResult.NO_PERMISSION && !syncing -> HeartCardMessage(
            stringResource(Res.string.heart_no_permission),
            stringResource(Res.string.heart_allow)
        ) { access.request { granted -> if (granted) sync(force = true) } }
        // An old workout with nothing saved stays quiet: no loader flashing, no "sync your band"
        !recent -> null
        syncing || result == null -> HeartCardMessage(stringResource(Res.string.heart_reading), null, null)
        result == HeartSyncResult.NO_DATA -> HeartCardMessage(
            stringResource(Res.string.heart_no_data),
            stringResource(Res.string.heart_refresh)
        ) { sync(force = true) }
        result == HeartSyncResult.NO_DURATION -> HeartCardMessage(stringResource(Res.string.heart_no_duration), null, null)
        else -> null
    }
    if (data == null && message == null) return

    HeartRateCard(
        data = data,
        maxHeartRate = maxHeartRate,
        syncing = syncing,
        onRefresh = if (canRead) ({ sync(force = true) }) else null,
        message = message,
        modifier = modifier
    )
}
