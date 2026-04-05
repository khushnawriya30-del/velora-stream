package com.cinevault.app.ui.screen

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.R
import com.cinevault.app.data.model.PremiumPlanDto
import com.cinevault.app.ui.viewmodel.PremiumViewModel

// ── Fallback plans if API fails ──
private val FALLBACK_PLANS = listOf(
    PremiumPlanDto("f1", "1m", "1 Month", 1, 159, 182, 10, null, 0, true),
    PremiumPlanDto("f2", "3m", "3 Months", 3, 459, 543, 15, "Most popular", 1, true),
    PremiumPlanDto("f3", "6m", "6 Months", 6, 829, 1038, 20, null, 2, true),
    PremiumPlanDto("f4", "12m", "12 Months", 12, 1529, 2042, 25, "Best Value", 3, true),
)

// ── Image coordinates (from 1080x2400 source) normalized to fractions ──
// Plan cards
private val PLAN_RECTS = listOf(
    floatArrayOf(32f / 1080f, 350f / 2400f, 528f / 1080f, 690f / 2400f),   // 1 Month
    floatArrayOf(552f / 1080f, 350f / 2400f, 1048f / 1080f, 690f / 2400f), // 3 Months
    floatArrayOf(32f / 1080f, 714f / 2400f, 528f / 1080f, 1054f / 2400f),  // 6 Months
    floatArrayOf(552f / 1080f, 714f / 2400f, 1048f / 1080f, 1054f / 2400f),// 12 Months
)
// Action rows
private val ACTION_RECTS = listOf(
    floatArrayOf(32f / 1080f, 1104f / 2400f, 1048f / 1080f, 1214f / 2400f), // Buy Premium Code
    floatArrayOf(32f / 1080f, 1234f / 2400f, 1048f / 1080f, 1344f / 2400f), // Activate with Premium Code
    floatArrayOf(32f / 1080f, 1364f / 2400f, 1048f / 1080f, 1474f / 2400f), // Help Center
)
// PAY NOW button
private val PAY_RECT = floatArrayOf(716f / 1080f, 2226f / 2400f, 1036f / 1080f, 2341f / 2400f)
// Bottom bar text area (for dynamic overlay)
private val BOTTOM_TEXT_RECT = floatArrayOf(0f, 2180f / 2400f, 700f / 1080f, 2400f / 2400f)

@Composable
fun ActivatePremiumScreen(
    onBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
            Toast.makeText(context, "Premium Activated! \uD83C\uDF89", Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    // Animated bottom bar price
    val animatedPrice by animateFloatAsState(
        targetValue = (selectedPlan?.price ?: 159).toFloat(),
        animationSpec = tween(300),
        label = "priceAnim",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val screenW = maxWidth
        val screenH = maxHeight

        // ── Static Background Image ──
        Image(
            painter = painterResource(R.drawable.premium_screen_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )

        // ══════════════════════════════════════════
        // ── Invisible Clickable Overlays ──
        // ══════════════════════════════════════════

        // Plan card overlays (clickable, no visual change)
        plans.take(4).forEachIndexed { index, plan ->
            if (index < PLAN_RECTS.size) {
                val rect = PLAN_RECTS[index]
                Box(
                    modifier = Modifier
                        .offset(
                            x = screenW * rect[0],
                            y = screenH * rect[1],
                        )
                        .size(
                            width = screenW * (rect[2] - rect[0]),
                            height = screenH * (rect[3] - rect[1]),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            selectedPlanId = plan.planId
                        },
                )
            }
        }

        // Action row overlays
        val actionLabels = listOf("Buy Premium Code", "Activate with Premium Code", "Help Center")
        ACTION_RECTS.forEachIndexed { index, rect ->
            Box(
                modifier = Modifier
                    .offset(
                        x = screenW * rect[0],
                        y = screenH * rect[1],
                    )
                    .size(
                        width = screenW * (rect[2] - rect[0]),
                        height = screenH * (rect[3] - rect[1]),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        when (index) {
                            0 -> Toast.makeText(context, "Buy Premium Code — Coming soon", Toast.LENGTH_SHORT).show()
                            1 -> Toast.makeText(context, "Activate Premium Code — Coming soon", Toast.LENGTH_SHORT).show()
                            2 -> Toast.makeText(context, "Help Center — Coming soon", Toast.LENGTH_SHORT).show()
                        }
                    },
            )
        }

        // PAY NOW button overlay
        Box(
            modifier = Modifier
                .offset(
                    x = screenW * PAY_RECT[0],
                    y = screenH * PAY_RECT[1],
                )
                .size(
                    width = screenW * (PAY_RECT[2] - PAY_RECT[0]),
                    height = screenH * (PAY_RECT[3] - PAY_RECT[1]),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    Toast.makeText(
                        context,
                        "Payment flow for ${selectedPlan?.name ?: "plan"} — ₹${selectedPlan?.price ?: 0}",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
        )

        // ══════════════════════════════════════════
        // ── Dynamic Bottom Text Overlay ──
        // ══════════════════════════════════════════
        if (selectedPlan != null) {
            val validDate = java.time.LocalDate.now().plusMonths(selectedPlan.months.toLong())
            val formatted = "${String.format("%02d", validDate.dayOfMonth)} " +
                "${validDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}," +
                "${validDate.year}"

            Box(
                modifier = Modifier
                    .offset(
                        x = screenW * BOTTOM_TEXT_RECT[0],
                        y = screenH * BOTTOM_TEXT_RECT[1],
                    )
                    .size(
                        width = screenW * (BOTTOM_TEXT_RECT[2] - BOTTOM_TEXT_RECT[0]),
                        height = screenH * (BOTTOM_TEXT_RECT[3] - BOTTOM_TEXT_RECT[1]),
                    )
                    .padding(start = 16.dp, top = 12.dp),
            ) {
                Column {
                    Text(
                        "₹${animatedPrice.toInt()}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD4AF37), // Gold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Selected Plan: ${selectedPlan.name}",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Valid until $formatted",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.38f),
                    )
                }
            }
        }
    }
}
