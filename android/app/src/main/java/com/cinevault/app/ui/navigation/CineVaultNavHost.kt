package com.cinevault.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.cinevault.app.ui.components.UpdateDialog
import com.cinevault.app.ui.screen.*
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.AppViewModel
import kotlin.math.cos
import kotlin.math.sin

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, Screen.Home.route),
    BottomNavItem("Downloads", Icons.Filled.Download, Screen.Downloads.route),
    BottomNavItem("Watchlist", Icons.Outlined.BookmarkBorder, Screen.Watchlist.route),
    BottomNavItem("Me", Icons.Filled.Person, Screen.Me.route),
)

// ═══════════════════════════════════════════════════════════════
// PREMIUM BOTTOM NAV BAR
// ═══════════════════════════════════════════════════════════════

@Composable
fun PremiumBottomNavBar(
    navController: NavHostController,
    currentRoute: String?,
) {
    val goldColor = CineVaultTheme.colors.accentGold
    val goldLight = CineVaultTheme.colors.accentLight
    val goldMuted = CineVaultTheme.colors.accentMuted

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        CineVaultTheme.colors.surface.copy(alpha = 0.7f),
                        CineVaultTheme.colors.surface.copy(alpha = 0.97f),
                        CineVaultTheme.colors.surface,
                    )
                )
            )
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentRoute == item.route

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    when (item.route) {
                        Screen.Home.route -> HomeNavItem(selected = selected, goldColor = goldColor, goldLight = goldLight, goldMuted = goldMuted)
                        Screen.Downloads.route -> DownloadNavItem(selected = selected, goldColor = goldColor, goldLight = goldLight, goldMuted = goldMuted)
                        Screen.Watchlist.route -> WatchlistNavItem(selected = selected, goldColor = goldColor, goldLight = goldLight)
                        Screen.Me.route -> MeNavItem(selected = selected, goldColor = goldColor)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HOME: House with star inside, soft rounded corners, roof integrated
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HomeNavItem(selected: Boolean, goldColor: Color, goldLight: Color, goldMuted: Color) {
    // Star pulse animation when selected
    val starScale by animateFloatAsState(
        targetValue = if (selected) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 300f),
        label = "starScale"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 350f),
        label = "homeIconScale"
    )

    val gray = Color(0xFF808080)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Canvas(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        ) {
            val w = size.width
            val h = size.height
            val strokeW = 2.2.dp.toPx()
            val cornerR = 4.dp.toPx()

            // Colors
            val outlineColor = if (selected) goldColor else gray
            val fillBrush = if (selected) Brush.verticalGradient(
                colors = listOf(goldLight, goldColor, goldMuted)
            ) else null

            // ── ROOF (triangle with rounded feel) ──
            val roofPeakY = h * 0.05f
            val roofBaseY = h * 0.4f
            val roofLeftX = w * 0.05f
            val roofRightX = w * 0.95f
            val roofPeakX = w * 0.5f

            val roofPath = Path().apply {
                moveTo(roofPeakX, roofPeakY)
                lineTo(roofRightX, roofBaseY)
                lineTo(roofLeftX, roofBaseY)
                close()
            }

            if (fillBrush != null) {
                drawPath(roofPath, fillBrush)
            }
            drawPath(roofPath, outlineColor, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // ── HOUSE BODY (rounded rectangle below roof) ──
            val bodyTop = roofBaseY - strokeW / 2f
            val bodyLeft = w * 0.15f
            val bodyRight = w * 0.85f
            val bodyBottom = h * 0.92f
            val bodyWidth = bodyRight - bodyLeft
            val bodyHeight = bodyBottom - bodyTop

            if (fillBrush != null) {
                drawRoundRect(
                    brush = fillBrush,
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(cornerR, cornerR),
                )
            }
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(cornerR, cornerR),
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // ── STAR in center ──
            val starCenterX = w * 0.5f
            val starCenterY = (bodyTop + bodyBottom) / 2f + h * 0.02f
            val outerR = w * 0.13f * starScale
            val innerR = outerR * 0.45f
            val starColor = if (selected) Color(0xFF1A1A1A) else gray

            drawStar(
                center = Offset(starCenterX, starCenterY),
                outerRadius = outerR,
                innerRadius = innerR,
                points = 5,
                color = starColor,
                style = if (selected) Fill else Stroke(width = 1.5.dp.toPx()),
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            "Home",
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) goldColor else Color(0xFF808080),
        )
    }
}

