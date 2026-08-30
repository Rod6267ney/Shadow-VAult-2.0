package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultSurface

@Composable
fun StorageVisualizerScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Storage, contentDescription = "Armazenamento", tint = NeonCyan, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Armazenamento Vault", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 20.dp.toPx()
                drawArc(
                    color = VaultSurface,
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )
                
                // Representing App Data (NeonCyan)
                drawArc(
                    color = NeonCyan,
                    startAngle = 140f,
                    sweepAngle = 160f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )
                
                // Representing Clones (ElectricPurple)
                drawArc(
                    color = ElectricPurple,
                    startAngle = 140f + 160f,
                    sweepAngle = 60f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("8.4 GB", color = NeonCyan, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Usado de 16 GB", color = Color.Gray, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        StorageLegendItem(color = NeonCyan, title = "Apps Clonados", sizeStr = "5.2 GB")
        Spacer(modifier = Modifier.height(12.dp))
        StorageLegendItem(color = ElectricPurple, title = "Mídia & Arquivos", sizeStr = "2.1 GB")
        Spacer(modifier = Modifier.height(12.dp))
        StorageLegendItem(color = VaultSurface, title = "Livre", sizeStr = "7.6 GB")

        Spacer(modifier = Modifier.height(32.dp))

        val context = androidx.compose.ui.platform.LocalContext.current
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        var isCleaning by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        androidx.compose.material3.Button(
            onClick = {
                isCleaning = true
                com.example.utils.HapticEngine.vibrateSuccess(context)
                scope.launch {
                    com.example.utils.ShizukuUtils.trimAllCaches()
                    kotlinx.coroutines.delay(1000)
                    isCleaning = false
                    android.widget.Toast.makeText(context, "🧹 Todos os caches foram limpos com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = NeonCyan.copy(alpha = 0.2f),
                contentColor = NeonCyan
            ),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isCleaning
        ) {
            androidx.compose.material3.Icon(
                Icons.Filled.DeleteSweep,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.material3.Text(
                if (isCleaning) "LIMPANDO CACHES..." else "LIMPAR TODOS OS CACHES",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun StorageLegendItem(color: Color, title: String, sizeStr: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(color))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, color = Color.White)
        }
        Text(sizeStr, color = Color.Gray)
    }
}
