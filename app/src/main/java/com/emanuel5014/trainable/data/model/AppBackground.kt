package com.emanuel5014.trainable.data.model

data class AppBackground(
    val mode: String = THEME,
    val color: Int = 0xFF35516B.toInt(),
    val wallpaperName: String? = null,
    val dimming: Float = .72f
) {
    val isCustom: Boolean get() = mode == COLOR || (mode == WALLPAPER && wallpaperName != null)

    companion object {
        const val THEME = "theme"
        const val COLOR = "color"
        const val WALLPAPER = "wallpaper"
        fun safeDimming(value: Float): Float = if (value.isFinite()) value.coerceIn(.65f, .90f) else .72f
    }
}
