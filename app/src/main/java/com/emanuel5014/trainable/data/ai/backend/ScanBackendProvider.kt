package com.emanuel5014.trainable.data.ai.backend

import android.content.Context
import com.emanuel5014.trainable.data.ai.AiModelVariant
import com.emanuel5014.trainable.data.ai.LocalLlmEngine
import com.emanuel5014.trainable.data.ai.ModelFileManager
import com.emanuel5014.trainable.data.ai.remote.AiProfileStore
import com.emanuel5014.trainable.data.ai.remote.AiRemoteHttp
import com.emanuel5014.trainable.data.ai.remote.RemoteImagePrep
import com.emanuel5014.trainable.data.ai.remote.RemoteScanBackend
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/** Picks where the next scan runs (the phone or the active remote profile) and whether it can run at all. */
@Singleton
class ScanBackendProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferencesRepository,
    private val store: AiProfileStore,
    private val engine: LocalLlmEngine,
    private val modelFileManager: ModelFileManager,
    baseClient: OkHttpClient
) {
    private val remoteClient by lazy { AiRemoteHttp.client(baseClient) }

    suspend fun current(): ScanBackend = withContext(Dispatchers.IO) {
        val active = activeProfile()
        if (active == null) {
            LocalScanBackend(engine, modelFileManager, AiModelVariant.fromId(prefs.aiModelVariant.first()))
        } else {
            RemoteScanBackend(active, store.readKey(active), store.keyState(active), remoteClient) { uri ->
                RemoteImagePrep.prepare(context, uri)
            }
        }
    }

    /** True while the on-device model is the choice; the phone's live metrics only mean something then. */
    val localActive: Flow<Boolean> = combine(store.activeId, store.profiles) { id, list ->
        ScanReadiness.resolveActive(id, list) == null
    }

    suspend fun isLocalActive(): Boolean = localActive.first()

    /** Whether a scan can be offered now: switched on, and the chosen place is usable. */
    val isReady: Flow<Boolean> = combine(
        prefs.aiScanEnabled,
        store.activeId,
        store.profiles,
        prefs.aiModelVariant,
        modelFileManager.filesUpdatedTrigger
    ) { enabled, id, list, variantId, _ ->
        ScanReadiness.isReady(enabled, id, list, modelFileManager.isDownloaded(AiModelVariant.fromId(variantId)))
    }

    suspend fun consentRequired(): ConsentRequest? = ScanReadiness.consentRequired(activeProfile())

    /** Records the user's agreement for the profile in use. */
    suspend fun acceptConsent() {
        activeProfile()?.let { store.acceptConsent(it.id) }
    }

    suspend fun releaseLocal() = engine.release()

    private suspend fun activeProfile() = ScanReadiness.resolveActive(store.activeId.first(), store.profiles.first())
}
