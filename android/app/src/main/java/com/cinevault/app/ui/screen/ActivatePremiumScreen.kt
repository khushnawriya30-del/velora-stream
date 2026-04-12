package com.cinevault.app.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.MainActivity
import com.cinevault.app.R
import com.cinevault.app.RazorpayPaymentResult
import com.cinevault.app.data.model.PremiumPlanDto
import com.cinevault.app.data.model.PremiumOfferDto
import com.cinevault.app.ui.viewmodel.PremiumViewModel
import com.razorpay.Checkout
import org.json.JSONObject
import com.cinevault.app.ui.theme.LocalAppDimens

// Dark Luxury Theme
private val BgPrimary = Color(0xFF121212)
private val BgSecondary = Color(0xFF1A1A1A)
private val CardBg = Color(0xFF1E1E1E)
private val Gold = Color(0xFFD4AF37)
private val GoldDark = Color(0xFFC9A227)
private val GoldSoft = Color(0xFFE6C55A)
private val GoldFaint = Color(0xFF3D3520)
private val White87 = Color.White.copy(alpha = 0.87f)
private val White60 = Color.White.copy(alpha = 0.60f)
private val White40 = Color.White.copy(alpha = 0.40f)
private val GreenBadge = Color(0xFF22C55E)

private val FALLBACK_PLANS = listOf(
    PremiumPlanDto("f1", "1m", "1 Month", 1, 1, 182, 1, null, 0, true),
    PremiumPlanDto("f2", "3m", "3 Months", 3, 1, 543, 1, "Most popular", 1, true),
    PremiumPlanDto("f3", "6m", "6 Months", 6, 1, 1038, 1, null, 2, true),
    PremiumPlanDto("f4", "12m", "12 Months", 12, 1, 2042, 1, "Best Value", 3, true),
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

    // Dialog states
    var showActivateDialog by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var showPaymentProcessing by remember { mutableStateOf(false) }
    var showPaymentResult by remember { mutableStateOf(false) }
    // Razorpay result tracking for dialog
    var razorpayPaymentFailed by remember { mutableStateOf(false) }
    var razorpayFailMessage by remember { mutableStateOf<String?>(null) }

    // Lifecycle: refresh premium status when app resumes
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Razorpay: Open checkout when order is ready ──
    val activity = context as? Activity
    LaunchedEffect(uiState.razorpayOrder) {
        val order = uiState.razorpayOrder ?: return@LaunchedEffect
        if (activity == null) return@LaunchedEffect
        try {
            val checkout = Checkout()
            checkout.setKeyID(order.keyId)
            val options = JSONObject().apply {
                put("name", "Velora Premium")
                put("description", order.planName)
                put("order_id", order.orderId)
                put("amount", order.amount) // in paise
                put("currency", order.currency)
                put("theme", JSONObject().put("color", "#D4AF37"))
                put("prefill", JSONObject().apply {
                    put("email", uiState.userName ?: "")
                })
            }
            checkout.open(activity, options)
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening payment: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ── Razorpay: Listen for payment result from Activity ──
    LaunchedEffect(Unit) {
        val mainActivity = activity as? MainActivity ?: return@LaunchedEffect
        mainActivity.razorpayResult.collect { result ->
            when (result) {
                is RazorpayPaymentResult.Success -> {
                    showPaymentProcessing = true
                    razorpayPaymentFailed = false
                    viewModel.verifyRazorpayPayment(
                        paymentId = result.paymentId,
                        orderId = result.orderId,
                        signature = result.signature,
                    )
                }
                is RazorpayPaymentResult.Error -> {
                    razorpayPaymentFailed = true
                    razorpayFailMessage = result.message
                    showPaymentResult = true
                }
            }
        }
    }

    // ── Razorpay verify result ──
    LaunchedEffect(uiState.razorpaySuccess) {
        if (uiState.razorpaySuccess) {
            showPaymentProcessing = false
            razorpayPaymentFailed = false
            showPaymentResult = true
        }
    }
    LaunchedEffect(uiState.razorpayFailed) {
        if (uiState.razorpayFailed) {
            showPaymentProcessing = false
            razorpayPaymentFailed = true
            razorpayFailMessage = uiState.razorpayMessage
            showPaymentResult = true
        }
    }

    LaunchedEffect(plans) {
        if (selectedPlanId == null && plans.isNotEmpty()) selectedPlanId = plans.first().planId
    }

    // Premium status auto-navigation after browser payment
    LaunchedEffect(uiState.isPremium) {
        if (uiState.isPremium && uiState.plan != null) {
            Toast.makeText(context, "Premium Activated! \uD83C\uDF89", Toast.LENGTH_LONG).show()
        }
    }

    // On successful activation via code
    LaunchedEffect(uiState.activationSuccess) {
        if (uiState.activationSuccess) {
            showActivateDialog = false
            codeInput = ""
            Toast.makeText(context, "Premium Activated! \uD83C\uDF89", Toast.LENGTH_LONG).show()
        }
    }

    // Show errors as toasts
    LaunchedEffect(uiState.error) {
        if (uiState.error != null && !showActivateDialog) {
            Toast.makeText(context, uiState.error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val animatedPrice by animateFloatAsState(
        targetValue = (selectedPlan?.price ?: 1).toFloat(),
        animationSpec = tween(200), label = "price",
    )

    // Payment Processing Dialog (spinner) - Razorpay verification
    if (showPaymentProcessing && uiState.isVerifyingRazorpay) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = CardBg,
            titleContentColor = White87,
            title = {
                Text("Verifying Payment", fontWeight = FontWeight.Bold, fontSize = LocalAppDimens.current.font18)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad16),
                ) {
                    CircularProgressIndicator(
                        color = Gold, modifier = Modifier.size(40.dp), strokeWidth = LocalAppDimens.current.strokeWidth
                    )
                    Text(
                        "Please wait while we verify your payment and activate premium...",
                        fontSize = LocalAppDimens.current.font14,
                        color = White60,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            confirmButton = {},
        )
    }

    // Payment Result Dialog - Razorpay
    if (showPaymentResult) {
        val isSuccess = uiState.razorpaySuccess && !razorpayPaymentFailed
        AlertDialog(
            onDismissRequest = {
                showPaymentResult = false
                razorpayPaymentFailed = false
                razorpayFailMessage = null
                viewModel.clearPaymentState()
            },
            containerColor = CardBg,
            titleContentColor = White87,
            title = {
                Text(
                    if (isSuccess) "Payment Successful!" else "Payment Failed",
                    fontWeight = FontWeight.Bold,
                    fontSize = LocalAppDimens.current.font18,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad12),
                ) {
                    if (isSuccess) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            "Success",
                            tint = GreenBadge,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            uiState.razorpayMessage ?: "Premium activated successfully!",
                            fontSize = LocalAppDimens.current.font14,
                            color = White87,
                            textAlign = TextAlign.Center,
                        )
                        val resp = uiState.razorpayVerifyResponse
                        if (resp?.daysRemaining != null) {
                            Text(
                                "Plan: ${resp.premiumPlan ?: ""} - ${resp.daysRemaining} days",
                                fontSize = LocalAppDimens.current.font13,
                                color = GoldSoft,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Text(
                            "\u274C",
                            fontSize = LocalAppDimens.current.font36,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            razorpayFailMessage
                                ?: uiState.razorpayMessage
                                ?: "Payment could not be verified. Please try again.",
                            fontSize = LocalAppDimens.current.font14,
                            color = White60,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPaymentResult = false
                        razorpayPaymentFailed = false
                        razorpayFailMessage = null
                        viewModel.clearPaymentState()
                        if (isSuccess) onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold, contentColor = BgPrimary
                    ),
                ) {
                    Text(if (isSuccess) "Done" else "OK", fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    // Activate Code Dialog
    if (showActivateDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isActivating) {
                    showActivateDialog = false
                    codeInput = ""
                }
            },
            containerColor = CardBg,
            titleContentColor = White87,
            textContentColor = White60,
            title = {
                Text("Activate Premium Code", fontWeight = FontWeight.Bold, fontSize = LocalAppDimens.current.font18)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad12)) {
                    Text(
                        "Enter your activation code to unlock Premium features.",
                        fontSize = LocalAppDimens.current.font13,
                        color = White60,
                    )
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase().take(14) },
                        label = { Text("Premium Code", color = White40) },
                        placeholder = {
                            Text("VLRA-XXXX-XXXX", color = White40.copy(alpha = 0.5f))
                        },
                        singleLine = true,
                        enabled = !uiState.isActivating,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (codeInput.isNotBlank() && !uiState.isActivating) {
                                    viewModel.activateCode(codeInput.trim())
                                }
                            },
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = White40.copy(alpha = 0.3f),
                            focusedLabelColor = Gold,
                            cursorColor = Gold,
                            focusedTextColor = White87,
                            unfocusedTextColor = White87,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (uiState.error != null) {
                        Text(
                            uiState.error ?: "",
                            fontSize = LocalAppDimens.current.font12,
                            color = Color(0xFFEF4444),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.activateCode(codeInput.trim()) },
                    enabled = codeInput.isNotBlank() && !uiState.isActivating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = BgPrimary,
                        disabledContainerColor = Gold.copy(alpha = 0.4f),
                    ),
                ) {
                    if (uiState.isActivating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = BgPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(LocalAppDimens.current.pad8))
                    }
                    Text(
                        if (uiState.isActivating) "Activating..." else "Activate",
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showActivateDialog = false
                        codeInput = ""
                        viewModel.clearError()
                    },
                    enabled = !uiState.isActivating,
                ) {
                    Text("Cancel", color = White60)
                }
            },
        )
    }

    // Main Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalAppDimens.current.pad8, vertical = LocalAppDimens.current.pad6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Back",
                        tint = White87,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    "Premium Plan",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = White87,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = LocalAppDimens.current.padTiny),
                )
            }

            // SCROLLABLE CONTENT
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Profile
                val userName = uiState.userName ?: "Premium User"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = LocalAppDimens.current.pad6),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF333333), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = userName.firstOrNull()?.uppercase() ?: "P",
                            fontSize = LocalAppDimens.current.font18,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF888888),
                        )
                    }
                    Spacer(Modifier.width(LocalAppDimens.current.pad12))
                    Text(
                        userName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White87
                    )
                }

                Spacer(Modifier.height(LocalAppDimens.current.pad10))

                // Premium Features
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    BgPrimary,
                                    Color(0xFF1E1A12),
                                    Color(0xFF2A2214),
                                    Color(0xFF1E1A12),
                                    BgPrimary,
                                ),
                            ),
                        )
                        .padding(vertical = LocalAppDimens.current.pad20),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LocalAppDimens.current.pad16),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        val features = listOf(
                            R.drawable.ic_prem_unlimited to "Unlimited",
                            R.drawable.ic_prem_first to "Premium\nFirst",
                            R.drawable.ic_prem_no_ads to "No Ads",
                            R.drawable.ic_prem_fhd to "FHD\n1080P",
                            R.drawable.ic_prem_exclusive to "Premium\nExclusive",
                        )
                        features.forEach { (iconRes, label) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f),
                            ) {
                                Image(
                                    painter = painterResource(iconRes),
                                    contentDescription = label,
                                    modifier = Modifier.size(42.dp),
                                    contentScale = ContentScale.Fit,
                                )
                                Spacer(Modifier.height(LocalAppDimens.current.pad6))
                                Text(
                                    label,
                                    fontSize = LocalAppDimens.current.font10,
                                    color = White60,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 13.sp,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(LocalAppDimens.current.pad14))

                // ── Dynamic Offer Banner (from Admin Panel) ──
                val activeOffers = uiState.offers
                if (activeOffers.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LocalAppDimens.current.pad16),
                        verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad10),
                    ) {
                        activeOffers.forEach { offer ->
                            OfferBanner(
                                offer = offer,
                                onSelect = {
                                    // Auto-select the plan matching this offer
                                    selectedPlanId = offer.planId
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(LocalAppDimens.current.pad14))
                }

                // Plan Cards (2x2 Grid)
                val planList = plans.take(4)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LocalAppDimens.current.pad16),
                    verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad12),
                ) {
                    for (rowIdx in 0..1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad12),
                        ) {
                            for (colIdx in 0..1) {
                                val idx = rowIdx * 2 + colIdx
                                if (idx < planList.size) {
                                    PlanCard(
                                        plan = planList[idx],
                                        isSelected = planList[idx].planId == selectedPlan?.planId,
                                        onClick = { selectedPlanId = planList[idx].planId },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(LocalAppDimens.current.pad16))

                // Action Rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LocalAppDimens.current.pad16),
                    verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad8),
                ) {
                    ActionRow(
                        icon = Icons.Outlined.VpnKey,
                        label = "Activate with Premium Code"
                    ) {
                        viewModel.clearError()
                        codeInput = ""
                        showActivateDialog = true
                    }
                    ActionRow(
                        icon = Icons.Outlined.ShoppingCart,
                        label = "Buy Premium Code"
                    ) {
                        try {
                            val whatsappUri = Uri.parse(
                                "https://wa.me/919625459277?text=${
                                    Uri.encode("Hi, I want to buy a Premium Code for Velora.")
                                }"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, whatsappUri))
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    ActionRow(
                        icon = Icons.Outlined.HelpOutline,
                        label = "Help Center"
                    ) {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:velorastream@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Premium Payment Help")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "velorastream@gmail.com", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }

                Spacer(Modifier.height(LocalAppDimens.current.pad16))
            }

            // STICKY BOTTOM PAY BAR
            BottomPayBar(
                selectedPlan = selectedPlan,
                animatedPrice = animatedPrice,
                isLoading = uiState.isCreatingRazorpayOrder,
                onPayClick = {
                    if (selectedPlan != null && !uiState.isCreatingRazorpayOrder) {
                        viewModel.createRazorpayOrder(selectedPlan.planId)
                    }
                },
            )
        }
    }
}

// ── Offer Banner (dynamic from Admin Panel) ──
@Composable
private fun OfferBanner(offer: PremiumOfferDto, onSelect: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(Gold.copy(alpha = 0.6f), GoldDark.copy(alpha = 0.3f))),
                RoundedCornerShape(14.dp),
            )
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2A2518), Color(0xFF1E1C16))
                ),
            )
            .clickable(onClick = onSelect)
            .padding(LocalAppDimens.current.pad14),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        offer.title,
                        fontSize = LocalAppDimens.current.font14,
                        fontWeight = FontWeight.Bold,
                        color = White87,
                    )
                    if (offer.badgeText?.isNotBlank() == true) {
                        Spacer(Modifier.width(LocalAppDimens.current.pad8))
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFFFF6B35), Color(0xFFFF4500))),
                                    RoundedCornerShape(LocalAppDimens.current.radius4),
                                )
                                .padding(horizontal = LocalAppDimens.current.pad6, vertical = LocalAppDimens.current.padTiny),
                        ) {
                            Text(
                                offer.badgeText ?: "",
                                fontSize = LocalAppDimens.current.font8,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
                if (offer.description?.isNotBlank() == true) {
                    Spacer(Modifier.height(LocalAppDimens.current.padTiny))
                    Text(
                        offer.description ?: "",
                        fontSize = LocalAppDimens.current.font11,
                        color = White60,
                        maxLines = 2,
                    )
                }
                Spacer(Modifier.height(LocalAppDimens.current.pad6))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹${offer.originalPrice}",
                        fontSize = LocalAppDimens.current.font13,
                        color = White40,
                        textDecoration = TextDecoration.LineThrough,
                    )
                    Spacer(Modifier.width(LocalAppDimens.current.pad8))
                    Text(
                        "₹${offer.discountPrice}",
                        fontSize = LocalAppDimens.current.font20,
                        fontWeight = FontWeight.ExtraBold,
                        color = Gold,
                    )
                    if (offer.discountPercent > 0) {
                        Spacer(Modifier.width(LocalAppDimens.current.pad8))
                        Box(
                            modifier = Modifier
                                .background(GreenBadge.copy(alpha = 0.2f), RoundedCornerShape(LocalAppDimens.current.radius4))
                                .padding(horizontal = LocalAppDimens.current.pad6, vertical = LocalAppDimens.current.padTiny),
                        ) {
                            Text(
                                "${offer.discountPercent}% OFF",
                                fontSize = LocalAppDimens.current.font10,
                                fontWeight = FontWeight.Bold,
                                color = GreenBadge,
                            )
                        }
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// Parse UPI Response String — handles various formats from different UPI apps
private fun parseUpiResponse(response: String): Map<String, String> {
    if (response.isBlank()) return emptyMap()
    // Some apps return URL-encoded format, some use & separator
    val cleaned = response.trim()
    return cleaned.split("&").mapNotNull { pair ->
        val parts = pair.split("=", limit = 2)
        if (parts.size == 2 && parts[0].isNotBlank()) {
            val key = java.net.URLDecoder.decode(parts[0].trim(), "UTF-8")
            val value = java.net.URLDecoder.decode(parts[1].trim(), "UTF-8")
            key to value
        } else null
    }.toMap()
}

// Plan Card
@Composable
private fun PlanCard(
    plan: PremiumPlanDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Gold else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(200), label = "border",
    )
    val bgBrush = Brush.verticalGradient(
        if (isSelected) listOf(Color(0xFF2A2518), Color(0xFF1E1C16))
        else listOf(CardBg, Color(0xFF1A1A1A))
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .background(bgBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LocalAppDimens.current.pad12),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (plan.badge != null) {
                    Text(
                        plan.badge,
                        fontSize = LocalAppDimens.current.font9,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    if (plan.badge == "Most popular")
                                        listOf(GreenBadge, GreenBadge.copy(alpha = 0.8f))
                                    else
                                        listOf(Gold, GoldDark)
                                ),
                                RoundedCornerShape(5.dp),
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                } else {
                    Spacer(Modifier.height(22.dp))
                }
                Text(
                    "${plan.discountPercent}% Off",
                    fontSize = LocalAppDimens.current.font10,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldSoft,
                    modifier = Modifier
                        .background(GoldFaint.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad10))

            Text(plan.name, fontSize = LocalAppDimens.current.font14, fontWeight = FontWeight.Medium, color = White87)

            Spacer(Modifier.height(LocalAppDimens.current.pad6))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "\u20B9",
                    fontSize = LocalAppDimens.current.font15,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    modifier = Modifier.padding(bottom = LocalAppDimens.current.padTiny),
                )
                Text(
                    "${plan.price}",
                    fontSize = LocalAppDimens.current.font28,
                    fontWeight = FontWeight.ExtraBold,
                    color = Gold,
                    lineHeight = 30.sp,
                )
                Spacer(Modifier.width(LocalAppDimens.current.pad6))
                Text(
                    "\u20B9${plan.originalPrice}",
                    fontSize = LocalAppDimens.current.font12,
                    color = White40,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }

            Text(
                if (plan.months > 1) "\u20B9${plan.price / plan.months}/month" else " ",
                fontSize = LocalAppDimens.current.font11,
                color = GoldSoft.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = LocalAppDimens.current.padTiny),
            )

            Spacer(Modifier.weight(1f))
        }

        if (isSelected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = Gold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(LocalAppDimens.current.pad8)
                    .size(20.dp),
            )
        }
    }
}

