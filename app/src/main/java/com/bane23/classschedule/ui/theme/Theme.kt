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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.R

// "Stanice" palettes: a cool blue-white base (light) or a deep night blue (dark) with one warm tangerine accent.
// Subjects bring their own colors (ui/Components.kt), so the base stays calm and the accent marks only what is
// current or tappable. The home-screen widget uses the same two palettes.

internal val LightColors = lightColorScheme(
    primary = Color(0xFFE8620C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE8D6),
    onPrimaryContainer = Color(0xFF7A3000),
    secondary = Color(0xFF3E7BFA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF1CC),
    onSecondaryContainer = Color(0xFF5C4200),
    tertiary = Color(0xFF1F8A55),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD7F2E3),
    onTertiaryContainer = Color(0xFF0E4A2C),
    background = Color(0xFFEFF4FF),
    onBackground = Color(0xFF131A33),
    surface = Color(0xFFEFF4FF),
    onSurface = Color(0xFF131A33),
    surfaceVariant = Color(0xFFE3E9F8),
    onSurfaceVariant = Color(0xFF59638A),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFE3E9F8),
    surfaceContainerHighest = Color(0xFFDCE3F5),
    outline = Color(0xFF8E97B8),
    outlineVariant = Color(0xFFDCE3F5),
    error = Color(0xFFC8323A),
    onError = Color.White,
    errorContainer = Color(0xFFFBDADC),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFF9A4D),
    onPrimary = Color(0xFF1A1208),
    primaryContainer = Color(0xFF3A2C35),
    onPrimaryContainer = Color(0xFFFFD9BF),
    secondary = Color(0xFF8FB2FF),
    onSecondary = Color(0xFF0F1530),
    secondaryContainer = Color(0xFF3B3524),
    onSecondaryContainer = Color(0xFFFFE6A8),
    tertiary = Color(0xFF6FD6A0),
    onTertiary = Color(0xFF0F1530),
    tertiaryContainer = Color(0xFF1E3D3A),
    onTertiaryContainer = Color(0xFFBDF2D6),
    background = Color(0xFF0F1530),
    onBackground = Color(0xFFF1F4FF),
    surface = Color(0xFF0F1530),
    onSurface = Color(0xFFF1F4FF),
    surfaceVariant = Color(0xFF2A3360),
    onSurfaceVariant = Color(0xFFA3ACD0),
    surfaceBright = Color(0xFF2A3360),
    surfaceContainerLowest = Color(0xFF1A2246),
    surfaceContainerLow = Color(0xFF1A2246),
    surfaceContainer = Color(0xFF1A2246),
    surfaceContainerHigh = Color(0xFF222B55),
    surfaceContainerHighest = Color(0xFF2A3360),
    outline = Color(0xFF6E78A3),
    outlineVariant = Color(0xFF2A3360),
    error = Color(0xFFFF8A8F),
    onError = Color(0xFF1A1208),
    errorContainer = Color(0xFF5A2430),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

/** Baloo 2 (SIL Open Font License 1.1): rounded and friendly, with every Serbian Latin letter. */
val Baloo = FontFamily(
    Font(R.font.baloo2_medium, FontWeight.Normal),
    Font(R.font.baloo2_medium, FontWeight.Medium),
    Font(R.font.baloo2_bold, FontWeight.SemiBold),
    Font(R.font.baloo2_bold, FontWeight.Bold),
    Font(R.font.baloo2_extrabold, FontWeight.ExtraBold),
)

private fun TextStyle.baloo(weight: FontWeight, size: Int? = null, line: Int? = null) = copy(
    fontFamily = Baloo,
    fontWeight = weight,
    fontSize = size?.sp ?: fontSize,
    lineHeight = line?.sp ?: lineHeight,
)

// Hierarchy comes from size and two weights (medium for text, extra bold for titles); secondary text uses a muted color.
private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.baloo(FontWeight.ExtraBold),
        headlineMedium = headlineMedium.baloo(FontWeight.ExtraBold, 28, 32),
        headlineSmall = headlineSmall.baloo(FontWeight.ExtraBold, 24, 28),
        titleLarge = titleLarge.baloo(FontWeight.ExtraBold, 22, 26),
        titleMedium = titleMedium.baloo(FontWeight.Bold, 17, 21),
        titleSmall = titleSmall.baloo(FontWeight.Bold, 15, 19),
        bodyLarge = bodyLarge.baloo(FontWeight.Medium, 16, 22),
        bodyMedium = bodyMedium.baloo(FontWeight.Medium, 14, 19),
        bodySmall = bodySmall.baloo(FontWeight.Medium, 12, 16),
        labelLarge = labelLarge.baloo(FontWeight.Bold, 14, 18),
        labelMedium = labelMedium.baloo(FontWeight.Bold, 12, 16),
        labelSmall = labelSmall.baloo(FontWeight.Bold, 11, 14),
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
