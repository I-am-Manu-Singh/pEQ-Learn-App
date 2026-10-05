package com.creativeblisslabs.peqlearn.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// =====================================================
// GLASS SYSTEM
// =====================================================

val GlassWhite = Color(0x05FFFFFF)
val GlassHighlight = Color(0x08FFFFFF)
val GlassBorderDark = Color(0x10FFFFFF)
val GlassBorderLight = Color(0x05000000)

// =====================================================
// DARK FOUNDATIONS
// =====================================================

val PureBlack = Color(0xFF000000)

val CarbonBlack = Color(0xFF050505)

val StudioBlack = Color(0xFF090B0F)

val MidnightBlue = Color(0xFF0E1117)

val DeepPanel = Color(0xFF121720)

val ElevatedPanel = Color(0xFF171E29)

val FloatingPanel = Color(0xFF1C2633)

// =====================================================
// LIGHT FOUNDATIONS
// =====================================================

val FrostWhite = Color(0xFFF8FAFC)

val FrostSurface = Color(0xFFFFFFFF)

val FrostPanel = Color(0xFFF1F5F9)

val FrostElevated = Color(0xFFE8EEF6)

// =====================================================
// ACCENTS
// =====================================================

val StudioAmber = Color(0xFFFFC857)

val StudioBlue = Color(0xFF4A7DFF)

val MutedCyan = Color(0xFF59D4FF)

val Emerald = Color(0xFF00D26A)

val NeonRed = Color(0xFFFF5A5F)

// =====================================================
// TEXT
// =====================================================

val TextPrimaryDark = Color(0xFFF8FAFC)

val TextSecondaryDark = Color(0xFF94A3B8)

val TextMutedDark = Color(0xFF64748B)

val TextPrimaryLight = Color(0xFF0F172A)

val TextSecondaryLight = Color(0xFF475569)

// =====================================================
// PRO DARK
// =====================================================

private val ProDarkColorScheme = darkColorScheme(

        primary = StudioAmber,
        secondary = StudioBlue,
        tertiary = MutedCyan,

        background = StudioBlack,

        surface = MidnightBlue,

        surfaceBright = ElevatedPanel,

        surfaceDim = StudioBlack,

        surfaceContainerLowest = CarbonBlack,

        surfaceContainerLow = MidnightBlue,

        surfaceContainer = DeepPanel,

        surfaceContainerHigh = ElevatedPanel,

        surfaceContainerHighest = FloatingPanel,

        surfaceVariant = DeepPanel,

        onPrimary = Color.Black,

        onSecondary = Color.White,

        onTertiary = Color.Black,

        onBackground = TextPrimaryDark,

        onSurface = TextPrimaryDark,

        onSurfaceVariant = TextSecondaryDark,

        outline = Color(0xFF243041),

        outlineVariant = Color(0xFF1A2432),

        error = NeonRed,

        scrim = Color(0xD9000000),

        inverseSurface = Color.White,

        inverseOnSurface = StudioBlack,

        surfaceTint = Color.Transparent
)

// =====================================================
// OLED
// =====================================================

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
        surfaceContainerHigh = CarbonBlack,
        surfaceContainerHighest = MidnightBlue,
        surfaceVariant = CarbonBlack,

        onPrimary = Color.Black,

        onSecondary = Color.White,

        onTertiary = Color.Black,

        onBackground = TextPrimaryDark,

        onSurface = TextPrimaryDark,

        onSurfaceVariant = TextSecondaryDark,

        outline = Color(0xFF182230),

        outlineVariant = Color(0xFF101720),

        error = NeonRed,

        scrim = Color.Black,

        inverseSurface = Color.White,

        inverseOnSurface = PureBlack,

        surfaceTint = Color.Transparent
)

// =====================================================
// LIGHT
// =====================================================

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

        surfaceContainerHighest = Color(0xFFDCE5F0),

        surfaceVariant = FrostPanel,

        onPrimary = Color.White,

        onSecondary = Color.Black,

        onTertiary = Color.Black,

        onBackground = TextPrimaryLight,

        onSurface = TextPrimaryLight,

        onSurfaceVariant = TextSecondaryLight,

        outline = Color(0xFFD7DEE8),

        outlineVariant = Color(0xFFE5EAF1),

        error = NeonRed,

        scrim = Color(0x55000000),

        inverseSurface = StudioBlack,

        inverseOnSurface = Color.White,

        surfaceTint = Color.Transparent
)

// =====================================================
// ACCENTS
// =====================================================

enum class pEQAccent(val color: Color, val label: String) {
    AMBER(StudioAmber, "Industrial Amber"),
    BLUE(StudioBlue, "Neural Blue"),
    CYAN(MutedCyan, "Aero Cyan"),
    EMERALD(Emerald, "Bio Emerald"),
    RED(NeonRed, "Critical Red"),
    MAGENTA(Color(0xFFFF00FF), "Cyber Magenta"),
    VIOLET(Color(0xFF8B5CF6), "Royal Violet")
}

// =====================================================
// THEME ENUM
// =====================================================

enum class pEQTheme {
    DARK,
    OLED,
    LIGHT,
    SYSTEM
}

// =====================================================
// MAIN THEME
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

    // Override primary with selected accent
    val colors = baseColors.copy(
        primary = accent.color,
        onPrimary = if (accent.color.luminance() > 0.5f) Color.Black else Color.White
    )

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        shapes = Shapes()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background
        ) {
            content()
        }
    }
}