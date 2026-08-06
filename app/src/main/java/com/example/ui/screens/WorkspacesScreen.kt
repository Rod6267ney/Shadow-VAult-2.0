package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import com.example.ui.viewmodel.WorkspaceCreationState
import com.example.ui.viewmodel.WorkspaceViewModel
import androidx.core.graphics.drawable.IconCompat
import com.example.data.VaultManager
import com.example.data.WorkspaceConfig
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.utils.BiometricAuthHelper
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.frostedGlass
import com.example.ui.theme.interactiveFrostedGlass
import com.example.utils.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import com.example.ui.theme.shimmer

fun getIconFromName(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name) {
        "Work" -> Icons.Filled.Work
        "Person" -> Icons.Filled.Person
        "School" -> Icons.Filled.School
        "SportsEsports" -> Icons.Filled.SportsEsports
        "Security" -> Icons.Filled.Security
        "Code" -> Icons.Filled.Code
        "FolderSpecial" -> Icons.Filled.FolderSpecial
        else -> Icons.Filled.Domain
    }
}

@Composable
fun WorkspacesScreen(
    workspaceViewModel: WorkspaceViewModel = viewModel(),
    navController: androidx.navigation.NavController? = null,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val creationState by workspaceViewModel.creationState.collectAsState()
    val (isShizukuAvailable, hasShizukuPermission) = com.example.utils.useShizukuStatus()
    var workspaces by remember { mutableStateOf<List<WorkspaceConfig>?>(null) }
    var apps by remember { mutableStateOf<List<com.example.data.AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var creationStep by remember { mutableIntStateOf(1) }
    var newWorkspaceName by remember { mutableStateOf("") }
    var useResidentialProxy by remember { mutableStateOf(false) }
    var selectedProxyRegion by remember { mutableStateOf("US - Nova York") }
    var useUnlimitedClones by remember { mutableStateOf(false) }
    var selectedIcon by remember { mutableStateOf("Domain") }
    var selectedWorkspaceType by remember { mutableStateOf("WORK_PROFILE") }
    var selectedFolderWorkspace by remember { mutableStateOf<WorkspaceConfig?>(null) }
    
    val settingsManager = remember { com.example.settings.SettingsManager(context) }
    val globalVpnEnabled by settingsManager.globalVpnEnabled.collectAsState(initial = false)
    val globalProxyRegion by settingsManager.globalProxyRegion.collectAsState(initial = "US - Nova York")
    
    val isExpandedScreen = windowSizeClass?.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded ||
                           windowSizeClass?.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium

    fun loadWorkspaces() {
        scope.launch {
            isLoading = true
            workspaces = ShizukuUtils.getWorkspaces(context)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (isShizukuAvailable && hasShizukuPermission) {
            loadWorkspaces()
        }
    }

    if (!isShizukuAvailable || !hasShizukuPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .border(1.dp, DangerRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Icon(
                    Icons.Filled.Security,
                    contentDescription = "Shizuku Offline",
                    tint = DangerRed,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "O Shizuku é necessário para gerenciar Instâncias Chaos OS.",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (isShizukuAvailable) {
                    Button(
                        onClick = { ShizukuUtils.requestShizukuPermission() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Conceder Permissão", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Button(
                        onClick = { ShizukuUtils.requestShizukuPermission() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tentar Novamente", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        return
    }

    if (showCreateDialog) {
        Dialog(
            onDismissRequest = {
                if (creationState !is WorkspaceCreationState.Loading) {
                    showCreateDialog = false
                    workspaceViewModel.resetState()
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .frostedGlass(24.dp)
                    .padding(20.dp)
            ) {
                Column {
                    // Header Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AddModerator, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Novo Espaço de Trabalho", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Text("Etapa $creationStep de 3", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stepper Progress Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (step in 1..3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .background(
                                        if (creationStep >= step) NeonCyan else Color.White.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step 1: Configuração Básica
                    if (creationStep == 1) {
                        Text("1. CONFIGURAÇÃO DO VAULT & TIPO", color = NeonCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = newWorkspaceName,
                            onValueChange = { newWorkspaceName = it },
                            label = { Text("Nome do Vault (ex: Finanças, Pessoal)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
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

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("ARQUITETURA DE ISOLAMENTO:", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isWorkProfile = selectedWorkspaceType == "WORK_PROFILE"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedWorkspaceType = "WORK_PROFILE" },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isWorkProfile) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                                ),
                                border = BorderStroke(1.dp, if (isWorkProfile) NeonCyan else Color.White.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isWorkProfile,
                                        onClick = { selectedWorkspaceType = "WORK_PROFILE" },
                                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Perfil de Trabalho", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("[Shizuku OS]", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            "Isolamento Kernel Android (UID & FS dedicados).",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            val isVirtual = selectedWorkspaceType == "VIRTUAL_CONTAINER"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedWorkspaceType = "VIRTUAL_CONTAINER" },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isVirtual) ElectricPurple.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                                ),
                                border = BorderStroke(1.dp, if (isVirtual) ElectricPurple else Color.White.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isVirtual,
                                        onClick = { selectedWorkspaceType = "VIRTUAL_CONTAINER" },
                                        colors = RadioButtonDefaults.colors(selectedColor = ElectricPurple)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Container Virtual Leve", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("[Sem Root]", color = ElectricPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            "Sandbox interna com cofre local encriptado.",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Designação de Ícone:", color = Color.LightGray, fontSize = 11.sp)
                        val iconOptions = listOf("Domain", "Work", "Person", "School", "SportsEsports", "Security", "Code", "FolderSpecial")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            items(iconOptions) { name ->
                                Box(
                                    modifier = Modifier
                                        .background(if (selectedIcon == name) NeonCyan.copy(alpha = 0.2f) else Color.DarkGray, RoundedCornerShape(10.dp))
                                        .border(1.dp, if (selectedIcon == name) NeonCyan else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable { selectedIcon = name }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(getIconFromName(name), contentDescription = name, tint = if (selectedIcon == name) NeonCyan else Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // Step 2: Rede & VPN
                    if (creationStep == 2) {
                        Text("2. PROTEÇÃO DE REDE & MODOS DE OCULTAÇÃO", color = NeonCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = useUnlimitedClones, onCheckedChange = { useUnlimitedClones = it }, colors = CheckboxDefaults.colors(checkedColor = NeonCyan))
                                    Column {
                                        Text("Modo Oculto (Clones Ilimitados)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("Permite clonar múltiplas instâncias do mesmo app.", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = useResidentialProxy, onCheckedChange = { useResidentialProxy = it }, colors = CheckboxDefaults.colors(checkedColor = NeonCyan))
                                    Column {
                                        Text("Proxy Residencial / VPN Exclusiva", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("Roteamento de IP isolado para todas as conexões deste Vault.", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        if (useResidentialProxy) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Região do Proxy Residencial:", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            val regions = listOf("US - Nova York", "UK - Londres", "BR - São Paulo", "JP - Tóquio", "DE - Frankfurt")
                            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(regions) { region ->
                                    Box(
                                        modifier = Modifier
                                            .background(if (selectedProxyRegion == region) NeonCyan.copy(alpha = 0.2f) else Color.DarkGray, RoundedCornerShape(8.dp))
                                            .border(1.dp, if (selectedProxyRegion == region) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectedProxyRegion = region }
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(region, color = if (selectedProxyRegion == region) NeonCyan else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ElectricPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, ElectricPurple.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Security, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Cofre com Banco SQLCipher AES-256 e Isolação de Clipboard vinculados.",
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Step 3: Sintetizador de Persona Gemini AI
                    if (creationStep == 3) {
                        Text("3. GERADOR DE PERSONA SINTÉTICA (GEMINI AI)", color = NeonCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            ElectricPurple.copy(alpha = 0.2f),
                                            Color(0xFF1B0B33)
                                        )
                                    )
                                )
                                .border(1.dp, ElectricPurple.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Identidade Sintética Automática", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Ao provisionar este Vault, o Gemini AI gerará uma Persona Sintética completa (Nome, Empresa, Email, Telefone) para mascarar dados reais no ambiente clonado.",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (creationState is WorkspaceCreationState.Loading) {
                            val msg = (creationState as WorkspaceCreationState.Loading).message
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NeonCyan.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = NeonCyan,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = msg,
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dialog Actions (Back, Cancel, Next / Finish)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (creationStep > 1) {
                                    creationStep--
                                } else {
                                    workspaceViewModel.resetState()
                                    showCreateDialog = false
                                }
                            },
                            enabled = creationState !is WorkspaceCreationState.Loading
                        ) {
                            Text(if (creationStep > 1) "Voltar" else "Cancelar", color = Color.Gray)
                        }

                        if (creationStep < 3) {
                            Button(
                                onClick = { creationStep++ },
                                enabled = newWorkspaceName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Avançar", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    workspaceViewModel.provisionWorkspaceWithIdentity(
                                        workspaceName = newWorkspaceName,
                                        workspaceType = selectedWorkspaceType,
                                        iconName = selectedIcon,
                                        unlimitedClones = useUnlimitedClones,
                                        useResidentialProxy = useResidentialProxy,
                                        selectedProxyRegion = selectedProxyRegion,
                                        onComplete = {
                                            showCreateDialog = false
                                            workspaceViewModel.resetState()
                                            loadWorkspaces()
                                        }
                                    )
                                },
                                enabled = creationState !is WorkspaceCreationState.Loading && newWorkspaceName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (creationState is WorkspaceCreationState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Provisionar Vault", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = workspaces == null || workspaces!!.isEmpty(),
            transitionSpec = {
                (fadeIn(animationSpec = tween(350)) + scaleIn(initialScale = 0.94f)) togetherWith
                (fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.98f))
            },
            label = "WorkspacesListTransition"
        ) { isStateEmpty ->
            if (workspaces == null) {
                LazyRow(
                    modifier = Modifier.fillMaxSize().padding(bottom = 90.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(3) {
                        Box(
                            modifier = Modifier
                                .width(320.dp)
                                .fillMaxHeight(0.85f)
                                .clip(RoundedCornerShape(32.dp))
                                .shimmer()
                        )
                    }
                }
            } else if (isStateEmpty) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.DataSaverOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nenhuma Instância Chaos OS Ativa", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                val listState = rememberLazyListState()
                if (isExpandedScreen) {
                    Row(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            items(workspaces!!, key = { it.id }) { space ->
                                Box(modifier = Modifier.clickable { selectedFolderWorkspace = space }) {
                                    VaultCard(space, scope, context, onOpenFolder = { selectedFolderWorkspace = it }) { loadWorkspaces() }
                                }
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(16.dp)
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedFolderWorkspace != null) {
                                val currentSpace = workspaces?.find { it.id == selectedFolderWorkspace?.id } ?: selectedFolderWorkspace!!
                                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                    Text("INSPEÇÃO DE WORKSPACE", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Nome: ${currentSpace.name}", color = Color.White, style = MaterialTheme.typography.titleLarge)
                                    Text("ID (Usuário Android): ${currentSpace.id}", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                    Text("Isolamento de Rede: ${if (currentSpace.networkIsolation) "Ativo" else "Inativo"}", color = Color.White)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { selectedFolderWorkspace = currentSpace }) {
                                        Text("Abrir Pasta Compartilhada")
                                    }
                                }
                            } else {
                                Text("Selecione um Vault para inspecionar", color = Color.Gray)
                            }
                        }
                    }
                } else {
                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 90.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(workspaces!!, key = { it.id }) { space ->
                            VaultCard(space, scope, context, onOpenFolder = { selectedFolderWorkspace = it }) { loadWorkspaces() }
                        }
                    }
                }
            }
        }

        if (selectedFolderWorkspace != null && !isExpandedScreen) {
            val currentSpace = workspaces?.find { it.id == selectedFolderWorkspace?.id } ?: selectedFolderWorkspace!!
            WorkspaceFolderDialog(
                space = currentSpace,
                onDismiss = { selectedFolderWorkspace = null },
                onRefresh = { loadWorkspaces() }
            )
        }



        FloatingActionButton(
            onClick = {
                navController?.navigate("storage_dashboard")
            },
            containerColor = Color(0xFF2C2C2C),
            contentColor = Color(0xFF64B5F6),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 100.dp, bottom = 72.dp)
        ) {
            Icon(Icons.Filled.Storage, contentDescription = "Storage Dashboard")
        }

        FloatingActionButton(
            onClick = {
                workspaceViewModel.hibernateAll()
                scope.launch {
                    kotlinx.coroutines.delay(1000)
                    loadWorkspaces()
                }
            },
            containerColor = Color(0xFF2C2C2C),
            contentColor = Color(0xFFFF5252),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(32.dp)
                .padding(bottom = 72.dp)
        ) {
            Icon(Icons.Filled.PowerOff, contentDescription = "Hibernate All")
        }

        FloatingActionButton(
            onClick = {
                creationStep = 1
                showCreateDialog = true
                newWorkspaceName = ""
                selectedIcon = "Domain"
                useResidentialProxy = globalVpnEnabled
                selectedProxyRegion = globalProxyRegion
            },
            containerColor = NeonCyan,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .padding(bottom = 72.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Vault")
        }
    }
}

@Composable
fun VaultCard(
    space: WorkspaceConfig,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onOpenFolder: (WorkspaceConfig) -> Unit,
    onRefresh: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val view = LocalView.current

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Purgação de Instância", color = DangerRed) },
            text = { Text("Tem certeza que deseja destruir irreversivelmente o Vault '${space.name}' (ID: ${space.id})? Todos os dados isolados serão perdidos.", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    com.example.services.CloneManager.deleteWorkspace(context, space.id) {
                        onRefresh()
                    }
                }) { Text("Destruir", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = Color.Gray) }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    val haptic = LocalHapticFeedback.current
    var isCardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isCardVisible = true
    }
    val cardAlpha by animateFloatAsState(
        targetValue = if (isCardVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
        label = "cardAlpha"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (isCardVisible) 1f else 0.92f,
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(0.85f)
            .graphicsLayer {
                alpha = cardAlpha
                scaleX = cardScale
                scaleY = cardScale
            }
            .frostedGlass(32.dp)
            .padding(1.dp) 
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val pulseAnim = rememberInfiniteTransition(label = "pulse")
                val pulseRadius by pulseAnim.animateFloat(
                    initialValue = 0f,
                    targetValue = 40f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulseRadius"
                )
                val pulseAlpha by pulseAnim.animateFloat(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulseAlpha"
                )

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            BiometricAuthHelper.authenticate(
                                context = context,
                                title = "Acessar ${space.name}",
                                subtitle = "Autentique com biometria para abrir este Perfil de Trabalho",
                                onSuccess = { onOpenFolder(space) }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = NeonCyan.copy(alpha = pulseAlpha * 0.5f),
                            radius = pulseRadius
                        )
                        drawCircle(
                            color = ElectricPurple.copy(alpha = pulseAlpha * 0.3f),
                            radius = pulseRadius * 1.2f
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                Brush.linearGradient(listOf(ElectricPurple.copy(alpha = 0.4f), NeonCyan.copy(alpha = 0.4f))),
                                shape = CircleShape
                            )
                            .border(1.5.dp, NeonCyan.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(getIconFromName(space.iconName), contentDescription = null, tint = NeonCyan, modifier = Modifier.size(26.dp))
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val isRunning = space.status == "Running"
                    Box(
                        modifier = Modifier
                            .background(if (isRunning) Color(0xFF10B981).copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .border(1.dp, if (isRunning) Color(0xFF10B981).copy(alpha=0.3f) else DangerRed.copy(alpha=0.3f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(if (isRunning) Color(0xFF10B981) else DangerRed, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isRunning) "ACTIVE • ONLINE" else "HALTED • OFF", color = if (isRunning) Color(0xFF10B981) else DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir Workspace", tint = DangerRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.clickable { onOpenFolder(space) }) {
                Text(space.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("INSTÂNCIA ISOLADA ID-${space.id}", color = NeonCyan.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("SEGURANÇA E REDE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            VaultDetailRow(Icons.Filled.Lock, "Criptografia", "AES-256 GCM")
            VaultDetailRow(Icons.Filled.VisibilityOff, "Ocultação Ativa", if (space.unlimitedClones) "Ativado (Sem Limite)" else "Desativado")
            VaultDetailRow(Icons.Filled.VpnKey, "Proxy/VPN", if (space.proxyIp != "Oculto" && space.proxyIp.isNotBlank()) "${space.proxyRegion} (Ativo)" else "Roteamento Padrão")
            VaultDetailRow(Icons.Filled.Router, "Endereço IP", space.proxyIp)

            Spacer(modifier = Modifier.height(20.dp))
            
            Text("FINGERPRINT DO DISPOSITIVO", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            VaultDetailRow(Icons.Filled.Fingerprint, "Nome Fantasma", space.fakeName)
            VaultDetailRow(Icons.Filled.Smartphone, "Hardware", "${space.fakeDeviceBrand} ${space.fakeDeviceModel}")
            
            Spacer(modifier = Modifier.weight(1f))

            // Prominent Folder button
            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    BiometricAuthHelper.authenticate(
                        context = context,
                        title = "Acessar Clones - ${space.name}",
                        subtitle = "Confirme sua biometria para abrir este Container Virtual",
                        onSuccess = { onOpenFolder(space) }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.FolderSpecial, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ABRIR PASTA DE CLONES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isRunningState = space.status == "Running"
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { scope.launch(Dispatchers.IO) { ShizukuUtils.executeCommand(if (isRunningState) "am stop-user ${space.id}" else "am start-user ${space.id}"); withContext(Dispatchers.Main) { onRefresh() } } },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunningState) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                        contentColor = if (isRunningState) Color(0xFFF59E0B) else Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { 
                    Icon(if (isRunningState) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRunningState) "PAUSAR" else "INICIAR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        com.example.services.CloneManager.clearInstanceCacheAndTemp(context, space.id) {
                            onRefresh()
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f), contentColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) { Icon(Icons.Filled.DeleteSweep, contentDescription = "Limpar Cache e Temporários", modifier = Modifier.size(20.dp)) }

                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            if (space.proxyIp != "Oculto" && space.proxyIp.isNotBlank()) {
                                com.example.vpn.VpnManager.disableWorkspaceVpn(context, space.id)
                            } else {
                                val reg = if (space.proxyRegion != "None") space.proxyRegion else "US - Nova York"
                                ShizukuUtils.executeCommand("settings put --user ${space.id} secure chaos_proxy_region '$reg'")
                                com.example.vpn.VpnManager.enableWorkspaceVpn(context, space.id, reg)
                            }
                            withContext(Dispatchers.Main) { onRefresh() }
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6).copy(alpha = 0.15f), contentColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp)
                ) { Icon(Icons.Filled.VpnLock, contentDescription = "Toggle VPN", modifier = Modifier.size(20.dp)) }

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.15f), contentColor = DangerRed),
                    shape = RoundedCornerShape(12.dp)
                ) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Excluir Vault", modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
fun VaultDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceFolderDialog(
    space: WorkspaceConfig,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAppWizard by remember { mutableStateOf(false) }
    var showDeleteWorkspaceDialog by remember { mutableStateOf(false) }
    
    val clonesViewModel: ClonesViewModel = viewModel()
    val allClones by clonesViewModel.clones.collectAsState()
    val workspaceClones = remember(allClones, space.id) {
        allClones.filter { it.userId == space.id }
    }

    if (showDeleteWorkspaceDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteWorkspaceDialog = false },
            title = { Text("Purgação de Instância", color = DangerRed) },
            text = { Text("Tem certeza que deseja destruir irreversivelmente o Vault '${space.name}' (ID: ${space.id})? Todos os dados isolados e clones serão apagados.", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteWorkspaceDialog = false
                    com.example.services.CloneManager.deleteWorkspace(context, space.id) {
                        onRefresh()
                        onDismiss()
                    }
                }) { Text("Destruir", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWorkspaceDialog = false }) { Text("Cancelar", color = Color.Gray) }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.example.ui.theme.VaultBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(listOf(ElectricPurple.copy(alpha = 0.3f), NeonCyan.copy(alpha = 0.3f))),
                                    shape = CircleShape
                                )
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(getIconFromName(space.iconName), contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Pasta: ${space.name}", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("PERFIL DE TRABALHO ISOLADO ID-${space.id}", color = NeonCyan.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                var folderSubTab by remember { mutableIntStateOf(0) }
                val folderTabs = listOf("Apps Clonados", "Identidade AI", "Clipboard Vault", "Notas Secretas")

                ScrollableTabRow(
                    selectedTabIndex = folderSubTab,
                    containerColor = Color.Transparent,
                    contentColor = NeonCyan,
                    edgePadding = 12.dp,
                    divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
                ) {
                    folderTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = folderSubTab == index,
                            onClick = { folderSubTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (folderSubTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (folderSubTab == index) NeonCyan else Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (folderSubTab) {
                        0 -> {
                            // Apps Clonados Tab
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Synthetic Identity Banner (Gemini AI Summary)
                                var isRandomizingIdentity by remember { mutableStateOf(false) }
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    ElectricPurple.copy(alpha = 0.15f),
                                                    Color(0xFF1E1035)
                                                )
                                            )
                                        )
                                        .border(1.dp, ElectricPurple.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Badge,
                                                    contentDescription = null,
                                                    tint = ElectricPurple,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "Identidade Sintética Vinculada",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    if (!isRandomizingIdentity) {
                                                        isRandomizingIdentity = true
                                                        scope.launch {
                                                            try {
                                                                com.example.utils.IdentityUtils.generateAndAttachIdentityToWorkspace(
                                                                    context = context,
                                                                    workspaceId = space.id,
                                                                    workspaceName = space.name
                                                                )
                                                                withContext(Dispatchers.Main) {
                                                                    android.widget.Toast.makeText(
                                                                        context,
                                                                        "Nova identidade gerada via Gemini AI!",
                                                                        android.widget.Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                                onRefresh()
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            } finally {
                                                                isRandomizingIdentity = false
                                                            }
                                                        }
                                                    }
                                                },
                                                enabled = !isRandomizingIdentity,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = ElectricPurple,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(20.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                if (isRandomizingIdentity) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(14.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Filled.AutoAwesome,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Randomizar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = space.fakeName.ifBlank { "Persona Não Gerada" },
                                                color = NeonCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "• ${space.fakeCompany.ifBlank { "Consultor Anônimo" }}",
                                                color = Color.LightGray,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        
                                        Text(
                                            text = "Email: ${space.fakeEmail}  |  Tel: ${space.fakePhone}",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                // Actions Toolbar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { showAppWizard = true },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Clonar App", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                        Button(
                            onClick = { com.example.utils.ShortcutUtils.createWorkspaceShortcut(context, space.id, space.name) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Shortcut, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Green)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Criar Atalho", fontSize = 12.sp)
                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { showDeleteWorkspaceDialog = true },
                                        modifier = Modifier.height(44.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.15f), contentColor = DangerRed),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Excluir Workspace", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Excluir Pasta", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                // Security Banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Shield, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            "Os aplicativos dentro desta pasta rodam isolados no Perfil de Trabalho ID-${space.id}, com Kernel Linux e UID dedicados.",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Clones Content
                                if (workspaceClones.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Esta pasta de Workspace está vazia", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Clique no botão 'Clonar App Nesta Pasta' para criar e isolar um aplicativo dentro desta instância.",
                                                color = Color.Gray,
                                                fontSize = 13.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 24.dp)
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(bottom = 24.dp)
                                    ) {
                                        items(workspaceClones) { clone ->
                                            CloneItem(clone = clone, viewModel = clonesViewModel)
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Identidade Sintética Tab
                            IdentitiesScreen(activeInstance = space)
                        }

                        2 -> {
                            // Clipboard Vault Tab
                            ClipboardVaultScreen(activeInstance = space)
                        }

                        3 -> {
                            // Notas Secretas Tab
                            EncryptedNotesScreen(activeInstance = space)
                        }
                    }
                }
            }

            if (showAppWizard) {
                AppSelectionWizard(
                    viewModel = clonesViewModel,
                    targetWorkspaceId = space.id,
                    onDismiss = {
                        showAppWizard = false
                        onRefresh()
                    }
                )
            }
        }
    }
}
