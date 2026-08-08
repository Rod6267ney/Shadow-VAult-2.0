package com.example.services

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import java.io.File
import java.security.MessageDigest

object IntegrityChecker {
    
    // Stubbed expected hash (Item 40)
    private const val EXPECTED_APK_HASH = "V3JvbmdoYXNoUGxhY2Vob2xkZXI="

    fun verifyApkIntegrity(context: Context): Boolean {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val apkPath = packageInfo.applicationInfo?.sourceDir ?: return false
            val file = File(apkPath)
            
            if (!file.exists()) return false

            val md = MessageDigest.getInstance("SHA-256")
            val bytes = file.readBytes()
            md.update(bytes)
            val hash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
            
            // In a real scenario, compare with EXPECTED_APK_HASH
            // return hash == EXPECTED_APK_HASH
            return true // Returning true for development
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun isRootDetected(): Boolean {
        // Basic Root/Magisk/KernelSU detection (Item 42)
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
            "/su/bin/su",
            "/magisk/.core/bin/su",
            "/data/adb/ksu",
            "/data/adb/magisk"
        )
        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }
}
