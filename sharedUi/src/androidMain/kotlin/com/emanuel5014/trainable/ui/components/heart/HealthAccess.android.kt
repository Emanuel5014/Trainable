package com.emanuel5014.trainable.ui.components.heart

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.emanuel5014.trainable.data.repository.HealthConnectHeartRateSource

/** Health Connect: built into Android 14+, an app from the Play Store on older versions. */
@Composable
actual fun rememberHealthAccess(): HealthAccess {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<(Boolean) -> Unit>(1) }
    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        pending[0]?.invoke(granted.containsAll(HealthConnectHeartRateSource.PERMISSIONS))
        pending[0] = null
    }
    val status = remember { HealthConnectHeartRateSource.sdkStatus(context) }
    return remember(launcher, status) {
        HealthAccess(
            available = status != HealthConnectClient.SDK_UNAVAILABLE,
            needsInstall = status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
            request = { onResult ->
                if (HealthConnectHeartRateSource.sdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                    pending[0] = onResult
                    runCatching { launcher.launch(HealthConnectHeartRateSource.PERMISSIONS) }
                        .onFailure { pending[0] = null; onResult(false) }
                } else {
                    // Not installed or too old: its Play Store page, which comes back to the app's onboarding
                    val uri = Uri.parse("market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding")
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }.onFailure {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PROVIDER"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                    onResult(false)
                }
            },
            isGranted = { HealthConnectHeartRateSource(context).hasPermissions() }
        )
    }
}

private const val PROVIDER = "com.google.android.apps.healthdata"
