package com.example.services

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.SessionLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull

object ChaosOsTester {
    suspend fun runSelfTest(context: Context) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).vaultDao()
        
        suspend fun logResult(type: String, message: String) {
            dao.insertSessionLog(
                SessionLogEntity(
                    timestamp = System.currentTimeMillis(),
                    eventType = type,
                    message = message
                )
            )
        }

        logResult("TEST_START", "Iniciando bateria de testes do Chaos OS...")

        try {
            // 1. Shizuku Connection
            val shizukuAvailable = com.example.utils.ShizukuUtils.isAvailable.value
            val shizukuPermission = com.example.utils.ShizukuUtils.hasPermission.value
            
            if (shizukuAvailable && shizukuPermission) {
                logResult("TEST_SUCCESS", "[Shizuku] Conexão ativa e permissão concedida.")
            } else {
                logResult("TEST_ERROR", "[Shizuku] Falha de conexão ou permissão negada.")
            }

            // 2. Database Status
            val instanceCount = dao.getAllInstanceConfigs().firstOrNull()?.size ?: 0
            logResult("TEST_SUCCESS", "[Database] Banco de dados acessível. Encontradas $instanceCount instâncias virtuais.")

            // 3. Command Execution
            if (shizukuAvailable && shizukuPermission) {
                try {
                    val output = com.example.utils.ShizukuUtils.executeCommand("echo 'CHAOS_TEST'")
                    if (output.trim() == "CHAOS_TEST") {
                        logResult("TEST_SUCCESS", "[Execução] Comandos via Shizuku executando com sucesso.")
                    } else {
                        logResult("TEST_ERROR", "[Execução] Resposta inesperada do Shizuku: $output")
                    }
                } catch(e: Exception) {
                    logResult("TEST_ERROR", "[Execução] Falha ao executar comando root/Shizuku: ${e.message}")
                }
            }

            // 4. File System Checks
            val clonesCount = dao.getAllClones().firstOrNull()?.size ?: 0
            logResult("TEST_SUCCESS", "[FileSystem] Acesso ao banco de clones OK. Encontrados $clonesCount clones.")

            logResult("TEST_SUCCESS", "Bateria de testes finalizada.")
        } catch (e: Exception) {
            logResult("TEST_ERROR", "Erro fatal durante os testes: ${e.message}")
        }
    }
}
