package com.cinevault.app.ui.screen

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
    initialTab: Int = 0, // 0 = Bank Details, 1 = Withdraw History, 2 = Earning History
    onBack: () -> Unit = {},
    viewModel: EarnMoneyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    // Bank form fields — pre-fill from saved bank details
    val saved = state.savedBankDetails
    var bankName by rememberSaveable { mutableStateOf("") }
    var accountNumber by rememberSaveable { mutableStateOf("") }
    var ifscCode by rememberSaveable { mutableStateOf("") }
    var accountHolderName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var isEditing by rememberSaveable { mutableStateOf(false) }

    // Pre-fill form from saved bank details (once)
    LaunchedEffect(saved) {
        if (saved != null && saved.hasBankDetails) {
            bankName = saved.bankName
            accountNumber = saved.accountNumber
            ifscCode = saved.ifscCode
            accountHolderName = saved.accountHolderName
            phoneNumber = saved.phoneNumber
            email = saved.email
        }
    }

    // After bank save success, exit editing mode
    LaunchedEffect(state.bankSaveSuccess) {
        if (state.bankSaveSuccess) {
            isEditing = false
        }
    }

    val invSettings = state.inviteSettings
    val threshold = invSettings?.targetAmount ?: state.withdrawThreshold
    val hasBankDetails = saved?.hasBankDetails == true

    val tabTitles = listOf("Bank Details", "Withdrawals", "Earnings")

    Scaffold(
        containerColor = DarkBlueBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        tabTitles.getOrElse(selectedTab) { "Withdraw" },
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
                tabTitles.forEachIndexed { idx, label ->
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
                            fontSize = 13.sp,
                            color = if (selectedTab == idx) Color.Black else Color.White.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Tab Content ──
            when (selectedTab) {
                0 -> BankDetailsTab(
                    state = state,
                    threshold = threshold,
                    hasBankDetails = hasBankDetails,
                    isEditing = isEditing,
                    bankName = bankName,
                    accountNumber = accountNumber,
                    ifscCode = ifscCode,
                    accountHolderName = accountHolderName,
                    phoneNumber = phoneNumber,
                    email = email,
                    onBankNameChange = { bankName = it },
                    onAccountNumberChange = { accountNumber = it },
                    onIfscCodeChange = { ifscCode = it },
                    onAccountHolderNameChange = { accountHolderName = it },
                    onPhoneNumberChange = { phoneNumber = it },
                    onEmailChange = { email = it },
                    onEditClick = { isEditing = true },
                    onSaveBankDetails = {
                        viewModel.saveBankDetails(
                            bankName = bankName.trim(),
                            accountNumber = accountNumber.trim(),
                            ifscCode = ifscCode.trim(),
                            accountHolderName = accountHolderName.trim(),
                            phoneNumber = phoneNumber.trim(),
                            email = email.trim(),
                        )
                    },
                    onWithdraw = {
                        viewModel.requestWithdrawal(amount = threshold)
                    },
                    onClearWithdrawState = { viewModel.clearWithdrawState() },
                )
                1 -> WithdrawHistoryTab(state = state)
                2 -> EarningTab(state = state)
            }
        }
    }
}

