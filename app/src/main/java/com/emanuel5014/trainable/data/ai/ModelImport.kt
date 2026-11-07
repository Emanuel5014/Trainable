package com.emanuel5014.trainable.data.ai

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.io.InputStream

/** The first [ModelFileValidator.HEADER_BYTES] bytes of [input], or all of it when it is shorter. */
fun readModelHeader(input: InputStream): ByteArray {
    val buffer = ByteArray(ModelFileValidator.HEADER_BYTES)
    var read = 0
    while (read < buffer.size) {
        val count = input.read(buffer, read, buffer.size - read)
        if (count < 0) break
        read += count
    }
    return buffer.copyOf(read)
}

/** The source ended before, or ran past, the size it was checked with: what was copied is not that file. */
class IncompleteModelCopy(val copied: Long, val expected: Long) :
    IOException("Copied $copied bytes of $expected")

/**
 * Copies [input] to [target] and tells [onProgress] the fraction done whenever it passes a whole percent (never at 0).
 * Stops as soon as the coroutine is cancelled. The caller removes [target] when this throws.
 *
 * @throws IncompleteModelCopy when the source is not exactly [expectedBytes] long.
 */
suspend fun copyModel(
    input: InputStream,
    target: File,
    expectedBytes: Long,
    bufferSize: Int = 1 shl 20,
    onProgress: (Float) -> Unit
) {
    val buffer = ByteArray(bufferSize)
    var copied = 0L
    var lastPercent = 0
    target.outputStream().use { output ->
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            copied += read
            // A source that runs past its size is not the file that was checked
            if (copied > expectedBytes) throw IncompleteModelCopy(copied, expectedBytes)
            val percent = (copied * 100 / expectedBytes).toInt()
            if (percent > lastPercent) {
                lastPercent = percent
                onProgress(copied.toFloat() / expectedBytes)
            }
        }
    }
    if (copied != expectedBytes) throw IncompleteModelCopy(copied, expectedBytes)
}
