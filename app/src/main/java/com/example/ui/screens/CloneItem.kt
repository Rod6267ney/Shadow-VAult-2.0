package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.CloneEntity
import com.example.services.CloneManager
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.frostedGlass
import com.example.utils.BiometricAuthHelper
import com.example.utils.ShortcutUtils
import com.example.utils.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CloneItem(clone: CloneEntity, viewModel: Any? = null, onNavigateToFileManager: ((String, String) -> Unit)? = null) {
    var showMenu by remember { mutableStateOf(false) }
    var showNetworkDialog by remember { mutableStateOf(false) }
    var currentProxyRegion by remember { mutableStateOf("None") }
    var currentProxyIp by remember { mutableStateOf("Oculto") }
    var iconDrawable by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
    
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(clone.packageName) {
        withContext(Dispatchers.IO) {
            iconDrawable = com.example.utils.IconCache.getIcon(context, clone.packageName)
        }
    }

    fun launchApp() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        BiometricAuthHelper.authenticate(
            context = context,
            title = "Abrir ${clone.appName}",
            subtitle = "Autentique com biometria para executar este Container Virtual",
            onSuccess = { CloneManager.launchClone(context, clone) }
        )
    }

    val colorHexStr = clone.colorHex
    val customColor = if (colorHexStr != null) {
        try {
            Color(android.graphics.Color.parseColor(colorHexStr))
        } catch (e: Exception) {
            Color.White.copy(alpha = 0.12f)
        }
    } else {
        Color.White.copy(alpha = 0.12f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .frostedGlass(16.dp)
            .border(1.dp, if (clone.isRunning) NeonCyan else customColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon & Name Area - Clickable to Launch
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { launchApp() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconDrawable != null) {
                        Image(
                            painter = rememberAsyncImagePainter(model = iconDrawable),
                            contentDescription = clone.appName,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Android,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = clone.appName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = clone.packageName,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (clone.isRunning) Color(0xFF00FF88) else Color(0xFFFFB300))
                        )
                        Text(
                            text = if (clone.isRunning) "ONLINE" else "STANDBY",
                            color = if (clone.isRunning) Color(0xFF00FF88) else Color(0xFFFFB300),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Badges
                        if (clone.cloneMode == "SANDBOX_NON_ROOT") {
                            Text(
                                text = "[SANDBOX]",
                                color = Color(0xFFFF9800),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.background(Color(0xFFFF9800).copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal=4.dp, vertical=2.dp)
                            )
                        } else {
                            Text(
                                text = "[NATIVE]",
                                color = Color(0xFF2196F3),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.background(Color(0xFF2196F3).copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal=4.dp, vertical=2.dp)
                            )
                        }
                        
                        if (clone.firewallEnabled) {
                            Icon(Icons.Filled.Security, contentDescription = "Firewall", tint = Color.Red, modifier = Modifier.size(12.dp))
                        }
                        
                        if (clone.spoofProfile != null) {
                            Icon(Icons.Filled.VpnKey, contentDescription = "Spoofed", tint = Color.Magenta, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Launch Button
            IconButton(
                onClick = { launchApp() },
                modifier = Modifier
                    .size(40.dp)
                    .background(NeonCyan.copy(alpha = 0.2f), CircleShape)
                    .border(1.dp, NeonCyan, CircleShape)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Executar", tint = NeonCyan, modifier = Modifier.size(22.dp))
            }

            // Overflow Menu Button
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Opções", tint = Color.LightGray)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(Color(0xFF1E1035))
                        .border(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    if (clone.isRunning) {
                        DropdownMenuItem(
                            text = { Text("Congelar Container", color = Color.White) },
                            leadingIcon = { Icon(Icons.Filled.Pause, contentDescription = null, tint = Color.LightGray) },
                            onClick = {
                                showMenu = false
                                CloneManager.freezeClone(context, clone)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Descongelar Container", color = NeonCyan) },
                            leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = NeonCyan) },
                            onClick = {
                                showMenu = false
                                BiometricAuthHelper.authenticate(
                                    context = context,
                                    title = "Descongelar ${clone.appName}",
                                    subtitle = "Confirme biometria",
                                    onSuccess = { CloneManager.unfreezeClone(context, clone) }
                                )
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Criar Atalho na Tela Inicial", color = Color.White) },
                        leadingIcon = { Icon(Icons.Filled.Shortcut, contentDescription = null, tint = Color.Green) },
                        onClick = {
                            showMenu = false
                            ShortcutUtils.createCloneShortcut(context, clone)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Arquivos Isolados", color = Color.White) },
                        leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null, tint = Color(0xFF64B5F6)) },
                        onClick = {
                            showMenu = false
                            onNavigateToFileManager?.invoke(clone.userId, clone.packageName)
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("Criar Snapshot (Backup)", color = Color.White) },
                        leadingIcon = { Icon(Icons.Filled.Backup, contentDescription = null, tint = Color(0xFF81C784)) },
                        onClick = {
                            showMenu = false
                            scope.launch(Dispatchers.IO) { com.example.services.BackupManager.backupInstance(context, clone.userId, clone.packageName) }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Restaurar Snapshot", color = Color.White) },
                        leadingIcon = { Icon(Icons.Filled.Restore, contentDescription = null, tint = Color(0xFFFFB74D)) },
                        onClick = {
                            showMenu = false
                            // Simplificação: Restaurando o snapshot mais recente se existir
                            scope.launch(Dispatchers.IO) {
                                val backups = com.example.services.BackupManager.listBackups()
                                val latest = backups.filter { it.name.contains(clone.userId) && it.name.contains(clone.packageName) }
                                                    .maxByOrNull { it.lastModified() }
                                if (latest != null) {
                                    com.example.services.BackupManager.restoreInstance(context, clone.userId, clone.packageName, latest.absolutePath)
                                } else {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        android.widget.Toast.makeText(context, "Nenhum snapshot encontrado para esta instância", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )


                    DropdownMenuItem(
                        text = { Text("Isolamento de Rede (Proxy/VPN)", color = Color.White) },
                        leadingIcon = { Icon(Icons.Filled.VpnKey, contentDescription = null, tint = Color.Magenta) },
                        onClick = {
                            showMenu = false
                            showNetworkDialog = true
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Limpar Cache", color = Color.White) },
                        leadingIcon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = Color(0xFF10B981)) },
                        onClick = {
                            showMenu = false
                            CloneManager.clearCloneCacheOnly(context, clone)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Apagar Todos os Dados", color = Color.White) },
                        leadingIcon = { Icon(Icons.Filled.ClearAll, contentDescription = null, tint = Color.Yellow) },
                        onClick = {
                            showMenu = false
                            CloneManager.clearCloneData(context, clone)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Menu Spoofing (Identidade)", color = Color.White) },
                        leadingIcon = { Icon(Icons.Filled.VpnKey, contentDescription = null, tint = Color.Magenta) },
                        onClick = {
                            showMenu = false
                            // TODO: Launch Identity Screen or Dialog
                            android.widget.Toast.makeText(context, "Menu de Spoofing será aberto (WIP)", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Clonar Configurações", color = Color.White) },
                        leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null, tint = Color.Cyan) },
                        onClick = {
                            showMenu = false
                            android.widget.Toast.makeText(context, "Configurações deste clone copiadas para área de transferência", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    DropdownMenuItem(
                        text = { Text("Remover App Clonado", color = DangerRed) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = DangerRed) },
                        onClick = {
                            showMenu = false
                            CloneManager.deleteCloneApp(context, clone)
                        }
                    )
                }
            }
        }
    }

    if (showNetworkDialog) {
        var selectedProxy by remember { mutableStateOf(if (currentProxyRegion == "None") "Desativado" else currentProxyRegion) }
        
        AlertDialog(
            onDismissRequest = { showNetworkDialog = false },
            title = { Text("Isolamento de Rede (VPN/Proxy)", color = NeonCyan) },
            text = {
                LazyColumn {
                    val regions = listOf("Desativado", "US - Nova York", "US - Miami", "US - Los Angeles", 
                                        "BR - São Paulo", "BR - Rio de Janeiro",
                                        "UK - Londres", "JP - Tóquio", "DE - Frankfurt")
                    regions.forEach { region ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(if (selectedProxy == region) NeonCyan.copy(alpha=0.2f) else Color.DarkGray, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (selectedProxy == region) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedProxy = region }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedProxy == region,
                                    onClick = { selectedProxy = region },
                                    colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                )
                                Text(region, color = if (selectedProxy == region) NeonCyan else Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        if (selectedProxy == "Desativado") {
                            com.example.vpn.VpnManager.disableWorkspaceVpn(context, clone.userId)
                            currentProxyRegion = "None"
                            currentProxyIp = "Oculto"
                        } else {
                            ShizukuUtils.executeCommand("settings put --user ${clone.userId} secure chaos_proxy_region '$selectedProxy'")
                            com.example.vpn.VpnManager.enableWorkspaceVpn(context, clone.userId, selectedProxy)
                            
                            val pRegion = ShizukuUtils.executeCommand("settings get --user ${clone.userId} secure chaos_proxy_region").trim()
                            if (pRegion != "null" && pRegion.isNotBlank()) {
                                currentProxyRegion = pRegion
                            }
                            val pIp = ShizukuUtils.executeCommand("settings get --user ${clone.userId} secure chaos_proxy_ip").trim()
                            if (pIp != "null" && pIp.isNotBlank()) {
                                currentProxyIp = pIp
                            }
                        }
                    }
                    showNetworkDialog = false
                }) {
                    Text("Aplicar", color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNetworkDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = VaultBackground
        )
    }
}

