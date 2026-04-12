package com.cinevault.tv.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.cinevault.tv.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onComplete: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val d = LocalTvDimens.current

    LaunchedEffect(Unit) {
        visible = true
        delay(2000)
        onComplete()
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "splash_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground),
        contentAlignment = Alignment.Center
    ) {
        // Background glow
        Box(
            modifier = Modifier
                .size(d.splashGlow)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TvPrimary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {
            Text(
                text = "VELORA",
                fontSize = d.fontSplash,
                fontWeight = FontWeight.ExtraBold,
                color = TvPrimary,
                letterSpacing = d.fontSmall * 0.5f,
            )

            Spacer(modifier = Modifier.height(d.padSmall))

            Text(
                text = "Premium Streaming",
                fontSize = d.fontMedium,
                fontWeight = FontWeight.Medium,
                color = TvOnSurfaceVariant,
                letterSpacing = d.fontSmall * 0.15f,
            )

            Spacer(modifier = Modifier.height(d.padSection))

            CircularProgressIndicator(
                modifier = Modifier.size(d.splashProgress),
                color = TvPrimary,
                strokeWidth = d.padTiny,
            )
        }
    }
}
