package com.cinevault.app.ui.screen

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpCenter
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

// ── Design Tokens ──
private val GoldPrimary = Color(0xFFD4AF37)
private val GoldLight = Color(0xFFF5D76E)
private val GoldDark = Color(0xFF8B7328)
private val GoldBright = Color(0xFFE8D48B)
private val CardBg = Color(0xFF161B2E)
private val PageBg1 = Color(0xFF0B0F1A)
private val PageBg2 = Color(0xFF121826)
private val TextGold = Color(0xFFF5C518)
private val GreenBadge = Color(0xFF22C55E)

// ── Fallback plans if API fails ──
private val FALLBACK_PLANS = listOf(
    PremiumPlanDto("f1", "1m", "1 Month", 1, 159, 182, 10, null, 0, true),
    PremiumPlanDto("f2", "3m", "3 Months", 3, 459, 543, 15, "Most popular", 1, true),
    PremiumPlanDto("f3", "6m", "6 Months", 6, 829, 1038, 20, null, 2, true),
    PremiumPlanDto("f4", "12m", "12 Months", 12, 1529, 2042, 25, "Best Value", 3, true),
)

// ── Benefit icon ──
private data class PremBenefit(val icon: Int, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivatePremiumScreen(
    onBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }

    // Use API plans, fallback to hardcoded if empty
    val plans = if (uiState.plans.isNotEmpty()) uiState.plans else FALLBACK_PLANS
    var selectedPlanId by remember { mutableStateOf<String?>(null) }

    // Auto-select first plan once plans are available
    val selectedPlan = plans.firstOrNull { it.planId == selectedPlanId } ?: plans.firstOrNull()
    LaunchedEffect(plans) {
        if (selectedPlanId == null && plans.isNotEmpty()) {
            selectedPlanId = plans.first().planId
        }
    }

    LaunchedEffect(uiState.activationSuccess) {
        if (uiState.activationSuccess) {
            Toast.makeText(context, "Premium Activated! 🎉", Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val benefits = remember {
        listOf(
            PremBenefit(R.drawable.ic_prem_unlimited, "Unlimited"),
            PremBenefit(R.drawable.ic_prem_first, "Premium\nFirst"),
            PremBenefit(R.drawable.ic_prem_no_ads, "No Ads"),
            PremBenefit(R.drawable.ic_prem_fhd, "FHD\n1080P"),
            PremBenefit(R.drawable.ic_prem_exclusive, "Premium\nExclusive"),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PageBg1, PageBg2))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── Top Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    "Premium Plan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Scrollable Content ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // User info
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A3A)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Person, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Premium User",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Benefits Icon Row ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E2233), Color(0xFF181C28)),
                            ),
                        )
                        .border(0.5.dp, GoldDark.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp),
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(benefits) { benefit ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(80.dp),
                            ) {
                                Image(
                                    painter = painterResource(benefit.icon),
                                    contentDescription = benefit.label,
                                    modifier = Modifier.size(52.dp),
                                    contentScale = ContentScale.Fit,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    benefit.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Plan Cards (2×2 Grid) ──
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val chunkedPlans = plans.chunked(2)
                    chunkedPlans.forEachIndexed { index, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { plan ->
                                PlanCard(
                                    plan = plan,
                                    isSelected = selectedPlanId == plan.planId,
                                    onClick = { selectedPlanId = plan.planId },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            // Fill remaining space if odd number
                            if (row.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        if (index < chunkedPlans.size - 1) {
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Action Rows ──
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ActionRow(icon = Icons.Filled.ShoppingCart, label = "Buy Premium Code")
                    Spacer(Modifier.height(10.dp))
                    ActionRow(icon = Icons.Filled.Key, label = "Activate with Premium Code") {
                        // TODO: navigate to code entry or show dialog
                    }
                    Spacer(Modifier.height(10.dp))
                    ActionRow(icon = Icons.AutoMirrored.Filled.HelpCenter, label = "Help Center")
                }

                // Activation code (inline) 
                Spacer(Modifier.height(20.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase().take(14) },
                        label = { Text("Activation Code") },
                        placeholder = { Text("VLRA-XXXX-XXXX") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (code.isNotBlank() && !uiState.isActivating) {
                                    viewModel.activateCode(code.trim())
                                }
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = GoldPrimary,
                            cursorColor = GoldPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    ActivateCodeButton(
                        isLoading = uiState.isActivating,
                        enabled = code.isNotBlank() && !uiState.isActivating,
                        onClick = { viewModel.activateCode(code.trim()) },
                    )
                }

                Spacer(Modifier.height(100.dp)) // space for bottom bar
            }

            // ── Bottom Payment Bar ──
            if (selectedPlan != null) {
                BottomPayBar(
                    plan = selectedPlan,
                    onPayClick = {
                        // TODO: wire payment
                        Toast.makeText(context, "Payment flow coming soon", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// ── Plan Card ──
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun PlanCard(
    plan: PremiumPlanDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(200),
        label = "planScale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.3f else 0f,
        animationSpec = tween(200),
        label = "planGlow",
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .then(
                if (isSelected) Modifier.border(1.5.dp, GoldPrimary, RoundedCornerShape(16.dp))
                else Modifier.border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            )
            .clickable(onClick = onClick),
    ) {
        // Top gold glow for selected
        if (glowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(GoldPrimary.copy(alpha = glowAlpha), Color.Transparent),
                        ),
                    ),
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Badge + Discount row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!plan.badge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (plan.badge == "Most popular") GreenBadge
                                else GoldPrimary,
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(plan.badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "${plan.discountPercent}% Off",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextGold,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Plan label
            Text(plan.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.8f))

            Spacer(Modifier.height(8.dp))

            // Price
            Row(verticalAlignment = Alignment.Bottom) {
                Text("₹", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    plan.price.toString(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 32.sp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "₹${plan.originalPrice}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            // Per-month for multi-month plans
            if (plan.months > 1) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "₹${plan.price / plan.months}/month",
                    fontSize = 12.sp,
                    color = TextGold.copy(alpha = 0.8f),
                )
            }
        }

        // Checkmark badge for selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(GoldPrimary, GoldLight)),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// ── Action Row ──
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════
// ── Activate Code Button ──
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ActivateCodeButton(isLoading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "actBtn",
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(),
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) Brush.horizontalGradient(listOf(GoldPrimary, GoldLight, GoldPrimary))
                    else Brush.horizontalGradient(listOf(GoldDark.copy(alpha = 0.3f), GoldDark.copy(alpha = 0.2f))),
                    RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                Text("ACTIVATE CODE", fontWeight = FontWeight.Bold, color = if (enabled) Color(0xFF1A1200) else Color.White.copy(alpha = 0.4f), letterSpacing = 1.sp, fontSize = 13.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// ── Bottom Payment Bar ──
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun BottomPayBar(plan: PremiumPlanDto, onPayClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val btnScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "payBtn",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1C28), Color(0xFF14161F)),
                ),
            )
            .border(
                width = 0.5.dp,
                color = GoldDark.copy(alpha = 0.2f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "₹${plan.price}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(
                    "Selected Plan: ${plan.name}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                )
                if (plan.months > 0) {
                    val validDate = java.time.LocalDate.now().plusMonths(plan.months.toLong())
                    val formatted = "${String.format("%02d", validDate.dayOfMonth)} ${validDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }},${validDate.year}"
                    Text(
                        "Valid until $formatted",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.35f),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFD4AF37), Color(0xFFF5C518), Color(0xFFB8961E)),
                        ),
                    )
                    .graphicsLayer { scaleX = btnScale; scaleY = btnScale }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onPayClick,
                    )
                    .padding(horizontal = 32.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "PAY NOW",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1200),
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}
