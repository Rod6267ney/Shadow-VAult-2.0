package com.example.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance

class LiquidRipple(val position: Offset) {
    val radius = Animatable(0f)
    val alpha = Animatable(0.8f)
}

fun Modifier.frostedGlass(
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp
) = composed {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) VaultSurface else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) GlassBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(surfaceColor)
        .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
}

fun Modifier.interactiveFrostedGlass(
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    color: Color? = null,
    onClick: () -> Unit
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val effectiveColor = color ?: if (isDark) Color.White else MaterialTheme.colorScheme.primary

    val alpha = if (isPressed) 0.25f else if (isHovered) 0.15f else 0.08f
    val borderAlpha = if (isPressed) 0.6f else if (isHovered) 0.4f else 0.25f

    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(if (isDark) effectiveColor.copy(alpha = alpha) else MaterialTheme.colorScheme.surface)
        .border(
            borderWidth,
            if (isDark) effectiveColor.copy(alpha = borderAlpha) else MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha),
            RoundedCornerShape(cornerRadius)
        )
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(bounded = true, color = effectiveColor),
            onClick = onClick
        )
}