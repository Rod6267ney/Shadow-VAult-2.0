package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.frostedGlass
import com.example.ui.viewmodel.WorkspaceCreationState
import com.example.ui.viewmodel.WorkspaceViewModel
import kotlinx.coroutines.delay

@Composable
fun WorkspaceCreationWizard(
    workspaceViewModel: WorkspaceViewModel,
    globalVpnEnabled: Boolean,
    globalProxyRegion: String,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val creationState by workspaceViewModel.creationState.collectAsState()
    val view = LocalView.current
    var step by remember { mutableIntStateOf(1) }
    val totalSteps = 4

    // Step 1 State
    var workspaceName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF6200EE)) } // Default Purple
    var selectedIcon by remember { mutableStateOf("Work") }

    // Step 2 State (Engine Type)
    var engineType by remember { mutableStateOf("VIRTUAL") }

    val hapticFeedback = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }

    // Full-screen View instead of a Modal Dialog
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .frostedGlass(16.dp)
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountTree, contentDescription = null, tint = selectedColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Criar Workspace", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("Etapa $step de $totalSteps", color = selectedColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            val progress = step.toFloat() / totalSteps.toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = selectedColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300)))
                        }
                    },
                    label = "stepper_animation"
                ) { targetStep ->
                    when (targetStep) {
                        1 -> StepIdentity(
                            workspaceName, selectedColor, selectedIcon,
                            { workspaceName = it }, { selectedColor = it }, { selectedIcon = it },
                            onDismiss,
                            { hapticFeedback(); step = 2 }
                        )
                        2 -> StepLimits(1, 3, onDismiss) { hapticFeedback(); step = 3 }
                        3 -> StepEngineType(
                            engineType,
                            { engineType = it },
                            onDismiss,
                            {
                                hapticFeedback()
                                step = 4
                                workspaceViewModel.provisionWorkspaceWithIdentity(
                                    workspaceName = workspaceName,
                                    workspaceType = engineType,
                                    iconName = selectedIcon,
                                    unlimitedClones = false,
                                    useResidentialProxy = false,
                                    selectedProxyRegion = "BR",
                                    killSwitchEnabled = false,
                                    hardwareSpoofingEnabled = false,
                                    burnerModeEnabled = false,
                                    fakeGpsRegion = "",
                                    personaArchetype = "Anônimo",
                                    generateComplexPasswords = false,
                                    onComplete = { onComplete() }
                                )
                            }
                        )
                        4 -> StepBuild(creationState, onComplete, onDismiss, selectedColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIdentity(
    name: String, color: Color, icon: String,
    onNameChange: (String) -> Unit, onColorChange: (Color) -> Unit, onIconChange: (String) -> Unit,
    onCancel: () -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Identidade Oculta (Configuração)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            label = { Text("Nome da Pasta/Cofre", color = Color.Gray) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = color, unfocusedTextColor = Color.White)
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Cor de Destaque", color = Color.White, fontSize = 14.sp)
        LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
            items(listOf(Color(0xFF6200EE), Color(0xFF00FFCC), Color(0xFFFF0055), Color(0xFFFFFF00))) { c ->
                Box(
                    modifier = Modifier.size(48.dp).padding(4.dp).clip(CircleShape).background(c).border(2.dp, if (color == c) Color.White else Color.Transparent, CircleShape).clickable { onColorChange(c) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Ícone Disfarçado", color = Color.White, fontSize = 14.sp)
        val icons = listOf("Work" to Icons.Filled.Work, "Calculator" to Icons.Filled.Calculate, "Notes" to Icons.Filled.Notes)
        LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
            items(icons) { (iconName, img) ->
                Box(
                    modifier = Modifier.size(56.dp).padding(4.dp).clip(RoundedCornerShape(12.dp)).background(if (icon == iconName) color.copy(alpha = 0.2f) else Color.Transparent).border(1.dp, if (icon == iconName) color else Color.Gray, RoundedCornerShape(12.dp)).clickable { onIconChange(iconName) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(img, contentDescription = null, tint = if (icon == iconName) color else Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                Text("Cancelar")
            }
            Button(onClick = onNext, enabled = name.isNotBlank(), modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = color)) {
                Text("Avançar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StepLimits(used: Int, max: Int, onCancel: () -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Medidor de Slots do Android", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("O Kernel do seu aparelho limita nativamente o número de Workspaces independentes. Para ter Clones Infinitos, use o modo 'Celular Virtual'.", color = Color.Gray, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Visual Meter
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            for (i in 1..max) {
                val isUsed = i <= used
                Box(
                    modifier = Modifier.size(60.dp).padding(4.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (isUsed) DangerRed else NeonCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isUsed) Icons.Filled.Lock else Icons.Filled.LockOpen, contentDescription = null, tint = Color.Black)
                }
            }
        }
        Text(" slots disponíveis", color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp))

        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                Text("Cancelar")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                Text("Prosseguir", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StepEngineType(type: String, onTypeChange: (String) -> Unit, onCancel: () -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Tipo de Motor (Hypervisor)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        val levels = listOf(
            "VIRTUAL" to "ChaosSpace (Sem Root): Motor isolado no estilo VMOS. Contorna totalmente bloqueios da fabricante.",
            "WORK_PROFILE" to "Nativo (Shizuku): Requer liberação via ADB. Mais rápido, mas pode ser bloqueado pela sua ROM."
        )
        
        levels.forEach { (engineCode, desc) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)).clickable { onTypeChange(engineCode) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = type == engineCode, onClick = { onTypeChange(engineCode) }, colors = RadioButtonDefaults.colors(selectedColor = NeonCyan))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(if (engineCode == "VIRTUAL") "Celular Virtual (Recomendado)" else "Perfil de Trabalho Nativo", color = if (type == engineCode) NeonCyan else Color.White, fontWeight = FontWeight.Bold)
                    Text(desc, color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                Text("Parar/Cancelar")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                Text("Construir Cofre", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StepBuild(state: WorkspaceCreationState, onComplete: () -> Unit, onDismiss: () -> Unit, color: Color) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        when (state) {
            is WorkspaceCreationState.Idle -> {
                CircularProgressIndicator(color = color)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Iniciando comandos PM Shell...", color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Text("Parar Operação")
                }
            }
            is WorkspaceCreationState.Loading -> {
                CircularProgressIndicator(color = color)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Provisionando Usuário Isolado no Android...", color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Text("Parar Operação")
                }
            }
            is WorkspaceCreationState.Success -> {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Workspace Criado com Sucesso!", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onComplete, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = color)) {
                    Text("Acessar Cofre", color = Color.Black)
                }
            }
            is WorkspaceCreationState.Error -> {
                Icon(Icons.Filled.Error, contentDescription = null, tint = DangerRed, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Falha na Criação", color = DangerRed, fontWeight = FontWeight.Bold)
                Text(state.errorMessage, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top=8.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                    Text("Sair")
                }
            }
        }
    }
}
