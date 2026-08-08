package com.example.ui.screens

import kotlinx.coroutines.isActive
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.CloneEntity
import com.example.ui.components.FrostedButton
import com.example.ui.theme.*
import com.example.utils.ShizukuUtils
import com.example.services.CloneManager
import com.example.utils.useShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClonesViewModel(application: Application) : AndroidViewModel(application) {
    private val _clones = kotlinx.coroutines.flow.MutableStateFlow<List<CloneEntity>>(emptyList())
    val clones: StateFlow<List<CloneEntity>> = _clones

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(application).vaultDao()
                dao.getAllClones().collect {
                    _clones.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun loadInstalledApps(context: Context): List<ApplicationInfo> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getWorkspaces(): List<Pair<String, String>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) return@withContext emptyList()
        val usersOutput = ShizukuUtils.executeCommand("pm list users")
        val userLines = usersOutput.lines().filter { it.contains("UserInfo") }
        userLines.mapNotNull { line ->
            try {
                val idPart = line.substringAfter("{").substringBefore(":")
                val namePart = line.substringAfter(":").substringBefore(":")
                if (idPart == "0") null else Pair(idPart, namePart)
            } catch (e: Exception) {
                null
            }
        }
    }

                        fun deleteCloneApp(context: Context, clone: CloneEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = ShizukuUtils.executeCommand("pm uninstall --user ${clone.userId} ${clone.packageName}")
            try {
                val dao = AppDatabase.getDatabase(context).vaultDao()
                if (!res.contains("Error") && !res.contains("Exception") && res.contains("Success")) {
                    dao.deleteClone(clone)
                    withContext(Dispatchers.Main) { Toast.makeText(context, "App removido do workspace", Toast.LENGTH_SHORT).show() }
                } else {
                    dao.deleteClone(clone)
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Removido localmente", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun ShizukuStatusCard() {
    val (isShizukuAvailable, hasShizukuPermission) = useShizukuStatus()
    val isConnected = isShizukuAvailable && hasShizukuPermission
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .frostedGlass(cornerRadius = 16.dp)
            .padding(16.dp)
    ) {
        AnimatedContent(
            targetState = isConnected,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            },
            label = "ShizukuStatusAnimation"
        ) { connected ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusColor = if (connected) NeonCyan else DangerRed
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (connected) "Shizuku Active" else "Shizuku Offline / No Permission",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Required for Work Profile & App Cloning",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!connected && isShizukuAvailable) {
                    FrostedButton(
                        onClick = { ShizukuUtils.requestShizukuPermission() },
                        color = ElectricPurple
                    ) {
                        Text("GRANT")
                    }
                }
            }
        }
    }
}

