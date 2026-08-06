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
        delay(800)
        onScanComplete()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scanner")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "outerRotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "innerRotation"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "textAlpha"
    )

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(20f, 30f)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(320.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radiusOuter = size.width / 2 - 20.dp.toPx()
            val radiusInner = size.width / 2 - 60.dp.toPx()

            rotate(outerRotation, center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(NeonCyan.copy(alpha=0.1f), NeonCyan, NeonCyan.copy(alpha=0.1f)),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - radiusOuter, center.y - radiusOuter),
                    size = Size(radiusOuter * 2, radiusOuter * 2),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
                
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
                        pathEffect = dashEffect
                    )
                )
                
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

            drawCircle(
                color = NeonCyan.copy(alpha = 0.3f * (pulse - 0.5f).coerceAtLeast(0f)),
                center = center,
                radius = radiusInner,
                blendMode = BlendMode.Screen
            )
        }

        Icon(
            imageVector = Icons.Filled.VpnKey,
            contentDescription = "Descriptografando",
            tint = NeonCyan,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    alpha = 0.8f + (pulse - 0.8f) 
                    shadowElevation = 25f * pulse
                    ambientShadowColor = NeonCyan
                    spotShadowColor = ElectricPurple
                }
        )

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
