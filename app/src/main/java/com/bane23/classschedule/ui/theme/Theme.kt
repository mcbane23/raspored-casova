package com.bane23.classschedule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Pastel palettes based on Catppuccin (https://catppuccin.com, MIT): "Latte" for the light theme and "Mocha" for
// the dark one. Both use one family of soft, low-contrast accents (lavender, teal, yellow) on calm neutral bases,
// so colored cards and buttons stay close to each other instead of competing.

private val LightColors = lightColorScheme(
    primary = Color(0xFF7287FD), // Latte lavender
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E6FE),
    onPrimaryContainer = Color(0xFF3B4483),
    secondary = Color(0xFFDF8E1D), // Latte yellow
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8EBD3),
    onSecondaryContainer = Color(0xFF6E4A14),
    tertiary = Color(0xFF179299), // Latte teal
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5EFEC),
    onTertiaryContainer = Color(0xFF145A5E),
    background = Color(0xFFEFF1F5), // base
    onBackground = Color(0xFF4C4F69), // text
    surface = Color(0xFFEFF1F5),
    onSurface = Color(0xFF4C4F69),
    surfaceVariant = Color(0xFFE6E9EF), // mantle
    onSurfaceVariant = Color(0xFF6C6F85), // subtext0
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFE6E9EF),
    surfaceContainerHighest = Color(0xFFDCE0E8), // crust
    outline = Color(0xFFACB0BE), // surface2
    outlineVariant = Color(0xFFCCD0DA), // surface0
    error = Color(0xFFD20F39),
    errorContainer = Color(0xFFF8D7DE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4BEFE), // Mocha lavender
    onPrimary = Color(0xFF1E1E2E),
    primaryContainer = Color(0xFF3A3D5E),
    onPrimaryContainer = Color(0xFFD5DAFF),
    secondary = Color(0xFFF9E2AF), // Mocha yellow
    onSecondary = Color(0xFF1E1E2E),
    secondaryContainer = Color(0xFF4A4436),
    onSecondaryContainer = Color(0xFFF9E2AF),
    tertiary = Color(0xFF94E2D5), // Mocha teal
    onTertiary = Color(0xFF1E1E2E),
    tertiaryContainer = Color(0xFF2E4A4B),
    onTertiaryContainer = Color(0xFFC2F0E8),
    background = Color(0xFF1E1E2E), // base
    onBackground = Color(0xFFCDD6F4), // text
    surface = Color(0xFF1E1E2E),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF313244), // surface0
    onSurfaceVariant = Color(0xFFA6ADC8), // subtext0
    surfaceBright = Color(0xFF45475A), // surface1
    surfaceContainerLowest = Color(0xFF28283B),
    surfaceContainerLow = Color(0xFF28283B),
    surfaceContainer = Color(0xFF28283B),
    surfaceContainerHigh = Color(0xFF313244),
    surfaceContainerHighest = Color(0xFF45475A),
    outline = Color(0xFF6C7086), // overlay0
    outlineVariant = Color(0xFF45475A),
    error = Color(0xFFF38BA8),
    errorContainer = Color(0xFF5A2E3C),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Hierarchy comes from a few sizes and two weights (regular and bold); secondary text uses a muted color.
private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
fun RasporedCasovaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
