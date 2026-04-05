package com.cinevault.app.ui.screen

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.R
import com.cinevault.app.data.model.PremiumPlanDto
import com.cinevault.app.ui.viewmodel.PremiumViewModel

// ── Theme Colors ──
private val BgPrimary = Color(0xFF121212)
private val BgSecondary = Color(0xFF1A1A1A)
private val CardBg = Color(0xFF1F1F1F)
private val CardBgLight = Color(0xFF252525)
private val Gold = Color(0xFFD4AF37)
private val GoldDark = Color(0xFFC9A227)
private val GoldSoft = Color(0xFFE6C55A)
private val GoldDim = Color(0xFF8A7328)
private val White87 = Color.White.copy(alpha = 0.87f)
private val White60 = Color.White.copy(alpha = 0.60f)
private val White38 = Color.White.copy(alpha = 0.38f)
private val GreenBadge = Color(0xFF22C55E)

// ── Fallback plans if API fails ──
private val FALLBACK_PLANS = listOf(
    PremiumPlanDto("f1", "1m", "1 Month", 1, 159, 182, 10, null, 0, true),
    PremiumPlanDto("f2", "3m", "3 Months", 3, 459, 543, 15, "Most popular", 1, true),
    PremiumPlanDto("f3", "6m", "6 Months", 6, 829, 1038, 20, null, 2, true),
    PremiumPlanDto("f4", "12m", "12 Months", 12, 1529, 2042, 25, "Best Value", 3, true),
)

