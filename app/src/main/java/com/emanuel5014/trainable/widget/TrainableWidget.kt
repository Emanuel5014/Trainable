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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
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
import com.emanuel5014.trainable.data.repository.WorkoutRepository
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
}

class TrainableWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val workoutRepository = entryPoint.workoutRepository()

        // Use system dynamic colors (Material You) on Android 12+, default Material 3 otherwise
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

        // Fetch workout data
        val plans = workoutRepository.getActivePlans().first()
        val allSessions = workoutRepository.getAllSessions().first()
        val unfinishedSessions = workoutRepository.getUnfinishedSessionsWithPlanName().first()

        // 3. Re-use Suggested Plan Logic identical to DashboardViewModel
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

        // Avoid showing same plan twice
        val finalSuggestedPlan = if (suggestedPlan?.id == todayPlan?.id) null else suggestedPlan

        provideContent {
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
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        fun update(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TrainableWidgetReceiver::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids.isNotEmpty()) {
                widgetScope.launch {
                    try {
                        TrainableWidget().updateAll(context)
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
        unfinishedSession != null -> unfinishedSession.planNome
        todayPlan != null -> todayPlan.nome
        suggestedPlan != null -> suggestedPlan.nome
        else -> null
    }

    val stateLabel = when {
        isResume -> "IN CORSO"
        todayPlan != null -> "CONSIGLIATO OGGI"
        suggestedPlan != null -> "CONSIGLIATO"
        else -> "BENVENUTO"
    }

    val buttonText = when {
        isResume -> "RIPRENDI"
        todayPlan != null || suggestedPlan != null -> "INIZIA"
        else -> "APRI APP"
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
            .cornerRadius(28.dp)
            .padding(16.dp),
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
                    modifier = GlanceModifier.padding(bottom = 6.dp)
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_app_logo),
                        contentDescription = "Trainable Logo",
                        modifier = GlanceModifier.size(16.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "TRAINABLE",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Status Label
                Text(
                    text = stateLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                // Plan/Routine Name
                Text(
                    text = displayPlanName ?: "Inizia una routine",
                    maxLines = 2,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Pixel-style Pill Action Button
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(20.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clickable(actionStartActivity(intent)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonText,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
