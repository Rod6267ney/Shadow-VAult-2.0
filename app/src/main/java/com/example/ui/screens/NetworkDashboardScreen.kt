package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settings.SettingsManager
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NetworkDashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val haptic = LocalHapticFeedback.current

    val globalVpnEnabled by settingsManager.globalVpnEnabled.collectAsState(initial = false)
    val killSwitchEnabled by settingsManager.killSwitchEnabled.collectAsState(initial = false)
    val dnsLeakProtection by settingsManager.dnsLeakProtection.collectAsState(initial = true)

    val torRoutingEnabled by settingsManager.torRoutingEnabled.collectAsState(initial = false)
    val dpiBypassEnabled by settingsManager.dpiBypassEnabled.collectAsState(initial = false)
    val adBlockEnabled by settingsManager.adBlockEnabled.collectAsState(initial = false)

    val dohProvider by settingsManager.dohProvider.collectAsState(initial = "Cloudflare")

    val spoofImeiEnabled by settingsManager.spoofImeiEnabled.collectAsState(initial = false)
    val spoofSerialEnabled by settingsManager.spoofSerialEnabled.collectAsState(initial = false)
    val spoofModelEnabled by settingsManager.spoofModelEnabled.collectAsState(initial = false)
    val basebandIsolationEnabled by settingsManager.basebandIsolationEnabled.collectAsState(initial = false)

    var showKillSwitchDialog by remember { mutableStateOf(false) }
    var showDnsDialog by remember { mutableStateOf(false) }

    if (showKillSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showKillSwitchDialog = false },
            title = { Text("Aviso de Segurança", color = DangerRed) },
            text = { Text("Desativar o Kill Switch pode expor seu IP real caso a conexão caia. Deseja continuar?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) { settingsManager.setKillSwitchEnabled(false) }
                    showKillSwitchDialog = false
                }) {
                    Text("Desativar", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillSwitchDialog = false }) {
                    Text("Cancelar", color = NeonCyan)
                }
            },
            containerColor = Color.DarkGray,
            titleContentColor = DangerRed,
            textContentColor = Color.White
        )
    }

    if (showDnsDialog) {
        AlertDialog(
            onDismissRequest = { showDnsDialog = false },
            title = { Text("Aviso de Segurança", color = DangerRed) },
            text = { Text("Desativar a proteção de DNS pode vazar seu histórico de navegação. Deseja continuar?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) { settingsManager.setDnsLeakProtection(false) }
                    showDnsDialog = false
                }) {
                    Text("Desativar", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDnsDialog = false }) {
                    Text("Cancelar", color = NeonCyan)
                }
            },
            containerColor = Color.DarkGray,
            titleContentColor = DangerRed,
            textContentColor = Color.White
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "CENTRAL DE REDE & ISOLAMENTO",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SettingsSection(title = "Status da Conexão", icon = Icons.Filled.Public) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Virtual IP", color = Color.Gray, fontSize = 12.sp)
                            Text("198.51.100.24", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Ping", color = Color.Gray, fontSize = 12.sp)
                            Text("45ms", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            if (!globalVpnEnabled && !killSwitchEnabled) {
                item {
                    Surface(
                        color = DangerRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DangerRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = DangerRed)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "ALERTA: O tráfego está exposto. Habilite a VPN Global ou o Kill Switch para proteção.",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Roteamento Básico", icon = Icons.Filled.Router) {
                    SettingsToggleItem(
                        title = "Forçar VPN Global",
                        subtitle = "Todas as instâncias usarão VPN",
                        checked = globalVpnEnabled,
                        onCheckedChange = { scope.launch(Dispatchers.IO) { settingsManager.setGlobalVpnEnabled(it) } }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Kill Switch",
                        subtitle = "Bloqueia tráfego se a conexão cair",
                        checked = killSwitchEnabled,
                        onCheckedChange = {
                            if (!it) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showKillSwitchDialog = true
                            } else {
                                scope.launch(Dispatchers.IO) { settingsManager.setKillSwitchEnabled(true) }
                            }
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Criptografia Avançada", icon = Icons.Filled.Security) {
                    SettingsToggleItem(
                        title = "Roteamento Onion (Tor)",
                        subtitle = "Direciona o tráfego via rede Tor",
                        checked = torRoutingEnabled,
                        onCheckedChange = { scope.launch(Dispatchers.IO) { settingsManager.setTorRoutingEnabled(it) } }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Bypass DPI",
                        subtitle = "Ofusca pacotes contra inspeção profunda",
                        checked = dpiBypassEnabled,
                        onCheckedChange = { scope.launch(Dispatchers.IO) { settingsManager.setDpiBypassEnabled(it) } }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "AdBlock & Anti-Tracking",
                        subtitle = "Filtro em nível de rede contra rastreadores",
                        checked = adBlockEnabled,
                        onCheckedChange = { scope.launch(Dispatchers.IO) { settingsManager.setAdBlockEnabled(it) } }
                    )
                }
            }

            item {
                SettingsSection(title = "Proteção de DNS", icon = Icons.Filled.Dns) {
                    SettingsToggleItem(
                        title = "Proteção contra Vazamento",
                        subtitle = "Bloqueia consultas DNS fora do túnel",
                        checked = dnsLeakProtection,
                        onCheckedChange = {
                            if (!it) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showDnsDialog = true
                            } else {
                                scope.launch(Dispatchers.IO) { settingsManager.setDnsLeakProtection(true) }
                            }
                        }
                    )
                    SettingsDivider()
                    Text("Provedor DoH", color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
                    SettingsButtonRow(
                        options = listOf(
                            "Cloudflare" to "Cloudflare",
                            "Quad9" to "Quad9",
                            "AdGuard" to "AdGuard"
                        ),
                        selectedOption = dohProvider,
                        onSelect = { scope.launch(Dispatchers.IO) { settingsManager.setDohProvider(it) } }
                    )
                }
            }

            item {
                SettingsSection(title = "Isolamento Físico Avançado", icon = Icons.Filled.PhonelinkLock) {
                    SettingsToggleItem(
                        title = "Spoofing de IMEI",
                        subtitle = "Gera um IMEI aleatório virtual",
                        checked = spoofImeiEnabled,
                        onCheckedChange = { scope.launch(Dispatchers.IO) { settingsManager.setSpoofImeiEnabled(it) } }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Spoofing de Serial",
                        subtitle = "Ofusca o número de série do hardware",
                        checked = spoofSerialEnabled,
                        onCheckedChange = { scope.launch(Dispatchers.IO) { settingsManager.setSpoofSerialEnabled(it) } }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Spoofing de Modelo",
                        subtitle = "Dispositivo reportado genericamente",
                        checked = spoofModelEnabled,
                        onCheckedChange = { scope.launch(Dispatchers.IO) { settingsManager.setSpoofModelEnabled(it) } }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Isolamento de Baseband",
                        subtitle = "Bloqueia comunicação silenciosa do modem",
                        checked = basebandIsolationEnabled,
                        onCheckedChange = { scope.launch(Dispatchers.IO) { settingsManager.setBasebandIsolationEnabled(it) } }
                    )
                }
            }

            item {
                SettingsSection(title = "Ferramentas de Rede", icon = Icons.Filled.Build) {
                    SettingsNavigationItem(
                        title = "Traffic Dump",
                        subtitle = "Capturar logs de rede (Mock)",
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Flush Sockets",
                        subtitle = "Limpar conexões abertas (Mock)",
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                    )
                }
            }

            item {
                Button(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .height(56.dp)
                ) {
                    Icon(Icons.Filled.Autorenew, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("RENOVAR ROTA (PANIC IP)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