@Composable
fun ActivatePremiumScreen(
    onBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val plans = if (uiState.plans.isNotEmpty()) uiState.plans else FALLBACK_PLANS
    var selectedPlanId by remember { mutableStateOf<String?>(null) }
    val selectedPlan = plans.firstOrNull { it.planId == selectedPlanId } ?: plans.firstOrNull()

    LaunchedEffect(plans) {
        if (selectedPlanId == null && plans.isNotEmpty()) selectedPlanId = plans.first().planId
    }
    LaunchedEffect(uiState.activationSuccess) {
        if (uiState.activationSuccess) {
            Toast.makeText(context, "Premium Activated! \uD83C\uDF89", Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val animatedPrice by animateFloatAsState(
        targetValue = (selectedPlan?.price ?: 159).toFloat(),
        animationSpec = tween(250), label = "price",
    )

    Scaffold(
        containerColor = BgPrimary,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 120.dp) // room for bottom bar
            ) {
                // ═══════════════════════════════════════
                //  TOP BAR
                // ═══════════════════════════════════════
                Spacer(Modifier.statusBarsPadding())
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = White87)
                    }
                    Text(
                        "Premium Plans",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                // ═══════════════════════════════════════
                //  PROFILE SECTION
                // ═══════════════════════════════════════
                ProfileCard(
                    userName = uiState.userName ?: "Premium User",
                    userId = uiState.userId,
                )

                Spacer(Modifier.height(20.dp))

                // ═══════════════════════════════════════
                //  PREMIUM FEATURES ROW
                // ═══════════════════════════════════════
                PremiumFeaturesRow()

                Spacer(Modifier.height(24.dp))

                // ═══════════════════════════════════════
                //  PLAN CARDS GRID (2×2)
                // ═══════════════════════════════════════
                Text(
                    "Choose Your Plan",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White87,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(14.dp))

                val rows = plans.take(4).chunked(2)
                rows.forEach { rowPlans ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowPlans.forEach { plan ->
                            PlanCard(
                                plan = plan,
                                isSelected = plan.planId == selectedPlan?.planId,
                                onClick = { selectedPlanId = plan.planId },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Fill empty slot if odd number
                        if (rowPlans.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(16.dp))

                // ═══════════════════════════════════════
                //  ACTION ROWS
                // ═══════════════════════════════════════
                ActionRow(
                    icon = R.drawable.ic_prem_first,
                    label = "Buy Premium Code",
                    onClick = {
                        Toast.makeText(context, "Buy Premium Code — Coming soon", Toast.LENGTH_SHORT).show()
                    },
                )
                ActionRow(
                    icon = R.drawable.ic_prem_unlimited,
                    label = "Activate with Premium Code",
                    onClick = {
                        Toast.makeText(context, "Activate Premium Code — Coming soon", Toast.LENGTH_SHORT).show()
                    },
                )
                ActionRow(
                    icon = R.drawable.ic_prem_exclusive,
                    label = "Help Center",
                    onClick = {
                        Toast.makeText(context, "Help Center — Coming soon", Toast.LENGTH_SHORT).show()
                    },
                )

                Spacer(Modifier.height(24.dp))
            }

            // ═══════════════════════════════════════
            //  BOTTOM PAY BAR (sticky)
            // ═══════════════════════════════════════
            BottomPayBar(
                selectedPlan = selectedPlan,
                animatedPrice = animatedPrice,
                onPayClick = {
                    Toast.makeText(
                        context,
                        "Payment flow for ${selectedPlan?.name ?: "plan"} — ₹${selectedPlan?.price ?: 0}",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

// ══════════════════════════════════════════
// ── Profile Card ──
// ══════════════════════════════════════════
@Composable
private fun ProfileCard(userName: String, userId: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    Brush.verticalGradient(listOf(Gold, GoldDark)),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                userName.take(1).uppercase(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = BgPrimary,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                userName,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = White87,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (userId != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "ID: ${userId.takeLast(8)}",
                    fontSize = 13.sp,
                    color = White38,
                )
            }
        }
    }
}

// ══════════════════════════════════════════
// ── Premium Features Row ──
// ══════════════════════════════════════════
@Composable
private fun PremiumFeaturesRow() {
    val features = listOf(
        R.drawable.ic_prem_unlimited to "Unlimited",
        R.drawable.ic_prem_first to "First\nAccess",
        R.drawable.ic_prem_no_ads to "No Ads",
        R.drawable.ic_prem_fhd to "FHD\n1080P",
        R.drawable.ic_prem_exclusive to "Exclusive",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(vertical = 18.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        features.forEach { (iconRes, label) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    label,
                    fontSize = 11.sp,
                    color = White60,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    maxLines = 2,
                )
            }
        }
    }
}

// ══════════════════════════════════════════
// ── Plan Card ──
// ══════════════════════════════════════════
@Composable
private fun PlanCard(
    plan: PremiumPlanDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Gold else Color.Transparent,
        animationSpec = tween(200), label = "border",
    )
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        animationSpec = tween(200), label = "elev",
    )

    Box(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(18.dp), ambientColor = if (isSelected) Gold.copy(alpha = 0.3f) else Color.Transparent)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp),
            )
            .background(CardBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Gold gradient highlight at top when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(listOf(GoldDark, Gold, GoldSoft)),
                            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                        )
                )
            } else {
                Spacer(Modifier.height(4.dp))
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                // Badge + Discount row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (plan.badge != null) {
                        Text(
                            plan.badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(
                                    if (plan.badge == "Most popular") GreenBadge else Gold,
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    Text(
                        "${plan.discountPercent}% Off",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Gold,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Plan name
                Text(
                    plan.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = White87,
                )

                Spacer(Modifier.height(8.dp))

                // Price row
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("₹", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                    Text(
                        "${plan.price}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Gold,
                        lineHeight = 34.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "₹${plan.originalPrice}",
                        fontSize = 13.sp,
                        color = White38,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                // Per month
                if (plan.months > 1) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "₹${plan.price / plan.months}/month",
                        fontSize = 12.sp,
                        color = GoldSoft.copy(alpha = 0.8f),
                    )
                }
            }

            // Checkmark at bottom-right when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp, bottom = 10.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = Gold,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ══════════════════════════════════════════
// ── Action Row ──
// ══════════════════════════════════════════
@Composable
private fun ActionRow(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .background(CardBg, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            fontSize = 15.sp,
            color = White87,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = GoldDim,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ══════════════════════════════════════════
// ── Bottom Pay Bar ──
// ══════════════════════════════════════════
@Composable
private fun BottomPayBar(
    selectedPlan: PremiumPlanDto?,
    animatedPrice: Float,
    onPayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedPlan == null) return

    val validDate = java.time.LocalDate.now().plusMonths(selectedPlan.months.toLong())
    val formatted = "${String.format("%02d", validDate.dayOfMonth)} " +
        "${validDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}, " +
        "${validDate.year}"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, BgPrimary),
                    startY = 0f,
                    endY = 40f,
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSecondary)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "₹${animatedPrice.toInt()}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Gold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${selectedPlan.name} • until $formatted",
                    fontSize = 12.sp,
                    color = White38,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onPayClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(listOf(GoldDark, Gold, GoldSoft)),
                        RoundedCornerShape(14.dp),
                    ),
            ) {
                Text(
                    "PAY NOW",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BgPrimary,
                )
            }
        }
    }
}
