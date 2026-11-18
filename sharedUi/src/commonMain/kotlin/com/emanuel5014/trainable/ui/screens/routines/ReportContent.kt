package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.ui.util.bodyPadding
import com.emanuel5014.trainable.ui.util.scrollEndInset
import com.emanuel5014.trainable.ui.glass.actionGlass
import com.emanuel5014.trainable.ui.glass.actionGlassContainer
import com.emanuel5014.trainable.ui.glass.actionGlassElevation
import com.emanuel5014.trainable.ui.glass.actionGlassSource
import com.emanuel5014.trainable.ui.glass.rememberActionGlassSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.export_successful
import com.emanuel5014.trainable.sharedui.resources.report
import com.emanuel5014.trainable.sharedui.resources.report_preview
import com.emanuel5014.trainable.sharedui.resources.save
import com.emanuel5014.trainable.sharedui.resources.share
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/**
 * The report's preview with Save and Share. Writing the file, sharing it and showing the HTML ([preview]) are the
 * platform's.
 */
@Composable
fun ReportContent(
    state: ReportModel,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    /** Shows the page; [endInset] is the room its end keeps for the bottom area the page runs under (iOS). */
    preview: @Composable (html: String, modifier: Modifier, endInset: androidx.compose.ui.unit.Dp) -> Unit
) {
    val uiState by state.uiState.collectAsState()
    val showMessage = com.emanuel5014.trainable.ui.components.LocalShowMessage.current
    val savedText = stringResource(Res.string.export_successful)

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            showMessage(savedText)
            state.clearSaveSuccess()
        }
    }

    val actionGlass = rememberActionGlassSource()
    Scaffold(
        containerColor = Surface,
        floatingActionButton = {
            if (uiState.html != null) {
                Column(horizontalAlignment = Alignment.End) {
                    ExtendedFloatingActionButton(
                        onClick = onSave,
                        containerColor = actionGlassContainer(actionGlass, SurfaceContainerHigh),
                        contentColor = Primary,
                        shape = Shapes.large,
                        elevation = actionGlassElevation(actionGlass),
                        modifier = Modifier.actionGlass(actionGlass, SurfaceContainerHigh)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = stringResource(Res.string.save))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(modifier = Modifier.width(Spacing.small))

                    ExtendedFloatingActionButton(
                        onClick = onShare,
                        containerColor = actionGlassContainer(actionGlass, Primary),
                        contentColor = OnPrimary,
                        shape = Shapes.large,
                        elevation = actionGlassElevation(actionGlass),
                        modifier = Modifier.actionGlass(actionGlass, Primary)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = stringResource(Res.string.share))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.share).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues.bodyPadding())
        ) {
            // Only the header is recorded for the buttons' glass, never the preview: the glass draws its recording again
            // through a blur, and a WebView drawn into such an off-screen layer flickers and then crashes the app
            // (Chromium's Vulkan draw on the Pixel). Over the page the buttons bend the plain screen background.
            Box(modifier = Modifier.actionGlassSource(actionGlass)) {
                ScreenHeader(
                    title = stringResource(Res.string.report),
                    subtitle = stringResource(Res.string.report_preview).uppercase(),
                    navigationIcon = {
                        GymIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = onNavigateBack,
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = OnSurface,
                            description = "Back"
                        )
                    },
                    titleInRow = true
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GymLoadingIndicator()
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = OnSurfaceVariant
                    )
                }
            } else if (uiState.html != null) {
                preview(uiState.html!!, Modifier.fillMaxSize(), paddingValues.scrollEndInset())
            }
        }
    }
}
