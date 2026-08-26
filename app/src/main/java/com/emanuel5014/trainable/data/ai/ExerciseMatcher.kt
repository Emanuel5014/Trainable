package com.emanuel5014.trainable.data.ai

import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import java.text.Normalizer

class ExerciseMatcher(
    private val catalog: List<ExerciseEntity>,
    private val languageCode: String
) {

    /**
     * Every known way to refer to each exercise: canonical English names plus
     * all localized names from every supported language.
     */
    private val candidates: List<Pair<ExerciseEntity, String>> by lazy {
        val byCanonicalName = catalog.associateBy { it.nome.lowercase() }
        buildList {
            catalog.forEach { add(it to normalizeAndExpand(it.nome)) }
            listOf(
                ExerciseTranslations.it,
                ExerciseTranslations.es,
                ExerciseTranslations.fr,
                ExerciseTranslations.de,
                ExerciseTranslations.pt
            ).forEach { translations ->
                translations.forEach { (canonical, localized) ->
                    byCanonicalName[canonical.lowercase()]?.let { exercise ->
                        add(exercise to normalizeAndExpand(localized))
                    }
                }
            }
        }.distinctBy { (ex, name) -> "${ex.id}_$name" }
    }

    data class Match(val exercise: ExerciseEntity, val score: Double)

    fun bestMatch(name: String): Match? {
        val query = normalizeAndExpand(name)
        if (query.isBlank()) return null

        var bestScore = 0.0
        var best: ExerciseEntity? = null

        for ((exercise, candidate) in candidates) {
            val score = calculateMatchScore(query, candidate)
            if (score > bestScore) {
                bestScore = score
                best = exercise
            }
        }
        return best?.takeIf { bestScore >= MATCH_THRESHOLD }?.let { Match(it, bestScore) }
    }

    fun suggestCategory(name: String, knownCategories: List<String> = emptyList()): String {
        val matched = bestMatch(name)?.exercise?.categoria
        if (!matched.isNullOrBlank()) return matched

        return inferCategoryFromName(name, knownCategories)
    }

    fun resolve(name: String): ExerciseEntity? = bestMatch(name)?.exercise

    companion object {
        private const val MATCH_THRESHOLD = 0.60
        private const val CATEGORY_MATCH_THRESHOLD = 0.65

        fun normalizeAndExpand(value: String): String {
            var text = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            // Expand common gym abbreviations
            text = text
                .replace(Regex("""\bp\b(?=\s|$)"""), "panca")
                .replace(Regex("""\binc\b|\binclin\b"""), "inclinata")
                .replace(Regex("""\bdec\b|\bdeclin\b"""), "declinata")
                .replace(Regex("""\bmach\b|\bmac\b"""), "machine")
                .replace(Regex("""\bext\b"""), "extension")
                .replace(Regex("""\bbil\b|\bbb\b"""), "bilanciere")
                .replace(Regex("""\bman\b|\bdb\b"""), "manubri")
                .replace(Regex("""\brdl\b"""), "stacco rumeno")
                .replace(Regex("""\bsq\b"""), "squat")
                .replace(Regex("""\bspinte\b"""), "press")
                .replace(Regex("""\balz\b"""), "alzate")
                .replace(Regex("""\btric\b"""), "tricipiti")
                .replace(Regex("""\bbicip\b"""), "bicipiti")
                .replace(Regex("""\s+"""), " ")
                .trim()

            return text
        }

        fun calculateMatchScore(query: String, candidate: String): Double {
            if (query == candidate) return 1.0
            if (query.isBlank() || candidate.isBlank()) return 0.0

            // 1. Check direct containment / substring
            if (query.contains(candidate) || candidate.contains(query)) {
                val minLen = minOf(query.length, candidate.length).toDouble()
                val maxLen = maxOf(query.length, candidate.length).toDouble()
                val containmentScore = (minLen / maxLen) * 0.95
                if (containmentScore >= 0.75) return containmentScore
            }

            // 2. Token Set Jaccard similarity
            val tokensA = query.split(" ").filter { it.isNotBlank() && it.length > 1 }.toSet()
            val tokensB = candidate.split(" ").filter { it.isNotBlank() && it.length > 1 }.toSet()

            val tokenScore = if (tokensA.isNotEmpty() && tokensB.isNotEmpty()) {
                val intersection = tokensA.intersect(tokensB).size
                val union = tokensA.union(tokensB).size
                val jaccard = intersection.toDouble() / union.toDouble()

                // If all words of query or candidate are matched
                if (intersection == tokensA.size || intersection == tokensB.size) {
                    0.85 + (jaccard * 0.15)
                } else {
                    jaccard
                }
            } else 0.0

            // 3. String Levenshtein edit distance
            val distance = levenshtein(query, candidate)
            val editScore = 1.0 - (distance.toDouble() / maxOf(query.length, candidate.length))

            return maxOf(tokenScore, editScore)
        }

        /** Maps an arbitrary (possibly translated) category name to a known one, or null. */
        fun mapToKnownCategory(name: String, knownCategories: List<String>): String? {
            val query = normalizeAndExpand(name)
            if (query.isBlank() || knownCategories.isEmpty()) return null

            var bestScore = 0.0
            var best: String? = null
            for (category in knownCategories) {
                val score = calculateMatchScore(query, normalizeAndExpand(category))
                if (score > bestScore) {
                    bestScore = score
                    best = category
                }
            }
            return best?.takeIf { bestScore >= CATEGORY_MATCH_THRESHOLD }
        }

        /** Heuristic keyword-based muscle group / category inference */
        fun inferCategoryFromName(name: String, knownCategories: List<String>): String {
            val q = normalizeAndExpand(name)

            val inferred = when {
                q.contains("panca") || q.contains("petto") || q.contains("chest") || q.contains("croci") || q.contains("pectoral") || q.contains("dip") -> "Chest"
                q.contains("lat") || q.contains("pulley") || q.contains("dorso") || q.contains("rematore") || q.contains("row") || q.contains("back") || q.contains("trazioni") || q.contains("pullup") || q.contains("pullover") || q.contains("iperestensioni") || q.contains("seal") -> "Back"
                q.contains("squat") || q.contains("leg") || q.contains("press") || q.contains("polpacci") || q.contains("calf") || q.contains("affondi") || q.contains("quadricipiti") || q.contains("femorali") || q.contains("glutei") || q.contains("stacco") || q.contains("lunge") || q.contains("hack") -> "Legs"
                q.contains("spalle") || q.contains("shoulder") || q.contains("military") || q.contains("lento") || q.contains("alzate") || q.contains("delt") || q.contains("arnold") || q.contains("trapezi") -> "Shoulders"
                q.contains("curl") || q.contains("bicipiti") || q.contains("tricipiti") || q.contains("french") || q.contains("pushdown") || q.contains("braccia") || q.contains("arms") || q.contains("hammer") -> "Arms"
                q.contains("crunch") || q.contains("plank") || q.contains("addominali") || q.contains("abs") || q.contains("core") || q.contains("twist") || q.contains("v-up") || q.contains("leg raise") -> "Core"
                q.contains("tapis") || q.contains("treadmill") || q.contains("cyclette") || q.contains("bike") || q.contains("cardio") || q.contains("vogatore") || q.contains("ellittica") || q.contains("corsa") -> "Cardio"
                else -> ""
            }

            if (inferred.isNotBlank() && knownCategories.isNotEmpty()) {
                val matched = mapToKnownCategory(inferred, knownCategories)
                if (matched != null) return matched
            }

            return inferred
        }

        private fun levenshtein(a: String, b: String): Int {
            var prev = IntArray(b.length + 1) { it }
            var curr = IntArray(b.length + 1)
            for (i in 1..a.length) {
                curr[0] = i
                for (j in 1..b.length) {
                    curr[j] = minOf(
                        prev[j] + 1,
                        curr[j - 1] + 1,
                        prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                    )
                }
                System.arraycopy(curr, 0, prev, 0, curr.size)
            }
            return prev[b.length]
        }
    }
}

