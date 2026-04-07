package com.example.gymtracking.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracking.ui.components.GymCard
import com.example.gymtracking.ui.components.analytics.BodyCompositionCard
import com.example.gymtracking.ui.components.analytics.ConsistencyCard
import com.example.gymtracking.ui.components.analytics.PersonalBestsSection
import com.example.gymtracking.ui.components.analytics.StrengthIndexCard
import com.example.gymtracking.ui.theme.OnPrimary
import com.example.gymtracking.ui.theme.OnSurface
import com.example.gymtracking.ui.theme.OnSurfaceVariant
import com.example.gymtracking.ui.theme.Primary
import com.example.gymtracking.ui.theme.Spacing
import com.example.gymtracking.ui.theme.Surface
import com.example.gymtracking.ui.theme.SurfaceContainerHigh

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(containerColor = Surface) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Analytics unavailable",
                        color = OnSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(Spacing.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    item {
                        AnalyticsHeader()
                    }

                    item {
                        StrengthIndexCard(strengthIndex = uiState.strengthIndex)
                    }
                    
                    item {
                        PersonalBestsSection(personalBests = uiState.personalBests)
                    }

                    item {
                        BodyCompositionCard(
                            selectedTimeRange = uiState.selectedTimeRange,
                            onTimeRangeSelected = viewModel::selectTimeRange,
                            bodyWeightHistory = uiState.bodyWeightHistory,
                            bodyWeightInput = uiState.bodyWeightInput,
                            onBodyWeightInputChanged = viewModel::onBodyWeightInputChanged,
                            onSubmitWeight = viewModel::submitWeight
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsHeader() {
    GymCard(containerColor = SurfaceContainerHigh) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Insights,
                        contentDescription = null,
                        tint = Primary
                    )
                }
                Column {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.headlineLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ACTIVE PLAN INSIGHTS",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}