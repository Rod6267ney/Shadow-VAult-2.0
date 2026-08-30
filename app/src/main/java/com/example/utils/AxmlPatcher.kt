package com.example.utils

import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Patcher leve de AXML (Android XML).
 * Modifica o AndroidManifest.xml compilado (binário) para alterar o packageName
 * sem precisar usar Apktool pesado ou depender do PC.
 */
object AxmlPatcher {

    /**
     * Altera o packageName de um AndroidManifest.xml já compilado.
     * Busca a string original no StringPool e substitui, mantendo o tamanho e integridade.
     */
    fun patchPackageName(manifestFile: File, oldPackageName: String, newPackageName: String): Boolean {
        if (!manifestFile.exists()) return false

        try {
            val bytes = manifestFile.readBytes()
            val buffer = ByteBuffer.wrap(bytes)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            // Header AXML: 0x00080003
            val magicNumber = buffer.getInt()
            if (magicNumber != 0x00080003) {
                return false // Não é um arquivo AXML válido
            }

            // Ignorar tamanho do arquivo
            buffer.getInt()

            // Chunk do StringPool
            val stringChunkType = buffer.getInt()
            if (stringChunkType != 0x001C0001) {
                return false
            }

            val stringChunkSize = buffer.getInt()
            val stringCount = buffer.getInt()
            val styleCount = buffer.getInt()
            buffer.getInt() // flags
            val stringsOffset = buffer.getInt()
            val stylesOffset = buffer.getInt()

            // Para o MVP (Minimum Viable Product), usaremos busca/substituição de bytes
            // pois o formato completo do StringPool exige reconstrução complexa dos offsets.
            // Isso funcionará se o novo pacote tiver o mesmo tamanho ou se preenchermos com espaços.
            
            // Garantir que os tamanhos sejam compatíveis no MVP
            val paddedNewPackage = newPackageName.padEnd(oldPackageName.length, ' ')
            if (paddedNewPackage.length > oldPackageName.length) {
                 return false // Para MVP, o novo nome não pode ser maior (exigiria rebuild completo)
            }

            val modifiedBytes = replaceInByteArray(bytes, oldPackageName.toByteArray(Charsets.UTF_16LE), paddedNewPackage.toByteArray(Charsets.UTF_16LE))
            
            val fos = FileOutputStream(manifestFile)
            fos.write(modifiedBytes)
            fos.close()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun replaceInByteArray(source: ByteArray, target: ByteArray, replacement: ByteArray): ByteArray {
        val result = source.copyOf()
        for (i in 0..result.size - target.size) {
            var match = true
            for (j in target.indices) {
                if (result[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                for (j in replacement.indices) {
                    if (j < target.size) {
                         result[i + j] = replacement[j]
                    }
                }
            }
        }
        return result
    }
}
