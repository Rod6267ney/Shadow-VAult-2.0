package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.interactiveFrostedGlass

@Composable
fun FrostedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .interactiveFrostedGlass(cornerRadius = 16.dp, color = color, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.labelLarge.copy(color = color)) {
            content()
        }
    }
}
