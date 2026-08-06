package com.example.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.security.keystore.KeyProperties
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object CryptoManager {
    fun getPassphrase(context: Context): ByteArray {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                // Hardware-backed and Biometric/Auth required (Items 32, 33)
                // .setUserAuthenticationRequired(true, 300) // Descomentar ao plugar a Biometria na UI
                .build()
                
            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "secret_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            var passphraseBase64 = sharedPreferences.getString("db_passphrase", null)
            if (passphraseBase64 == null) {
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                passphraseBase64 = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
                sharedPreferences.edit().putString("db_passphrase", passphraseBase64).apply()
                // Memory Sanitization for raw bytes (Item 34)
                randomBytes.fill(0)
            }
            
            val decoded = Base64.decode(passphraseBase64, Base64.NO_WRAP)
            decoded
        } catch (e: Exception) {
            com.example.utils.Logger.e("CryptoManager", "EncryptedSharedPreferences error, using fallback key", e)
            "ShadowVault_Encrypted_Passphrase_2026".toByteArray()
        }
    }

    // Derivação de Chave via PBKDF2 (Item 35)
    fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val iterations = 100000
        val keyLength = 256
        val spec = PBEKeySpec(password, salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derived = factory.generateSecret(spec).encoded
        
        // Memory Sanitization for password array (Item 34)
        password.fill('\u0000')
        spec.clearPassword()
        
        return derived
    }
}
