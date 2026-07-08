package com.emanuel5014.trainable.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
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
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
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
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.Calendar
import kotlin.math.ceil

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WeeklyGoalEntryPoint {
    fun workoutRepository(): WorkoutRepository
    fun userPreferencesRepository(): UserPreferencesRepository
}

class WeeklyGoalWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WeeklyGoalEntryPoint::class.java)
        val workoutRepository = entryPoint.workoutRepository()
        val prefs = entryPoint.userPreferencesRepository()

        provideContent {
            val weeklyGoal by prefs.weeklyGoal.collectAsState(initial = 3)
            val allSessions by workoutRepository.getAllSessions().collectAsState(initial = emptyList())
            val weekStartMillis = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
            val numWorkoutsThisWeek = allSessions.filter { it.timestamp >= weekStartMillis }.size
            val cardioCount by workoutRepository.getCardioSessionCountSince(weekStartMillis)
                .collectAsState(initial = 0)

            val progress = if (weeklyGoal > 0)
                (numWorkoutsThisWeek.toFloat() / weeklyGoal.toFloat()).coerceIn(0f, 1f)
            else 0f
            val goalMet = progress >= 1f

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

            val isDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) darkColorScheme() else lightColorScheme()
            }

            val wavyColorArgb = colorScheme.primary.copy(alpha = 0.15f).toArgb()
            val primaryColorCheckArgb = colorScheme.primary.toArgb()
            val density = context.resources.displayMetrics.density

            val wavyBitmap = remember(progress, isDark) {
                createWavyBitmap(
                    width = (360 * density).toInt(),
                    height = (60 * density).toInt(),
                    colorArgb = wavyColorArgb,
                    percent = progress,
                    periodPx = 20f * density,
                    amplitudePx = 4f * density
                )
            }

            val checkBitmap = remember(goalMet, isDark) {
                if (goalMet) createCheckCircleBitmap(
                    size = (28 * density).toInt(),
                    circleColorArgb = primaryColorCheckArgb
                ) else null
            }

            GlanceTheme(colors = colors) {
                WeeklyGoalContent(
                    context = context,
                    wavyBitmap = wavyBitmap,
                    checkBitmap = checkBitmap,
                    workoutsThisWeek = numWorkoutsThisWeek,
                    weeklyGoal = weeklyGoal,
                    cardioWorkoutsThisWeek = cardioCount,
                    goalMet = goalMet
                )
            }
        }
    }

    companion object {
        fun update(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, WeeklyGoalWidgetReceiver::class.java)
                val ids = appWidgetManager.getAppWidgetIds(componentName)
                if (ids.isNotEmpty()) {
                    val updateIntent = Intent(context, WeeklyGoalWidgetReceiver::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(updateIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
private fun WeeklyGoalContent(
    context: Context,
    wavyBitmap: Bitmap,
    checkBitmap: Bitmap?,
    workoutsThisWeek: Int,
    weeklyGoal: Int,
    cardioWorkoutsThisWeek: Int,
    goalMet: Boolean
) {
    val remaining = weeklyGoal - workoutsThisWeek
    val progressColor = GlanceTheme.colors.primary

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(24.dp)
            .clickable(
                actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(wavyBitmap),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = context.getString(R.string.weekly_goal),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(2.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$workoutsThisWeek",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = " / $weeklyGoal",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                if (goalMet && checkBitmap != null) {
                    Image(
                        provider = ImageProvider(checkBitmap),
                        contentDescription = context.getString(R.string.goal_met),
                        modifier = GlanceModifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(2.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val remainingText = when {
                    goalMet -> context.getString(R.string.goal_met)
                    remaining == 1 -> context.getString(R.string.workout_remaining, remaining)
                    else -> context.getString(R.string.workouts_remaining, remaining)
                }
                Text(
                    text = remainingText,
                    style = TextStyle(
                        color = progressColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (cardioWorkoutsThisWeek > 0) {
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "\u2022 $cardioWorkoutsThisWeek CARDIO",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

private fun createWavyBitmap(
    width: Int,
    height: Int,
    colorArgb: Int,
    percent: Float,
    periodPx: Float,
    amplitudePx: Float
): Bitmap {
    if (width <= 0 || height <= 0) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val clampedPercent = percent.coerceIn(0f, 1f)
    if (clampedPercent <= 0f) return bitmap

    val edgeX = width * clampedPercent
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = colorArgb
        style = Paint.Style.FILL
    }

    if (clampedPercent >= 1f || edgeX >= width) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    val halfPeriod = periodPx / 2
    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(edgeX, 0f)

        val wavesNeeded = ceil(height.toFloat() / halfPeriod + 2f).toInt()
        for (i in 0 until wavesNeeded) {
            val baseY = i * halfPeriod
            if (baseY > height + halfPeriod) break
            if (baseY < -halfPeriod) continue

            val direction = if (i % 2 == 0) 1 else -1
            val waveX = edgeX + amplitudePx * direction
            val startY = baseY.coerceAtLeast(0f)
            val endY = (baseY + halfPeriod).coerceAtMost(height.toFloat())

            if (startY < height && endY > startY) {
                val midY = (startY + endY) / 2f
                quadTo(waveX, midY, edgeX, endY)
            }
        }

        lineTo(0f, height.toFloat())
        close()
    }

    canvas.drawPath(path, paint)
    return bitmap
}

private fun createCheckCircleBitmap(size: Int, circleColorArgb: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val padding = size * 0.08f

    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = circleColorArgb
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - padding, circlePaint)

    val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.TRANSPARENT
        style = Paint.Style.STROKE
        strokeWidth = size * 0.12f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
    }

    val checkPath = Path().apply {
        val s = size.toFloat()
        moveTo(s * 0.32f, s * 0.52f)
        lineTo(s * 0.46f, s * 0.66f)
        lineTo(s * 0.72f, s * 0.32f)
    }
    canvas.drawPath(checkPath, checkPaint)

    return bitmap
}
