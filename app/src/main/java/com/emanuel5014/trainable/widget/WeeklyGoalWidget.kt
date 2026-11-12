package com.emanuel5014.trainable.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.weekStartMillis
import com.emanuel5014.trainable.ui.screens.dashboard.firstDayOfWeek
import com.emanuel5014.trainable.widget.model.GoalLayout
import com.emanuel5014.trainable.widget.model.GoalSizes
import com.emanuel5014.trainable.widget.model.WeekProgress
import com.emanuel5014.trainable.widget.model.goalLayoutFor
import com.emanuel5014.trainable.widget.model.weekProgress
import com.emanuel5014.trainable.widget.ui.DayRow
import com.emanuel5014.trainable.widget.ui.ShapeBadge
import com.emanuel5014.trainable.widget.ui.WidgetIcon
import com.emanuel5014.trainable.widget.ui.WidgetSurface
import com.emanuel5014.trainable.widget.ui.wavyRingBitmap
import com.emanuel5014.trainable.widget.ui.widgetThemeFlow
import dagger.hilt.android.EntryPointAccessors
import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** The weekly goal: a wavy ring of workouts done and, when there is room, the week's days. */
class WeeklyGoalWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(GoalSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val workoutRepository = entryPoint.workoutRepository()
        val prefs = entryPoint.userPreferencesRepository()
        val themeFlow = widgetThemeFlow(prefs)
        val initialTheme = themeFlow.first()
        val initialGoal = prefs.weeklyGoal.first()

        provideContent {
            val theme by themeFlow.collectAsState(initial = initialTheme)
            val goal by prefs.weeklyGoal.collectAsState(initial = initialGoal)
            val sessions by workoutRepository.getAllSessions().collectAsState(initial = emptyList())

            // Same week as the dashboard: the locale's first day, midnight in the system zone
            val zone = TimeZone.currentSystemDefault()
            val today = Clock.System.todayIn(zone)
            val firstDay = firstDayOfWeek()
            val weekStart = weekStartMillis(today, firstDay, zone)
            val cardio by remember(weekStart) { workoutRepository.getCardioSessionCountSince(weekStart) }
                .collectAsState(initial = 0)

            val week = weekProgress(sessions.map { it.timestamp }, goal, cardio, today, firstDay, zone)
            val colors = remember(theme) { theme.colorProviders(context) }

            GlanceTheme(colors = colors) {
                WeeklyGoalContent(context, week, goalLayoutFor(LocalSize.current))
            }
        }
    }
}

private val RingStroke = 9.dp
private val RingAmplitude = 2.8.dp
private val RingMax = 160.dp

@Composable
private fun WeeklyGoalContent(context: Context, week: WeekProgress, layout: GoalLayout) {
    val colors = GlanceTheme.colors
    val size = LocalSize.current
    val title = if (week.met) context.getString(R.string.widget_goal_met)
    else context.resources.getQuantityString(R.plurals.widget_goal_remaining, week.remaining, week.remaining)

    WidgetSurface(
        color = colors.widgetBackground,
        modifier = GlanceModifier.clickable(actionStartActivity(openAppIntent(context))),
        contentAlignment = Alignment.Center
    ) {
        when (layout) {
            GoalLayout.SMALL -> Ring(context, week, min(size.width, size.height) - 16.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DoneOfGoal(week, bigSp = 26, smallSp = 15)
                    if (week.met) {
                        GoalMetBadge(20.dp)
                    } else {
                        WidgetText(context.getString(R.string.widget_goal_unit), colors.onSurfaceVariant, 11, FontWeight.Normal)
                    }
                }
            }
            GoalLayout.WIDE -> Row(
                modifier = GlanceModifier.fillMaxSize().padding(start = 10.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Ring(context, week, size.height - 24.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DoneOfGoal(week, bigSp = 24, smallSp = 14)
                        if (week.met) GoalMetBadge(18.dp)
                    }
                }
                Spacer(GlanceModifier.width(14.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    WidgetText(context.getString(R.string.widget_goal_title), colors.primary, 11, FontWeight.Medium)
                    WidgetText(title, colors.onSurface, 15, FontWeight.Medium)
                    if (week.cardio > 0) {
                        WidgetText(context.getString(R.string.widget_goal_cardio, week.cardio), colors.onSurfaceVariant, 11, FontWeight.Normal)
                    }
                    Spacer(GlanceModifier.height(6.dp))
                    DayRow(week.days, modifier = GlanceModifier.fillMaxWidth(), dotSize = 20.dp)
                }
            }
            GoalLayout.STRIP -> Column(
                modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DoneOfGoal(week, bigSp = 18, smallSp = 13)
                    Spacer(GlanceModifier.width(8.dp))
                    WidgetText(title, colors.onSurface, 13, FontWeight.Medium)
                }
                DayRow(week.days, modifier = GlanceModifier.fillMaxWidth(), dotSize = 16.dp)
            }
        }
    }
}

/** "3/4": the done count big, the goal smaller and muted. */
@Composable
private fun DoneOfGoal(week: WeekProgress, bigSp: Int, smallSp: Int) {
    val colors = GlanceTheme.colors
    Row(verticalAlignment = Alignment.Bottom) {
        Text(week.done.toString(), style = TextStyle(color = colors.onSurface, fontSize = bigSp.sp, fontWeight = FontWeight.Medium))
        Text("/${week.goal}", style = TextStyle(color = colors.onSurfaceVariant, fontSize = smallSp.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun GoalMetBadge(size: Dp) {
    val colors = GlanceTheme.colors
    ShapeBadge(R.drawable.widget_shape_sunny, colors.primary, size) {
        WidgetIcon(R.drawable.widget_ic_check, colors.onPrimary, size * 0.62f)
    }
}

/**
 * The wavy ring: two white bitmaps (track and arc) tinted with theme colours, so they follow light and dark like the
 * rest of the widget. [content] sits in the middle.
 */
@Composable
private fun Ring(context: Context, week: WeekProgress, side: Dp, content: @Composable () -> Unit) {
    val colors = GlanceTheme.colors
    val ringSide = side.coerceIn(48.dp, RingMax)
    val density = context.resources.displayMetrics.density
    val sizePx = (ringSide.value * density).toInt()
    val stroke = RingStroke.value * density
    val amplitude = RingAmplitude.value * density
    val (track, arc) = remember(sizePx, week.progress) {
        ringLayers(sizePx, week.progress, stroke, amplitude)
    }
    Box(modifier = GlanceModifier.size(ringSide), contentAlignment = Alignment.Center) {
        Image(ImageProvider(track), contentDescription = null, modifier = GlanceModifier.size(ringSide), colorFilter = ColorFilter.tint(colors.primaryContainer))
        if (arc != null) {
            Image(ImageProvider(arc), contentDescription = null, modifier = GlanceModifier.size(ringSide), colorFilter = ColorFilter.tint(colors.primary))
        }
        content()
    }
}

private const val White = 0xFFFFFFFF.toInt()

private fun ringLayers(sizePx: Int, progress: Float, stroke: Float, amplitude: Float): Pair<Bitmap, Bitmap?> {
    val track = wavyRingBitmap(sizePx, 0f, trackArgb = White, arcArgb = White, strokePx = stroke, amplitudePx = amplitude)
    val arc = if (progress > 0f) wavyRingBitmap(sizePx, progress, trackArgb = 0, arcArgb = White, strokePx = stroke, amplitudePx = amplitude) else null
    return track to arc
}
