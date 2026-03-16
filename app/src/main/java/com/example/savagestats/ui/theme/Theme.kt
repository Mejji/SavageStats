package com.example.savagestats.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SavageDarkScheme = darkColorScheme(
    primary = SavageRed,
    onPrimary = TextPrimary,
    primaryContainer = SavageRedDark,
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted,
    outlineVariant = DarkSurfaceVariant,
    error = SavageRed,
    onError = TextPrimary,
)

@Composable
fun SavageStatsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SavageDarkScheme,
        typography = Typography,
        content = content
    )
}
