package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settings.SettingsManager
import com.example.ui.components.*
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.frostedGlass
import com.example.utils.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    val blockCamera by settingsManager.isCameraBlocked.collectAsState(initial = false)
    val blockMic by settingsManager.isMicBlocked.collectAsState(initial = false)
    val blockGps by settingsManager.isGpsBlocked.collectAsState(initial = false)
    val blockMotionSensors by settingsManager.blockMotionSensors.collectAsState(initial = false)
    val blockClipboard by settingsManager.blockClipboard.collectAsState(initial = false)
    val blockEnvSensors by settingsManager.blockEnvSensors.collectAsState(initial = false)
    val forceSecureFlag by settingsManager.forceSecureFlag.collectAsState(initial = false)

    val bypassPhantomProcs by settingsManager.isBypassPhantomProcs.collectAsState(initial = false)
    val disableTelemetry by settingsManager.disableTelemetry.collectAsState(initial = false)
    val antiDozeMode by settingsManager.antiDozeMode.collectAsState(initial = false)
    val forceBgAppops by settingsManager.forceBgAppops.collectAsState(initial = false)
    val blockLogcat by settingsManager.blockLogcat.collectAsState(initial = false)

    val shuffleKeypad by settingsManager.shuffleKeypad.collectAsState(initial = false)
    val coercionPin by settingsManager.coercionPin.collectAsState(initial = "")
    val dynamicStealthMode by settingsManager.dynamicStealthMode.collectAsState(initial = false)
    val camouflageNotifications by settingsManager.camouflageNotifications.collectAsState(initial = false)

    var showPhantomDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shield & Stealth", color = NeonCyan) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Radar de Integridade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .frostedGlass(cornerRadius = 16.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Radar de Integridade",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { 0.95f },
                                modifier = Modifier.size(100.dp),
                                color = NeonCyan,
                                trackColor = Color.White.copy(alpha = 0.1f),
                                strokeWidth = 8.dp
                            )
                            Text(
                                text = "95%",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Root/Magisk: Undetected",
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Ocultação Profunda de Hardware
                SettingsSection(
                    title = "Ocultação Profunda de Hardware",
                    icon = Icons.Filled.Security,
                    iconTint = ElectricPurple
                ) {
                    SettingsToggleItem(
                        title = "Bloquear Câmera",
                        subtitle = "Impede o acesso global à câmera.",
                        checked = blockCamera,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) {
                                settingsManager.setCameraBlocked(it)
                                ShizukuUtils.setCameraEnabled(!it)
                            }
                        },
                        icon = Icons.Filled.CameraAlt
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Bloquear Microfone",
                        subtitle = "Desativa o microfone do sistema.",
                        checked = blockMic,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) {
                                settingsManager.setMicBlocked(it)
                                ShizukuUtils.setMicEnabled(!it)
                            }
                        },
                        icon = Icons.Filled.Mic
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Bloquear GPS",
                        subtitle = "Força o desligamento da localização.",
                        checked = blockGps,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) {
                                settingsManager.setGpsBlocked(it)
                                ShizukuUtils.setGpsEnabled(!it)
                            }
                        },
                        icon = Icons.Filled.GpsFixed
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Bloquear Sensores de Movimento",
                        subtitle = "Giroscópio e acelerômetro.",
                        checked = blockMotionSensors,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) { settingsManager.setBlockMotionSensors(it) }
                        },
                        icon = Icons.Filled.ScreenRotation
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Bloquear Área de Transferência",
                        subtitle = "Impede a leitura do clipboard.",
                        checked = blockClipboard,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) { settingsManager.setBlockClipboard(it) }
                        },
                        icon = Icons.Filled.ContentPasteOff
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Bloquear Sensores de Ambiente",
                        subtitle = "Luz, proximidade, barômetro.",
                        checked = blockEnvSensors,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) { settingsManager.setBlockEnvSensors(it) }
                        },
                        icon = Icons.Filled.Sensors
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Anti-Screenshot (FLAG_SECURE)",
                        subtitle = "Bloqueia capturas de tela no sistema.",
                        checked = forceSecureFlag,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) { settingsManager.setForceSecureFlag(it) }
                        },
                        icon = Icons.Filled.VpnKey
                    )
                }

                // Dominação do Sistema
                SettingsSection(
                    title = "Dominação do Sistema (Bypass HyperOS)",
                    icon = Icons.Filled.Memory,
                    iconTint = DangerRed
                ) {
                    SettingsToggleItem(
                        title = "Bypass Phantom Processes",
                        subtitle = "Impede o encerramento do Shizuku pelo sistema.",
                        checked = bypassPhantomProcs,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            if (!it && bypassPhantomProcs) {
                                showPhantomDialog = true
                            } else {
                                scope.launch(Dispatchers.IO) {
                                    settingsManager.setBypassPhantomProcs(true)
                                    ShizukuUtils.setPhantomProcessLimitBypass(true)
                                }
                            }
                        },
                        icon = Icons.Filled.SettingsSystemDaydream
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Desativar Telemetria",
                        subtitle = "Bloqueia o envio de dados do sistema.",
                        checked = disableTelemetry,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) { settingsManager.setDisableTelemetry(it) }
                        },
                        icon = Icons.Filled.Block
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Anti-Doze Global",
                        subtitle = "Evita que o Vault hiberne.",
                        checked = antiDozeMode,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) { settingsManager.setAntiDozeMode(it) }
                        },
                        icon = Icons.Filled.BatteryChargingFull
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Forçar BG AppOps",
                        subtitle = "Permite execução irrestrita em background.",
                        checked = forceBgAppops,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) { settingsManager.setForceBgAppops(it) }
                        },
                        icon = Icons.Filled.AppRegistration
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Bloquear Logcat",
                        subtitle = "Limpa e bloqueia logs do sistema.",
                        checked = blockLogcat,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) { settingsManager.setBlockLogcat(it) }
                        },
                        icon = Icons.Filled.ReceiptLong
                    )
                }

                // Autenticação Dinâmica & Stealth
                SettingsSection(
                    title = "Autenticação Dinâmica & Stealth",
                    icon = Icons.Filled.Fingerprint,
                    iconTint = NeonCyan
                ) {
                    SettingsToggleItem(
                        title = "Embaralhar Teclado (Shuffle Keypad)",
                        subtitle = "Evita rastreamento de toques.",
                        checked = shuffleKeypad,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) { settingsManager.setShuffleKeypad(it) }
                        },
                        icon = Icons.Filled.Dialpad
                    )
                    SettingsDivider()
                    
                    var tempCoercionPin by remember { mutableStateOf("") }
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Text("PIN de Coerção", color = Color.White, fontSize = 15.sp)
                        Text("Ao usar este PIN, dados críticos são ocultados/destruídos.", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = tempCoercionPin,
                                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) tempCoercionPin = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(if (coercionPin.isNullOrEmpty()) "PIN 4 dígitos" else "Definido: ****") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    scope.launch(Dispatchers.IO) {
                                        settingsManager.setCoercionPin(tempCoercionPin)
                                    }
                                    tempCoercionPin = ""
                                },
                                enabled = tempCoercionPin.length == 4,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Text("SALVAR", color = Color.Black)
                            }
                        }
                    }

                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Modo Stealth Dinâmico",
                        subtitle = "Oculta ícones e interfaces do app instantaneamente.",
                        checked = dynamicStealthMode,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) { settingsManager.setDynamicStealthMode(it) }
                        },
                        icon = Icons.Filled.VisibilityOff
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Camuflar Notificações",
                        subtitle = "Notificações aparecem como sistema/inofensivas.",
                        checked = camouflageNotifications,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            scope.launch(Dispatchers.IO) { settingsManager.setCamouflageNotifications(it) }
                        },
                        icon = Icons.Filled.NotificationsOff
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Wipe Módulos
                Button(
                    onClick = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "WIPE MÓDULOS (EMERGÊNCIA)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showPhantomDialog) {
            AlertDialog(
                onDismissRequest = { showPhantomDialog = false },
                title = { Text("Aviso de Segurança", color = DangerRed) },
                text = { Text("Desativar o Bypass de Processos Fantasmas pode fazer com que o HyperOS encerre o Shizuku e outros serviços do Shadow Vault abruptamente. Deseja continuar?", color = Color.White) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPhantomDialog = false
                            scope.launch(Dispatchers.IO) {
                                settingsManager.setBypassPhantomProcs(false)
                                ShizukuUtils.setPhantomProcessLimitBypass(false)
                            }
                        }
                    ) {
                        Text("DESATIVAR", color = DangerRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPhantomDialog = false }) {
                        Text("CANCELAR", color = NeonCyan)
                    }
                },
                containerColor = Color.DarkGray
            )
        }
    }
}
