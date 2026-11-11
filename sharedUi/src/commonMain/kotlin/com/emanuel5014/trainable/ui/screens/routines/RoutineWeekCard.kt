package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.routines_week_rest
import com.emanuel5014.trainable.sharedui.resources.routines_week_title
import com.emanuel5014.trainable.sharedui.resources.routines_week_today
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.util.narrowDayName
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource

/**
 * "Your week" as the head of the routines group: seven boxes with the weekday and the number of the routine planned
 * on it (its place in the list), today highlighted. [collapsed] folds it into one line that still says what is on
 * today. Tapping a day opens its routine, or a small menu when there are more.
 */
@Composable
internal fun RoutineWeekCard(
    plans: List<PlanWithDetails>,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onOpenPlan: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val week = remember(plans) { routineWeek(plans) }
    val today = todayDayIndex()
    val numberOf: (PlanWithDetails) -> Int = { p -> plans.indexOfFirst { it.plan.id == p.plan.id } + 1 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(routineWeekCorners().toShape())
            .background(SurfaceContainerHigh)
            .animateContentSize()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (collapsed) {
                    stringResource(Res.string.routines_week_today, week[today].firstOrNull()?.plan?.nome ?: stringResource(Res.string.routines_week_rest))
                } else {
                    stringResource(Res.string.routines_week_title)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (collapsed) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = OnSurfaceVariant
            )
        }

        if (!collapsed) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DayOfWeek.entries.forEachIndexed { index, day ->
                    WeekDay(
                        label = narrowDayName(day),
                        plans = week[index],
                        numbers = week[index].map(numberOf),
                        isToday = index == today,
                        onOpenPlan = onOpenPlan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekDay(
    label: String,
    plans: List<PlanWithDetails>,
    numbers: List<Int>,
    isToday: Boolean,
    onOpenPlan: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val container = if (isToday) scheme.primaryContainer else Surface
    val labelColor = if (isToday) scheme.onPrimaryContainer else OnSurfaceVariant
    val numberColor = when {
        isToday -> scheme.onPrimaryContainer
        numbers.isNotEmpty() -> OnSurface
        else -> OnSurfaceVariant.copy(alpha = 0.4f)
    }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(container)
                .then(
                    if (plans.isEmpty()) Modifier else Modifier.clickable {
                        if (plans.size == 1) onOpenPlan(plans.first().plan.id) else menuOpen = true
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = labelColor)
            Text(
                // One routine: its number; two: "1·2"; more: the first and "+"
                text = when {
                    numbers.isEmpty() -> "·"
                    numbers.size <= 2 -> numbers.joinToString("·")
                    else -> "${numbers.first()}+"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = numberColor,
                maxLines = 1
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            plans.forEachIndexed { i, p ->
                DropdownMenuItem(
                    text = { Text("${numbers[i]}  ${p.plan.nome}") },
                    onClick = {
                        menuOpen = false
                        onOpenPlan(p.plan.id)
                    }
                )
            }
        }
    }
}
