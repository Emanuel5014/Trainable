package com.emanuel5014.trainable.widget.model

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class RoutineLayout { COMPACT, LARGE }

enum class GoalLayout { SMALL, WIDE, STRIP }

private val TallEnough = 100.dp
private val WideEnough = 240.dp

/** 3×1 and 4×1 get one row; from about 2 cells tall the full layout. */
fun routineLayoutFor(size: DpSize): RoutineLayout =
    if (size.height >= TallEnough) RoutineLayout.LARGE else RoutineLayout.COMPACT

/** One cell tall: the strip with the days; 2×2: only the ring; 4×2: ring, text and days. */
fun goalLayoutFor(size: DpSize): GoalLayout = when {
    size.height < TallEnough -> GoalLayout.STRIP
    size.width >= WideEnough -> GoalLayout.WIDE
    else -> GoalLayout.SMALL
}

/** The sizes handed to SizeMode.Responsive: Glance renders one layout each and the launcher picks the closest. */
val RoutineSizes: Set<DpSize> = setOf(
    DpSize(180.dp, 56.dp), DpSize(250.dp, 56.dp), DpSize(180.dp, 110.dp), DpSize(250.dp, 110.dp)
)

val GoalSizes: Set<DpSize> = setOf(
    DpSize(110.dp, 110.dp), DpSize(180.dp, 180.dp), DpSize(180.dp, 56.dp), DpSize(250.dp, 56.dp),
    DpSize(250.dp, 110.dp), DpSize(250.dp, 180.dp)
)
