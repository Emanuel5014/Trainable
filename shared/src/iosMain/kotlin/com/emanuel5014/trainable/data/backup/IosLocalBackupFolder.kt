@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.data.backup

import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSFileCoordinator
import platform.Foundation.NSFileCoordinatorReadingWithoutChanges
import platform.Foundation.NSFileCoordinatorWritingForDeleting
import platform.Foundation.NSFileCoordinatorWritingForReplacing
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLContentModificationDateKey
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970

/**
 * The folder chosen in Files for automatic local backups. The preference `auto_backup_folder_uri` holds a base64
 * bookmark of it (Android keeps a SAF tree URI there). Every call opens the security scope, coordinates with the
 * folder's file provider (iCloud Drive, other apps) and closes the scope again.
 */
class IosLocalBackupFolder(
    private val prefs: UserPreferencesRepository,
    private val archiver: BackupArchiver
) : LocalBackupFolder {

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val url = resolve() ?: return@withContext false
        access(url) { isDirectory(url) }
    }

    /** The folder's name as Files shows it, or null when no usable folder is chosen. */
    suspend fun folderLabel(): String? = withContext(Dispatchers.IO) {
        resolve()?.let { url -> access(url) { if (isDirectory(url)) url.lastPathComponent else null } }
    }

    override suspend fun exportTo(name: String, includeImages: Boolean): Boolean {
        if (!isAvailable()) return false
        val zip = archiver.createZip(name, includeImages) ?: return false
        return try {
            withContext(Dispatchers.IO) {
                val folder = resolve() ?: return@withContext false
                access(folder) {
                    val target = folder.URLByAppendingPathComponent(name) ?: return@access false
                    var copied = false
                    coordinate(target, write = NSFileCoordinatorWritingForReplacing) { url ->
                        val fm = NSFileManager.defaultManager
                        if (url.path?.let(fm::fileExistsAtPath) == true) fm.removeItemAtURL(url, null)
                        copied = fm.copyItemAtURL(NSURL.fileURLWithPath(zip), url, null)
                    }
                    copied
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        } finally {
            archiver.discard(zip)
        }
    }

    override suspend fun listAutoBackups(): List<Pair<String, Long>> = withContext(Dispatchers.IO) {
        val folder = resolve() ?: return@withContext emptyList()
        access(folder) {
            val found = mutableListOf<Pair<String, Long>>()
            coordinate(folder, read = true) { url ->
                val items = NSFileManager.defaultManager.contentsOfDirectoryAtURL(url, listOf(NSURLContentModificationDateKey), 0u, null)
                items.orEmpty().forEach { item ->
                    val file = item as NSURL
                    val name = file.lastPathComponent ?: return@forEach
                    if (name.startsWith(BackupNames.AUTO_PREFIX) && name.endsWith(".zip")) found += name to modifiedMillis(file)
                }
            }
            found
        }
    }

    override suspend fun delete(name: String) {
        withContext(Dispatchers.IO) {
            val folder = resolve() ?: return@withContext
            access(folder) {
                val target = folder.URLByAppendingPathComponent(name) ?: return@access
                coordinate(target, write = NSFileCoordinatorWritingForDeleting) { url ->
                    NSFileManager.defaultManager.removeItemAtURL(url, null)
                }
            }
        }
    }

    /** The bookmarked folder, its bookmark refreshed when the system marks it stale; null for anything else. */
    private suspend fun resolve(): NSURL? {
        val stored = prefs.autoBackupFolderUri.first()?.takeIf { it.isNotEmpty() } ?: return null
        val data = NSData.create(base64EncodedString = stored, options = 0u) ?: return null
        val (url, stale) = memScoped {
            val isStale = alloc<BooleanVar>()
            val error = alloc<ObjCObjectVar<NSError?>>()
            NSURL.URLByResolvingBookmarkData(data, 0u, null, isStale.ptr, error.ptr) to isStale.value
        }
        if (url != null && stale) bookmarkOf(url)?.let { prefs.setAutoBackupFolderUri(it) }
        return url
    }

    private fun <T> access(url: NSURL, block: () -> T): T {
        val opened = url.startAccessingSecurityScopedResource()
        try {
            return block()
        } finally {
            if (opened) url.stopAccessingSecurityScopedResource()
        }
    }

    /**
     * Runs [block] under file coordination. When the coordinator itself fails before running it (seen with
     * Cocoa error 512 where no coordination service runs, e.g. the headless test simulator), [block] runs on
     * [url] directly, as an uncoordinated file manager call would.
     */
    private fun coordinate(url: NSURL, read: Boolean = false, write: ULong = 0u, block: (NSURL) -> Unit) {
        val coordinator = NSFileCoordinator(filePresenter = null)
        var ran = false
        val run: (NSURL?) -> Unit = { granted -> ran = true; block(granted ?: url) }
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            if (read) {
                coordinator.coordinateReadingItemAtURL(url, NSFileCoordinatorReadingWithoutChanges, error.ptr, run)
            } else {
                coordinator.coordinateWritingItemAtURL(url, write, error.ptr, run)
            }
        }
        if (!ran) block(url)
    }

    private fun isDirectory(url: NSURL): Boolean = memScoped {
        val directory = alloc<BooleanVar>()
        val path = url.path ?: return@memScoped false
        NSFileManager.defaultManager.fileExistsAtPath(path, directory.ptr) && directory.value
    }

    private fun modifiedMillis(file: NSURL): Long = memScoped {
        val value = alloc<ObjCObjectVar<Any?>>()
        file.getResourceValue(value.ptr, NSURLContentModificationDateKey, null)
        ((value.value as? NSDate)?.timeIntervalSince1970 ?: 0.0).times(1000).toLong()
    }

    companion object {
        /** A base64 bookmark of [url] (a folder picked in Files), to store in `auto_backup_folder_uri`. */
        fun bookmarkOf(url: NSURL): String? {
            val opened = url.startAccessingSecurityScopedResource()
            try {
                return url.bookmarkDataWithOptions(0u, null, null, null)?.base64EncodedStringWithOptions(0u)
            } finally {
                if (opened) url.stopAccessingSecurityScopedResource()
            }
        }
    }
}
