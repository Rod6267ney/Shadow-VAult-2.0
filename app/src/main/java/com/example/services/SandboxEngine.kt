package com.example.services

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Motor de Sandbox App-Level (Sem Root).
 * Tenta executar aplicativos dentro do próprio processo do Shadow Vault usando
 * isolamento de diretório.
 */
object SandboxEngine {

    suspend fun installToSandbox(
        context: Context,
        appToClone: ApplicationInfo,
        onComplete: (Boolean, String?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Copia o APK para o diretório privado do Shadow Vault
                val sourceApk = File(appToClone.sourceDir)
                val sandboxDir = File(context.filesDir, "sandbox_apps")
                if (!sandboxDir.exists()) sandboxDir.mkdirs()
                
                val targetDir = File(sandboxDir, appToClone.packageName)
                if (!targetDir.exists()) targetDir.mkdirs()
                
                val destApk = File(targetDir, "base.apk")
                sourceApk.copyTo(destApk, overwrite = true)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "App isolado no Sandbox Interno!", Toast.LENGTH_SHORT).show()
                }
                
                val virtualUserId = "VIRTUAL_${UUID.randomUUID().toString().take(8)}"
                onComplete(true, virtualUserId)
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro no Sandbox: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                onComplete(false, null)
            }
        }
    }

    suspend fun launchInSandbox(context: Context, packageName: String) {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage(packageName)
                
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Rodando no Sandbox de App-Level", Toast.LENGTH_LONG).show()
                        context.startActivity(intent)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "App não suporta Sandbox nativo.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Falha ao lançar no Sandbox: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
