package com.emanuel5014.trainable.util.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Only the JSON operations used by the backup format; Android delegates these to its existing org.json calls. */
interface BackupJsonObject {
    fun has(key: String): Boolean
    fun isNull(key: String): Boolean
    fun getString(key: String): String
    fun getBoolean(key: String): Boolean
    fun getInt(key: String): Int
    fun getDouble(key: String): Double
    fun optString(key: String): String
    fun optBoolean(key: String, fallback: Boolean): Boolean
    fun getStrings(key: String): Set<String>
    fun put(key: String, value: String)
    fun put(key: String, value: Boolean)
    fun put(key: String, value: Int)
    fun put(key: String, value: Double)
    fun putStrings(key: String, value: Set<String>)
    fun encode(): String
}

/** Portable implementation for iOS and format checks. Backup keys and values are never logged. */
class KotlinxBackupJsonObject(source: String? = null) : BackupJsonObject {
    private val values = source?.let { Json.parseToJsonElement(it) as? JsonObject
        ?: throw IllegalArgumentException("Backup settings must be an object") }?.toMutableMap()
        ?: linkedMapOf<String, JsonElement>()

    override fun has(key: String): Boolean = key in values
    override fun isNull(key: String): Boolean = values[key] == null || values[key] == JsonNull
    private fun value(key: String): JsonElement = values[key]
        ?: throw IllegalArgumentException("Missing backup setting: $key")
    private fun string(value: JsonElement): String = (value as? JsonPrimitive)?.content ?: value.toString()
    override fun getString(key: String): String = string(value(key))
    override fun getBoolean(key: String): Boolean = getString(key).let {
        when {
            it.equals("true", ignoreCase = true) -> true
            it.equals("false", ignoreCase = true) -> false
            else -> throw IllegalArgumentException("Expected a boolean backup setting: $key")
        }
    }
    override fun getInt(key: String): Int = getString(key).toDoubleOrNull()?.toInt()
        ?: throw IllegalArgumentException("Expected an integer backup setting: $key")
    override fun getDouble(key: String): Double = getString(key).toDoubleOrNull()
        ?: throw IllegalArgumentException("Expected a numeric backup setting: $key")
    override fun optString(key: String): String = if (isNull(key)) "" else getString(key)
    override fun optBoolean(key: String, fallback: Boolean): Boolean =
        runCatching { getBoolean(key) }.getOrDefault(fallback)
    override fun getStrings(key: String): Set<String> = (value(key) as? JsonArray
        ?: throw IllegalArgumentException("Expected an array backup setting: $key")).map(::string).toSet()

    override fun put(key: String, value: String) { values[key] = JsonPrimitive(value) }
    override fun put(key: String, value: Boolean) { values[key] = JsonPrimitive(value) }
    override fun put(key: String, value: Int) { values[key] = JsonPrimitive(value) }
    override fun put(key: String, value: Double) {
        require(value.isFinite()) { "Non-finite backup setting: $key" }
        values[key] = JsonPrimitive(value)
    }
    override fun putStrings(key: String, value: Set<String>) { values[key] = JsonArray(value.map(::JsonPrimitive)) }
    override fun encode(): String = JsonObject(values).toString()
}
