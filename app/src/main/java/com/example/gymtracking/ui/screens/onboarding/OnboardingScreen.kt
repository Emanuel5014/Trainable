package com.example.gymtracking.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracking.R
import com.example.gymtracking.ui.components.GymButton
import com.example.gymtracking.ui.components.GymInputField
import com.example.gymtracking.ui.theme.OnSurface
import com.example.gymtracking.ui.theme.OnSurfaceVariant
import com.example.gymtracking.ui.theme.Primary
import com.example.gymtracking.ui.theme.Surface
import com.example.gymtracking.ui.theme.SurfaceContainerHigh
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
    var weightUnit by remember { mutableStateOf("kg") }
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
                        weightUnit = weightUnit,
                        onWeightUnitChange = { weightUnit = it },
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
                            viewModel.completeOnboarding(username, weight, goal, weightUnit)
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
    weightUnit: String,
    onWeightUnitChange: (String) -> Unit,
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
        
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            GymInputField(
                value = username,
                onValueChange = onUsernameChange,
                label = "How should we call you?",
                modifier = Modifier.fillMaxWidth()
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current weight (optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GymInputField(
                        value = weightInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() || it == '.' || it == ',' }) {
                                onWeightChange(newValue)
                            }
                        },
                        label = "0.0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainerHigh)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("kg", "lb").forEach { unit ->
                            val isSelected = weightUnit == unit
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Primary else Color.Transparent)
                                    .clickable { onWeightUnitChange(unit) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unit,
                                    color = if (isSelected) Color.White else OnSurfaceVariant,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            GymInputField(
                value = weeklyGoalInput,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        onWeeklyGoalChange(newValue)
                    }
                },
                label = "Weekly workout goal",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Text(
                        "days", 
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "You can always change these later in settings.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}
