package com.josem.misgastos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colors = lightColorScheme(
    primary = Color(0xFF2F6656),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E9E1),
    secondary = Color(0xFF755B27),
    background = Color(0xFFF7F7F2),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFBA1A1A),
)

@Composable
fun MisGastosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
