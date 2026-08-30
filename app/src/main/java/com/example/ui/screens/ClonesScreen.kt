package com.example.ui.screens

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.CloneEntity
import com.example.data.VaultManager
import com.example.ui.theme.*
import com.example.utils.BiometricAuthHelper
import com.example.utils.ShizukuUtils
import com.example.services.CloneManager
import com.example.ui.components.EmptyStateView
import com.example.utils.HapticEngine
import com.example.utils.useShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.text.SimpleDateFormat

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

    suspend fun loadInstalledApps(context: Context): List<ApplicationInfo> = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getWorkspaces(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
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
            val res = ShizukuUtils.executeCommand("pm uninstall --user  ")
            try {
                val dao = AppDatabase.getDatabase(context).vaultDao()
                dao.deleteClone(clone)
                withContext(Dispatchers.Main) { Toast.makeText(context, "Clone removido", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun ClonesScreen(
    navController: androidx.navigation.NavController? = null,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass? = null,
    viewModel: ClonesViewModel = viewModel()
) {
    val clones by viewModel.clones.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vaultManager = remember { VaultManager(context) }
    var showCloneWizard by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(vaultManager.getClonesViewMode()) } // "GRID" or "LIST"
    var biometricLockVersion by remember { mutableIntStateOf(0) } // Trigger recomposition on lock toggle

    val filteredClones = remember(clones, searchQuery) {
        if (searchQuery.isBlank()) clones
        else clones.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true) ||
            it.userId.contains(searchQuery, ignoreCase = true)
        }
    }

    fun handleCloneClick(clone: CloneEntity) {
        HapticEngine.vibrateClick(context)
        // [SECURITY] Biometric prompt before opening clone REMOVED per user request
        CloneManager.launchClone(context, clone)
    }

    Box(modifier = Modifier.fillMaxSize().background(VaultBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gaveta de Clones",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                if (clones.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Limpar Cache
                        FilledTonalButton(
                            onClick = {
                                HapticEngine.vibrateSuccess(context)
                                scope.launch {
                                    ShizukuUtils.trimAllCaches()
                                    Toast.makeText(context, "🧹 Caches de clones limpos!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Limpar Cache", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpar Cache", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Freezer
                        FilledTonalButton(
                            onClick = {
                                HapticEngine.vibrateFreeze(context)
                                CloneManager.freezeAllClones(context)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.frostedGlass(10.dp).border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                Icons.Filled.AcUnit,
                                contentDescription = "Congelar Clones",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Congelar Todos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Search Bar & View Mode Toggle (Items 05 & 06)
            if (clones.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar clone por nome ou pacote...", color = Color.Gray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = NeonCyan, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Limpar", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.3f),
                            focusedBorderColor = NeonCyan.copy(alpha = 0.6f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Grid / List Toggle
                    IconButton(
                        onClick = {
                            val newMode = if (viewMode == "GRID") "LIST" else "GRID"
                            viewMode = newMode
                            vaultManager.saveClonesViewMode(newMode)
                            HapticEngine.vibrateClick(context)
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFF0F172A).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            if (viewMode == "GRID") Icons.Filled.List else Icons.Filled.GridView,
                            contentDescription = "Alternar Visualização",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (clones.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.Apps,
                    title = "Nenhum Clone Criado",
                    subtitle = "Seus aplicativos isolados em perfis de segurança aparecerão aqui. Toque abaixo para clonar.",
                    actionLabel = "CLONAR PRIMEIRO APP",
                    onAction = { 
                        HapticEngine.vibrateClick(context)
                        showCloneWizard = true 
                    },
                    modifier = Modifier.weight(1f)
                )
            } else if (filteredClones.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.SearchOff,
                    title = "Nenhum Clone Encontrado",
                    subtitle = "Nenhum aplicativo corresponde ao termo '$searchQuery'.",
                    actionLabel = "LIMPAR BUSCA",
                    onAction = { searchQuery = "" },
                    modifier = Modifier.weight(1f)
                )
            } else if (viewMode == "GRID") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredClones, key = { it.id }) { clone ->
                        val isLocked = remember(clone.id, biometricLockVersion) { vaultManager.isCloneBiometricLocked(clone.id) }
                        CloneGridItem(
                            clone = clone,
                            isBiometricLocked = isLocked,
                            onToggleBiometric = {
                                val nextState = !isLocked
                                vaultManager.setCloneBiometricLocked(clone.id, nextState)
                                biometricLockVersion++
                                HapticEngine.vibrateClick(context)
                                Toast.makeText(context, if (nextState) "🔒 Biometria exigida para ${clone.appName}" else "🔓 Biometria desativada", Toast.LENGTH_SHORT).show()
                            },
                            onClick = { handleCloneClick(clone) },
                            onDelete = {
                                HapticEngine.vibrateError(context)
                                viewModel.deleteCloneApp(context, clone)
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredClones, key = { it.id }) { clone ->
                        val isLocked = remember(clone.id, biometricLockVersion) { vaultManager.isCloneBiometricLocked(clone.id) }
                        CloneListItem(
                            clone = clone,
                            isBiometricLocked = isLocked,
                            onToggleBiometric = {
                                val nextState = !isLocked
                                vaultManager.setCloneBiometricLocked(clone.id, nextState)
                                biometricLockVersion++
                                HapticEngine.vibrateClick(context)
                                Toast.makeText(context, if (nextState) "🔒 Biometria exigida para ${clone.appName}" else "🔓 Biometria desativada", Toast.LENGTH_SHORT).show()
                            },
                            onClick = { handleCloneClick(clone) },
                            onDelete = {
                                HapticEngine.vibrateError(context)
                                viewModel.deleteCloneApp(context, clone)
                            }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showCloneWizard = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = NeonCyan
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Clonar Apps", tint = Color.Black)
        }

        if (showCloneWizard) {
            CloneBatchWizard(viewModel, onDismiss = { showCloneWizard = false })
        }
    }
}

@Composable
fun CloneGridItem(
    clone: CloneEntity,
    isBiometricLocked: Boolean = false,
    onToggleBiometric: () -> Unit = {},
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var appIcon by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
    LaunchedEffect(clone.packageName) {
        try { appIcon = pm.getApplicationIcon(clone.packageName) } catch(e: Exception){}
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box {
            Image(
                painter = rememberAsyncImagePainter(appIcon),
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
            )
            // Freeze Badge (App Freezer)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-6).dp, y = (-6).dp)
                    .size(22.dp)
                    .background(Color(0xFF0284C7), CircleShape)
                    .clickable {
                        HapticEngine.vibrateFreeze(context)
                        com.example.services.CloneManager.freezeClone(context, clone)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AcUnit, contentDescription = "Congelar", tint = Color.White, modifier = Modifier.size(12.dp))
            }

            // Biometric Lock Toggle Badge (Item 11)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-6).dp, y = 6.dp)
                    .size(22.dp)
                    .background(if (isBiometricLocked) ElectricPurple else Color.DarkGray.copy(alpha = 0.8f), CircleShape)
                    .clickable(onClick = onToggleBiometric),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isBiometricLocked) Icons.Filled.Fingerprint else Icons.Filled.LockOpen,
                    contentDescription = "Bloqueio Biométrico",
                    tint = if (isBiometricLocked) NeonCyan else Color.LightGray,
                    modifier = Modifier.size(13.dp)
                )
            }

            // Delete Badge
            Box(
                modifier = Modifier.align(Alignment.TopEnd).offset(x=8.dp, y=(-8).dp).size(24.dp)
                    .background(DangerRed, CircleShape).clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(clone.appName, color = Color.White, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Bold)
        Text("Wksp: ${clone.userId}", color = NeonCyan, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
fun CloneListItem(
    clone: CloneEntity,
    isBiometricLocked: Boolean = false,
    onToggleBiometric: () -> Unit = {},
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var appIcon by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
    LaunchedEffect(clone.packageName) {
        try { appIcon = pm.getApplicationIcon(clone.packageName) } catch(e: Exception){}
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth().frostedGlass(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(appIcon),
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(clone.appName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(clone.packageName, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Workspace ${clone.userId}", color = NeonCyan, fontSize = 11.sp)
                    if (isBiometricLocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Filled.Fingerprint, contentDescription = "Biometria Ativa", tint = NeonCyan, modifier = Modifier.size(12.dp))
                    }
                }
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Biometric Lock Toggle
                IconButton(onClick = onToggleBiometric) {
                    Icon(
                        if (isBiometricLocked) Icons.Filled.Fingerprint else Icons.Filled.LockOpen,
                        contentDescription = "Biometria",
                        tint = if (isBiometricLocked) NeonCyan else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Freeze
                IconButton(onClick = {
                    HapticEngine.vibrateFreeze(context)
                    com.example.services.CloneManager.freezeClone(context, clone)
                }) {
                    Icon(Icons.Filled.AcUnit, contentDescription = "Congelar", tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                }

                // Delete
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remover", tint = DangerRed.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun CloneBatchWizard(viewModel: ClonesViewModel, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var workspaces by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedWorkspaceId by remember { mutableStateOf<String?>(null) }
    
    var installedApps by remember { mutableStateOf<List<ApplicationInfo>>(emptyList()) }
    var selectedApps by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    
    var isInstalling by remember { mutableStateOf(false) }
    var installLog by remember { mutableStateOf("Preparando motor Shizuku...") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        workspaces = viewModel.getWorkspaces()
        installedApps = viewModel.loadInstalledApps(context)
    }

    Dialog(onDismissRequest = { if(!isInstalling) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.8f)).padding(16.dp), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f).frostedGlass(24.dp).padding(16.dp)) {
                Text("Instalador em Lote (Batch)", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(targetState = step, label="") { s ->
                    when (s) {
                        1 -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text("Selecione o Workspace de Destino", color = NeonCyan)
                                Spacer(modifier = Modifier.height(16.dp))
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(workspaces) { (id, name) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                                                .background(if(selectedWorkspaceId == id) ElectricPurple else Color.White.copy(0.1f))
                                                .clickable { selectedWorkspaceId = id }.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.AccountTree, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                                                Text("ID: ", color = Color.Gray, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                                Button(
                                    onClick = { step = 2 }, enabled = selectedWorkspaceId != null,
                                    modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Text("Avançar", color = Color.Black)
                                }
                            }
                        }
                        2 -> {
                            val pm = context.packageManager
                            val filteredApps = installedApps.filter { pm.getApplicationLabel(it).toString().contains(searchQuery, true) }
                            Column(modifier = Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = searchQuery, onValueChange = { searchQuery = it },
                                    placeholder = { Text("Buscar apps...") }, modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(" apps selecionados", color = NeonCyan, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(filteredApps) { app ->
                                        val isSelected = selectedApps.contains(app.packageName)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                                selectedApps = if (isSelected) selectedApps - app.packageName else selectedApps + app.packageName
                                            },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(checked = isSelected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = NeonCyan))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(pm.getApplicationLabel(app).toString(), color = Color.White)
                                        }
                                    }
                                }
                                Button(
                                    onClick = { step = 3 }, enabled = selectedApps.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Text("Instalar Clones", color = Color.Black)
                                }
                            }
                        }
                        3 -> {
                            LaunchedEffect(Unit) {
                                isInstalling = true
                                val dao = AppDatabase.getDatabase(context).vaultDao()
                                val pm = context.packageManager
                                for (pkg in selectedApps) {
                                    val appInfo = installedApps.find { it.packageName == pkg }
                                    val name = if (appInfo != null) pm.getApplicationLabel(appInfo).toString() else pkg
                                    installLog = "Instalando  em Workspace ...\n"
                                    
                                    val sourceDir = pm.getApplicationInfo(pkg, 0).sourceDir
                                    val res = ShizukuUtils.executeCommand("pm install -r --user  ")
                                    
                                    if (res.contains("Success")) {
                                        dao.insertClone(CloneEntity(packageName = pkg, appName = name, userId = selectedWorkspaceId!!, dateCreated = System.currentTimeMillis()))
                                        installLog = "[OK]  instalado.\n"
                                    } else {
                                        installLog = "[ERRO]  falhou: \n"
                                    }
                                }
                                installLog = "Concluído!\n"
                                isInstalling = false
                            }
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text("Console de Instalação", color = NeonCyan)
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black).padding(8.dp)) {
                                    Text(installLog, color = Color.Green, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                if (!isInstalling) {
                                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                                        Text("Fechar", color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
