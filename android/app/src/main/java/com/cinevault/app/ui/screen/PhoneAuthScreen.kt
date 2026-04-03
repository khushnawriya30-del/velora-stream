package com.cinevault.app.ui.screen

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    onSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as Activity
    val firebaseAuth = remember { FirebaseAuth.getInstance() }

    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var otpSent by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Navigate on success
    LaunchedEffect(uiState.phoneLoginSuccess) {
        if (uiState.phoneLoginSuccess) {
            onSuccess()
            viewModel.resetState()
        }
    }

    // Propagate ViewModel errors
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            error = uiState.error
            isLoading = false
        }
    }

    fun sendOtp(forceResend: Boolean = false) {
        val fullPhone = "+91$phoneNumber"
        isLoading = true
        error = null

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval or instant verification — sign in immediately
                isLoading = true
                firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        result.user?.getIdToken(false)?.addOnSuccessListener { tokenResult ->
                            val idToken = tokenResult.token ?: return@addOnSuccessListener
                            viewModel.firebasePhoneVerify(idToken)
                        }
                    }
                    .addOnFailureListener { e ->
                        error = e.localizedMessage ?: "Auto-verification failed"
                        isLoading = false
                    }
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                error = e.localizedMessage ?: "Verification failed"
                isLoading = false
            }

            override fun onCodeSent(vId: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = vId
                resendToken = token
                otpSent = true
                isLoading = false
                // Start 60-second countdown
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(fullPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (forceResend && resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken!!)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
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
                .padding(12.dp)
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
                .padding(horizontal = 24.dp),
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

            Spacer(Modifier.height(20.dp))

            Text(
                if (!otpSent) "Enter your mobile number" else "Verify your number",
                style = CineVaultTheme.typography.title,
                color = CineVaultTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (!otpSent)
                    AnnotatedString("We'll send a 6-digit OTP via SMS to your mobile number")
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
                shape = RoundedCornerShape(16.dp),
                color = CineVaultTheme.colors.surface.copy(alpha = 0.6f),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    if (!otpSent) {
                        // ── STEP 1: Phone Number Entry ──────────────────
                        Text(
                            "Mobile Number",
                            style = CineVaultTheme.typography.label,
                            color = CineVaultTheme.colors.textSecondary,
                        )
                        Spacer(Modifier.height(8.dp))

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
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${phoneNumber.length}/10 digits",
                                style = CineVaultTheme.typography.label.copy(fontSize = 11.sp),
                                color = CineVaultTheme.colors.textSecondary,
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        if (error != null) {
                            Text(
                                error!!,
                                style = CineVaultTheme.typography.label,
                                color = CineVaultTheme.colors.error,
                                modifier = Modifier.padding(bottom = 8.dp),
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
                        Spacer(Modifier.height(8.dp))

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
                                        val credential = PhoneAuthProvider.getCredential(verificationId!!, otpValue)
                                        isLoading = true
                                        error = null
                                        firebaseAuth.signInWithCredential(credential)
                                            .addOnSuccessListener { result ->
                                                result.user?.getIdToken(false)?.addOnSuccessListener { tokenResult ->
                                                    val idToken = tokenResult.token ?: return@addOnSuccessListener
                                                    viewModel.firebasePhoneVerify(idToken)
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                error = e.localizedMessage ?: "Invalid OTP"
                                                isLoading = false
                                            }
                                    }
                                },
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                letterSpacing = 8.sp,
                                textAlign = TextAlign.Center,
                                fontSize = 22.sp,
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

                        Spacer(Modifier.height(12.dp))

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
                                        otpSent = false
                                        sendOtp(forceResend = true)
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

                        Spacer(Modifier.height(8.dp))

                        if (error != null) {
                            Text(
                                error!!,
                                style = CineVaultTheme.typography.label,
                                color = CineVaultTheme.colors.error,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }

                        GoldButton(
                            text = "Verify & Continue",
                            onClick = {
                                focusManager.clearFocus()
                                val vId = verificationId ?: return@GoldButton
                                val credential = PhoneAuthProvider.getCredential(vId, otpValue)
                                isLoading = true
                                error = null
                                firebaseAuth.signInWithCredential(credential)
                                    .addOnSuccessListener { result ->
                                        result.user?.getIdToken(false)?.addOnSuccessListener { tokenResult ->
                                            val idToken = tokenResult.token ?: return@addOnSuccessListener
                                            viewModel.firebasePhoneVerify(idToken)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        error = e.localizedMessage ?: "Invalid OTP"
                                        isLoading = false
                                    }
                            },
                            isLoading = isLoading || uiState.isLoading,
                            enabled = otpValue.length == 6,
                        )

                        Spacer(Modifier.height(8.dp))

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

            Spacer(Modifier.height(32.dp))

            Text(
                "Only Indian mobile numbers (+91) supported.\nOTP sent via SMS by Firebase.",
                style = CineVaultTheme.typography.label.copy(fontSize = 12.sp),
                color = CineVaultTheme.colors.textSecondary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

