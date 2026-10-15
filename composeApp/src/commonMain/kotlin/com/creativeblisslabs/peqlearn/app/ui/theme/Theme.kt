package com.creativeblisslabs.peqlearn.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================
// GLASS SYSTEM (Professional Refinement)
// =====================================================

val GlassWhite = Color(0x08FFFFFF)
val GlassHighlight = Color(0x0CFFFFFF)
val GlassBorderDark = Color(0x15FFFFFF)
val GlassBorderLight = Color(0x08000000)

// =====================================================
// DARK FOUNDATIONS (Neural Studio Aesthetic)
// =====================================================

val PureBlack = Color(0xFF000000)
val CarbonBlack = Color(0xFF08080A)
val StudioBlack = Color(0xFF0C0E12)
val MidnightBlue = Color(0xFF12151C)
val DeepPanel = Color(0xFF1A1D25)
val ElevatedPanel = Color(0xFF232731)
val FloatingPanel = Color(0xFF2C323D)

// =====================================================
// LIGHT FOUNDATIONS (Alabaster Professional)
// =====================================================

val FrostWhite = Color(0xFFFFFDF5)
val FrostSurface = Color(0xFFFFFFFF)
val FrostPanel = Color(0xFFF1F5F9)
val FrostElevated = Color(0xFFE2E8F0)

// =====================================================
// ACCENTS (Studio Grade)
// =====================================================

val StudioAmber = Color(0xFFCD7F32)
val StudioBlue = Color(0xFF4A7DFF)
val MutedCyan = Color(0xFF59D4FF)
val Emerald = Color(0xFF00D26A)
val NeonRed = Color(0xFFFF5A5F)
val CyberMagenta = Color(0xFFFF4DFF)
val RoyalViolet = Color(0xFF8B5CF6)

// =====================================================
// TEXT TOKENS
// =====================================================

val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)

// =====================================================
// TYPOGRAPHY SYSTEM
// =====================================================

val pEQTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

// =====================================================
// SHAPE SYSTEM
// =====================================================

val pEQShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// =====================================================
// COLOR SCHEMES
// =====================================================

private val ProDarkColorScheme = darkColorScheme(
    primary = StudioAmber,
    secondary = StudioBlue,
    tertiary = MutedCyan,
    background = StudioBlack,
    surface = MidnightBlue,
    surfaceBright = ElevatedPanel,
    surfaceDim = CarbonBlack,
    surfaceContainerLowest = PureBlack,
    surfaceContainerLow = CarbonBlack,
    surfaceContainer = MidnightBlue,
    surfaceContainerHigh = DeepPanel,
    surfaceContainerHighest = ElevatedPanel,
    surfaceVariant = DeepPanel,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF2D3748),
    outlineVariant = Color(0xFF1E293B),
    error = NeonRed,
    scrim = Color(0xE6000000),
    inverseSurface = Color.White,
    inverseOnSurface = StudioBlack,
    surfaceTint = Color.Transparent
)

private val OLEDColorScheme = darkColorScheme(
    primary = StudioAmber,
    secondary = StudioBlue,
    tertiary = MutedCyan,
    background = PureBlack,
    surface = PureBlack,
    surfaceBright = CarbonBlack,
    surfaceDim = PureBlack,
    surfaceContainerLowest = PureBlack,
    surfaceContainerLow = PureBlack,
    surfaceContainer = CarbonBlack,
    surfaceContainerHigh = Color(0xFF0A0A0C),
    surfaceContainerHighest = MidnightBlue,
    surfaceVariant = CarbonBlack,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF1E293B),
    outlineVariant = Color(0xFF0F172A),
    error = NeonRed,
    scrim = Color.Black,
    inverseSurface = Color.White,
    inverseOnSurface = PureBlack,
    surfaceTint = Color.Transparent
)

private val ProLightColorScheme = lightColorScheme(
    primary = StudioBlue,
    secondary = StudioAmber,
    tertiary = MutedCyan,
    background = FrostWhite,
    surface = FrostSurface,
    surfaceBright = Color.White,
    surfaceDim = FrostPanel,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = FrostWhite,
    surfaceContainer = FrostPanel,
    surfaceContainerHigh = FrostElevated,
    surfaceContainerHighest = Color(0xFFCBD5E1),
    surfaceVariant = FrostPanel,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFF1F5F9),
    error = NeonRed,
    scrim = Color(0x66000000),
    inverseSurface = StudioBlack,
    inverseOnSurface = Color.White,
    surfaceTint = Color.Transparent
)

// =====================================================
// THEME ENUMS
// =====================================================

enum class pEQAccent(val color: Color, val label: String) {
    AMBER(StudioAmber, "Industrial Amber"),
    BLUE(StudioBlue, "Neural Blue"),
    CYAN(MutedCyan, "Aero Cyan"),
    EMERALD(Emerald, "Bio Emerald"),
    RED(NeonRed, "Critical Red"),
    MAGENTA(CyberMagenta, "Cyber Magenta"),
    VIOLET(RoyalViolet, "Royal Violet")
}

enum class pEQTheme {
    DARK,
    OLED,
    LIGHT,
    SYSTEM
}

// =====================================================
// MAIN THEME COMPOSABLE
// =====================================================

@Composable
fun pEQTheme(
    theme: pEQTheme = pEQTheme.DARK,
    accent: pEQAccent = pEQAccent.AMBER,
    content: @Composable () -> Unit
) {
    val darkSystem = isSystemInDarkTheme()

    val baseColors = when (theme) {
        pEQTheme.DARK -> ProDarkColorScheme
        pEQTheme.OLED -> OLEDColorScheme
        pEQTheme.LIGHT -> ProLightColorScheme
        pEQTheme.SYSTEM -> {
            if (darkSystem) ProDarkColorScheme else ProLightColorScheme
        }
    }

    // Dynamic color harmony: Override primary and secondary based on accent
    val colors = baseColors.copy(
        primary = accent.color,
        onPrimary = if (accent.color.luminance() > 0.5f) Color.Black else Color.White,
        // derive a subtle container color for better harmony
        primaryContainer = accent.color.copy(alpha = if (theme == pEQTheme.LIGHT) 0.1f else 0.15f),
        onPrimaryContainer = if (theme == pEQTheme.LIGHT) accent.color else TextPrimaryDark
    )

    MaterialTheme(
        colorScheme = colors,
        typography = pEQTypography,
        shapes = pEQShapes
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background,
            content = content
        )
    }
}
