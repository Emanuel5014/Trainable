@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.data.backup

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_get_global_queue
import kotlin.concurrent.AtomicInt

/**
 * Whether the current network is "expensive" (mobile data, a phone hotspot): the iOS reading of Android's metered
 * network, used for the "Wi-Fi only" Nextcloud setting.
 */
object IosNetworkPath {
    private val expensive = AtomicInt(0)
    private val firstUpdate = CompletableDeferred<Unit>()

    private val monitor by lazy {
        nw_path_monitor_create().also { monitor ->
            nw_path_monitor_set_update_handler(monitor) { path ->
                expensive.value = if (nw_path_is_expensive(path)) 1 else 0
                firstUpdate.complete(Unit)
            }
            nw_path_monitor_set_queue(monitor, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u))
            nw_path_monitor_start(monitor)
        }
    }

    fun start() { monitor }

    /** Waits up to 2 s for the first reading after start (a cold background launch), then answers. */
    suspend fun isExpensive(): Boolean {
        start()
        withTimeoutOrNull(2_000) { firstUpdate.await() }
        return expensive.value == 1
    }
}
