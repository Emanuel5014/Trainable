package com.emanuel5014.trainable.data.ai

/**
 * The tolerant JSON the scan parsers read model answers with, behaving like Android's own org.json (libcore): single
 * quotes, unquoted names and values, comments, `=`/`=>`/`;` separators and trailing commas are all accepted, and
 * anything after the first value is ignored. Android keeps parsing with the system org.json ([parseLenientJson] actual)
 * and only converts the result; iOS parses with [LenientJsonTokener], a port of libcore's JSONTokener.
 *
 * Values are: [String], [Int], [Long], [Double], [Boolean], [LenientJson.NULL], [LenientJsonObject], [LenientJsonArray].
 */
object LenientJson {
    /** JSON's null, distinct from a missing value (org.json's JSONObject.NULL). */
    object NULL {
        override fun toString(): String = "null"
    }

    /** The first JSON value in [text]; throws [IllegalArgumentException] when there is none. */
    fun parse(text: String): Any = parseLenientJson(text)

    /** [text] as an object, like `JSONObject(text)`; throws when it isn't one. */
    fun parseObject(text: String): LenientJsonObject =
        parse(text) as? LenientJsonObject ?: throw IllegalArgumentException("Value is not a JSON object")

    /** [text] as an array, like `JSONArray(text)`; throws when it isn't one. */
    fun parseArray(text: String): LenientJsonArray =
        parse(text) as? LenientJsonArray ?: throw IllegalArgumentException("Value is not a JSON array")

    // libcore's JSON helper: how opt* coerce a stored value

    internal fun toStringOrNull(value: Any?): String? = when (value) {
        null -> null
        is String -> value
        else -> value.toString()
    }

    internal fun toInt(value: Any?): Int? = when (value) {
        is Int -> value
        is Number -> value.toInt()
        is String -> javaDouble(value)?.toInt()
        else -> null
    }

    internal fun toDouble(value: Any?): Double? = when (value) {
        is Double -> value
        is Number -> value.toDouble()
        is String -> javaDouble(value)
        else -> null
    }

    internal fun toBoolean(value: Any?): Boolean? = when {
        value is Boolean -> value
        value is String && value.equals("true", ignoreCase = true) -> true
        value is String && value.equals("false", ignoreCase = true) -> false
        else -> null
    }

    /** Java's Double.valueOf for the texts a model writes: surrounding whitespace allowed, nothing else after the number. */
    internal fun javaDouble(text: String): Double? = text.trim().toDoubleOrNull()

    internal fun quote(text: String): String = buildString {
        append('"')
        for (c in text) {
            when (c) {
                '"', '\\', '/' -> append('\\').append(c)
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\u000C' -> append("\\f")
                else -> if (c.code <= 0x1F) append("\\u").append(c.code.toString(16).padStart(4, '0')) else append(c)
            }
        }
        append('"')
    }

    internal fun valueToString(value: Any?): String = when (value) {
        null -> "null"
        is String -> quote(value)
        else -> value.toString()
    }
}

/** A JSON object read by [LenientJson]; the opt* methods coerce exactly like Android's JSONObject. */
class LenientJsonObject(internal val map: LinkedHashMap<String, Any?> = LinkedHashMap()) {
    fun has(name: String): Boolean = map.containsKey(name)
    fun opt(name: String): Any? = map[name]
    fun isNull(name: String): Boolean = opt(name).let { it == null || it == LenientJson.NULL }
    fun optString(name: String, fallback: String = ""): String = LenientJson.toStringOrNull(opt(name)) ?: fallback
    fun optInt(name: String, fallback: Int = 0): Int = LenientJson.toInt(opt(name)) ?: fallback
    fun optDouble(name: String, fallback: Double = Double.NaN): Double = LenientJson.toDouble(opt(name)) ?: fallback
    fun optBoolean(name: String, fallback: Boolean = false): Boolean = LenientJson.toBoolean(opt(name)) ?: fallback
    fun optJSONObject(name: String): LenientJsonObject? = opt(name) as? LenientJsonObject
    fun optJSONArray(name: String): LenientJsonArray? = opt(name) as? LenientJsonArray
    fun keys(): Set<String> = map.keys

