package com.cinevault.app.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinevault.app.R
import com.cinevault.app.ui.viewmodel.EarnMoneyViewModel
import kotlinx.coroutines.delay

// ── Color Palette ──
private val DarkBlueBg = Color(0xFF0D1B3E)
private val DarkBlueCenter = Color(0xFF162D5A)
private val CardBlue = Color(0xFF1B2B55)
private val CardBlueLighter = Color(0xFF283D6A)
private val GoldYellow = Color(0xFFFFD700)
private val GoldAmber = Color(0xFFFFC107)
private val OrangeBright = Color(0xFFFF9800)
private val OrangeDeep = Color(0xFFEF6C00)
private val RedOrange = Color(0xFFE53935)
private val GreenProgress = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarnMoneyScreen(
    onBack: () -> Unit = {},
    onNavigateToWithdrawEarning: (Int) -> Unit = {},
    viewModel: EarnMoneyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showRules by remember { mutableStateOf(false) }

    // ── Animated balance counter ──
    var animatedBalance by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.balance) {
        if (state.balance > 0) {
            val steps = 30
            val stepDelay = 900L / steps
            for (i in 1..steps) {
                animatedBalance = (state.balance * i) / steps
                delay(stepDelay)
            }
            animatedBalance = state.balance
        }
    }

    // ── Coin rotation animation ──
    val coinTransition = rememberInfiniteTransition(label = "coin")
    val coinRotation by coinTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "coinRot",
    )

    // ── Withdraw button breathing ──
    val wdPulse = rememberInfiniteTransition(label = "wdPulse")
    val wdScale by wdPulse.animateFloat(
        initialValue = 0.97f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wdScale",
    )
    val wdGlow by wdPulse.animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wdGlow",
    )

    // ── Invite button faster breathing ──
    val invPulse = rememberInfiniteTransition(label = "invPulse")
    val invScale by invPulse.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "invScale",
    )
    val invGlow by invPulse.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "invGlow",
    )

    // ── Progress calculations (dynamic from invite settings) ──
    val invSettings = state.inviteSettings
    val threshold = invSettings?.targetAmount ?: state.withdrawThreshold
    val balance = state.balance
    val amountLeft = (threshold - balance).coerceAtLeast(0)
    val progressFraction = (balance.toFloat() / threshold.toFloat()).coerceIn(0f, 1f)

    // ═══════════════════════════════════════════
    // Main Scaffold with fixed bottom bar
    // ═══════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(R.drawable.earn_bg),
                contentScale = ContentScale.Crop,
            ),
    ) {
        // ── Scrollable content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp) // space for fixed bottom bar
                .verticalScroll(rememberScrollState()),
        ) {
            // ═══════════════════════════════════════════
            // SECTION 1 — HEADER
            // ═══════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showRules = !showRules }) {
                    Text("Rules", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Title
            Text(
                buildAnnotatedString {
                    append("Earn ")
                    withStyle(SpanStyle(color = GoldYellow, fontWeight = FontWeight.ExtraBold)) {
                        append("Cash")
                    }
                    append(" By Inviting")
                },
                modifier = Modifier.padding(horizontal = 20.dp),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(16.dp))

            // ═══════════════════════════════════════════
            // SECTION 2 — BALANCE CARD (Left: balance, Right: empty for video)
            // ═══════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                // Left side — My Balance card
                Box(
                    modifier = Modifier
                        .weight(0.55f)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(GoldYellow.copy(alpha = 0.5f), GoldAmber.copy(alpha = 0.3f))
                            ),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .background(CardBlue, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                ) {
                    Column {
                        // Coin + My Balance
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_earn_coin),
                                contentDescription = "Coin",
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        rotationY = coinRotation
                                        cameraDistance = 12f * density
                                    },
                                contentScale = ContentScale.Fit,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("My Balance", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GoldYellow)
                        }
                        Spacer(Modifier.height(6.dp))

                        // Balance
                        Text(
                            "₹${animatedBalance}.00",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(10.dp))

                        // Withdraw button
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = wdScale
                                    scaleY = wdScale
                                    alpha = wdGlow
                                }
                                .border(
                                    width = 2.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFFFFE082), Color(0xFFFFC107))
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                )
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFFFFCC80),
                                            Color(0xFFFFB74D),
                                            Color(0xFFFF9800),
                                        )
                                    ),
                                    RoundedCornerShape(20.dp),
                                )
                                .clickable { onNavigateToWithdrawEarning(0) }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            Text(
                                "Withdraw",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3E2723),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Right side — Empty space for video (clean, no placeholder)
                Spacer(modifier = Modifier.weight(0.45f))
            }

            Spacer(Modifier.height(16.dp))

            // ═══════════════════════════════════════════
            // SECTION 3 — SHARE APP CARD
            // ═══════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(GoldAmber.copy(alpha = 0.6f), OrangeBright.copy(alpha = 0.4f))
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .background(CardBlue, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Column {
                    // Share APP header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // App logo PNG from drawable-nodpi
                        Image(
                            painter = painterResource(R.drawable.app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Share APP",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(Modifier.weight(1f))
                        // Invitation Stats button
                        Box(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.4f),
                                    RoundedCornerShape(20.dp),
                                )
                                .clickable { /* stats */ }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "Invitation Stats",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Gold Progress Bar with Treasure Box ──
                    val levels = listOf("Lv.1", "Lv.2", "Lv.3", "Lv.4")
                    val animatedProgress = animateFloatAsState(
                        targetValue = progressFraction,
                        animationSpec = tween(1200, easing = FastOutSlowInEasing),
                        label = "progressAnim",
                    )
                    // Treasure box pulse animation
                    val treasurePulse = rememberInfiniteTransition(label = "treasure")
                    val treasureScale by treasurePulse.animateFloat(
                        initialValue = 1f, targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse,
                        ),
                        label = "tScale",
                    )
                    val treasureGlow by treasurePulse.animateFloat(
                        initialValue = 0.7f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse,
                        ),
                        label = "tGlow",
                    )

                    Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                        // Track background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2A3040)),
                        )
                        // Gold gradient progress fill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress.value)
                                .height(12.dp)
                                .align(Alignment.CenterStart)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(GoldAmber, GoldYellow, Color(0xFFFFA000))
                                    ),
                                    RoundedCornerShape(6.dp),
                                ),
                        )
                        // Dynamic progress percentage — moves with progress bar
                        if (animatedProgress.value > 0.05f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxWidth(animatedProgress.value)
                                    .height(12.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Color(0xAAB8860B),
                                            RoundedCornerShape(8.dp),
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        "${(animatedProgress.value * 100).toInt()}%",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                        // Balance / target text below bar
                        Text(
                            "₹${balance} / ₹${threshold}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldYellow,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                        // Treasure box at progress end
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .graphicsLayer {
                                    scaleX = treasureScale
                                    scaleY = treasureScale
                                    alpha = treasureGlow
                                },
                        ) {
                            // Treasure box emoji/icon
                            Text(
                                "🎁",
                                fontSize = 26.sp,
                            )
                        }
                        // Level markers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            levels.forEachIndexed { index, level ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_earn_coin),
                                        contentDescription = level,
                                        modifier = Modifier.size(
                                            if (index == 3) 22.dp else 16.dp,
                                        ),
                                        contentScale = ContentScale.Fit,
                                    )
                                    Text(
                                        level,
                                        fontSize = 8.sp,
                                        color = GoldYellow.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Progress text
                    Text(
                        buildAnnotatedString {
                            append("Only ")
                            withStyle(
                                SpanStyle(
                                    color = GoldYellow,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            ) {
                                append("₹${amountLeft}.00")
                            }
                            append(" left to withdraw ")
                            withStyle(
                                SpanStyle(
                                    color = GoldYellow,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            ) {
                                append("₹${threshold}.00")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color.White,
                    )

                    Spacer(Modifier.height(14.dp))

                    // ═══════════════════════════════════════════
                    // SECTION 4 — INVITE BUTTON
                    // ═══════════════════════════════════════════
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = invScale
                                scaleY = invScale
                                alpha = invGlow
                            }
                            .border(
                                width = 2.dp,
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFFFFE082), Color(0xFFFF5722))
                                ),
                                shape = RoundedCornerShape(10.dp),
                            )
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFFFB74D),
                                        OrangeBright,
                                        OrangeDeep,
                                        RedOrange,
                                    )
                                ),
                                RoundedCornerShape(10.dp),
                            )
                            .clickable {
                                val shareText =
                                    "Join Velora App & Earn Cash! 🎬💰\n\nUse my referral code: ${state.referralCode}\n\nDownload Now: https://website-omega-liard-77.vercel.app?ref=${state.referralCode}"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, "Share Velora")
                                )
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "+ Invite new users to earn cash",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // ═══════════════════════════════════════════
                    // SECTION 5 — DYNAMIC TIMER
                    // ═══════════════════════════════════════════
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "End in ",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "${state.daysRemaining}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        Text(
                            " Days",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Qualification text
                    Text(
                        "To qualify: Only new, active users of the app qualify for referral rewards.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontStyle = FontStyle.Italic,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ═══════════════════════════════════════════
            // SECTION 6 & 7 — WITHDRAW / EARNING HISTORY
            // ═══════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Withdraw History
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            Brush.verticalGradient(
                                listOf(CardBlueLighter, CardBlue)
                            ),
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { onNavigateToWithdrawEarning(1) }
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Withdraw History",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            "Go",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                // Earning History
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            Brush.verticalGradient(
                                listOf(CardBlueLighter, CardBlue)
                            ),
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { onNavigateToWithdrawEarning(2) }
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Earning History",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            "Go",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ═══════════════════════════════════════════
            // SECTION 8 — HOW MUCH OTHERS EARNED (using provided PNG)
            // ═══════════════════════════════════════════
            Image(
                painter = painterResource(R.drawable.how_much_other_earned),
                contentDescription = "How much others earned",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.FillWidth,
            )

            Spacer(Modifier.height(12.dp))

            // ── Dynamic Earner Proof Screenshots from Admin ──
            if (state.earnerProofs.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.earnerProofs) { proof ->
                        Column(
                            modifier = Modifier
                                .width(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardBlue)
                                .border(
                                    1.dp,
                                    GoldYellow.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp),
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(proof.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = proof.caption,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.75f)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            if (proof.caption.isNotEmpty()) {
                                Text(
                                    proof.caption,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GoldYellow,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Referral Code Section ──
            if (state.referralCode.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(CardBlue, RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            GoldYellow.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(16.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Your Referral Code",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                state.referralCode,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldYellow,
                                letterSpacing = 3.sp,
                            )
                            Spacer(Modifier.width(12.dp))
                            IconButton(onClick = {
                                clipboardManager.setText(
                                    AnnotatedString(state.referralCode)
                                )
                            }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    "Copy",
                                    tint = GoldYellow,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Rules Section (toggleable, expandable cards with Hindi/English toggle) ──
            // (moved to ModalBottomSheet below)

            Spacer(Modifier.height(20.dp))
        }

        // ═══════════════════════════════════════════
        // FIXED BOTTOM BAR — Join Channels
        // ═══════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(DarkBlueBg)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                "Join channels to learn more",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // WhatsApp
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF25D366), RoundedCornerShape(24.dp))
                        .clickable {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://whatsapp.com"),
                                )
                            )
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "WhatsApp",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
                // Telegram
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF0088CC), RoundedCornerShape(24.dp))
                        .clickable {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://t.me"),
                                )
                            )
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✈️", fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Telegram",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    // RULES BOTTOM SHEET
    // ═══════════════════════════════════════════
    if (showRules) {
        ModalBottomSheet(
            onDismissRequest = { showRules = false },
            containerColor = CardBlue,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "📋 Withdrawal Rules",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    IconButton(onClick = { showRules = false }) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(16.dp))

                ExpandableRulesContent()
            }
        }
    }
}

