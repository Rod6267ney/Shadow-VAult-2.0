package com.example.services

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.example.utils.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BackupManager {

    suspend fun backupInstance(context: Context, userId: String, packageName: String): Result<String> = withContext(Dispatchers.IO) {
        if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
            return@withContext Result.failure(Exception("Shizuku não disponível"))
        }

        try {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Iniciando snapshot da Instância $userId...", Toast.LENGTH_SHORT).show() }

            val backupDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ShadowVault_Backups")
            
            // Tenta criar o diretório de backups via Shizuku para evitar problemas de permissão
            ShizukuUtils.executeCommand("mkdir -p ${backupDir.absolutePath}")
            
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "snapshot_user_${userId}_${packageName}_$timestamp.tar.gz")
            val targetDir = "/data/user/$userId/$packageName"

            // Verifica se o diretório existe
            val checkDir = ShizukuUtils.executeCommand("ls $targetDir")
            if (checkDir.contains("No such file or directory")) {
                return@withContext Result.failure(Exception("Aplicativo não encontrado na Instância $userId"))
            }

            // Para o aplicativo antes do backup para consistência dos dados
            ShizukuUtils.executeCommand("am force-stop --user $userId $packageName")

            // Cria o arquivo tar.gz usando o binário tar do Android (via shell do Shizuku)
            // A flag -p preserva permissões
            val cmd = "tar -czpf ${backupFile.absolutePath} -C $targetDir ."
            val output = ShizukuUtils.executeCommand(cmd, 60000L) // Timeout maior para backup

            if (output.contains("Error") || output.contains("Exception") || output.contains("tar: ")) {
                 return@withContext Result.failure(Exception("Erro no backup: $output"))
            }

            // Ajusta a permissão do arquivo de backup para ser lido pelo usuário comum
            ShizukuUtils.executeCommand("chmod 666 ${backupFile.absolutePath}")

            val msg = "Snapshot criado com sucesso em: ${backupFile.absolutePath}"
            withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
            
            Result.success(backupFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreInstance(context: Context, userId: String, packageName: String, backupFilePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
            return@withContext Result.failure(Exception("Shizuku não disponível"))
        }

        try {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Restaurando snapshot na Instância $userId...", Toast.LENGTH_SHORT).show() }

            val targetDir = "/data/user/$userId/$packageName"

            // 1. Para o app
            ShizukuUtils.executeCommand("am force-stop --user $userId $packageName")

            // 2. Limpa os dados atuais usando pm clear (isso recria o diretório limpo e os IDs corretos)
            val clearRes = ShizukuUtils.executeCommand("pm clear --user $userId $packageName")
            if (clearRes.contains("Failed")) {
                 return@withContext Result.failure(Exception("Não foi possível limpar a pasta original antes do restore."))
            }

            // 3. Lê o UID e GID da pasta recriada para restaurar a posse correta
            val statOutput = ShizukuUtils.executeCommand("stat -c '%u:%g' $targetDir")
            val owner = statOutput.trim()

            // 4. Restaura o tar.gz
            val cmd = "tar -xzpf $backupFilePath -C $targetDir"
            val output = ShizukuUtils.executeCommand(cmd, 60000L)

            if (output.contains("Error") || output.contains("Exception") || output.contains("tar: ")) {
                 return@withContext Result.failure(Exception("Erro no restore: $output"))
            }

            // 5. Restaura as permissões/owner originais da instalação (crucial para o Android não crashar o app)
            if (owner.isNotBlank() && owner.contains(":")) {
                ShizukuUtils.executeCommand("chown -R $owner $targetDir")
            }
            
            // Restaura o SELinux context (chcon -R u:object_r:app_data_file:s0:c... é complexo, usamos restorecon)
            ShizukuUtils.executeCommand("restorecon -R $targetDir")

            val msg = "Snapshot restaurado com sucesso!"
            withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun listBackups(): List<File> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ShadowVault_Backups")
            if (backupDir.exists() && backupDir.isDirectory) {
                return@withContext backupDir.listFiles()?.filter { it.name.endsWith(".tar.gz") }?.toList() ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }
}