    /** org.json's put: a null value removes the name. */
    internal fun put(name: String, value: Any?) {
        if (value == null) map.remove(name) else map[name] = value
    }

    override fun toString(): String =
        map.entries.joinToString(",", "{", "}") { (k, v) -> LenientJson.quote(k) + ":" + LenientJson.valueToString(v) }

    override fun equals(other: Any?): Boolean = other is LenientJsonObject && other.map == map
    override fun hashCode(): Int = map.hashCode()
}

/** A JSON array read by [LenientJson]; elements may be Kotlin null (org.json's empty slots from `[1,,2]`). */
class LenientJsonArray(internal val values: MutableList<Any?> = mutableListOf()) {
    fun length(): Int = values.size
    fun opt(index: Int): Any? = values.getOrNull(index)
    fun optJSONObject(index: Int): LenientJsonObject? = opt(index) as? LenientJsonObject
    fun optJSONArray(index: Int): LenientJsonArray? = opt(index) as? LenientJsonArray
    fun optString(index: Int, fallback: String = ""): String = LenientJson.toStringOrNull(opt(index)) ?: fallback

    internal fun add(value: Any?) {
        values += value
    }

    override fun toString(): String = values.joinToString(",", "[", "]") { LenientJson.valueToString(it) }
    override fun equals(other: Any?): Boolean = other is LenientJsonArray && other.values == values
    override fun hashCode(): Int = values.hashCode()
}

/** Platform parse: Android's system org.json converted to [LenientJson] values; iOS [LenientJsonTokener]. */
internal expect fun parseLenientJson(text: String): Any

/**
 * A port of Android libcore's org.json JSONTokener (the parser the app has always used on Android), so iOS reads model
 * answers the same way. Throws [IllegalArgumentException] where libcore throws JSONException.
 */
class LenientJsonTokener(input: String) {
    private val text: String = if (input.startsWith("﻿")) input.substring(1) else input
    private var pos = 0

    fun nextValue(): Any {
        val c = nextCleanInternal()
        return when (c) {
            -1 -> throw error("End of input")
            '{'.code -> readObject()
            '['.code -> readArray()
            '\''.code, '"'.code -> nextString(c.toChar())
            else -> {
                pos--
                readLiteral()
            }
        }
    }

    private fun nextCleanInternal(): Int {
        while (pos < text.length) {
            val c = text[pos++]
            when (c) {
                '\t', ' ', '\n', '\r' -> continue
                '/' -> {
                    if (pos == text.length) return c.code
                    when (text[pos]) {
                        '*' -> {
                            pos++
                            val end = text.indexOf("*/", pos)
                            if (end == -1) throw error("Unterminated comment")
                            pos = end + 2
                            continue
                        }
                        '/' -> {
                            pos++
                            skipToEndOfLine()
                            continue
                        }
                        else -> return c.code
                    }
                }
                '#' -> {
                    skipToEndOfLine()
                    continue
                }
                else -> return c.code
            }
        }
        return -1
    }

    private fun skipToEndOfLine() {
        while (pos < text.length) {
            val c = text[pos]
            if (c == '\r' || c == '\n') {
                pos++
                break
            }
            pos++
        }
    }

    private fun nextString(quote: Char): String {
        var builder: StringBuilder? = null
        var start = pos
        while (pos < text.length) {
            val c = text[pos++]
            if (c == quote) {
                return if (builder == null) text.substring(start, pos - 1) else builder.append(text, start, pos - 1).toString()
            }
            if (c == '\\') {
                if (pos == text.length) throw error("Unterminated escape sequence")
                if (builder == null) builder = StringBuilder()
                builder.append(text, start, pos - 1)
                builder.append(readEscapeCharacter())
                start = pos
            }
        }
        throw error("Unterminated string")
    }

