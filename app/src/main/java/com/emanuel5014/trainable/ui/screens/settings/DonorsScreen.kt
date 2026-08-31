package com.emanuel5014.trainable.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest

data class Donor(
    val name: String,
    val tier: String? = null,
    val note: String? = null
)

val defaultDonors = listOf(
    Donor(
        name = "THED4NIEL",
        tier = "Supporter"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorsScreen(
    onNavigateBack: () -> Unit,
    donors: List<Donor> = defaultDonors
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.donors_title),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        GymIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = onNavigateBack,
                            containerColor = Color.Transparent,
                            contentColor = OnSurface,
                            description = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = OnSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Gratitude Card
            GymCard(containerColor = SurfaceContainerHigh) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VolunteerActivism,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.donors_hero_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = OnSurface,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.donors_hero_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }

                    GymButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/emanuel5014"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Primary,
                        contentColor = OnPrimary,
                        height = 48,
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_kofi),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.donors_support_button),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Hall of Fame Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.donors_hall_of_fame),
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                GymCard(containerColor = SurfaceContainerHigh) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        donors.forEachIndexed { index, donor ->
                            if (index > 0) {
                                HorizontalDivider(color = Surface.copy(alpha = 0.5f))
                            }
                            DonorItem(donor = donor)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DonorItem(donor: Donor) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = donor.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = donor.name,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
            if (!donor.note.isNullOrBlank()) {
                Text(
                    text = donor.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }

        donor.tier?.let { tier ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerHighest)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (tier == "Supporter") stringResource(R.string.donor_supporter_badge) else tier,
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
