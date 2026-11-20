package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = DairyRoyalBlue,
    onPrimary = Color.White,
    primaryContainer = DairyRoyalBlueLight,
    onPrimaryContainer = DairyRoyalBlueDark,
    secondary = DairyFreshGreen,
    onSecondary = Color.White,
    secondaryContainer = DairyFreshGreenLight,
    onSecondaryContainer = DairyFreshGreenDark,
    tertiary = StatusAmber,
    onTertiary = Color.White,
    tertiaryContainer = StatusAmberContainer,
    onTertiaryContainer = Color(0xFF78350F),
    background = DairyCanvas,
    onBackground = InkPrimary,
    surface = DairySurface,
    onSurface = InkPrimary,
    surfaceVariant = DairySurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = DairyOutline,
    outlineVariant = DairyOutlineSubtle,
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorContainer,
    onErrorContainer = Color(0xFF991B1B)
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF00255E),
    primaryContainer = Color(0xFF0B3A82),
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = DarkSecondary,
    onSecondary = Color(0xFF003824),
    secondaryContainer = Color(0xFF08563A),
    onSecondaryContainer = Color(0xFFBCEFD9),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = DarkCanvas,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInkSecondary,
    outline = Color(0xFF334A6E),
    outlineVariant = Color(0xFF233654),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SnehalMilkTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
