package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedButton
import com.example.ui.theme.frostedGlass
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.utils.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.models.InstalledApp
import com.example.utils.AppManager

data class UserProfile(val id: String, val name: String, val status: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkProfilesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val (isShizukuAvailable, hasShizukuPermission) = com.example.utils.useShizukuStatus()

    var profiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var profileConfigs by remember { mutableStateOf<Map<String, com.example.data.ProfileConfigEntity>>(emptyMap()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    var selectedProfileForApps by remember { mutableStateOf<UserProfile?>(null) }
    var profileToDelete by remember { mutableStateOf<UserProfile?>(null) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }

    fun refreshProfiles() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isRefreshing = true }
            val output = ShizukuUtils.executeCommand("pm list users")
            val userLines = output.lines().filter { it.contains("UserInfo") }
            val parsedProfiles = userLines.mapNotNull { line ->
                try {
                    val idPart = line.substringAfter("{").substringBefore(":")
                    val namePart = line.substringAfter(":").substringBefore(":")
                    val isRunning = line.contains("running", ignoreCase = true)
                    UserProfile(id = idPart, name = namePart, status = if (isRunning) "Running" else "Stopped")
                } catch (e: Exception) {
                    null
                }
            }.filter { it.id != "0" } 
            
            val vaultManager = com.example.data.VaultManager(context)
            val configs = vaultManager.getAllProfileConfigs()
            
            withContext(Dispatchers.Main) {
                profiles = parsedProfiles
                profileConfigs = configs
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(isShizukuAvailable, hasShizukuPermission) {
        if (isShizukuAvailable && hasShizukuPermission) {
            refreshProfiles()
        }
    }
    
    if (selectedProfileForApps != null) {
        LaunchedEffect(selectedProfileForApps) {
            isLoadingApps = true
            val apps = withContext(Dispatchers.IO) { AppManager.getInstalledApps(context) }
            installedApps = apps
            isLoadingApps = false
        }
    }

    if (selectedProfileForApps != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedProfileForApps = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF121212),
            contentColor = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Select Apps to Clone into Profile ${selectedProfileForApps!!.name}", 
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Android handles the creation of a 'Work' tab in your app drawer. Any app you select here will be injected directly into that isolated secure folder.", 
                    fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoadingApps) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                } else {
                    LazyColumn {
                        items(installedApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch(Dispatchers.IO) {
                                            Toast.makeText(context, "Cloning ${app.name}...", Toast.LENGTH_SHORT).show()
                                            val installRes = ShizukuUtils.installExistingApp(selectedProfileForApps!!.id, app.packageName)
                                            withContext(Dispatchers.Main) {
                                                if (installRes.isSuccess) {
                                                    Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
                                                    ShizukuUtils.launchApp(context, selectedProfileForApps!!.id, app.packageName)
                                                } else {
                                                    Toast.makeText(context, "Failed: ${installRes.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var iconDrawable by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
                                LaunchedEffect(app.packageName) {
                                    withContext(Dispatchers.IO) {
                                        iconDrawable = try {
                                            context.packageManager.getApplicationIcon(app.packageName)
                                        } catch (e: Exception) { null }
                                    }
                                }
                                Image(
                                    painter = rememberAsyncImagePainter(model = iconDrawable),
                                    contentDescription = app.name,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.name, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(app.packageName, color = Color.Gray, fontSize = 10.sp)
                                }
                                Icon(Icons.Filled.Add, contentDescription = "Add", tint = NeonCyan)
                            }
                        }
                    }
                }
            }
        }
    }

    if (profileToDelete != null) {
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text(text = "Excluir Perfil?") },
            text = { Text("Tem certeza de que deseja excluir permanentemente o perfil \"${profileToDelete!!.name}\" e todos os seus dados? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    val profileId = profileToDelete!!.id
                    profileToDelete = null
                    scope.launch(Dispatchers.IO) {
                        Toast.makeText(context, "Excluindo perfil...", Toast.LENGTH_SHORT).show()
                        val out = ShizukuUtils.executeCommand("pm remove-user $profileId")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Perfil excluído. Saída: $out", Toast.LENGTH_SHORT).show()
                        }
                        refreshProfiles()
                    }
                }) {
                    Text("Excluir", color = com.example.ui.theme.DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Shizuku Header
        val statusColor = if (isShizukuAvailable && hasShizukuPermission) NeonCyan else com.example.ui.theme.DangerRed
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ElectricPurple.copy(alpha = 0.3f),
                            ElectricPurple.copy(alpha = 0.05f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = ElectricPurple.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isShizukuAvailable && hasShizukuPermission) "Ecosystem Online" else "Shizuku Not Connected",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Work profiles management via AM/PM commands.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Create Profile Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.3f),
                            NeonCyan.copy(alpha = 0.05f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = NeonCyan.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable {
                    if (!isShizukuAvailable || !hasShizukuPermission) {
                        Toast.makeText(context, "Shizuku is required.", Toast.LENGTH_SHORT).show()
                        return@clickable
                    }
                    scope.launch(Dispatchers.IO) {
                        Toast.makeText(context, "Criando perfil...", Toast.LENGTH_SHORT).show()
                        val res = ShizukuUtils.createWorkProfile("Shadow_Space_${System.currentTimeMillis() % 1000}")
                        withContext(Dispatchers.Main) {
                            if (res.isFailure) {
                                Toast.makeText(context, "Erro: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Perfil criado (ID: ${res.getOrNull()})!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        refreshProfiles()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, contentDescription = "Create", tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DEPLOY LOCAL ISOLATED SPACE", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // AI Persona Sync Profile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ElectricPurple.copy(alpha = 0.3f),
                            ElectricPurple.copy(alpha = 0.05f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = ElectricPurple.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable {
                    if (!isShizukuAvailable || !hasShizukuPermission) {
                        Toast.makeText(context, "Shizuku is required.", Toast.LENGTH_SHORT).show()
                        return@clickable
                    }
                    scope.launch(Dispatchers.IO) {
                        Toast.makeText(context, "Generating Synthetic Persona...", Toast.LENGTH_SHORT).show()
                        val jsonResponse = com.example.ai.AiIdentityGenerator(context).generateIdentity("Professional worker in a major tech hub")
                        
                        // Parse fakeName safely
                        var fakeName = "AI_Persona_${System.currentTimeMillis() % 1000}"
                        try {
                            if (jsonResponse.contains("{")) {
                                val jsonObj = org.json.JSONObject(jsonResponse)
                                fakeName = jsonObj.getString("fakeName").replace(" ", "_")
                                if (fakeName.isBlank()) fakeName = "AI_Persona"
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        
                        Toast.makeText(context, "Deploying persona space: $fakeName...", Toast.LENGTH_SHORT).show()

                        val res = ShizukuUtils.createWorkProfile(fakeName)
                        
                        if (res.isSuccess) {
                            val profileId = res.getOrNull()!!
                            // Save profile data in VaultManager
                            val vaultManager = com.example.data.VaultManager(context)
                            vaultManager.saveProfileConfig(profileId, jsonResponse)
                            
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Success! Secure config saved.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        refreshProfiles()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.GroupWork, contentDescription = "AI Sync", tint = ElectricPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DEPLOY WITH SYNTHETIC PERSONA", color = ElectricPurple, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (profiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isRefreshing) "Scanning sub-systems..." else "No active work profiles found.",
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            val categorizedProfiles = profiles.groupBy { profile ->
                profileConfigs[profile.id]?.category ?: "Uncategorized"
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                categorizedProfiles.forEach { (category, categoryProfiles) ->
                    item {
                        Text(
                            text = category,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(categoryProfiles) { profile ->
                        ProfileCard(
                            profile = profile,
                            jobTitle = profileConfigs[profile.id]?.jobTitle ?: "No Job Title",
                            onAction = { action ->
                                if (action == "apps") {
                                    selectedProfileForApps = profile
                                } else {
                                    scope.launch(Dispatchers.IO) {
                                        when (action) {
                                            "start" -> {
                                                Toast.makeText(context, "Iniciando perfil...", Toast.LENGTH_SHORT).show()
                                                ShizukuUtils.executeCommand("am start-user ${profile.id}")
                                            }
                                            "stop" -> {
                                                Toast.makeText(context, "Parando perfil...", Toast.LENGTH_SHORT).show()
                                                ShizukuUtils.executeCommand("am stop-user ${profile.id}")
                                            }
                                            "remove" -> {
                                                profileToDelete = profile
                                                return@launch
                                            }
                                        }
                                        refreshProfiles()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(profile: UserProfile, jobTitle: String, onAction: (String) -> Unit) {
    val isRunning = profile.status == "Running"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ElectricPurple.copy(alpha = 0.15f),
                        ElectricPurple.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = ElectricPurple.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ElectricPurple.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Work, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${profile.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(jobTitle, color = Color.Gray, fontSize = 12.sp)
            Text(
                text = "Status: ${profile.status}",
                color = if (isRunning) NeonCyan else Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = { onAction("apps") },
                modifier = Modifier.size(36.dp).background(NeonCyan.copy(alpha=0.1f), CircleShape)
            ) {
                Icon(Icons.Filled.Apps, contentDescription = "Manage Apps", tint = NeonCyan, modifier = Modifier.size(20.dp))
            }
            if (isRunning) {
                IconButton(
                    onClick = { onAction("stop") },
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha=0.1f), CircleShape)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            } else {
                IconButton(
                    onClick = { onAction("start") },
                    modifier = Modifier.size(36.dp).background(NeonCyan.copy(alpha=0.2f), CircleShape)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Start", tint = NeonCyan, modifier = Modifier.size(20.dp))
                }
            }
            IconButton(
                onClick = { onAction("remove") },
                modifier = Modifier.size(36.dp).background(com.example.ui.theme.DangerRed.copy(alpha=0.2f), CircleShape)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = com.example.ui.theme.DangerRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}
