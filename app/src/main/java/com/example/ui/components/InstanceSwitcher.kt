package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkspaceConfig
import com.example.services.ClipboardSanitizer
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ElectricPurple
import com.example.utils.ClipboardSettings
import com.example.utils.ShizukuUtils

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.draw.clip
import com.example.utils.BiometricAuthHelper

@Composable
fun InstanceSwitcher(
    activeInstance: WorkspaceConfig?,
    onInstanceSelected: (WorkspaceConfig?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var instances by remember { mutableStateOf<List<WorkspaceConfig>>(emptyList()) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var previousWorkspaceId by remember { mutableStateOf<String?>(activeInstance?.id) }
    var isIsolationEnabled by remember { mutableStateOf(ClipboardSettings.isIsolationEnabled(context)) }

    LaunchedEffect(activeInstance?.id) {
        if (activeInstance?.id != previousWorkspaceId) {
            val oldId = previousWorkspaceId
            previousWorkspaceId = activeInstance?.id
            ClipboardSanitizer.sanitizeAndWipeForWorkspaceSwitch(
                context = context,
                oldWorkspaceId = oldId,
                newWorkspaceId = activeInstance?.id
            )
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            instances = ShizukuUtils.getWorkspaces(context)
            isIsolationEnabled = ClipboardSettings.isIsolationEnabled(context)
        }
    }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    expanded = true
                }
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            // Real-time status indicator dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (activeInstance != null) Color(0xFF00FF88) else NeonCyan)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.Domain,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = activeInstance?.name ?: "Todas as Instâncias",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Switch",
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF1E1E1E))
                .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .width(260.dp)
        ) {
            // Clipboard Isolation Toggle inside Dropdown Menu
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Isolação de Clipboard",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                if (isIsolationEnabled) "Restrito por Workspace" else "Compartilhado Global",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
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
                },
                onClick = {},
                leadingIcon = {
                    Icon(
                        imageVector = if (isIsolationEnabled) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = null,
                        tint = if (isIsolationEnabled) NeonCyan else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            HorizontalDivider(color = Color.DarkGray)

            DropdownMenuItem(
                text = { Text("Todas as Instâncias", color = if (activeInstance == null) NeonCyan else Color.White) },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (activeInstance != null) {
                        ClipboardSanitizer.sanitizeAndWipeForWorkspaceSwitch(
                            context = context,
                            oldWorkspaceId = activeInstance.id,
                            newWorkspaceId = null
                        )
                    }
                    onInstanceSelected(null)
                    expanded = false
                }
            )

            HorizontalDivider(color = Color.DarkGray)

            instances.forEach { instance ->
                val isSelected = activeInstance?.id == instance.id
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    instance.name,
                                    color = if (isSelected) NeonCyan else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("ID: ${instance.id} • ${instance.fakeName}", color = Color.Gray, fontSize = 10.sp)
                            }
                            // Status indicator badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF00FF88) else Color(0xFF00E5FF))
                                )
                                Text(
                                    if (isSelected) "ATIVO" else "ONLINE",
                                    color = if (isSelected) Color(0xFF00FF88) else Color(0xFF00E5FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        expanded = false
                        // Require biometric authentication to switch instance
                        BiometricAuthHelper.authenticate(
                            context = context,
                            title = "Trocar para ${instance.name}",
                            subtitle = "Autentique com biometria para acessar este Perfil de Trabalho",
                            onSuccess = {
                                if (activeInstance?.id != instance.id) {
                                    ClipboardSanitizer.sanitizeAndWipeForWorkspaceSwitch(
                                        context = context,
                                        oldWorkspaceId = activeInstance?.id,
                                        newWorkspaceId = instance.id
                                    )
                                }
                                onInstanceSelected(instance)
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Work, contentDescription = null, tint = if (isSelected) NeonCyan else ElectricPurple, modifier = Modifier.size(20.dp))
                    }
                )
            }
        }
    }
}
