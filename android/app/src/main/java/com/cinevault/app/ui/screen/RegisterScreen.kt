package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.ui.components.GoldButton
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.registerSuccess) {
        if (uiState.registerSuccess) {
            onRegisterSuccess()
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(60.dp))

            Text(
                "CINEVAULT",
                style = CineVaultTheme.typography.heroTitle.copy(
                    fontSize = 32.sp,
                    letterSpacing = 6.sp,
                ),
                color = CineVaultTheme.colors.accentGold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Create your account",
                style = CineVaultTheme.typography.body,
                color = CineVaultTheme.colors.textSecondary,
            )

            Spacer(Modifier.height(36.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CineVaultTheme.colors.surface.copy(alpha = 0.6f),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    CineVaultTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Full Name",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                    )

                    Spacer(Modifier.height(14.dp))

                    CineVaultTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                    )

                    Spacer(Modifier.height(14.dp))

                    CineVaultTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password",
                                    tint = CineVaultTheme.colors.textSecondary,
                                )
                            }
                        },
                    )

                    Spacer(Modifier.height(14.dp))

                    CineVaultTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm Password",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.register(name, email, password, confirmPassword)
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
                        text = "Create Account",
                        onClick = { viewModel.register(name, email, password, confirmPassword) },
                        isLoading = uiState.isLoading,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Already have an account?",
                    style = CineVaultTheme.typography.body,
                    color = CineVaultTheme.colors.textSecondary,
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        "Sign In",
                        style = CineVaultTheme.typography.body,
                        color = CineVaultTheme.colors.accentGold,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