// Draw a 5-pointed star
private fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    points: Int,
    color: Color,
    style: androidx.compose.ui.graphics.drawscope.DrawStyle,
) {
    val path = Path()
    val angleStep = Math.PI / points
    var angle = -Math.PI / 2.0 // start from top

    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        angle += angleStep
    }
    path.close()
    drawPath(path, color, style = style)
}

// ═══════════════════════════════════════════════════════════════
// DOWNLOAD: Rounded box with arrow, premium gold fill when selected
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DownloadNavItem(selected: Boolean, goldColor: Color, goldLight: Color, goldMuted: Color) {
    // Bounce animation — plays on selection then settles
    var triggerBounce by remember { mutableStateOf(false) }
    LaunchedEffect(selected) {
        if (selected) {
            triggerBounce = true
        }
    }

    val bounceOffset by animateFloatAsState(
        targetValue = if (triggerBounce && selected) 1f else 0f,
        animationSpec = if (triggerBounce && selected) {
            keyframes {
                durationMillis = 800
                0f at 0 using LinearEasing
                1f at 200 using FastOutSlowInEasing
                0.3f at 400 using FastOutSlowInEasing
                0.8f at 550 using FastOutSlowInEasing
                0.1f at 700 using FastOutSlowInEasing
                0f at 800 using FastOutSlowInEasing
            }
        } else {
            tween(200)
        },
        label = "dlArrowBounce",
        finishedListener = { triggerBounce = false }
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "dlScale"
    )

    val gray = Color(0xFF808080)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Canvas(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        ) {
            val w = size.width
            val h = size.height
            val strokeW = 2.2.dp.toPx()
            val cornerR = 6.dp.toPx()

            val outlineColor = if (selected) goldColor else gray
            val fillBrush = if (selected) Brush.verticalGradient(
                colors = listOf(goldLight, goldColor, goldMuted)
            ) else null

            val boxLeft = w * 0.08f
            val boxTop = h * 0.05f
            val boxRight = w * 0.92f
            val boxBottom = h * 0.92f
            val boxWidth = boxRight - boxLeft
            val boxHeight = boxBottom - boxTop

            // ── OUTER BOX (rounded rectangle) ──
            if (fillBrush != null) {
                drawRoundRect(
                    brush = fillBrush,
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxWidth, boxHeight),
                    cornerRadius = CornerRadius(cornerR, cornerR),
                )
            }
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(cornerR, cornerR),
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // ── HORIZONTAL LINE across middle (like file fold) ──
            val foldY = boxTop + boxHeight * 0.45f
            drawLine(
                color = if (selected) Color(0xFF1A1A1A).copy(alpha = 0.4f) else gray.copy(alpha = 0.5f),
                start = Offset(boxLeft + strokeW, foldY),
                end = Offset(boxRight - strokeW, foldY),
                strokeWidth = strokeW * 0.8f,
                cap = StrokeCap.Round,
            )

            // ── ARROW pointing down ──
            val arrowColor = if (selected) Color(0xFF1A1A1A) else gray
            val arrowCenterX = w * 0.5f
            val arrowBounce = bounceOffset * 4.dp.toPx()

            // Shaft
            val shaftTop = boxTop + boxHeight * 0.18f + arrowBounce
            val shaftBottom = foldY - boxHeight * 0.04f + arrowBounce
            val shaftW = strokeW * 1.3f
            drawLine(
                color = arrowColor,
                start = Offset(arrowCenterX, shaftTop),
                end = Offset(arrowCenterX, shaftBottom),
                strokeWidth = shaftW,
                cap = StrokeCap.Round,
            )

            // Arrow head (V chevron)
            val chevronSize = w * 0.16f
            val chevronTipY = shaftBottom + chevronSize * 0.5f
            drawLine(
                color = arrowColor,
                start = Offset(arrowCenterX - chevronSize, shaftBottom - chevronSize * 0.1f),
                end = Offset(arrowCenterX, chevronTipY),
                strokeWidth = shaftW,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = arrowColor,
                start = Offset(arrowCenterX + chevronSize, shaftBottom - chevronSize * 0.1f),
                end = Offset(arrowCenterX, chevronTipY),
                strokeWidth = shaftW,
                cap = StrokeCap.Round,
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            "Download",
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) goldColor else Color(0xFF808080),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// WATCHLIST: Bookmark icon with soft glow and scale
// ═══════════════════════════════════════════════════════════════

@Composable
private fun WatchlistNavItem(selected: Boolean, goldColor: Color, goldLight: Color) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "wlScale"
    )

    val gray = Color(0xFF808080)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        // Custom drawn bookmark
        Canvas(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        ) {
            val w = size.width
            val h = size.height
            val strokeW = 2.2.dp.toPx()
            val cornerR = 5.dp.toPx()
            val color = if (selected) goldColor else gray

            val left = w * 0.2f
            val right = w * 0.8f
            val top = h * 0.08f
            val bottom = h * 0.88f
            val notchY = bottom - h * 0.2f

            val bookmarkPath = Path().apply {
                // Top-left rounded corner
                moveTo(left + cornerR, top)
                lineTo(right - cornerR, top)
                // Top-right curve
                cubicTo(right, top, right, top, right, top + cornerR)
                lineTo(right, notchY)
                // V notch at bottom
                lineTo((left + right) / 2f, bottom)
                lineTo(left, notchY)
                lineTo(left, top + cornerR)
                // Top-left curve
                cubicTo(left, top, left, top, left + cornerR, top)
                close()
            }

            if (selected) {
                drawPath(bookmarkPath, Brush.verticalGradient(listOf(goldLight, goldColor)))
            }
            drawPath(bookmarkPath, color, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Plus sign in center
            val plusSize = w * 0.12f
            val plusCX = w * 0.5f
            val plusCY = h * 0.4f
            val plusColor = if (selected) Color(0xFF1A1A1A) else gray
            drawLine(plusColor, Offset(plusCX - plusSize, plusCY), Offset(plusCX + plusSize, plusCY), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(plusColor, Offset(plusCX, plusCY - plusSize), Offset(plusCX, plusCY + plusSize), strokeWidth = strokeW, cap = StrokeCap.Round)
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            "Watchlist",
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) goldColor else Color(0xFF808080),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// ME: Circle face with sunglasses 😎 drawn as icon, transparent bg
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MeNavItem(selected: Boolean, goldColor: Color) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 350f),
        label = "meScale"
    )

    // Subtle bounce when selected
    var triggerBounce by remember { mutableStateOf(false) }
    LaunchedEffect(selected) {
        if (selected) triggerBounce = true
    }
    val bounceY by animateFloatAsState(
        targetValue = if (triggerBounce && selected) 1f else 0f,
        animationSpec = if (triggerBounce && selected) {
            keyframes {
                durationMillis = 600
                0f at 0
                -1f at 150
                0.3f at 350
                -0.2f at 500
                0f at 600
            }
        } else tween(200),
        label = "meBounceY",
        finishedListener = { triggerBounce = false }
    )

    val gray = Color(0xFF808080)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Canvas(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    translationY = bounceY * 4.dp.toPx()
                }
        ) {
            val w = size.width
            val h = size.height
            val strokeW = 2.2.dp.toPx()
            val color = if (selected) goldColor else gray

            val cx = w / 2f
            val cy = h / 2f
            val radius = w * 0.42f

            // ── CIRCLE (face outline) ──
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = strokeW),
            )

            // ── SUNGLASSES ──
            val glassY = cy - radius * 0.08f
            val glassW = radius * 0.42f
            val glassH = radius * 0.32f
            val glassCornerR = 3.dp.toPx()
            val bridgeY = glassY

            // Left lens
            val leftGlassX = cx - radius * 0.52f
            drawRoundRect(
                color = color,
                topLeft = Offset(leftGlassX, glassY - glassH / 2f),
                size = Size(glassW, glassH),
                cornerRadius = CornerRadius(glassCornerR, glassCornerR),
                style = if (selected) Fill else Stroke(width = strokeW * 0.8f),
            )

            // Right lens
            val rightGlassX = cx + radius * 0.1f
            drawRoundRect(
                color = color,
                topLeft = Offset(rightGlassX, glassY - glassH / 2f),
                size = Size(glassW, glassH),
                cornerRadius = CornerRadius(glassCornerR, glassCornerR),
                style = if (selected) Fill else Stroke(width = strokeW * 0.8f),
            )

            // Bridge between lenses
            drawLine(
                color = color,
                start = Offset(leftGlassX + glassW, bridgeY),
                end = Offset(rightGlassX, bridgeY),
                strokeWidth = strokeW * 0.8f,
                cap = StrokeCap.Round,
            )

            // Temple arms (lines going to edge of circle)
            drawLine(
                color = color,
                start = Offset(leftGlassX, bridgeY),
                end = Offset(cx - radius * 0.8f, bridgeY - radius * 0.15f),
                strokeWidth = strokeW * 0.7f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(rightGlassX + glassW, bridgeY),
                end = Offset(cx + radius * 0.8f, bridgeY - radius * 0.15f),
                strokeWidth = strokeW * 0.7f,
                cap = StrokeCap.Round,
            )

            // ── SMILE ──
            val smilePath = Path().apply {
                val smileY = cy + radius * 0.38f
                val smileW = radius * 0.4f
                val smileH = radius * 0.15f
                moveTo(cx - smileW, smileY)
                quadraticBezierTo(cx, smileY + smileH * 2.5f, cx + smileW, smileY)
            }
            drawPath(
                smilePath,
                color = color,
                style = Stroke(width = strokeW * 0.8f, cap = StrokeCap.Round),
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            "Me",
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) goldColor else Color(0xFF808080),
        )
    }
}

