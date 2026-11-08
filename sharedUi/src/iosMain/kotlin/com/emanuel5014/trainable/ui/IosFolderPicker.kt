@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.interop.LocalUIViewController
import com.emanuel5014.trainable.data.backup.IosLocalBackupFolder
import kotlinx.coroutines.launch
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.darwin.NSObject

/**
 * Opens Files to choose the automatic-backup folder and hands over its bookmark, by default into
 * `auto_backup_folder_uri` (Android's "choose folder" opens the SAF tree picker and keeps the tree URI there).
 * The onboarding keeps it in its choices until the setup finishes.
 */
@Composable
internal fun rememberIosBackupFolderPicker(onPicked: ((String) -> Unit)? = null): () -> Unit {
    val scope = rememberCoroutineScope()
    val presenter = LocalUIViewController.current
    val picked by rememberUpdatedState(onPicked)
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val folder = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
                val bookmark = IosLocalBackupFolder.bookmarkOf(folder) ?: return
                picked?.invoke(bookmark) ?: scope.launch { IosAppGraph.preferences.setAutoBackupFolderUri(bookmark) }
            }
        }
    }
    return remember(presenter) {
        {
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeFolder))
            picker.delegate = delegate
            picker.allowsMultipleSelection = false
            topController(presenter).presentViewController(picker, true, null)
        }
    }
}

private fun topController(root: UIViewController): UIViewController {
    var top = root
    while (top.presentedViewController != null) top = top.presentedViewController!!
    return top
}
