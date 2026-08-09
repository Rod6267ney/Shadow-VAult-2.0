package com.example.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.AiIdentityGenerator
import com.example.data.AppDatabase
import com.example.data.IdentityEntity
import com.example.ui.components.FrostedButton
import com.example.ui.components.SettingsSection
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.interactiveFrostedGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class IdentitiesViewModel(application: Application) : AndroidViewModel(application) {
    private val generator = AiIdentityGenerator(application)

    var isGenerating by mutableStateOf(false)
        private set

    private val _identities = MutableStateFlow<List<IdentityEntity>>(emptyList())
    val identities: StateFlow<List<IdentityEntity>> = _identities

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(application).vaultDao()
                dao.getAllIdentities().collect {
                    _identities.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateIdentity(identity: IdentityEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(getApplication()).vaultDao()
                dao.insertIdentity(identity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteIdentity(identity: IdentityEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(getApplication()).vaultDao()
                dao.deleteIdentity(identity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generateIdentity(prompt: String) {
        val finalPrompt = if (prompt.isBlank()) "Persona Anônima Aleatória para Privacidade" else prompt
        viewModelScope.launch {
            isGenerating = true
            try {
                val jsonStr = generator.generateIdentity(finalPrompt)
                val cleanJsonStr = jsonStr.replace("```json", "").replace("```", "").trim()
                val jsonObj = JSONObject(cleanJsonStr)
                val identity = IdentityEntity(
                    fakeName = jsonObj.optString("fakeName", "Identidade Anônima"),
                    jobTitle = jsonObj.optString("jobTitle", "Consultor Indoc"),
                    location = jsonObj.optString("location", "São Paulo, BR"),
                    dob = jsonObj.optString("dob", "1990-01-01"),
                    address = jsonObj.optString("address", "Av. Paulista, 1000"),
                    email = jsonObj.optString("email", "shadow.persona@vault.sec"),
                    profileIdea = finalPrompt
                )
                val dao = AppDatabase.getDatabase(getApplication()).vaultDao()
                dao.insertIdentity(identity)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isGenerating = false
            }
        }
    }
}

@Composable
fun IdentitiesScreen(activeInstance: com.example.data.WorkspaceConfig?, viewModel: IdentitiesViewModel = viewModel()) {
    val identities by viewModel.identities.collectAsState()
    var prompt by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    val quickPresets = listOf(
        Pair("Jornalista", "Jornalista Investigativo em Londres, cobrindo segurança nacional."),
        Pair("Fantasma Corporativo", "Consultor Anônimo em Singapura, trabalhando remotamente com fusões."),
        Pair("Investigador", "Pesquisador Open Source em Berlim, focado em análise de dados públicos.")
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxHeight()
                .padding(16.dp)
                .padding(bottom = 80.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (activeInstance != null) {
                Text(
                    "Credenciais Criptografadas: ${activeInstance.name}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Encrypted Storage Banner Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Protegido por Room Database Criptografado com SQLCipher AES-256",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(
                title = "Nova Identidade",
                icon = Icons.Filled.PersonAdd,
                iconTint = NeonCyan
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Descreva o perfil desejado") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Prompts Inteligentes:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickPresets) { preset ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.3f)),
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    prompt = preset.second
                                }
                        ) {
                            Text(
                                preset.first,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FrostedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.generateIdentity(prompt)
                        prompt = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (viewModel.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gerando Identidade via Gemini AI...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gerar Identidade Sintética", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (identities.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Nenhuma identidade sintética salva no vault.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    identities.forEach { identity ->
                        IdentityItem(
                            identity = identity,
                            onUpdate = viewModel::updateIdentity,
                            onDelete = viewModel::deleteIdentity
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IdentityItem(identity: IdentityEntity, onUpdate: (IdentityEntity) -> Unit, onDelete: (IdentityEntity) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    var email by remember { mutableStateOf(identity.email) }
    var address by remember { mutableStateOf(identity.address) }
    var dob by remember { mutableStateOf(identity.dob) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Destruir Identidade") },
            text = { Text("Tem certeza? Linked clones will be orphaned. Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(identity)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                ) {
                    Text("Destruir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveFrostedGlass(16.dp, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                expanded = !expanded
            })
            .padding(16.dp)
    ) {
        // Avatar Holográfico
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(NeonCyan, ElectricPurple)),
                        shape = CircleShape
                    )
                    .background(Color.DarkGray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Avatar Holográfico",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(identity.fakeName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(identity.fakeName))
                            Toast.makeText(context, "Nome copiado", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Name", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
                Text("${identity.jobTitle}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(identity.location, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(identity.location))
                            Toast.makeText(context, "Localização copiada", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Location", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!expanded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(identity.email, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(identity.email))
                        Toast.makeText(context, "Email copiado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp).padding(start = 4.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Email", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
            if (identity.dob.isNotBlank()) {
                Text("Nasc: ${identity.dob}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
            if (identity.address.isNotBlank()) {
                Text(identity.address, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
            }
            
            // Mock fields
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ID/Passport: ❖❖❖❖-MOCK", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
                Text("CC: **** **** **** 4021", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text("Prompt: ${identity.profileIdea}", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Sintético") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Data de Nascimento") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Endereço Sintético") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showDeleteDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("Destruir Identidade", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Button(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUpdate(identity.copy(
                                email = email,
                                dob = dob,
                                address = address
                            ))
                            expanded = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
