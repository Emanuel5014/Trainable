package com.emanuel5014.trainable.webserver

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import com.emanuel5014.trainable.data.local.dao.AnalyticsDao
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.dao.WeightLogDao
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.ui.theme.ThemeColorStore
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.util.concurrent.TimeUnit
import java.io.InputStream
import java.util.Locale

private const val TAG = "TrainableWebServer"

private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

private fun daysToStartDate(days: Int): Long {
    return if (days <= 0) 0L else System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
}

private fun successJson(data: kotlinx.serialization.json.JsonElement) = buildJsonObject {
    put("success", true)
    put("data", data)
}

private fun errorJson(message: String) = buildJsonObject {
    put("success", false)
    put("error", message)
}

private val SUPPORTED_LANGS = listOf("en", "it", "es", "fr", "de", "pt")

private suspend fun resolveLanguage(userPrefsRepo: UserPreferencesRepository): String {
    val stored = userPrefsRepo.userLanguage.first()
    if (!stored.isNullOrEmpty() && stored != "system") return stored
    val sysLang = Locale.getDefault().language
    return if (sysLang in SUPPORTED_LANGS) sysLang else "en"
}

@Serializable
data class OverviewResponse(
    val totalVolume: Float,
    val totalSessions: Int,
    val trainingDays: Int,
    val personalBests: Int
)

@Serializable
data class PlanResponse(
    val id: Int,
    val name: String,
    val note: String?,
    val isActive: Boolean,
    val sessionsPerWeek: Int,
    val startDate: Long,
    val exercises: List<PlanExerciseResponse>
)

@Serializable
data class PlanExerciseResponse(
    val exerciseName: String,
    val category: String,
    val targetSets: Int,
    val targetReps: String,
    val targetRest: Int
)

@Serializable
data class SessionResponse(
    val id: Int,
    val planId: Int,
    val planName: String,
    val timestamp: Long,
    val noteSessione: String?,
    val totalVolume: Float,
    val totalSets: Int
)

@Serializable
data class SessionDetailResponse(
    val id: Int,
    val planId: Int,
    val planName: String,
    val timestamp: Long,
    val noteSessione: String?,
    val totalVolume: Float,
    val totalSets: Int,
    val sets: List<SetResponse>
)

@Serializable
data class SetResponse(
    val exerciseName: String,
    val category: String,
    val pesoSollevato: Float,
    val repsEffettive: Int,
    val numeroSerie: Int,
    val rpe: Int?,
    val note: String?
)

