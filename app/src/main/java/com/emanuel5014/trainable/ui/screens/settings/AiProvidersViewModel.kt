package com.emanuel5014.trainable.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import com.emanuel5014.trainable.data.ai.remote.AiProfile
import com.emanuel5014.trainable.data.ai.remote.AiProfileList
import com.emanuel5014.trainable.data.ai.remote.AiProfileStore
import com.emanuel5014.trainable.data.ai.remote.AiProtocol
import com.emanuel5014.trainable.data.ai.remote.AiRemoteHttp
import com.emanuel5014.trainable.data.ai.remote.BaseUrlPolicy
import com.emanuel5014.trainable.data.ai.remote.HostKind
import com.emanuel5014.trainable.data.ai.remote.KeyState
import com.emanuel5014.trainable.data.ai.remote.ProtocolClient
import com.emanuel5014.trainable.data.ai.remote.RemoteProbe
import com.emanuel5014.trainable.data.ai.remote.UrlCheck
import com.emanuel5014.trainable.data.ai.remote.createProtocolClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.util.UUID
import javax.inject.Inject

/** What the provider editor holds before saving. [plainKey] null means "keep the saved key". */
data class ProviderDraft(
    val id: String?,
    val name: String,
    val protocol: AiProtocol,
    val baseUrl: String,
    val model: String,
    val plainKey: String?
)

/** A saved provider as the list shows it. [kind] is null when the address can't be read. */
data class ProviderRow(
    val profile: AiProfile,
    val host: String?,
    val kind: HostKind?,
    val keyState: KeyState
)

@HiltViewModel
class AiProvidersViewModel @Inject constructor(
    private val store: AiProfileStore,
    baseClient: OkHttpClient
) : ViewModel() {

    private val probe = RemoteProbe(AiRemoteHttp.client(baseClient))

    val rows: StateFlow<List<ProviderRow>> = store.profiles
        .map { list -> list.map(::rowOf) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** [AiProfileList.LOCAL] or the id of the profile the scan runs on. */
    val activeId: StateFlow<String> = store.activeId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiProfileList.LOCAL)

    fun select(id: String) {
        viewModelScope.launch { store.setActive(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { store.delete(id) }
    }

    /** A new provider becomes the active one; editing an existing one leaves the choice alone. */
    fun save(draft: ProviderDraft, onSaved: () -> Unit) {
        val check = BaseUrlPolicy.check(draft.baseUrl) as? UrlCheck.Ok ?: return
        // A key a header can't carry is refused here too, not only greyed out in the editor
        AiProfileList.cleanKey(draft.plainKey)?.let { if (!AiProfileList.isValidKey(it)) return }
        viewModelScope.launch {
            val profile = AiProfile(
                id = draft.id ?: UUID.randomUUID().toString(),
                name = draft.name.trim().ifBlank { check.url.host },
                protocol = draft.protocol,
                baseUrl = BaseUrlPolicy.normalize(draft.baseUrl),
                model = draft.model.trim()
            )
            withContext(Dispatchers.IO) { store.save(profile, draft.plainKey, makeActive = draft.id == null) }
            onSaved()
        }
    }

    /** The models the provider lists, or why it couldn't. */
    suspend fun loadModels(draft: ProviderDraft): Result<List<String>> =
        withClient(draft) { probe.listModels(it) }

    /** Whether a tiny request with this address, key and model works. */
    suspend fun test(draft: ProviderDraft): Result<Unit> =
        withClient(draft) { probe.ping(it) }

    private suspend fun <T> withClient(draft: ProviderDraft, block: suspend (ProtocolClient) -> Result<T>): Result<T> {
        (BaseUrlPolicy.check(draft.baseUrl) as? UrlCheck.Rejected)?.let {
            return Result.failure(ScanBackendException.PolicyBlocked(it.reason))
        }
        val stored = draft.id?.let { id -> store.profiles.first().firstOrNull { it.id == id } }
        val profile = AiProfile(
            id = draft.id ?: "draft",
            name = draft.name,
            protocol = draft.protocol,
            baseUrl = BaseUrlPolicy.normalize(draft.baseUrl),
            model = draft.model.trim()
        )
        // The saved key follows the profile only to the host it was saved for
        val key = AiProfileList.cleanKey(draft.plainKey)
            ?: stored?.takeIf { AiProfileList.keepsKey(it, profile) }?.let { withContext(Dispatchers.IO) { store.readKey(it) } }
        if (key != null && !AiProfileList.isValidKey(key)) return Result.failure(ScanBackendException.KeyUnreadable())
        return block(createProtocolClient(profile, key))
    }

    private fun rowOf(profile: AiProfile): ProviderRow {
        val host = BaseUrlPolicy.normalize(profile.baseUrl).toHttpUrlOrNull()?.host
        return ProviderRow(profile, host, host?.let(BaseUrlPolicy::hostKind), store.keyState(profile))
    }
}
