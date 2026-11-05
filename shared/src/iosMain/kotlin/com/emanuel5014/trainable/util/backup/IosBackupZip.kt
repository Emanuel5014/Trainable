@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.util.backup

import okio.*
import okio.Path.Companion.toPath

/** ZIP container only; compression/decompression is Okio's native zlib, never a whole archive in memory. */
internal object IosBackupZip {
    const val MAX_TOTAL_BYTES = 2L * 1024 * 1024 * 1024
    private const val MAX_ENTRY_BYTES = 512L * 1024 * 1024
    private const val MAX_ENTRIES = 20_000
    private const val UINT_MAX = 0xffffffffL
    private const val FLAGS = 0x808 // UTF-8 and a data descriptor

    data class File(val name: String, val path: Path)
    private data class Entry(val name: String, val flags: Int, val method: Int, val crc: Long,
        val compressed: Long, val size: Long, val offset: Long)

    fun write(output: Path, files: List<File>, fs: FileSystem = FileSystem.SYSTEM) {
        require(files.size <= MAX_ENTRIES)
        require(files.map { it.name }.toSet().size == files.size)
        val entries = mutableListOf<Entry>()
        val target = fs.sink(output)
        val counter = object : Sink by target {
            var count = 0L
            override fun write(source: Buffer, byteCount: Long) { target.write(source, byteCount); count += byteCount }
        }
        counter.buffer().use { sink ->
            fun position(): Long = counter.count + sink.buffer.size
            var total = 0L
            for (file in files) {
                require(safeName(file.name) && !file.name.endsWith('/'))
                val name = file.name.encodeToByteArray()
                require(name.size <= 65535)
                val offset = position()
                require(offset <= UINT_MAX)
                sink.writeIntLe(0x04034b50).writeShortLe(20).writeShortLe(FLAGS).writeShortLe(8)
                    .writeShortLe(0).writeShortLe(33).writeIntLe(0).writeIntLe(0).writeIntLe(0)
                    .writeShortLe(name.size).writeShortLe(0).write(name)
                val start = position()
                var size = 0L
                val crc = Crc32()
                val leaveOpen = object : Sink by sink { override fun close() { sink.flush() } }
                DeflaterSink(leaveOpen, Deflater(-1, true)).buffer().use { compressed ->
                    fs.source(file.path).buffer().use { source ->
                        val bytes = ByteArray(8192)
                        while (true) {
                            val count = source.read(bytes)
                            if (count == -1) break
                            size += count
                            total += count
                            require(size <= MAX_ENTRY_BYTES && total <= MAX_TOTAL_BYTES) { "Backup is too large" }
                            crc.update(bytes, count)
                            compressed.write(bytes, 0, count)
                        }
                    }
                }
                val compressedSize = position() - start
                require(compressedSize <= UINT_MAX)
                sink.writeIntLe(0x08074b50).writeIntLe(crc.value.toInt())
                    .writeIntLe(compressedSize.toInt()).writeIntLe(size.toInt())
                entries += Entry(file.name, FLAGS, 8, crc.value, compressedSize, size, offset)
            }
            val central = position()
            for (entry in entries) {
                val name = entry.name.encodeToByteArray()
                sink.writeIntLe(0x02014b50).writeShortLe(20).writeShortLe(20).writeShortLe(entry.flags)
                    .writeShortLe(entry.method).writeShortLe(0).writeShortLe(33).writeIntLe(entry.crc.toInt())
                    .writeIntLe(entry.compressed.toInt()).writeIntLe(entry.size.toInt())
                    .writeShortLe(name.size).writeShortLe(0).writeShortLe(0).writeShortLe(0)
                    .writeShortLe(0).writeIntLe(0).writeIntLe(entry.offset.toInt()).write(name)
            }
            val centralSize = position() - central
            require(central <= UINT_MAX && centralSize <= UINT_MAX && position() + 22 <= MAX_TOTAL_BYTES)
            sink.writeIntLe(0x06054b50).writeShortLe(0).writeShortLe(0)
                .writeShortLe(entries.size).writeShortLe(entries.size)
                .writeIntLe(centralSize.toInt()).writeIntLe(central.toInt()).writeShortLe(0)
        }
    }

    /** Only known, flat backup paths are extracted; validate raw names before any path normalization. */
    fun extract(archive: Path, destination: Path, fs: FileSystem = FileSystem.SYSTEM): Set<String> {
        val (entries, centralOffset) = directory(archive, fs)
        val result = mutableSetOf<String>()
        var total = 0L
        for (entry in entries) {
            if (entry.name.endsWith('/') || !recognized(entry.name)) continue
            require(entry.size <= if (entry.name.endsWith(".json")) 2L * 1024 * 1024 else MAX_ENTRY_BYTES)
            total += entry.size
            require(total <= MAX_TOTAL_BYTES) { "Backup is too large" }
            val target = destination / entry.name
            fs.createDirectories(requireNotNull(target.parent))
            fs.openReadOnly(archive).use { handle ->
                handle.source(entry.offset).buffer().use { source ->
                    require(source.readIntLe() == 0x04034b50) { "Invalid ZIP local header" }
                    source.skip(2)
                    require(source.ushort() == entry.flags && source.ushort() == entry.method)
                    source.skip(16)
                    val nameLength = source.ushort()
                    val extraLength = source.ushort()
                    require(source.readUtf8(nameLength.toLong()) == entry.name)
                    source.skip(extraLength.toLong())
                    val start = entry.offset + 30 + nameLength + extraLength
                    require(start + entry.compressed <= centralOffset) { "Overlapping ZIP entry" }
                    val limited = LimitedSource(source, entry.compressed)
                    val decoded = if (entry.method == 8) InflaterSource(limited, Inflater(true)) else limited
                    decoded.buffer().use { input ->
                        val crc = Crc32()
                        var size = 0L
                        fs.sink(target).buffer().use { output ->
                            val bytes = ByteArray(8192)
                            while (true) {
                                val count = input.read(bytes)
                                if (count == -1) break
                                size += count
                                require(size <= entry.size) { "Invalid ZIP entry size" }
                                crc.update(bytes, count)
                                output.write(bytes, 0, count)
                            }
                        }
                        require(size == entry.size && crc.value == entry.crc) { "Damaged backup entry" }
                    }
                }
            }
            result += entry.name
        }
        require(BackupFormat.DATABASE in result) { "Backup database is missing" }
        return result
    }

