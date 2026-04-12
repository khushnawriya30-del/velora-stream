package com.cinevault.tv.ui.screens.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.cinevault.tv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PremiumGateScreen(onLogout: () -> Unit) {
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .clip(RoundedCornerShape(20.dp))
                .background(TvSurface)
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "👑",
                fontSize = 56.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Premium Required",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TvGold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "CineVault TV is exclusively available\nfor Premium members",
                fontSize = 16.sp,
                color = TvDimText,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvSurfaceVariant)
                    .padding(24.dp),
            ) {
                Text(
                    text = "How to get Premium:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TvOnSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "1. Open CineVault app on your phone",
                    fontSize = 14.sp,
                    color = TvOnSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "2. Go to Premium section",
                    fontSize = 14.sp,
                    color = TvOnSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "3. Subscribe to any plan",
                    fontSize = 14.sp,
                    color = TvOnSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "4. Login again on TV",
                    fontSize = 14.sp,
                    color = TvOnSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

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
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
        }
    }
}
