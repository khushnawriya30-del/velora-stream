package com.cinevault.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Responsive dimension system — all sizes scale based on screen width/height.
 * Reference design: 960dp width (typical TV/landscape phone).
 * Smaller screens get proportionally smaller values, larger screens get larger.
 */
data class TvDimens(
    val screenWidthDp: Float,
    val screenHeightDp: Float,
) {
    // Scale factor relative to 960dp reference width
    private val widthScale: Float = (screenWidthDp / 960f).coerceIn(0.5f, 1.6f)
    private val heightScale: Float = (screenHeightDp / 540f).coerceIn(0.5f, 1.6f)
    // Combined scale for general sizing
    private val scale: Float = ((widthScale + heightScale) / 2f).coerceIn(0.55f, 1.5f)

    // --- Padding ---
    val screenPadH: Dp get() = (48f * widthScale).coerceAtLeast(16f).dp
    val screenPadV: Dp get() = (24f * heightScale).coerceAtLeast(8f).dp
    val padTiny: Dp get() = (4f * scale).coerceAtLeast(2f).dp
    val padSmall: Dp get() = (8f * scale).coerceAtLeast(4f).dp
    val padMedium: Dp get() = (12f * scale).coerceAtLeast(6f).dp
    val padLarge: Dp get() = (16f * scale).coerceAtLeast(8f).dp
    val padXL: Dp get() = (24f * scale).coerceAtLeast(12f).dp
    val padXXL: Dp get() = (32f * scale).coerceAtLeast(16f).dp
    val padSection: Dp get() = (48f * widthScale).coerceAtLeast(16f).dp

    // --- Font Sizes ---
    val fontTiny: TextUnit get() = (9f * scale).coerceAtLeast(7f).sp
    val fontXSmall: TextUnit get() = (10f * scale).coerceAtLeast(8f).sp
    val fontSmall: TextUnit get() = (12f * scale).coerceAtLeast(9f).sp
    val fontBody: TextUnit get() = (14f * scale).coerceAtLeast(10f).sp
    val fontMedium: TextUnit get() = (16f * scale).coerceAtLeast(11f).sp
    val fontLarge: TextUnit get() = (18f * scale).coerceAtLeast(12f).sp
    val fontXL: TextUnit get() = (20f * scale).coerceAtLeast(14f).sp
    val fontXXL: TextUnit get() = (24f * scale).coerceAtLeast(16f).sp
    val fontTitle: TextUnit get() = (28f * scale).coerceAtLeast(18f).sp
    val fontHero: TextUnit get() = (36f * scale).coerceAtLeast(22f).sp
    val fontDisplay: TextUnit get() = (48f * scale).coerceAtLeast(28f).sp
    val fontSplash: TextUnit get() = (56f * scale).coerceAtLeast(32f).sp
    val fontTrending: TextUnit get() = (60f * scale).coerceAtLeast(32f).sp

    // --- Card / Element Sizes ---
    val bannerHeight: Dp get() = (380f * heightScale).coerceAtLeast(180f).dp
    val heroHeight: Dp get() = (400f * heightScale).coerceAtLeast(200f).dp
    val midBannerHeight: Dp get() = (180f * heightScale).coerceAtLeast(100f).dp

    val movieCardW: Dp get() = (140f * widthScale).coerceAtLeast(90f).dp
    val movieCardH: Dp get() = (210f * heightScale).coerceAtLeast(130f).dp
    val movieCardLargeW: Dp get() = (180f * widthScale).coerceAtLeast(110f).dp
    val movieCardLargeH: Dp get() = (270f * heightScale).coerceAtLeast(160f).dp

    val trendingCardW: Dp get() = (120f * widthScale).coerceAtLeast(80f).dp
    val trendingCardH: Dp get() = (180f * heightScale).coerceAtLeast(110f).dp
    val trendingRankW: Dp get() = (50f * widthScale).coerceAtLeast(28f).dp

    val continueCardW: Dp get() = (200f * widthScale).coerceAtLeast(130f).dp
    val continueCardH: Dp get() = (130f * heightScale).coerceAtLeast(80f).dp

    val episodeCardW: Dp get() = (220f * widthScale).coerceAtLeast(150f).dp
    val episodeCardH: Dp get() = (72f * heightScale).coerceAtLeast(48f).dp

    val relatedCardW: Dp get() = (140f * widthScale).coerceAtLeast(90f).dp
    val relatedCardH: Dp get() = (210f * heightScale).coerceAtLeast(130f).dp

    val searchCardW: Dp get() = (160f * widthScale).coerceAtLeast(100f).dp
    val searchCardH: Dp get() = (240f * heightScale).coerceAtLeast(150f).dp
    val searchGridMinW: Dp get() = (160f * widthScale).coerceAtLeast(100f).dp

    val qrSize: Dp get() = (220f * scale).coerceIn(120f, 300f).dp

    val iconSmall: Dp get() = (20f * scale).coerceAtLeast(14f).dp
    val iconMedium: Dp get() = (24f * scale).coerceAtLeast(16f).dp
    val iconLarge: Dp get() = (28f * scale).coerceAtLeast(18f).dp

    val splashGlow: Dp get() = (400f * scale).coerceAtLeast(200f).dp
    val splashProgress: Dp get() = (28f * scale).coerceAtLeast(18f).dp

    val stepCircle: Dp get() = (28f * scale).coerceAtLeast(18f).dp

    // Line heights
    val lineHeightBody: TextUnit get() = (20f * scale).coerceAtLeast(14f).sp
    val lineHeightLarge: TextUnit get() = (24f * scale).coerceAtLeast(16f).sp

    // Specific UI fractions
    val searchBarH: Dp get() = (50f * heightScale).coerceAtLeast(36f).dp
    val filterLabelW: Dp get() = (80f * widthScale).coerceAtLeast(50f).dp
    val progressBarH: Dp get() = (4f * scale).coerceAtLeast(2f).dp

    val premiumPopupFraction: Float get() = if (screenWidthDp < 700f) 0.7f else 0.4f
    val premiumGateFraction: Float get() = if (screenWidthDp < 700f) 0.85f else 0.5f
    val qrLoginFractionW: Float get() = if (screenWidthDp < 700f) 0.95f else 0.8f
    val qrLoginFractionH: Float get() = if (screenHeightDp < 400f) 0.95f else 0.8f
}

val LocalTvDimens = staticCompositionLocalOf { TvDimens(960f, 540f) }

@Composable
fun ProvideTvDimens(content: @Composable () -> Unit) {
    val config = LocalConfiguration.current
    val dimens = TvDimens(
        screenWidthDp = config.screenWidthDp.toFloat(),
        screenHeightDp = config.screenHeightDp.toFloat(),
    )
    CompositionLocalProvider(LocalTvDimens provides dimens) {
        content()
    }
}
