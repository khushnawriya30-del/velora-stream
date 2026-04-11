package com.cinevault.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.ui.viewmodel.EarnMoneyViewModel

// ── Color Palette (matches EarnMoneyScreen) ──
private val DarkBlueBg = Color(0xFF0D1B3E)
private val CardBlue = Color(0xFF1B2B55)
private val CardBlueLighter = Color(0xFF283D6A)
private val GoldYellow = Color(0xFFFFD700)
private val GoldAmber = Color(0xFFFFC107)
private val GreenProgress = Color(0xFF4CAF50)
private val RedOrange = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawEarningScreen(
    initialTab: Int = 0, // 0 = Withdraw, 1 = Earning
    onBack: () -> Unit = {},
    viewModel: EarnMoneyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    // Bank form fields
    var bankName by rememberSaveable { mutableStateOf("") }
    var accountNumber by rememberSaveable { mutableStateOf("") }
    var ifscCode by rememberSaveable { mutableStateOf("") }
    var accountHolderName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var showBankForm by rememberSaveable { mutableStateOf(false) }

    // Rules
    var isHindi by rememberSaveable { mutableStateOf(false) }

    // Auto-fill from last withdrawal
    LaunchedEffect(state.withdrawals) {
        val last = state.withdrawals.firstOrNull()
        if (last != null && bankName.isEmpty()) {
            bankName = last.bankName ?: ""
            accountNumber = last.accountNumber ?: ""
            ifscCode = last.ifscCode ?: ""
            accountHolderName = last.accountHolderName ?: ""
            phoneNumber = last.phoneNumber ?: ""
            email = last.email ?: ""
        }
    }

    val invSettings = state.inviteSettings
    val threshold = invSettings?.targetAmount ?: state.withdrawThreshold

    Scaffold(
        containerColor = DarkBlueBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedTab == 0) "Withdraw" else "Earnings",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ── Tab Row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(CardBlue, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf("Withdraw", "Earning").forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == idx) GoldAmber else Color.Transparent)
                            .clickable { selectedTab = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (selectedTab == idx) Color.Black else Color.White.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Tab Content ──
            when (selectedTab) {
                0 -> WithdrawTab(
                    state = state,
                    threshold = threshold,
                    bankName = bankName,
                    accountNumber = accountNumber,
                    ifscCode = ifscCode,
                    accountHolderName = accountHolderName,
                    phoneNumber = phoneNumber,
                    email = email,
                    showBankForm = showBankForm,
                    isHindi = isHindi,
                    onBankNameChange = { bankName = it },
                    onAccountNumberChange = { accountNumber = it },
                    onIfscCodeChange = { ifscCode = it },
                    onAccountHolderNameChange = { accountHolderName = it },
                    onPhoneNumberChange = { phoneNumber = it },
                    onEmailChange = { email = it },
                    onShowBankForm = { showBankForm = it },
                    onLanguageToggle = { isHindi = !isHindi },
                    onSubmitWithdrawal = {
                        viewModel.requestWithdrawal(
                            amount = threshold,
                            bankName = bankName.trim(),
                            accountNumber = accountNumber.trim(),
                            ifscCode = ifscCode.trim(),
                            accountHolderName = accountHolderName.trim(),
                            phoneNumber = phoneNumber.trim(),
                            email = email.trim(),
                        )
                    },
                    onClearWithdrawState = { viewModel.clearWithdrawState() },
                )
                1 -> EarningTab(state = state, isHindi = isHindi, onLanguageToggle = { isHindi = !isHindi })
            }
        }
    }
}

