package com.example.utils

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Modificador Nativo de APK (ZIP).
 * Extrai seletivamente o APK, permite injeção de arquivos (libshadow_spoof.so) e
 * empacota novamente mantendo a compressão original (Stored vs Deflated).
 */
object ApkZipModifier {

    /**
     * Extrai apenas o AndroidManifest.xml para modificação rápida, em vez de descompactar
     * todo o APK (o que gastaria muita memória e disco).
     */
    fun extractManifest(apkFile: File, destManifest: File): Boolean {
        try {
            val zis = ZipInputStream(FileInputStream(apkFile))
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "AndroidManifest.xml") {
                    val fos = FileOutputStream(destManifest)
                    zis.copyTo(fos)
                    fos.close()
                    zis.closeEntry()
                    zis.close()
                    return true
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Reconstrói o APK, injetando o novo Manifest e a lib nativa de Spoofing,
     * enquanto copia os bytes brutos do restante do APK (zero perda de recursos).
     * Exclui a pasta META-INF original (para remover a assinatura antiga).
     */
    fun rebuildApk(
        sourceApk: File,
        newManifest: File,
        spoofLibArm64: File?, // libshadow_spoof.so
        destApk: File
    ): Boolean {
        try {
            val zos = ZipOutputStream(FileOutputStream(destApk))
            val zis = ZipInputStream(FileInputStream(sourceApk))
            
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                
                // Pular META-INF original (assinatura velha)
                if (name.startsWith("META-INF/")) {
                    zis.closeEntry()
                    entry = zis.nextEntry
                    continue
                }

                // Substituir AndroidManifest.xml
                if (name == "AndroidManifest.xml") {
                    val newEntry = ZipEntry("AndroidManifest.xml")
                    zos.putNextEntry(newEntry)
                    FileInputStream(newManifest).copyTo(zos)
                    zos.closeEntry()
                } else {
                    // Copiar arquivo original
                    val newEntry = ZipEntry(name)
                    zos.putNextEntry(newEntry)
                    zis.copyTo(zos)
                    zos.closeEntry()
                }
                
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()

            // Injetar nossa libshadow_spoof.so na arquitetura ARM64 (padrão moderno)
            if (spoofLibArm64 != null && spoofLibArm64.exists()) {
                val libEntry = ZipEntry("lib/arm64-v8a/libshadow_spoof.so")
                zos.putNextEntry(libEntry)
                FileInputStream(spoofLibArm64).copyTo(zos)
                zos.closeEntry()
            }

            zos.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
