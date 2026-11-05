package com.emanuel5014.trainable.util.backup

import org.json.JSONArray
import org.json.JSONObject

/** Preserve Android's JSON serialization and coercion behaviour for all existing backups. */
internal class AndroidBackupJsonObject private constructor(private val json: JSONObject) : BackupJsonObject {
    constructor() : this(JSONObject())
    constructor(source: String) : this(JSONObject(source))

    override fun has(key: String): Boolean = json.has(key)
    override fun isNull(key: String): Boolean = json.isNull(key)
    override fun getString(key: String): String = json.getString(key)
    override fun getBoolean(key: String): Boolean = json.getBoolean(key)
    override fun getInt(key: String): Int = json.getInt(key)
    override fun getDouble(key: String): Double = json.getDouble(key)
    override fun optString(key: String): String = json.optString(key)
    override fun optBoolean(key: String, fallback: Boolean): Boolean = json.optBoolean(key, fallback)
    override fun getStrings(key: String): Set<String> {
        val array = json.getJSONArray(key)
        val result = mutableSetOf<String>()
        for (i in 0 until array.length()) result.add(array.getString(i))
        return result
    }
    override fun put(key: String, value: String) { json.put(key, value) }
    override fun put(key: String, value: Boolean) { json.put(key, value) }
    override fun put(key: String, value: Int) { json.put(key, value) }
    override fun put(key: String, value: Double) { json.put(key, value) }
    override fun putStrings(key: String, value: Set<String>) {
        val array = JSONArray()
        value.forEach { array.put(it) }
        json.put(key, array)
    }
    override fun encode(): String = json.toString()
}
