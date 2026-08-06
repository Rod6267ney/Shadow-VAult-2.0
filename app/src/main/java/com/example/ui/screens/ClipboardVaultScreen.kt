package com.example.ui.screens

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ClipboardEntity
import com.example.data.WorkspaceConfig
import com.example.services.ClipboardSanitizer
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.DangerRed
import com.example.utils.ClipboardSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipboardViewModel(application: Application) : AndroidViewModel(application) {
    private val _clipboardItems = MutableStateFlow<List<ClipboardEntity>>(emptyList())
    val clipboardItems: StateFlow<List<ClipboardEntity>> = _clipboardItems

    fun loadItems(context: Context, workspaceId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(getApplication<Application>()).vaultDao()
                val isIsolated = ClipboardSettings.isIsolationEnabled(context)
                
                val flow = if (isIsolated && workspaceId != null) {
                    dao.getClipboardItemsForWorkspace(workspaceId)
                } else if (isIsolated) {
                    dao.getGlobalClipboardItems()
                } else {
                    dao.getAllClipboardItems()
                }
                
                flow.collect {
                    _clipboardItems.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveText(context: Context, text: String, workspaceId: String? = null, source: String = "Chaos OS Vault") {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ClipboardSanitizer.onNewTextCopied(
                    context = context,
                    text = text,
                    workspaceId = workspaceId,
                    sourceApp = source
                )

                // Copy to system clipboard
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = android.content.ClipData.newPlainText("ChaosVault", text)
                clipboard.setPrimaryClip(clip)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Snippet salvo no Cofre SQLCipher e copiado!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteItem(context: Context, item: ClipboardEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(context).vaultDao()
                dao.deleteClipboardItem(item)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAll(context: Context, workspaceId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(context).vaultDao()
                val isIsolated = ClipboardSettings.isIsolationEnabled(context)
                if (isIsolated && workspaceId != null) {
                    dao.clearClipboardVaultForWorkspace(workspaceId)
                } else {
                    dao.clearClipboardVault()
                }
                withContext(Dispatchers.Main) {
                    ClipboardSanitizer.sanitizeClipboard(context, notifyUser = true, reason = "Limpeza manual do cofre")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("ChaosVault", text)
        clipboard.setPrimaryClip(clip)
        
        val timeoutSec = ClipboardSettings.getAutoClearTimeoutSeconds(context)
        if (timeoutSec > 0) {
            ClipboardSanitizer.scheduleAutoClear(context, timeoutSec)
        }
        
        Toast.makeText(context, "Copiado! Higienização automática agendada em ${timeoutSec}s.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ClipboardVaultScreen(
    activeInstance: WorkspaceConfig? = null,
    viewModel: ClipboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val items by viewModel.clipboardItems.collectAsState()
    var newText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    var isIsolationEnabled by remember { mutableStateOf(ClipboardSettings.isIsolationEnabled(context)) }
    var autoClearTimeoutSec by remember { mutableStateOf(ClipboardSettings.getAutoClearTimeoutSeconds(context)) }
    var isClearOnBackground by remember { mutableStateOf(ClipboardSettings.isClearOnBackgroundEnabled(context)) }
    var showSettingsCard by remember { mutableStateOf(false) }

    LaunchedEffect(activeInstance?.id, isIsolationEnabled) {
        viewModel.loadItems(context, activeInstance?.id)
        ClipboardSanitizer.startMonitoring(context)
    }

    val filteredItems = items.filter { 
        it.copiedText.contains(searchQuery, ignoreCase = true) || 
        it.sourceApp.contains(searchQuery, ignoreCase = true) 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "COFRE DE CLIPBOARD & SNIPPETS",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Instância Ativa: ${activeInstance?.name ?: "Global"} • SQLCipher AES-256",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showSettingsCard = !showSettingsCard }) {
                    Icon(Icons.Filled.Tune, contentDescription = "Configurações de Isolação", tint = NeonCyan)
                }
                if (items.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearAll(context, activeInstance?.id) }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Limpar Tudo", tint = DangerRed)
                    }
                }
            }
        }

        // Isolation Status Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isIsolationEnabled) ElectricPurple.copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isIsolationEnabled) ElectricPurple.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isIsolationEnabled) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = null,
                        tint = if (isIsolationEnabled) NeonCyan else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = if (isIsolationEnabled) "ISOLAÇÃO DE CLIPBOARD ATIVA" else "ISOLAÇÃO DESATIVADA (MODO GLOBAL)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isIsolationEnabled) "Snippets restritos ao workspace [${activeInstance?.name ?: "Global"}]" else "Histórico compartilhado entre todas as instâncias",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
                Switch(
                    checked = isIsolationEnabled,
                    onCheckedChange = { enabled ->
                        isIsolationEnabled = enabled
                        ClipboardSettings.setIsolationEnabled(context, enabled)
                        Toast.makeText(
                            context,
                            if (enabled) "🔒 Isolação de Clipboard Ativada!" else "🔓 Isolação Desativada!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = ElectricPurple.copy(alpha = 0.5f)
                    )
                )
            }
        }

        // Expanded Settings Panel
        if (showSettingsCard) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("HIGIENIZAÇÃO AUTOMÁTICA DA ÁREA DE TRANSFERÊNCIA", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Limpar ao colocar app em 2º plano:", color = Color.White, fontSize = 12.sp)
                        Switch(
                            checked = isClearOnBackground,
                            onCheckedChange = {
                                isClearOnBackground = it
                                ClipboardSettings.setClearOnBackgroundEnabled(context, it)
                            }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tempo de Auto-Destruição da Área de Transferência: ${autoClearTimeoutSec}s", color = Color.White, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(10, 30, 60, 0).forEach { timeoutSec ->
                                FilterChip(
                                    selected = autoClearTimeoutSec == timeoutSec,
                                    onClick = {
                                        autoClearTimeoutSec = timeoutSec
                                        ClipboardSettings.setAutoClearTimeoutSeconds(context, timeoutSec)
                                    },
                                    label = {
                                        Text(if (timeoutSec == 0) "Desativado" else "${timeoutSec}s", fontSize = 11.sp)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            ClipboardSanitizer.sanitizeClipboard(context, notifyUser = true, reason = "Limpeza manual instantânea")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Higienizar Área de Transferência Agora", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Input Card to add custom snippet
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    label = { Text("Novo snippet / texto para o cofre...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            if (newText.isNotBlank()) {
                                viewModel.saveText(context, newText, activeInstance?.id)
                                newText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salvar e Copiar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Pesquisar no histórico de snippets...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = NeonCyan) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // List of items
        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum snippet salvo para esta instância.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = item.sourceApp,
                                        color = ElectricPurple,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = item.copiedText,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { viewModel.copyToClipboard(context, item.copiedText) }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Usar / Copiar", color = NeonCyan)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { viewModel.deleteItem(context, item) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Excluir", color = DangerRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
