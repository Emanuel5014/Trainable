package com.emanuel5014.trainable.data.ai

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

/** The system org.json parses, exactly as before the parsers moved here; the result is only converted. */
internal actual fun parseLenientJson(text: String): Any = try {
    convert(JSONTokener(text).nextValue()) ?: throw IllegalArgumentException("End of input")
} catch (e: JSONException) {
    throw IllegalArgumentException(e.message, e)
}

private fun convert(value: Any?): Any? = when {
    value == null -> null
    // JSONObject.NULL equals Kotlin null by its own equals: compare the instance
    value === JSONObject.NULL -> LenientJson.NULL
    value is JSONObject -> LenientJsonObject().also { result ->
        value.keys().forEach { key -> result.put(key, convert(value.opt(key))) }
    }
    value is JSONArray -> LenientJsonArray().also { result ->
        for (i in 0 until value.length()) result.add(convert(value.opt(i)))
    }
    value is Int || value is Long || value is Double || value is String || value is Boolean -> value
    // Only the JVM test library makes other numbers (BigDecimal, BigInteger, Float)
    value is Number -> if (value.toDouble() % 1.0 == 0.0 && value.toDouble() in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) {
        value.toLong().let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it }
    } else value.toDouble()
    else -> value.toString()
}
