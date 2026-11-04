package com.emanuel5014.trainable.ui.screens.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.screens.onerepmax.OneRepMaxesEntryCard
import kotlinx.coroutines.flow.map

/** The shared analytics tab with Android's lifetime, haptics preference and 1RM entry. */
@Composable
fun AnalyticsScreen(
    onNavigateToOneRepMaxes: () -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)
    AnalyticsContent(
        state = viewModel.state,
        hapticEnabled = hapticEnabled,
        oneRepMaxesEntry = { modifier -> OneRepMaxesEntryCard(onClick = onNavigateToOneRepMaxes, modifier = modifier) }
    )
}
