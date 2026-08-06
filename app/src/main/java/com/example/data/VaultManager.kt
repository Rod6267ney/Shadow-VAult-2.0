package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VaultManager(context: Context) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val alias = "ProfileConfigKey"
    private val dao = AppDatabase.getDatabase(context).vaultDao()

    init {
        createKeyIfNeeded()
    }

    private fun createKeyIfNeeded() {
        if (!keyStore.containsAlias(alias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(alias, null) as SecretKey
    }

    private fun encrypt(data: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        
        val combined = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    private fun decrypt(encryptedBase64: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, combined, 0, 12)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        
        val decryptedData = cipher.doFinal(combined, 12, combined.size - 12)
        return String(decryptedData, Charsets.UTF_8)
    }
    
    fun encryptInstanceData(data: String): String {
        return encrypt(data)
    }

    fun decryptInstanceData(encryptedBase64: String): String {
        return decrypt(encryptedBase64)
    }
    
    suspend fun saveProfileConfig(profileId: String, configJson: String) = withContext(Dispatchers.IO) {
        val encryptedData = encrypt(configJson)
        var profileName = ""
        var jobTitle = "Unknown"
        var category = "Uncategorized"
        try {
            if (configJson.contains("{")) {
                val jsonObj = org.json.JSONObject(configJson)
                profileName = jsonObj.optString("fakeName", "")
                jobTitle = jsonObj.optString("jobTitle", "Unknown")
                category = "Persona Space"
            }
        } catch(e: Exception) {}

        val entity = ProfileConfigEntity(
            profileId = profileId,
            profileName = profileName,
            category = category,
            jobTitle = jobTitle,
            encryptedConfigData = encryptedData
        )
        dao.insertProfileConfig(entity)
    }

    suspend fun getAllProfileConfigs(): Map<String, ProfileConfigEntity> = withContext(Dispatchers.IO) {
        dao.getAllProfileConfigs().associateBy { it.profileId }
    }
    
    suspend fun getProfileConfig(profileId: String): String? = withContext(Dispatchers.IO) {
        val entity = dao.getProfileConfig(profileId) ?: return@withContext null
        return@withContext try {
            decrypt(entity.encryptedConfigData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val prefs = context.getSharedPreferences("ShadowVaultSettings", Context.MODE_PRIVATE)

    fun saveAppLanguage(lang: String) {
        prefs.edit().putString("app_language", lang).apply()
    }

    fun getAppLanguage(): String {
        return prefs.getString("app_language", "en") ?: "en"
    }

    fun saveGeminiApiKey(key: String) {
        if (key.isBlank()) {
            prefs.edit().remove("gemini_api_key").apply()
            return
        }
        val encryptedKey = try { encrypt(key) } catch(e: Exception) { key }
        prefs.edit().putString("gemini_api_key", encryptedKey).apply()
    }

    fun getGeminiApiKey(): String {
        val encryptedKey = prefs.getString("gemini_api_key", "") ?: ""
        if (encryptedKey.isEmpty()) return ""
        return try { decrypt(encryptedKey) } catch(e: Exception) { encryptedKey }
    }

    fun saveProfileImageUri(uri: String) {
        prefs.edit().putString("profile_image_uri", uri).apply()
    }

    fun getProfileImageUri(): String? {
        return prefs.getString("profile_image_uri", null)
    }

    fun saveThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString("theme_mode", "DARK") ?: "DARK"
    }
}
