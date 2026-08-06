package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ElectricPurple

@Composable
fun HolographicScannerScreen(onScanComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3500)
        onScanComplete()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scanner")

    // Rotação do Anel Externo
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "outerRotation"
    )

    // Rotação do Anel Interno (direção oposta)
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "innerRotation"
    )
    
    // Efeito de Pulso para o Ícone Central
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // Opacidade para o texto piscar suavemente
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "textAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Fundo escuro absoluto para destaque dos neons
        contentAlignment = Alignment.Center
    ) {
        // Canvas Complexo para os Anéis e Partículas
        Canvas(modifier = Modifier.size(320.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radiusOuter = size.width / 2 - 20.dp.toPx()
            val radiusInner = size.width / 2 - 60.dp.toPx()

            // Anel externo segmentado (Gradiente Neon)
            rotate(outerRotation, center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(NeonCyan.copy(alpha=0.1f), NeonCyan, NeonCyan.copy(alpha=0.1f))
                    ),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - radiusOuter, center.y - radiusOuter),
                    size = Size(radiusOuter * 2, radiusOuter * 2),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Detalhe grosso roxo girando junto
                drawArc(
                    color = ElectricPurple.copy(alpha = 0.8f),
                    startAngle = 300f,
                    sweepAngle = 40f,
                    useCenter = false,
                    topLeft = Offset(center.x - radiusOuter, center.y - radiusOuter),
                    size = Size(radiusOuter * 2, radiusOuter * 2),
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Anel interno tecnológico (Traçado tracejado)
            rotate(innerRotation, center) {
                drawArc(
                    color = ElectricPurple,
                    startAngle = 45f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - radiusInner, center.y - radiusInner),
                    size = Size(radiusInner * 2, radiusInner * 2),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 30f))
                    )
                )
                
                // Arco de foco cyan girando no sentido oposto
                drawArc(
                    color = NeonCyan.copy(alpha = 0.9f),
                    startAngle = 250f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(center.x - radiusInner, center.y - radiusInner),
                    size = Size(radiusInner * 2, radiusInner * 2),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Brilho ambiente central (pulsante)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.3f * (pulse - 0.5f)), Color.Transparent),
                    center = center,
                    radius = radiusInner
                ),
                center = center,
                radius = radiusInner,
                blendMode = BlendMode.Screen
            )
        }

        // Ícone Animado Central (Chave / Cadeado)
        Icon(
            imageVector = Icons.Filled.VpnKey,
            contentDescription = "Descriptografando",
            tint = NeonCyan,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    alpha = 0.8f + (pulse - 0.8f) // Varia de 0.8 a 1.2 na opacidade virtual
                    shadowElevation = 25f * pulse
                    ambientShadowColor = NeonCyan
                    spotShadowColor = ElectricPurple
                }
        )

        // Textos de Status
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DESCRIPTOGRAFANDO VAULT",
                color = NeonCyan,
                style = MaterialTheme.typography.titleMedium.copy(
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.graphicsLayer { alpha = textAlpha }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "ESTABELECENDO CONEXÃO SEGURA...",
                color = ElectricPurple.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}