    private fun directory(path: Path, fs: FileSystem): Pair<List<Entry>, Long> {
        val size = fs.metadata(path).size ?: error("Unreadable backup")
        require(size in 22..MAX_TOTAL_BYTES) { "Invalid backup size" }
        return fs.openReadOnly(path).use { handle ->
            val tailSize = minOf(size, 65557).toInt()
            val tail = handle.source(size - tailSize).buffer().use { it.readByteArray() }
            var end = -1
            for (i in tail.size - 22 downTo 0) {
                if (tail[i] == 0x50.toByte() && tail[i + 1] == 0x4b.toByte() &&
                    tail[i + 2] == 5.toByte() && tail[i + 3] == 6.toByte()) {
                    val length = (tail[i + 20].toInt() and 255) or ((tail[i + 21].toInt() and 255) shl 8)
                    if (i + 22 + length == tail.size) { end = i; break }
                }
            }
            require(end >= 0) { "Missing ZIP directory" }
            val footer = Buffer().write(tail, end + 4, tail.size - end - 4)
            require(footer.ushort() == 0 && footer.ushort() == 0) { "Multi-disk ZIP is unsupported" }
            val count = footer.ushort()
            require(count in 1..MAX_ENTRIES && footer.ushort() == count)
            val centralSize = footer.uint()
            val central = footer.uint()
            require(central + centralSize == size - tailSize + end) { "Invalid ZIP directory bounds" }
            val names = mutableSetOf<String>()
            val entries = handle.source(central).buffer().use { source ->
                var consumed = 0L
                List(count) {
                    require(source.readIntLe() == 0x02014b50)
                    source.skip(4)
                    val flags = source.ushort()
                    val method = source.ushort()
                    require(flags and 0x41 == 0 && method in setOf(0, 8)) { "Unsupported ZIP encoding" }
                    source.skip(4)
                    val crc = source.uint()
                    val compressed = source.uint()
                    val uncompressed = source.uint()
                    require(compressed != UINT_MAX && uncompressed <= MAX_ENTRY_BYTES)
                    if (method == 0) require(compressed == uncompressed)
                    val nameLength = source.ushort()
                    val extraLength = source.ushort()
                    val commentLength = source.ushort()
                    require(source.ushort() == 0)
                    source.skip(2)
                    val attributes = source.uint()
                    require((attributes ushr 16).toInt() and 0xf000 != 0xa000) { "ZIP symlinks are unsupported" }
                    val offset = source.uint()
                    val name = source.readByteArray(nameLength.toLong()).decodeToString(throwOnInvalidSequence = true)
                    require(safeName(name) && names.add(name.removeSuffix("/"))) { "Invalid or repeated ZIP path" }
                    require(offset + 30 + compressed <= central)
                    source.skip(extraLength.toLong() + commentLength)
                    consumed += 46L + nameLength + extraLength + commentLength
                    require(consumed <= centralSize)
                    Entry(name, flags, method, crc, compressed, uncompressed, offset)
                }.also { require(consumed == centralSize) }
            }
            entries to central
        }
    }

    private fun recognized(name: String): Boolean = name in setOf(BackupFormat.DATABASE,
        BackupFormat.DATABASE + "-wal", BackupFormat.DATABASE + "-shm", BackupFormat.SETTINGS,
        BackupFormat.ANALYTICS_SETTINGS, BackupFormat.APP_PREFERENCES) ||
        listOf(BackupFormat.IMAGES, BackupFormat.EXERCISE_MEDIA, BackupFormat.PHYSICAL_CHECKS).any { prefix ->
            name.startsWith(prefix) && '/' !in name.removePrefix(prefix)
        }

    private fun safeName(name: String): Boolean = name.isNotBlank() && !name.startsWith('/') &&
        '\\' !in name && '\u0000' !in name && ':' !in name &&
        name.removeSuffix("/").split('/').all { it.isNotBlank() && it != "." && it != ".." }

    private fun BufferedSource.ushort(): Int = readShortLe().toInt() and 0xffff
    private fun BufferedSource.uint(): Long = readIntLe().toLong() and UINT_MAX

    private class LimitedSource(source: Source, private var remaining: Long) : ForwardingSource(source) {
        override fun read(sink: Buffer, byteCount: Long): Long {
            if (byteCount == 0L) return 0
            if (remaining == 0L) return -1
            val read = super.read(sink, minOf(byteCount, remaining))
            require(read != -1L) { "Truncated ZIP entry" }
            remaining -= read
            return read
        }
    }
}

private class Crc32 {
    private var crc = -1
    val value: Long get() = crc.inv().toLong() and 0xffffffffL
    fun update(bytes: ByteArray, count: Int) {
        for (i in 0 until count) crc = table[(crc xor bytes[i].toInt()) and 255] xor (crc ushr 8)
    }
    companion object {
        private val table = IntArray(256) { value ->
            var crc = value
            repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0xedb88320.toInt() else crc ushr 1 }
            crc
        }
    }
}
