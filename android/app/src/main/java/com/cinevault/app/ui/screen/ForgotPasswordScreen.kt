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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.ui.components.GoldButton
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }

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
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(40.dp))

                if (uiState.forgotPasswordSuccess) {
                    // Success state
                    Text(
                        "Check Your Email",
                        style = CineVaultTheme.typography.displayLarge,
                        color = CineVaultTheme.colors.textPrimary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "We've sent a password reset link to your email address. Please check your inbox.",
                        style = CineVaultTheme.typography.body,
                        color = CineVaultTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(32.dp))
                    GoldButton(text = "Back to Sign In", onClick = onBack)
                } else {
                    Text(
                        "Reset Password",
                        style = CineVaultTheme.typography.displayLarge,
                        color = CineVaultTheme.colors.textPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Enter your email address and we'll send you a link to reset your password.",
                        style = CineVaultTheme.typography.body,
                        color = CineVaultTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(32.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CineVaultTheme.colors.surface.copy(alpha = 0.6f),
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            CineVaultTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = "Email Address",
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        viewModel.forgotPassword(email)
                                    }
                                ),
                            )

                            Spacer(Modifier.height(20.dp))

                            if (uiState.error != null) {
                                Text(
                                    uiState.error!!,
                                    style = CineVaultTheme.typography.label,
                                    color = CineVaultTheme.colors.error,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }

                            GoldButton(
                                text = "Send Reset Link",
                                onClick = { viewModel.forgotPassword(email) },
                                isLoading = uiState.isLoading,
                            )
                        }
                    }
                }
            }
        }
    }
}
