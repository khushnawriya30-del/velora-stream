package com.cinevault.app.ui.screen

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.PremiumViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivatePremiumScreen(
    onBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }

    LaunchedEffect(uiState.activationSuccess) {
        if (uiState.activationSuccess) {
            Toast.makeText(context, "Premium Activated! 🎉", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = CineVaultTheme.colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Premium", color = CineVaultTheme.colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CineVaultTheme.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineVaultTheme.colors.background),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // Crown icon
            Text("👑", fontSize = 56.sp)

            Spacer(Modifier.height(16.dp))

            Text(
                if (uiState.isPremium) "You're Premium!" else "Upgrade to Premium",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD4AF37),
            )

            Spacer(Modifier.height(8.dp))

            if (uiState.isPremium) {
                // Show premium status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Verified, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Active Premium", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        uiState.plan?.let {
                            Text("Plan: ${it.replace("_", " ").replaceFirstChar { c -> c.uppercase() }}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        }
                        uiState.daysRemaining?.let { days ->
                            Text("$days days remaining", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Features list
                PremiumFeatureItem("1080p+ HD Quality", Icons.Filled.HighQuality)
                PremiumFeatureItem("Ad-Free Streaming", Icons.Filled.Block)
                PremiumFeatureItem("Exclusive Content", Icons.Filled.Star)
            } else {
                Text(
                    "Enter your activation code to unlock\npremium features",
                    color = CineVaultTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(32.dp))

                // Features preview
                PremiumFeatureItem("1080p+ HD Quality", Icons.Filled.HighQuality)
                PremiumFeatureItem("Ad-Free Streaming", Icons.Filled.Block)
                PremiumFeatureItem("Exclusive Premium Content", Icons.Filled.Star)

                Spacer(Modifier.height(32.dp))

                // Code input
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(14) },
                    label = { Text("Activation Code") },
                    placeholder = { Text("VLRA-XXXX-XXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (code.isNotBlank() && !uiState.isActivating) {
                                viewModel.activateCode(code.trim())
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD4AF37),
                        unfocusedBorderColor = CineVaultTheme.colors.borderSubtle,
                        focusedLabelColor = Color(0xFFD4AF37),
                        cursorColor = Color(0xFFD4AF37),
                        focusedTextColor = CineVaultTheme.colors.textPrimary,
                        unfocusedTextColor = CineVaultTheme.colors.textPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.activateCode(code.trim()) },
                    enabled = code.isNotBlank() && !uiState.isActivating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFD4AF37), Color(0xFFFFBE45))
                                ),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (uiState.isActivating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                "ACTIVATE PREMIUM",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                letterSpacing = 1.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumFeatureItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFFD4AF37),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 15.sp,
        )
    }
}