// ═══════════════════════════════════════════
// WITHDRAW TAB
// ═══════════════════════════════════════════
@Composable
private fun WithdrawTab(
    state: com.cinevault.app.ui.viewmodel.EarnMoneyUiState,
    threshold: Int,
    bankName: String,
    accountNumber: String,
    ifscCode: String,
    accountHolderName: String,
    phoneNumber: String,
    email: String,
    showBankForm: Boolean,
    isHindi: Boolean,
    onBankNameChange: (String) -> Unit,
    onAccountNumberChange: (String) -> Unit,
    onIfscCodeChange: (String) -> Unit,
    onAccountHolderNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onShowBankForm: (Boolean) -> Unit,
    onLanguageToggle: () -> Unit,
    onSubmitWithdrawal: () -> Unit,
    onClearWithdrawState: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // ── Rules Section ──
        RulesSection(isHindi = isHindi, onLanguageToggle = onLanguageToggle)

        Spacer(Modifier.height(16.dp))

        if (showBankForm) {
            // ── Bank Details Form ──
            BankDetailsForm(
                state = state,
                threshold = threshold,
                bankName = bankName,
                accountNumber = accountNumber,
                ifscCode = ifscCode,
                accountHolderName = accountHolderName,
                phoneNumber = phoneNumber,
                email = email,
                onBankNameChange = onBankNameChange,
                onAccountNumberChange = onAccountNumberChange,
                onIfscCodeChange = onIfscCodeChange,
                onAccountHolderNameChange = onAccountHolderNameChange,
                onPhoneNumberChange = onPhoneNumberChange,
                onEmailChange = onEmailChange,
                onSubmit = onSubmitWithdrawal,
                onBack = { onShowBankForm(false); onClearWithdrawState() },
            )
        } else {
            // ── Withdrawal History ──
            if (state.withdrawals.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBlue, RoundedCornerShape(12.dp))
                        .padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No Record",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                        Text(
                            "No withdrawal requests yet",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.3f),
                        )
                    }
                }
            } else {
                Text(
                    "Withdrawal History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.height(8.dp))
                state.withdrawals.forEach { item ->
                    WithdrawalHistoryCard(item)
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Withdraw Button ──
            if (state.canWithdraw) {
                Button(
                    onClick = { onShowBankForm(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAmber),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        "Withdraw ₹$threshold",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(CardBlueLighter, CardBlue)),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Need ₹${state.amountNeeded} more to withdraw",
                        color = Color(0xFFFFAB00),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ═══════════════════════════════════════════
// EARNING TAB
// ═══════════════════════════════════════════
@Composable
private fun EarningTab(
    state: com.cinevault.app.ui.viewmodel.EarnMoneyUiState,
    isHindi: Boolean,
    onLanguageToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // ── Rules Section ──
        RulesSection(isHindi = isHindi, onLanguageToggle = onLanguageToggle)

        Spacer(Modifier.height(16.dp))

        if (state.earnings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBlue, RoundedCornerShape(12.dp))
                    .padding(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No Earnings Yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
        } else {
            Text(
                "Earnings History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            state.earnings.forEach { item ->
                EarningHistoryCard(item)
                Spacer(Modifier.height(8.dp))
            }
            // "No More" at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "— No More —",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.3f),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ═══════════════════════════════════════════
// RULES SECTION — Expandable Cards with
// Hindi/English Toggle
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
private fun RulesSection(isHindi: Boolean, onLanguageToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBlue, RoundedCornerShape(12.dp))
            .border(1.dp, GoldYellow.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        // Header with language toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "📋 Rules / नियम",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            // Language toggle button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHindi) Color(0xFF3A6BA5) else Color(0xFF2E5090))
                    .clickable { onLanguageToggle() }
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

        // Expandable rule cards
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
    var expanded by rememberSaveable { mutableStateOf(false) }
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
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(GoldYellow),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$index",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
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
                    .rotate(rotationAngle),
            )
        }

        // Expandable content
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)),
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

// ═══════════════════════════════════════════
// BANK DETAILS FORM
// ═══════════════════════════════════════════
@Composable
private fun BankDetailsForm(
    state: com.cinevault.app.ui.viewmodel.EarnMoneyUiState,
    threshold: Int,
    bankName: String,
    accountNumber: String,
    ifscCode: String,
    accountHolderName: String,
    phoneNumber: String,
    email: String,
    onBankNameChange: (String) -> Unit,
    onAccountNumberChange: (String) -> Unit,
    onIfscCodeChange: (String) -> Unit,
    onAccountHolderNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    val isFormValid = bankName.isNotBlank() && accountNumber.isNotBlank() &&
            ifscCode.isNotBlank() && accountHolderName.isNotBlank() &&
            phoneNumber.length >= 10 && email.contains("@")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBlue, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Withdrawal - ₹$threshold",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(16.dp))

        if (state.withdrawSuccess) {
            // Success state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenProgress.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GreenProgress,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Withdrawal Request Submitted!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenProgress,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your request will be processed in 2-5 business days.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAmber),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Bank detail fields
            BankTextField("Bank Name", bankName, onBankNameChange, Icons.Default.AccountBalance)
            Spacer(Modifier.height(10.dp))
            BankTextField("Account Number", accountNumber, onAccountNumberChange, Icons.Default.Numbers, KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            BankTextField("IFSC Code", ifscCode, onIfscCodeChange, Icons.Default.Code)
            Spacer(Modifier.height(10.dp))
            BankTextField("Account Holder Name", accountHolderName, onAccountHolderNameChange, Icons.Default.Person)
            Spacer(Modifier.height(10.dp))
            BankTextField("Phone Number", phoneNumber, onPhoneNumberChange, Icons.Default.Phone, KeyboardType.Phone)
            Spacer(Modifier.height(10.dp))
            BankTextField("Email", email, onEmailChange, Icons.Default.Email, KeyboardType.Email)

            if (state.withdrawError != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    state.withdrawError!!,
                    color = RedOrange,
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onSubmit,
                enabled = isFormValid && !state.isWithdrawing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAmber,
                    disabledContainerColor = GoldAmber.copy(alpha = 0.4f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (state.isWithdrawing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        "Submit Withdrawal Request",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun BankTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = GoldYellow.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
}

// ═══════════════════════════════════════════
// HISTORY CARDS
// ═══════════════════════════════════════════
@Composable
private fun WithdrawalHistoryCard(item: com.cinevault.app.data.model.WithdrawalHistoryItem) {
    val statusColor = when (item.status) {
        "approved" -> GreenProgress
        "rejected" -> RedOrange
        else -> Color(0xFFFFAB00)
    }
    val statusIcon = when (item.status) {
        "approved" -> Icons.Default.CheckCircle
        "rejected" -> Icons.Default.Cancel
        else -> Icons.Default.HourglassTop
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBlueLighter, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "₹${item.amount}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                if (!item.bankName.isNullOrBlank()) {
                    Text(
                        "${item.bankName} - ${item.accountNumber?.takeLast(4) ?: ""}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
                if (item.status == "rejected" && !item.rejectionReason.isNullOrBlank()) {
                    Text(
                        item.rejectionReason!!,
                        fontSize = 11.sp,
                        color = RedOrange.copy(alpha = 0.8f),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    item.status.replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                )
                Text(
                    formatDate(item.createdAt),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
private fun EarningHistoryCard(item: com.cinevault.app.data.model.EarningItem) {
    val isGift = item.type == "app_gift"
    val icon = if (isGift) Icons.Default.CardGiftcard else Icons.Default.PersonAdd
    val iconColor = if (isGift) GoldYellow else GreenProgress

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBlueLighter, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
                Text(
                    formatDate(item.createdAt),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.4f),
                )
            }
            Text(
                "+₹${item.amount}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GreenProgress,
            )
        }
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val date = sdf.parse(dateStr.take(19))
        val outFmt = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        date?.let { outFmt.format(it) } ?: dateStr.take(10)
    } catch (_: Exception) {
        dateStr.take(10)
    }
}
