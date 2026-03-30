package com.cinevault.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Primary Palette — VELORA Purple & Gold
val BackgroundDark = Color(0xFF0B0515)
val SurfaceDark = Color(0xFF130D22)
val SurfaceElevated = Color(0xFF1C1230)
val BorderSubtle = Color(0xFF2D1F4E)

// Accent — Metallic Gold
val AccentGold = Color(0xFFD4AF37)
val AccentGoldMuted = Color(0xFFA8892C)
val AccentGoldLight = Color(0xFFE8D48B)

// Purple Accent
val AccentPurple = Color(0xFF9B59B6)
val AccentPurpleLight = Color(0xFFBB86FC)
val AccentPurpleDark = Color(0xFF6C3483)

// Text
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0A3C4)
val TextMuted = Color(0xFF6B5E80)

// Semantic
val RatingGold = Color(0xFFD4AF37)
val SuccessGreen = Color(0xFF22C55E)
val ErrorRed = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)

// Gradients
val GradientOverlayStart = Color(0x00000000)
val GradientOverlayEnd = Color(0xFF0B0515)

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
