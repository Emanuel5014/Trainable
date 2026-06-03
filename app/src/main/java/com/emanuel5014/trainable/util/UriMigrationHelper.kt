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
            val fileName = rawPath.substringAfterLast("/")
            
            // 1. Try exact subpath
            val subPath = rawPath.substringAfter("/files/")
            val exactFile = File(context.filesDir, subPath)
            if (exactFile.exists()) {
                return if (isFileUri) "file://${exactFile.absolutePath}" else exactFile.absolutePath
            }
            
            // 2. Try in routine_images (where compression moves them)
            val compressedFile = File(File(context.filesDir, "routine_images"), fileName)
            if (compressedFile.exists()) {
                return if (isFileUri) "file://${compressedFile.absolutePath}" else compressedFile.absolutePath
            }
            
            // 3. Try in root filesDir (fallback for backup restores)
            val rootFile = File(context.filesDir, fileName)
            if (rootFile.exists()) {
                return if (isFileUri) "file://${rootFile.absolutePath}" else rootFile.absolutePath
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
