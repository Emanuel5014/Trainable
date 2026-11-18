package com.emanuel5014.trainable.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.one_rep_maxes_empty_title
import com.emanuel5014.trainable.ui.components.formatWeight
import com.emanuel5014.trainable.ui.screens.onerepmax.OneRepMaxesEntryContent
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun IosOneRepMaxesEntry(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val entries by remember { IosAppGraph.oneRepMaxes.allWithExercise() }.collectAsState(initial = emptyList())
    val unit by IosAppGraph.preferences.weightUnit.collectAsState(initial = "kg")
    val current = remember(entries) {
        entries.groupBy { it.entry.exerciseId }.values.map { history ->
            history.sortedWith(compareByDescending<com.emanuel5014.trainable.data.local.relation.OneRepMaxWithExercise> { it.entry.date }
                .thenByDescending { it.entry.id }).first()
        }.sortedByDescending { it.entry.date }
    }
    val language = com.emanuel5014.trainable.platform.IosAppLocale.language()
    val subtitle = if (current.isEmpty()) stringResource(Res.string.one_rep_maxes_empty_title)
    else current.take(3).joinToString(" · ") {
        "${ExerciseTranslations.translate(it.exerciseName, language)} ${formatWeight(it.entry.weightKg, unit)}"
    }
    OneRepMaxesEntryContent(subtitle = subtitle, onClick = onClick, modifier = modifier)
}
