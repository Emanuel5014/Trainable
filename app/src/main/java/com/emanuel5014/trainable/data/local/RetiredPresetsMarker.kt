package com.emanuel5014.trainable.data.local

import android.content.Context

/**
 * Remembers, next to the database and not in it, which version of the `RetiredPresets` list was already cleaned out of the
 * library, so that a retired preset the user brings back is not hidden again at the next launch. The marker belongs to the
 * database file: replacing the file (a restored backup, a reset) makes it [forget], and the clean-up runs on the new one.
 */
internal object RetiredPresetsMarker {
    private const val FILE = "exercise_library"
    private const val KEY = "retired_presets_version"

    /** The version of the list applied to the database in use, 0 when none. */
    fun applied(context: Context): Int = prefs(context).getInt(KEY, 0)

    fun markApplied(context: Context, version: Int) {
        prefs(context).edit().putInt(KEY, version).apply()
    }

    fun forget(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
