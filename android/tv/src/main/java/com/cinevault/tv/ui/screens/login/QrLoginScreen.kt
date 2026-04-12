package com.cinevault.tv.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.cinevault.tv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QrLoginScreen(
    onLoginSuccess: (isPremium: Boolean) -> Unit,
    viewModel: QrLoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) {
            onLoginSuccess(state.isPremium)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left side - Branding
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "VELORA",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TvPrimary,
                    letterSpacing = 6.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PREMIUM TV",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    color = TvOnSurfaceVariant,
                    letterSpacing = 8.sp,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Stream unlimited movies & series\non the big screen",
                    fontSize = 16.sp,
                    color = TvTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                )
            }

            // Right side - QR Code
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvSurface)
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Scan to Login",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TvOnSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Open Velora app on your phone\nand scan this QR code",
                    fontSize = 14.sp,
                    color = TvTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Loading...",
                            color = TvTextMuted,
                            fontSize = 16.sp,
                        )
                    }
                } else if (state.qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                    ) {
                        Image(
                            bitmap = state.qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code for login",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error!!,
                        color = TvPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Steps
                Column(horizontalAlignment = Alignment.Start) {
                    StepRow(number = "1", text = "Open Velora on your phone")
                    Spacer(modifier = Modifier.height(8.dp))
                    StepRow(number = "2", text = "Go to Profile → Link TV")
                    Spacer(modifier = Modifier.height(8.dp))
                    StepRow(number = "3", text = "Scan the QR code above")
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StepRow(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(TvPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = TvOnSurfaceVariant,
            fontSize = 14.sp,
        )
    }
}
