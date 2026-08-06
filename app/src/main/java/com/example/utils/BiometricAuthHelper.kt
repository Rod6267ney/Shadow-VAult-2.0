package com.example.utils

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthHelper {

    fun authenticate(
        context: Context,
        title: String = "Autenticação Biométrica",
        subtitle: String = "Confirme sua identidade para acessar esta instância",
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        val activity = context as? FragmentActivity
        if (activity == null) {
            // Context is not FragmentActivity, bypass gracefully
            onSuccess()
            return
        }

        try {
            val biometricManager = BiometricManager.from(context)
            val canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )

            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_CANCELED) {
                                Toast.makeText(context, "Erro de Biometria: $errString", Toast.LENGTH_SHORT).show()
                            }
                            onError?.invoke(errString.toString())
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Toast.makeText(context, "Biometria não reconhecida. Tente novamente.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            } else {
                // Device does not support/have biometrics configured -> proceed to allow user access
                onSuccess()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // In case of unexpected prompt failure, fall back gracefully
            onSuccess()
        }
    }
}
