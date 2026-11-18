package com.emanuel5014.trainable.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.emanuel5014.trainable.R

@Composable
fun DonorsScreen(onNavigateBack: () -> Unit, donors: List<Donor> = defaultDonors) {
    val context = LocalContext.current
    DonorsContent(
        onNavigateBack = onNavigateBack,
        onSupport = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/emanuel5014"))
            context.startActivity(intent)
        },
        donors = donors,
        kofiIcon = painterResource(R.drawable.ic_kofi)
    )
}
