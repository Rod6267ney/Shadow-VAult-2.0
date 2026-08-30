package com.example.services

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.utils.ShizukuUtils

/**
 * Motor de Firewall Granular.
 * Usa iptables via Shizuku/Root para bloquear conexões.
 */
object FirewallEngine {

    suspend fun enableFirewallForUid(context: Context, uid: Int, blockAll: Boolean = true) {
        withContext(Dispatchers.IO) {
            try {
                if (blockAll) {
                    // Bloqueia todo o tráfego do UID específico
                    val cmdOut = "iptables -A OUTPUT -m owner --uid-owner $uid -j DROP"
                    val cmdIn = "iptables -A INPUT -m owner --uid-owner $uid -j DROP"
                    
                    ShizukuUtils.executeCommand(cmdOut)
                    ShizukuUtils.executeCommand(cmdIn)
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Firewall ativado (UID $uid)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro no Firewall: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    suspend fun disableFirewallForUid(uid: Int) {
        withContext(Dispatchers.IO) {
            try {
                val cmdOut = "iptables -D OUTPUT -m owner --uid-owner $uid -j DROP"
                val cmdIn = "iptables -D INPUT -m owner --uid-owner $uid -j DROP"
                
                ShizukuUtils.executeCommand(cmdOut)
                ShizukuUtils.executeCommand(cmdIn)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
