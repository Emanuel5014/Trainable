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
     * all localized names from every supported language. Fuzzy-matching the
     * scanned text against this vocabulary makes OCR typos tolerant
     * (e.g. "panca piano" still resolves to "Flat Bench Press").
     */
    private val candidates: List<Pair<ExerciseEntity, String>> by lazy {
        val byCanonicalName = catalog.associateBy { it.nome.lowercase() }
        buildList {
            catalog.forEach { add(it to normalize(it.nome)) }
            listOf(
                ExerciseTranslations.it,
                ExerciseTranslations.es,
                ExerciseTranslations.fr,
                ExerciseTranslations.de,
                ExerciseTranslations.pt
            ).forEach { translations ->
                translations.forEach { (canonical, localized) ->
                    byCanonicalName[canonical.lowercase()]?.let { exercise ->
                        add(exercise to normalize(localized))
                    }
                }
            }
        }.distinctBy { (_, name) -> name }
    }

    data class Match(val exercise: ExerciseEntity, val score: Double)

    fun bestMatch(name: String): Match? {
        val query = normalize(name)
        if (query.isBlank()) return null

        var bestScore = 0.0
        var best: ExerciseEntity? = null
        for ((exercise, candidate) in candidates) {
            val score = similarity(query, candidate)
            if (score > bestScore) {
                bestScore = score
                best = exercise
            }
        }
        return best?.takeIf { bestScore >= MATCH_THRESHOLD }?.let { Match(it, bestScore) }
    }

    fun suggestCategory(name: String): String = bestMatch(name)?.exercise?.categoria ?: ""

    fun resolve(name: String): ExerciseEntity? = bestMatch(name)?.exercise

    companion object {
        private const val MATCH_THRESHOLD = 0.72

        fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        fun similarity(a: String, b: String): Double {
            if (a == b) return 1.0
            if (a.isBlank() || b.isBlank()) return 0.0
            if (a.contains(b) || b.contains(a)) return 0.88
            val distance = levenshtein(a, b)
            return 1.0 - distance.toDouble() / maxOf(a.length, b.length)
        }

        /** Maps an arbitrary (possibly translated) category name to a known one, or null. */
        fun mapToKnownCategory(name: String, knownCategories: List<String>): String? {
            val query = normalize(name)
            if (query.isBlank() || knownCategories.isEmpty()) return null

            var bestScore = 0.0
            var best: String? = null
            for (category in knownCategories) {
                val score = similarity(query, normalize(category))
                if (score > bestScore) {
                    bestScore = score
                    best = category
                }
            }
            return best?.takeIf { bestScore >= CATEGORY_MATCH_THRESHOLD }
        }

        private const val CATEGORY_MATCH_THRESHOLD = 0.7

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
