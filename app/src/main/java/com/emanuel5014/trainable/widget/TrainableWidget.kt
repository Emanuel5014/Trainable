package com.emanuel5014.trainable.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.ui.theme.getAppColorScheme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun workoutRepository(): WorkoutRepository
    fun userPreferencesRepository(): UserPreferencesRepository
}

class TrainableWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val workoutRepository = entryPoint.workoutRepository()
        val prefs = entryPoint.userPreferencesRepository()

        provideContent {
            // Collect database flows as state reactively
            val plans by workoutRepository.getActivePlans().collectAsState(initial = emptyList())
            val allSessions by workoutRepository.getAllSessions().collectAsState(initial = emptyList())
            val unfinishedSessions by workoutRepository.getUnfinishedSessionsWithPlanName().collectAsState(initial = emptyList())

            val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ColorProviders(
                    light = dynamicLightColorScheme(context),
                    dark = dynamicDarkColorScheme(context)
                )
            } else {
                ColorProviders(
                    light = lightColorScheme(),
                    dark = darkColorScheme()
                )
            }

            // Re-use Suggested Plan Logic
            val calendar = Calendar.getInstance()
            val weekStartMillis = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val todayStartMillis = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val workoutsThisWeek = allSessions.filter { it.timestamp >= weekStartMillis }
            val lastSession = allSessions.firstOrNull()
            val trainedToday = lastSession != null && lastSession.timestamp >= todayStartMillis

            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val currentDayValue = when (today) {
                Calendar.MONDAY -> "1"
                Calendar.TUESDAY -> "2"
                Calendar.WEDNESDAY -> "3"
                Calendar.THURSDAY -> "4"
                Calendar.FRIDAY -> "5"
                Calendar.SATURDAY -> "6"
                Calendar.SUNDAY -> "7"
                else -> "1"
            }

            val planForToday = plans.find { it.giorniSettimana?.split(",")?.contains(currentDayValue) == true }
            val idsPerformedThisWeek = workoutsThisWeek.map { it.planId }.toSet()
            val firstNotPerformedThisWeek = plans.find { it.id !in idsPerformedThisWeek }

            val todayPlan = if (!trainedToday) planForToday else null

            var suggestedPlan: WorkoutPlanEntity? = null
            if (plans.isNotEmpty()) {
                suggestedPlan = if (firstNotPerformedThisWeek != null) {
                    firstNotPerformedThisWeek
                } else {
                    val lastPlanIndex = plans.indexOfFirst { it.id == lastSession?.planId }
                    if (lastPlanIndex != -1) {
                        plans[(lastPlanIndex + 1) % plans.size]
                    } else {
                        plans.first()
                    }
                }
            }

            val finalSuggestedPlan = if (suggestedPlan?.id == todayPlan?.id) null else suggestedPlan

            GlanceTheme(colors = colors) {
                WidgetContent(
                    context = context,
                    unfinishedSession = unfinishedSessions.firstOrNull(),
                    todayPlan = todayPlan,
                    suggestedPlan = finalSuggestedPlan
                )
            }
        }
    }

    companion object {
        fun update(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TrainableWidgetReceiver::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids.isNotEmpty()) {
                val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                coroutineScope.launch {
                    try {
                        TrainableWidget().updateAll(context)
                        val updateIntent = Intent(context, TrainableWidgetReceiver::class.java).apply {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }
                        context.sendBroadcast(updateIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetContent(
    context: Context,
    unfinishedSession: SessionWithPlanName?,
    todayPlan: WorkoutPlanEntity?,
    suggestedPlan: WorkoutPlanEntity?
) {
    val isResume = unfinishedSession != null
    val displayPlanName = when {
        unfinishedSession != null -> unfinishedSession.displayName
        todayPlan != null -> todayPlan.nome
        suggestedPlan != null -> suggestedPlan.nome
        else -> null
    }

    val stateLabel = when {
        isResume -> context.getString(R.string.widget_state_in_progress)
        todayPlan != null -> context.getString(R.string.widget_state_suggested_today)
        suggestedPlan != null -> context.getString(R.string.widget_state_suggested)
        else -> context.getString(R.string.widget_state_welcome)
    }

    val buttonText = when {
        isResume -> context.getString(R.string.widget_action_resume)
        todayPlan != null || suggestedPlan != null -> context.getString(R.string.widget_action_start)
        else -> context.getString(R.string.widget_action_open_app)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        when {
            unfinishedSession != null -> {
                putExtra("workout_session_id", unfinishedSession.session.id)
                putExtra("workout_plan_id", unfinishedSession.session.planId)
            }
            todayPlan != null -> {
                putExtra("workout_plan_id", todayPlan.id)
            }
            suggestedPlan != null -> {
                putExtra("workout_plan_id", suggestedPlan.id)
            }
        }
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(24.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.Top
            ) {
                // Header Logo & Label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.padding(bottom = 4.dp)
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_app_logo),
                        contentDescription = "Trainable Logo",
                        modifier = GlanceModifier.size(14.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "TRAINABLE",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Status Label
                Text(
                    text = stateLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(1.dp))

                // Plan/Routine Name
                Text(
                    text = displayPlanName ?: context.getString(R.string.widget_start_routine),
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Pixel-style Pill Action Button
            val activePlanId = todayPlan?.id ?: suggestedPlan?.id
            val clickModifier = if (buttonText == "INIZIA" && activePlanId != null) {
                GlanceModifier.clickable(
                    actionRunCallback<StartWorkoutAction>(
                        actionParametersOf(PlanIdKey to activePlanId)
                    )
                )
            } else {
                GlanceModifier.clickable(actionStartActivity(intent))
            }

            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(24.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .then(clickModifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonText,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

val PlanIdKey = ActionParameters.Key<Int>("plan_id")

class StartWorkoutAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val planId = parameters[PlanIdKey] ?: return
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val workoutRepository = entryPoint.workoutRepository()
        
        // Start the session immediately. This triggers triggerWidgetUpdate in the repository
        val sessionId = workoutRepository.startSession(planId, System.currentTimeMillis()).toInt()

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("workout_plan_id", planId)
            putExtra("workout_session_id", sessionId)
        }
        context.startActivity(intent)
    }
}
