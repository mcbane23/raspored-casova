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
    surfaceBright = Color.White,
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

// Dark mode keeps the same indigo / yellow / teal identity at a deeper tone so colored cards stay readable with white
// text, and cards are lighter than the background so they stand out without shadows.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF7C86F7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2C3275),
    onPrimaryContainer = Color(0xFFDDE0FF),
    secondary = Color(0xFFFFC94D),
    onSecondary = Color(0xFF3F2A00),
    secondaryContainer = Color(0xFF4A3A12),
    onSecondaryContainer = Color(0xFFFFE6A8),
    tertiary = Color(0xFF1C9E8F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF113F3A),
    onTertiaryContainer = Color(0xFFB8F0E7),
    background = Color(0xFF111319),
    onBackground = Color(0xFFE7E8F0),
    surface = Color(0xFF111319),
    onSurface = Color(0xFFE7E8F0),
    surfaceVariant = Color(0xFF262A38),
    onSurfaceVariant = Color(0xFFA8ADC2),
    surfaceBright = Color(0xFF3A3F53),
    surfaceContainerLowest = Color(0xFF1C1F2B),
    surfaceContainerLow = Color(0xFF1C1F2B),
    surfaceContainer = Color(0xFF1C1F2B),
    surfaceContainerHigh = Color(0xFF232634),
    surfaceContainerHighest = Color(0xFF2A2E3D),
    outline = Color(0xFF4A4F64),
    outlineVariant = Color(0xFF2E3242),
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
