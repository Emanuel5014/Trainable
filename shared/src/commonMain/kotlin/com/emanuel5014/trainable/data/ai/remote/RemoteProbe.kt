package com.emanuel5014.trainable.data.ai.remote

import com.emanuel5014.trainable.data.ai.backend.ScanBackendException

/** The two quick checks the provider editor offers: which models the server lists, and whether a request works. */
class RemoteProbe(private val http: AiHttp) {

    suspend fun listModels(client: ProtocolClient): Result<List<String>> = outcome {
        http.call(client.buildModelsRequest()) { response ->
            val body = response.bodyString()
            if (!response.isSuccessful) throw client.mapError(response.code, body)
            client.parseModels(body)
        }
    }

    /** A tiny text request; retried once if the provider refuses a parameter (same rule as a scan). */
    suspend fun ping(client: ProtocolClient): Result<Unit> = outcome {
        var quirks = RequestQuirks()
        var retried = false
        while (true) {
            val failure = http.call(client.buildPingRequest(quirks)) { response ->
                if (response.isSuccessful) null else response.code to response.bodyString()
            } ?: return@outcome
            val next = if (retried) null else client.retryQuirks(failure.first, failure.second, quirks)
            if (next == null) throw client.mapError(failure.first, failure.second)
            quirks = next
            retried = true
        }
    }

    /** Only a [ScanBackendException] is an answer; a cancellation or a bug must still propagate. */
    private inline fun <T> outcome(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: ScanBackendException) {
        Result.failure(e)
    } catch (_: AiRequestUnusable) {
        // A request that can't even be built: the address is malformed
        Result.failure(ScanBackendException.PolicyBlocked(UrlRejection.INVALID))
    }
}