// Action Row
@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Gold,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(LocalAppDimens.current.pad14))
        Text(label, fontSize = LocalAppDimens.current.font14, color = White87, modifier = Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = White40,
            modifier = Modifier.size(20.dp),
        )
    }
}

// Bottom Pay Bar (Sticky)
@Composable
private fun BottomPayBar(
    selectedPlan: PremiumPlanDto?,
    animatedPrice: Float,
    isLoading: Boolean = false,
    onPayClick: () -> Unit,
) {
    if (selectedPlan == null) return

    val validDate = java.time.LocalDate.now().plusMonths(selectedPlan.months.toLong())
    val monthAbbr = arrayOf(
        "JAN", "FEB", "MAR", "APR", "MAY", "JUNE",
        "JULY", "AUG", "SEPT", "OCT", "NOV", "DEC",
    )
    val formatted = "${String.format("%02d", validDate.dayOfMonth)} " +
        "${monthAbbr[validDate.monthValue - 1]}, " +
        "${validDate.year}"

    Surface(
        color = BgSecondary,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = LocalAppDimens.current.pad12, bottom = LocalAppDimens.current.pad12)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "\u20B9${animatedPrice.toInt()}",
                        fontSize = LocalAppDimens.current.font24,
                        fontWeight = FontWeight.ExtraBold,
                        color = Gold,
                    )
                    Text(
                        "Selected Plan: ${selectedPlan.name}",
                        fontSize = LocalAppDimens.current.font12,
                        color = White60,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                    Text(
                        "Valid until $formatted",
                        fontSize = LocalAppDimens.current.font11,
                        color = White40,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                Spacer(Modifier.width(LocalAppDimens.current.pad10))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isLoading) Brush.horizontalGradient(
                                listOf(GoldDark.copy(alpha = 0.5f), Gold.copy(alpha = 0.5f))
                            )
                            else Brush.horizontalGradient(listOf(GoldDark, Gold, GoldSoft))
                        )
                        .clickable(enabled = !isLoading, onClick = onPayClick)
                        .padding(horizontal = 28.dp, vertical = LocalAppDimens.current.pad14),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = BgPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(LocalAppDimens.current.pad8))
                            Text(
                                "LOADING...",
                                fontSize = LocalAppDimens.current.font14,
                                fontWeight = FontWeight.Bold,
                                color = BgPrimary
                            )
                        }
                    } else {
                        Text(
                            "PAY NOW",
                            fontSize = LocalAppDimens.current.font15,
                            fontWeight = FontWeight.Bold,
                            color = BgPrimary
                        )
                    }
                }
            }
        }
    }
}