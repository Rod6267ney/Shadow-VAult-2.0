package com.example.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.AiIdentityGenerator
import com.example.data.AppDatabase
import com.example.data.IdentityEntity
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.frostedGlass
import com.example.ui.theme.interactiveFrostedGlass
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class IdentitiesViewModel(application: Application) : AndroidViewModel(application) {
    private val generator = AiIdentityGenerator(application)

    var isGenerating by mutableStateOf(false)
        private set

    private val _identities = kotlinx.coroutines.flow.MutableStateFlow<List<IdentityEntity>>(emptyList())
    val identities: StateFlow<List<IdentityEntity>> = _identities

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(getApplication()).vaultDao()
                dao.insertIdentity(identity)
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
        Pair("Dev/Engenheiro", "Desenvolvedor de Software Sênior em São Paulo, focado em infraestrutura cloud e criptografia."),
        Pair("Executivo", "CEO Executivo em Nova York, viajante frequente de negócios internacionais."),
        Pair("Designer Freelancer", "Designer Gráfico em Lisboa, trabalhando remotamente com estúdios de arte europeus."),
        Pair("Pesquisador", "Investigador Acadêmico Anônimo em Berlim, especializado em análise de dados comportamentais."),
        Pair("Consultor InfoSec", "Auditor de Segurança Ofensiva, prestando consultoria em redes fechadas no Brasil.")
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(bottom = 80.dp)) {
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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = { Text("Descreva o perfil desejado (ex: Executivo em NY)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Presets Cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(quickPresets) { preset ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.3f)),
                    modifier = Modifier.width(200.dp).clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        prompt = preset.second
                    }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(preset.first, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(preset.second, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        com.example.ui.components.FrostedButton(
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
                    Text("Gerar Identidade Sintética (Gemini AI)", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (identities.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Nenhuma identidade sintética salva no vault.", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(identities) { identity ->
                    IdentityItem(identity, onUpdate = viewModel::updateIdentity)
                }
            }
        }
    }
}

@Composable
fun IdentityItem(identity: IdentityEntity, onUpdate: (IdentityEntity) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    var email by remember { mutableStateOf(identity.email) }
    var address by remember { mutableStateOf(identity.address) }
    var dob by remember { mutableStateOf(identity.dob) }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveFrostedGlass(16.dp, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                expanded = !expanded
            })
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Badge, contentDescription = "ID", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(identity.fakeName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${identity.jobTitle} • ${identity.location}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                if (!expanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (identity.email.isNotBlank()) {
                        Text(identity.email, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    }
                    if (identity.dob.isNotBlank()) {
                        Text("Nasc: ${identity.dob}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    }
                    if (identity.address.isNotBlank()) {
                        Text(identity.address, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Prompt: ${identity.profileIdea}", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val copyText = """
                    Nome: ${identity.fakeName}
                    Ocupação: ${identity.jobTitle}
                    Local: ${identity.location}
                    Email: ${identity.email}
                    Data Nasc: ${identity.dob}
                    Endereço: ${identity.address}
                """.trimIndent()
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(copyText))
                android.widget.Toast.makeText(context, "Identidade copiada para a área de transferência", android.widget.Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
            }
        }

        androidx.compose.animation.AnimatedVisibility(visible = expanded) {
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
                    horizontalArrangement = Arrangement.End
                ) {
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
                        Text("Salvar Alterações", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

