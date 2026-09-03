package com.emanuel5014.trainable.data.ai

import org.json.JSONArray
import org.json.JSONObject

object RoutineScanParser {

    /**
     * Parses the LLM raw response (whether in JSON format, markdown blocks,
     * fragmented JSON objects, or plain text exercise lists) into a list of [ParsedExercise].
     */
    fun parse(raw: String): List<ParsedExercise> {
        val text = raw.trim()
        if (text.isBlank()) return emptyList()

        // 1. Try extracting and parsing a full JSON Array [...]
        val arrayResults = tryParseJsonArray(text)
        if (arrayResults.isNotEmpty()) return arrayResults

        // 2. Try extracting individual JSON objects {...} via regex
        val objectResults = tryParseJsonObjects(text)
        if (objectResults.isNotEmpty()) return objectResults

        // 3. Fallback: Parse line-by-line formatted text / markdown lists
        return tryParseTextList(text)
    }

    private fun tryParseJsonArray(raw: String): List<ParsedExercise> {
        val jsonArrayStr = extractJsonArrayString(raw) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonArrayStr)
            val list = mutableListOf<ParsedExercise>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                parseExerciseObject(obj)?.let { list.add(it) }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun tryParseJsonObjects(raw: String): List<ParsedExercise> {
        val list = mutableListOf<ParsedExercise>()
        val objectRegex = Regex("""\{[^{}]*?"(?:name|exercise|esercizio|nome)"[^{}]*?\}""", RegexOption.DOT_MATCHES_ALL)
        val matches = objectRegex.findAll(raw)

        for (match in matches) {
            try {
                val obj = JSONObject(match.value)
                parseExerciseObject(obj)?.let { list.add(it) }
            } catch (_: Exception) {
                // Ignore individual malformed objects
            }
        }
        return list
    }

    private fun parseExerciseObject(obj: JSONObject): ParsedExercise? {
        val rawName = obj.optString("name")
            .ifBlank { obj.optString("exercise") }
            .ifBlank { obj.optString("esercizio") }
            .ifBlank { obj.optString("nome") }
            .trim()

        if (rawName.isBlank()) return null

        val cleanName = cleanExerciseName(rawName)
        if (cleanName.isBlank()) return null

        val reps = parseReps(obj)
        val sets = parseSets(obj, reps)
        val rest = parseRest(obj)
        val cardio = parseCardioMinutes(obj, cleanName)
        val category = obj.optString("category")
            .ifBlank { obj.optString("categoria") }
            .ifBlank { obj.optString("muscle_group") }
            .takeIf { it.isNotBlank() }
        val explicitType = obj.optString("exercise_type")
            .ifBlank { obj.optString("type") }
            .ifBlank { obj.optString("tipo") }
            .trim()
            .lowercase()

        var explicitTimeSeconds: Int? = null
        if (obj.has("time_seconds")) {
            explicitTimeSeconds = obj.optInt("time_seconds").takeIf { it > 0 }
        } else if (obj.has("seconds")) {
            explicitTimeSeconds = obj.optInt("seconds").takeIf { it > 0 }
        }

        val isTimed = explicitType == "time_and_weight" ||
            reps.endsWith("s", ignoreCase = true) ||
            reps.endsWith("sec", ignoreCase = true) ||
            reps.contains("''") ||
            reps.contains("\"") ||
            cleanName.lowercase().let { it.contains("plank") || it.contains("wall sit") || it.contains("isometria") || it.contains("hollow") }

        val detectedType = when {
            cardio != null || explicitType == "cardio" -> "cardio"
            isTimed -> "time_and_weight"
            explicitType == "strength" -> "strength"
            else -> "strength"
        }

        val parsedTimeSeconds = if (detectedType == "time_and_weight") {
            explicitTimeSeconds ?: reps.filter { it.isDigit() }.toIntOrNull() ?: 45
        } else null

        return ParsedExercise(
            name = cleanName,
            sets = sets,
            reps = if (detectedType == "time_and_weight" && !reps.endsWith("s")) "${parsedTimeSeconds ?: 45}s" else reps,
            restSeconds = rest,
            cardioMinutes = cardio,
            category = category,
            exerciseType = detectedType,
            timeSeconds = parsedTimeSeconds
        )
    }

