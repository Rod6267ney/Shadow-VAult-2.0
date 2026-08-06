package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedMeshBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Blob 1 - Neon Cyan
        // Usamos graphicsLayer para mover o elemento na GPU (Hardware Accelerated),
        // em vez de recriar Shaders ou recalcular desenhos na CPU a cada quadro.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = (sin(phase) * size.width * 0.4).toFloat()
                    translationY = (cos(phase * 1.3) * size.height * 0.2).toFloat()
                }
                .drawBehind {
                    // O desenho e o Gradiente são compilados apenas 1x
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        radius = size.width * 0.8f
                    )
                }
        )

        // Blob 2 - Electric Purple
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = (cos(phase * 0.8) * size.width * 0.3).toFloat()
                    translationY = (sin(phase * 1.5) * size.height * 0.3).toFloat()
                }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ElectricPurple.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        radius = size.width * 0.8f
                    )
                }
        )
        
        // Blob 3 - White Highlight soft overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = (sin(phase * 1.7) * size.width * 0.3).toFloat()
                    translationY = (cos(phase * 0.9) * size.height * 0.3).toFloat()
                }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
                        ),
                        radius = size.width * 0.6f
                    )
                }
        )
    }
}
