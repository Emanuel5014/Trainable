package com.emanuel5014.trainable.ui.theme

object ThemeColorStore {
    private var _darkColors: Map<String, String> = emptyMap()
    private var _lightColors: Map<String, String> = emptyMap()

    val darkColors: Map<String, String> get() = _darkColors
    val lightColors: Map<String, String> get() = _lightColors

    fun setColors(dark: Map<String, String>, light: Map<String, String>) {
        _darkColors = dark
        _lightColors = light
    }
}
