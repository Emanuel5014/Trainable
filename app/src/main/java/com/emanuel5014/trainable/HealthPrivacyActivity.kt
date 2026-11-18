package com.emanuel5014.trainable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import com.emanuel5014.trainable.ui.components.heart.HealthPrivacyContent
import com.emanuel5014.trainable.ui.theme.GymTrackingTheme

/**
 * Why Trainable reads health data, what it reads and where it keeps it. Health Connect opens it from its permission
 * screen (and Android 14+ from the permission usage screen); without it Health Connect refuses to ask for permissions.
 */
class HealthPrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GymTrackingTheme(darkTheme = isSystemInDarkTheme()) {
                HealthPrivacyContent(onClose = { finish() })
            }
        }
    }
}
