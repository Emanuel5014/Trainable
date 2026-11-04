@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.data.repository.ExerciseMediaImport
import com.emanuel5014.trainable.data.repository.IosExerciseMediaFiles
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import kotlin.test.*

/** File tests use only fresh temporary directories; neither a user database nor the photo library is touched. */
class IosExerciseMediaTest {
    private fun inFolder(block: (String) -> Unit) {
        val path = NSTemporaryDirectory() + "TrainableMediaTest_" + NSUUID().UUIDString
        val manager = NSFileManager.defaultManager
        assertTrue(manager.createDirectoryAtPath(path, true, null, null))
        try { block(path) } finally { manager.removeItemAtPath(path, null) }
    }

    private fun storage(folder: String): IosExerciseMediaFiles = IosExerciseMediaFiles { folder }

    @Test
    fun stillImagesAreDownscaledAndImportsDoNotCollide() = inFolder { folder ->
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(2560.0, 1000.0), false, 1.0)
        val image = try {
            UIColor.redColor.setFill()
            UIRectFill(CGRectMake(0.0, 0.0, 2560.0, 1000.0))
            assertNotNull(UIGraphicsGetImageFromCurrentImageContext())
        } finally { UIGraphicsEndImageContext() }
        val source = "$folder/source.png"
        assertTrue(assertNotNull(UIImagePNGRepresentation(image)).writeToFile(source, true))
        val files = storage(folder)
        val first = assertIs<ExerciseMediaImport.Saved>(files.import(source, 7))
        val second = assertIs<ExerciseMediaImport.Saved>(files.import(NSURL.fileURLWithPath(source).absoluteString!!, 7))
        assertNotEquals(first.fileName, second.fileName)
        assertTrue(NSFileManager.defaultManager.fileExistsAtPath(source))
        val saved = assertNotNull(UIImage.imageWithContentsOfFile("$folder/${first.fileName}"))
        saved.size.useContents { assertEquals(1280.0, width); assertEquals(500.0, height) }
        files.delete(first.fileName)
        assertFalse(NSFileManager.defaultManager.fileExistsAtPath("$folder/${first.fileName}"))
        assertTrue(NSFileManager.defaultManager.fileExistsAtPath("$folder/${second.fileName}"))
    }

    @Test
    fun gifFramesAndTheirTimingArePreserved() = inFolder { folder ->
        val data = assertNotNull(NSData.create(base64EncodedString = GIF, options = 0u))
        val path = "$folder/input.gif"
        assertTrue(data.writeToFile(path, true))
        val saved = assertIs<ExerciseMediaImport.Saved>(storage(folder).import(path, 8))
        assertTrue(saved.fileName.endsWith(".gif"))
        val copied = assertNotNull(NSData.dataWithContentsOfFile("$folder/${saved.fileName}"))
        assertTrue(data.isEqualToData(copied))
        val source = assertNotNull(IosExerciseMediaSource.open("$folder/${saved.fileName}"))
        try {
            assertEquals(2, source.count)
            assertEquals(100L, source.durationMillis(0))
            assertEquals(150L, source.durationMillis(1))
            val first = assertNotNull(source.frame(0, 64))
            val second = assertNotNull(source.frame(1, 64))
            assertTrue(first.animated)
            assertEquals(1f, first.aspect)
            assertFalse(assertNotNull(UIImagePNGRepresentation(first.image)).isEqualToData(
                assertNotNull(UIImagePNGRepresentation(second.image))))
        } finally { source.close() }
    }

    @Test
    fun largeAndMalformedFilesAreRejectedWithoutRemovingTheirSources() = inFolder { folder ->
        val manager = NSFileManager.defaultManager
        val large = "$folder/large"
        assertTrue(manager.createFileAtPath(large, null, null))
        val handle = assertNotNull(NSFileHandle.fileHandleForWritingAtPath(large))
        try { handle.truncateFileAtOffset(15uL * 1024uL * 1024uL + 1uL) } finally { handle.closeFile() }
        val files = storage(folder)
        assertEquals(ExerciseMediaImport.TooLarge, files.import(large, 9))
        val bad = "$folder/bad"
        assertTrue(assertNotNull(NSData.create(base64EncodedString = "bm90IGFuIGltYWdl", options = 0u)).writeToFile(bad, true))
        assertEquals(ExerciseMediaImport.Unsupported, files.import(bad, 9))
        assertEquals(ExerciseMediaImport.Unsupported, files.import("https://example.invalid/image.png", 9))
        assertTrue(manager.fileExistsAtPath(large))
        assertTrue(manager.fileExistsAtPath(bad))
    }

    @Test
    fun invalidNamesCannotDeleteAParentOrSibling() = inFolder { root ->
        val manager = NSFileManager.defaultManager
        val media = "$root/media"
        assertTrue(manager.createDirectoryAtPath(media, true, null, null))
        val sentinel = "$root/sentinel"
        assertTrue(manager.createFileAtPath(sentinel, null, null))
        val files = storage(media)
        listOf("", ".", "..", "../sentinel", "../../sentinel", "\\sentinel", "/sentinel").forEach(files::delete)
        assertTrue(manager.fileExistsAtPath(sentinel))
        assertTrue(manager.fileExistsAtPath(media))
    }

    companion object { private const val GIF = "R0lGODlhAQABAIAAAAAAAP///yH/C05FVFNDQVBFMi4wAwEAAAAh+QQACgAAACwAAAAAAQABAAACAkQBACH5BAAPAAAALAAAAAABAAEAAAICTAEAOw==" }
}
