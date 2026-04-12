package com.cinevault.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.ui.components.GoldButton
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import com.cinevault.app.ui.theme.LocalAppDimens

data class OnboardingPage(val title: String, val subtitle: String, val icon: String)

private val pages = listOf(
    OnboardingPage(
        title = "Cinematic Streaming",
        subtitle = "Experience movies and series in stunning quality with our premium streaming platform.",
        icon = "🎬",
    ),
    OnboardingPage(
        title = "Curated For You",
        subtitle = "Discover personalized recommendations powered by intelligent algorithms that learn your taste.",
        icon = "✨",
    ),
    OnboardingPage(
        title = "Watch Anywhere",
        subtitle = "Continue watching seamlessly across devices with automatic progress sync and offline downloads.",
        icon = "📱",
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad12),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = {
                    viewModel.completeOnboarding()
                    onComplete()
                }) {
                    Text(
                        "Skip",
                        style = CineVaultTheme.typography.label,
                        color = CineVaultTheme.colors.textSecondary,
                    )
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // Indicators
            Row(
                modifier = Modifier.padding(LocalAppDimens.current.pad16),
                horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad8),
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) CineVaultTheme.colors.accentGold
                                else CineVaultTheme.colors.surface,
                            )
                            .animateContentSize(animationSpec = tween(300)),
                    )
                }
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad16))

            // Button
            GoldButton(
                text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next",
                onClick = {
                    if (pagerState.currentPage == pages.size - 1) {
                        viewModel.completeOnboarding()
                        onComplete()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.padding(horizontal = LocalAppDimens.current.pad24),
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = LocalAppDimens.current.pad32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Icon placeholder
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CineVaultTheme.colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(page.icon, style = CineVaultTheme.typography.heroTitle)
        }
        Spacer(Modifier.height(40.dp))
        Text(
            page.title,
            style = CineVaultTheme.typography.displayLarge,
            color = CineVaultTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(LocalAppDimens.current.pad12))
        Text(
            page.subtitle,
            style = CineVaultTheme.typography.body,
            color = CineVaultTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
