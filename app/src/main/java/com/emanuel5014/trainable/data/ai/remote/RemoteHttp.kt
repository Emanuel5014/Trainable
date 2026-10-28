package com.emanuel5014.trainable.data.ai.remote

import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Runs one OkHttp call off the main thread and closes it the moment the coroutine is cancelled. */
internal class RemoteHttp(private val factory: Call.Factory) {

    /**
     * Executes [request] and hands the response to [read] (which may stream the body). A network failure
     * becomes [ScanBackendException.Unreachable]; a cancellation stays a cancellation.
     */
    suspend fun <T> call(request: Request, read: (Response) -> T): T = coroutineScope {
        val call = factory.newCall(request)
        val finished = AtomicBoolean(false)
        // Reading blocks a thread, so cancelling has to reach the call from outside
        val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                if (!finished.get()) call.cancel()
            }
        }
        try {
            withContext(Dispatchers.IO) { call.execute().use(read) }
        } catch (e: IOException) {
            ensureActive()
            throw ScanBackendException.Unreachable(e)
        } finally {
            finished.set(true)
            watcher.cancel()
        }
    }
}
