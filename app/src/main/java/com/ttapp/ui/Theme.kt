package com.ttapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Carbon = Color(0xFF101417)
val Graphite = Color(0xFF1C2429)
val Raised = Color(0xFF253138)
val Cyan = Color(0xFF00D9E8)
val SoftWhite = Color(0xFFE7F0F2)
val Muted = Color(0xFF9EADB5)

private val Dark = darkColorScheme(
    primary = Cyan, onPrimary = Carbon,
    background = Carbon, onBackground = SoftWhite,
    surface = Graphite, onSurface = SoftWhite,
    surfaceVariant = Raised, onSurfaceVariant = Muted,
    outline = Color(0xFF425157),
    secondary = Color(0xFFB3CDD2),
    error = Color(0xFFFF818B)
)

@Composable
fun TTTheme(content: @Composable () -> Unit) {
    // Intentionally dark by default, independent of system theme.
    MaterialTheme(colorScheme = Dark, typography = Typography(), content = content)
}
