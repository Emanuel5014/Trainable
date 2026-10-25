package com.emanuel5014.trainable.ui.components.journal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.journal.JournalRating
import com.emanuel5014.trainable.ui.components.motion.rememberAnimatedShape
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import kotlinx.coroutines.delay

/** Gap between two stars lighting up one after the other. */
private const val STAGGER_MS = 30L

@Composable
fun ratingLabel(rating: Int): String = stringResource(
    when (rating) {
        1 -> R.string.journal_rating_1
        2 -> R.string.journal_rating_2
        3 -> R.string.journal_rating_3
        4 -> R.string.journal_rating_4
        else -> R.string.journal_rating_5
    }
)

/** "★ 4": a rating as small as a chip, for lists. */
@Composable
fun RatingPill(rating: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.journal_rating_cd, rating)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Primary.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
        Text(
            text = rating.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Primary
        )
    }
}

/**
 * Five stars in a row. Tapping star n rates the workout n (stars 1..n light up in a quick cascade with a springy pop,
 * the chosen star sits in a container that morphs from a rounded square into a cookie shape) and tapping the chosen
 * star again clears the rating. With [readOnly] it is only a row of small stars: no tapping, no label.
 */
@Composable
fun RatingStars(
    rating: Int?,
    onRatingChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    hapticEnabled: Boolean = true,
    readOnly: Boolean = false
) {
    if (readOnly) {
        val description = rating?.let { stringResource(R.string.journal_rating_cd, it) }
        Row(
            modifier = modifier.clearAndSetSemantics { description?.let { contentDescription = it } },
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (star in JournalRating.MIN..JournalRating.MAX) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = if (rating != null && star <= rating) Primary else OnSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        return
    }

    val haptic = LocalHapticFeedback.current
    // The stars that were already lit stay still: the cascade starts at the first one that is new
    val previous = remember { intArrayOf(rating ?: 0) }
    val firstNew = previous[0] + 1
    SideEffect { previous[0] = rating ?: 0 }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (star in JournalRating.MIN..JournalRating.MAX) {
                StarButton(
                    star = star,
                    filled = rating != null && star <= rating,
                    chosen = star == rating,
                    stagger = (star - firstNew).coerceAtLeast(0) * STAGGER_MS,
                    onClick = {
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRatingChange(if (rating == star) null else star)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        AnimatedContent(
            targetState = rating,
            transitionSpec = {
                (fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 3 }) togetherWith fadeOut(tween(90))
            },
            label = "ratingLabel"
        ) { shown ->
            Text(
                text = if (shown != null) ratingLabel(shown) else stringResource(R.string.journal_not_rated),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (shown != null) Primary else OnSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StarButton(
    star: Int,
    filled: Boolean,
    chosen: Boolean,
    stagger: Long,
    onClick: () -> Unit
) {
    val morph = remember { Morph(MaterialShapes.Square, MaterialShapes.Cookie9Sided) }
    val morphProgress by animateFloatAsState(
        targetValue = if (chosen) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "starShape"
    )
    val shape = rememberAnimatedShape(morph, morphProgress)
    val tint by animateColorAsState(
        targetValue = if (filled) Primary else OnSurfaceVariant.copy(alpha = 0.35f),
        animationSpec = tween(durationMillis = 140, delayMillis = if (filled) stagger.toInt() else 0),
        label = "starTint"
    )

    // A little pop when the star lights up (not when the sheet opens with the stars already lit)
    val pop = remember { Animatable(1f) }
    val firstRun = remember { booleanArrayOf(true) }
    LaunchedEffect(filled) {
        val skip = firstRun[0]
        firstRun[0] = false
        if (filled && !skip) {
            delay(stagger)
            pop.animateTo(1.3f, tween(70))
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }

    val description = stringResource(R.string.journal_rating_cd, star)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(if (chosen) Primary.copy(alpha = 0.14f) else Color.Transparent)
            .selectable(selected = chosen, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(32.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
        )
    }
}
