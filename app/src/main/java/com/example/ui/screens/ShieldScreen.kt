package com.example.ui.screens

import kotlinx.coroutines.Dispatchers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.settings.SettingsManager
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.frostedGlass
import com.example.ui.theme.interactiveFrostedGlass
import kotlinx.coroutines.launch

@Composable
fun ShieldScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    val isStealthMode by settingsManager.isStealthMode.collectAsState(initial = false)
    val isCameraBlocked by settingsManager.isCameraBlocked.collectAsState(initial = false)
    val isMicBlocked by settingsManager.isMicBlocked.collectAsState(initial = false)
    val isGpsBlocked by settingsManager.isGpsBlocked.collectAsState(initial = false)
    val decoyPin by settingsManager.decoyPin.collectAsState(initial = "")

    val isBypassPhantomProcs by settingsManager.isBypassPhantomProcs.collectAsState(initial = false)
    val isBypassBatterySaver by settingsManager.isBypassBatterySaver.collectAsState(initial = false)
    val isBypassBgLaunches by settingsManager.isBypassBgLaunches.collectAsState(initial = false)
    val isBypassLimits by settingsManager.isBypassLimits.collectAsState(initial = false)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        
        Text("CONTROLE DE MÓDULOS E SISTEMA", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Gerencie os privilégios avançados de segurança e otimizações de barreira do HyperOS / MIUI.", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(24.dp))

        Text("SISTEMA & DYSFUNÇÃO HYPEROS / MIUI", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(8.dp))

        ShieldToggleItem(
            title = "Bypass Limits (Desbloquear HyperOS)",
            description = "Remove limitações de suspensão agressiva do HyperOS, desativa otimização do sistema e libera os limites do congelador de aplicativos cached.",
            icon = Icons.Filled.Tune,
            isChecked = isBypassLimits
        ) {
            scope.launch(Dispatchers.IO) {
                settingsManager.setBypassLimits(it)
                com.example.utils.ShizukuUtils.setHyperOSLimitsBypass(it)
            }
        }

        ShieldToggleItem(
            title = "Bypass Phantom Processes",
            description = "Remove o limite de 32 processos em segundo plano imposto pelo Android 12+/HyperOS, prevenindo fechamentos do Shizuku.",
            icon = Icons.Filled.Memory,
            isChecked = isBypassPhantomProcs
        ) {
            scope.launch(Dispatchers.IO) {
                settingsManager.setBypassPhantomProcs(it)
                com.example.utils.ShizukuUtils.setPhantomProcessLimitBypass(it)
            }
        }

        ShieldToggleItem(
            title = "Desativar Otimização de Bateria",
            description = "Insere o Vault e o Shizuku na lista branca de economia de bateria global do sistema.",
            icon = Icons.Filled.ElectricBolt,
            isChecked = isBypassBatterySaver
        ) {
            scope.launch(Dispatchers.IO) {
                settingsManager.setBypassBatterySaver(it)
                com.example.utils.ShizukuUtils.setBatterySaverBypass(context, it)
            }
        }

        ShieldToggleItem(
            title = "Liberar Inicialização em 2º Plano",
            description = "Atribui permissão 'allow' nas permissões de background appops para evitar restrição de abertura remota de clones.",
            icon = Icons.Filled.RocketLaunch,
            isChecked = isBypassBgLaunches
        ) {
            scope.launch(Dispatchers.IO) {
                settingsManager.setBypassBgLaunches(it)
                com.example.utils.ShizukuUtils.setBackgroundLaunchBypass(context, it)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("BLOQUEIO DE HARDWARE", style = MaterialTheme.typography.titleSmall, color = DangerRed)
        Spacer(modifier = Modifier.height(16.dp))
        
        ShieldToggleItem("Bloquear Câmera", null, Icons.Filled.CameraAlt, isCameraBlocked) { 
            scope.launch(Dispatchers.IO) { 
                settingsManager.setCameraBlocked(it) 
                com.example.utils.ShizukuUtils.setCameraEnabled(!it)
            } 
        }
        ShieldToggleItem("Bloquear Microfone", null, Icons.Filled.Mic, isMicBlocked) { 
            scope.launch(Dispatchers.IO) { 
                settingsManager.setMicBlocked(it) 
                com.example.utils.ShizukuUtils.setMicEnabled(!it)
            } 
        }
        ShieldToggleItem("Bloquear GPS (Localização)", null, Icons.Filled.GpsFixed, isGpsBlocked) { 
            scope.launch(Dispatchers.IO) { 
                settingsManager.setGpsBlocked(it) 
                com.example.utils.ShizukuUtils.setGpsEnabled(!it)
            } 
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("STEALTH & DISGUISE", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        
        ShieldToggleItem("Calculator Disguise", null, Icons.Filled.VisibilityOff, isStealthMode) { 
            scope.launch(Dispatchers.IO) { settingsManager.setStealthMode(it) } 
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("AUTENTICAÇÃO DO VAULT", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        val realPinState by settingsManager.realPin.collectAsState(initial = "0000")
        var newFallbackPin by remember { mutableStateOf("") }
        var pinSavedSuccess by remember { mutableStateOf(false) }

        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Impressão Digital + PIN Backup", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        Text("O leitor biométrico é a chave principal. Caso haja falhas, o PIN de contingência libera o acesso.", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = GlassBorder)
                Spacer(modifier = Modifier.height(16.dp))

                Text("PIN DE CONTINGÊNCIA (BACKUP DE SEGURANÇA)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newFallbackPin,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newFallbackPin = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (realPinState.isNullOrEmpty()) "PIN 4 dígitos" else "PIN Atual: ${realPinState ?: "0000"}") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    if (newFallbackPin.length == 4) {
                        com.example.ui.components.FrostedButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    settingsManager.setRealPin(newFallbackPin)
                                    pinSavedSuccess = true
                                }
                                newFallbackPin = ""
                            },
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text("SALVAR")
                        }
                    } else {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text("SALVAR", color = Color.Gray)
                        }
                    }
                }

                if (pinSavedSuccess) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Novo PIN de contingência salvo com sucesso!", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Use este PIN na tela de login caso o leitor de impressão digital do aparelho não funcione.", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ShieldToggleItem(
    title: String,
    description: String? = null,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .interactiveFrostedGlass(16.dp, onClick = { onCheckedChange(!isChecked) })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = if (isChecked) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = isChecked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha=0.3f)
            )
        )
    }
}
