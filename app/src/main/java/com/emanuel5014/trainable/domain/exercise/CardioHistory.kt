package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.relation.CardioSetWithSession

/**
 * What one workout did for one cardio activity. An activity done more than once in the workout (a treadmill warm-up
 * and cool-down around the lifting) is still one performance: [runs] tell the times apart.
 */
data class CardioPerformance(
    val sessionId: Int,
    val timestamp: Long,
    val sessionNote: String?,
    val planName: String,
    /** In the order done: by run, then by set. */
    val sets: List<CardioLogEntity>
) {
    /** The sets of each run of the activity, in the order done. */
    val runs: List<List<CardioLogEntity>> = sets.groupBy { it.ordineEsercizio }.values.toList()

    val totalSeconds: Int = sets.sumOf { it.durataSecondi }

    val totalKm: Float = sets.sumOf { it.distanza.toDouble() }.toFloat()

    /** Null when the workout has no distance, or too little of it (or of time) to say anything. */
    val averageSpeedKmh: Float? = CardioHistory.speedKmh(totalSeconds, totalKm)
}

/** The personal records of a cardio activity; the ones its workouts give no value for are null. */
data class CardioRecords(
    val sessionCount: Int,
    val setCount: Int,
    val totalSeconds: Long,
    val totalKm: Float,
    val firstTimestamp: Long?,
    val lastTimestamp: Long?,
    /** Seconds of the longest workout. */
    val longestSession: RecordValue?,
    /** Kilometres of the longest-distance workout. */
    val farthestSession: RecordValue?,
    /** Km/h of the workout with the best average speed. */
    val bestSpeed: RecordValue?
)

/** Every finished workout of one cardio activity, newest first, and its records. */
data class CardioHistory(
    val performances: List<CardioPerformance>,
    val records: CardioRecords
) {
    companion object {
        /** Below this a distance or a time says nothing about speed (40 m in five minutes, 600 m in twenty seconds). */
        private const val MIN_KM_FOR_SPEED = 0.1f
        private const val MIN_SECONDS_FOR_SPEED = 60

        fun speedKmh(seconds: Int, km: Float): Float? =
            if (km >= MIN_KM_FOR_SPEED && seconds >= MIN_SECONDS_FOR_SPEED) km / (seconds / 3600f) else null

        /** [rows] are the sets of the activity, the newest workout first. */
        fun of(rows: List<CardioSetWithSession>): CardioHistory {
            val bySession = LinkedHashMap<Int, MutableList<CardioSetWithSession>>()
            rows.forEach { bySession.getOrPut(it.cardio.sessionId) { mutableListOf() }.add(it) }
            val performances = bySession.values.map { group ->
                val first = group.first()
                CardioPerformance(
                    sessionId = first.cardio.sessionId,
                    timestamp = first.sessionTimestamp,
                    sessionNote = first.sessionNote,
                    planName = first.planName,
                    sets = group.map { it.cardio }.sortedWith(compareBy({ it.ordineEsercizio }, { it.numeroSerie }, { it.id }))
                )
            }
            return CardioHistory(performances, recordsOf(performances))
        }

        private fun recordsOf(performances: List<CardioPerformance>): CardioRecords {
            val timestamps = performances.map { it.timestamp }
            return CardioRecords(
                sessionCount = performances.size,
                setCount = performances.sumOf { it.sets.size },
                totalSeconds = performances.sumOf { it.totalSeconds.toLong() },
                totalKm = performances.sumOf { it.totalKm.toDouble() }.toFloat(),
                firstTimestamp = timestamps.minOrNull(),
                lastTimestamp = timestamps.maxOrNull(),
                longestSession = performances.best { it.totalSeconds.toFloat() },
                farthestSession = performances.best { it.totalKm },
                bestSpeed = performances.best { it.averageSpeedKmh ?: 0f }
            )
        }

        /** The workout with the highest [score], when above zero; the more recent one wins a tie. */
        private fun List<CardioPerformance>.best(score: (CardioPerformance) -> Float): RecordValue? =
            filter { score(it) > 0f }
                .maxWithOrNull(compareBy({ score(it) }, { it.timestamp }))
                ?.let { RecordValue(score(it), it.timestamp) }
    }
}
