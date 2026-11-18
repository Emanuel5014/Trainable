package com.emanuel5014.trainable.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Redraws every Trainable widget on the home screen: after a workout change, a theme change and just after midnight. */
object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun widgets(): List<GlanceAppWidget> = listOf(TrainableWidget(), WeeklyGoalWidget(), QuickStartWidget())

    fun updateAll(context: Context) {
        val app = context.applicationContext
        scope.launch { updateAllNow(app) }
    }

    /** The same, waiting until every widget has been redrawn (for a worker that must not end before). */
    suspend fun updateAllNow(context: Context) {
        widgets().forEach { widget ->
            try {
                widget.updateAll(context)
            } catch (e: Exception) {
                Log.w("WidgetUpdater", "could not update ${widget::class.simpleName}", e)
            }
        }
    }
}
