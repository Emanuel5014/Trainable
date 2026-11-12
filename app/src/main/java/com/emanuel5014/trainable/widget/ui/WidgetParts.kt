package com.emanuel5014.trainable.widget.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.widget.model.DayMark
import com.emanuel5014.trainable.widget.model.DaySlot
import java.util.Locale
import kotlinx.datetime.toJavaDayOfWeek

/** A widget's surface: the rounded background drawable tinted with [color], so corners are round on every API level. */
@Composable
fun WidgetSurface(
    color: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background), ContentScale.FillBounds, ColorFilter.tint(color))
            .then(modifier),
        contentAlignment = contentAlignment
    ) { content() }
}

/** An Expressive shape ([shape] = one of the widget_shape_* drawables) filled with [fill], with [content] centred on it. */
@Composable
fun ShapeBadge(
    @DrawableRes shape: Int,
    fill: ColorProvider,
    size: Dp,
    modifier: GlanceModifier = GlanceModifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(shape),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(size),
            colorFilter = ColorFilter.tint(fill)
        )
        content()
    }
}

/** A tinted icon (one of the widget_ic_* drawables). */
@Composable
fun WidgetIcon(@DrawableRes icon: Int, color: ColorProvider, size: Dp, modifier: GlanceModifier = GlanceModifier) {
    Image(
        provider = ImageProvider(icon),
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(color)
    )
}

/** The initial letter in a badge. */
@Composable
fun BadgeLetter(letter: String, color: ColorProvider, fontSize: TextUnit) {
    Text(
        text = letter,
        maxLines = 1,
        style = TextStyle(color = color, fontSize = fontSize, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    )
}

enum class PillStyle { FILLED, TONAL, OUTLINED }

/** The full pill button: FILLED primary, TONAL primaryContainer (on a primary surface), OUTLINED primary stroke. */
@Composable
fun PillButton(
    text: String,
    @DrawableRes icon: Int?,
    style: PillStyle,
    action: Action,
    modifier: GlanceModifier = GlanceModifier,
    height: Dp = 32.dp
) {
    val colors = GlanceTheme.colors
    val (background, content) = when (style) {
        PillStyle.FILLED -> R.drawable.widget_pill to colors.onPrimary
        PillStyle.TONAL -> R.drawable.widget_pill to colors.onPrimaryContainer
        PillStyle.OUTLINED -> R.drawable.widget_pill_outline to colors.primary
    }
    val fill = when (style) {
        PillStyle.FILLED, PillStyle.OUTLINED -> colors.primary
        PillStyle.TONAL -> colors.primaryContainer
    }
    Box(
        modifier = modifier
            .height(height)
            .background(ImageProvider(background), ContentScale.FillBounds, ColorFilter.tint(fill))
            .clickable(action)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                WidgetIcon(icon, content, 18.dp)
                Spacer(GlanceModifier.width(6.dp))
            }
            Text(
                text = text,
                maxLines = 1,
                style = TextStyle(color = content, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            )
        }
    }
}

/**
 * The week in the weekly goal: a filled Cookie4 for a day with a workout (with the count when there were 2 or more),
 * an outlined circle for today without one, a soft circle for the other days; the day's narrow letter below.
 */
@Composable
fun DayRow(days: List<DaySlot>, modifier: GlanceModifier = GlanceModifier, dotSize: Dp = 22.dp) {
    val colors = GlanceTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        days.forEach { slot ->
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = GlanceModifier.size(dotSize + 6.dp), contentAlignment = Alignment.Center) {
                    when (slot.mark) {
                        DayMark.TRAINED -> ShapeBadge(R.drawable.widget_shape_cookie4, colors.primary, dotSize)
                        DayMark.TODAY_EMPTY -> WidgetIcon(R.drawable.widget_day_today, colors.primary, dotSize)
                        DayMark.EMPTY -> WidgetIcon(R.drawable.widget_day_empty, colors.primaryContainer, dotSize)
                    }
                    if (slot.sessions >= 2) {
                        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                            Box(
                                modifier = GlanceModifier
                                    .size(14.dp)
                                    .background(ImageProvider(R.drawable.widget_pill), ContentScale.FillBounds, ColorFilter.tint(colors.tertiary)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = slot.sessions.toString(),
                                    style = TextStyle(color = colors.onTertiary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = slot.day.toJavaDayOfWeek().getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault()),
                    style = TextStyle(color = colors.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center)
                )
            }
        }
    }
}
