package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.utils.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PlayArrow
import kotlinx.coroutines.flow.first

@Composable
fun OverviewScreen() {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var totalProfiles by remember { mutableStateOf(0) }
    var activeProfiles by remember { mutableStateOf(0) }
    var profiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val shizukuStatus = com.example.utils.useShizukuStatus()

    LaunchedEffect(refreshTrigger) {
        scope.launch(Dispatchers.IO) {
            if (shizukuStatus.first && shizukuStatus.second) {
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

                withContext(Dispatchers.Main) {
                    profiles = parsedProfiles
                    totalProfiles = parsedProfiles.size
                    activeProfiles = parsedProfiles.count { it.status == "Running" }
                    isLoading = false
                }
            } else {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Visão Geral",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                title = "Total de Perfis",
                value = totalProfiles.toString(),
                icon = Icons.Filled.GroupWork,
                color = ElectricPurple,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Ativos",
                value = activeProfiles.toString(),
                icon = Icons.Filled.Person,
                color = NeonCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Perfis Gerenciados",
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Carregando informações...", color = Color.Gray)
            }
        } else if (profiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum perfil encontrado.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(profiles) { profile ->
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    if (showDeleteDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Confirmar Exclusão", color = NeonCyan) },
                            text = { Text("Tem certeza que deseja excluir o workspace ${profile.id}? Isso apagará todos os dados e apps deste espaço.", color = Color.White) },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    showDeleteDialog = false
                                    scope.launch {
                                        android.widget.Toast.makeText(context, "Deletando ${profile.id}...", android.widget.Toast.LENGTH_SHORT).show()
                                        val res = withContext(Dispatchers.IO) {
                                            ShizukuUtils.executeCommand("pm remove-user ${profile.id}")
                                        }
                                        if (res.contains("Error") || res.contains("Exception")) {
                                            android.widget.Toast.makeText(context, "Erro: $res", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Limpar clones do banco de dados para não ficarem "órfãos"
                                            withContext(Dispatchers.IO) {
                                                try {
                                                    val dao = com.example.data.AppDatabase.getDatabase(context.applicationContext as android.app.Application).vaultDao()
                                                    val allClones = dao.getAllClones().first()
                                                    val clonesToRemove = allClones.filter { it.userId == profile.id }
                                                    for (clone in clonesToRemove) {
                                                        dao.deleteClone(clone)
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                            android.widget.Toast.makeText(context, "Espaço excluído", android.widget.Toast.LENGTH_SHORT).show()
                                            refreshTrigger++
                                        }
                                    }
                                }) {
                                    Text("Excluir", color = com.example.ui.theme.DangerRed)
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancelar", color = Color.Gray)
                                }
                            },
                            containerColor = Color(0xFF1E1E2E)
                        )
                    }

                    ProfileMiniCard(
                        profile = profile,
                        onDelete = {
                            showDeleteDialog = true
                        },
                        onStart = {
                            scope.launch(Dispatchers.IO) {
                                val res = ShizukuUtils.executeCommand("am start-user ${profile.id}")
                                withContext(Dispatchers.Main) {
                                    if (res.contains("Error") || res.contains("Exception")) {
                                        android.widget.Toast.makeText(context, "Erro: $res", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Espaço iniciado", android.widget.Toast.LENGTH_SHORT).show()
                                        refreshTrigger++
                                    }
                                }
                            }
                        },
                        onStop = {
                            scope.launch(Dispatchers.IO) {
                                val res = ShizukuUtils.executeCommand("am stop-user ${profile.id}")
                                withContext(Dispatchers.Main) {
                                    if (res.contains("Error") || res.contains("Exception")) {
                                        android.widget.Toast.makeText(context, "Erro: $res", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Espaço parado", android.widget.Toast.LENGTH_SHORT).show()
                                        refreshTrigger++
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(120.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Column {
                Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text(text = title, color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ProfileMiniCard(profile: UserProfile, onDelete: () -> Unit, onStart: () -> Unit, onStop: () -> Unit) {
    val isRunning = profile.status == "Running"
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (isRunning) NeonCyan else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "Rodando" else "Parado",
                        color = if (isRunning) NeonCyan else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    if (isRunning) {
                        androidx.compose.material3.IconButton(
                            onClick = onStop,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "Parar Workspace", tint = Color.LightGray)
                        }
                    } else {
                        androidx.compose.material3.IconButton(
                            onClick = onStart,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Iniciar Workspace", tint = NeonCyan)
                        }
                    }
                    androidx.compose.material3.IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = com.example.ui.theme.DangerRed)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = profile.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ID: ${profile.id}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}
