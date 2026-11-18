@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.interop.LocalUIViewController
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.export_failed
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.screens.routines.SharePlans
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

/** The existing routine-list share action, with the same JSON/options/filename as Android. */
@Composable
internal fun rememberIosRoutineSharing(): SharePlans {
    val presenter = LocalUIViewController.current
    val showMessage by rememberUpdatedState(LocalShowMessage.current)
    val failedMessage by rememberUpdatedState(stringResource(Res.string.export_failed))
    val session = remember(presenter) { IosRoutineShareSession() }

    DisposableEffect(presenter) {
        onDispose {
            session.disposed = true
            session.sheet?.dismissViewControllerAnimated(false, null)
            session.cleanup()
        }
    }

    return remember(session) {
        val share: SharePlans = share@{ ids, includeImages, includeMaximums, fileName ->
            if (session.disposed || session.directory != null) return@share
            val folder = NSTemporaryDirectory() + "routine-share-" + NSUUID().UUIDString
            session.directory = folder
            var presented = false
            try {
                val url = withContext(Dispatchers.IO) {
                    val json = IosAppGraph.workouts.exportPlans(ids, includeImages, includeMaximums)
                    writeIosRoutineShareFile(folder, fileName, json)
                }
                if (session.disposed) return@share
                val controller = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
                controller.completionWithItemsHandler = { _, _, _, error ->
                    session.cleanup()
                    if (error != null) showMessage(failedMessage)
                }
                var top = presenter
                while (top.presentedViewController != null) top = top.presentedViewController!!
                controller.popoverPresentationController?.apply {
                    sourceView = top.view
                    sourceRect = CGRectMake(top.view.bounds.useContents { size.width / 2 }, top.view.bounds.useContents { size.height / 2 }, 0.0, 0.0)
                }
                session.sheet = controller
                top.presentViewController(controller, animated = true, completion = null)
                presented = true
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { showMessage(failedMessage) }
            finally {
                if (!presented) {
                    session.cleanup()
                    // Disposal may have run before the background write finished creating its directory.
                    NSFileManager.defaultManager.removeItemAtPath(folder, null)
                }
            }
        }
        share
    }
}

/** Keeps UIKit's completion callback and its file alive, independent of recompositions. */
private class IosRoutineShareSession {
    var disposed = false
    var directory: String? = null
    var sheet: UIActivityViewController? = null
    fun cleanup() {
        sheet?.completionWithItemsHandler = null
        sheet = null
        directory?.let { NSFileManager.defaultManager.removeItemAtPath(it, null) }
        directory = null
    }
}

/** Only the app-owned unique folder is removed; Files/AirDrop recipients own independent copies. */
internal fun writeIosRoutineShareFile(directory: String, fileName: String, json: String): NSURL {
    require(Regex("[a-z0-9-]+\\.trainableplan").matches(fileName))
    check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null))
    val url = NSURL.fileURLWithPath("$directory/$fileName")
    val bytes = json.encodeToByteArray()
    require(bytes.isNotEmpty())
    val data = bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) }
    check(data.writeToURL(url, atomically = true))
    return url
}