    private fun readEscapeCharacter(): Char {
        val escaped = text[pos++]
        return when (escaped) {
            'u' -> {
                if (pos + 4 > text.length) throw error("Unterminated escape sequence")
                val hex = text.substring(pos, pos + 4)
                pos += 4
                hex.toIntOrNull(16)?.toChar() ?: throw error("Invalid escape sequence: $hex")
            }
            't' -> '\t'
            'b' -> '\b'
            'n' -> '\n'
            'r' -> '\r'
            'f' -> '\u000C'
            else -> escaped
        }
    }

    private fun readLiteral(): Any {
        val literal = nextToInternal("{}[]/\\:,=;# \t\u000C")
        if (literal.isEmpty()) throw error("Expected literal value")
        if (literal.equals("null", ignoreCase = true)) return LenientJson.NULL
        if (literal.equals("true", ignoreCase = true)) return true
        if (literal.equals("false", ignoreCase = true)) return false
        // Integral first (Java Long.parseLong with libcore's 0x / leading-0 octal rule), then floating point
        if ('.' !in literal) {
            var number = literal
            var base = 10
            if (number.startsWith("0x") || number.startsWith("0X")) {
                number = number.substring(2)
                base = 16
            } else if (number.startsWith("0") && number.length > 1) {
                number = number.substring(1)
                base = 8
            }
            javaParseLong(number, base)?.let { value ->
                return if (value in Int.MIN_VALUE..Int.MAX_VALUE) value.toInt() else value
            }
        }
        literal.toDoubleOrNull()?.let { return it }
        // An unquoted string
        return literal
    }

    /** Java's Long.parseLong: an optional sign, then digits of [radix] only. */
    private fun javaParseLong(s: String, radix: Int): Long? {
        if (s.isEmpty()) return null
        val digits = if (s[0] == '+' || s[0] == '-') s.substring(1) else s
        if (digits.isEmpty() || digits.any { it.digitToIntOrNull(radix) == null }) return null
        return s.toLongOrNull(radix)
    }

    private fun nextToInternal(excluded: String): String {
        val start = pos
        while (pos < text.length) {
            val c = text[pos]
            if (c == '\r' || c == '\n' || excluded.indexOf(c) != -1) return text.substring(start, pos)
            pos++
        }
        return text.substring(start)
    }

    private fun readObject(): LenientJsonObject {
        val result = LenientJsonObject()
        val first = nextCleanInternal()
        if (first == '}'.code) return result
        if (first != -1) pos--
        while (true) {
            val name = nextValue()
            if (name !is String) throw error("Names must be strings, but $name is not")
            val separator = nextCleanInternal()
            if (separator != ':'.code && separator != '='.code) throw error("Expected ':' after $name")
            if (pos < text.length && text[pos] == '>') pos++
            result.put(name, nextValue())
            when (nextCleanInternal()) {
                '}'.code -> return result
                ';'.code, ','.code -> continue
                else -> throw error("Unterminated object")
            }
        }
    }

    private fun readArray(): LenientJsonArray {
        val result = LenientJsonArray()
        var hasTrailingSeparator = false
        while (true) {
            when (nextCleanInternal()) {
                -1 -> throw error("Unterminated array")
                ']'.code -> {
                    if (hasTrailingSeparator) result.add(null)
                    return result
                }
                ','.code, ';'.code -> {
                    // A separator with no value before it is an empty slot
                    result.add(null)
                    hasTrailingSeparator = true
                    continue
                }
                else -> pos--
            }
            result.add(nextValue())
            when (nextCleanInternal()) {
                ']'.code -> return result
                ','.code, ';'.code -> {
                    hasTrailingSeparator = true
                    continue
                }
                else -> throw error("Unterminated array")
            }
        }
    }

    private fun error(message: String) = IllegalArgumentException("$message at character $pos")
}
