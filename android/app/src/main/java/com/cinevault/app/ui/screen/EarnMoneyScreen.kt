package com.cinevault.app.ui.screen

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.EarnMoneyViewModel
import kotlinx.coroutines.delay

// Gold palette
private val Gold = Color(0xFFC5A44E)
private val GoldLight = Color(0xFFF2D078)
private val GoldDark = Color(0xFF8B7330)
private val GoldBrush = Brush.linearGradient(listOf(Gold, GoldLight, Gold))
private val DarkSurface = Color(0xFF121212)
private val DarkCard = Color(0xFF1A1A1A)
private val DarkCardBorder = Color(0xFF2A2A2A)

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        // ── Top Bar ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    "Earn Cash",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                // Animated coin
                AnimatedCoinIcon()
            }
        }

        // ── Title ──
        item {
            Text(
                "Earn Cash by Inviting Friends",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    brush = GoldBrush,
                    shadow = Shadow(Gold.copy(alpha = 0.3f), Offset(0f, 2f), 8f),
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Balance Card ──
        item {
            BalanceCard(
                balance = state.balance,
                amountNeeded = state.amountNeeded,
                threshold = state.withdrawThreshold,
                canWithdraw = state.canWithdraw,
                isLoading = state.isLoading,
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Invite Section ──
        item {
            InviteSection(
                referralCode = state.referralCode,
                onShare = {
                    val shareText = "Join Velora App & Earn Cash! 🎬💰\n\nUse my referral code: ${state.referralCode}\n\nDownload Now: https://velora-stream.web.app"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Velora"))
                },
                onCopyCode = {
                    clipboardManager.setText(AnnotatedString(state.referralCode))
                },
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Withdraw Button ──
        item {
            WithdrawSection(
                canWithdraw = state.canWithdraw,
                balance = state.balance,
                threshold = state.withdrawThreshold,
                amountNeeded = state.amountNeeded,
                onWithdrawClick = { showWithdrawDialog = true },
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Invite Status Card ──
        item {
            InviteStatusCard(
                totalInvited = state.totalInvited,
                totalEarned = state.totalEarned,
                totalWithdrawn = state.totalWithdrawn,
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Withdrawal History ──
        if (state.withdrawals.isNotEmpty()) {
            item {
                SectionHeader("Withdrawal History")
            }
            items(state.withdrawals) { w ->
                HistoryCard(
                    amount = "₹${w.amount}",
                    status = w.status.replaceFirstChar { it.uppercase() },
                    subtitle = w.upiId,
                    statusColor = when (w.status) {
                        "approved" -> Color(0xFF4CAF50)
                        "rejected" -> Color(0xFFFF5252)
                        else -> Gold
                    },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // ── Earnings History ──
        if (state.earnings.isNotEmpty()) {
            item {
                SectionHeader("Earnings History")
            }
            items(state.earnings.take(20)) { e ->
                HistoryCard(
                    amount = "+₹${e.amount}",
                    status = "Invite",
                    subtitle = e.description,
                    statusColor = Color(0xFF4CAF50),
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // ── User Feedbacks ──
        item {
            SectionHeader("What Users Say")
            Spacer(Modifier.height(8.dp))
        }
        items(dummyFeedbacks) { fb ->
            FeedbackCard(fb.first, fb.second)
        }

        // ── Rules ──
        item {
            Spacer(Modifier.height(20.dp))
            RulesSection()
            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Withdraw Dialog ──
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = {
                showWithdrawDialog = false
                viewModel.clearWithdrawState()
            },
            containerColor = DarkCard,
            title = {
                Text(
                    "Withdraw Request",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column {
                    if (state.withdrawSuccess) {
                        Text(
                            "✅ Withdrawal request submitted successfully!",
                            color = Color(0xFF4CAF50),
                            fontSize = 15.sp,
                        )
                    } else {
                        Text(
                            "Amount: ₹${state.withdrawThreshold}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = upiInput,
                            onValueChange = { upiInput = it },
                            label = { Text("Enter UPI ID") },
                            placeholder = { Text("example@upi") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = DarkCardBorder,
                                cursorColor = Gold,
                                focusedLabelColor = Gold,
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
                if (!state.withdrawSuccess) {
                    Button(
                        onClick = {
                            viewModel.requestWithdrawal(state.withdrawThreshold, upiInput.trim())
                        },
                        enabled = upiInput.contains("@") && !state.isWithdrawing,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    ) {
                        if (state.isWithdrawing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Confirm Withdraw", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            showWithdrawDialog = false
                            viewModel.clearWithdrawState()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
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
// Animated Coin Icon
// ═══════════════════════════════════════════
@Composable
private fun AnimatedCoinIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "coin")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "coinScale",
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "coinRot",
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(GoldLight, Gold, GoldDark))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "₹",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF3A2A00),
            modifier = Modifier.offset(y = (-1).dp),
        )
    }
}

// ═══════════════════════════════════════════
// Balance Card with animated counter
// ═══════════════════════════════════════════
@Composable
private fun BalanceCard(
    balance: Int,
    amountNeeded: Int,
    threshold: Int,
    canWithdraw: Boolean,
    isLoading: Boolean,
) {
    var animatedBalance by remember { mutableIntStateOf(0) }

    LaunchedEffect(balance) {
        if (balance > 0) {
            val steps = 30
            val stepDelay = 800L / steps
            for (i in 1..steps) {
                animatedBalance = (balance * i) / steps
                delay(stepDelay)
            }
            animatedBalance = balance
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1E1A0F),
                            Color(0xFF2A2210),
                            Color(0xFF1E1A0F),
                        )
                    )
                )
                .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("My Balance", color = Color(0xFFAA9960), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedCoinIcon()
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "₹$animatedBalance",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(brush = GoldBrush),
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (!canWithdraw && amountNeeded > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Gold.copy(alpha = 0.12f),
                    ) {
                        Text(
                            "Only ₹$amountNeeded more to withdraw ₹$threshold",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldLight,
                        )
                    }
                } else if (canWithdraw) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                    ) {
                        Text(
                            "✅ Ready to withdraw!",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50),
                        )
                    }
                }

                if (isLoading) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Gold,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Invite Section
// ═══════════════════════════════════════════
@Composable
private fun InviteSection(
    referralCode: String,
    onShare: () -> Unit,
    onCopyCode: () -> Unit,
) {
    val bounceTransition = rememberInfiniteTransition(label = "gift")
    val giftScale by bounceTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "giftScale",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Animated gift
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(giftScale)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(GoldLight.copy(alpha = 0.3f), Color.Transparent))),
                contentAlignment = Alignment.Center,
            ) {
                Text("🎁", fontSize = 36.sp)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Invite New Users to Earn Cash",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Earn ₹1 for every successful invite",
                fontSize = 14.sp,
                color = Color(0xFF888888),
            )

            Spacer(Modifier.height(16.dp))

            // Referral code
            if (referralCode.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF222222),
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { onCopyCode() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            referralCode,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight,
                            letterSpacing = 2.sp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            Icons.Default.ContentCopy,
                            "Copy",
                            tint = Gold,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Tap to copy code", fontSize = 11.sp, color = Color(0xFF666666))
            }

            Spacer(Modifier.height(16.dp))

            // Share button
            Button(
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GoldBrush, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Share,
                            "Share",
                            tint = Color(0xFF1A1A00),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Share App & Earn",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1A00),
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Withdraw Section
// ═══════════════════════════════════════════
@Composable
private fun WithdrawSection(
    canWithdraw: Boolean,
    balance: Int,
    threshold: Int,
    amountNeeded: Int,
    onWithdrawClick: () -> Unit,
) {
    val glowAnim = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowAnim.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onWithdrawClick,
            enabled = canWithdraw,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canWithdraw) Gold else Color(0xFF333333),
                disabledContainerColor = Color(0xFF333333),
            ),
        ) {
            if (canWithdraw) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    "Withdraw",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                if (canWithdraw) "Withdraw ₹$threshold"
                else "Need ₹$amountNeeded more to withdraw",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (canWithdraw) Color.Black else Color(0xFF888888),
            )
        }
    }
}

// ═══════════════════════════════════════════
// Invite Status Card
// ═══════════════════════════════════════════
@Composable
private fun InviteStatusCard(
    totalInvited: Int,
    totalEarned: Int,
    totalWithdrawn: Int,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Invite Status", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem("Invited", "$totalInvited", Icons.Default.People)
                StatItem("Earned", "₹$totalEarned", Icons.Default.TrendingUp)
                StatItem("Withdrawn", "₹$totalWithdrawn", Icons.Default.AccountBalanceWallet)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = Gold, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 12.sp, color = Color(0xFF888888))
    }
}

// ═══════════════════════════════════════════
// Section Header
// ═══════════════════════════════════════════
@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
}

// ═══════════════════════════════════════════
// History Card
// ═══════════════════════════════════════════
@Composable
private fun HistoryCard(
    amount: String,
    status: String,
    subtitle: String,
    statusColor: Color,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(amount, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subtitle, fontSize = 13.sp, color = Color(0xFF888888), maxLines = 1)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.15f),
            ) {
                Text(
                    status,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// Feedback Section
// ═══════════════════════════════════════════
private val dummyFeedbacks = listOf(
    "Rahul K." to "Great app to earn money! Withdrawal was smooth 💰",
    "Priya S." to "Very easy to use. Got ₹100 in 2 days!",
    "Amit J." to "Best streaming app with earn feature 🎬",
    "Sneha R." to "Smooth withdrawal to UPI. Highly recommended!",
    "Vikram T." to "Invited 50 friends, earned ₹50 easily",
    "Neha P." to "Genuine app, real money credited to my account",
    "Rohan M." to "Love the dark theme and gold design ✨",
)

@Composable
private fun FeedbackCard(name: String, feedback: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.first().toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(feedback, fontSize = 13.sp, color = Color(0xFF999999), lineHeight = 18.sp)
                Row {
                    repeat(5) {
                        Text("⭐", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Rules Section
// ═══════════════════════════════════════════
@Composable
private fun RulesSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("📋 Rules / नियम", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))

            val rules = listOf(
                "Minimum withdrawal ₹100 / न्यूनतम निकासी ₹100",
                "₹1 per referral / हर रेफरल पर ₹1",
                "Only verified users counted / केवल सत्यापित उपयोगकर्ता",
                "No fake invites allowed / नकली आमंत्रण की अनुमति नहीं",
                "Self-referral not allowed / स्वयं रेफरल की अनुमति नहीं",
                "Withdrawal via UPI only / केवल UPI से निकासी",
            )
            rules.forEach { rule ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("•", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(rule, fontSize = 13.sp, color = Color(0xFF999999), lineHeight = 18.sp)
                }
            }
        }
    }
}
