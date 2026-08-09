package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.SettingsSection
import com.example.ui.components.SettingsToggleItem
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.frostedGlass
import com.example.ui.viewmodel.WorkspaceCreationState
import com.example.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WorkspaceCreationWizard(
    workspaceViewModel: WorkspaceViewModel,
    globalVpnEnabled: Boolean,
    globalProxyRegion: String,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val creationState by workspaceViewModel.creationState.collectAsState()
    val view = LocalView.current
    var step by remember { mutableIntStateOf(1) }

    // Step 1 State
    var workspaceName by remember { mutableStateOf("") }
    var selectedWorkspaceType by remember { mutableStateOf("WORK_PROFILE") }
    var selectedIcon by remember { mutableStateOf("Domain") }

    // Step 2 State
    var useUnlimitedClones by remember { mutableStateOf(false) }
    var useResidentialProxy by remember { mutableStateOf(globalVpnEnabled) }
    var selectedProxyRegion by remember { mutableStateOf(globalProxyRegion) }
    var useKillSwitch by remember { mutableStateOf(false) }
    var useHardwareSpoofing by remember { mutableStateOf(false) }
    var useFakeGps by remember { mutableStateOf(false) }

    // Step 3 State
    var archetype by remember { mutableStateOf("Anônimo") }
    var generateComplexPasswords by remember { mutableStateOf(false) }

    // Step 4 State
    var burnerModeEnabled by remember { mutableStateOf(false) }
    var confirmBurnerMode by remember { mutableStateOf(false) }

    val hapticFeedback = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }

    Dialog(
        onDismissRequest = {
            if (creationState !is WorkspaceCreationState.Loading) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .frostedGlass(24.dp)
                    .padding(20.dp)
            ) {
                // Header Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AddModerator, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Novo Espaço Shadow", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text("Etapa $step de 4", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Holographic Animated Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..4) {
                        val isActive = step >= i
                        val color = if (isActive) NeonCyan else Color.White.copy(alpha = 0.1f)
                        val animWidth by animateFloatAsState(if (step == i) 1.5f else 1f)
                        Box(
                            modifier = Modifier
                                .weight(animWidth)
                                .height(6.dp)
                                .background(color, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Content Area with AnimatedContent
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }
                        },
                        label = "WizardStepTransition"
                    ) { currentStep ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            when (currentStep) {
                                1 -> {
                                    // Fase 1 & 2: Arquitetura Visual e Isolamento
                                    Text("1. CONFIGURAÇÃO BASE & ARQUITETURA", color = NeonCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = workspaceName,
                                            onValueChange = { workspaceName = it },
                                            label = { Text("Nome do Vault (ex: Operação Fênix)", color = Color.Gray) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                                unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedIndicatorColor = NeonCyan,
                                                unfocusedIndicatorColor = Color.DarkGray
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                hapticFeedback()
                                                val prefixes = listOf("Operação", "Projeto", "Vault", "Sombra")
                                                val suffixes = listOf("Fênix", "Nexus", "Zero", "Fantasma")
                                                workspaceName = "${prefixes.random()} ${suffixes.random()}"
                                            },
                                            modifier = Modifier.background(NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Filled.AutoFixHigh, contentDescription = "Gerador", tint = NeonCyan)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("ISOLAMENTO (HARDWARE-BACKED)", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        val isWorkProfile = selectedWorkspaceType == "WORK_PROFILE"
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { hapticFeedback(); selectedWorkspaceType = "WORK_PROFILE" },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = if (isWorkProfile) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                                            border = BorderStroke(1.dp, if (isWorkProfile) NeonCyan else Color.White.copy(alpha = 0.12f))
                                        ) {
                                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = isWorkProfile, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = NeonCyan))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Isolamento Nível Kernel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Badge(containerColor = DangerRed) { Text("Root Necessário", color = Color.White, fontSize = 9.sp) }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Criação de Perfil de Trabalho via Shizuku. UID e File System completamente separados.", color = Color.Gray, fontSize = 12.sp)
                                                }
                                            }
                                        }

                                        val isVirtual = selectedWorkspaceType == "VIRTUAL_CONTAINER"
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { hapticFeedback(); selectedWorkspaceType = "VIRTUAL_CONTAINER" },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = if (isVirtual) ElectricPurple.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
                                            border = BorderStroke(1.dp, if (isVirtual) ElectricPurple else Color.White.copy(alpha = 0.12f))
                                        ) {
                                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = isVirtual, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = ElectricPurple))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Sandbox App-Level", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Badge(containerColor = NeonCyan) { Text("Furtividade Máxima", color = Color.Black, fontSize = 9.sp) }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Emulação dentro do próprio app. Leve, porém não isola completamente do OS.", color = Color.Gray, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("ÍCONE E CLASSIFICAÇÃO", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val iconOptions = listOf("Domain", "Work", "Person", "School", "SportsEsports", "Security", "Code", "FolderSpecial")
                                    // Grid of Icons
                                    val columns = 4
                                    Column {
                                        for (i in iconOptions.indices step columns) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                                for (j in 0 until columns) {
                                                    if (i + j < iconOptions.size) {
                                                        val name = iconOptions[i + j]
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .aspectRatio(1f)
                                                                .background(if (selectedIcon == name) NeonCyan.copy(alpha = 0.2f) else Color.DarkGray, RoundedCornerShape(12.dp))
                                                                .border(1.dp, if (selectedIcon == name) NeonCyan else Color.Transparent, RoundedCornerShape(12.dp))
                                                                .clickable { hapticFeedback(); selectedIcon = name },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(getIconFromName(name), contentDescription = name, tint = if (selectedIcon == name) NeonCyan else Color.White, modifier = Modifier.size(28.dp))
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    // Fase 3: Rede e Ocultação Avançada
                                    Text("2. REDE, OCULTAÇÃO E SPOOFING", color = NeonCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    SettingsSection("Conectividade e Roteamento", icon = Icons.Filled.Wifi) {
                                        SettingsToggleItem(
                                            title = "VPN Dedicada / Rede Tor",
                                            subtitle = "Roteia todo o tráfego deste espaço de forma isolada.",
                                            icon = Icons.Filled.VpnKey,
                                            checked = useResidentialProxy,
                                            onCheckedChange = { hapticFeedback(); useResidentialProxy = it }
                                        )

                                        if (useResidentialProxy) {
                                            SettingsToggleItem(
                                                title = "Kill-Switch de Rede",
                                                subtitle = "Bloqueio total (drop packets) se a VPN cair.",
                                                icon = Icons.Filled.SignalCellularConnectedNoInternet0Bar,
                                                checked = useKillSwitch,
                                                onCheckedChange = { hapticFeedback(); useKillSwitch = it }
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Nodo de Saída VPN / Proxy:", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val regions = listOf("US - Nova York", "UK - Londres", "BR - São Paulo", "RU - Moscou", "CH - Zurique")
                                            LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(regions) { region ->
                                                    Box(
                                                        modifier = Modifier
                                                            .background(if (selectedProxyRegion == region) NeonCyan.copy(alpha = 0.2f) else Color.DarkGray, RoundedCornerShape(8.dp))
                                                            .border(1.dp, if (selectedProxyRegion == region) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                            .clickable { hapticFeedback(); selectedProxyRegion = region }
                                                            .padding(10.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(region, color = if (selectedProxyRegion == region) NeonCyan else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }

                                    SettingsSection("Ocultação de Hardware & Ambiente", icon = Icons.Filled.Devices) {
                                        SettingsToggleItem(
                                            title = "Spoofing de Hardware",
                                            subtitle = "Mascarar IMEI, MAC Address e Build.prop (Requer Xposed/Shizuku).",
                                            icon = Icons.Filled.Fingerprint,
                                            checked = useHardwareSpoofing,
                                            onCheckedChange = { hapticFeedback(); useHardwareSpoofing = it }
                                        )

                                        SettingsToggleItem(
                                            title = "Modo Oculto (Clones Ilimitados)",
                                            subtitle = "Permite instalar apps já existentes sem conflito de assinatura.",
                                            icon = Icons.Filled.ContentCopy,
                                            checked = useUnlimitedClones,
                                            onCheckedChange = { hapticFeedback(); useUnlimitedClones = it }
                                        )

                                        SettingsToggleItem(
                                            title = "Fake GPS Associado",
                                            subtitle = "Sincroniza automaticamente o GPS fake com a região do Proxy/VPN.",
                                            icon = Icons.Filled.LocationOn,
                                            checked = useFakeGps,
                                            onCheckedChange = { hapticFeedback(); useFakeGps = it }
                                        )
                                    }
                                }
                                3 -> {
                                    // Fase 4: Inteligência Sintética (Gemini AI)
                                    Text("3. SINTETIZADOR DE IDENTIDADE", color = NeonCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    SettingsSection("Persona Sintética via IA", icon = Icons.Filled.AutoAwesome) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Arquétipo da Identidade", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("O Gemini AI criará nomes, e-mails e profissões com base no perfil escolhido.", color = Color.Gray, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            val archetypes = listOf("Jornalista Investigativo", "CEO / Executivo", "Fantasma / Anônimo", "Gamer / Streamer")
                                            archetypes.forEach { arch ->
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { hapticFeedback(); archetype = arch }.padding(vertical = 6.dp)) {
                                                    RadioButton(selected = archetype == arch, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = ElectricPurple))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(arch, color = Color.White, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                        SettingsToggleItem(
                                            title = "Geração de Senhas Complexas",
                                            subtitle = "Cria senhas automáticas e preenche via AutoFill Biométrico.",
                                            icon = Icons.Filled.Password,
                                            checked = generateComplexPasswords,
                                            onCheckedChange = { hapticFeedback(); generateComplexPasswords = it }
                                        )
                                    }
                                }
                                4 -> {
                                    // Fase 5: Revisão e Segurança
                                    if (creationState is WorkspaceCreationState.Loading) {
                                        // Loading Terminal Hacker
                                        val msg = (creationState as WorkspaceCreationState.Loading).message
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Filled.Terminal, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(64.dp))
                                            Spacer(modifier = Modifier.height(24.dp))
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NeonCyan, trackColor = Color.DarkGray)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(msg, color = NeonCyan, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                                        }
                                    } else {
                                        Text("4. REVISÃO DE MISSÃO", color = NeonCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text("Nome do Vault:", color = Color.Gray, fontSize = 12.sp)
                                                Text(workspaceName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Arquitetura:", color = Color.Gray, fontSize = 12.sp)
                                                Text(if (selectedWorkspaceType == "WORK_PROFILE") "Isolamento Nível Kernel" else "Sandbox Virtual", color = NeonCyan, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Persona AI:", color = Color.Gray, fontSize = 12.sp)
                                                Text(archetype, color = ElectricPurple, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Módulos Ativos:", color = Color.Gray, fontSize = 12.sp)
                                                val modules = mutableListOf<String>()
                                                if (useResidentialProxy) modules.add("VPN ($selectedProxyRegion)")
                                                if (useKillSwitch) modules.add("Kill-Switch")
                                                if (useHardwareSpoofing) modules.add("Spoofing")
                                                if (useFakeGps) modules.add("Fake GPS")
                                                Text(if (modules.isEmpty()) "Nenhum" else modules.joinToString(", "), color = Color.White, fontSize = 13.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))
                                        SettingsToggleItem(
                                            title = "Modo Burner (Autodestruição)",
                                            subtitle = "Ativa botão de pânico para obliterar instantaneamente o Vault.",
                                            icon = Icons.Filled.LocalFireDepartment,
                                            checked = burnerModeEnabled,
                                            onCheckedChange = { hapticFeedback(); burnerModeEnabled = it },
                                            iconTint = DangerRed
                                        )

                                        if (burnerModeEnabled) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(DangerRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .border(1.dp, DangerRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(checked = confirmBurnerMode, onCheckedChange = { hapticFeedback(); confirmBurnerMode = it }, colors = CheckboxDefaults.colors(checkedColor = DangerRed))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Confirmo os riscos do Modo Burner. Os dados serão apagados sem aviso ao acionar o Kill-Switch.", color = Color.White, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer Actions
                if (creationState !is WorkspaceCreationState.Loading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                hapticFeedback()
                                if (step > 1) step-- else onDismiss()
                            }
                        ) {
                            Text(if (step > 1) "Voltar" else "Cancelar", color = Color.Gray)
                        }

                        Button(
                            onClick = {
                                hapticFeedback()
                                if (step < 4) {
                                    step++
                                } else {
                                    workspaceViewModel.provisionWorkspaceWithIdentity(
                                        workspaceName = workspaceName,
                                        workspaceType = selectedWorkspaceType,
                                        iconName = selectedIcon,
                                        unlimitedClones = useUnlimitedClones,
                                        useResidentialProxy = useResidentialProxy,
                                        selectedProxyRegion = selectedProxyRegion,
                                        killSwitchEnabled = useKillSwitch,
                                        hardwareSpoofingEnabled = useHardwareSpoofing,
                                        fakeGpsRegion = if (useFakeGps) selectedProxyRegion else "",
                                        personaArchetype = archetype,
                                        generateComplexPasswords = generateComplexPasswords,
                                        burnerModeEnabled = burnerModeEnabled && confirmBurnerMode,
                                        onComplete = onComplete
                                    )
                                }
                            },
                            enabled = when (step) {
                                1 -> workspaceName.isNotBlank()
                                4 -> !burnerModeEnabled || confirmBurnerMode
                                else -> true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (step == 4) "Provisionar Vault" else "Avançar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
