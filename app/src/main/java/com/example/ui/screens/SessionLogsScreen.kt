package com.example.ui.screens

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.SessionLogEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import com.example.ui.theme.frostedGlass
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.ContentCopy
import android.widget.Toast

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.PlayArrow



class SessionLogsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).vaultDao()

    val logs: StateFlow<List<SessionLogEntity>> = dao.getAllSessionLogs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
}

@Composable
fun UptimeChart(logs: List<SessionLogEntity>) {
    if (logs.isEmpty()) return
    
    val connectionLogs = logs.filter { it.eventType == "CONNECTION" || it.eventType == "DISCONNECTION" }
        .sortedBy { it.timestamp }
        
    if (connectionLogs.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text("Linha do Tempo de Status", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .frostedGlass(cornerRadius = 12.dp)
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val startX = 0f
                val endX = size.width
                val centerY = size.height / 2
                
                // Draw background line (offline)
                drawLine(
                    color = com.example.ui.theme.DangerRed.copy(alpha = 0.5f),
                    start = Offset(startX, centerY),
                    end = Offset(endX, centerY),
                    strokeWidth = 12f,
                    cap = StrokeCap.Round
                )
                
                if (connectionLogs.isNotEmpty()) {
                    val firstTime = connectionLogs.first().timestamp
                    val now = System.currentTimeMillis()
                    val totalDuration = (now - firstTime).coerceAtLeast(1)
                    
                    var currentStatus = connectionLogs.first().eventType == "CONNECTION"
                    var lastEventTime = firstTime
                    
                    for (i in 0 until connectionLogs.size) {
                        val event = connectionLogs[i]
                        val nextTime = connectionLogs.getOrNull(i + 1)?.timestamp ?: now
                        
                        if (currentStatus) {
                            val startRatio = (lastEventTime - firstTime).toFloat() / totalDuration
                            val endRatio = (nextTime - firstTime).toFloat() / totalDuration
                            
                            val startXPos = (startRatio * size.width).coerceIn(0f, size.width)
                            val endXPos = (endRatio * size.width).coerceIn(0f, size.width)
                            
                            if (endXPos > startXPos) {
                                drawLine(
                                    color = com.example.ui.theme.NeonCyan,
                                    start = Offset(startXPos, centerY),
                                    end = Offset(endXPos, centerY),
                                    strokeWidth = 12f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                        
                        currentStatus = event.eventType == "CONNECTION"
                        lastEventTime = nextTime
                    }
                }
            }
        }
    }
}

@Composable
fun SessionLogsScreen(viewModel: SessionLogsViewModel = viewModel()) {
    val logs by viewModel.logs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 90.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val clipboardManager = LocalClipboardManager.current
            val context = LocalContext.current
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "LOGS DE AUDITORIA & SESSÃO",
                    style = MaterialTheme.typography.titleLarge,
                    color = com.example.ui.theme.NeonCyan,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    "Registros imutáveis de eventos do sistema e Shizuku",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            val scope = rememberCoroutineScope()
            IconButton(onClick = {
                scope.launch {
                    com.example.services.ChaosOsTester.runSelfTest(context)
                }
            }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Rodar Testes", tint = com.example.ui.theme.NeonCyan)
            }
            IconButton(onClick = {
                val allLogsText = logs.joinToString("\n") { log ->
                    val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    "[${log.eventType}] $timeStr - ${log.message}"
                }
                clipboardManager.setText(AnnotatedString(allLogsText))
                Toast.makeText(context, "Logs copiados para a área de transferência", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar Logs", tint = com.example.ui.theme.NeonCyan)
            }
        }

        UptimeChart(logs)
        
        if (logs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Nenhum registro de sessão salvo.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    SessionLogItem(log)
                }
            }
        }
    }
}

@Composable
fun SessionLogItem(log: SessionLogEntity) {
    val iconAndTint = when (log.eventType) {
        "ERROR", "TEST_ERROR" -> Pair(Icons.Filled.Warning, com.example.ui.theme.DangerRed)
        "CONNECTION", "DISCONNECTION" -> Pair(Icons.Filled.Info, com.example.ui.theme.NeonCyan)
        "PROFILE_CREATED", "PERMISSION", "TEST_SUCCESS", "SUCCESS" -> Pair(Icons.Filled.CheckCircle, com.example.ui.theme.ElectricPurple)
        else -> Pair(Icons.Filled.Info, Color.White)
    }
    
    val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val time = format.format(Date(log.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = iconAndTint.first,
            contentDescription = null,
            tint = iconAndTint.second,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "${log.eventType} • $time",
                color = iconAndTint.second,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = log.message,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
