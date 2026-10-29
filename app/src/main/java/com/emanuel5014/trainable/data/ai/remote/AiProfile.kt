package com.emanuel5014.trainable.data.ai.remote

import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class AiProtocol { OPENAI_COMPAT, ANTHROPIC }

/** What the app can say about a stored key without ever showing it. */
enum class KeyState { NONE, PRESENT, UNREADABLE }

/** A remote place to read sheets: a cloud service or a server on the user's own PC. */
@Serializable
data class AiProfile(
    val id: String,
    val name: String,
    val protocol: AiProtocol,
    val baseUrl: String,
    val model: String,
    /** The API key encrypted with the Keystore (hex), like the Nextcloud password; null when there is none. */
    val encryptedKey: String? = null,
    val keyIv: String? = null,
    /** The user accepted that photos go to this host; reset when the host changes. */
    val consentGiven: Boolean = false
)

/** Claude and Gemini refuse a request without a key; the servers on the user's PC usually don't need one. */
fun requiresKey(protocol: AiProtocol, baseUrl: String): Boolean =
    protocol == AiProtocol.ANTHROPIC || GeminiEndpoint.matches(baseUrl)

/** Address accepted by [BaseUrlPolicy], a model chosen, and a key where the provider demands one. */
fun AiProfile.isComplete(): Boolean =
    BaseUrlPolicy.check(baseUrl) is UrlCheck.Ok &&
        model.isNotBlank() &&
        (!requiresKey(protocol, baseUrl) || !encryptedKey.isNullOrBlank())

/** The profile list and the choice of where the AI runs, as pure functions over what the preferences hold. */
object AiProfileList {

    /** The value of `ai_active_backend` for the on-device model. */
    const val LOCAL = "local"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun decode(raw: String?): List<AiProfile> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<AiProfile>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encode(list: List<AiProfile>): String = json.encodeToString(list)

    fun upsert(list: List<AiProfile>, profile: AiProfile): List<AiProfile> =
        if (list.any { it.id == profile.id }) list.map { if (it.id == profile.id) profile else it } else list + profile

    /** The list without [removeId] and the active choice that goes with it (back to [LOCAL] if the active one went). */
    fun remove(list: List<AiProfile>, activeId: String, removeId: String): Pair<List<AiProfile>, String> =
        list.filterNot { it.id == removeId } to (if (activeId == removeId) LOCAL else activeId)

    /** [edited] as it should be stored: consent carries over from [old] only while the host is unchanged. */
    fun applyEdit(old: AiProfile?, edited: AiProfile): AiProfile =
        edited.copy(consentGiven = old != null && old.consentGiven && keepsKey(old, edited))

    /**
     * Whether a key saved on [old] may be used for [edited]: only while the address still points at the same
     * host, so editing a profile can't send a key to a place the user never gave it to.
     */
    fun keepsKey(old: AiProfile?, edited: AiProfile): Boolean =
        old != null && hostOf(old.baseUrl) == hostOf(edited.baseUrl)

    private val QUOTE_PAIRS = mapOf('"' to '"', '\'' to '\'', '\u201C' to '\u201D', '\u2018' to '\u2019')
    private val INVISIBLE = setOf('\u200B', '\u200C', '\u200D', '\u2060', '\uFEFF')

    /** A key as pasted: no surrounding quotes, no whitespace or invisible characters anywhere; null when nothing is left. */
    fun cleanKey(raw: String?): String? {
        var key = raw?.trim().orEmpty()
        if (key.length >= 2 && QUOTE_PAIRS[key.first()] == key.last()) key = key.substring(1, key.length - 1)
        return key.filterNot { it.isWhitespace() || it in INVISIBLE }.ifEmpty { null }
    }

    /** A request header can only carry printable ASCII; anything else would make OkHttp throw with the key in its message. */
    fun isValidKey(key: String): Boolean = key.isNotEmpty() && key.all { it.code in 0x21..0x7E }

    private fun hostOf(url: String): String {
        val normalized = BaseUrlPolicy.normalize(url)
        return normalized.toHttpUrlOrNull()?.host ?: normalized.lowercase()
    }
}
