package com.cinevault.tv.ui.screens.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.cinevault.tv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PremiumGateScreen(onLogout: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val d = LocalTvDimens.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(d.premiumGateFraction)
                .clip(RoundedCornerShape(d.padXL))
                .background(TvSurface)
                .padding(d.padSection),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "👑",
                fontSize = d.fontSplash,
            )

            Spacer(modifier = Modifier.height(d.padLarge))

            Text(
                text = "Premium Required",
                fontSize = d.fontHero,
                fontWeight = FontWeight.Bold,
                color = TvPrimary,
            )

            Spacer(modifier = Modifier.height(d.padMedium))

            Text(
                text = "Velora TV requires a Premium subscription\nto play content. You can still browse.",
                fontSize = d.fontMedium,
                color = TvTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = d.lineHeightLarge,
            )

            Spacer(modifier = Modifier.height(d.padXXL))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(d.padMedium))
                    .background(TvSurfaceVariant)
                    .padding(d.padXL),
            ) {
                Text(
                    text = "How to get Premium on TV:",
                    fontSize = d.fontMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TvOnSurface,
                )
                Spacer(modifier = Modifier.height(d.padMedium))
                Text(text = "1. Open Velora app on your phone", fontSize = d.fontBody, color = TvOnSurfaceVariant)
                Spacer(modifier = Modifier.height(d.padSmall))
                Text(text = "2. Go to Premium section", fontSize = d.fontBody, color = TvOnSurfaceVariant)
                Spacer(modifier = Modifier.height(d.padSmall))
                Text(text = "3. Subscribe to any plan", fontSize = d.fontBody, color = TvOnSurfaceVariant)
                Spacer(modifier = Modifier.height(d.padSmall))
                Text(text = "4. Login again on TV", fontSize = d.fontBody, color = TvOnSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(d.padXXL))

            Button(
                onClick = onLogout,
                modifier = Modifier.focusRequester(focusRequester),
                colors = ButtonDefaults.colors(
                    containerColor = TvPrimary,
                    contentColor = TvOnPrimary,
                ),
            ) {
                Text(
                    text = "Logout & Try Again",
                    fontSize = d.fontMedium,
                    modifier = Modifier.padding(horizontal = d.padXL, vertical = d.padTiny),
                )
            }
        }
    }
}
