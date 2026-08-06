package com.example.ui.screens

import kotlinx.coroutines.Dispatchers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.settings.SettingsManager
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.DangerRed
import com.example.ui.theme.VaultBackground

@Composable
fun NetworkDashboardScreen() {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    
    val globalVpnEnabled by settingsManager.globalVpnEnabled.collectAsState(initial = false)
    val globalProxyRegion by settingsManager.globalProxyRegion.collectAsState(initial = "US - Nova York")
    val killSwitchEnabled by settingsManager.killSwitchEnabled.collectAsState(initial = false)
    val dnsLeakProtection by settingsManager.dnsLeakProtection.collectAsState(initial = true)
    val randomizeMac by settingsManager.randomizeMac.collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "CENTRAL DE REDE & ISOLAMENTO",
            style = MaterialTheme.typography.titleLarge,
            color = NeonCyan,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            "Gerencie as configurações globais de proxy, VPN e ofuscação de rede para todas as instâncias do Chaos OS.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )

        // Roteamento Global Card
        DashboardCard(title = "Roteamento Global", icon = Icons.Filled.Public) {
            SettingSwitchRow(
                title = "Forçar VPN Global",
                subtitle = "Todas as novas instâncias usarão a VPN por padrão.",
                checked = globalVpnEnabled,
                onCheckedChange = { scope.launch(Dispatchers.IO) { settingsManager.setGlobalVpnEnabled(it) } }
            )
            
            if (globalVpnEnabled) {
                Text("Região Padrão:", color = Color.White, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                val regions = listOf("US - Nova York", "US - Miami", "BR - São Paulo", "UK - Londres", "JP - Tóquio")
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(regions.size) { index ->
                        val region = regions[index]
                        Box(
                            modifier = Modifier
                                .background(if (globalProxyRegion == region) NeonCyan.copy(alpha=0.2f) else Color.DarkGray, RoundedCornerShape(8.dp))
                                .border(1.dp, if (globalProxyRegion == region) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { scope.launch(Dispatchers.IO) { settingsManager.setGlobalProxyRegion(region) } }
                                .padding(8.dp)
                        ) {
                            Text(region, color = if (globalProxyRegion == region) NeonCyan else Color.White)
                        }
                    }
                }
            }
        }
        
        // Segurança de Rede Card
        DashboardCard(title = "Segurança de Rede", icon = Icons.Filled.Security) {
            SettingSwitchRow(
                title = "Kill Switch",
                subtitle = "Bloqueia a internet da instância se a conexão com o proxy cair.",
                checked = killSwitchEnabled,
                onCheckedChange = { scope.launch(Dispatchers.IO) { settingsManager.setKillSwitchEnabled(it) } }
            )
            SettingSwitchRow(
                title = "Proteção contra vazamento de DNS",
                subtitle = "Força as instâncias a usarem servidores DNS privados do Chaos OS.",
                checked = dnsLeakProtection,
                onCheckedChange = { scope.launch(Dispatchers.IO) { settingsManager.setDnsLeakProtection(it) } }
            )
        }
        
        // Isolamento de Hardware de Rede
        DashboardCard(title = "Isolamento Físico", icon = Icons.Filled.Router) {
             SettingSwitchRow(
                title = "Randomizar MAC Address",
                subtitle = "Altera o endereço MAC virtual para cada nova instância criada.",
                checked = randomizeMac,
                onCheckedChange = { scope.launch(Dispatchers.IO) { settingsManager.setRandomizeMac(it) } }
            )
        }
    }
}

@Composable
fun DashboardCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        content()
    }
}

@Composable
fun SettingSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonCyan.copy(alpha = 0.3f)
            )
        )
    }
}
