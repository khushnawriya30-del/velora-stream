package com.cinevault.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Primary Palette
val BackgroundDark = Color(0xFF0A0A0A)
val SurfaceDark = Color(0xFF141414)
val SurfaceElevated = Color(0xFF1E1E1E)
val BorderSubtle = Color(0xFF2A2A2A)

// Accent
val AccentGold = Color(0xFFF5A623)
val AccentGoldMuted = Color(0xFFC4841A)
val AccentGoldLight = Color(0xFFFFD076)

// Text
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A0A0)
val TextMuted = Color(0xFF606060)

// Semantic
val RatingGold = Color(0xFFFFD700)
val SuccessGreen = Color(0xFF22C55E)
val ErrorRed = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)

// Gradients
val GradientOverlayStart = Color(0x00000000)
val GradientOverlayEnd = Color(0xFF0A0A0A)

@Immutable
data class CineVaultColors(
    val background: Color = BackgroundDark,
    val surface: Color = SurfaceDark,
    val surfaceElevated: Color = SurfaceElevated,
    val borderSubtle: Color = BorderSubtle,
    val accent: Color = AccentGold,
    val accentMuted: Color = AccentGoldMuted,
    val accentLight: Color = AccentGoldLight,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textMuted: Color = TextMuted,
    val ratingGold: Color = RatingGold,
    val success: Color = SuccessGreen,
    val error: Color = ErrorRed,
    val warning: Color = WarningAmber,
) {
    val accentGold: Color get() = accent
    val border: Color get() = borderSubtle
}

val LocalCineVaultColors = staticCompositionLocalOf { CineVaultColors() }
