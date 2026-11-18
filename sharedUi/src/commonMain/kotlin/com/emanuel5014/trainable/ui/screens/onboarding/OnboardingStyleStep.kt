package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The name of a style, as shown on its card and in the Tweaks subtitle. */
fun OnboardingStyle.nameRes(): StringResource = when (this) {
    OnboardingStyle.Simple -> Res.string.onboarding_style_simple
    OnboardingStyle.Hypertrophy -> Res.string.onboarding_style_hypertrophy
    OnboardingStyle.Strength -> Res.string.onboarding_style_strength
}

private fun OnboardingStyle.descRes(): StringResource = when (this) {
    OnboardingStyle.Simple -> Res.string.onboarding_style_simple_desc
    OnboardingStyle.Hypertrophy -> Res.string.onboarding_style_hypertrophy_desc
    OnboardingStyle.Strength -> Res.string.onboarding_style_strength_desc
}

private fun OnboardingStyle.icon(): ImageVector = when (this) {
    OnboardingStyle.Simple -> Icons.Rounded.FitnessCenter
    OnboardingStyle.Hypertrophy -> Icons.Rounded.LocalFireDepartment
    OnboardingStyle.Strength -> Icons.Rounded.EmojiEvents
}

/** "Come ti alleni?": three style cards; picking one applies its features, after a warning if Tweaks were edited. */
@Composable
fun StyleStep(choices: OnboardingChoices, hero: @Composable () -> Unit) {
    val current by choices.style
    var pending by remember { mutableStateOf<OnboardingStyle?>(null) }

    OnboardingPage(underTopRow = true) {
        hero()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OnboardingTitle(stringResource(Res.string.onboarding_style_title))
            Text(
                stringResource(Res.string.onboarding_style_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OnboardingStyle.entries.forEach { style ->
                StyleCard(style, selected = style == current) {
                    when {
                        style == current -> Unit
                        current.isTweaked(choices) -> pending = style
                        else -> style.applyTo(choices)
                    }
                }
            }
        }
    }

    pending?.let { target ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(Res.string.onboarding_style_change_title)) },
            text = { Text(stringResource(Res.string.onboarding_style_change_desc)) },
            confirmButton = {
                TextButton(onClick = { target.applyTo(choices); pending = null }) {
                    Text(stringResource(Res.string.onboarding_style_change_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }
}

@Composable
private fun StyleCard(style: OnboardingStyle, selected: Boolean, onClick: () -> Unit) {
    val reduceMotion = rememberReduceMotion()
    val spec = if (reduceMotion) snap() else MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.Dp>()
    val corner by animateDpAsState(if (selected) 28.dp else 16.dp, spec, label = "styleCorner")
    val iconCorner by animateDpAsState(if (selected) 20.dp else 11.dp, spec, label = "styleIconCorner")
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "styleContainer"
    )
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(container)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(iconCorner))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center
        ) {
            Icon(style.icon(), null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(style.nameRes()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = content)
            Text(stringResource(style.descRes()), style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.8f))
        }
        if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
    }
}
