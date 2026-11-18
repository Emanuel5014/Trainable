package com.emanuel5014.trainable.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.os.Build

/** The seed colours Android gives for the home-screen wallpaper (three from 12 on, one before); empty if unknown. */
fun wallpaperSeedColors(context: Context): List<Int> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return emptyList()
    return try {
        val colors = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            ?: return emptyList()
        buildList {
            add(colors.primaryColor.toArgb())
            colors.secondaryColor?.toArgb()?.let(::add)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) colors.tertiaryColor?.toArgb()?.let(::add)
        }.distinct()
    } catch (e: Exception) {
        emptyList()
    }
}
