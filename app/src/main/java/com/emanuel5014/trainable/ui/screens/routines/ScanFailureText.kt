package com.emanuel5014.trainable.ui.screens.routines

import android.content.Context
import androidx.annotation.StringRes
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException

/** What went wrong with a scan, in words the user can act on. */
@StringRes
fun ScanBackendException.messageRes(): Int = when (this) {
    is ScanBackendException.ModelMissing -> R.string.ai_err_model_missing
    is ScanBackendException.NotConfigured -> R.string.ai_err_not_configured
    is ScanBackendException.KeyUnreadable -> R.string.ai_err_key_unreadable
    is ScanBackendException.PolicyBlocked -> R.string.ai_err_policy_blocked
    is ScanBackendException.Unauthorized -> R.string.ai_err_unauthorized
    is ScanBackendException.ModelNotFound -> R.string.ai_err_model_not_found
    is ScanBackendException.RateLimited -> R.string.ai_err_rate_limited
    is ScanBackendException.PayloadTooLarge -> R.string.ai_err_too_large
    is ScanBackendException.Rejected -> R.string.ai_err_rejected
    is ScanBackendException.Server -> R.string.ai_err_server
    is ScanBackendException.Unreachable -> R.string.ai_err_unreachable
    is ScanBackendException.StreamError -> R.string.ai_err_stream
    is ScanBackendException.BadResponse -> R.string.ai_err_bad_response
}

/** The message, then the provider's own words when it gave any. */
fun ScanBackendException.userText(context: Context): String {
    val message = context.getString(messageRes())
    return detail?.let { "$message\n$it" } ?: message
}
