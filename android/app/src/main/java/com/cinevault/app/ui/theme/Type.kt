package com.cinevault.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Font families - using system fonts as fallback, replace with actual font files
val PlayfairDisplay = FontFamily.Serif // Replace with actual Playfair Display font
val DMSans = FontFamily.SansSerif // Replace with actual DM Sans font
val JetBrainsMono = FontFamily.Monospace // Replace with actual JetBrains Mono font

@Immutable
data class CineVaultTypography(
    // Display / Hero
    val heroTitle: TextStyle = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        color = TextPrimary,
    ),
    val displayLarge: TextStyle = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        color = TextPrimary,
    ),

    // Section Headers
    val sectionTitle: TextStyle = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 1.28.sp, // 0.08em
        color = TextPrimary,
    ),
    val subsectionTitle: TextStyle = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.56.sp,
        color = TextPrimary,
    ),

    // Body
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = TextPrimary,
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextPrimary,
    ),
    val bodySmall: TextStyle = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary,
    ),

    // Labels / Metadata
    val labelMedium: TextStyle = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextMuted,
    ),
    val labelSmall: TextStyle = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = TextMuted,
    ),

    // Button
    val button: TextStyle = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),

    // Monospace / Technical
    val mono: TextStyle = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextMuted,
    ),
) {
    val body: TextStyle get() = bodyMedium
    val label: TextStyle get() = labelMedium
    val title: TextStyle get() = sectionTitle
    val subtitle: TextStyle get() = subsectionTitle
}

val LocalCineVaultTypography = staticCompositionLocalOf { CineVaultTypography() }
