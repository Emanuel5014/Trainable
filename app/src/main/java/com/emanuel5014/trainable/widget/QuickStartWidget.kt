package com.emanuel5014.trainable.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.widget.model.RoutineWidgetState
import com.emanuel5014.trainable.widget.ui.ShapeBadge
import com.emanuel5014.trainable.widget.ui.WidgetIcon
import com.emanuel5014.trainable.widget.ui.widgetThemeFlow
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/** 1×1: only the Cookie9 button, doing what the Routine widget's button does (start, resume or open the app). */
class QuickStartWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val workoutRepository = entryPoint.workoutRepository()
        val themeFlow = widgetThemeFlow(entryPoint.userPreferencesRepository())
        val initialTheme = themeFlow.first()

        provideContent {
            val theme by themeFlow.collectAsState(initial = initialTheme)
            val state = collectRoutineWidgetState(workoutRepository, withExerciseCount = false)
            val colors = remember(theme) { theme.colorProviders(context) }
            GlanceTheme(colors = colors) {
                QuickStartContent(context, state)
            }
        }
    }
}

@Composable
private fun QuickStartContent(context: Context, state: RoutineWidgetState) {
    val colors = GlanceTheme.colors
    val side = min(LocalSize.current.width, LocalSize.current.height).coerceAtLeast(40.dp)
    val description = when (state) {
        is RoutineWidgetState.Suggested -> context.getString(R.string.widget_cd_start, state.name)
        is RoutineWidgetState.InProgress -> context.getString(R.string.widget_cd_resume, state.name)
        RoutineWidgetState.NoRoutine -> context.getString(R.string.widget_cd_open)
    }
    val (fill, icon, iconColor) = when (state) {
        is RoutineWidgetState.Suggested -> Triple(colors.primary, R.drawable.widget_ic_play, colors.onPrimary)
        is RoutineWidgetState.InProgress -> Triple(colors.primaryContainer, R.drawable.widget_ic_fitness, colors.onPrimaryContainer)
        RoutineWidgetState.NoRoutine -> Triple(colors.primary, R.drawable.widget_ic_add, colors.onPrimary)
    }
    Box(
        modifier = GlanceModifier.fillMaxSize().clickable(mainAction(context, state)),
        contentAlignment = Alignment.Center
    ) {
        ShapeBadge(R.drawable.widget_shape_cookie9, fill, side, contentDescription = description) {
            WidgetIcon(icon, iconColor, side * 0.42f)
        }
    }
}
