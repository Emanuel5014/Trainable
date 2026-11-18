package com.emanuel5014.trainable.data.repository

/**
 * A small synchronous store of named values, for choices that never went to the settings file: Android keeps them in
 * a SharedPreferences file of their own ("analytics_prefs"), iOS in its user defaults.
 */
interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getString(key: String, default: String?): String?
    fun putString(key: String, value: String)
    fun getStringSet(key: String, default: Set<String>?): Set<String>?
    fun putStringSet(key: String, value: Set<String>)
}
