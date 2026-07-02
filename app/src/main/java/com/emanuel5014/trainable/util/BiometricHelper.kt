package com.emanuel5014.trainable.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun checkAndShowBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val biometricManager = BiometricManager.from(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuthenticate = biometricManager.canAuthenticate(authenticators)

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        activity.runOnUiThread { onSuccess() }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        activity.runOnUiThread { onError(errString.toString()) }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Accesso Protetto")
                .setSubtitle("Autenticati per accedere ai Check Fisici")
                .setAllowedAuthenticators(authenticators)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            // Se non disponibile o configurata (es. no lockscreen), consenti l'accesso direttamente
            onSuccess()
        }
    }
}
