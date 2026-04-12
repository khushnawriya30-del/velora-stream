package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.ui.components.GoldButton
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.SettingsViewModel
import com.cinevault.app.ui.theme.LocalAppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(uiState.passwordResetSuccess) {
        if (uiState.passwordResetSuccess) {
            kotlinx.coroutines.delay(1500)
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CineVaultTheme.colors.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineVaultTheme.colors.background),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalAppDimens.current.pad24),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(40.dp))

                when {
                    uiState.passwordResetSuccess -> {
                        Text("Password Reset!", style = CineVaultTheme.typography.displayLarge, color = CineVaultTheme.colors.textPrimary)
                        Spacer(Modifier.height(LocalAppDimens.current.pad12))
                        Text("Your password has been reset successfully. Redirecting to sign in...", style = CineVaultTheme.typography.body, color = CineVaultTheme.colors.textSecondary, textAlign = TextAlign.Center)
                    }

                    uiState.otpVerified -> {
                        // Step 3: New Password
                        Text("New Password", style = CineVaultTheme.typography.displayLarge, color = CineVaultTheme.colors.textPrimary)
                        Spacer(Modifier.height(LocalAppDimens.current.pad8))
                        Text("Enter your new password.", style = CineVaultTheme.typography.body, color = CineVaultTheme.colors.textSecondary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(LocalAppDimens.current.pad32))
                        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(LocalAppDimens.current.radius16), color = CineVaultTheme.colors.surface.copy(alpha = 0.6f)) {
                            Column(modifier = Modifier.padding(LocalAppDimens.current.pad24)) {
                                CineVaultTextField(value = newPassword, onValueChange = { newPassword = it }, label = "New Password", visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next))
                                Spacer(Modifier.height(LocalAppDimens.current.pad12))
                                CineVaultTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password", visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); uiState.resetToken?.let { viewModel.resetPasswordWithToken(it, newPassword, confirmPassword) } }))
                                Spacer(Modifier.height(LocalAppDimens.current.pad16))
                                uiState.error?.let { Text(it, style = CineVaultTheme.typography.label, color = CineVaultTheme.colors.error, modifier = Modifier.padding(bottom = LocalAppDimens.current.pad8)) }
                                GoldButton(text = "Reset Password", onClick = { uiState.resetToken?.let { viewModel.resetPasswordWithToken(it, newPassword, confirmPassword) } }, isLoading = uiState.isLoading)
                            }
                        }
                    }

                    uiState.otpSent -> {
                        // Step 2: OTP Input
                        Text("Enter OTP", style = CineVaultTheme.typography.displayLarge, color = CineVaultTheme.colors.textPrimary)
                        Spacer(Modifier.height(LocalAppDimens.current.pad8))
                        Text("We've sent a 6-digit code to $email", style = CineVaultTheme.typography.body, color = CineVaultTheme.colors.textSecondary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(LocalAppDimens.current.pad32))
                        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(LocalAppDimens.current.radius16), color = CineVaultTheme.colors.surface.copy(alpha = 0.6f)) {
                            Column(modifier = Modifier.padding(LocalAppDimens.current.pad24)) {
                                CineVaultTextField(
                                    value = otp,
                                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otp = it },
                                    label = "6-Digit OTP",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.verifyOtp(email, otp) }),
                                )
                                Spacer(Modifier.height(LocalAppDimens.current.pad16))
                                uiState.error?.let { Text(it, style = CineVaultTheme.typography.label, color = CineVaultTheme.colors.error, modifier = Modifier.padding(bottom = LocalAppDimens.current.pad8)) }
                                GoldButton(text = "Verify OTP", onClick = { viewModel.verifyOtp(email, otp) }, isLoading = uiState.isLoading)
                                Spacer(Modifier.height(LocalAppDimens.current.pad12))
                                TextButton(onClick = { viewModel.sendOtp(email) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                    Text("Resend OTP", color = CineVaultTheme.colors.accentGold, fontSize = LocalAppDimens.current.font14)
                                }
                            }
                        }
                    }

                    else -> {
                        // Step 1: Email input
                        Text("Reset Password", style = CineVaultTheme.typography.displayLarge, color = CineVaultTheme.colors.textPrimary)
                        Spacer(Modifier.height(LocalAppDimens.current.pad8))
                        Text("Enter your email address and we'll send you a verification code.", style = CineVaultTheme.typography.body, color = CineVaultTheme.colors.textSecondary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(LocalAppDimens.current.pad32))
                        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(LocalAppDimens.current.radius16), color = CineVaultTheme.colors.surface.copy(alpha = 0.6f)) {
                            Column(modifier = Modifier.padding(LocalAppDimens.current.pad24)) {
                                CineVaultTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = "Email Address",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.sendOtp(email) }),
                                )
                                Spacer(Modifier.height(LocalAppDimens.current.pad20))
                                uiState.error?.let { Text(it, style = CineVaultTheme.typography.label, color = CineVaultTheme.colors.error, modifier = Modifier.padding(bottom = LocalAppDimens.current.pad8)) }
                                GoldButton(text = "Send Verification Code", onClick = { viewModel.sendOtp(email) }, isLoading = uiState.isLoading)
                            }
                        }
                    }
                }
            }
        }
    }
}
