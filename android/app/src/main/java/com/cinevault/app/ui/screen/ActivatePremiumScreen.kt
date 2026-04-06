package com.cinevault.app.ui.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.cinevault.app.R
import com.cinevault.app.data.model.PremiumPlanDto
import com.cinevault.app.ui.viewmodel.PremiumViewModel

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
    PremiumPlanDto("f1", "1m", "1 Month", 1, 1, 182, 10, null, 0, true),
    PremiumPlanDto("f2", "3m", "3 Months", 3, 1, 543, 15, "Most popular", 1, true),
    PremiumPlanDto("f3", "6m", "6 Months", 6, 1, 1038, 20, null, 2, true),
    PremiumPlanDto("f4", "12m", "12 Months", 12, 1, 2042, 25, "Best Value", 3, true),
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
    var pendingOrderId by remember { mutableStateOf<String?>(null) }

    // UPI intent launcher - captures result from UPI app
    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val orderId = pendingOrderId ?: return@rememberLauncherForActivityResult

        // Parse UPI response from intent data
        val response = data?.getStringExtra("response")
            ?: data?.data?.getQueryParameter("response") ?: ""
        val responseMap = parseUpiResponse(response)

        val status = responseMap["Status"] ?: responseMap["status"] ?: "FAILURE"
        val txnId = responseMap["txnId"] ?: responseMap["TxnId"] ?: ""
        val responseCode = responseMap["responseCode"] ?: responseMap["ResponseCode"] ?: ""
        val approvalRefNo = responseMap["ApprovalRefNo"] ?: responseMap["approvalRefNo"] ?: ""

        // Send to backend for verification + auto-activation
        showPaymentProcessing = true
        viewModel.verifyPayment(orderId, status, txnId, responseCode, approvalRefNo)
    }

    LaunchedEffect(plans) {
        if (selectedPlanId == null && plans.isNotEmpty()) selectedPlanId = plans.first().planId
    }

    // When order is created, launch UPI intent
    LaunchedEffect(uiState.currentOrder) {
        val order = uiState.currentOrder ?: return@LaunchedEffect
        pendingOrderId = order.orderId
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(order.upiLink))
            upiLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No UPI app found. Please install Google Pay, PhonePe, or Paytm.",
                Toast.LENGTH_LONG
            ).show()
            viewModel.clearPaymentState()
        }
    }

    // Payment verification result
    LaunchedEffect(uiState.paymentVerified) {
        if (uiState.paymentVerified) {
            showPaymentProcessing = false
            showPaymentResult = true
        }
    }
    LaunchedEffect(uiState.paymentFailed) {
        if (uiState.paymentFailed) {
            showPaymentProcessing = false
            showPaymentResult = true
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

    // Payment Processing Dialog (spinner)
    if (showPaymentProcessing && uiState.isVerifyingPayment) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = CardBg,
            titleContentColor = White87,
            title = {
                Text("Verifying Payment", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(
                        color = Gold, modifier = Modifier.size(40.dp), strokeWidth = 3.dp
                    )
                    Text(
                        "Please wait while we verify your payment and activate premium...",
                        fontSize = 14.sp,
                        color = White60,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            confirmButton = {},
        )
    }

    // Payment Result Dialog
    if (showPaymentResult) {
        val isSuccess = uiState.paymentVerified
        AlertDialog(
            onDismissRequest = {
                showPaymentResult = false
                viewModel.clearPaymentState()
                pendingOrderId = null
            },
            containerColor = CardBg,
            titleContentColor = White87,
            title = {
                Text(
                    if (isSuccess) "Payment Successful!" else "Payment Failed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (isSuccess) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            "Success",
                            tint = GreenBadge,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            uiState.verifyMessage ?: "Premium activated successfully!",
                            fontSize = 14.sp,
                            color = White87,
                            textAlign = TextAlign.Center,
                        )
                        val resp = uiState.verifyResponse
                        if (resp?.daysRemaining != null) {
                            Text(
                                "Plan: ${resp.plan ?: ""} - ${resp.daysRemaining} days",
                                fontSize = 13.sp,
                                color = GoldSoft,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Text(
                            "\u274C",
                            fontSize = 36.sp,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            uiState.verifyMessage
                                ?: "Payment could not be verified. Please try again.",
                            fontSize = 14.sp,
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
                        viewModel.clearPaymentState()
                        pendingOrderId = null
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
                Text("Activate Premium Code", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter your activation code to unlock Premium features.",
                        fontSize = 13.sp,
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
                            fontSize = 12.sp,
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
                        Spacer(Modifier.width(8.dp))
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
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                        .padding(start = 2.dp),
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
                        .padding(horizontal = 18.dp, vertical = 6.dp),
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF888888),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        userName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White87
                    )
                }

                Spacer(Modifier.height(10.dp))

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
                        .padding(vertical = 20.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
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
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    label,
                                    fontSize = 10.sp,
                                    color = White60,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 13.sp,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Plan Cards (2x2 Grid)
                val planList = plans.take(4)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    for (rowIdx in 0..1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
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

                Spacer(Modifier.height(16.dp))

                // Action Rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                "https://wa.me/917015920368?text=${
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

                Spacer(Modifier.height(16.dp))
            }

            // STICKY BOTTOM PAY BAR
            BottomPayBar(
                selectedPlan = selectedPlan,
                animatedPrice = animatedPrice,
                isLoading = uiState.isCreatingOrder,
                onPayClick = {
                    if (selectedPlan != null && !uiState.isCreatingOrder) {
                        val deviceInfo =
                            "${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE}"
                        viewModel.createOrder(selectedPlan.planId, deviceInfo)
                    }
                },
            )
        }
    }
}

// Parse UPI Response String
private fun parseUpiResponse(response: String): Map<String, String> {
    if (response.isBlank()) return emptyMap()
    return response.split("&").associate { pair ->
        val parts = pair.split("=", limit = 2)
        if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
    }
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
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (plan.badge != null) {
                    Text(
                        plan.badge,
                        fontSize = 9.sp,
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldSoft,
                    modifier = Modifier
                        .background(GoldFaint.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(plan.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = White87)

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "\u20B9",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                Text(
                    "${plan.price}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Gold,
                    lineHeight = 30.sp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "\u20B9${plan.originalPrice}",
                    fontSize = 12.sp,
                    color = White40,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }

            Text(
                if (plan.months > 1) "\u20B9${plan.price / plan.months}/month" else " ",
                fontSize = 11.sp,
                color = GoldSoft.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp),
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
                    .padding(8.dp)
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Gold,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 14.sp, color = White87, modifier = Modifier.weight(1f))
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
                .padding(top = 12.dp, bottom = 12.dp)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "\u20B9${animatedPrice.toInt()}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Gold,
                    )
                    Text(
                        "Selected Plan: ${selectedPlan.name}",
                        fontSize = 12.sp,
                        color = White60,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                    Text(
                        "Valid until $formatted",
                        fontSize = 11.sp,
                        color = White40,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
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
                        .padding(horizontal = 28.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = BgPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "LOADING...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BgPrimary
                            )
                        }
                    } else {
                        Text(
                            "PAY NOW",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BgPrimary
                        )
                    }
                }
            }
        }
    }
}