@Composable
fun ClonesScreen(
    viewModel: ClonesViewModel = viewModel(),
    navController: androidx.navigation.NavController? = null,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass? = null
) {
    val clones by viewModel.clones.collectAsState()
    var showAppSelector by remember { mutableStateOf(false) }
    var availableWorkspaces by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var selectedClone by remember { mutableStateOf<CloneEntity?>(null) }
    val context = LocalContext.current

    val isExpandedScreen = windowSizeClass?.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded ||
                           windowSizeClass?.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium

    LaunchedEffect(clones, refreshTrigger) {
        availableWorkspaces = viewModel.getWorkspaces()
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .interactiveFrostedGlass(cornerRadius = 16.dp, color = MaterialTheme.colorScheme.primary, onClick = { showAppSelector = true }),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Clone", tint = MaterialTheme.colorScheme.primary)
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                ShizukuStatusCard()

            if (availableWorkspaces.isEmpty() && clones.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Sem espaços de trabalho ou clones.", color = Color.Gray)
                }
            } else {
                val groupedClones by remember(clones, availableWorkspaces) {
                    derivedStateOf {
                        availableWorkspaces.map { ws ->
                            ws to clones.filter { it.userId == ws.first }
                        }
                    }
                }
                val orphanedClones by remember(clones, availableWorkspaces) {
                    derivedStateOf {
                        clones.filter { clone -> availableWorkspaces.none { it.first == clone.userId } }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedClones.forEach { (ws, userClones) ->
                        val userId = ws.first
                        
                        item {
                            val ctx = LocalContext.current
                            var showDeleteDialog by remember { mutableStateOf(false) }

                            if (showDeleteDialog) {
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { showDeleteDialog = false },
                                    title = { Text("Confirmar Exclusão", color = NeonCyan) },
                                    text = { Text("Tem certeza que deseja excluir o workspace $userId? Isso apagará todos os dados e apps isolados deste espaço.", color = Color.White) },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showDeleteDialog = false
                                            CloneManager.deleteWorkspace(ctx, userId, userClones) {
                                                refreshTrigger++
                                            }
                                        }) {
                                            Text("Excluir", color = DangerRed)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteDialog = false }) {
                                            Text("Cancelar", color = Color.Gray)
                                        }
                                    },
                                    containerColor = VaultBackground.copy(alpha = 0.95f)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Folder, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Workspace: $userId", color = NeonCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Excluir Workspace", tint = DangerRed)
                                }
                            }
                        }
                        
                        items(userClones) { clone ->
                            Box(modifier = Modifier.clickable { selectedClone = clone }) {
                                CloneItem(clone = clone, viewModel = viewModel)
                            }
                        }
                    }
                    
                    if (orphanedClones.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                                Icon(Icons.Filled.Android, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clones Órfãos (Main Profile)", color = Color.Gray, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        items(orphanedClones) { clone ->
                            Box(modifier = Modifier.clickable { selectedClone = clone }) {
                                CloneItem(clone = clone, viewModel = viewModel)
                            }
                        }
                    }
                }
            }
            }
            
            if (isExpandedScreen) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 16.dp)
                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                ) {
                    if (selectedClone != null) {
                        CloneDetailPane(clone = selectedClone!!)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Selecione um clone para inspecionar", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }


    if (showAppSelector) {
        AppSelectionWizard(viewModel, onDismiss = { showAppSelector = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionWizard(viewModel: ClonesViewModel, targetWorkspaceId: String? = null, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<ApplicationInfo>>(emptyList()) }
    LaunchedEffect(Unit) {
        installedApps = viewModel.loadInstalledApps(context)
    }
    var selectedApp by remember { mutableStateOf<ApplicationInfo?>(null) }
    
    // Screens: 0 = App Selection, 1 = Config, 2 = Device Privacy, 3 = Location Privacy
    var currentScreen by remember { mutableIntStateOf(0) }
    
    // General Settings states
    var enableVirtualSdCard by remember { mutableStateOf(true) }
    var enableGoogleServices by remember { mutableStateOf(true) }
    var hideAppIcon by remember { mutableStateOf(false) }
    var openLinksInClone by remember { mutableStateOf(true) }
    var enableHardwareAccel by remember { mutableStateOf(true) }
    var bypassBatteryOpt by remember { mutableStateOf(true) }
    
    // Config states
    var cloneName by remember { mutableStateOf("") }
    var chaosOsEnabled by remember { mutableStateOf(false) }
    var chaosOsVersion by remember { mutableStateOf("v2.7.1") }
    
    // Device Privacy states
    var enableDevicePrivacy by remember { mutableStateOf(false) }
    var spoofAndroidId by remember { mutableStateOf(false) }
    var spoofImei by remember { mutableStateOf(false) }
    var spoofMac by remember { mutableStateOf(false) }
    var spoofGoogleAdId by remember { mutableStateOf(false) }
    var spoofSimInfo by remember { mutableStateOf(false) }
    
    // Deep Stealth / Chaos OS specific
    var isolateFilesystem by remember { mutableStateOf(false) }
    var spoofSensors by remember { mutableStateOf(false) }
    var hideRoot by remember { mutableStateOf(false) }
    var strictPackageFirewall by remember { mutableStateOf(false) }
    var selectedBrandModel by remember { mutableStateOf("Apple - Game phone Black shark 4pro") }
    
    // Location Privacy states
    var spoofLocation by remember { mutableStateOf(false) }
    var fakeLat by remember { mutableStateOf("0.0") }
    var fakeLng by remember { mutableStateOf("0.0") }

    // Network Privacy states
    var useCloneProxy by remember { mutableStateOf(false) }
    var cloneProxyRegion by remember { mutableStateOf("US - Nova York") }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VaultBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (currentScreen > 1) {
                            currentScreen = 1
                        } else if (currentScreen == 1) {
                            currentScreen = 0
                            selectedApp = null
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = when (currentScreen) {
                            0 -> "Instalar App em Instância (Clonagem)"
                            1 -> "Configurar Instalação (Chaos OS)"
                            2 -> "Privacidade do Dispositivo"
                            3 -> "Privacidade de Localização"
                            4 -> "Isolamento de Rede"
                            5 -> "Configurações Gerais"
                            else -> ""
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentScreen) {
                        0 -> {
                            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                                items(installedApps) { appInfo ->
                                    InstalledAppRow(
                                        appInfo = appInfo,
                                        context = context,
                                        onClick = {
                                            selectedApp = appInfo
                                            cloneName = appInfo.loadLabel(context.packageManager).toString() + " Clone"
                                            currentScreen = 1
                                        }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                        1 -> {
                            // Main Config Screen (Clone App Pro Style)
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Text("O aplicativo clonado executa em máquina virtual e irá gerar a instância conforme a configuração abaixo.", 
                                    color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    if (selectedApp != null) {
                                        var selectedIconDrawable by remember(selectedApp) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
                                        LaunchedEffect(selectedApp) {
                                            withContext(Dispatchers.IO) {
                                                selectedIconDrawable = try { selectedApp!!.loadIcon(context.packageManager) } catch (e: Exception) { null }
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selectedIconDrawable != null) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(model = selectedIconDrawable),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            } else {
                                                Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(28.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    OutlinedTextField(
                                        value = cloneName,
                                        onValueChange = { cloneName = it },
                                        label = { Text("Nome do Clone", color = Color.Gray) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedBorderColor = NeonCyan
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Settings list
                                 SettingRow(title = "Chaos OS", subtitle = "If the cloned application doesn't work with this Chaos OS, you can try switching to another version", value = chaosOsVersion, icon = Icons.Filled.SettingsSystemDaydream) {
                                    chaosOsEnabled = !chaosOsEnabled
                                }
                                SettingRow(title = "General settings", subtitle = "Set up a virtual SD card, Google services, etc.", icon = Icons.Filled.Settings) {
                                    currentScreen = 5
                                }
                                SettingRow(title = "Device privacy", subtitle = "Set brand model, change device ID, etc.", icon = Icons.Filled.Security) {
                                    currentScreen = 2
                                }
                                SettingRow(title = "Location privacy", subtitle = "Set fake GPS location, virtual address book, etc.", icon = Icons.Filled.LocationOn) {
                                    currentScreen = 3
                                }
                                SettingRow(title = "Network isolation", subtitle = "Assign specific VPN or Proxy to this clone", icon = Icons.Filled.VpnKey) {
                                    currentScreen = 4
                                }
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    FrostedButton(
                                        onClick = {
                                            CloneManager.installCloneToWorkspace(
                                                context = context, 
                                                appToClone = selectedApp!!, 
                                                targetUserId = targetWorkspaceId, 
                                                isVirtual = true, 
                                                fakeAndroidId = if(spoofAndroidId) "dummy_android_id" else null,
                                                fakeBrand = if(enableDevicePrivacy) selectedBrandModel else null,
                                                fakeGps = if(spoofLocation) "$fakeLat,$fakeLng" else null,
                                                fakeImei = spoofImei,
                                                fakeMac = spoofMac,
                                                fakeAdId = spoofGoogleAdId,
                                                fakeSim = spoofSimInfo,
                                                chaosOsVersion = if(chaosOsEnabled) chaosOsVersion else null,
                                                customName = cloneName,
                                                isolateFilesystem = isolateFilesystem,
                                                spoofSensors = spoofSensors,
                                                hideRoot = hideRoot,
                                                strictPackageFirewall = strictPackageFirewall,
                                                proxyRegion = if(useCloneProxy) cloneProxyRegion else null
                                            )
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        color = NeonCyan
                                    ) {
                                        Text("Internal installation")
                                    }
                                    
                                    FrostedButton(
                                        onClick = {
                                            CloneManager.installCloneToWorkspace(
                                                context = context, 
                                                appToClone = selectedApp!!, 
                                                targetUserId = targetWorkspaceId, 
                                                isVirtual = false,
                                                fakeAndroidId = if(spoofAndroidId) "dummy_android_id" else null,
                                                fakeBrand = if(enableDevicePrivacy) selectedBrandModel else null,
                                                fakeGps = if(spoofLocation) "$fakeLat,$fakeLng" else null,
                                                fakeImei = spoofImei,
                                                fakeMac = spoofMac,
                                                fakeAdId = spoofGoogleAdId,
                                                fakeSim = spoofSimInfo,
                                                chaosOsVersion = null,
                                                customName = cloneName,
                                                proxyRegion = if(useCloneProxy) cloneProxyRegion else null
                                            )
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        color = ElectricPurple
                                    ) {
                                        Text("Standalone installation")
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        2 -> {
                            // Device Privacy Screen
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom=16.dp)) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("brand/model", color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Choose another brand model, the clone application will not get the real device brand model logo", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(checked = enableDevicePrivacy, onCheckedChange = { enableDevicePrivacy = it }, colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha=0.3f)))
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha=0.05f))
                                }
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical=16.dp)) {
                                        Text("Open a new identity with one click", color = Color.White)
                                        Switch(checked = enableDevicePrivacy, onCheckedChange = { enableDevicePrivacy = it }, colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha=0.3f)))
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha=0.05f))
                                }
                                item { ToggleSettingRow("Android ID", "Report the changed Android ID to the cloned application to prevent obtaining the native Android ID", spoofAndroidId) { spoofAndroidId = it } }
                                item { ToggleSettingRow("IMEI/IMSI", "Change the IMEI/IMSI number of the phone in the clone application", spoofImei) { spoofImei = it } }
                                item { ToggleSettingRow("WiFi/Bluetooth MAC address", "Change your WiFi/Bluetooth mac address", spoofMac) { spoofMac = it } }
                                item { ToggleSettingRow("Google Advertising ID", "Report a random Google advertising ID to the cloned application to prevent it from being tracked by advertising", spoofGoogleAdId) { spoofGoogleAdId = it } }
                                item { ToggleSettingRow("SIM card and carrier information", "Hide local SIM card serial number, phone number, phone location, carrier identifier/name/country and other information", spoofSimInfo) { spoofSimInfo = it } }
                            }
                        }
                        4 -> {
                            // Network Isolation Screen
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom=16.dp)) {
                                        Column {
                                            Text("Isolamento de Rede", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("Aplicar VPN/Proxy APENAS neste clone", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Switch(
                                            checked = useCloneProxy,
                                            onCheckedChange = { useCloneProxy = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha=0.3f))
                                        )
                                    }
                                }
                                
                                if (useCloneProxy) {
                                    item {
                                        Text("Região do Proxy", color = Color.White, modifier = Modifier.padding(top=8.dp, bottom=8.dp))
                                        
                                        val regions = listOf(
                                            "US - Nova York", "US - Miami", "US - Los Angeles", 
                                            "BR - São Paulo", "BR - Rio de Janeiro",
                                            "UK - Londres", "JP - Tóquio", "DE - Frankfurt"
                                        )
                                        
                                        regions.forEach { region ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .background(if (cloneProxyRegion == region) NeonCyan.copy(alpha=0.2f) else Color.DarkGray, RoundedCornerShape(8.dp))
                                                    .border(1.dp, if (cloneProxyRegion == region) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                    .clickable { cloneProxyRegion = region }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = cloneProxyRegion == region,
                                                    onClick = { cloneProxyRegion = region },
                                                    colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                                )
                                                Text(region, color = if (cloneProxyRegion == region) NeonCyan else Color.White, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            // Location Privacy
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom=16.dp)) {
                                    Text("Location simulation", color = Color.White, fontWeight = FontWeight.Bold)
                                    Switch(checked = spoofLocation, onCheckedChange = { spoofLocation = it }, colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha=0.3f)))
                                }
                                if (spoofLocation) {
                                    OutlinedTextField(
                                        value = fakeLat,
                                        onValueChange = { fakeLat = it },
                                        label = { Text("Latitude", color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)
                                    )
                                    OutlinedTextField(
                                        value = fakeLng,
                                        onValueChange = { fakeLng = it },
                                        label = { Text("Longitude", color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text("Location simulation is not enabled", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top=32.dp))
                                }
                            }
                        }
                        5 -> {
                            // General Settings Screen
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                item {
                                    ToggleSettingRow(
                                        title = "Cartão SD Virtual Isolado",
                                        subtitle = "Cria um diretório SD Card virtual exclusivo para salvar mídias e arquivos do clone sem expor seu armazenamento interno real",
                                        checked = enableVirtualSdCard
                                    ) { enableVirtualSdCard = it }
                                    HorizontalDivider(color = Color.White.copy(alpha=0.05f))
                                }
                                item {
                                    ToggleSettingRow(
                                        title = "Serviços Google Play",
                                        subtitle = "Habilita suporte a login Google, Play Framework e notificações para este clone",
                                        checked = enableGoogleServices
                                    ) { enableGoogleServices = it }
                                    HorizontalDivider(color = Color.White.copy(alpha=0.05f))
                                }
                                item {
                                    ToggleSettingRow(
                                        title = "Ocultar Ícone no Launcher",
                                        subtitle = "Não cria atalho no iniciador de aplicativos do celular principal. O clone só poderá ser aberto dentro deste app",
                                        checked = hideAppIcon
                                    ) { hideAppIcon = it }
                                    HorizontalDivider(color = Color.White.copy(alpha=0.05f))
                                }
                                item {
                                    ToggleSettingRow(
                                        title = "Abrir Links Web no Clone",
                                        subtitle = "Redireciona URLs acionadas dentro do aplicativo clonado para o navegador seguro do próprio ambiente virtual",
                                        checked = openLinksInClone
                                    ) { openLinksInClone = it }
                                    HorizontalDivider(color = Color.White.copy(alpha=0.05f))
                                }
                                item {
                                    ToggleSettingRow(
                                        title = "Aceleração de Hardware GPU Direct",
                                        subtitle = "Melhora o desempenho do app clonado utilizando a aceleração gráfica diretamente",
                                        checked = enableHardwareAccel
                                    ) { enableHardwareAccel = it }
                                    HorizontalDivider(color = Color.White.copy(alpha=0.05f))
                                }
                                item {
                                    ToggleSettingRow(
                                        title = "Ignorar Otimização de Bateria",
                                        subtitle = "Impede que o sistema operacional encerre o processo do clone em segundo plano",
                                        checked = bypassBatteryOpt
                                    ) { bypassBatteryOpt = it }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRow(title: String, subtitle: String, value: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = ElectricPurple, modifier = Modifier.padding(end=16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
        }
        if (value != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(value, color = NeonCyan, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun ToggleSettingRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha=0.3f)))
    }
}

@Composable
fun InstalledAppRow(
    appInfo: ApplicationInfo,
    context: Context,
    onClick: () -> Unit
) {
    val appName = remember(appInfo) { appInfo.loadLabel(context.packageManager).toString() }
    var iconDrawable by remember(appInfo) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

    LaunchedEffect(appInfo.packageName) {
        withContext(Dispatchers.IO) {
            iconDrawable = try {
                appInfo.loadIcon(context.packageManager)
            } catch (e: Exception) { null }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (iconDrawable != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = iconDrawable),
                    contentDescription = appName,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    tint = ElectricPurple,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(appName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(appInfo.packageName, color = Color.Gray, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ClonesScreenPreview() {
    val dummyClones = listOf(
        CloneEntity(appName = "WhatsApp Clone", packageName = "com.whatsapp", isRunning = true),
        CloneEntity(appName = "Telegram Secure", packageName = "org.telegram.messenger", isRunning = false)
    )
    ShadowVaultTheme(darkTheme = true) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            dummyClones.forEach { clone ->
                CloneItem(clone = clone)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun CloneDetailPane(clone: CloneEntity) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Android, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(clone.appName, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(clone.packageName, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("STATUS DO CLONE", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("ID do Workspace: ${clone.userId}", color = Color.White)
        Text("Em execução: ${if (clone.isRunning) "Sim" else "Não"}", color = if (clone.isRunning) Color(0xFF00FF88) else Color.White)
        Text("Criado em: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(clone.dateCreated))}", color = Color.White)
    }
}
