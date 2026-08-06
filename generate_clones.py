import sys

full_code = """package com.example.ui.screens

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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.CloneEntity
import com.example.ui.components.FrostedButton
import com.example.ui.theme.*
import com.example.utils.ShizukuUtils
import com.example.utils.useShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClonesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).vaultDao()
    val clones: StateFlow<List<CloneEntity>> = dao.getAllClones().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun loadInstalledApps(context: Context): List<ApplicationInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
    }

    suspend fun getWorkspaces(): List<Pair<String, String>> {
        if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) return emptyList()
        val usersOutput = ShizukuUtils.executeCommand("pm list users")
        val userLines = usersOutput.lines().filter { it.contains("UserInfo") }
        return userLines.mapNotNull { line ->
            try {
                val idPart = line.substringAfter("{").substringBefore(":")
                val namePart = line.substringAfter(":").substringBefore(":")
                if (idPart == "0") null else Pair(idPart, namePart)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun installCloneToWorkspace(
        context: Context, 
        appToClone: ApplicationInfo, 
        targetUserId: String?,
        isVirtual: Boolean = false,
        fakeAndroidId: String? = null,
        fakeBrand: String? = null,
        fakeGps: String? = null,
        customName: String? = null
    ) {
        viewModelScope.launch {
            if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Shizuku not ready", Toast.LENGTH_SHORT).show() }
                return@launch
            }

            val pm = context.packageManager
            val packageName = appToClone.packageName
            val appName = customName ?: pm.getApplicationLabel(appToClone).toString()
            
            var finalUserId: String
            if (targetUserId == null) {
                withContext(Dispatchers.Main) { Toast.makeText(context, if (isVirtual) "Creating Chaos OS Virtual Env..." else "Creating new workspace...", Toast.LENGTH_SHORT).show() }
                val prefix = if (isVirtual) "ChaosOS_" else "Workspace_"
                val profileResult = ShizukuUtils.createWorkProfile("${prefix}${System.currentTimeMillis() % 1000}")
                if (profileResult.isFailure) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Cannot create workspace: ${profileResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                finalUserId = profileResult.getOrNull() ?: "10"
            } else {
                val existingClones = dao.getAllClones().first()
                val alreadyExists = existingClones.any { it.packageName == packageName && it.userId == targetUserId }
                if (alreadyExists) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Instância existente. Criando container único...", Toast.LENGTH_SHORT).show() }
                    val profileResult = ShizukuUtils.createWorkProfile("Clone_${packageName.takeLast(4)}_${System.currentTimeMillis() % 1000}")
                    if (profileResult.isFailure) {
                        withContext(Dispatchers.Main) { Toast.makeText(context, "Erro ao criar container interno.", Toast.LENGTH_SHORT).show() }
                        return@launch
                    }
                    finalUserId = profileResult.getOrNull() ?: targetUserId
                } else {
                    finalUserId = targetUserId
                }
            }
            
            // Apply Fake Settings
            if (fakeAndroidId != null) {
                ShizukuUtils.executeCommand("settings put --user $finalUserId secure fake_device_android_id '$fakeAndroidId'")
            }
            if (fakeBrand != null) {
                ShizukuUtils.executeCommand("settings put --user $finalUserId secure fake_device_brand '${fakeBrand.split(" - ").first()}'")
                ShizukuUtils.executeCommand("settings put --user $finalUserId secure fake_device_model '${fakeBrand.split(" - ").last()}'")
            }
            if (fakeGps != null) {
                ShizukuUtils.executeCommand("settings put --user $finalUserId secure mock_location '1'")
                ShizukuUtils.executeCommand("settings put --user $finalUserId secure fake_location '$fakeGps'")
            }
            
            withContext(Dispatchers.Main) { Toast.makeText(context, "Installing $appName...", Toast.LENGTH_SHORT).show() }
            val installResult = ShizukuUtils.installExistingApp(finalUserId, packageName)
            
            if (installResult.isFailure) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to clone app: ${installResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
                return@launch
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Cloned successfully to ID $finalUserId", Toast.LENGTH_SHORT).show()
            }
            dao.insertClone(CloneEntity(appName = appName, packageName = packageName, userId = finalUserId, isRunning = true))
        }
    }

    fun launchClone(context: Context, clone: CloneEntity) {
        viewModelScope.launch {
            if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Shizuku not ready.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            ShizukuUtils.executeCommand("pm enable --user ${clone.userId} ${clone.packageName}")
            dao.updateCloneState(clone.id, true)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Starting profile ${clone.userId}...", Toast.LENGTH_SHORT).show()
            }
            val launchResult = ShizukuUtils.launchApp(context, clone.userId, clone.packageName)
            if (launchResult.isFailure) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error launching: ${launchResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Launched ${clone.appName}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteWorkspace(context: Context, userId: String, clonesInWorkspace: List<CloneEntity>, onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Shizuku not ready.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Deleting Workspace ID $userId...", Toast.LENGTH_SHORT).show()
            }
            
            val output = ShizukuUtils.executeCommand("pm remove-user $userId")
            if (output.contains("Error") || output.contains("Exception")) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error deleting workspace: $output", Toast.LENGTH_LONG).show()
                }
            } else {
                clonesInWorkspace.forEach {
                    dao.deleteClone(it)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Workspace $userId deleted.", Toast.LENGTH_SHORT).show()
                    onDeleted()
                }
            }
        }
    }

    fun freezeClone(context: Context, clone: CloneEntity) {
        viewModelScope.launch {
            val res = ShizukuUtils.executeCommand("pm disable-user --user ${clone.userId} ${clone.packageName}")
            if (!res.contains("Error") && !res.contains("Exception")) {
                dao.updateCloneState(clone.id, false)
                withContext(Dispatchers.Main) { Toast.makeText(context, "App congelado", Toast.LENGTH_SHORT).show() }
            } else {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Erro ao congelar: $res", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun unfreezeClone(context: Context, clone: CloneEntity) {
        viewModelScope.launch {
            val res = ShizukuUtils.executeCommand("pm enable --user ${clone.userId} ${clone.packageName}")
            if (!res.contains("Error") && !res.contains("Exception")) {
                dao.updateCloneState(clone.id, true)
                withContext(Dispatchers.Main) { Toast.makeText(context, "App descongelado", Toast.LENGTH_SHORT).show() }
            } else {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Erro ao descongelar: $res", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun deleteCloneApp(context: Context, clone: CloneEntity) {
        viewModelScope.launch {
            val res = ShizukuUtils.executeCommand("pm uninstall --user ${clone.userId} ${clone.packageName}")
            if (!res.contains("Error") && !res.contains("Exception") && res.contains("Success")) {
                dao.deleteClone(clone)
                withContext(Dispatchers.Main) { Toast.makeText(context, "App removido do workspace", Toast.LENGTH_SHORT).show() }
            } else {
                dao.deleteClone(clone)
                withContext(Dispatchers.Main) { Toast.makeText(context, "Removido localmente", Toast.LENGTH_SHORT).show() }
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
fun ClonesScreen(viewModel: ClonesViewModel = viewModel()) {
    val clones by viewModel.clones.collectAsState()
    var showAppSelector by remember { mutableStateOf(false) }
    var availableWorkspaces by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ShizukuStatusCard()

            if (availableWorkspaces.isEmpty() && clones.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Sem espaços de trabalho ou clones.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    availableWorkspaces.forEach { ws ->
                        val userId = ws.first
                        val userClones = clones.filter { it.userId == userId }
                        
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
                                            viewModel.deleteWorkspace(ctx, userId, userClones) {
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
                                    .padding(top = 16.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Workspace: $userId (${ws.second})", color = NeonCyan, style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { 
                                    showDeleteDialog = true
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Workspace", tint = DangerRed)
                                }
                            }
                        }
                        
                        if (userClones.isEmpty()) {
                            item {
                                Text("Nenhum app clonado neste espaço.", color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                            }
                        } else {
                            items(userClones) { clone ->
                                CloneItem(clone, viewModel)
                            }
                        }
                    }
                    
                    val orphanedClones = clones.filter { clone -> availableWorkspaces.none { it.first == clone.userId } }
                    if (orphanedClones.isNotEmpty()) {
                        item {
                            Text("Clones Órfãos (Workspace não encontrado)", color = DangerRed, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                        }
                        items(orphanedClones) { clone ->
                            CloneItem(clone, viewModel)
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
fun AppSelectionWizard(viewModel: ClonesViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val installedApps = remember { viewModel.loadInstalledApps(context) }
    var selectedApp by remember { mutableStateOf<ApplicationInfo?>(null) }
    
    // Screens: 0 = App Selection, 1 = Config, 2 = Device Privacy, 3 = Location Privacy
    var currentScreen by remember { mutableIntStateOf(0) }
    
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
    var selectedBrandModel by remember { mutableStateOf("Apple - Game phone Black shark 4pro") }
    
    // Location Privacy states
    var spoofLocation by remember { mutableStateOf(false) }
    var fakeLat by remember { mutableStateOf("0.0") }
    var fakeLng by remember { mutableStateOf("0.0") }
    
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
                            0 -> "Select App to Clone"
                            1 -> "Clone Application"
                            2 -> "Device Privacy"
                            3 -> "Location Privacy"
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
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { 
                                                selectedApp = appInfo
                                                cloneName = appInfo.loadLabel(context.packageManager).toString() + " Clone"
                                                currentScreen = 1
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = ElectricPurple)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(appInfo.loadLabel(context.packageManager).toString(), color = Color.White)
                                            Text(appInfo.packageName, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha=0.05f))
                                }
                            }
                        }
                        1 -> {
                            // Main Config Screen (Clone App Pro Style)
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Text("The cloned application runs in a virtual machine and will generate an installation file (apk) for the virtual machine to run according to the configuration", 
                                    color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 24.dp))
                                
                                OutlinedTextField(
                                    value = cloneName,
                                    onValueChange = { cloneName = it },
                                    label = { Text("Name", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedBorderColor = NeonCyan
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Settings list
                                SettingRow(title = "Chaos OS", subtitle = "If the cloned application doesn't work with this Chaos OS, you can try switching to another version", value = chaosOsVersion) {
                                    chaosOsEnabled = !chaosOsEnabled
                                }
                                SettingRow(title = "General settings", subtitle = "Set up a virtual SD card, Google services, etc.") {}
                                SettingRow(title = "Device privacy", subtitle = "Set brand model, change device ID, etc.", icon = Icons.Filled.Security) {
                                    currentScreen = 2
                                }
                                SettingRow(title = "Location privacy", subtitle = "Set fake GPS location, virtual address book, etc.", icon = Icons.Filled.LocationOn) {
                                    currentScreen = 3
                                }
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    FrostedButton(
                                        onClick = {
                                            viewModel.installCloneToWorkspace(
                                                context = context, 
                                                appToClone = selectedApp!!, 
                                                targetUserId = null, 
                                                isVirtual = true, 
                                                fakeAndroidId = if(spoofAndroidId) "dummy_android_id" else null,
                                                fakeBrand = if(enableDevicePrivacy) selectedBrandModel else null,
                                                fakeGps = if(spoofLocation) "$fakeLat,$fakeLng" else null,
                                                customName = cloneName
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
                                            viewModel.installCloneToWorkspace(
                                                context = context, 
                                                appToClone = selectedApp!!, 
                                                targetUserId = null, 
                                                isVirtual = false,
                                                fakeAndroidId = if(spoofAndroidId) "dummy_android_id" else null,
                                                fakeBrand = if(enableDevicePrivacy) selectedBrandModel else null,
                                                fakeGps = if(spoofLocation) "$fakeLat,$fakeLng" else null,
                                                customName = cloneName
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
"""

with open('app/src/main/java/com/example/ui/screens/ClonesScreen.kt', 'w') as f:
    f.write(full_code)

