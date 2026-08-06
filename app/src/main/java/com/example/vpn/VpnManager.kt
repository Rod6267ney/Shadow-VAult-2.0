package com.example.vpn

import android.content.Context
import com.example.utils.ShizukuUtils

object VpnManager {
    suspend fun enableWorkspaceVpn(context: Context, userId: String, region: String) {
        if (userId.startsWith("v_")) return
        
        val pkg = context.packageName
        
        ShizukuUtils.executeCommand("cmd package install-existing --user $userId $pkg")
        ShizukuUtils.executeCommand("appops set --user $userId $pkg 47 allow")
        ShizukuUtils.executeCommand("appops set --user $userId $pkg ACTIVATE_VPN allow")
        ShizukuUtils.executeCommand("appops set --user $userId $pkg ACTIVATE_PLATFORM_VPN allow")
        
        ShizukuUtils.executeCommand("settings put --user $userId secure always_on_vpn_app $pkg")
        ShizukuUtils.executeCommand("settings put --user $userId secure always_on_vpn_lockdown 0")
        
        val mockIp = generateMockIpForRegion(region)
        
        ShizukuUtils.executeCommand("settings put --user $userId secure chaos_proxy_ip '$mockIp'")
        
        val packagesStr = ShizukuUtils.executeCommand("pm list packages --user $userId -3")
        val allowedApps = packagesStr.lines()
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }
            .filter { it != pkg && it.isNotBlank() }
            .joinToString(",")
            
        ShizukuUtils.executeCommand("am start-service --user $userId -n $pkg/com.example.vpn.ChaosVpnService --es REGION '$region' --es MOCK_IP '$mockIp' --es ALLOWED_APPS '$allowedApps'")
    }
    
    suspend fun disableWorkspaceVpn(context: Context, userId: String) {
        if (userId.startsWith("v_")) return
        val pkg = context.packageName
        ShizukuUtils.executeCommand("settings delete --user $userId secure always_on_vpn_app")
        ShizukuUtils.executeCommand("settings delete --user $userId secure always_on_vpn_lockdown")
        ShizukuUtils.executeCommand("settings put --user $userId secure chaos_proxy_ip 'Oculto'")
        ShizukuUtils.executeCommand("am force-stop --user $userId $pkg")
    }

    private fun generateMockIpForRegion(region: String): String {
        return when {
            region.contains("US") -> "198.51.100.${(10..250).random()}"
            region.contains("UK") -> "203.0.113.${(10..250).random()}"
            region.contains("BR") -> "177.10.${(1..250).random()}.${(1..250).random()}"
            region.contains("JP") -> "114.114.${(1..250).random()}.${(1..250).random()}"
            region.contains("DE") -> "46.46.${(1..250).random()}.${(1..250).random()}"
            else -> "10.0.0.${(2..250).random()}"
        }
    }
}
