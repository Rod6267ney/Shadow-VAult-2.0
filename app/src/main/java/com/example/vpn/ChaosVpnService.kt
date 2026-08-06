package com.example.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class ChaosVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val region = intent?.getStringExtra("REGION") ?: "Unknown"
        val mockIp = intent?.getStringExtra("MOCK_IP") ?: "10.0.0.2"
        val allowedApps = intent?.getStringExtra("ALLOWED_APPS") ?: ""
        
        Log.d("ChaosVpn", "Starting VPN for region $region with IP $mockIp")
        setupVpn(region, mockIp, allowedApps)
        
        return START_STICKY
    }

    private fun setupVpn(region: String, mockIp: String, allowedApps: String) {
        if (vpnInterface != null) {
            vpnInterface?.close()
            vpnInterface = null
        }

        try {
            val builder = Builder()
                .addAddress(mockIp, 24)
                .addRoute("10.255.255.255", 32) 
                .setSession("ChaosVPN-$region")

            if (allowedApps.isNotBlank()) {
                val apps = allowedApps.split(",")
                for (app in apps) {
                    if (app.isNotBlank()) {
                        try {
                            builder.addAllowedApplication(app.trim())
                        } catch (e: Exception) {
                            Log.e("ChaosVpn", "App not found to allow: $app", e)
                        }
                    }
                }
            }

            vpnInterface = builder.establish()
            Log.d("ChaosVpn", "VPN Established with IP $mockIp for region $region")
        } catch (e: Exception) {
            Log.e("ChaosVpn", "Failed to establish VPN", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e("ChaosVpn", "Error closing VPN interface", e)
        }
        vpnInterface = null
    }
}
