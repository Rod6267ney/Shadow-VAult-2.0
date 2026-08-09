package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settings.SettingsManager
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.DangerRed
import kotlinx.coroutines.launch

data class ChaosFeatureItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "Structural" or "Control"
    val icon: ImageVector,
    val initialEnabled: Boolean = true
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChaosOsControlScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()
    
    val activeFeaturesFlow by settingsManager.chaosOsActiveFeatures.collectAsState(initial = null)
    
    val structuralFeatures = listOf(
        ChaosFeatureItem("s1", "Camada de Isolamento de Processos (Namespaces PID/IPC)", "Isolamento estrito entre instâncias para que clones não enxerguem processos de outros workspaces.", "Estruturais", Icons.Filled.Security),
        ChaosFeatureItem("s2", "Criptografia Avançada SQLCipher em Camadas", "Chaves de criptografia AES-256 isoladas por workspace.", "Estruturais", Icons.Filled.VpnKey),
        ChaosFeatureItem("s3", "Gerenciador de Memória Dinâmica por Instância", "Limitação de RAM e CPU individual por workspace.", "Estruturais", Icons.Filled.Memory),
        ChaosFeatureItem("s4", "Sistema de Auditoria de Syscalls em Tempo Real", "Monitoramento em segundo plano de ptrace, execve e sockets.", "Estruturais", Icons.Filled.Monitor),
        ChaosFeatureItem("s5", "Cache Criptografado em Memória RAM Volátil", "Dados sensíveis armazenados estritamente na RAM, limpos ao bloquear.", "Estruturais", Icons.Filled.Lock),
        ChaosFeatureItem("s6", "Interface Fluida com Renderização Acelerada", "Animações otimizadas com graphicsLayer e remember sem jank.", "Estruturais", Icons.Filled.Speed),
        ChaosFeatureItem("s7", "Motor de Injeção de Shizuku Assíncrono", "Auto-reconexão e fila de comandos resiliente ao daemon Shizuku.", "Estruturais", Icons.Filled.Sync),
        ChaosFeatureItem("s8", "Modo Furtivo (Stealth Mode) Profundo", "Ocultação completa do Chaos OS do gerenciador e Logcat.", "Estruturais", Icons.Filled.VisibilityOff),
        ChaosFeatureItem("s9", "Firewall de Camada de Enlace (iptables / pf)", "Controle granular para bloquear pacotes UDP/TCP indesejados.", "Estruturais", Icons.Filled.Public),
        ChaosFeatureItem("s10", "Gerenciador de Bateria e Doze Mode Customizado", "Impedir esgotamento de bateria por apps em segundo plano.", "Estruturais", Icons.Filled.BatteryChargingFull),
        ChaosFeatureItem("s11", "Sanitização Automática de Clipboard", "Limpeza de texto copiado após 30s ou ao alternar instâncias.", "Estruturais", Icons.Filled.ContentPaste),
        ChaosFeatureItem("s12", "Rotação Automática de Fingerprints", "Ciclos automáticos de alteração de IMEI, MAC, Serial e Android ID.", "Estruturais", Icons.Filled.Refresh),
        ChaosFeatureItem("s13", "Sandboxing de Armazenamento Avançado", "Redirecionamento de /data/data/[pkg] para partição virtual.", "Estruturais", Icons.Filled.FolderSpecial),
        ChaosFeatureItem("s14", "Proteção Contra Captura de Tela (FLAG_SECURE)", "Bloqueio absoluto contra gravadores e apps maliciosos.", "Estruturais", Icons.Filled.Block),
        ChaosFeatureItem("s15", "Backup e Restauração Criptografados (.chaos)", "Exportar workspaces inteiros em arquivos protegidos por senha.", "Estruturais", Icons.Filled.Backup),
        ChaosFeatureItem("s16", "Monitor de Tráfego de Rede por Aplicação", "Gráfico em tempo real de banda e destinos de cada clone.", "Estruturais", Icons.Filled.NetworkCheck),
        ChaosFeatureItem("s17", "Desativação de Sensores Físicos", "Bloqueio de acelerômetro, giroscópio e magnetômetro contra fingerprinting.", "Estruturais", Icons.Filled.SensorsOff),
        ChaosFeatureItem("s18", "Mecanismo Anti-Debugging e Anti-Root", "Proteção contra Frida, Ghidra e engenharia reversa.", "Estruturais", Icons.Filled.Shield),
        ChaosFeatureItem("s19", "Atualização Dinâmica de Regras via IA (Gemini)", "Análise de permissões por IA para sugerir perfis ideais.", "Estruturais", Icons.Filled.SmartToy),
        ChaosFeatureItem("s20", "Logs de Auditoria Forense Incorruptíveis", "Registro criptografado imutável de todas as ações e wipes.", "Estruturais", Icons.Filled.ReceiptLong)
    )

    val controlFunctions = listOf(
        ChaosFeatureItem("c1", "Multi-User Matrix (Gerenciador Avançado)", "Gerenciamento visual de usuários Android (user_id 0, 10, 11...).", "Controle", Icons.Filled.Group),
        ChaosFeatureItem("c2", "Panic Wipe com Destruição de Chaves", "Apaga SQLCipher, KeyStore e desinstala clones instantaneamente.", "Controle", Icons.Filled.Warning),
        ChaosFeatureItem("c3", "Persona Engine (Gerenciador de Identidades IA)", "Geração de CPFs, nomes, cartões e biografias consistentes.", "Controle", Icons.Filled.PersonSearch),
        ChaosFeatureItem("c4", "Kill Switch de Rede Absoluto", "Corta tráfego se o túnel proxy/VPN sofrer qualquer queda.", "Controle", Icons.Filled.WifiOff),
        ChaosFeatureItem("c5", "Mock Location Engine (GPS Falso Global)", "Coordenadas geográficas falsas individuais por app clonado.", "Controle", Icons.Filled.LocationOn),
        ChaosFeatureItem("c6", "Bloqueador de Câmera/Mic em Nível HAL", "Retorna frames pretos e silêncio absoluto para apps não autorizados.", "Controle", Icons.Filled.MicOff),
        ChaosFeatureItem("c7", "Proxy/SOCKS5/WireGuard Nativos", "Roteamento de tráfego individual por instância para túneis VPN.", "Controle", Icons.Filled.VpnLock),
        ChaosFeatureItem("c8", "Injetor de Permissões Automáticas em Massa", "Concessão ou bloqueio automático de permissões na instalação.", "Controle", Icons.Filled.PlaylistAddCheck),
        ChaosFeatureItem("c9", "Espelhamento de Notificações Seletivo", "Central unificada de alertas ou isolamento total por workspace.", "Controle", Icons.Filled.NotificationsActive),
        ChaosFeatureItem("c10", "PIN Duplo (Verdadeiro vs. Decoy/Isca)", "PIN falso que abre workspace inofensivo sob coação.", "Controle", Icons.Filled.Password),
        ChaosFeatureItem("c11", "Ghost Call / Bloqueio de Chamadas e SMS", "Impedir chamadas ocultas e rastreamento por SMS.", "Controle", Icons.Filled.PhoneDisabled),
        ChaosFeatureItem("c12", "Inspecionador de Pacotes APK Integrado", "Análise de permissões e trackers antes da clonagem.", "Controle", Icons.Filled.FindInPage),
        ChaosFeatureItem("c13", "Build.java Spoofing (Falsificação Profunda)", "Fingir ser Pixel 8 Pro, Galaxy S24 Ultra, etc.", "Controle", Icons.Filled.PhoneAndroid),
        ChaosFeatureItem("c14", "Gerenciador de Sessões de Apps Ativos", "Visualizar e encerrar clones rodando em segundo plano.", "Controle", Icons.Filled.ListAlt),
        ChaosFeatureItem("c15", "Gerenciador de Módulos de Privilégio", "Injeção de patches em tempo reativo sem root permanente.", "Controle", Icons.Filled.Extension),
        ChaosFeatureItem("c16", "DNS Leak Shield (Proteção de DNS)", "Forçar DoH / DoT (Cloudflare, Quad9, privado) por workspace.", "Controle", Icons.Filled.Dns),
        ChaosFeatureItem("c17", "Cofre de Credenciais e Auto-Preenchimento", "Gerenciador blindado por SQLCipher para uso nos clones.", "Controle", Icons.Filled.Key),
        ChaosFeatureItem("c18", "Varredura de Vulnerabilidades em Apps", "Detector heurístico contra acesso indevido fora do workspace.", "Controle", Icons.Filled.BugReport),
        ChaosFeatureItem("c19", "Auto-Cleaner (Agendador de Limpeza)", "Limpeza periódica de cache e arquivos temporários.", "Controle", Icons.Filled.CleaningServices),
        ChaosFeatureItem("c20", "Modo Pânico por Gestos (Sensor de Queda/Agitar)", "Gatilhos rápidos por movimento físico para travamento.", "Controle", Icons.Filled.Gesture)
    )

    val allFeatures = remember { structuralFeatures + controlFunctions }
    
    var initialized by remember { mutableStateOf(false) }
    var activeSet by remember { mutableStateOf(setOf<String>()) }
    
    LaunchedEffect(activeFeaturesFlow) {
        if (!initialized) {
            if (activeFeaturesFlow != null) {
                if (activeFeaturesFlow!!.isEmpty()) {
                    val allIds = allFeatures.map { it.id }.toSet()
                    settingsManager.setChaosOsActiveFeatures(allIds)
                    activeSet = allIds
                } else {
                    activeSet = activeFeaturesFlow!!
                }
                initialized = true
            }
        } else {
            if (activeFeaturesFlow != null) {
                activeSet = activeFeaturesFlow!!
            }
        }
    }

    var selectedCategory by remember { mutableStateOf("Estruturais") }
    var searchQuery by remember { mutableStateOf("") }
    
    var showDialog by remember { mutableStateOf(false) }
    var featureToDisable by remember { mutableStateOf<ChaosFeatureItem?>(null) }
    
    val criticalFeatures = setOf("s1", "s2", "c2", "c4")
    
    fun toggleFeature(item: ChaosFeatureItem, checked: Boolean) {
        if (!checked && item.id in criticalFeatures) {
            featureToDisable = item
            showDialog = true
        } else {
            val newSet = if (checked) activeSet + item.id else activeSet - item.id
            activeSet = newSet
            scope.launch { settingsManager.setChaosOsActiveFeatures(newSet) }
        }
    }

    fun confirmDisable() {
        featureToDisable?.let { item ->
            val newSet = activeSet - item.id
            activeSet = newSet
            scope.launch { settingsManager.setChaosOsActiveFeatures(newSet) }
        }
        showDialog = false
        featureToDisable = null
    }

    val currentList = remember(selectedCategory, searchQuery) {
        val list = if (selectedCategory == "Estruturais") structuralFeatures else controlFunctions
        if (searchQuery.isBlank()) list else list.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
        }
    }
    
    val allCurrentActive = currentList.isNotEmpty() && currentList.all { it.id in activeSet }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Aviso de Segurança", color = DangerRed) },
            text = { Text("Desativar '${featureToDisable?.title}' pode comprometer seriamente a segurança do Vault. Deseja continuar?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = { confirmDisable() }) {
                    Text("Desativar", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar", color = NeonCyan)
                }
            },
            containerColor = Color.DarkGray,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "MATRIZ DE CONTROLE",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Shielding: ${activeSet.size}/40 Active",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (activeSet.size == 40) NeonCyan else Color.Yellow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar funções...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = NeonCyan) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { selectedCategory = "Estruturais" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedCategory == "Estruturais") NeonCyan else Color.DarkGray
                    )
                ) {
                    Text(
                        "Estruturais",
                        color = if (selectedCategory == "Estruturais") Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = { selectedCategory = "Controle" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedCategory == "Controle") ElectricPurple else Color.DarkGray
                    )
                ) {
                    Text(
                        "Controle",
                        color = if (selectedCategory == "Controle") Color.White else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Perfis Rápidos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { 
                        val newSet = allFeatures.map { it.id }.toSet()
                        activeSet = newSet
                        scope.launch { settingsManager.setChaosOsActiveFeatures(newSet) }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed)
                ) { Text("Paranoia", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = { 
                        val newSet = allFeatures.filter { it.id !in setOf("s4", "s5", "s10", "s16", "c12", "c18") }.map { it.id }.toSet()
                        activeSet = newSet
                        scope.launch { settingsManager.setChaosOsActiveFeatures(newSet) }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricPurple),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricPurple)
                ) { Text("Equilíbrio", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = { 
                        val newSet = setOf("s1", "s2", "s6", "c4", "c7", "c10")
                        activeSet = newSet
                        scope.launch { settingsManager.setChaosOsActiveFeatures(newSet) }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                ) { Text("Performance", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }

            // Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ativar todos na aba atual",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = allCurrentActive,
                    onCheckedChange = { enableAll ->
                        val currentListIds = currentList.map { it.id }
                        val newSet = if (enableAll) {
                            activeSet + currentListIds
                        } else {
                            activeSet - currentListIds.filter { it !in criticalFeatures }.toSet()
                        }
                        activeSet = newSet
                        scope.launch { settingsManager.setChaosOsActiveFeatures(newSet) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (selectedCategory == "Estruturais") NeonCyan else ElectricPurple,
                        checkedTrackColor = (if (selectedCategory == "Estruturais") NeonCyan else ElectricPurple).copy(alpha = 0.3f)
                    )
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentList, key = { it.id }) { item ->
                    val isChecked = item.id in activeSet
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().animateItemPlacement()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (selectedCategory == "Estruturais") NeonCyan else ElectricPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.description,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    toggleFeature(item, checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = if (selectedCategory == "Estruturais") NeonCyan else ElectricPurple,
                                    checkedTrackColor = (if (selectedCategory == "Estruturais") NeonCyan else ElectricPurple).copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }
            
            // Rodapé de Ações
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { 
                        // Mock do log
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chaos Logs", color = Color.White, fontSize = 12.sp)
                }
                Button(
                    onClick = { 
                        val newSet = allFeatures.map { it.id }.toSet()
                        activeSet = newSet
                        scope.launch { settingsManager.setChaosOsActiveFeatures(newSet) }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.8f))
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Factory Reset", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}
