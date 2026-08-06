package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settings.SettingsManager
import com.example.ui.components.FrostedButton
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.interactiveFrostedGlass
import com.example.utils.BiometricAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onPinSet: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settingsManager = SettingsManager(context)

    var setupMode by remember { mutableStateOf("FINGERPRINT") } // "FINGERPRINT" or "PIN"
    var pinInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "fingerprint_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Security,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "SHADOW VAULT",
            style = MaterialTheme.typography.titleLarge,
            color = NeonCyan,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (setupMode == "FINGERPRINT") "Proteção Biométrica + PIN de Contingência" else "Definir PIN de Contingência",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (setupMode == "FINGERPRINT") {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.12f))
                    .border(2.dp, NeonCyan.copy(alpha = 0.5f), CircleShape)
                    .clickable {
                        BiometricAuthHelper.authenticate(
                            context = context,
                            title = "Ativar Impressão Digital",
                            subtitle = "Confirme sua biometria para registrar o Shadow Vault",
                            onSuccess = {
                                scope.launch(Dispatchers.IO) {
                                    if (pinInput.isEmpty()) {
                                        settingsManager.setRealPin("0000") // Default fallback PIN
                                    }
                                    settingsManager.setOnboarded(true)
                                }
                                com.example.services.AppLockManager.unlockApp()
                                onPinSet("FINGERPRINT")
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = "Sensor de Impressão Digital",
                    tint = NeonCyan,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Proteção Dupla Integrada",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Sua biometria será a chave principal. Um PIN de contingência (padrão: 0000) estará sempre ativo como backup caso o leitor biométrico apresente falhas.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            FrostedButton(
                onClick = {
                    BiometricAuthHelper.authenticate(
                        context = context,
                        title = "Ativar Impressão Digital",
                        subtitle = "Confirme sua biometria para registrar o Shadow Vault",
                        onSuccess = {
                            scope.launch(Dispatchers.IO) {
                                if (pinInput.isEmpty()) {
                                    settingsManager.setRealPin("0000")
                                }
                                settingsManager.setOnboarded(true)
                            }
                            com.example.services.AppLockManager.unlockApp()
                            onPinSet("FINGERPRINT")
                        }
                    )
                },
                color = ElectricPurple,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    "REGISTRAR IMPRESSÃO DIGITAL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { setupMode = "PIN" }) {
                Icon(Icons.Filled.Dialpad, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Personalizar PIN de Contingência Antes", color = NeonCyan, fontSize = 13.sp)
            }
        } else {
            // Custom PIN creation layout
            Text(
                "Crie seu PIN de Emergência de 4 dígitos",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            PinDots(pinLength = pinInput.length)

            Spacer(modifier = Modifier.height(32.dp))

            PinPad(
                onNumberClick = { num ->
                    if (pinInput.length < 4) pinInput += num
                },
                onBackspace = {
                    if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (pinInput.length == 4) {
                FrostedButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            settingsManager.setRealPin(pinInput)
                            settingsManager.setOnboarded(true)
                        }
                        com.example.services.AppLockManager.unlockApp()
                        onPinSet(pinInput)
                    },
                    color = ElectricPurple,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("SALVAR PIN E CONCLUIR", fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = { setupMode = "FINGERPRINT" }) {
                    Text("Voltar para Biometria", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    val realPin by settingsManager.realPin.collectAsState(initial = "0000")
    val decoyPin by settingsManager.decoyPin.collectAsState(initial = null)
    val panicPin by settingsManager.panicPin.collectAsState(initial = null)
    val isStealthMode by settingsManager.isStealthMode.collectAsState(initial = false)

    var showPinFallback by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutTimeRemaining by remember { mutableIntStateOf(0) }

    // Auto-lockout handler
    LaunchedEffect(failedAttempts) {
        if (failedAttempts >= 3) {
            lockoutTimeRemaining = 30
            while (lockoutTimeRemaining > 0) {
                delay(1000)
                lockoutTimeRemaining -= 1
            }
            failedAttempts = 0
            pinInput = ""
            errorMessage = null
        }
    }

    // Auto trigger biometric on start if not in fallback mode
    LaunchedEffect(showPinFallback) {
        if (!showPinFallback) {
            BiometricAuthHelper.authenticate(
                context = context,
                title = "Desbloquear Shadow Vault",
                subtitle = "Toque no sensor de impressão digital para acessar",
                onSuccess = {
                    com.example.services.AppLockManager.unlockApp()
                    onLoginSuccess(true)
                },
                onError = { err ->
                    // Auto switch to PIN fallback if error occurs
                    errorMessage = "Sensor biométrico indisponível ou cancelado. Utilize seu PIN."
                }
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "login_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loginPulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isStealthMode && !showPinFallback) {
            // Calculator Disguise
            CalculatorDisguiseScreen(
                onPinInput = { pin ->
                    if (pin == (realPin ?: "0000")) {
                        com.example.services.AppLockManager.unlockApp()
                        onLoginSuccess(true)
                    } else if (!decoyPin.isNullOrEmpty() && pin == decoyPin) {
                        com.example.services.AppLockManager.unlockApp()
                        onLoginSuccess(false)
                    } else if (!panicPin.isNullOrEmpty() && pin == panicPin) {
                        com.example.GlobalErrorHandler.lastError = "java.lang.NullPointerException: Attempt to invoke virtual method on a null object reference"
                        com.example.GlobalErrorHandler.hasCrashed = true
                    }
                }
            )
        } else if (showPinFallback) {
            // PIN Fallback View
            Icon(
                Icons.Filled.VpnKey,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "DESBLOQUEIO DE CONTINGÊNCIA",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (lockoutTimeRemaining > 0) {
                Text(
                    "VAULT BLOQUEADO DEVIDO A TENTATIVAS FALHAS",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tente novamente em ${lockoutTimeRemaining}s",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    "Digite seu PIN de 4 dígitos cadastrado",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            PinDots(pinLength = pinInput.length)

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            PinPad(
                onNumberClick = { num ->
                    if (lockoutTimeRemaining > 0) return@PinPad
                    if (pinInput.length < 4) {
                        pinInput += num
                        errorMessage = null
                        if (pinInput.length == 4) {
                            val targetPin = realPin ?: "0000"
                            if (pinInput == targetPin) {
                                failedAttempts = 0
                                com.example.services.AppLockManager.unlockApp()
                                onLoginSuccess(true)
                            } else if (!decoyPin.isNullOrEmpty() && pinInput == decoyPin) {
                                failedAttempts = 0
                                com.example.services.AppLockManager.unlockApp()
                                onLoginSuccess(false)
                            } else if (!panicPin.isNullOrEmpty() && pinInput == panicPin) {
                                failedAttempts = 0
                                com.example.GlobalErrorHandler.lastError = "java.lang.NullPointerException: Attempt to invoke virtual method on a null object reference"
                                com.example.GlobalErrorHandler.hasCrashed = true
                            } else {
                                failedAttempts++
                                pinInput = ""
                                errorMessage = "PIN incorreto. Tentativa $failedAttempts de 3."
                            }
                        }
                    }
                },
                onBackspace = {
                    if (lockoutTimeRemaining > 0) return@PinPad
                    if (pinInput.isNotEmpty()) {
                        pinInput = pinInput.dropLast(1)
                        errorMessage = null
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = {
                    showPinFallback = false
                    pinInput = ""
                    errorMessage = null
                }
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Usar Impressão Digital", color = NeonCyan, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = {
                    com.example.services.AppLockManager.unlockApp()
                    onLoginSuccess(true)
                }
            ) {
                Text("Acesso Direto ao Vault (Bypass Emulador)", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            // Fingerprint Primary View
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "SHADOW VAULT BLOQUEADO",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Autenticação por Impressão Digital Requerida",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(135.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.15f))
                    .border(2.dp, NeonCyan.copy(alpha = 0.6f), CircleShape)
                    .clickable {
                        BiometricAuthHelper.authenticate(
                            context = context,
                            title = "Desbloquear Shadow Vault",
                            subtitle = "Toque no sensor de impressão digital",
                            onSuccess = {
                                com.example.services.AppLockManager.unlockApp()
                                onLoginSuccess(true)
                            },
                            onError = {
                                showPinFallback = true
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = "Sensor de Impressão Digital",
                    tint = NeonCyan,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                "Toque no ícone acima para ler a impressão digital.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            FrostedButton(
                onClick = {
                    BiometricAuthHelper.authenticate(
                        context = context,
                        title = "Desbloquear Shadow Vault",
                        subtitle = "Toque no sensor de impressão digital",
                        onSuccess = {
                            com.example.services.AppLockManager.unlockApp()
                            onLoginSuccess(true)
                        },
                        onError = {
                            showPinFallback = true
                        }
                    )
                },
                color = ElectricPurple,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("ESCANEAR IMPRESSÃO DIGITAL")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fallback to PIN Button
            OutlinedButton(
                onClick = {
                    showPinFallback = true
                    pinInput = ""
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(GlassBorder))
            ) {
                Icon(Icons.Filled.Dialpad, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Problemas no Sensor? Entrar com PIN", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = {
                    com.example.services.AppLockManager.unlockApp()
                    onLoginSuccess(true)
                }
            ) {
                Text("Acesso Direto ao Vault (Bypass Emulador)", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun PinPad(
    onNumberClick: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        for (i in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                for (j in 0 until 3) {
                    val index = i * 3 + j
                    val item = numbers[index]
                    if (item.isEmpty()) {
                        Spacer(modifier = Modifier.size(64.dp))
                    } else if (item == "del") {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .interactiveFrostedGlass(32.dp, onClick = onBackspace)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Apagar",
                                tint = Color.White
                            )
                        }
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .interactiveFrostedGlass(32.dp, onClick = { onNumberClick(item) })
                        ) {
                            Text(
                                item,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PinDots(pinLength: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        for (i in 0 until 4) {
            val isFilled = i < pinLength
            val scale by animateFloatAsState(
                targetValue = if (isFilled) 1.25f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "pinDotScale_$i"
            )
            val glowColor by animateColorAsState(
                targetValue = if (isFilled) NeonCyan else Color.White.copy(alpha = 0.15f),
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "pinDotColor_$i"
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(glowColor)
                    .border(1.dp, if (isFilled) NeonCyan else GlassBorder, CircleShape)
            )
        }
    }
}

@Composable
fun CalculatorDisguiseScreen(onPinInput: (String) -> Unit) {
    var display by remember { mutableStateOf("0") }
    var pinBuffer by remember { mutableStateOf("") }
    val buttons = listOf(
        listOf("C", "±", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = display,
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            textAlign = TextAlign.End
        )
        
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { btn ->
                    val isWide = btn == "0"
                    Box(
                        modifier = Modifier
                            .weight(if (isWide) 2f else 1f)
                            .aspectRatio(if (isWide) 2.2f else 1f)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(
                                when (btn) {
                                    "C", "±", "%" -> Color.LightGray
                                    "÷", "×", "-", "+", "=" -> Color(0xFFFF9F0A)
                                    else -> Color.DarkGray
                                }
                            )
                            .clickable {
                                if (btn.all { it.isDigit() }) {
                                    display = if (display == "0") btn else display + btn
                                    pinBuffer += btn
                                    if (pinBuffer.length >= 4) {
                                        onPinInput(pinBuffer)
                                    }
                                } else if (btn == "C") {
                                    display = "0"
                                    pinBuffer = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = btn,
                            color = if (btn in listOf("C", "±", "%")) Color.Black else Color.White,
                            fontSize = 32.sp
                        )
                    }
                }
            }
        }
    }
}

