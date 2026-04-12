package com.cinevault.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Responsive dimension system for the mobile app.
 * Reference design: 392dp width × 857dp height (typical 6.1" phone like Pixel 7).
 * Scales proportionally for smaller phones (320-360dp) and tablets (600dp+).
 */
data class AppDimens(
    val screenWidthDp: Float,
    val screenHeightDp: Float,
) {
    private val widthScale: Float = (screenWidthDp / 392f).coerceIn(0.75f, 1.5f)
    private val heightScale: Float = (screenHeightDp / 857f).coerceIn(0.75f, 1.5f)
    private val scale: Float = ((widthScale + heightScale) / 2f).coerceIn(0.8f, 1.4f)

    // ── Padding ──
    val padTiny: Dp get() = (2f * scale).coerceAtLeast(1f).dp
    val pad4: Dp get() = (4f * scale).coerceAtLeast(2f).dp
    val pad6: Dp get() = (6f * scale).coerceAtLeast(4f).dp
    val pad8: Dp get() = (8f * scale).coerceAtLeast(4f).dp
    val pad10: Dp get() = (10f * scale).coerceAtLeast(6f).dp
    val pad12: Dp get() = (12f * scale).coerceAtLeast(8f).dp
    val pad14: Dp get() = (14f * scale).coerceAtLeast(8f).dp
    val pad16: Dp get() = (16f * widthScale).coerceAtLeast(10f).dp
    val pad20: Dp get() = (20f * widthScale).coerceAtLeast(12f).dp
    val pad24: Dp get() = (24f * widthScale).coerceAtLeast(14f).dp
    val pad32: Dp get() = (32f * widthScale).coerceAtLeast(18f).dp

    // ── Font Sizes ──
    val font8: TextUnit get() = (8f * scale).coerceAtLeast(6f).sp
    val font9: TextUnit get() = (9f * scale).coerceAtLeast(7f).sp
    val font10: TextUnit get() = (10f * scale).coerceAtLeast(8f).sp
    val font11: TextUnit get() = (11f * scale).coerceAtLeast(8f).sp
    val font12: TextUnit get() = (12f * scale).coerceAtLeast(9f).sp
    val font13: TextUnit get() = (13f * scale).coerceAtLeast(10f).sp
    val font14: TextUnit get() = (14f * scale).coerceAtLeast(10f).sp
    val font15: TextUnit get() = (15f * scale).coerceAtLeast(11f).sp
    val font16: TextUnit get() = (16f * scale).coerceAtLeast(12f).sp
    val font18: TextUnit get() = (18f * scale).coerceAtLeast(13f).sp
    val font20: TextUnit get() = (20f * scale).coerceAtLeast(14f).sp
    val font22: TextUnit get() = (22f * scale).coerceAtLeast(16f).sp
    val font24: TextUnit get() = (24f * scale).coerceAtLeast(16f).sp
    val font26: TextUnit get() = (26f * scale).coerceAtLeast(18f).sp
    val font28: TextUnit get() = (28f * scale).coerceAtLeast(20f).sp
    val font32: TextUnit get() = (32f * scale).coerceAtLeast(22f).sp
    val font36: TextUnit get() = (36f * scale).coerceAtLeast(24f).sp
    val font48: TextUnit get() = (48f * scale).coerceAtLeast(32f).sp

    // ── Line Heights ──
    val lineHeight16: TextUnit get() = (16f * scale).coerceAtLeast(12f).sp
    val lineHeight18: TextUnit get() = (18f * scale).coerceAtLeast(14f).sp
    val lineHeight20: TextUnit get() = (20f * scale).coerceAtLeast(14f).sp
    val lineHeight22: TextUnit get() = (22f * scale).coerceAtLeast(16f).sp
    val lineHeight24: TextUnit get() = (24f * scale).coerceAtLeast(16f).sp

    // ── Icon Sizes ──
    val icon14: Dp get() = (14f * scale).coerceAtLeast(10f).dp
    val icon18: Dp get() = (18f * scale).coerceAtLeast(12f).dp
    val icon20: Dp get() = (20f * scale).coerceAtLeast(14f).dp
    val icon22: Dp get() = (22f * scale).coerceAtLeast(16f).dp
    val icon24: Dp get() = (24f * scale).coerceAtLeast(16f).dp
    val icon28: Dp get() = (28f * scale).coerceAtLeast(18f).dp
    val icon30: Dp get() = (30f * scale).coerceAtLeast(20f).dp
    val icon36: Dp get() = (36f * scale).coerceAtLeast(24f).dp

    // ── UI Element Sizes ──
    val searchBarH: Dp get() = (44f * heightScale).coerceAtLeast(36f).dp
    val buttonH: Dp get() = (42f * heightScale).coerceAtLeast(34f).dp
    val chipH: Dp get() = (30f * heightScale).coerceAtLeast(24f).dp
    val thumbnailSmall: Dp get() = (52f * scale).coerceAtLeast(36f).dp
    val avatarMedium: Dp get() = (80f * scale).coerceAtLeast(56f).dp
    val topFadeH: Dp get() = (100f * heightScale).coerceAtLeast(60f).dp
    val sideFadeW: Dp get() = (40f * widthScale).coerceAtLeast(24f).dp

    // ── Card Sizes ──
    val cardSmallW: Dp get() = (115f * widthScale).coerceAtLeast(85f).dp
    val cardMediumW: Dp get() = (140f * widthScale).coerceAtLeast(100f).dp
    val cardLargeW: Dp get() = (170f * widthScale).coerceAtLeast(120f).dp

    // ── Shape Radii ──
    val radius4: Dp get() = (4f * scale).coerceAtLeast(2f).dp
    val radius6: Dp get() = (6f * scale).coerceAtLeast(4f).dp
    val radius8: Dp get() = (8f * scale).coerceAtLeast(4f).dp
    val radius10: Dp get() = (10f * scale).coerceAtLeast(6f).dp
    val radius16: Dp get() = (16f * scale).coerceAtLeast(10f).dp
    val radius22: Dp get() = (22f * scale).coerceAtLeast(14f).dp

    // ── Spacers / Misc ──
    val bottomNavSpacer: Dp get() = (90f * heightScale).coerceAtLeast(60f).dp
    val loadingBoxH: Dp get() = (200f * heightScale).coerceAtLeast(120f).dp
    val gradientH: Dp get() = (60f * heightScale).coerceAtLeast(40f).dp
    val underlineW: Dp get() = (36f * widthScale).coerceAtLeast(24f).dp
    val underlineH: Dp get() = (3f * scale).coerceAtLeast(2f).dp
    val dotActive: Dp get() = (8f * scale).coerceAtLeast(6f).dp
    val dotInactive: Dp get() = (6f * scale).coerceAtLeast(4f).dp
    val premiumBadge: Dp get() = (36f * scale).coerceAtLeast(24f).dp
    val premiumBadgeSmall: Dp get() = (24f * scale).coerceAtLeast(16f).dp
    val progressBarH: Dp get() = (4f * scale).coerceAtLeast(2f).dp
    val strokeWidth: Dp get() = (3f * scale).coerceAtLeast(2f).dp
}

val LocalAppDimens = staticCompositionLocalOf { AppDimens(392f, 857f) }

@Composable
fun ProvideAppDimens(content: @Composable () -> Unit) {
    val config = LocalConfiguration.current
    val dimens = AppDimens(
        screenWidthDp = config.screenWidthDp.toFloat(),
        screenHeightDp = config.screenHeightDp.toFloat(),
    )
    CompositionLocalProvider(LocalAppDimens provides dimens) {
        content()
    }
}
