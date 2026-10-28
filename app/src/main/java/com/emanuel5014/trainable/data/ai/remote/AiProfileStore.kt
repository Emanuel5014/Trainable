package com.emanuel5014.trainable.data.ai.remote

import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.util.AiKeyCryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The saved remote profiles and which one is active, with each API key kept encrypted. */
@Singleton
class AiProfileStore @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val crypto: AiKeyCryptoManager
) {
    val profiles: Flow<List<AiProfile>> = prefs.aiProfilesJson.map(AiProfileList::decode)

    /** [AiProfileList.LOCAL] or the id of a profile. */
    val activeId: Flow<String> = prefs.aiActiveBackend

    /**
     * Stores [profile]. [plainKey] null keeps the key already saved; a blank one clears it; anything else
     * replaces it. [makeActive] is for a profile just created.
     */
    suspend fun save(profile: AiProfile, plainKey: String?, makeActive: Boolean) {
        val current = profiles.first()
        val old = current.firstOrNull { it.id == profile.id }
        val withKey = when {
            // Keep the saved key only while the address still points at the host it was given for
            plainKey == null -> if (AiProfileList.keepsKey(old, profile)) {
                profile.copy(encryptedKey = old?.encryptedKey, keyIv = old?.keyIv)
            } else {
                profile.copy(encryptedKey = null, keyIv = null)
            }
            else -> {
                val clean = AiProfileList.cleanKey(plainKey)
                if (clean == null) {
                    profile.copy(encryptedKey = null, keyIv = null)
                } else {
                    val (cipher, iv) = crypto.encrypt(clean)
                    profile.copy(encryptedKey = cipher.toHexString(), keyIv = iv.toHexString())
                }
            }
        }
        val stored = AiProfileList.applyEdit(old, withKey)
        val active = if (makeActive) stored.id else activeId.first()
        prefs.setAiProfilesAndActive(AiProfileList.encode(AiProfileList.upsert(current, stored)), active)
    }

    suspend fun delete(id: String) {
        val (list, active) = AiProfileList.remove(profiles.first(), activeId.first(), id)
        prefs.setAiProfilesAndActive(AiProfileList.encode(list), active)
    }

    suspend fun setActive(id: String) = prefs.setAiActiveBackend(id)

    suspend fun acceptConsent(id: String) {
        val current = profiles.first()
        val target = current.firstOrNull { it.id == id } ?: return
        prefs.setAiProfilesAndActive(
            AiProfileList.encode(AiProfileList.upsert(current, target.copy(consentGiven = true))),
            activeId.first()
        )
    }

    /** The decrypted key, or null when there is none or it can't be decrypted. */
    fun readKey(profile: AiProfile): String? {
        val cipher = profile.encryptedKey ?: return null
        val iv = profile.keyIv ?: return null
        return try {
            crypto.decrypt(cipher.hexToByteArray(), iv.hexToByteArray())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun keyState(profile: AiProfile): KeyState = when {
        profile.encryptedKey.isNullOrBlank() -> KeyState.NONE
        readKey(profile) != null -> KeyState.PRESENT
        else -> KeyState.UNREADABLE
    }
}
