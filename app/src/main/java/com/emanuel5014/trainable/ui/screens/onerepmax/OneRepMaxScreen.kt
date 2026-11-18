package com.emanuel5014.trainable.ui.screens.onerepmax

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.ui.components.formatWeight

@Composable
fun OneRepMaxScreen(onNavigateBack: () -> Unit, viewModel: OneRepMaxViewModel = hiltViewModel()) =
    OneRepMaxContent(onNavigateBack, viewModel.state)

/** Compact entry point shown on the analytics tab: count of tracked lifts and the latest 1RMs. */
@Composable
fun OneRepMaxesEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OneRepMaxViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState()
    OneRepMaxesEntryContent(
        subtitle = if (state.rows.isEmpty()) stringResource(R.string.one_rep_maxes_empty_title)
                    else state.rows.take(3).joinToString(" · ") {
                        "${ExerciseTranslations.translate(it.exerciseName, languageCode)} ${formatWeight(it.current.weightKg, state.weightUnit)}"
                    },
        onClick = onClick,
        modifier = modifier
    )
}
