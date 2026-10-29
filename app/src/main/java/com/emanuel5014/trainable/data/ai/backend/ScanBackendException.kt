package com.emanuel5014.trainable.data.ai.backend

import com.emanuel5014.trainable.data.ai.remote.UrlRejection

/**
 * Why a scan could not be read, in terms the user can act on. [detail] is the provider's own message,
 * already shortened and stripped of the API key.
 */
sealed class ScanBackendException(val detail: String? = null, cause: Throwable? = null) : Exception(detail, cause) {
    class ModelMissing : ScanBackendException()
    class NotConfigured : ScanBackendException()
    class KeyUnreadable : ScanBackendException()
    class PolicyBlocked(val rejection: UrlRejection) : ScanBackendException()
    class Unauthorized(detail: String?) : ScanBackendException(detail)
    class ModelNotFound(detail: String?) : ScanBackendException(detail)
    class RateLimited(detail: String?) : ScanBackendException(detail)
    class PayloadTooLarge(detail: String? = null) : ScanBackendException(detail)
    class Rejected(val code: Int, detail: String?) : ScanBackendException(detail)
    class Server(val code: Int, detail: String?) : ScanBackendException(detail)
    class Unreachable(cause: Throwable) : ScanBackendException(cause.message, cause)
    class StreamError(detail: String?) : ScanBackendException(detail)
    class BadResponse(detail: String? = null) : ScanBackendException(detail)
}
