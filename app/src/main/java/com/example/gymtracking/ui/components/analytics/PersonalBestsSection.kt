package com.example.gymtracking.ui.components.analytics

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.gymtracking.ui.components.GymCard
import com.example.gymtracking.ui.screens.analytics.PersonalBestUiModel
import com.example.gymtracking.ui.theme.OnSurface
import com.example.gymtracking.ui.theme.OnSurfaceVariant
import com.example.gymtracking.ui.theme.Primary
import com.example.gymtracking.ui.theme.Spacing
import com.example.gymtracking.ui.theme.SurfaceContainer
import kotlin.math.absoluteValue

@Composable
fun PersonalBestsSection(personalBests: List<PersonalBestUiModel>) {
    var isShowingAll by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PERSONAL BESTS",
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isShowingAll) "SHOW CAROUSEL" else "VIEW ALL",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.clickable { isShowingAll = !isShowingAll }
            )
        }

        if (personalBests.isEmpty()) {
            GymCard(containerColor = SurfaceContainer) {
                Text(
                    text = "No PR data yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
            return
        }

        AnimatedContent(targetState = isShowingAll, label = "PersonalBestsToggle") { showAll ->
            if (showAll) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                    personalBests.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                        ) {
                            rowItems.forEach { best ->
                                PersonalBestCard(
                                    best = best,
                                    isLarge = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { personalBests.size })
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        pageSpacing = Spacing.medium,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                    ) { page ->
                        PersonalBestCard(
                            best = personalBests[page],
                            isLarge = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    val pageOffset = (
                                            (pagerState.currentPage - page) + pagerState
                                                .currentPageOffsetFraction
                                            ).absoluteValue
                                    alpha = lerp(
                                        start = 0.5f,
                                        stop = 1f,
                                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                    )
                                    scaleY = lerp(
                                        start = 0.9f,
                                        stop = 1f,
                                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                    )
                                }
                        )
                    }

                    // Pager Indicator
                    Row(
                        modifier = Modifier.height(Spacing.small),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(personalBests.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) Primary else OnSurfaceVariant.copy(alpha = 0.2f)
                            Box(
                                modifier = Modifier
                                    .size(if (pagerState.currentPage == iteration) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalBestCard(
    best: PersonalBestUiModel,
    isLarge: Boolean,
    modifier: Modifier = Modifier
) {
    GymCard(
        modifier = modifier,
        containerColor = SurfaceContainer
    ) {
        Column(
            horizontalAlignment = if (isLarge) Alignment.CenterHorizontally else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(if (isLarge) Spacing.medium else Spacing.small),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(if (isLarge) 64.dp else 44.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(if (isLarge) 32.dp else 24.dp)
                )
            }

            Column(
                horizontalAlignment = if (isLarge) Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)
            ) {
                Text(
                    text = best.exerciseName,
                    style = if (isLarge) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (isLarge) androidx.compose.ui.text.style.TextAlign.Center else null
                )
                Text(
                    text = best.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(java.util.Locale.getDefault(), "%.1f", best.maxWeightKg),
                        style = if (isLarge) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displaySmall,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}