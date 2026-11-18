package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.theme.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.ic_github
import com.emanuel5014.trainable.sharedui.resources.ic_kofi
import com.emanuel5014.trainable.sharedui.resources.about
import com.emanuel5014.trainable.sharedui.resources.buy_me_a_coffee
import com.emanuel5014.trainable.sharedui.resources.donors_desc
import com.emanuel5014.trainable.sharedui.resources.donors_title
import com.emanuel5014.trainable.sharedui.resources.star_on_github

/** About links shared by both apps; the Android updater is supplied only by the Android shell. */
@Composable
fun AboutSettingsSection(
    versionName: String,
    onOpenSource: () -> Unit,
    onSupport: () -> Unit,
    onNavigateToDonors: () -> Unit,
    onAuthorEasterEgg: () -> Unit,
    githubIcon: Painter = painterResource(Res.drawable.ic_github),
    kofiIcon: Painter = painterResource(Res.drawable.ic_kofi),
    authorHeart: InlineTextContent? = null,
    updateAction: @Composable () -> Unit = {}
) {
    var easterEggClicks by remember { mutableIntStateOf(0) }
    SettingsSection(title = stringResource(Res.string.about)) {
        GymCard(containerColor = SurfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (versionName.isBlank()) "Trainable" else "Trainable v$versionName",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = remember {
                                buildAnnotatedString {
                                    append("Made with ")
                                    appendInlineContent("authorHeart", "❤️")
                                    append(" by Emanuel5014")
                                }
                            },
                            inlineContent = authorHeart?.let { mapOf("authorHeart" to it) } ?: emptyMap(),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                easterEggClicks++
                                if (easterEggClicks >= 3) {
                                    onAuthorEasterEgg()
                                    easterEggClicks = 0
                                }
                            }
                        )
                    }
                    updateAction()
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Surface.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GymButton(
                        onClick = {
                            onOpenSource()
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = Surface.copy(alpha = 0.5f),
                        contentColor = OnSurface
                    ) {
                        Icon(
                            painter = githubIcon,
                            contentDescription = null,
                            tint = OnSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.star_on_github), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                    }

                    GymButton(
                        onClick = {
                            onSupport()
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = Surface.copy(alpha = 0.5f),
                        contentColor = OnSurface
                    ) {
                        Icon(
                            painter = kofiIcon,
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.buy_me_a_coffee), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Surface.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateToDonors() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Rounded.VolunteerActivism,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                stringResource(Res.string.donors_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                stringResource(Res.string.donors_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                }
            }
        }
    }
}
