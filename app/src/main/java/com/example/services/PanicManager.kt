package com.example.services

import android.content.Context
import android.widget.Toast
import com.example.data.AppDatabase
import com.example.utils.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore

object PanicManager {
    suspend fun executePanicWipe(context: Context) = withContext(Dispatchers.IO) {
        if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Shizuku needed for wipe", Toast.LENGTH_SHORT).show() }
            return@withContext
        }

        withContext(Dispatchers.Main) { Toast.makeText(context, "Iniciando Limpeza de Emergência...", Toast.LENGTH_LONG).show() }

        val dao = AppDatabase.getDatabase(context).vaultDao()
        val workspaces = ShizukuUtils.getWorkspaces()
        
        for (ws in workspaces) {
            ShizukuUtils.executeCommand("pm remove-user ${ws.id}")
        }
        
        // Zeroization: Delete Master Key from KeyStore (Item 39)
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry("_androidx_security_master_key_")
            withContext(Dispatchers.Main) { Toast.makeText(context, "Chave Criptográfica Destruída", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            com.example.utils.Logger.e("PanicManager", "Falha ao destruir chave", e)
        }
        
        // Clear DB
        dao.clearSessionLogs()
        dao.clearAllClones()
        dao.clearAllIdentities()
        dao.clearAllProfileConfigs()
        
        // Add a single log entry saying panic wipe was executed
        dao.insertSessionLog(com.example.data.SessionLogEntity(eventType = "PANIC_WIPE", message = "All workspaces and data wiped successfully."))

        withContext(Dispatchers.Main) { Toast.makeText(context, "Limpeza de Emergência Concluída.", Toast.LENGTH_LONG).show() }
    }
}
