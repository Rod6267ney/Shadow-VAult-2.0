package com.example.services

import com.example.utils.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object DeviceIdentifierGenerator {

    fun generateAndroidId(): String {
        return UUID.randomUUID().toString().replace("-", "").take(16).lowercase()
    }

    /**
     * Generates a 15-digit IMEI with valid Luhn checksum calculation.
     */
    fun generateImei(): String {
        val tacList = listOf("35208411", "86439204", "35876209", "86523904", "35492108")
        val prefix = tacList.random()
        val serial = (1..6).map { (0..9).random() }.joinToString("")
        val first14 = prefix + serial
        var sum = 0
        for (i in 0 until 14) {
            var digit = first14[i].digitToInt()
            if (i % 2 != 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        val checkDigit = (10 - (sum % 10)) % 10
        return first14 + checkDigit
    }

    fun generateMacAddress(): String {
        val random = java.util.Random()
        val mac = ByteArray(6)
        random.nextBytes(mac)
        mac[0] = (mac[0].toInt() and 252 or 2).toByte() // locally administered, unicast
        return mac.joinToString(":") { String.format("%02x", it) }
    }

    fun generateAdId(): String {
        return UUID.randomUUID().toString()
    }

    fun generateSimSerial(): String {
        return "89" + (1..17).map { (0..9).random() }.joinToString("")
    }

    fun generateSerial(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..10).map { chars.random() }.joinToString("")
    }

    suspend fun applySpoofedIdentifiers(
        userId: String,
        fakeAndroidId: String?,
        fakeBrand: String?,
        fakeGps: String?,
        fakeImei: Boolean,
        fakeMac: Boolean,
        fakeAdId: Boolean,
        fakeSim: Boolean
    ) = withContext(Dispatchers.IO) {
        
        if (fakeAndroidId != null) {
            val generatedAndroidId = if (fakeAndroidId == "random") generateAndroidId() else fakeAndroidId
            ShizukuUtils.executeCommand("settings put --user $userId secure fake_device_android_id '$generatedAndroidId'")
            ShizukuUtils.executeCommand("settings put --user $userId secure android_id '$generatedAndroidId'")
        }
        
        if (fakeBrand != null) {
            val parts = fakeBrand.split(" - ")
            val brand = parts.firstOrNull() ?: "Google"
            val model = parts.lastOrNull() ?: "Pixel"
            ShizukuUtils.executeCommand("settings put --user $userId secure fake_device_brand '$brand'")
            ShizukuUtils.executeCommand("settings put --user $userId secure fake_device_model '$model'")
            // Hooks for Build.DEVICE and Build.SERIAL
            ShizukuUtils.executeCommand("settings put --user $userId secure fake_device_name '${model.replace(" ", "_")}'")
            ShizukuUtils.executeCommand("settings put --user $userId secure fake_device_serial '${generateSerial()}'")
        }
        
        if (fakeGps != null) {
            ShizukuUtils.executeCommand("settings put --user $userId secure mock_location '1'")
            ShizukuUtils.executeCommand("settings put --user $userId secure fake_location '$fakeGps'")
        }
        
        if (fakeImei) {
            val imei = generateImei()
            ShizukuUtils.executeCommand("settings put --user $userId secure fake_device_imei '$imei'")
        }
        
        if (fakeMac) {
            val mac = generateMacAddress()
            ShizukuUtils.executeCommand("settings put --user $userId secure fake_device_mac '$mac'")
            ShizukuUtils.executeCommand("settings put --user $userId secure fake_wifi_mac '$mac'")
        }
        
        if (fakeAdId) {
            val adId = generateAdId()
            ShizukuUtils.executeCommand("settings put --user $userId secure fake_ad_id '$adId'")
            ShizukuUtils.executeCommand("settings put --user $userId global advertising_id '$adId'")
        }
        
        if (fakeSim) {
            ShizukuUtils.executeCommand("settings put --user $userId secure fake_sim_serial '${generateSimSerial()}'")
        }
    }
}
