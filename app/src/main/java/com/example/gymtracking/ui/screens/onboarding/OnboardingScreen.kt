package com.example.gymtracking.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracking.R
import com.example.gymtracking.ui.components.GymButton
import com.example.gymtracking.ui.components.GymInputField
import com.example.gymtracking.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var weeklyGoalInput by remember { mutableStateOf("3") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        // Aesthetic background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.15f),
                            Surface,
                            Surface
                        )
                    )
                )
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (page) {
                    0 -> WelcomeSlide()
                    1 -> FeaturesSlide()
                    2 -> ProfileSetupSlide(
                        username = username,
                        onUsernameChange = { username = it },
                        weightInput = weightInput,
                        onWeightChange = { weightInput = it },
                        weeklyGoalInput = weeklyGoalInput,
                        onWeeklyGoalChange = { weeklyGoalInput = it }
                    )
                }
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Pager Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    val width by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "indicator_width")
                    val color by animateColorAsState(if (isSelected) Primary else OnSurfaceVariant.copy(alpha = 0.3f), label = "indicator_color")
                    
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // Action Button
            val isLastPage = pagerState.currentPage == pagerState.pageCount - 1
            GymButton(
                onClick = {
                    coroutineScope.launch {
                        if (isLastPage) {
                            val weight = weightInput.replace(',', '.').toFloatOrNull() ?: 0f
                            val goal = weeklyGoalInput.toIntOrNull() ?: 3
                            viewModel.completeOnboarding(username, weight, goal)
                            onFinished()
                        } else {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (isLastPage) "FINISH SETUP" else "CONTINUE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isLastPage) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun WelcomeSlide() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aesthetic Icon Container
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(56.dp))
                    .background(Primary.copy(alpha = 0.1f))
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(130.dp),
                tint = Primary
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "TRAINABLE",
            style = MaterialTheme.typography.displayMedium,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your new monolithic companion for strength training. Clean, powerful, and built for performance.",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
    }
}

@Composable
private fun FeaturesSlide() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = "Elevate your\ntraining.",
            style = MaterialTheme.typography.displaySmall,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            lineHeight = 44.sp
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            FeatureItemExpressive(
                icon = Icons.Rounded.Bolt,
                title = "Smart Suggestions",
                desc = "Trainable learns your schedule and suggests the perfect workout next."
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.DragHandle,
                title = "Total Control",
                desc = "Drag, drop, and customize your routines with a tactile interface."
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.Analytics,
                title = "Deep Insights",
                desc = "Visualize your volume and personal bests with editorial charts."
            )
        }
    }
}

@Composable
private fun FeatureItemExpressive(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun ProfileSetupSlide(
    username: String,
    onUsernameChange: (String) -> Unit,
    weightInput: String,
    onWeightChange: (String) -> Unit,
    weeklyGoalInput: String,
    onWeeklyGoalChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Personalize\nyour experience.",
            style = MaterialTheme.typography.displaySmall,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            lineHeight = 44.sp
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            GymInputField(
                value = username,
                onValueChange = onUsernameChange,
                label = "How should we call you?",
                modifier = Modifier.fillMaxWidth()
            )
            
            GymInputField(
                value = weightInput,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() || it == '.' || it == ',' }) {
                        onWeightChange(newValue)
                    }
                },
                label = "Current weight (optional)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            GymInputField(
                value = weeklyGoalInput,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        onWeeklyGoalChange(newValue)
                    }
                },
                label = "Weekly workout goal",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "You can always change these later in settings.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}