// ═══════════════════════════════════════════
// EXPANDABLE RULES SECTION — Cards with Hindi/English Toggle
// ═══════════════════════════════════════════
private data class RuleCard(
    val titleEn: String,
    val titleHi: String,
    val contentEn: String,
    val contentHi: String,
)

private val rulesData = listOf(
    RuleCard(
        titleEn = "Basic Event Information",
        titleHi = "बेसिक इवेंट जानकारी",
        contentEn = "Share the app with your friends using your referral code. Each successful referral earns you ₹1 directly to your wallet. Your App's Gift amount is shown in your earnings history.",
        contentHi = "अपने रेफरल कोड का उपयोग करके ऐप को अपने दोस्तों के साथ शेयर करें। हर सफल रेफरल पर आपको सीधे वॉलेट में ₹1 मिलता है। आपकी ऐप गिफ्ट राशि आपकी कमाई हिस्ट्री में दिखाई देती है।",
    ),
    RuleCard(
        titleEn = "Core Mechanism & Reward Levels",
        titleHi = "मुख्य प्रक्रिया और रिवॉर्ड लेवल",
        contentEn = "When a new user downloads the app and signs up using your referral code, you earn ₹1. Only verified and active users are counted. The more you invite, the more you earn — there is no upper limit.",
        contentHi = "जब कोई नया यूजर ऐप डाउनलोड करके आपके रेफरल कोड से साइन अप करता है, तो आपको ₹1 मिलता है। केवल सत्यापित और सक्रिय यूजर गिने जाते हैं। जितना ज़्यादा इनवाइट करेंगे, उतना ज़्यादा कमाएंगे — कोई ऊपरी सीमा नहीं।",
    ),
    RuleCard(
        titleEn = "Limited-Time Sprint Bonuses",
        titleHi = "लिमिटेड-टाइम स्प्रिंट बोनस",
        contentEn = "Special bonus events may be activated from time to time. During these events, you can earn extra rewards per referral. Keep an eye on notifications for bonus rounds!",
        contentHi = "समय-समय पर विशेष बोनस इवेंट सक्रिय किए जा सकते हैं। इन इवेंट्स के दौरान, आप प्रति रेफरल अतिरिक्त रिवॉर्ड कमा सकते हैं। बोनस राउंड के लिए नोटिफिकेशन पर नज़र रखें!",
    ),
    RuleCard(
        titleEn = "Reward Distribution & Withdrawal",
        titleHi = "रिवॉर्ड वितरण और निकासी",
        contentEn = "Earnings are added to your wallet instantly. You can withdraw once your balance reaches the minimum threshold (₹100). Provide your bank details for withdrawal. Processing takes 2-5 business days.",
        contentHi = "कमाई तुरंत आपके वॉलेट में जुड़ जाती है। जब आपका बैलेंस न्यूनतम सीमा (₹100) तक पहुंच जाए तो आप निकासी कर सकते हैं। निकासी के लिए अपने बैंक डिटेल्स दें। प्रोसेसिंग में 2-5 कार्य दिवस लगते हैं।",
    ),
    RuleCard(
        titleEn = "Important Terms",
        titleHi = "ज़रूरी शर्तें",
        contentEn = "Self-referral is not allowed. Fake or duplicate accounts will be detected and banned. The app reserves the right to modify reward amounts. Misuse of the referral system will result in wallet suspension.",
        contentHi = "स्वयं रेफरल की अनुमति नहीं है। फर्जी या डुप्लीकेट अकाउंट पकड़े जाएंगे और बैन किए जाएंगे। ऐप को रिवॉर्ड राशि में बदलाव करने का अधिकार है। रेफरल सिस्टम के दुरुपयोग पर वॉलेट सस्पेंड किया जाएगा।",
    ),
)

@Composable
private fun ExpandableRulesContent() {
    var isHindi by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Language toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHindi) Color(0xFF3A6BA5) else Color(0xFF2E5090))
                    .clickable { isHindi = !isHindi }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    if (isHindi) "हिंदी" else "English",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        rulesData.forEachIndexed { index, rule ->
            ExpandableRuleCard(
                title = if (isHindi) rule.titleHi else rule.titleEn,
                content = if (isHindi) rule.contentHi else rule.contentEn,
                index = index + 1,
            )
            if (index < rulesData.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ExpandableRuleCard(title: String, content: String, index: Int) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "arrow",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBlueLighter.copy(alpha = 0.6f))
            .clickable { expanded = !expanded },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(GoldYellow),
                contentAlignment = Alignment.Center,
            ) {
                Text("$index", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotationAngle },
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = expanded,
            enter = androidx.compose.animation.expandVertically(animationSpec = tween(300)) +
                    androidx.compose.animation.fadeIn(animationSpec = tween(300)),
            exit = androidx.compose.animation.shrinkVertically(animationSpec = tween(300)) +
                    androidx.compose.animation.fadeOut(animationSpec = tween(300)),
        ) {
            Text(
                content,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 19.sp,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            )
        }
    }
}
