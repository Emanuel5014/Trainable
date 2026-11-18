package com.emanuel5014.trainable.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.emanuel5014.trainable.domain.heart.HeartMath
import com.emanuel5014.trainable.domain.heart.HeartSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Heart rate, calories and recorded workouts from Health Connect, where Mi Fitness, Zepp, Samsung Health, Garmin
 * and the others write what the watch or band measured. Several apps may write the same span: only the one with
 * the most heart-rate readings is used, so calories are never counted twice.
 */
class HealthConnectHeartRateSource(private val context: Context) : HeartRateSource {

    private val client: HealthConnectClient?
        get() = if (isAvailable(context)) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() else null

    override suspend fun hasPermissions(): Boolean {
        val client = client ?: return false
        return runCatching { client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS) }
            .getOrDefault(false)
    }

    override suspend fun read(startMs: Long, endMs: Long): HeartReading? = withContext(Dispatchers.IO) {
        val client = client ?: return@withContext null
        runCatching {
            // Health Connect only returns records lying inside the asked range, so a heart-rate or calorie block that
            // started before the workout would be missed: the range is widened and everything clipped here
            val heart = readAll(client, HeartRateRecord::class, startMs - HEART_MARGIN_MS, endMs + HEART_MARGIN_MS)
            val active = readAll(client, ActiveCaloriesBurnedRecord::class, startMs - KCAL_MARGIN_MS, endMs + KCAL_MARGIN_MS)
            val total = readAll(client, TotalCaloriesBurnedRecord::class, startMs - KCAL_MARGIN_MS, endMs + KCAL_MARGIN_MS)
            val samplesByApp = heart.groupBy { it.metadata.dataOrigin.packageName }.mapValues { (_, records) ->
                records.flatMap { record ->
                    record.samples
                        .map { HeartSample(it.time.toEpochMilli(), it.beatsPerMinute.toInt()) }
                        .filter { it.timeMs in startMs until endMs }
                }
            }.filterValues { it.isNotEmpty() }
            val chosenApp = samplesByApp.maxByOrNull { (_, samples) -> samples.size }?.key

            /** Calories inside the workout per app; a block longer than [KCAL_MARGIN_MS] (a daily total) says nothing about it. */
            fun <R : Record> kcalByApp(records: List<R>, start: (R) -> Instant, end: (R) -> Instant, kcal: (R) -> Double): Map<String, Double> =
                records
                    .filter { end(it).toEpochMilli() - start(it).toEpochMilli() <= KCAL_MARGIN_MS }
                    .groupBy { it.metadata.dataOrigin.packageName }
                    .mapValues { (_, list) ->
                        list.sumOf { kcal(it) * HeartMath.overlapShare(start(it).toEpochMilli(), end(it).toEpochMilli(), startMs, endMs) }
                    }
                    .filterValues { it > 0.0 }

            // Only the calories of the app that measured the heart rate: the phone's own estimate (steps, written by
            // Android or Fit) knows nothing of a set of squats, and two apps are never added up
            val activeKcal = chosenApp?.let { kcalByApp(active, { it.startTime }, { it.endTime }) { it.energy.inKilocalories }[it] }
            val totalKcal = chosenApp?.let { kcalByApp(total, { it.startTime }, { it.endTime }) { it.energy.inKilocalories }[it] }
            Log.i(
                TAG,
                "read ${Instant.ofEpochMilli(startMs)}..${Instant.ofEpochMilli(endMs)}: heart records ${heart.size} " +
                    "from ${heart.map { it.metadata.dataOrigin.packageName }.distinct()}, used $chosenApp " +
                    "(${samplesByApp[chosenApp]?.size ?: 0} readings), kcal $activeKcal/$totalKcal"
            )

            HeartReading(
                samples = samplesByApp[chosenApp].orEmpty(),
                activeKcal = activeKcal,
                totalKcal = totalKcal,
                source = chosenApp?.let(::appName)
            )
        }.onFailure { Log.w(TAG, "read failed", it) }.getOrNull()
    }

    override suspend fun recordedExercises(startMs: Long, endMs: Long): List<RecordedExercise> =
        withContext(Dispatchers.IO) {
            val client = client ?: return@withContext emptyList()
            runCatching {
                readAll(client, ExerciseSessionRecord::class, startMs, endMs)
                    .map { RecordedExercise(it.startTime.toEpochMilli(), it.endTime.toEpochMilli()) }
            }.onFailure { Log.w(TAG, "sessions failed", it) }.getOrDefault(emptyList())
        }

    private suspend fun <T : Record> readAll(client: HealthConnectClient, type: KClass<T>, startMs: Long, endMs: Long): List<T> {
        val filter = TimeRangeFilter.between(Instant.ofEpochMilli(startMs), Instant.ofEpochMilli(endMs))
        val records = mutableListOf<T>()
        var token: String? = null
        var pages = 0
        do {
            val response = client.readRecords(ReadRecordsRequest(recordType = type, timeRangeFilter = filter, pageToken = token))
            records += response.records
            token = response.pageToken
            pages++
        } while (!token.isNullOrEmpty() && pages < MAX_PAGES)
        return records
    }

    /** The app's own name when the phone tells it, a known name otherwise, the package as a last resort. */
    private fun appName(packageName: String): String {
        KNOWN_APPS[packageName]?.let { return it }
        return runCatching {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }

    companion object {
        private const val MAX_PAGES = 50
        private const val TAG = "TrainableHeart"

        /** Heart-rate records may span a whole day (one reading every few minutes): read a day around the workout. */
        private const val HEART_MARGIN_MS = 24 * 3_600_000L

        /** Calorie blocks up to this long are clipped to the workout; longer ones are left out. */
        private const val KCAL_MARGIN_MS = 3 * 3_600_000L

        /** What the app asks for: read only, never write. */
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        )

        fun sdkStatus(context: Context): Int = HealthConnectClient.getSdkStatus(context)

        /** Health Connect is on the phone (built into Android 14+, an app before that). */
        fun isAvailable(context: Context): Boolean = sdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

        private val KNOWN_APPS = mapOf(
            "com.mi.health" to "Mi Fitness",
            "com.xiaomi.wearable" to "Mi Fitness",
            // better-mi-fitness-sync: copies Mi Fitness data from the Xiaomi cloud into Health Connect
            "com.bettermifitness.sync" to "Mi Fitness",
            "com.xiaomi.hm.health" to "Zepp Life",
            "com.huami.watch.hmwatchmanager" to "Zepp",
            "nodomain.freeyourgadget.gadgetbridge" to "Gadgetbridge",
            "com.sec.android.app.shealth" to "Samsung Health",
            "com.google.android.apps.fitness" to "Google Fit",
            "com.fitbit.FitbitMobile" to "Fitbit",
            "com.garmin.android.apps.connectmobile" to "Garmin Connect",
            "com.huawei.health" to "Huawei Health",
            "com.polar.beat" to "Polar",
            "fi.polar.polarflow" to "Polar Flow",
            "com.withings.wiscale2" to "Withings"
        )
    }
}
