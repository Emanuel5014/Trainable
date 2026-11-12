package com.emanuel5014.trainable.widget

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.widget.model.RoutineWidgetState

/** Opens the app on the dashboard. */
fun openAppIntent(context: Context): Intent = Intent(context, MainActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
}

/**
 * The one thing the Routine widget's button and the Quick start widget do: start the suggested routine, resume the
 * open workout, or open the app. Decided on the state, never on the button's text.
 */
fun mainAction(context: Context, state: RoutineWidgetState): Action = when (state) {
    is RoutineWidgetState.Suggested -> actionRunCallback<StartWorkoutAction>(actionParametersOf(PlanIdKey to state.planId))
    is RoutineWidgetState.InProgress -> actionStartActivity(openAppIntent(context).apply {
        putExtra("workout_session_id", state.sessionId)
        putExtra("workout_plan_id", state.planId)
    })
    RoutineWidgetState.NoRoutine -> actionStartActivity(openAppIntent(context))
}
