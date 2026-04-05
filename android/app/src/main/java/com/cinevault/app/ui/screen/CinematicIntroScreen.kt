package com.cinevault.app.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cinevault.app.R
import kotlinx.coroutines.delay

private val IntroBlack = Color(0xFF000000)
private val IntroGold = Color(0xFFD4AF37)
private val IntroGoldLight = Color(0xFFE8D48B)
private val IntroGoldDim = Color(0xFF8B7328)
private val IntroGoldBright = Color(0xFFF5D76E)

@Composable
fun CinematicIntroScreen(
    onIntroFinished: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // ── Force landscape + immersive ──
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val window = activity?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose { }
    }

    // ── 9-phase timeline (~6.8s total) ──
    var phase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(300)    // 0→1: fade to solid black
        phase = 1
        delay(500)    // 1→2: ambient gradient + particles fade in
        phase = 2
        delay(600)    // 2→3: circular golden glow appears behind logo (small)
        phase = 3
        delay(700)    // 3→4: circular logo fades in + glow starts expanding
        phase = 4
        delay(800)    // 4→5: app name slides up, glow at full expansion
        phase = 5
        delay(700)    // 5→6: tagline + golden ring arcs appear, glow softens
        phase = 6
        delay(900)    // 6→7: shimmer sweep across logo
        phase = 7
        delay(1000)   // 7→8: hold at full beauty
        phase = 8
        delay(600)    // 8→9: fade out
        phase = 9
        delay(400)
        onIntroFinished()
    }

    // ── Animations ──

    // Screen opacity
    val screenAlpha by animateFloatAsState(
        targetValue = when {
            phase == 0 -> 0.2f
            phase >= 9 -> 0f
            else -> 1f
        },
        animationSpec = tween(if (phase >= 9) 500 else 400, easing = FastOutSlowInEasing),
        label = "screen",
    )

    // Ambient gradient
    val ambientIntensity by animateFloatAsState(
        targetValue = when {
            phase < 1 -> 0f
            phase in 1..2 -> 0.3f
            phase in 3..7 -> 1f
            phase == 8 -> 0.8f
            else -> 0f
        },
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "ambient",
    )

    // ── Circular golden glow (the star feature) ──
    // Scale: 0.8 → 1.0 → 1.3 → settle at 1.1 → fade out
    val circularGlowScale by animateFloatAsState(
        targetValue = when {
            phase < 2 -> 0f
            phase == 2 -> 0.5f         // small glow appears
            phase == 3 -> 0.8f         // glow grows as logo appears
            phase == 4 -> 1.1f         // glow expands past logo
            phase == 5 -> 1.3f         // full expansion
            phase in 6..7 -> 1.15f     // settle back slightly
            phase == 8 -> 1.0f         // hold
            else -> 0f                 // fade
        },
        animationSpec = tween(
            durationMillis = when (phase) {
                5 -> 800
                else -> 600
            },
            easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f),
        ),
        label = "glowScale",
    )
    val circularGlowAlpha by animateFloatAsState(
        targetValue = when {
            phase < 2 -> 0f
            phase == 2 -> 0.3f
            phase in 3..4 -> 0.7f
            phase == 5 -> 0.85f        // brightest moment
            phase in 6..7 -> 0.55f     // soften after peak
            phase == 8 -> 0.4f         // gentle hold
            else -> 0f
        },
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "glowAlpha",
    )

    // Outer radial burst (secondary ring of light)
    val outerBurstScale by animateFloatAsState(
        targetValue = when {
            phase < 4 -> 0f
            phase == 4 -> 0.6f
            phase == 5 -> 1.5f
            phase in 6..7 -> 1.8f
            else -> 0f
        },
        animationSpec = tween(900, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "burst",
    )
    val outerBurstAlpha by animateFloatAsState(
        targetValue = when {
            phase < 4 -> 0f
            phase in 4..5 -> 0.25f
            phase in 6..7 -> 0.1f
            else -> 0f
        },
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "burstA",
    )

    // Logo (circular)
    val logoScale by animateFloatAsState(
        targetValue = when {
            phase < 3 -> 0.4f
            phase == 3 -> 1.08f
            phase == 4 -> 1.0f
            phase == 5 -> 1.02f
            phase == 6 -> 1.05f
            phase in 7..8 -> 1.0f
            else -> 0.92f
        },
        animationSpec = tween(
            if (phase == 3) 700 else 500,
            easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f),
        ),
        label = "logoS",
    )
    val logoAlpha by animateFloatAsState(
        targetValue = when {
            phase < 3 -> 0f
            phase >= 9 -> 0f
            else -> 1f
        },
        animationSpec = tween(if (phase == 3) 500 else 400, easing = FastOutSlowInEasing),
        label = "logoA",
    )

    // App name
    val nameAlpha by animateFloatAsState(
        targetValue = if (phase in 4..8) 1f else 0f,
        animationSpec = tween(500, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "nameA",
    )
    val nameTranslateY by animateFloatAsState(
        targetValue = if (phase in 4..8) 0f else 28f,
        animationSpec = tween(600, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "nameY",
    )

    // Tagline
    val taglineAlpha by animateFloatAsState(
        targetValue = if (phase in 5..8) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "tagA",
    )
    val taglineTranslateY by animateFloatAsState(
        targetValue = if (phase in 5..8) 0f else 14f,
        animationSpec = tween(500, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "tagY",
    )

    // Golden arc ring
    val ringAlpha by animateFloatAsState(
        targetValue = if (phase in 5..8) 0.75f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "ringA",
    )

    // Shimmer
    val shimmerProgress by animateFloatAsState(
        targetValue = if (phase >= 6) 1.3f else -0.3f,
        animationSpec = tween(900, easing = LinearEasing),
        label = "shimmer",
    )

    // Infinite transitions
    val infiniteTransition = rememberInfiniteTransition(label = "inf")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "ringR",
    )
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pOff",
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gPulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IntroBlack)
            .alpha(screenAlpha),
        contentAlignment = Alignment.Center,
    ) {
        // ── Ambient background gradient + particles ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        IntroGoldDim.copy(alpha = 0.14f * ambientIntensity),
                        IntroGoldDim.copy(alpha = 0.05f * ambientIntensity),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height * 0.44f),
                    radius = size.width * 0.8f,
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width / 2f, size.height * 0.44f),
            )
            // Floating particles
            if (ambientIntensity > 0.2f) {
                val pAlpha = (ambientIntensity - 0.2f).coerceIn(0f, 1f) * 0.35f
                for (i in 0 until 14) {
                    val px = size.width * ((i * 0.073f + particleOffset * 0.025f * (if (i % 2 == 0) 1 else -1)) % 1f)
                    val py = size.height * ((i * 0.069f + 0.08f + particleOffset * 0.018f * (if (i % 3 == 0) 1 else -1)) % 1f)
                    drawCircle(
                        color = IntroGoldLight.copy(alpha = pAlpha * (0.25f + (i % 4) * 0.18f)),
                        radius = (1.2f + (i % 3) * 0.7f).dp.toPx(),
                        center = Offset(px, py),
                    )
                }
            }
        }

        // ── Cinematic widescreen bars ──
        Box(
            Modifier.fillMaxWidth().height(1.5.dp).align(Alignment.TopCenter).offset(y = 36.dp)
                .alpha(ambientIntensity * 0.25f)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, IntroGold.copy(alpha = 0.35f), Color.Transparent)))
        )
        Box(
            Modifier.fillMaxWidth().height(1.5.dp).align(Alignment.BottomCenter).offset(y = (-36).dp)
                .alpha(ambientIntensity * 0.25f)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, IntroGold.copy(alpha = 0.35f), Color.Transparent)))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ── Logo container with Canvas-based glow (no Box = no square) ──
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp),
            ) {
                // All glow layers drawn on Canvas — pure circles, zero square artifacts
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Scale the entire glow canvas for the expansion animation
                            scaleX = circularGlowScale * glowPulse
                            scaleY = circularGlowScale * glowPulse
                            alpha = circularGlowAlpha
                        }
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // Layer 1: Wide outer ambient glow (very soft, large radius)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to IntroGoldDim.copy(alpha = 0.18f),
                                0.25f to IntroGoldDim.copy(alpha = 0.10f),
                                0.5f to IntroGold.copy(alpha = 0.04f),
                                0.75f to IntroGold.copy(alpha = 0.01f),
                                1.0f to Color.Transparent,
                            ),
                            center = Offset(cx, cy),
                            radius = size.minDimension * 0.5f,
                        ),
                        center = Offset(cx, cy),
                        radius = size.minDimension * 0.5f,
                    )

                    // Layer 2: Primary golden glow (medium radius, main visible glow)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to IntroGold.copy(alpha = 0.45f),
                                0.15f to IntroGold.copy(alpha = 0.35f),
                                0.35f to IntroGoldLight.copy(alpha = 0.18f),
                                0.55f to IntroGoldDim.copy(alpha = 0.06f),
                                0.8f to IntroGoldDim.copy(alpha = 0.01f),
                                1.0f to Color.Transparent,
                            ),
                            center = Offset(cx, cy),
                            radius = size.minDimension * 0.38f,
                        ),
                        center = Offset(cx, cy),
                        radius = size.minDimension * 0.38f,
                    )

                    // Layer 3: Inner bright core (tight, bright center)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to IntroGoldBright.copy(alpha = 0.4f),
                                0.2f to IntroGold.copy(alpha = 0.25f),
                                0.5f to IntroGold.copy(alpha = 0.08f),
                                1.0f to Color.Transparent,
                            ),
                            center = Offset(cx, cy),
                            radius = size.minDimension * 0.22f,
                        ),
                        center = Offset(cx, cy),
                        radius = size.minDimension * 0.22f,
                    )
                }

                // Outer burst glow (separate canvas for independent scale)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = outerBurstScale
                            scaleY = outerBurstScale
                            alpha = outerBurstAlpha
                        }
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to IntroGoldLight.copy(alpha = 0.15f),
                                0.3f to IntroGold.copy(alpha = 0.06f),
                                0.6f to IntroGoldDim.copy(alpha = 0.02f),
                                1.0f to Color.Transparent,
                            ),
                            center = Offset(cx, cy),
                            radius = size.minDimension * 0.48f,
                        ),
                        center = Offset(cx, cy),
                        radius = size.minDimension * 0.48f,
                    )
                }

                // Rotating golden arc ring
                Canvas(
                    modifier = Modifier
                        .size(165.dp)
                        .alpha(ringAlpha)
                        .graphicsLayer { rotationZ = ringRotation }
                ) {
                    rotate(0f) {
                        drawArc(
                            color = IntroGold.copy(alpha = 0.55f),
                            startAngle = 0f, sweepAngle = 120f, useCenter = false,
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                            size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                        )
                    }
                    rotate(180f) {
                        drawArc(
                            color = IntroGoldLight.copy(alpha = 0.35f),
                            startAngle = 0f, sweepAngle = 90f, useCenter = false,
                            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                            size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                        )
                    }
                    rotate(90f) {
                        drawArc(
                            color = IntroGoldBright.copy(alpha = 0.25f),
                            startAngle = 0f, sweepAngle = 60f, useCenter = false,
                            style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                            size = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx()),
                        )
                    }
                }

                // ── Circular App Logo (zero square artifacts) ──
                Image(
                    painter = painterResource(id = R.drawable.velora_logo_round),
                    contentDescription = "Velora",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .scale(logoScale)
                        .alpha(logoAlpha)
                        .drawBehind {
                            // Shimmer sweep
                            if (shimmerProgress in 0f..1f) {
                                val sx = size.width * shimmerProgress
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.12f),
                                            Color.White.copy(alpha = 0.3f),
                                            Color.White.copy(alpha = 0.45f),
                                            Color.White.copy(alpha = 0.3f),
                                            Color.White.copy(alpha = 0.12f),
                                            Color.Transparent,
                                        ),
                                        start = Offset(sx - size.width * 0.25f, 0f),
                                        end = Offset(sx + size.width * 0.25f, size.height),
                                    ),
                                )
                            }
                        },
                )
            }

            Spacer(Modifier.height(22.dp))

            // ── App Name ──
            Text(
                text = "VELORA",
                color = IntroGold,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(nameAlpha)
                    .graphicsLayer { translationY = nameTranslateY },
            )

            Spacer(Modifier.height(8.dp))

            // ── Separator line ──
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(1.dp)
                    .alpha(nameAlpha * 0.45f)
                    .graphicsLayer { translationY = nameTranslateY * 0.4f }
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, IntroGold.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
            )

            Spacer(Modifier.height(12.dp))

            // ── Tagline ──
            Text(
                text = "P R E M I U M   S T R E A M I N G",
                color = IntroGoldLight.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(taglineAlpha)
                    .graphicsLayer { translationY = taglineTranslateY },
            )
        }
    }
}