    private fun cleanExerciseName(name: String): String {
        return name
            .replace(Regex("""^[\d+\.\-\)\*\#]+\s*"""), "") // Remove leading numbers/bullets like "1.", "A)", "* "
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '"', '\'', '`', ',', '.', ';')
    }

    private fun parseSets(obj: JSONObject, reps: String): Int {
        var explicitSets: Int? = null
        if (obj.has("sets")) {
            val v = obj.opt("sets")
            if (v is Number) explicitSets = v.toInt()
            else if (v is String) {
                explicitSets = Regex("""\d+""").find(v)?.value?.toIntOrNull()
            }
        }
        if (explicitSets == null && obj.has("serie")) {
            val v = obj.opt("serie")
            if (v is Number) explicitSets = v.toInt()
            else if (v is String) {
                explicitSets = Regex("""\d+""").find(v)?.value?.toIntOrNull()
            }
        }

        // Deduce sets count if reps has a pyramidal sequence (e.g. "8-6-4-2" -> 4 sets)
        val numbers = Regex("""\d+""").findAll(reps).mapNotNull { it.value.toIntOrNull() }.toList()
        if (numbers.size >= 3) {
            // If explicitSets is missing or defaulted to less than number of stages, use stages count
            if (explicitSets == null || explicitSets < numbers.size) {
                return numbers.size.coerceIn(1, 30)
            }
        } else if (numbers.size == 2 && numbers[0] > numbers[1] && (explicitSets == null || explicitSets < 2)) {
            // E.g. "8-6" (descending 2 stages)
            return 2
        }

        return (explicitSets ?: 3).coerceIn(1, 30)
    }

    private fun parseReps(obj: JSONObject): String {
        val raw = if (obj.has("reps")) {
            val v = obj.opt("reps")
            if (v is String && v.isNotBlank()) v.trim()
            else if (v is Number) v.toString()
            else null
        } else if (obj.has("ripetizioni")) {
            val v = obj.opt("ripetizioni")
            if (v is String && v.isNotBlank()) v.trim()
            else if (v is Number) v.toString()
            else null
        } else null

        if (raw != null) {
            return cleanRepsString(raw)
        }
        return "8-12"
    }

    fun cleanRepsString(raw: String): String {
        var reps = raw.trim().trim('"', '\'', '`')
        if (reps.isBlank()) return "8-12"

        // If it's a multi-number sequence separated by slashes, commas, dots, or spaces (e.g. "8/6/4/2", "12, 10, 8, 6", "8 6 4 2")
        if ((reps.contains("/") || reps.contains(",") || reps.contains(" ") || (reps.contains(".") && !reps.contains(".."))) && !reps.contains("+")) {
            val numbers = Regex("""\d+""").findAll(reps).map { it.value }.toList()
            if (numbers.size >= 2) {
                return numbers.joinToString("-")
            }
        }

        return reps
            .replace(Regex("""\s*-\s*"""), "-")
            .replace(Regex("""\s*\+\s*"""), "+")
            .replace(Regex("""\s*/\s*"""), "-")
    }

    private fun parseRest(obj: JSONObject): Int {
        val keys = listOf("rest_seconds", "restSeconds", "rest", "recupero", "pausa")
        for (k in keys) {
            if (!obj.has(k)) continue
            val v = obj.opt(k)
            if (v is Number) return v.toInt().coerceIn(0, 600)
            if (v is String) {
                val text = v.lowercase().trim()
                // Check format like "1'30\"" or "1:30"
                val minSecMatch = Regex("""(\d+)\s*[':]\s*(\d+)""").find(text)
                if (minSecMatch != null) {
                    val m = minSecMatch.groupValues[1].toIntOrNull() ?: 0
                    val s = minSecMatch.groupValues[2].toIntOrNull() ?: 0
                    return (m * 60 + s).coerceIn(0, 600)
                }
                // Check format like "2 min" or "2m"
                val minMatch = Regex("""(\d+)\s*(?:min|m)""").find(text)
                if (minMatch != null) {
                    val m = minMatch.groupValues[1].toIntOrNull() ?: 0
                    return (m * 60).coerceIn(0, 600)
                }
                // Check format like "90s" or "90"
                val secMatch = Regex("""\d+""").find(text)
                if (secMatch != null) {
                    val s = secMatch.value.toIntOrNull() ?: 120
                    return s.coerceIn(0, 600)
                }
            }
        }
        return 120
    }

    private fun parseCardioMinutes(obj: JSONObject, name: String): Int? {
        val keys = listOf("cardio_minutes", "cardioMinutes", "cardio", "duration", "durata")
        for (k in keys) {
            if (!obj.has(k)) continue
            val v = obj.opt(k)
            if (v is Number && v.toInt() > 0) return v.toInt()
            if (v is String) {
                val num = Regex("""\d+""").find(v)?.value?.toIntOrNull()
                if (num != null && num > 0) return num
            }
        }
        // If name clearly indicates cardio machine without duration, default to 20
        val isCardioName = listOf("treadmill", "tapis roulant", "tapis", "cyclette", "bike", "ellittica", "vogatore", "rower")
            .any { name.lowercase().contains(it) }
        return if (isCardioName) 20 else null
    }

    private fun extractJsonArrayString(raw: String): String? {
        var text = raw.trim()
        if (text.contains("```")) {
            text = text.substringAfter("```")
            if (text.startsWith("json", ignoreCase = true)) {
                text = text.substringAfter("json").trim()
            }
            text = text.substringBefore("```").trim()
        }
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }

    private fun tryParseTextList(text: String): List<ParsedExercise> {
        val list = mutableListOf<ParsedExercise>()
        val lines = text.lines()

        // Match with explicit multiplier "4x 8-6-4-2" or "3x 8-10":
        val explicitSetsRegex = Regex("""(?:^|\n)\s*(?:[\d+\.\-\*\#\)]+\s*)?([A-Za-zÀ-ÿ\s\/\-\'\(\)]+?)\s*[:\-]?\s*(\d+)\s*[xX*]\s*([0-9\-\/\,\+\s]+?)(?:[^\d\n]*?(\d+)\s*(?:s|sec|min|'|"))?(?:\n|$)""")
        // Match without multiplier "Panca Piana 8-6-4-2 rec 90s":
        val directRepsRegex = Regex("""(?:^|\n)\s*(?:[\d+\.\-\*\#\)]+\s*)?([A-Za-zÀ-ÿ\s\/\-\'\(\)]+?)\s*[:\-]?\s*(\d+(?:[\-\/\,\s]\d+){1,6})(?:[^\d\n]*?(\d+)\s*(?:s|sec|min|'|"))?(?:\n|$)""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length < 4) continue

            val explicitMatch = explicitSetsRegex.find(trimmed)
            if (explicitMatch != null) {
                val name = cleanExerciseName(explicitMatch.groupValues[1])
                if (name.length >= 3) {
                    val rawReps = explicitMatch.groupValues[3]
                    val reps = cleanRepsString(rawReps)
                    val rawSets = explicitMatch.groupValues[2].toIntOrNull() ?: 3
                    val numbers = Regex("""\d+""").findAll(reps).toList()
                    val sets = if (numbers.size >= 3 && rawSets < numbers.size) numbers.size else rawSets
                    val rest = explicitMatch.groupValues.getOrNull(4)?.toIntOrNull() ?: 120
                    val cardio = if (listOf("treadmill", "tapis", "cyclette", "bike").any { name.lowercase().contains(it) }) 20 else null
                    val isTimed = reps.endsWith("s", ignoreCase = true) ||
                        reps.endsWith("sec", ignoreCase = true) ||
                        name.lowercase().let { it.contains("plank") || it.contains("wall sit") || it.contains("isometria") || it.contains("hollow") }
                    val type = if (cardio != null) "cardio" else if (isTimed) "time_and_weight" else "strength"
                    val timeSec = if (type == "time_and_weight") reps.filter { it.isDigit() }.toIntOrNull() ?: 45 else null

                    list.add(
                        ParsedExercise(
                            name = name,
                            sets = sets.coerceIn(1, 30),
                            reps = if (type == "time_and_weight" && !reps.endsWith("s")) "${timeSec ?: 45}s" else reps,
                            restSeconds = rest.coerceIn(0, 600),
                            cardioMinutes = cardio,
                            category = null,
                            exerciseType = type,
                            timeSeconds = timeSec
                        )
                    )
                    continue
                }
            }

            val directMatch = directRepsRegex.find(trimmed)
            if (directMatch != null) {
                val name = cleanExerciseName(directMatch.groupValues[1])
                if (name.length >= 3) {
                    val rawReps = directMatch.groupValues[2]
                    val reps = cleanRepsString(rawReps)
                    val numbers = Regex("""\d+""").findAll(reps).mapNotNull { it.value.toIntOrNull() }.toList()
                    val sets = if (numbers.size >= 3) numbers.size else if (numbers.size == 2 && numbers[0] > numbers[1]) 2 else 3
                    val rest = directMatch.groupValues.getOrNull(3)?.toIntOrNull() ?: 120
                    val cardio = if (listOf("treadmill", "tapis", "cyclette", "bike").any { name.lowercase().contains(it) }) 20 else null
                    val isTimed = reps.endsWith("s", ignoreCase = true) ||
                        reps.endsWith("sec", ignoreCase = true) ||
                        name.lowercase().let { it.contains("plank") || it.contains("wall sit") || it.contains("isometria") || it.contains("hollow") }
                    val type = if (cardio != null) "cardio" else if (isTimed) "time_and_weight" else "strength"
                    val timeSec = if (type == "time_and_weight") reps.filter { it.isDigit() }.toIntOrNull() ?: 45 else null

                    list.add(
                        ParsedExercise(
                            name = name,
                            sets = sets.coerceIn(1, 30),
                            reps = if (type == "time_and_weight" && !reps.endsWith("s")) "${timeSec ?: 45}s" else reps,
                            restSeconds = rest.coerceIn(0, 600),
                            cardioMinutes = cardio,
                            category = null,
                            exerciseType = type,
                            timeSeconds = timeSec
                        )
                    )
                }
            }
        }
        return list
    }
}

