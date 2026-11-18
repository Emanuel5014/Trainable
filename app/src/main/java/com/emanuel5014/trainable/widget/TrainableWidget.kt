package com.emanuel5014.trainable.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.widget.model.RoutineLayout
import com.emanuel5014.trainable.widget.model.RoutineSizes
import com.emanuel5014.trainable.widget.model.RoutineWidgetState
import com.emanuel5014.trainable.widget.model.initial
import com.emanuel5014.trainable.widget.model.routineLayoutFor
import com.emanuel5014.trainable.widget.ui.BadgeLetter
import com.emanuel5014.trainable.widget.ui.PillButton
import com.emanuel5014.trainable.widget.ui.PillStyle
import com.emanuel5014.trainable.widget.ui.ShapeBadge
import com.emanuel5014.trainable.widget.ui.WidgetIcon
import com.emanuel5014.trainable.widget.ui.WidgetSurface
import com.emanuel5014.trainable.widget.ui.widgetThemeFlow
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.Date
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun workoutRepository(): WorkoutRepository
    fun userPreferencesRepository(): UserPreferencesRepository
}

/** The Routine widget: what to do now (resume, today's or the next routine) with one button that does it. */
class TrainableWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(RoutineSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val workoutRepository = entryPoint.workoutRepository()
        val themeFlow = widgetThemeFlow(entryPoint.userPreferencesRepository())
        val initialTheme = themeFlow.first()

        provideContent {
            val theme by themeFlow.collectAsState(initial = initialTheme)
            val state = collectRoutineWidgetState(workoutRepository, withExerciseCount = true)
            val colors = remember(theme) { theme.colorProviders(context) }

            GlanceTheme(colors = colors) {
                RoutineWidgetContent(context, state, routineLayoutFor(LocalSize.current))
            }
        }
    }
}

private data class RoutineTexts(val label: String, val title: String, val subtitle: String?, val button: String)

private fun RoutineWidgetState.texts(context: Context): RoutineTexts = when (this) {
    is RoutineWidgetState.InProgress -> RoutineTexts(
        label = context.getString(R.string.widget_label_in_progress),
        title = name,
        subtitle = context.getString(R.string.widget_started_at, DateFormat.getTimeFormat(context).format(Date(startedAt))),
        button = context.getString(R.string.widget_action_resume)
    )
    is RoutineWidgetState.Suggested -> RoutineTexts(
        label = context.getString(if (today) R.string.widget_label_suggested_today else R.string.widget_label_up_next),
        title = name,
        subtitle = exerciseCount?.takeIf { it > 0 }?.let {
            context.resources.getQuantityString(R.plurals.widget_exercise_count, it, it)
        },
        button = context.getString(R.string.widget_action_start)
    )
    RoutineWidgetState.NoRoutine -> RoutineTexts(
        label = context.getString(R.string.app_name),
        title = context.getString(R.string.widget_create_routine),
        subtitle = null,
        button = context.getString(R.string.widget_action_open_app)
    )
}

@Composable
private fun RoutineWidgetContent(context: Context, state: RoutineWidgetState, layout: RoutineLayout) {
    val colors = GlanceTheme.colors
    val inProgress = state is RoutineWidgetState.InProgress
    val texts = state.texts(context)
    val action = mainAction(context, state)
    // The whole widget opens the app; for an open workout or no routine that is the main action itself
    val widgetClick = if (state is RoutineWidgetState.Suggested) actionStartActivity(openAppIntent(context)) else action

    val surface = if (inProgress) colors.primary else colors.widgetBackground
    val labelColor = if (inProgress) colors.primaryContainer else colors.primary
    val titleColor = if (inProgress) colors.onPrimary else colors.onSurface
    val subtitleColor = if (inProgress) colors.primaryContainer else colors.onSurfaceVariant
    val pillStyle = when (state) {
        is RoutineWidgetState.InProgress -> PillStyle.TONAL
        is RoutineWidgetState.Suggested -> PillStyle.FILLED
        RoutineWidgetState.NoRoutine -> PillStyle.OUTLINED
    }
    val pillIcon = if (state is RoutineWidgetState.NoRoutine) null else R.drawable.widget_ic_play

    WidgetSurface(color = surface, modifier = GlanceModifier.clickable(widgetClick)) {
        when (layout) {
            RoutineLayout.COMPACT -> Row(
                modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoutineBadge(state, badgeSize = 40)
                Spacer(GlanceModifier.width(10.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    WidgetText(texts.label, labelColor, 11, FontWeight.Medium)
                    WidgetText(texts.title, titleColor, 15, FontWeight.Medium)
                }
                Spacer(GlanceModifier.width(8.dp))
                PillButton(texts.button, pillIcon, pillStyle, action)
            }
            RoutineLayout.LARGE -> Column(
                modifier = GlanceModifier.fillMaxSize().padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RoutineBadge(state, badgeSize = 54)
                    Spacer(GlanceModifier.width(12.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        WidgetText(texts.label, labelColor, 11, FontWeight.Medium)
                        WidgetText(texts.title, titleColor, 18, FontWeight.Medium)
                        texts.subtitle?.let { WidgetText(it, subtitleColor, 11, FontWeight.Normal) }
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.height(8.dp))
                PillButton(texts.button, pillIcon, pillStyle, action, modifier = GlanceModifier.fillMaxWidth(), height = 34.dp)
            }
        }
    }
}

/** Cookie9 with the routine's initial; for an open workout the shape turns light with the dumbbell, for none a plus. */
@Composable
private fun RoutineBadge(state: RoutineWidgetState, badgeSize: Int) {
    val colors = GlanceTheme.colors
    val size = badgeSize.dp
    when (state) {
        is RoutineWidgetState.Suggested -> ShapeBadge(R.drawable.widget_shape_cookie9, colors.primary, size) {
            BadgeLetter(state.initial, colors.onPrimary, (badgeSize * 0.42f).sp)
        }
        is RoutineWidgetState.InProgress -> ShapeBadge(R.drawable.widget_shape_cookie9, colors.primaryContainer, size) {
            WidgetIcon(R.drawable.widget_ic_fitness, colors.onPrimaryContainer, (badgeSize * 0.46f).dp)
        }
        RoutineWidgetState.NoRoutine -> ShapeBadge(R.drawable.widget_shape_cookie9, colors.primaryContainer, size) {
            WidgetIcon(R.drawable.widget_ic_add, colors.onPrimaryContainer, (badgeSize * 0.46f).dp)
        }
    }
}

@Composable
internal fun WidgetText(text: String, color: ColorProvider, sizeSp: Int, weight: FontWeight) {
    Text(text = text, maxLines = 1, style = TextStyle(color = color, fontSize = sizeSp.sp, fontWeight = weight))
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
