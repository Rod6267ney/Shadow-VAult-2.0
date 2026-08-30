package com.example.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.android.apksig.ApkSigner
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal

/**
 * Motor de assinatura de APK V2 (APK Signature Scheme v2).
 * Responsável por gerar um certificado invisível dinâmico via Hardware/AndroidKeyStore
 * e assinar o APK usando a biblioteca oficial do Google (apksig) para que o Package Manager
 * valide e instale o Clone Supremo.
 */
object V2Signer {

    private const val TAG = "V2Signer"
    private const val KEY_ALIAS = "shadow_clone_key"

    /**
     * Assina um arquivo APK não assinado usando o Esquema V2 e V3.
     */
    fun signApk(context: Context, inputApk: File, outputApk: File): Boolean {
        try {
            Log.d(TAG, "Iniciando processo de assinatura V2 dinâmico...")

            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            // Gera a chave apenas se ela não existir
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateDynamicKeyStore()
            }

            val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
            val certificate = keyStore.getCertificate(KEY_ALIAS) as X509Certificate

            // Usa a biblioteca apksig do Google para assinar fisicamente o arquivo
            val signerConfig = ApkSigner.SignerConfig.Builder("shadow_signer", privateKey, listOf(certificate)).build()
            
            Log.d(TAG, "Aplicando assinaturas criptográficas no APK...")
            ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(inputApk)
                .setOutputApk(outputApk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true) // Assinatura avançada
                .build()
                .sign()

            Log.d(TAG, "Assinatura injetada com sucesso no APK: ${outputApk.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Falha Crítica ao assinar APK: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun generateDynamicKeyStore() {
        Log.d(TAG, "Gerando novo certificado Shadow CA via AndroidKeyStore (Hardware/Keystore)...")
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setCertificateSubject(X500Principal("CN=Shadow Vault CA, O=Shadow, C=US"))
            .setCertificateSerialNumber(BigInteger.valueOf(1))
            .build()

        kpg.initialize(parameterSpec)
        kpg.generateKeyPair()
        Log.d(TAG, "Certificado digital forjado com sucesso (Shadow CA).")
    }
}
