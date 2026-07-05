package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.ui.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalCheckCompareScreen(
    checkId1: Int,
    checkId2: Int,
    onNavigateBack: () -> Unit,
    viewModel: PhysicalCheckViewModel = hiltViewModel()
) {
    val checks by viewModel.checks.collectAsState()
    
    val check1 = remember(checks, checkId1) { checks.find { it.id == checkId1 } }
    val check2 = remember(checks, checkId2) { checks.find { it.id == checkId2 } }

    if (check1 == null || check2 == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val (olderCheck, newerCheck) = remember(check1, check2) {
        if (check1.timestamp <= check2.timestamp) Pair(check1, check2) else Pair(check2, check1)
    }

    val olderPhotos = remember(olderCheck.fotoFilenames) {
        if (olderCheck.fotoFilenames.isEmpty()) emptyList() else olderCheck.fotoFilenames.split(",")
    }
    val newerPhotos = remember(newerCheck.fotoFilenames) {
        if (newerCheck.fotoFilenames.isEmpty()) emptyList() else newerCheck.fotoFilenames.split(",")
    }

    var selectedOlderPhoto by remember { mutableStateOf(olderPhotos.firstOrNull()) }
    var selectedNewerPhoto by remember { mutableStateOf(newerPhotos.firstOrNull()) }

    var showPhotoSelectors by remember { mutableStateOf(false) }
    val hasMultiplePhotos = olderPhotos.size > 1 || newerPhotos.size > 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confronto Check Fisici", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (showPhotoSelectors) showPhotoSelectors = false
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -100f) {
                                    showPhotoSelectors = true
                                }
                            }
                        )
                    },
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.DarkGray.copy(alpha = 0.3f))
                        ) {
                            if (selectedOlderPhoto != null) {
                                DecryptedImage(
                                    filename = selectedOlderPhoto!!,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 16.dp)
                            ) {
                                Text(
                                    text = "Prima: ${DateFormatter.format(olderCheck.timestamp)}" + 
                                           (olderCheck.peso?.let { " - $it kg" } ?: ""),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp).background(Color.White.copy(alpha = 0.2f)))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.DarkGray.copy(alpha = 0.3f))
                        ) {
                            if (selectedNewerPhoto != null) {
                                DecryptedImage(
                                    filename = selectedNewerPhoto!!,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 16.dp)
                            ) {
                                Text(
                                    text = "Dopo: ${DateFormatter.format(newerCheck.timestamp)}" + 
                                           (newerCheck.peso?.let { " - $it kg" } ?: ""),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (hasMultiplePhotos && !showPhotoSelectors) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(24.dp)
                        .clickable { showPhotoSelectors = true }
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                }
            }

            AnimatedVisibility(
                visible = showPhotoSelectors && hasMultiplePhotos,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(bottom = 16.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount > 100f) {
                                        showPhotoSelectors = false
                                    }
                                }
                            )
                        },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        )
                    }
                    if (olderPhotos.size > 1) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "Seleziona Foto Prima:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(olderPhotos) { photo ->
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedOlderPhoto = photo }
                                            .background(if (selectedOlderPhoto == photo) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .padding(if (selectedOlderPhoto == photo) 3.dp else 0.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    ) {
                                        DecryptedImage(
                                            filename = photo,
                                            viewModel = viewModel,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (newerPhotos.size > 1) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "Seleziona Foto Dopo:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(newerPhotos) { photo ->
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedNewerPhoto = photo }
                                            .background(if (selectedNewerPhoto == photo) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .padding(if (selectedNewerPhoto == photo) 3.dp else 0.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    ) {
                                        DecryptedImage(
                                            filename = photo,
                                            viewModel = viewModel,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
