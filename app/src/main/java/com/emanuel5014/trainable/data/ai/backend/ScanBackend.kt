package com.emanuel5014.trainable.data.ai.backend

import android.net.Uri
import com.emanuel5014.trainable.data.ai.LlmScanResult
import com.emanuel5014.trainable.data.ai.ScanImageMode

/** Everything a backend needs to read one photo; the scanners build it, each backend interprets it. */
data class ScanRequest(
    val imageUri: Uri,
    val prompt: String,
    val imageMode: ScanImageMode,
    /** Copy the text off the picture: near-greedy sampling and a cap on the answer. */
    val transcription: Boolean,
    val maxOutputTokens: Int?
)

/** Where a photo of a sheet is read: the model on the phone or a remote provider. */
interface ScanBackend {
    /** True for the on-device model; drives the first phase label and the phone's live metrics. */
    val isLocal: Boolean

    /** Gets the backend ready (loads the model, checks the profile). May throw [ScanBackendException]. */
    suspend fun prepare()

    /** Reads the photo; [onStreamUpdate] gets the cumulative (output, thinking) text. Cancellable. */
    suspend fun run(request: ScanRequest, onStreamUpdate: (String, String) -> Unit): LlmScanResult

    suspend fun release()
}
