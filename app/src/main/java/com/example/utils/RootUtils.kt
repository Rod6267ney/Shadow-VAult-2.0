package com.example.utils

import java.io.BufferedReader
import java.io.InputStreamReader

object RootUtils {

    /**
     * Tenta executar um comando via `su`. 
     * Retorna um par (sucesso: Boolean, output: String).
     */
    fun executeRootCommand(command: String): Pair<Boolean, String> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            while (errorReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            process.waitFor()
            val exitCode = process.exitValue()
            
            Pair(exitCode == 0, output.toString().trim())
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Root execution failed: ${e.message}")
        }
    }

    /**
     * Verifica se o binário `su` está disponível.
     */
    fun isRootAvailable(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (java.io.File(path).exists()) return true
        }
        
        // Testa a execução
        val (success, _) = executeRootCommand("id")
        return success
    }
}
