package com.emanuel5014.trainable.util

import android.content.Context
import java.io.File

object UriMigrationHelper {
    private const val OLD_PACKAGE = "com.example.gymtracking"

    /**
     * Fixes image paths that might contain the old package name or different absolute paths
     * (e.g. /data/user/0/ vs /data/data/ or from a backup on a different device).
     */
    fun fixPath(path: String?, context: Context): String? {
        if (path == null) return null
        
        val isFileUri = path.startsWith("file://")
        val rawPath = if (isFileUri) path.removePrefix("file://") else path
        
        // Handle images stored in the app's internal files directory or its subdirectories
        if (rawPath.contains("/files/")) {
            // First check if it's in a subdirectory like /files/routine_images/
            val subPath = rawPath.substringAfter("/files/")
            val correctFile = File(context.filesDir, subPath)
            if (correctFile.exists()) {
                return if (isFileUri) "file://${correctFile.absolutePath}" else correctFile.absolutePath
            }
            
            // Fallback: check just the file name directly in filesDir
            // (BackupManager restores all images directly to filesDir, stripping subdirectories)
            val fileName = rawPath.substringAfterLast("/")
            val fallbackFile = File(context.filesDir, fileName)
            if (fallbackFile.exists()) {
                return if (isFileUri) "file://${fallbackFile.absolutePath}" else fallbackFile.absolutePath
            }
        }
        
        val newPackage = context.packageName
        var updatedPath = path
        if (updatedPath.contains(OLD_PACKAGE)) {
            updatedPath = updatedPath.replace(OLD_PACKAGE, newPackage)
        }
        
        return updatedPath
    }
}
