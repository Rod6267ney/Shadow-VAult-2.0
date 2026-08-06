package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.AppState
import com.example.R
import com.example.data.VaultManager
import com.example.data.WorkspaceConfig
import com.example.services.PanicManager
import com.example.ui.components.InstanceSwitcher
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.frostedGlass
import kotlinx.coroutines.launch

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView

@Composable
fun DashboardScreen(
    isReal: Boolean, 
    onLock: () -> Unit, 
    navController: androidx.navigation.NavController? = null,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass? = null
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var fileManagerUserId by remember { mutableStateOf<String?>(null) }
    var fileManagerPackage by remember { mutableStateOf<String?>(null) }
    var profileImageUri by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    var activeInstance by remember { mutableStateOf<WorkspaceConfig?>(null) }

    // Sub-tab states for unified hubs
    var vaultSubTab by remember { mutableStateOf(0) } // 0: Instâncias, 1: Clones
    var identitySubTab by remember { mutableStateOf(0) } // 0: Identidades, 1: Clipboard
    var systemSubTab by remember { mutableStateOf(0) } // 0: Chaos OS, 1: Logs

    // Auto-Lock Inactivity Tracker (60 seconds = 60,000ms)
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000)
            if (System.currentTimeMillis() - lastInteractionTime >= 60_000L) {
                com.example.AppLockManager.isUnlocked = false
                onLock()
                break
            }
        }
    }

    LaunchedEffect(showSettings) {
        if (!showSettings) {
            val vaultManager = VaultManager(context)
            profileImageUri = vaultManager.getProfileImageUri()
        }
    }

    LaunchedEffect(AppState.targetWorkspaceShortcut) {
        if (AppState.targetWorkspaceShortcut != null && isReal) {
            selectedTab = 0 // Vault Hub
            vaultSubTab = 0
        }
    }

    val tabs: List<Triple<String, ImageVector, @Composable () -> Unit>> = listOf(
        Triple("Vault", Icons.Filled.Dashboard) {
            if (!isReal) {
                FakeEmptyScreen("Nenhuma Instância Chaos OS.")
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    SegmentedTabHeader(
                        options = listOf("Instâncias Isoladas", "Todos os Clones"),
                        selectedIndex = vaultSubTab,
                        onSelect = { vaultSubTab = it }
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = vaultSubTab,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.95f)) togetherWith
                                (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.98f))
                            },
                            label = "VaultSubTabTransition"
                        ) { subTab ->
                            if (subTab == 0) {
                                WorkspacesScreen(navController = navController, windowSizeClass = windowSizeClass)
                            } else {
                                ClonesScreen(navController = navController, windowSizeClass = windowSizeClass)
                            }
                        }
                    }
                }
            }
        },
        Triple("Escudo", Icons.Filled.Security) { ShieldScreen() },
        Triple("Rede", Icons.Filled.Public) { if (isReal) NetworkDashboardScreen() else FakeEmptyScreen("Rede de Testes.") },
        Triple("Sistema", Icons.Filled.Build) {
            if (!isReal) {
                FakeEmptyScreen("Sem Registros.")
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    SegmentedTabHeader(
                        options = listOf("Controle Chaos OS", "Logs", "Armazenamento"),
                        selectedIndex = systemSubTab,
                        onSelect = { systemSubTab = it }
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = systemSubTab,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.95f)) togetherWith
                                (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.98f))
                            },
                            label = "SystemSubTabTransition"
                        ) { subTab ->
                            when (subTab) {
                                0 -> ChaosOsControlScreen()
                                1 -> SessionLogsScreen()
                                2 -> StorageVisualizerScreen()
                            }
                        }
                    }
                }
            }
        }
    )

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    if (fileManagerUserId != null && fileManagerPackage != null) {
        IsolatedFileManagerScreen(
            userId = fileManagerUserId!!,
            packageName = fileManagerPackage!!,
            onNavigateBack = { 
                fileManagerUserId = null 
                fileManagerPackage = null
            }
        )
        return
    }

    val scope = rememberCoroutineScope()
    var showPanicConfirm by remember { mutableStateOf(false) }

    if (showPanicConfirm) {
        AlertDialog(
            onDismissRequest = { showPanicConfirm = false },
            title = { Text("PANIC WIPE", color = DangerRed) },
            text = { Text("Isso apagará TODOS os workspaces, clones e dados. Continuar?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    showPanicConfirm = false
                    scope.launch { PanicManager.executePanicWipe(context) }
                }) { Text("APAGAR TUDO", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { showPanicConfirm = false }) { Text("Cancelar", color = NeonCyan) }
            },
            containerColor = Color.DarkGray
        )
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent()
                    lastInteractionTime = System.currentTimeMillis()
                }
            }
        },
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Main Header Row: Identity + Quick Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { showSettings = true }
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageUri != null) {
                                AsyncImage(
                                    model = profileImageUri,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_ghost_shield),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            val gradient = Brush.linearGradient(colors = listOf(ElectricPurple, NeonCyan))
                            Text(
                                "ShadowVault",
                                style = TextStyle(brush = gradient),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                "CHAOS OS • VAULT ISOLADO",
                                color = NeonCyan.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    // Quick Liquid Glass Action Capsule
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = { showPanicConfirm = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = "Panic Wipe", tint = DangerRed, modifier = Modifier.size(18.dp))
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(14.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )
                        IconButton(
                            onClick = onLock,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = "Lock", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Sub Header Row: Active Scope / Workspace Switcher
                if (isReal) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                            .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (activeInstance != null) NeonCyan else ElectricPurple)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ESCOPO ATIVO:",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            InstanceSwitcher(
                                activeInstance = activeInstance,
                                onInstanceSelected = { activeInstance = it }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            val isExpandedScreen = windowSizeClass?.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded ||
                                   windowSizeClass?.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium
            
            if (!isExpandedScreen) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .fillMaxWidth()
                        .height(68.dp)
                        .frostedGlass(cornerRadius = 34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = selectedTab == index
                            val itemAlpha by animateFloatAsState(targetValue = if (isSelected) 1f else 0.45f, label = "alpha")
                            val itemScale by animateFloatAsState(targetValue = if (isSelected) 1.15f else 1f, label = "scale")

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        selectedTab = index
                                    }
                            ) {
                                Icon(
                                    imageVector = tab.second,
                                    contentDescription = tab.first,
                                    tint = if (isSelected) NeonCyan else Color.White.copy(alpha = itemAlpha),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .scale(itemScale)
                                        .graphicsLayer {
                                            if (isSelected) {
                                                shadowElevation = 12f
                                                ambientShadowColor = ElectricPurple
                                                spotShadowColor = ElectricPurple
                                            }
                                        }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.first,
                                    color = if (isSelected) NeonCyan else Color.White.copy(alpha = itemAlpha),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val isExpandedScreen = windowSizeClass?.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded ||
                               windowSizeClass?.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isExpandedScreen) {
                // Navigation Rail for Tablets
                NavigationRail(
                    containerColor = Color.Transparent,
                    modifier = Modifier.width(90.dp).fillMaxHeight().padding(vertical = 16.dp, horizontal = 8.dp).frostedGlass(24.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTab == index
                        val itemAlpha by animateFloatAsState(targetValue = if (isSelected) 1f else 0.45f, label = "alpha")
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                selectedTab = index
                            },
                            icon = { Icon(tab.second, contentDescription = tab.first, tint = if (isSelected) NeonCyan else Color.White.copy(alpha = itemAlpha)) },
                            label = { Text(tab.first, color = if (isSelected) NeonCyan else Color.White.copy(alpha = itemAlpha), fontSize = 10.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.16f),
                                NeonCyan.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        ),
                        RoundedCornerShape(22.dp)
                    )
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f)) togetherWith
                        (fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.98f))
                    },
                    label = "MainTabTransition"
                ) { targetTab ->
                    tabs[targetTab].third()
                }
            }
        }
    }
}

@Composable
fun SegmentedTabHeader(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.14f), NeonCyan.copy(alpha = 0.1f), Color.White.copy(alpha = 0.05f))
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSelected) {
                            Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(ElectricPurple.copy(alpha = 0.55f), NeonCyan.copy(alpha = 0.55f))
                                    )
                                )
                                .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                    )
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onSelect(index)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun FakeEmptyScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray)
    }
}
