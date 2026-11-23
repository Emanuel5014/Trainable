package com.emanuel5014.trainable.util.backup

import java.io.File

/**
 * Where the entries of an imported ZIP may be written. A backup is a file from outside (shared, downloaded), so an
 * entry such as `physical_checks/../shared_prefs/x.xml` must never reach a path out of its own folder.
 */
object BackupEntryPaths {

    /** The database files a backup carries; nothing else is written next to the database. */
    val databaseFiles: Set<String> = setOf(
        BackupFormat.DATABASE,
        "${BackupFormat.DATABASE}-wal",
        "${BackupFormat.DATABASE}-shm",
        "${BackupFormat.DATABASE}-journal"
    )

    /** [name] when it is one plain file name (no folder, no `.` or `..`), else null. */
    fun plainName(name: String): String? =
        name.takeIf { it.isNotBlank() && it != "." && it != ".." && '/' !in it && '\\' !in it && '\u0000' !in it }

    /** The file [name] directly inside [dir], or null when [name] is not a plain name or would end up elsewhere. */
    fun childOf(dir: File, name: String): File? {
        val plain = plainName(name) ?: return null
        val file = File(dir, plain)
        return file.takeIf { it.canonicalFile.parentFile == dir.canonicalFile }
    }
}
