package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

import com.example.ui.components.AnimatedMeshBackground

@Composable
fun AmbientBackground(content: @Composable () -> Unit) {
    val bgColor = MaterialTheme.colorScheme.background
    val isDark = bgColor.luminance() < 0.5f

    val topGlow = if (isDark) ElectricPurple.copy(alpha = 0.2f) else Color(0xFFC084FC).copy(alpha = 0.25f)
    val rightGlow = if (isDark) NeonCyan.copy(alpha = 0.15f) else Color(0xFF38BDF8).copy(alpha = 0.2f)
    val bottomGlow = if (isDark) DangerRed.copy(alpha = 0.1f) else Color(0xFFF87171).copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top Left Glow
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(topGlow, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(x = 0f, y = height * 0.1f),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = androidx.compose.ui.geometry.Offset(x = 0f, y = height * 0.1f)
            )

            // Right Glow
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(rightGlow, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(x = width, y = height * 0.4f),
                    radius = width * 0.5f
                ),
                radius = width * 0.5f,
                center = androidx.compose.ui.geometry.Offset(x = width, y = height * 0.4f)
            )

            // Bottom Left Glow
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(bottomGlow, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(x = width * 0.2f, y = height),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = androidx.compose.ui.geometry.Offset(x = width * 0.2f, y = height)
            )
        }
        
        if (isDark) {
            AnimatedMeshBackground()
        }
        
        content()
    }
}