@Composable
fun CineVaultNavHost(navController: NavHostController = rememberNavController()) {
    val appViewModel: AppViewModel = hiltViewModel()
    val updateInfo by appViewModel.updateInfo.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        containerColor = CineVaultTheme.colors.background,
        bottomBar = {
            if (showBottomBar) {
                PremiumBottomNavBar(
                    navController = navController,
                    currentRoute = currentRoute,
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() },
                )
            }

            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onMovieClick = { movieId -> navController.navigate(Screen.MovieDetail.createRoute(movieId)) },
                    onPlayClick = { contentId -> navController.navigate(Screen.Player.createRoute(contentId, null)) },
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                    onSectionClick = { section ->
                        SectionDataHolder.set(section)
                        navController.navigate(Screen.SectionDetail.createRoute(section.id))
                    },
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onMovieClick = { movieId -> navController.navigate(Screen.MovieDetail.createRoute(movieId)) },
                )
            }

            composable(Screen.Watchlist.route) {
                WatchlistScreen(
                    onMovieClick = { movieId -> navController.navigate(Screen.MovieDetail.createRoute(movieId)) },
                )
            }

            composable(Screen.Me.route) {
                MeScreen(
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onMovieClick = { movieId -> navController.navigate(Screen.MovieDetail.createRoute(movieId)) },
                    onHistoryItemClick = { contentId, episodeId ->
                        // Push DetailPage then Player: Back from Player → Trailer Page
                        navController.navigate(Screen.MovieDetail.createRoute(contentId))
                        navController.navigate(Screen.Player.createRoute(contentId, episodeId))
                    },
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToWatchHistory = { navController.navigate(Screen.WatchHistory.route) },
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onMovieClick = { movieId -> navController.navigate(Screen.MovieDetail.createRoute(movieId)) },
                )
            }

            composable(Screen.Downloads.route) {
                DownloadsScreen()
            }

            composable(Screen.WatchHistory.route) {
                WatchHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onHistoryItemClick = { contentId, episodeId ->
                        // Push DetailPage then Player: Back from Player → Trailer Page
                        navController.navigate(Screen.MovieDetail.createRoute(contentId))
                        navController.navigate(Screen.Player.createRoute(contentId, episodeId))
                    },
                )
            }

            composable(
                Screen.SectionDetail.route,
                arguments = listOf(navArgument("sectionId") { type = NavType.StringType }),
            ) {
                val section = SectionDataHolder.get()
                SectionDetailScreen(
                    sectionTitle = section?.title ?: "Section",
                    movies = section?.items ?: emptyList(),
                    onMovieClick = { movieId -> navController.navigate(Screen.MovieDetail.createRoute(movieId)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                Screen.MovieDetail.route,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType }),
            ) {
                MovieDetailScreen(
                    onBack = { navController.popBackStack() },
                    onPlay = { contentId, episodeId ->
                        navController.navigate(Screen.Player.createRoute(contentId, episodeId))
                    },
                    onRelatedClick = { movieId ->
                        navController.navigate(Screen.MovieDetail.createRoute(movieId))
                    },
                )
            }

            composable(
                Screen.Player.route,
                arguments = listOf(
                    navArgument("contentId") { type = NavType.StringType },
                    navArgument("episodeId") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                PlayerScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    // Show update dialog on top of everything when an update is available
    updateInfo?.let { info ->
        UpdateDialog(
            info = info,
            onDismiss = { appViewModel.dismissUpdate() }
        )
    }
}
