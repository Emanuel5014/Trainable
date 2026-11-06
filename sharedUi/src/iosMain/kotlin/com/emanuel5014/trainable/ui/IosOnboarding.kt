package com.emanuel5014.trainable.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingChoices
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingContent
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingModel
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingPage
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingTitle
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import org.jetbrains.compose.resources.stringResource

/** Same setup as Android, with only capabilities available on iOS. */
@Composable
internal fun IosOnboarding() {
    val scope = rememberCoroutineScope()
    val choices = remember { OnboardingChoices().apply { nextcloudAutoBackupEnabled.value = false } }
    val model = remember { OnboardingModel(IosAppGraph.users, IosAppGraph.preferences, scope) }
    OnboardingContent(
        model = model,
        onFinished = {}, // The persisted flag is the sole owner of the first-launch gate.
        choices = choices,
        pages = IosOnboardingPages,
        wallpaperSupported = false,
        customVibrationSupported = false,
        automaticBackupsSupported = false,
        prepareCompletion = { selected ->
            val timers = selected.timerNotificationsEnabled.value
            val membership = selected.gymMembershipExpiryNotificationsEnabled.value
            if (timers || membership) {
                val allowed = IosNotifications.authorizeForOnboarding()
                selected.timerNotificationsEnabled.value = timers && allowed
                selected.gymMembershipExpiryNotificationsEnabled.value = membership && allowed
            }
        },
        platformPage = { page, _ ->
            check(page == 8)
            OnboardingPage {
                OnboardingTitle(stringResource(Res.string.onboarding_backup_title))
                Text(stringResource(Res.string.onboarding_manual_backup_desc),
                    style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                IosBackupButtons(restoreOnly = true, completeOnboarding = true)
                Text(stringResource(Res.string.onboarding_setup_footer),
                    style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
    )
}

internal val IosOnboardingPages = listOf(0, 1, 2, 3, 4, 5, 6, 8, 10)