// ═══════════════════════════════════════════
// BANK DETAILS TAB (save/view + withdraw)
// ═══════════════════════════════════════════
@Composable
private fun BankDetailsTab(
    state: com.cinevault.app.ui.viewmodel.EarnMoneyUiState,
    threshold: Int,
    hasBankDetails: Boolean,
    isEditing: Boolean,
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
    onEditClick: () -> Unit,
    onSaveBankDetails: () -> Unit,
    onWithdraw: () -> Unit,
    onClearWithdrawState: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        if (hasBankDetails && !isEditing) {
            // ── Saved Bank Details (Read-Only) ──
            SavedBankDetailsCard(
                bankDetails = state.savedBankDetails!!,
                onEditClick = onEditClick,
            )

            Spacer(Modifier.height(16.dp))

            // ── Withdraw Section ──
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
                            onClick = onClearWithdrawState,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAmber),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (state.canWithdraw) {
                Button(
                    onClick = onWithdraw,
                    enabled = !state.isWithdrawing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAmber,
                        disabledContainerColor = GoldAmber.copy(alpha = 0.5f),
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
                            "Withdraw ₹$threshold",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }
                if (state.withdrawError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.withdrawError!!, color = RedOrange, fontSize = 13.sp)
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
        } else {
            // ── Bank Details Form (first time or editing) ──
            BankDetailsForm(
                bankName = bankName,
                accountNumber = accountNumber,
                ifscCode = ifscCode,
                accountHolderName = accountHolderName,
                phoneNumber = phoneNumber,
                email = email,
                isSaving = state.isSavingBank,
                saveError = state.bankSaveError,
                onBankNameChange = onBankNameChange,
                onAccountNumberChange = onAccountNumberChange,
                onIfscCodeChange = onIfscCodeChange,
                onAccountHolderNameChange = onAccountHolderNameChange,
                onPhoneNumberChange = onPhoneNumberChange,
                onEmailChange = onEmailChange,
                onSave = onSaveBankDetails,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ═══════════════════════════════════════════
// WITHDRAW HISTORY TAB
// ═══════════════════════════════════════════
@Composable
private fun WithdrawHistoryTab(
    state: com.cinevault.app.ui.viewmodel.EarnMoneyUiState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        if (state.withdrawals.isEmpty()) {
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
                        "No withdrawal record yet",
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
// SAVED BANK DETAILS (Read-Only Card)
// ═══════════════════════════════════════════
@Composable
private fun SavedBankDetailsCard(
    bankDetails: com.cinevault.app.data.model.BankDetailsResponse,
    onEditClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBlue, RoundedCornerShape(12.dp))
            .border(1.dp, GoldYellow.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = GoldYellow,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Saved Bank Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, "Edit", tint = GoldYellow, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        BankDetailRow("Bank Name", bankDetails.bankName)
        BankDetailRow("Account Number", bankDetails.accountNumber)
        BankDetailRow("IFSC Code", bankDetails.ifscCode)
        BankDetailRow("Account Holder", bankDetails.accountHolderName)
        BankDetailRow("Phone", bankDetails.phoneNumber)
        BankDetailRow("Email", bankDetails.email)
    }
}

@Composable
private fun BankDetailRow(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.5f),
            )
            Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        }
    }
}

// ═══════════════════════════════════════════
// EARNING TAB
// ═══════════════════════════════════════════
@Composable
private fun EarningTab(
    state: com.cinevault.app.ui.viewmodel.EarnMoneyUiState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
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
// BANK DETAILS FORM (Save once)
// ═══════════════════════════════════════════
@Composable
private fun BankDetailsForm(
    bankName: String,
    accountNumber: String,
    ifscCode: String,
    accountHolderName: String,
    phoneNumber: String,
    email: String,
    isSaving: Boolean,
    saveError: String?,
    onBankNameChange: (String) -> Unit,
    onAccountNumberChange: (String) -> Unit,
    onIfscCodeChange: (String) -> Unit,
    onAccountHolderNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSave: () -> Unit,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AccountBalance,
                contentDescription = null,
                tint = GoldYellow,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Enter Bank Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Save your bank details once for all future withdrawals",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
        )

        Spacer(Modifier.height(16.dp))

        BankTextField("Bank Name", bankName, onBankNameChange, Icons.Default.AccountBalance, placeholder = "e.g. - State Bank of India")
        Spacer(Modifier.height(10.dp))
        BankTextField("Account No.", accountNumber, onAccountNumberChange, Icons.Default.Numbers, KeyboardType.Number, placeholder = "e.g. - 123456789012")
        Spacer(Modifier.height(10.dp))
        BankTextField("IFSC Code", ifscCode, onIfscCodeChange, Icons.Default.Code, placeholder = "e.g. - SBIN0000123")
        Spacer(Modifier.height(10.dp))
        BankTextField("Account Holder Name", accountHolderName, onAccountHolderNameChange, Icons.Default.Person, placeholder = "e.g. - Rajesh Kumar")
        Spacer(Modifier.height(10.dp))
        BankTextField("Phone Number", phoneNumber, onPhoneNumberChange, Icons.Default.Phone, KeyboardType.Phone, placeholder = "IN +91 | 00000 00000")
        Spacer(Modifier.height(10.dp))
        BankTextField("Email", email, onEmailChange, Icons.Default.Email, KeyboardType.Email, placeholder = "Required")

        if (saveError != null) {
            Spacer(Modifier.height(10.dp))
            Text(saveError, color = RedOrange, fontSize = 13.sp)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onSave,
            enabled = isFormValid && !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldAmber,
                disabledContainerColor = GoldAmber.copy(alpha = 0.4f),
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    "Save Details",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
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
    placeholder: String = "",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) {{ Text(placeholder, color = Color.White.copy(alpha = 0.3f)) }} else null,
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
