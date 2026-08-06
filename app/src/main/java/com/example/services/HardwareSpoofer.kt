package com.example.services

import com.example.utils.ShizukuUtils

object HardwareSpoofer {

    suspend fun spoofBattery(level: Int = 85, temp: Int = 300, voltage: Int = 4000, charging: Boolean = false) {
        if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) return
        
        ShizukuUtils.executeCommand("dumpsys battery set level $level")
        ShizukuUtils.executeCommand("dumpsys battery set temp $temp")
        ShizukuUtils.executeCommand("dumpsys battery set voltage $voltage")
        ShizukuUtils.executeCommand("dumpsys battery set status ${if (charging) 2 else 3}")
    }

    suspend fun resetBattery() {
        if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) return
        ShizukuUtils.executeCommand("dumpsys battery reset")
    }

    suspend fun spoofCarrierInfo(mccMnc: String = "72405", operatorName: String = "Claro BR") {
        if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) return
        
        ShizukuUtils.executeCommand("setprop gsm.sim.operator.numeric $mccMnc")
        ShizukuUtils.executeCommand("setprop gsm.sim.operator.alpha '$operatorName'")
        ShizukuUtils.executeCommand("setprop gsm.operator.numeric $mccMnc")
        ShizukuUtils.executeCommand("setprop gsm.operator.alpha '$operatorName'")
        ShizukuUtils.executeCommand("setprop gsm.network.type LTE")
    }

    suspend fun simulateGooglePlayInstallation(userId: String, packageName: String) {
        if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) return
        
        // Simula que o app foi instalado pela Google Play Store
        ShizukuUtils.executeCommand("pm set-installer --user $userId $packageName com.android.vending")
    }

    suspend fun spoofWiFiState(macAddress: String, ssid: String = "Rede_Generica") {
        if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) return
        
        // Força configurações falsas de Wi-Fi globais
        ShizukuUtils.executeCommand("settings put global wifi_on 1")
        ShizukuUtils.executeCommand("settings put secure fake_wifi_mac '$macAddress'")
        ShizukuUtils.executeCommand("settings put secure fake_wifi_ssid '$ssid'")
    }

    suspend fun virtualizeBootTime(uptimeUptimeMillis: Long) {
        if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) return
        
        // Define variáveis globais customizadas que a engine de hooking pode ler
        ShizukuUtils.executeCommand("settings put secure fake_uptime_millis '$uptimeUptimeMillis'")
    }

    suspend fun emulateSensors(accelerometerX: Float, accelerometerY: Float, accelerometerZ: Float) {
        if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) return
        
        // Define variáveis para a engine C++ injetar no retorno do SensorManager
        ShizukuUtils.executeCommand("settings put secure fake_sensor_accel_x '$accelerometerX'")
        ShizukuUtils.executeCommand("settings put secure fake_sensor_accel_y '$accelerometerY'")
        ShizukuUtils.executeCommand("settings put secure fake_sensor_accel_z '$accelerometerZ'")
    }
}
