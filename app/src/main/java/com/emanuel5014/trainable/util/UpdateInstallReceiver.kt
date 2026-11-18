package com.emanuel5014.trainable.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Receives Android's answers about the update's installer session and passes them to [UpdateManager]. */
@AndroidEntryPoint
class UpdateInstallReceiver : BroadcastReceiver() {

    @Inject
    lateinit var updateManager: UpdateManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return
        val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }
        updateManager.onInstallerStatus(
            sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1),
            tag = intent.getStringExtra(EXTRA_TAG),
            status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE),
            message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
            confirm = confirm
        )
    }

    companion object {
        private const val ACTION_INSTALL_STATUS = "com.emanuel5014.trainable.action.UPDATE_INSTALL_STATUS"
        private const val EXTRA_TAG = "update_tag"

        /** Where Android sends the session's answers; mutable because Android adds the status to the intent. */
        fun statusSender(context: Context, sessionId: Int, tag: String): IntentSender {
            val intent = Intent(context, UpdateInstallReceiver::class.java)
                .setAction(ACTION_INSTALL_STATUS)
                .putExtra(EXTRA_TAG, tag)
            val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            return PendingIntent.getBroadcast(
                context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT or mutable
            ).intentSender
        }
    }
}
