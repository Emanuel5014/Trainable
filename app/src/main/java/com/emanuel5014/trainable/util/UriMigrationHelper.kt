package com.emanuel5014.trainable.util

import android.content.Context

object UriMigrationHelper {
    private const val OLD_PACKAGE = "com.example.gymtracking"

    /**
     * Fixes image paths that might contain the old package name after a database import.
     */
    fun fixPath(path: String?, context: Context): String? {
        if (path == null) return null
        val newPackage = context.packageName
        return if (path.contains(OLD_PACKAGE)) {
            path.replace(OLD_PACKAGE, newPackage)
        } else {
            path
        }
    }
}
