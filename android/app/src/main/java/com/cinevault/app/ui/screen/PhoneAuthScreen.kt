package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.ui.components.GoldButton
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import com.cinevault.app.ui.theme.LocalAppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    onSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var otpSent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Navigate on success
    LaunchedEffect(uiState.phoneLoginSuccess) {
        if (uiState.phoneLoginSuccess) {
            onSuccess()
            viewModel.resetState()
        }
    }

    // Backend OTP sent successfully
    LaunchedEffect(uiState.phoneOtpSent) {
        if (uiState.phoneOtpSent) {
            otpSent = true
            isLoading = false
            viewModel.resetPhoneOtpSent()
        }
    }

    // Propagate ViewModel errors
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            error = uiState.error
            isLoading = false
        }
    }

    fun sendOtp() {
        isLoading = true
        error = null
        viewModel.sendPhoneOtp(phoneNumber)
    }

    // Countdown timer
    LaunchedEffect(otpSent) {
        if (otpSent) {
            countdownSeconds = 60
            while (countdownSeconds > 0) {
                delay(1000L)
                countdownSeconds--
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
    ) {
        // Back button
        IconButton(
            onClick = {
                viewModel.resetState()
                onNavigateBack()
            },
            modifier = Modifier
                .padding(LocalAppDimens.current.pad12)
                .align(Alignment.TopStart),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = CineVaultTheme.colors.textPrimary,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LocalAppDimens.current.pad24),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(80.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        CineVaultTheme.colors.accentGold.copy(alpha = 0.15f),
                        RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Phone,
                    contentDescription = null,
                    tint = CineVaultTheme.colors.accentGold,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad20))

            Text(
                if (!otpSent) "Enter your mobile number" else "Verify your number",
                style = CineVaultTheme.typography.title,
                color = CineVaultTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LocalAppDimens.current.pad8))
            Text(
                if (!otpSent)
                    buildAnnotatedString { append("We'll send a 6-digit OTP via SMS to your mobile number") }
                else
                    buildAnnotatedString {
                        append("OTP sent to +91 ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = CineVaultTheme.colors.accentGold)) {
                            append(phoneNumber)
                        }
                    },
                style = CineVaultTheme.typography.body,
                color = CineVaultTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(36.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LocalAppDimens.current.radius16),
                color = CineVaultTheme.colors.surface.copy(alpha = 0.6f),
            ) {
                Column(modifier = Modifier.padding(LocalAppDimens.current.pad24)) {

                    if (!otpSent) {
                        // ── STEP 1: Phone Number Entry ──────────────────
                        Text(
                            "Mobile Number",
                            style = CineVaultTheme.typography.label,
                            color = CineVaultTheme.colors.textSecondary,
                        )
                        Spacer(Modifier.height(LocalAppDimens.current.pad8))

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { value ->
                                val digits = value.filter { it.isDigit() }.take(10)
                                phoneNumber = digits
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            prefix = {
                                Text(
                                    "+91  ",
                                    style = CineVaultTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                                    color = CineVaultTheme.colors.accentGold,
                                )
                            },
                            placeholder = {
                                Text(
                                    "98765 43210",
                                    color = CineVaultTheme.colors.textSecondary.copy(alpha = 0.5f),
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (phoneNumber.length == 10) sendOtp()
                                },
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CineVaultTheme.colors.accentGold,
                                unfocusedBorderColor = CineVaultTheme.colors.border,
                                focusedTextColor = CineVaultTheme.colors.textPrimary,
                                unfocusedTextColor = CineVaultTheme.colors.textPrimary,
                                cursorColor = CineVaultTheme.colors.accentGold,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        )

                        if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
                            Spacer(Modifier.height(LocalAppDimens.current.pad4))
                            Text(
                                "${phoneNumber.length}/10 digits",
                                style = CineVaultTheme.typography.label.copy(fontSize = LocalAppDimens.current.font11),
                                color = CineVaultTheme.colors.textSecondary,
                            )
                        }

                        Spacer(Modifier.height(LocalAppDimens.current.pad20))

                        if (error != null) {
                            Text(
                                error!!,
                                style = CineVaultTheme.typography.label,
                                color = CineVaultTheme.colors.error,
                                modifier = Modifier.padding(bottom = LocalAppDimens.current.pad8),
                            )
                        }

                        GoldButton(
                            text = "Send OTP",
                            onClick = {
                                focusManager.clearFocus()
                                sendOtp()
                            },
                            isLoading = isLoading,
                            enabled = phoneNumber.length == 10 &&
                                phoneNumber[0].toString().matches(Regex("[6-9]")),
                        )

                    } else {
                        // ── STEP 2: OTP Entry ───────────────────────────
                        Text(
                            "Enter 6-digit OTP",
                            style = CineVaultTheme.typography.label,
                            color = CineVaultTheme.colors.textSecondary,
                        )
                        Spacer(Modifier.height(LocalAppDimens.current.pad8))

                        OutlinedTextField(
                            value = otpValue,
                            onValueChange = { value ->
                                val digits = value.filter { it.isDigit() }.take(6)
                                otpValue = digits
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    "• • • • • •",
                                    color = CineVaultTheme.colors.textSecondary.copy(alpha = 0.4f),
                                    letterSpacing = 6.sp,
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (otpValue.length == 6) {
                                        isLoading = true
                                        error = null
                                        viewModel.verifyPhoneOtp(phoneNumber, otpValue)
                                    }
                                },
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                letterSpacing = 8.sp,
                                textAlign = TextAlign.Center,
                                fontSize = LocalAppDimens.current.font22,
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CineVaultTheme.colors.accentGold,
                                unfocusedBorderColor = CineVaultTheme.colors.border,
                                focusedTextColor = CineVaultTheme.colors.textPrimary,
                                unfocusedTextColor = CineVaultTheme.colors.textPrimary,
                                cursorColor = CineVaultTheme.colors.accentGold,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        )

                        Spacer(Modifier.height(LocalAppDimens.current.pad12))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (countdownSeconds > 0) {
                                Text(
                                    "Resend OTP in ",
                                    style = CineVaultTheme.typography.label,
                                    color = CineVaultTheme.colors.textSecondary,
                                )
                                Text(
                                    "${countdownSeconds}s",
                                    style = CineVaultTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                                    color = CineVaultTheme.colors.accentGold,
                                )
                            } else {
                                TextButton(
                                    onClick = {
                                        otpValue = ""
                                        error = null
                                        sendOtp()
                                    },
                                    enabled = !isLoading,
                                ) {
                                    Text(
                                        "Resend OTP",
                                        style = CineVaultTheme.typography.label,
                                        color = CineVaultTheme.colors.accentGold,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(LocalAppDimens.current.pad8))

                        if (error != null) {
                            Text(
                                error!!,
                                style = CineVaultTheme.typography.label,
                                color = CineVaultTheme.colors.error,
                                modifier = Modifier.padding(bottom = LocalAppDimens.current.pad8),
                            )
                        }

                        GoldButton(
                            text = "Verify & Continue",
                            onClick = {
                                focusManager.clearFocus()
                                isLoading = true
                                error = null
                                viewModel.verifyPhoneOtp(phoneNumber, otpValue)
                            },
                            isLoading = isLoading || uiState.isLoading,
                            enabled = otpValue.length == 6,
                        )

                        Spacer(Modifier.height(LocalAppDimens.current.pad8))

                        TextButton(
                            onClick = {
                                otpValue = ""
                                otpSent = false
                                error = null
                                viewModel.clearError()
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(
                                "Change mobile number",
                                style = CineVaultTheme.typography.label,
                                color = CineVaultTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad32))

            Text(
                "Only Indian mobile numbers (+91) supported.\nOTP sent via SMS.",
                style = CineVaultTheme.typography.label.copy(fontSize = LocalAppDimens.current.font12),
                color = CineVaultTheme.colors.textSecondary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

