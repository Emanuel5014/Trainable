package com.emanuel5014.trainable.ui.screens.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.sharedui.resources.Res
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant
import com.emanuel5014.trainable.sharedui.resources.expired
import com.emanuel5014.trainable.sharedui.resources.expires_in_days
import com.emanuel5014.trainable.sharedui.resources.goal_met
import com.emanuel5014.trainable.sharedui.resources.gym_membership
import com.emanuel5014.trainable.sharedui.resources.tap_to_set
import com.emanuel5014.trainable.sharedui.resources.valid_thru
import com.emanuel5014.trainable.sharedui.resources.weekly_goal
import com.emanuel5014.trainable.sharedui.resources.workout_remaining
import com.emanuel5014.trainable.sharedui.resources.workouts_remaining

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GymMembershipCard(
    expiryDateMillis: Long?,
    username: String,
    onClick: () -> Unit,
    isActive: Boolean = true
) {
    val gemPath = MaterialShapes.Gem.toPath()
    val sunnyPath = MaterialShapes.Sunny.toPath()

    val gemRotation = remember { Animatable(0f) }
    val sunnyRotation = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            gemRotation.snapTo(0f)
            while (true) {
                gemRotation.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(15000, easing = LinearEasing)
                )
                gemRotation.snapTo(0f)
            }
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            sunnyRotation.snapTo(0f)
            while (true) {
                sunnyRotation.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(20000, easing = LinearEasing)
                )
                sunnyRotation.snapTo(0f)
            }
        }
    }

    val daysLeft = expiryDateMillis?.let {
        val zone = TimeZone.currentSystemDefault()
        val expiryDate = Instant.fromEpochMilliseconds(it).toLocalDateTime(zone).date
        Clock.System.todayIn(zone).daysUntil(expiryDate)
    }

    val isExpired = daysLeft != null && daysLeft < 0
    val isExpiringSoon = daysLeft != null && daysLeft in 0..7
    
    val isDark = with(MaterialTheme.colorScheme.surface) { red + green + blue < 1.5f }

    val cardGradient = if (isDark) {
        when {
            isExpired -> listOf(Error.copy(alpha = 0.8f), Error.copy(alpha = 0.5f))
            isExpiringSoon -> listOf(Tertiary.copy(alpha = 0.9f), Tertiary.copy(alpha = 0.6f))
            else -> listOf(Primary.copy(alpha = 0.9f), Primary.copy(alpha = 0.6f))
        }
    } else {
        when {
            isExpired -> listOf(Error.copy(alpha = 0.25f), Error.copy(alpha = 0.12f))
            isExpiringSoon -> listOf(Tertiary.copy(alpha = 0.25f), Tertiary.copy(alpha = 0.12f))
            else -> listOf(Primary.copy(alpha = 0.22f), Primary.copy(alpha = 0.1f))
        }
    }
    
    val textColor = Color(0xFF1A1A1A)
    
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                onClick = onClick,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = ripple(color = OnPrimary)
            ),
        shape = Shapes.large,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 2.dp else 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(cardGradient))
        ) {
            // Background expressive pattern
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gemScale = size.width * 0.6f
                val scaledGem = Path().apply {
                    addPath(gemPath)
                    transform(Matrix().apply { scale(gemScale, gemScale) })
                    val c = getBounds().center
                    transform(Matrix().apply {
                        translate(c.x, c.y)
                        rotateZ(gemRotation.value)
                        translate(-c.x, -c.y)
                    })
                    translate(Offset(size.width - c.x, -c.y))
                }
                drawPath(scaledGem, color = Color.White.copy(alpha = 0.1f))

                val sunnyScale = size.width * 0.5f
                val scaledSunny = Path().apply {
                    addPath(sunnyPath)
                    transform(Matrix().apply { scale(sunnyScale, sunnyScale) })
                    val c = getBounds().center
                    transform(Matrix().apply {
                        translate(c.x, c.y)
                        rotateZ(sunnyRotation.value)
                        translate(-c.x, -c.y)
                    })
                    val fc = getBounds().center
                    translate(Offset(size.width * 0.06f - fc.x, size.height * 0.88f - fc.y))
                }
                drawPath(scaledSunny, color = Color.Black.copy(alpha = 0.05f))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CreditCard,
                            contentDescription = "Gym Membership",
                            tint = textColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.gym_membership).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor.copy(alpha = 0.9f),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                    }
                    
                    if (isExpired) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Expired",
                            tint = textColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = username.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (expiryDateMillis != null) {
                            Text(
                                text = "${stringResource(Res.string.valid_thru)} ${DateFormatter.format(expiryDateMillis)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.tap_to_set),
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    if (daysLeft != null) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = when {
                                    isExpired -> stringResource(Res.string.expired)
                                    isExpiringSoon -> stringResource(Res.string.expires_in_days, daysLeft)
                                    else -> stringResource(Res.string.expires_in_days, daysLeft)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun DrawScope.drawWavyPattern(
    color: Color,
    percent: Float,
    shift: Float,
    periodPx: Float = 60f,
    amplitudePx: Float = 8f,
) {
    if (size.height <= 0f || size.width <= 0f) return

    val clampedPercent = percent.coerceIn(0f, 1f)
    val edgeX = size.width * clampedPercent
    val height = size.height

    if (clampedPercent <= 0f) return

    if (clampedPercent >= 1f || edgeX >= size.width) {
        drawRect(color = color, size = size)
        return
    }

    val halfPeriod = periodPx / 2

    val wavyPath = Path().apply {
        moveTo(x = 0f, y = 0f)
        lineTo(x = edgeX, y = 0f)

        val phaseOffset = shift * halfPeriod
        val wavesNeeded = kotlin.math.ceil(height / halfPeriod + 2).toInt()

        for (i in 0 until wavesNeeded) {
            val baseY = i * halfPeriod - phaseOffset
            if (baseY > height + halfPeriod) break
            if (baseY < -halfPeriod) continue

            val direction = if (i % 2 == 0) 1 else -1
            val waveX = edgeX + amplitudePx * direction

            val startY = baseY.coerceAtLeast(0f)
            val endY = (baseY + halfPeriod).coerceAtMost(height)

            if (startY < height && endY > startY) {
                val midY = (startY + endY) / 2
                quadraticTo(
                    x1 = waveX,
                    y1 = midY,
                    x2 = edgeX,
                    y2 = endY
                )
            }
        }

        lineTo(x = 0f, y = height)
        close()
    }

    drawPath(path = wavyPath, color = color)
}

@Composable
internal fun WeeklyGoalCard(workoutsThisWeek: Int, weeklyGoal: Int, cardioWorkoutsThisWeek: Int, isActive: Boolean = true) {
    val progress = if (weeklyGoal > 0) (workoutsThisWeek.toFloat() / weeklyGoal.toFloat()).coerceIn(0f, 1f) else 0f
    val density = LocalDensity.current
    val periodPx = remember { with(density) { 42.dp.toPx() } }
    val amplitudePx = remember { with(density) { 6.dp.toPx() } }
    val remaining = weeklyGoal - workoutsThisWeek
    val goalMet = progress >= 1f
    val primaryColor = Primary
    val tertiaryColor = Tertiary
    val progressColor = if (goalMet) tertiaryColor else primaryColor

    val shift = remember { Animatable(2f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            shift.snapTo(2f)
            while (true) {
                shift.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(3000, easing = LinearEasing)
                )
                shift.snapTo(2f)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = SurfaceContainerHigh,
            contentColor = OnSurface,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawWavyPattern(
                        color = primaryColor.copy(alpha = 0.15f),
                        percent = progress,
                        shift = shift.value,
                        periodPx = periodPx,
                        amplitudePx = amplitudePx,
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 20.dp)
            ) {
                Text(
                    text = stringResource(Res.string.weekly_goal),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$workoutsThisWeek",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "/ $weeklyGoal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (goalMet) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(Res.string.goal_met),
                            tint = tertiaryColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            goalMet -> stringResource(Res.string.goal_met)
                            remaining == 1 -> stringResource(Res.string.workout_remaining, remaining)
                            else -> stringResource(Res.string.workouts_remaining, remaining)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = progressColor,
                        fontWeight = FontWeight.Bold,
                    )
                    if (cardioWorkoutsThisWeek > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "\u2022 $cardioWorkoutsThisWeek CARDIO",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
