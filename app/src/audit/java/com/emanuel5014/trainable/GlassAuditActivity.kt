package com.emanuel5014.trainable

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.emanuel5014.trainable.data.local.GymDatabase
import com.emanuel5014.trainable.data.local.entity.*
import com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming
import com.emanuel5014.trainable.ui.components.BottomNavBarFlo
import com.emanuel5014.trainable.ui.navigation.*
import com.emanuel5014.trainable.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

/** Only compiled in the isolated audit package. Intent extras select normal production destinations. */
@AndroidEntryPoint
class GlassAuditActivity : FragmentActivity() {
    @Inject lateinit var database: GymDatabase
    @Inject lateinit var preferences: com.emanuel5014.trainable.data.repository.UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(BuildConfig.APPLICATION_ID.endsWith(".audit"))
        enableEdgeToEdge()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { seed(); seedCompactWorkout() }
            android.util.Log.i("GlassAudit", "Fixture exercises=" + database.workoutDao().getPlanWithDetails(901).first()?.exercises?.size)
            val destination: Any = when (intent.getStringExtra("screen")) {
                "routine" -> RoutineDetail(901)
                "edit" -> EditWorkoutSession(901)
                "compare" -> CompareSessions(901, 902)
                "workout" -> WorkoutExecution(planId = 901)
                "workout-compact" -> WorkoutExecution(planId = 905)
                "physical" -> PhysicalCheck
                "physical-settings" -> PhysicalCheckSettings
                "physical-compare" -> PhysicalCheckCompare(901, 902)
                "settings" -> Settings
                "workout-settings" -> WorkoutSettings
                "personalization" -> PersonalizationSettings
                "notifications" -> NotificationSettings
                "ai" -> AiSettings
                "exercises" -> Exercises
                "journal" -> JournalTags
                "import" -> ImportHistory
                "donors" -> Donors
                "maxes" -> OneRepMaxes
                "report" -> Report("901")
                else -> MainTabs
            }
            setContent {
                val background by preferences.appBackground.collectAsState(initial = com.emanuel5014.trainable.data.model.AppBackground())
                val motion by preferences.glassMotionEnabled.collectAsState(initial = true)
                val glassEnabled by preferences.liquidGlassEnabled.collectAsState(initial = true)
                val savedIntensity by preferences.glassIntensity.collectAsState(initial = .75f)
                GymTrackingTheme(
                    dynamicColor = false, seedColor = 0xFF85BDE8.toInt(),
                    darkTheme = intent.getBooleanExtra("oled", false) || !intent.getBooleanExtra("light", false),
                    oledTheme = intent.getBooleanExtra("oled", false),
                    liquidGlassEnabled = glassEnabled && !intent.getBooleanExtra("solid", false),
                    glassIntensity = intent.getFloatExtra("intensity", savedIntensity),
                    appBackground = background,
                    glassMotionEnabled = motion
                ) {
                    if (intent.getBooleanExtra("motionProbe", false)) {
                        val light = LocalGlassLight.current
                        LaunchedEffect(light) {
                            snapshotFlow { light.bias }.collect { android.util.Log.i("GlassAuditMotion", it.toString()) }
                        }
                    }
                    CompositionLocalProvider(LocalAdvancedProgramming provides (intent.getStringExtra("screen") != "workout-compact")) {
                        val nav = rememberNavController()
                        val pager = rememberPagerState(initialPage = intent.getIntExtra("tab", 0)) { 4 }
                        val entry by nav.currentBackStackEntryAsState()
                        Box(Modifier.fillMaxSize()) {
                            if (intent.getStringExtra("screen") == "onboarding") {
                                CompositionLocalProvider(LocalLightweightGlassPanels provides true) {
                                    com.emanuel5014.trainable.ui.screens.onboarding.OnboardingScreen(onFinished = {})
                                }
                            } else MainNavGraph(nav, pager, startDestination = destination, modifier = Modifier.fillMaxSize())
                            if (entry?.destination?.route?.contains("MainTabs") == true) {
                                LocalGlassBackdrop.current?.let { backdrop ->
                                    BottomNavBarFlo(nav, pager, backdrop, !intent.getBooleanExtra("light", false),
                                        Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun seedCompactWorkout() {
        val workout = database.workoutDao()
        if (workout.getPlanWithDetails(905).first()?.exercises?.isNotEmpty() == true) return
        database.exerciseDao().upsertExercises(listOf(ExerciseEntity(9006, "Arnold Press", "Shoulders")))
        workout.insertPlan(WorkoutPlanEntity(id = 905, userId = 1, nome = "ciao", dataInizio = System.currentTimeMillis()))
        workout.insertPlanExercise(PlanExerciseEntity(id = 9506, planId = 905, exerciseId = 9006,
            serieTarget = 3, repsTarget = "8", recuperoTarget = 0, ordine = 0, exerciseType = "strength"))
    }

    private suspend fun seed() {
        if (database.workoutDao().getPlanWithDetails(901).first()?.exercises?.isNotEmpty() == true) return
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        database.userDao().insertUser(UserEntity(1, "Alex", now - 120 * day))
        val exercises = listOf(
            ExerciseEntity(9001, "Barbell bench press", "Chest", descrizione = "Controlled descent, stable shoulders.", isFavorite = true),
            ExerciseEntity(9002, "Seated cable row", "Back"),
            ExerciseEntity(9003, "Incline dumbbell press", "Chest"),
            ExerciseEntity(9004, "Face pull", "Shoulders"),
            ExerciseEntity(9005, "Treadmill run", "Cardio")
        )
        database.exerciseDao().upsertExercises(exercises)
        val workout = database.workoutDao()
        workout.insertPlan(WorkoutPlanEntity(id = 901, userId = 1, nome = "Upper body • Strength & control",
            dataInizio = now - 21 * day, dataFine = now + 35 * day, note = "Four-week progression. Keep two reps in reserve.",
            sessioniTargetSettimana = 3, giorniSettimana = "1,3,5", weeksCount = 4, currentWeek = 2))
        workout.insertPlan(WorkoutPlanEntity(id = 902, userId = 1, nome = "Lower body & conditioning",
            dataInizio = now - 21 * day, ordine = 1))
        workout.insertPlan(WorkoutPlanEntity(id = 903, userId = 1, nome = "Foundation • completed",
            dataInizio = now - 100 * day, isActive = false, ordine = 2))
        exercises.forEachIndexed { index, exercise ->
            val pe = 901 + index
            workout.insertPlanExercise(PlanExerciseEntity(id = pe, planId = 901, exerciseId = exercise.id,
                serieTarget = if (index == 4) 1 else 3, repsTarget = "8-12", recuperoTarget = 90, ordine = index,
                supersetId = if (index in 2..3) "audit-superset" else null,
                exerciseType = if (index == 4) "cardio" else "strength",
                durataTargetSecondi = if (index == 4) 900 else null))
            if (index < 4) workout.insertBlocks((1..4).map { week ->
                PlanExerciseBlockEntity(planExerciseId = pe, week = week, sets = 3, reps = "${12 - week}",
                    restSeconds = 90, note = "Smooth tempo")
            })
        }
        repeat(8) { index ->
            val sid = 901 + index
            workout.insertSession(WorkoutSessionEntity(id = sid, planId = 901, timestamp = now - (index * 3 + 1) * day,
                isFinished = true, durationMs = 3_000_000L, journalNote = "Good focus and steady technique.", rating = 4))
            workout.insertSets(exercises.take(4).flatMapIndexed { order, exercise ->
                (1..3).map { set -> SetLogEntity(sessionId = sid, exerciseId = exercise.id,
                    pesoSollevato = 40f + order * 5 + (8 - index), repsEffettive = 10, numeroSerie = set,
                    ordineEsercizio = order, rpe = 8f) }
            })
        }
        repeat(8) { index ->
            database.weightLogDao().insertWeight(WeightLogEntity(userId = 1, pesoCorporeo = 78f + index * .3f,
                timestamp = now - index * 7 * day))
        }
        listOf(901, 902).forEachIndexed { index, id ->
            database.physicalCheckDao().insertPhysicalCheck(PhysicalCheckEntity(id = id, timestamp = now - index * 30 * day,
                peso = 78f + index, note = "Monthly check-in", fotoFilenames = ""))
        }
        database.oneRepMaxDao().insert(OneRepMaxEntity(exerciseId = 9001, weightKg = 85f, date = now))
        database.journalDao().insertTag(SessionTagEntity(id = 901, name = "Focused", nameKey = "focused", colorIndex = 1))
        database.journalDao().insertTag(SessionTagEntity(id = 902, name = "Gym", nameKey = "gym", colorIndex = 2))
        database.journalDao().insertRefs(listOf(SessionTagRefEntity(901, 901), SessionTagRefEntity(901, 902)))
        getSharedPreferences("analytics_prefs", MODE_PRIVATE).edit()
            .putString("widget_order", "calendar,weight,category_volume,exercise_9001_OneMonth_1,volume_901_OneMonth_2")
            .putBoolean("show_progress_cards", true).apply()
    }
}
