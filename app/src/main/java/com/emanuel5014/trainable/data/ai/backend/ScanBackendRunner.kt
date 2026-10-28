package com.emanuel5014.trainable.data.ai.backend

import com.emanuel5014.trainable.data.ai.LlmScanResult
import com.emanuel5014.trainable.data.ai.ScanPhase
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** The prepare → run → release sequence both scanners share, whichever backend reads the photo. */
object ScanBackendRunner {

    suspend fun execute(
        backend: ScanBackend,
        request: ScanRequest,
        onPhase: (ScanPhase) -> Unit,
        onStreamUpdate: (String, String) -> Unit
    ): LlmScanResult {
        try {
            onPhase(if (backend.isLocal) ScanPhase.LOADING_MODEL else ScanPhase.CONNECTING)
            backend.prepare()
            onPhase(ScanPhase.READING_SHEET)
            return backend.run(request, onStreamUpdate)
        } finally {
            withContext(NonCancellable) { backend.release() }
        }
    }
}
