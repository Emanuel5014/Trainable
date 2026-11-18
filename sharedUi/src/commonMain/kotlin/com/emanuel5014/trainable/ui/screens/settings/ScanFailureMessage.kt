package com.emanuel5014.trainable.ui.screens.settings

import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.ai_err_bad_response
import com.emanuel5014.trainable.sharedui.resources.ai_err_key_unreadable
import com.emanuel5014.trainable.sharedui.resources.ai_err_model_missing
import com.emanuel5014.trainable.sharedui.resources.ai_err_model_not_found
import com.emanuel5014.trainable.sharedui.resources.ai_err_not_configured
import com.emanuel5014.trainable.sharedui.resources.ai_err_policy_blocked
import com.emanuel5014.trainable.sharedui.resources.ai_err_rate_limited
import com.emanuel5014.trainable.sharedui.resources.ai_err_rejected
import com.emanuel5014.trainable.sharedui.resources.ai_err_server
import com.emanuel5014.trainable.sharedui.resources.ai_err_stream
import com.emanuel5014.trainable.sharedui.resources.ai_err_too_large
import com.emanuel5014.trainable.sharedui.resources.ai_err_unauthorized
import com.emanuel5014.trainable.sharedui.resources.ai_err_unreachable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** What went wrong with a scan or a provider check, in words the user can act on (Android's ScanFailureText, shared). */
fun ScanBackendException.messageResource(): StringResource = when (this) {
    is ScanBackendException.ModelMissing -> Res.string.ai_err_model_missing
    is ScanBackendException.NotConfigured -> Res.string.ai_err_not_configured
    is ScanBackendException.KeyUnreadable -> Res.string.ai_err_key_unreadable
    is ScanBackendException.PolicyBlocked -> Res.string.ai_err_policy_blocked
    is ScanBackendException.Unauthorized -> Res.string.ai_err_unauthorized
    is ScanBackendException.ModelNotFound -> Res.string.ai_err_model_not_found
    is ScanBackendException.RateLimited -> Res.string.ai_err_rate_limited
    is ScanBackendException.PayloadTooLarge -> Res.string.ai_err_too_large
    is ScanBackendException.Rejected -> Res.string.ai_err_rejected
    is ScanBackendException.Server -> Res.string.ai_err_server
    is ScanBackendException.Unreachable -> Res.string.ai_err_unreachable
    is ScanBackendException.StreamError -> Res.string.ai_err_stream
    is ScanBackendException.BadResponse -> Res.string.ai_err_bad_response
}

/** The message, then the provider's own words when it gave any. */
suspend fun ScanBackendException.userMessage(): String {
    val message = getString(messageResource())
    return detail?.let { "$message\n$it" } ?: message
}
