package com.emanuel5014.trainable.data.ai

import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import java.text.Normalizer

class ExerciseMatcher(
    private val catalog: List<ExerciseEntity>,
    private val languageCode: String
) {

    // Reverse translations: localized name -> canonical English name
    private val reverseTranslation: Map<String, String> by lazy {
        val forward = when (languageCode) {
            "it" -> ExerciseTranslations.it
            "es" -> ExerciseTranslations.es
            "fr" -> ExerciseTranslations.fr
            "de" -> ExerciseTranslations.de
            "pt" -> ExerciseTranslations.pt
            else -> emptyMap()
        }
        forward.entries.associate { (en, localized) -> normalize(localized) to en }
    }

    data class Match(val exercise: ExerciseEntity, val score: Double)

    fun bestMatch(name: String): Match? {
        val query = normalize(name)
        if (query.isBlank()) return null

        var best: Match? = null
        for (exercise in catalog) {
            val canonicalQuery = reverseTranslation[query] ?: query
            val score = maxOf(
                similarity(query, normalize(exercise.nome)),
                if (canonicalQuery != query) similarity(normalize(canonicalQuery), normalize(exercise.nome)) else 0.0
            )
            if (best == null || score > best.score) {
                best = Match(exercise, score)
            }
        }
        return best?.takeIf { it.score >= MATCH_THRESHOLD }
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
