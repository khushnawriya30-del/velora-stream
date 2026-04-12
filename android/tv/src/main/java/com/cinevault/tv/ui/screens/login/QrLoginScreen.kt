package com.cinevault.tv.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    val d = LocalTvDimens.current

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
                .fillMaxWidth(d.qrLoginFractionW)
                .fillMaxHeight(d.qrLoginFractionH)
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left side - Branding
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = d.padXXL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "VELORA",
                    fontSize = d.fontDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    color = TvPrimary,
                    letterSpacing = d.fontSmall * 0.5f,
                )
                Spacer(modifier = Modifier.height(d.padSmall))
                Text(
                    text = "PREMIUM TV",
                    fontSize = d.fontXL,
                    fontWeight = FontWeight.Light,
                    color = TvOnSurfaceVariant,
                    letterSpacing = d.fontSmall * 0.6f,
                )
                Spacer(modifier = Modifier.height(d.padXXL))
                Text(
                    text = "Stream unlimited movies & series\non the big screen",
                    fontSize = d.fontMedium,
                    color = TvTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = d.lineHeightLarge,
                )
            }

            // Right side - QR Code
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(d.padLarge))
                    .background(TvSurface)
                    .padding(d.padXXL)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Scan to Login",
                    fontSize = d.fontXXL,
                    fontWeight = FontWeight.SemiBold,
                    color = TvOnSurface,
                )

                Spacer(modifier = Modifier.height(d.padSmall))

                Text(
                    text = "Open Velora app on your phone\nand scan this QR code",
                    fontSize = d.fontBody,
                    color = TvTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = d.lineHeightBody,
                )

                Spacer(modifier = Modifier.height(d.padXL))

                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .size(d.qrSize)
                            .clip(RoundedCornerShape(d.padMedium))
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Loading...",
                            color = TvTextMuted,
                            fontSize = d.fontMedium,
                        )
                    }
                } else if (state.qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(d.qrSize)
                            .clip(RoundedCornerShape(d.padMedium))
                            .background(Color.White)
                            .padding(d.padMedium),
                    ) {
                        Image(
                            bitmap = state.qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code for login",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(d.padLarge))
                    Text(
                        text = state.error!!,
                        color = TvPrimary,
                        fontSize = d.fontBody,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(d.padMedium))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(d.padSmall))
                            .background(TvPrimary)
                            .clickable { viewModel.generateQrCode() }
                            .padding(horizontal = d.padXL, vertical = d.padSmall),
                    ) {
                        Text(
                            text = "Retry",
                            color = Color.Black,
                            fontSize = d.fontBody,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(d.padXL))

                // Steps
                Column(horizontalAlignment = Alignment.Start) {
                    StepRow(number = "1", text = "Open Velora on your phone")
                    Spacer(modifier = Modifier.height(d.padSmall))
                    StepRow(number = "2", text = "Go to Profile → Link TV")
                    Spacer(modifier = Modifier.height(d.padSmall))
                    StepRow(number = "3", text = "Scan the QR code above")
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StepRow(number: String, text: String) {
    val d = LocalTvDimens.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(d.stepCircle)
                .clip(RoundedCornerShape(d.stepCircle / 2))
                .background(TvPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = d.fontBody,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(d.padMedium))
        Text(
            text = text,
            color = TvOnSurfaceVariant,
            fontSize = d.fontBody,
        )
    }
}
