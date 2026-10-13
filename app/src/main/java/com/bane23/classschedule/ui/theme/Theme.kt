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

// A fixed, friendly palette instead of the phone's dynamic colors, so the app looks the same for every child and
// parent: a calm off-white base, dark ink for text, indigo for actions and sunny yellow for highlights.

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F5BD5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E6FD),
    onPrimaryContainer = Color(0xFF1A2275),
    secondary = Color(0xFFF2A100),
    onSecondary = Color(0xFF2B1D00),
    secondaryContainer = Color(0xFFFFF0CC),
    onSecondaryContainer = Color(0xFF5A3D00),
    tertiary = Color(0xFF16A394),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3F4EF),
    onTertiaryContainer = Color(0xFF00413A),
    background = Color(0xFFF5F6FB),
    onBackground = Color(0xFF1C1F2E),
    surface = Color(0xFFF5F6FB),
    onSurface = Color(0xFF1C1F2E),
    surfaceVariant = Color(0xFFEBEDF5),
    onSurfaceVariant = Color(0xFF5F6478),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0F1F8),
    surfaceContainerHighest = Color(0xFFEBEDF5),
    outline = Color(0xFFC6C9D8),
    outlineVariant = Color(0xFFE2E4EE),
    error = Color(0xFFD93F3F),
    errorContainer = Color(0xFFFDE3E3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB0B7FF),
    onPrimary = Color(0xFF1A2275),
    primaryContainer = Color(0xFF353FA8),
    onPrimaryContainer = Color(0xFFE3E6FD),
    secondary = Color(0xFFFFC94D),
    onSecondary = Color(0xFF3F2A00),
    secondaryContainer = Color(0xFF5A3D00),
    onSecondaryContainer = Color(0xFFFFF0CC),
    tertiary = Color(0xFF5FD8C8),
    onTertiary = Color(0xFF00413A),
    tertiaryContainer = Color(0xFF00574D),
    onTertiaryContainer = Color(0xFFD3F4EF),
    background = Color(0xFF12131A),
    onBackground = Color(0xFFE6E7F0),
    surface = Color(0xFF12131A),
    onSurface = Color(0xFFE6E7F0),
    surfaceVariant = Color(0xFF2A2C38),
    onSurfaceVariant = Color(0xFFA9ADC0),
    surfaceContainerLowest = Color(0xFF181A23),
    surfaceContainerLow = Color(0xFF1C1E28),
    surfaceContainer = Color(0xFF1C1E28),
    surfaceContainerHigh = Color(0xFF242632),
    surfaceContainerHighest = Color(0xFF2A2C38),
    outline = Color(0xFF4A4D5E),
    outlineVariant = Color(0xFF30323F),
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF5C1A1A),
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
