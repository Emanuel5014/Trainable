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
 * Routine photos as files in Documents/routine_images (like Android's filesDir/routine_images). The widgets and the
 * CSV export's own words are not on iOS yet: no refresh, and techniques by their stored code.
 */
class IosWorkoutPlatform : WorkoutPlatform {
    override fun encodeImage(uri: String): String? {
        // A photo copied from an Android backup points to a path that does not exist here
        val path = uri.removePrefix("file://")
        return NSData.dataWithContentsOfFile(path)?.base64EncodedStringWithOptions(0u)
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

/** Exercise media in Documents/exercise_media. Picking one is not on iOS yet. */
class IosExerciseMediaFiles : ExerciseMediaFiles {
    override fun import(source: String, exerciseId: Int): ExerciseMediaImport = ExerciseMediaImport.Unsupported

    @OptIn(ExperimentalForeignApi::class)
    override fun delete(fileName: String?) {
        // Only a bare name inside the media folder, as on Android
        if (fileName == null || fileName.contains('/')) return
        NSFileManager.defaultManager.removeItemAtPath(documentsFolder("exercise_media") + "/" + fileName, error = null)
    }
}

/** One write transaction of the iOS database, as Room's withTransaction is on Android. */
class IosTransactionRunner(private val database: com.emanuel5014.trainable.data.local.GymDatabase) : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R =
        database.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
}
