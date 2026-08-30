package com.example.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.zip.ZipInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Gerencia o ciclo de vida das imagens (ROMs GSI) do Android secundário.
 * Responsável por baixar, extrair (tar.gz, img) e preparar o RootFS para o libloader.so
 */
object RomManager {
    private const val TAG = "RomManager"
    
    // Configuração do HttpClient para downloads grandes
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .build()
    }

    // URLs Reais (Em um cenário de produção seriam URLs dinâmicos ou de um bucket S3/Release)
    private const val ROM_URL_8 = "https://sourceforge.net/projects/gsi-albus/files/latest/download" 
    private const val ROM_URL_10 = "https://sourceforge.net/projects/gsi-albus/files/latest/download"

    /**
     * Prepara a imagem ROM (RootFS) no diretório interno do aplicativo.
     * Baixa o arquivo tar.gz e descompacta o system.img/vendor.img para o rootfs.
     */
    suspend fun setupRom(context: Context?, version: String, onProgress: ((Int) -> Unit)? = null) {
        if (context == null) {
            Log.e(TAG, "Contexto nulo, impossível preparar a ROM.")
            return
        }

        val rootfsDir = File(context.filesDir, "rootfs")
        if (!rootfsDir.exists()) {
            rootfsDir.mkdirs()
        }

        val url = if (version.contains("10")) ROM_URL_10 else ROM_URL_8
        val archiveFile = File(context.cacheDir, "rom_archive.zip")

        // 1. Download Real
        if (!archiveFile.exists() || archiveFile.length() < 100 * 1024 * 1024) { // Menos de 100MB assumimos incompleto
            Log.d(TAG, "Iniciando download real da ROM a partir de: $url")
            downloadFile(url, archiveFile, onProgress)
        } else {
            Log.d(TAG, "Arquivo da ROM já baixado em cache.")
            onProgress?.invoke(50) // Pula progressão de download
        }

        // 2. Extração Real
        val systemImg = File(rootfsDir, "system.img")
        if (!systemImg.exists()) {
            Log.d(TAG, "Extraindo RootFS...")
            extractZip(archiveFile, rootfsDir)
            onProgress?.invoke(90)
        } else {
            Log.d(TAG, "RootFS já extraído.")
            onProgress?.invoke(100)
        }

        Log.d(TAG, "ROM $version configurada com sucesso e pronta para injeção via Root.")
        onProgress?.invoke(100)
    }

    /**
     * Prepara a imagem ROM a partir de um arquivo local selecionado pelo usuário.
     */
    suspend fun setupLocalRom(context: Context, uri: android.net.Uri, onProgress: ((Int) -> Unit)?) = withContext(Dispatchers.IO) {
        val rootfsDir = File(context.filesDir, "rootfs")
        if (!rootfsDir.exists()) {
            rootfsDir.mkdirs()
        }

        val archiveFile = File(context.cacheDir, "rom_archive.zip")
        Log.d(TAG, "Copiando ROM local do Uri: $uri")

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Não foi possível abrir o arquivo selecionado.")
            
            val outputStream = FileOutputStream(archiveFile)
            val buffer = ByteArray(8192)
            var len: Int
            var copied = 0L
            val totalSizeEstimate = 1_500_000_000L // Estimativa de 1.5GB
            
            while (inputStream.read(buffer).also { len = it } != -1) {
                outputStream.write(buffer, 0, len)
                copied += len
                val p = ((copied.toFloat() / totalSizeEstimate) * 50f).toInt().coerceAtMost(50)
                onProgress?.invoke(p)
            }
            outputStream.close()
            inputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao copiar ROM local", e)
            throw IOException("Falha ao copiar ROM local: ${e.message}")
        }

        Log.d(TAG, "Extraindo RootFS...")
        extractZip(archiveFile, rootfsDir)
        onProgress?.invoke(100)
    }

    private suspend fun downloadFile(url: String, destFile: File, onProgress: ((Int) -> Unit)?) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Falha no download: $response")

            val body = response.body
            if (body == null) {
                Log.e(TAG, "Corpo da resposta nulo.")
                return@withContext
            }

            val contentLength = body.contentLength()
            val source = body.source()
            val sink = FileOutputStream(destFile)

            var bytesCopied = 0L
            val buffer = ByteArray(8 * 1024)
            var bytes = source.read(buffer)
            
            while (bytes >= 0) {
                sink.write(buffer, 0, bytes)
                bytesCopied += bytes
                
                if (contentLength > 0 && onProgress != null) {
                    val progress = (bytesCopied * 50 / contentLength).toInt() // Pesa 50% no progresso total
                    onProgress(progress)
                }
                
                bytes = source.read(buffer)
            }
            
            sink.flush()
            sink.close()
            source.close()
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao baixar ROM", e)
            if (destFile.exists()) destFile.delete()
            throw IOException("Falha no download da ROM: ${e.message}")
        }
    }

    private suspend fun extractZip(archiveFile: File, destDir: File) = withContext(Dispatchers.IO) {
        try {
            val zipInputStream = ZipInputStream(archiveFile.inputStream())
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                val outputFile = File(destDir, entry.name)
                
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    val outputStream = FileOutputStream(outputFile)
                    
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (zipInputStream.read(buffer).also { len = it } != -1) {
                        outputStream.write(buffer, 0, len)
                    }
                    outputStream.close()
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao extrair zip", e)
            throw IOException("Failed to extract RootFS: ${e.message}")
        }
    }
}