fun Application.configureServer(
    context: Context,
    workoutDao: WorkoutDao,
    analyticsDao: AnalyticsDao,
    exerciseDao: ExerciseDao,
    userDao: UserDao,
    weightLogDao: WeightLogDao,
    userPrefsRepo: UserPreferencesRepository
) {
    install(ContentNegotiation) {
        json(json)
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            Log.e(TAG, "Server error: ${cause.message}", cause)
            call.respondText(
                json.encodeToString(errorJson(cause.message ?: "Unknown error")),
                ContentType.Application.Json,
                HttpStatusCode.InternalServerError
            )
        }
    }

    routing {
        get("/") {
            call.respondText(context.assets.open("web/index.html").bufferedReader().readText(), ContentType.Text.Html)
        }
        get("/analytics") {
            call.respondText(context.assets.open("web/analytics.html").bufferedReader().readText(), ContentType.Text.Html)
        }
        get("/plans") {
            call.respondText(context.assets.open("web/plans.html").bufferedReader().readText(), ContentType.Text.Html)
        }
        get("/sessions") {
            call.respondText(context.assets.open("web/sessions.html").bufferedReader().readText(), ContentType.Text.Html)
        }
        get("/session/{id}") {
            call.respondText(context.assets.open("web/session-detail.html").bufferedReader().readText(), ContentType.Text.Html)
        }
        get("/style.css") {
            call.respondText(context.assets.open("web/style.css").bufferedReader().readText(), ContentType.Text.CSS)
        }
        get("/app.js") {
            call.respondText(context.assets.open("web/app.js").bufferedReader().readText(), ContentType.Application.JavaScript)
        }
        get("/icon.png") {
            call.respondBytes(context.assets.open("web/icon.png").readBytes(), ContentType.Image.PNG)
        }
        get("/icon.svg") {
            call.respondBytes(context.assets.open("web/icon.svg").readBytes(), ContentType.Image.SVG)
        }
        get("/github.svg") {
            call.respondBytes(context.assets.open("web/github.svg").readBytes(), ContentType.Image.SVG)
        }
        get("/kofi_symbol.svg") {
            call.respondBytes(context.assets.open("web/kofi_symbol.svg").readBytes(), ContentType.Image.SVG)
        }
        get("/i18n.js") {
            call.respondText(context.assets.open("web/i18n.js").bufferedReader().readText(), ContentType.Application.JavaScript)
        }

        // Serve M3E library files from assets/web/lib/
        get("/lib/{path...}") {
            val filePath = call.parameters.getAll("path")?.joinToString("/") ?: return@get
            try {
                val stream: InputStream = context.assets.open("web/lib/$filePath")
                val bytes = stream.readBytes()
                stream.close()
                val contentType = when {
                    filePath.endsWith(".js", ignoreCase = true) -> ContentType.Application.JavaScript
                    filePath.endsWith(".map", ignoreCase = true) -> ContentType.Application.Json
                    filePath.endsWith(".css", ignoreCase = true) -> ContentType.Text.CSS
                    else -> ContentType.Application.OctetStream
                }
                call.respondBytes(bytes, contentType)
            } catch (e: Exception) {
                call.respondText("Not Found", ContentType.Text.Plain, HttpStatusCode.NotFound)
            }
        }

        route("/api") {
            get("/user") {
                try {
                    val user = userDao.getUser().first()
                    if (user != null) {
                        call.respondText(json.encodeToString(successJson(buildJsonObject {
                            put("id", JsonPrimitive(user.id))
                            put("username", JsonPrimitive(user.username))
                            put("registrationDate", JsonPrimitive(user.dataIscrizione))
                        })), ContentType.Application.Json)
                    } else {
                        call.respondText(json.encodeToString(errorJson("No user found")), ContentType.Application.Json)
                    }
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            // Theme colors
            get("/theme") {
                try {
                    val dark = ThemeColorStore.darkColors
                    val light = ThemeColorStore.lightColors

                    call.respondText(json.encodeToString(successJson(buildJsonObject {
                        put("dark", buildJsonObject {
                            dark.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
                        })
                        put("light", buildJsonObject {
                            light.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
                        })
                    })), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            // All exercises for the exercise picker
            get("/exercises") {
                try {
                    val exercises = exerciseDao.getAllExercises().first()
                    val lang = resolveLanguage(userPrefsRepo)
                    val items = buildJsonArray {
                        exercises.forEach { ex ->
                            add(buildJsonObject {
                                put("id", JsonPrimitive(ex.id))
                                put("name", JsonPrimitive(ExerciseTranslations.translate(ex.nome, lang)))
                                put("category", JsonPrimitive(ExerciseTranslations.translateCategory(ex.categoria, lang)))
                            })
                        }
                    }
                    call.respondText(json.encodeToString(successJson(items)), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            // User preferences (language + weight unit)
            get("/preferences") {
                try {
                    val lang = resolveLanguage(userPrefsRepo)
                    val unit = userPrefsRepo.weightUnit.first()
                    call.respondText(json.encodeToString(successJson(buildJsonObject {
                        put("language", JsonPrimitive(lang))
                        put("weightUnit", JsonPrimitive(unit))
                    })), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            // Dashboard - Membership
            get("/dashboard/membership") {
                try {
                    val user = userDao.getUser().first()
                    val username = user?.username ?: "Athlete"
                    val expiryDate = userPrefsRepo.gymMembershipExpiryDate.first()
                    call.respondText(json.encodeToString(successJson(buildJsonObject {
                        put("username", JsonPrimitive(username))
                        if (expiryDate != null) put("expiryDate", JsonPrimitive(expiryDate))
                    })), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            // Dashboard - Weekly Goal
            get("/dashboard/weekly-goal") {
                try {
                    val weeklyGoal = userPrefsRepo.weeklyGoal.first()

                    val cal = java.util.Calendar.getInstance()
                    cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    val weekStartMillis = cal.timeInMillis

                    val allSessions = workoutDao.getAllSessions().first()
                    val workoutsThisWeek = allSessions.count { it.timestamp >= weekStartMillis }
                    val cardioCount = workoutDao.getCardioSessionCountSince(weekStartMillis).first()

                    call.respondText(json.encodeToString(successJson(buildJsonObject {
                        put("weeklyGoal", JsonPrimitive(weeklyGoal))
                        put("workoutsThisWeek", JsonPrimitive(workoutsThisWeek))
                        put("cardioWorkoutsThisWeek", JsonPrimitive(cardioCount))
                    })), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            get("/analytics/overview") {
                try {
                    val totalVolume = analyticsDao.getTotalVolume().first() ?: 0f
                    val sessions = workoutDao.getAllSessions().first()
                    val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
                    val trainingDays = analyticsDao.getTrainingDays(thirtyDaysAgo, System.currentTimeMillis()).first()
                    val personalBests = analyticsDao.getAllPersonalBests().first().count { it.maxWeight > 0 }

                    call.respondText(json.encodeToString(successJson(
                        json.encodeToJsonElement(OverviewResponse(totalVolume, sessions.size, trainingDays, personalBests))
                    )), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            // Consistency calendar data (GitHub-style)
            get("/analytics/consistency-calendar") {
                try {
                    val weeks = call.request.queryParameters["weeks"]?.toIntOrNull() ?: 14
                    val startDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis((weeks * 7 + 1).toLong())
                    val sessions = workoutDao.getAllSessions().first()
                        .filter { it.timestamp >= startDate && it.isFinished }

                    val dayMap = mutableMapOf<String, Int>()
                    sessions.forEach { session ->
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getDefault()
                        val dateKey = sdf.format(java.util.Date(session.timestamp))
                        dayMap[dateKey] = (dayMap[dateKey] ?: 0) + 1
                    }

                    call.respondText(json.encodeToString(successJson(buildJsonObject {
                        put("weeks", JsonPrimitive(weeks))
                        put("days", buildJsonArray {
                            val cal = java.util.Calendar.getInstance()
                            cal.timeInMillis = startDate
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            sdf.timeZone = java.util.TimeZone.getDefault()
                            val endCal = java.util.Calendar.getInstance()
                            endCal.timeInMillis = System.currentTimeMillis()
                            while (cal.before(endCal) || cal == endCal) {
                                val dateKey = sdf.format(cal.time)
                                val count = dayMap[dateKey] ?: 0
                                add(buildJsonObject {
                                    if (count > 0) {
                                        val maxInRange = dayMap.values.maxOrNull() ?: 1
                                        val intensity = when {
                                            count > maxInRange * 0.75 -> 4
                                            count > maxInRange * 0.5 -> 3
                                            count > maxInRange * 0.25 -> 2
                                            else -> 1
                                        }
                                        put("level", JsonPrimitive(intensity))
                                    } else {
                                        put("level", JsonPrimitive(0))
                                    }
                                    put("date", JsonPrimitive(dateKey))
                                    put("count", JsonPrimitive(count))
                                })
                                cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                            }
                        })
                    })), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            get("/analytics/volume") {
                try {
                    val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 90
                    val startDate = daysToStartDate(days)
                    val volumeHistory = analyticsDao.getVolumeHistory(startDate).first()

                    val items = buildJsonArray {
                        volumeHistory.forEach { dv ->
                            add(buildJsonObject {
                                put("volume", dv.volume)
                                put("timestamp", dv.timestamp)
                            })
                        }
                    }
                    call.respondText(json.encodeToString(successJson(items)), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            get("/analytics/personal-bests") {
                try {
                    val pbs = analyticsDao.getAllPersonalBests().first()
                    val lang = resolveLanguage(userPrefsRepo)
                    val items = buildJsonArray {
                        pbs.forEach { pb ->
                            add(buildJsonObject {
                                put("exerciseId", pb.exerciseId)
                                put("exerciseName", pb.exerciseName?.let { ExerciseTranslations.translate(it, lang) })
                                put("category", pb.category?.let { ExerciseTranslations.translateCategory(it, lang) })
                                put("maxWeight", pb.maxWeight)
                                put("reps", pb.reps)
                            })
                        }
                    }
                    call.respondText(json.encodeToString(successJson(items)), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            // 1RM progress history for a specific exercise
            get("/analytics/exercise-progress/{exerciseId}") {
                try {
                    val exerciseId = call.parameters["exerciseId"]?.toIntOrNull()
                    if (exerciseId == null) {
                        call.respondText(json.encodeToString(errorJson("Invalid exercise ID")), ContentType.Application.Json)
                        return@get
                    }
                    val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 90
                    val startDate = daysToStartDate(days)
                    val history = analyticsDao.getExerciseProgressHistory(exerciseId, startDate).first()
                    val items = buildJsonArray {
                        history.forEach { h ->
                            add(buildJsonObject {
                                put("value", JsonPrimitive(h.maxValue))
                                put("timestamp", JsonPrimitive(h.timestamp))
                            })
                        }
                    }
                    call.respondText(json.encodeToString(successJson(items)), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            get("/analytics/volume-by-category") {
                try {
                    val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 30
                    val startDate = daysToStartDate(days)
                    val categoryVolume = analyticsDao.getVolumeByCategory(startDate).first()

                    val items = buildJsonArray {
                        categoryVolume.forEach { cv ->
                            add(buildJsonObject {
                                put("category", cv.category)
                                put("volume", cv.volume)
                            })
                        }
                    }
                    call.respondText(json.encodeToString(successJson(items)), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            get("/analytics/strength-index") {
                try {
                    val startDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
                    val strengthIndex = analyticsDao.getStrengthIndex(startDate, startDate - TimeUnit.DAYS.toMillis(30)).first()
                    call.respondText(json.encodeToString(successJson(JsonPrimitive(strengthIndex ?: 0f))), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            // Body weight history
            get("/analytics/body-weight") {
                try {
                    val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 90
                    val startDate = daysToStartDate(days)
                    val weightLogs = weightLogDao.getWeightHistory(startDate).first()

                    val items = buildJsonArray {
                        weightLogs.forEach { wl ->
                            add(buildJsonObject {
                                put("weight", wl.pesoCorporeo)
                                put("timestamp", wl.timestamp)
                            })
                        }
                    }
                    call.respondText(json.encodeToString(successJson(items)), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            // Plan volume history (tonnage chart for each plan)
            get("/analytics/plan-volume-history/{planId}") {
                try {
                    val planId = call.parameters["planId"]?.toIntOrNull()
                    if (planId == null) {
                        call.respondText(json.encodeToString(errorJson("Invalid plan ID")), ContentType.Application.Json)
                        return@get
                    }
                    val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 90
                    val startDate = daysToStartDate(days)
                    val volumeHistory = analyticsDao.getVolumeHistoryForPlan(planId, startDate).first()

                    val items = buildJsonArray {
                        volumeHistory.forEach { dv ->
                            add(buildJsonObject {
                                put("volume", dv.volume)
                                put("timestamp", dv.timestamp)
                            })
                        }
                    }
                    call.respondText(json.encodeToString(successJson(items)), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            get("/plans") {
                try {
                    val plans = workoutDao.getAllPlansSorted().first()
                    val plansWithDetails = workoutDao.getPlansWithDetails(plans.map { it.id })
                    val lang = resolveLanguage(userPrefsRepo)

                    val items = buildJsonArray {
                        plansWithDetails.forEach { pwd ->
                            add(buildJsonObject {
                                put("id", pwd.plan.id)
                                put("name", pwd.plan.nome)
                                if (pwd.plan.note != null) put("note", pwd.plan.note!!)
                                put("isActive", pwd.plan.isActive)
                                put("startDate", pwd.plan.dataInizio)
                                if (pwd.plan.dataFine != null) put("endDate", pwd.plan.dataFine!!)
                                put("exercises", buildJsonArray {
                                    pwd.exercises.forEach { ex ->
                                        add(buildJsonObject {
                                            put("exerciseName", ExerciseTranslations.translate(ex.exercise.nome, lang))
                                            put("category", ExerciseTranslations.translateCategory(ex.exercise.categoria, lang))
                                            put("targetSets", ex.planExercise.serieTarget)
                                            put("targetReps", ex.planExercise.repsTarget)
                                            put("targetRest", ex.planExercise.recuperoTarget)
                                        })
                                    }
                                })
                                put("images", buildJsonArray {
                                    pwd.images.forEach { img ->
                                        add(JsonPrimitive(img.imageUri))
                                    }
                                })
                            })
                        }
                    }
                    call.respondText(json.encodeToString(successJson(items)), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            // Serve plan image by URI or file path
            get("/plan-image") {
                try {
                    val uriStr = call.request.queryParameters["uri"]
                    if (uriStr == null) {
                        call.respondText(json.encodeToString(errorJson("No URI provided")), ContentType.Application.Json)
                        return@get
                    }
                    val rawPath = java.net.URLDecoder.decode(uriStr, "UTF-8")
                    val cleanPath = rawPath.removePrefix("file://")

                    val inputStream = try {
                        // Try direct path first
                        val file = File(cleanPath)
                        if (file.exists()) {
                            java.io.FileInputStream(file)
                        } else {
                            // Fallback: try routine_images/ subdirectory
                            val fileName = cleanPath.substringAfterLast("/")
                            val fallback1 = File(context.filesDir, "routine_images/$fileName")
                            if (fallback1.exists()) {
                                java.io.FileInputStream(fallback1)
                            } else {
                                // Fallback: try root filesDir
                                val fallback2 = File(context.filesDir, fileName)
                                if (fallback2.exists()) {
                                    java.io.FileInputStream(fallback2)
                                } else {
                                    null
                                }
                            }
                        }
                    } catch (_: Exception) { null }

                    if (inputStream != null) {
                        val bytes = inputStream.readBytes()
                        inputStream.close()
                        val contentType = when {
                            rawPath.endsWith(".png", ignoreCase = true) -> ContentType.Image.PNG
                            else -> ContentType.Image.JPEG
                        }
                        call.respondBytes(bytes, contentType)
                    } else {
                        call.respondText(json.encodeToString(errorJson("Image not found")), ContentType.Application.Json)
                    }
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            get("/sessions") {
                try {
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                    val dateParam = call.request.queryParameters["date"]
                    val planIdParam = call.request.queryParameters["planId"]?.toIntOrNull()
                    val daysParam = call.request.queryParameters["days"]?.toIntOrNull()
                    var sessions = workoutDao.getAllSessionsWithDetails().first()

                    if (dateParam != null) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getDefault()
                        val date = sdf.parse(dateParam)
                        if (date != null) {
                            val dayStart = date.time
                            val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1)
                            sessions = sessions.filter { it.session.timestamp in dayStart until dayEnd }
                        }
                    }

                    if (planIdParam != null) {
                        sessions = sessions.filter { it.session.planId == planIdParam }
                    }

                    if (daysParam != null && daysParam > 0) {
                        val startDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysParam.toLong())
                        sessions = sessions.filter { it.session.timestamp >= startDate }
                    }

                    sessions = sessions.take(limit)

                    val items = buildJsonArray {
                        sessions.forEach { swd ->
                            val volume = swd.sets.sumOf { (it.setLog.pesoSollevato * it.setLog.repsEffettive).toDouble() }.toFloat()
                            add(buildJsonObject {
                                put("id", swd.session.id)
                                put("planId", swd.session.planId)
                                put("planName", swd.plan.nome)
                                put("timestamp", swd.session.timestamp)
                                swd.session.noteSessione?.let { put("noteSessione", it) }
                                put("totalVolume", volume)
                                put("totalSets", swd.sets.size)
                            })
                        }
                    }
                    call.respondText(json.encodeToString(successJson(items)), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }

            get("/sessions/{id}") {
                try {
                    val sessionId = call.parameters["id"]?.toIntOrNull()
                    if (sessionId == null) {
                        call.respondText(json.encodeToString(errorJson("Invalid session ID")), ContentType.Application.Json)
                        return@get
                    }
                    val session = workoutDao.getSessionWithDetails(sessionId).first()
                    if (session == null) {
                        call.respondText(json.encodeToString(errorJson("Session not found")), ContentType.Application.Json)
                        return@get
                    }

                    val lang = resolveLanguage(userPrefsRepo)
                    val volume = session.sets.sumOf { (it.setLog.pesoSollevato * it.setLog.repsEffettive).toDouble() }.toFloat()
                    val setsArray = buildJsonArray {
                        session.sets.forEach { swd ->
                            add(buildJsonObject {
                                put("exerciseName", ExerciseTranslations.translate(swd.exercise.nome, lang))
                                put("category", ExerciseTranslations.translateCategory(swd.exercise.categoria, lang))
                                put("pesoSollevato", swd.setLog.pesoSollevato)
                                put("repsEffettive", swd.setLog.repsEffettive)
                                put("numeroSerie", swd.setLog.numeroSerie)
                                swd.setLog.rpe?.let { put("rpe", it) }
                                swd.setLog.note?.let { put("note", it) }
                            })
                        }
                    }

                    call.respondText(json.encodeToString(successJson(buildJsonObject {
                        put("id", session.session.id)
                        put("planId", session.session.planId)
                        put("planName", session.plan.nome)
                        put("timestamp", session.session.timestamp)
                        session.session.noteSessione?.let { put("noteSessione", it) }
                        put("totalVolume", volume)
                        put("totalSets", session.sets.size)
                        put("sets", setsArray)
                    })), ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(json.encodeToString(errorJson(e.message ?: "Unknown error")), ContentType.Application.Json)
                }
            }
        }
    }
}
