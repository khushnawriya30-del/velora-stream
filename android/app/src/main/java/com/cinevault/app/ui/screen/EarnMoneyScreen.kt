package com.cinevault.app.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.cinevault.app.R
import com.cinevault.app.ui.viewmodel.EarnMoneyViewModel
import kotlinx.coroutines.delay

// ── Color Palette ──
private val DarkBlueBg = Color(0xFF0D1B3E)
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
    viewModel: EarnMoneyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var upiInput by remember { mutableStateOf("") }
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

    // ── Progress calculations ──
    val threshold = state.withdrawThreshold
    val balance = state.balance
    val amountLeft = (threshold - balance).coerceAtLeast(0)
    val progressFraction = (balance.toFloat() / threshold.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlueBg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
    ) {
        // ═══════════════════════════════════════════
        // SECTION 1 — HEADER
        // ═══════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
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
        // SECTION 2 — BALANCE CARD
        // ═══════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(GoldYellow.copy(alpha = 0.5f), GoldAmber.copy(alpha = 0.3f))),
                    shape = RoundedCornerShape(12.dp),
                )
                .background(CardBlue, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            Column {
                // My Balance label with coin
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_earn_coin),
                        contentDescription = "Coin",
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                rotationY = coinRotation
                                cameraDistance = 12f * density
                            },
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("My Balance", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GoldYellow)
                }
                Spacer(Modifier.height(8.dp))

                // Balance amount
                Text(
                    "₹${animatedBalance}.00",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
                Spacer(Modifier.height(14.dp))

                // Withdraw button with breathing animation
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = wdScale
                            scaleY = wdScale
                            alpha = wdGlow
                        }
                        .border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFC107))),
                            shape = RoundedCornerShape(24.dp),
                        )
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFFFFCC80), Color(0xFFFFB74D), Color(0xFFFF9800))),
                            RoundedCornerShape(24.dp),
                        )
                        .clickable { showWithdrawDialog = true }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                ) {
                    Text(
                        "Withdraw",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723),
                    )
                }
            }
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
                    brush = Brush.linearGradient(listOf(GoldAmber.copy(alpha = 0.6f), OrangeBright.copy(alpha = 0.4f))),
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
                    // App icon placeholder
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF5C2D91), Color(0xFFD81B60))),
                                RoundedCornerShape(10.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("V", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Share APP", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    // Invitation Stats button
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .clickable { /* stats */ }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text("Invitation Stats", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Progress Bar with Levels ──
                val levels = listOf("Lv.1", "Lv.2", "Lv.3", "Lv.4")
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Track background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .align(Alignment.Center)
                            .background(Color(0xFF2A3B5C), RoundedCornerShape(4.dp)),
                    )
                    // Green progress fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .height(8.dp)
                            .align(Alignment.CenterStart)
                            .background(
                                Brush.horizontalGradient(listOf(GreenProgress, Color(0xFF66BB6A))),
                                RoundedCornerShape(4.dp),
                            ),
                    )
                    // Progress percentage text inside green bar
                    if (progressFraction > 0.1f) {
                        Text(
                            "${(progressFraction * 100).toInt()}%",
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 6.dp),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    // Level markers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        levels.forEachIndexed { index, level ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(level, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                Spacer(Modifier.height(2.dp))
                                // Coin marker at each level
                                Image(
                                    painter = painterResource(R.drawable.ic_earn_coin),
                                    contentDescription = level,
                                    modifier = Modifier.size(if (index == 3) 24.dp else 20.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Progress text
                Text(
                    buildAnnotatedString {
                        append("Only ")
                        withStyle(SpanStyle(color = GoldYellow, fontWeight = FontWeight.ExtraBold)) {
                            append("₹${amountLeft}.00")
                        }
                        append(" left to withdraw ")
                        withStyle(SpanStyle(color = GoldYellow, fontWeight = FontWeight.ExtraBold)) {
                            append("₹${threshold}")
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
                            brush = Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFF5722))),
                            shape = RoundedCornerShape(10.dp),
                        )
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFFFFB74D), OrangeBright, OrangeDeep, RedOrange)),
                            RoundedCornerShape(10.dp),
                        )
                        .clickable {
                            val shareText =
                                "Join Velora App & Earn Cash! 🎬💰\n\nUse my referral code: ${state.referralCode}\n\nDownload Now: https://velora-stream.web.app"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Velora"))
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
                // SECTION 5 — TIMER
                // ═══════════════════════════════════════════
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("End in ", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("56", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(" Days", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
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
                        Brush.verticalGradient(listOf(CardBlueLighter, CardBlue)),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { /* navigate to withdraw history */ }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Withdraw History", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Go", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }
            // Earning History
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        Brush.verticalGradient(listOf(CardBlueLighter, CardBlue)),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { /* navigate to earning history */ }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Earning History", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Go", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ═══════════════════════════════════════════
        // SECTION 8 — HOW MUCH OTHERS EARNED
        // ═══════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFFFCC80), GoldAmber, Color(0xFFFFB300))),
                    RoundedCornerShape(24.dp),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💬", fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "How much others earned?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontStyle = FontStyle.Italic,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── User testimonials / leaderboard ──
        dummyEarners.forEach { (name, amount) ->
            EarnerItem(name = name, amount = amount)
        }

        Spacer(Modifier.height(20.dp))

        // ── Referral Code Section ──
        if (state.referralCode.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(CardBlue, RoundedCornerShape(12.dp))
                    .border(1.dp, GoldYellow.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Your Referral Code", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
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
                            clipboardManager.setText(AnnotatedString(state.referralCode))
                        }) {
                            Icon(Icons.Default.ContentCopy, "Copy", tint = GoldYellow, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Join Channels Footer ──
        Text(
            "Join channels to learn more",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // WhatsApp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF25D366), RoundedCornerShape(24.dp))
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com")))
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💬", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("WhatsApp", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            // Telegram
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF0088CC), RoundedCornerShape(24.dp))
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me")))
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✈️", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Telegram", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Rules Section (toggleable) ──
        if (showRules) {
            RulesSection()
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(80.dp))
    }

    // ═══════════════════════════════════════════
    // WITHDRAW DIALOG
    // ═══════════════════════════════════════════
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = {
                showWithdrawDialog = false
                viewModel.clearWithdrawState()
            },
            containerColor = CardBlue,
            title = {
                Text("Withdraw Request", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    if (state.withdrawSuccess) {
                        Text("✅ Withdrawal request submitted successfully!", color = GreenProgress, fontSize = 15.sp)
                    } else if (!state.canWithdraw) {
                        Text(
                            "You need ₹${state.amountNeeded} more to withdraw ₹${threshold}",
                            color = Color(0xFFFFAB00),
                            fontSize = 14.sp,
                        )
                    } else {
                        Text(
                            "Amount: ₹${threshold}",
                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = upiInput,
                            onValueChange = { upiInput = it },
                            label = { Text("Enter UPI ID") },
                            placeholder = { Text("example@upi") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldYellow,
                                unfocusedBorderColor = Color(0xFF3A4A6C),
                                cursorColor = GoldYellow,
                                focusedLabelColor = GoldYellow,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (state.withdrawError != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(state.withdrawError!!, color = Color(0xFFFF5252), fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                if (state.withdrawSuccess) {
                    Button(
                        onClick = {
                            showWithdrawDialog = false
                            viewModel.clearWithdrawState()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAmber),
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else if (state.canWithdraw) {
                    Button(
                        onClick = { viewModel.requestWithdrawal(threshold, upiInput.trim()) },
                        enabled = upiInput.contains("@") && !state.isWithdrawing,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAmber),
                    ) {
                        if (state.isWithdrawing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("Confirm Withdraw", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                if (!state.withdrawSuccess) {
                    TextButton(onClick = {
                        showWithdrawDialog = false
                        viewModel.clearWithdrawState()
                    }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            },
        )
    }
}

// ═══════════════════════════════════════════
// Earner Item (leaderboard)
// ═══════════════════════════════════════════
private val dummyEarners = listOf(
    "sharma077" to "₹340",
    "priya_k" to "₹280",
    "rahul_99" to "₹210",
    "sneha_r" to "₹190",
    "amit_j21" to "₹150",
)

@Composable
private fun EarnerItem(name: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(CardBlue.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF3A4A6C)),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.first().uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(name, fontSize = 14.sp, color = Color.White, modifier = Modifier.weight(1f))
        Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenProgress)
    }
}

// ═══════════════════════════════════════════
// Rules Section
// ═══════════════════════════════════════════
@Composable
private fun RulesSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(CardBlue, RoundedCornerShape(12.dp))
            .border(1.dp, GoldYellow.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Column {
            Text("📋 Rules / नियम", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            val rules = listOf(
                "Minimum withdrawal ₹100 / न्यूनतम निकासी ₹100",
                "₹1 per referral / हर रेफरल पर ₹1",
                "Only verified users counted / केवल सत्यापित उपयोगकर्ता",
                "No fake invites allowed / नकली आमंत्रण की अनुमति नहीं",
                "Self-referral not allowed / स्वयं रेफरल की अनुमति नहीं",
                "Withdrawal via UPI only / केवल UPI से निकासी",
                "60-day earn window / 60 दिन की कमाई अवधि",
            )
            rules.forEach { rule ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("•", color = GoldYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(rule, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), lineHeight = 18.sp)
                }
            }
        }
    }
}
