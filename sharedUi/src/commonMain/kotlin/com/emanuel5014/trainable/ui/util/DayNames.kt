package com.emanuel5014.trainable.ui.util

import kotlinx.datetime.DayOfWeek

/** The one-letter name of [day] in the app's language ("L" for Monday in Italian). */
expect fun narrowDayName(day: DayOfWeek): String
