package com.emanuel5014.trainable.data.ai.backend

import com.emanuel5014.trainable.data.ai.remote.AiProfile
import com.emanuel5014.trainable.data.ai.remote.AiProfileList
import com.emanuel5014.trainable.data.ai.remote.BaseUrlPolicy
import com.emanuel5014.trainable.data.ai.remote.HostKind
import com.emanuel5014.trainable.data.ai.remote.UrlCheck
import com.emanuel5014.trainable.data.ai.remote.isComplete

/** What the user is asked to accept before a photo is sent to [host]. */
data class ConsentRequest(val profileId: String, val profileName: String, val host: String)

/** Whether the scan can run, and whether the user still has to agree to where it runs; no Android in here. */
object ScanReadiness {

    /** The profile the scan runs on, or null for the on-device model (also when the id no longer exists). */
    fun resolveActive(activeId: String, profiles: List<AiProfile>): AiProfile? =
        if (activeId == AiProfileList.LOCAL) null else profiles.firstOrNull { it.id == activeId }

    fun isReady(
        scanEnabled: Boolean,
        activeId: String,
        profiles: List<AiProfile>,
        localModelDownloaded: Boolean
    ): Boolean {
        if (!scanEnabled) return false
        val profile = resolveActive(activeId, profiles) ?: return localModelDownloaded
        return profile.isComplete()
    }

    /** Set when [active] sends photos to a public host the user hasn't agreed to yet. */
    fun consentRequired(active: AiProfile?): ConsentRequest? {
        if (active == null || active.consentGiven) return null
        val check = BaseUrlPolicy.check(active.baseUrl) as? UrlCheck.Ok ?: return null
        if (check.kind != HostKind.PUBLIC) return null
        return ConsentRequest(active.id, active.name, check.url.host)
    }
}
