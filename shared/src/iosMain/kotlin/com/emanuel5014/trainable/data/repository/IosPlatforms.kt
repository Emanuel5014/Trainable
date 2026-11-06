package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.domain.prescription.TechniqueCodec
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSDataBase64DecodingIgnoreUnknownCharacters
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUUID
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

/** A folder inside the app's Documents, created when missing. */
@OptIn(ExperimentalForeignApi::class)
internal fun documentsFolder(name: String): String {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    val path = requireNotNull(documents?.path) + "/" + name
    NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
    return path
}

/**
 * Routine photos as files in Documents/routine_images (like Android's filesDir/routine_images).
 * Widgets are not on iOS yet. CSV export supplies its own localized technique labels.
 */
class IosWorkoutPlatform : WorkoutPlatform {
    @OptIn(ExperimentalForeignApi::class)
    override fun encodeImage(uri: String): String? {
        val documents = NSFileManager.defaultManager.URLForDirectory(
            NSDocumentDirectory, NSUserDomainMask, null, false, null
        )?.path ?: return null
        return readIosRoutineImage(uri, documents)?.base64EncodedStringWithOptions(0u)
    }

    override fun saveImage(base64: String): String? {
        val data = NSData.create(base64EncodedString = base64, options = NSDataBase64DecodingIgnoreUnknownCharacters)
            ?: return null
        val path = documentsFolder("routine_images") + "/routine_" + NSUUID().UUIDString + ".jpg"
        return if (data.writeToFile(path, atomically = true)) "file://$path" else null
    }

    override fun onWorkoutsChanged() {}

    override fun techniqueLabel(technique: Technique): String = TechniqueCodec.encode(listOf(technique)).orEmpty()

    override val noTechniqueLabel: String get() = "-"
}

/** Backup paths can belong to Android or an old iOS container. Resolve only photos in our own storage. */
fun readIosRoutineImage(uri: String, documents: String): NSData? =
    iosRoutineImagePaths(uri, documents).firstNotNullOfOrNull { NSData.dataWithContentsOfFile(it) }

/** Where a stored routine photo path can be in this app's Documents, by its file name; empty for non-photos. */
private fun iosRoutineImagePaths(uri: String, documents: String): List<String> {
    val name = NSURL(string = uri)?.lastPathComponent ?: uri.substringAfterLast('/')
    if (name.isNullOrBlank() || name.substringAfterLast('.', "").lowercase() !in
        setOf("jpg", "jpeg", "png", "webp", "heic", "gif")) return emptyList()
    return listOf("$documents/routine_images/$name", "$documents/$name")
}

/** The app's Documents folder path. */
@OptIn(ExperimentalForeignApi::class)
fun iosDocumentsPath(): String? = NSFileManager.defaultManager.URLForDirectory(
    NSDocumentDirectory, NSUserDomainMask, null, false, null
)?.path

/**
 * Stores a routine photo (bytes already a small upright JPEG) as Android does: Documents/routine_images/
 * routine_<millis>.jpg. Returns the path saved in the database, or null when it cannot be written.
 */
fun saveIosRoutineImage(bytes: ByteArray): String? {
    val folder = documentsFolder("routine_images")
    val millis = kotlin.time.Clock.System.now().toEpochMilliseconds()
    val path = generateSequence(0) { it + 1 }
        .map { n -> if (n == 0) "$folder/routine_$millis.jpg" else "$folder/routine_${millis}_$n.jpg" }
        .first { !NSFileManager.defaultManager.fileExistsAtPath(it) }
    return if (bytes.toNSData().writeToFile(path, atomically = true)) path else null
}

/** Deletes a routine photo's file, only inside Documents/routine_images (like Android, which deletes only its own files). */
@OptIn(ExperimentalForeignApi::class)
fun deleteIosRoutineImage(uri: String) {
    val documents = iosDocumentsPath() ?: return
    iosRoutineImagePaths(uri, documents).firstOrNull()
        ?.takeIf { NSFileManager.defaultManager.fileExistsAtPath(it) }
        ?.let { NSFileManager.defaultManager.removeItemAtPath(it, null) }
}

/** One write transaction of the iOS database, as Room's withTransaction is on Android. */
class IosTransactionRunner(private val database: com.emanuel5014.trainable.data.local.GymDatabase) : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R =
        database.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
}

/** A [KeyValueStore] in the user defaults, its keys prefixed with [name] like the Android file they mirror. */
class UserDefaultsStore(private val name: String) : KeyValueStore {
    private val defaults = platform.Foundation.NSUserDefaults.standardUserDefaults
    private fun key(key: String) = "$name.$key"

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key(key)) == null) default else defaults.boolForKey(key(key))

    override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, key(key))

    override fun getString(key: String, default: String?): String? = defaults.stringForKey(key(key)) ?: default

    override fun putString(key: String, value: String) = defaults.setObject(value, key(key))

    override fun getStringSet(key: String, default: Set<String>?): Set<String>? =
        defaults.stringArrayForKey(key(key))?.map { it as String }?.toSet() ?: default

    override fun putStringSet(key: String, value: Set<String>) = defaults.setObject(value.toList(), key(key))
}
