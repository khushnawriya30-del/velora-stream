package com.cinevault.app.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cinevault.app.R
import com.cinevault.app.data.model.MovieDto
import com.cinevault.app.data.model.ProfileDto
import com.cinevault.app.data.model.WatchProgressDto
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.PremiumViewModel
import com.cinevault.app.ui.viewmodel.ProfileViewModel

// Premium text color
private val PremTextOnGold = Color(0xFF7A5C1F)

@Composable
fun MeScreen(
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onMovieClick: (String) -> Unit = {},
    /** Navigate to content from history. For movies: episodeId = null. For episodes: episodeId = episode id, contentId = seriesId. */
    onHistoryItemClick: (contentId: String, episodeId: String?) -> Unit = { _, _ -> },
    profileViewModel: ProfileViewModel = hiltViewModel(),
    premiumViewModel: PremiumViewModel = hiltViewModel(),
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val premiumState by premiumViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        MePremiumHeader(
            userName = uiState.userName,
            userEmail = uiState.userEmail,
            onBellClick = onNavigateToNotifications,
            onSettingsClick = onNavigateToSettings,
        )

        // ── Premium Subscription Section ──
        Spacer(Modifier.height(4.dp))
        MePremiumSection(
            isPremium = premiumState.isPremium,
            plan = premiumState.plan,
            daysRemaining = premiumState.daysRemaining,
            expiresAt = premiumState.expiresAt,
            onSubscribeClick = onNavigateToPremium,
            onRenewClick = onNavigateToPremium,
        )

        if (uiState.profiles.size > 1) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Switch Profile",
                style = CineVaultTheme.typography.labelSmall,
                color = CineVaultTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(uiState.profiles) { profile ->
                    MeProfileChip(
                        profile = profile,
                        isActive = profile.id == uiState.activeProfile?.id,
                        onClick = { profileViewModel.selectProfile(profile) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(4.dp))
        }

        if (uiState.watchHistory.isNotEmpty()) {
            MeSectionHeader(icon = Icons.Filled.History, title = "Watch History", onSeeAll = null)
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.watchHistory) { progress ->
                    MeWatchHistoryCard(
                        progress = progress,
                        onClick = {
                            if (progress.contentType == "episode" && !progress.seriesId.isNullOrBlank()) {
                                onHistoryItemClick(progress.seriesId, progress.contentId)
                            } else {
                                onHistoryItemClick(progress.contentId, null)
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        if (uiState.watchlist.isNotEmpty()) {
            MeSectionHeader(icon = Icons.Outlined.BookmarkBorder, title = "My List", onSeeAll = null)
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.watchlist) { movie ->
                    MeMovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        if (uiState.likedMovies.isNotEmpty()) {
            MeSectionHeader(icon = Icons.Outlined.ThumbUp, title = "Liked Videos", onSeeAll = null)
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.likedMovies) { movie ->
                    MeMovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun MePremiumHeader(
    userName: String,
    userEmail: String,
    onBellClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(CineVaultTheme.colors.surface.copy(alpha = 0.9f), CineVaultTheme.colors.background)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(58.dp).clip(CircleShape)
                    .background(Color(0xFF333333)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = userName.take(1).uppercase().ifEmpty { "?" },
                    style = CineVaultTheme.typography.sectionTitle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFF999999),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hello! ${userName.ifBlank { "Valued User" }}",
                    style = CineVaultTheme.typography.sectionTitle,
                    color = CineVaultTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (userEmail.isNotBlank()) {
                    Text(text = userEmail, style = CineVaultTheme.typography.labelSmall, color = CineVaultTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onBellClick) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = CineVaultTheme.colors.textPrimary, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = CineVaultTheme.colors.textPrimary, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun MeSectionHeader(icon: ImageVector, title: String, onSeeAll: (() -> Unit)?) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = CineVaultTheme.colors.accentGold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = title, style = CineVaultTheme.typography.sectionTitle, color = CineVaultTheme.colors.textPrimary, modifier = Modifier.weight(1f))
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("See All", style = CineVaultTheme.typography.label, color = CineVaultTheme.colors.accentGold)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = CineVaultTheme.colors.accentGold, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MeMovieCard(movie: MovieDto, onClick: () -> Unit) {
    Column(modifier = Modifier.width(110.dp).clickable(onClick = onClick)) {
        Box(modifier = Modifier.fillMaxWidth().height(155.dp).clip(RoundedCornerShape(10.dp)).background(CineVaultTheme.colors.surface)) {
            AsyncImage(model = movie.posterUrl, contentDescription = movie.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if ((movie.rating ?: 0.0) > 0.0) {
                Surface(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp), shape = RoundedCornerShape(4.dp), color = Color(0xCC000000)) {
                    Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = CineVaultTheme.colors.accentGold, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(String.format("%.1f", movie.rating), style = CineVaultTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(movie.title, style = CineVaultTheme.typography.labelSmall, color = CineVaultTheme.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MeWatchHistoryCard(progress: WatchProgressDto, onClick: () -> Unit) {
    val fraction = if (progress.totalDuration > 0) (progress.currentTime.toFloat() / progress.totalDuration).coerceIn(0f, 1f) else 0f
    val watchedMin = (progress.currentTime / 60000).coerceAtLeast(0)
    val remainingMin = ((progress.totalDuration - progress.currentTime) / 60000).coerceAtLeast(0)

    Column(modifier = Modifier.width(150.dp).clickable(onClick = onClick)) {
        Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(10.dp)).background(CineVaultTheme.colors.surface)) {
            // Poster / thumbnail
            AsyncImage(model = progress.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

            // Bottom gradient overlay for text readability
            Box(modifier = Modifier.fillMaxWidth().height(48.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xDD000000)))))

            // Completed badge
            if (progress.isCompleted) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF22C55E).copy(alpha = 0.9f),
                ) {
                    Text("Watched", style = CineVaultTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.SemiBold), color = Color.White, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                }
            }

            // Play icon
            Box(modifier = Modifier.size(32.dp).align(Alignment.Center).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            // Watched / Remaining time overlay at bottom
            if (progress.totalDuration > 0 && !progress.isCompleted) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${watchedMin}m watched", style = CineVaultTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color.White.copy(alpha = 0.9f))
                    Text("${remainingMin}m left", style = CineVaultTheme.typography.labelSmall.copy(fontSize = 8.sp), color = CineVaultTheme.colors.accentGold)
                }
            }

            // Progress bar at very bottom
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter).background(CineVaultTheme.colors.surface.copy(alpha = 0.4f))) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).background(CineVaultTheme.colors.accentGold))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(progress.contentTitle ?: "", style = CineVaultTheme.typography.labelSmall, color = CineVaultTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MeProfileChip(profile: ProfileDto, isActive: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.width(64.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape)
                .background(if (isActive) CineVaultTheme.colors.accentGold.copy(alpha = 0.15f) else CineVaultTheme.colors.surface)
                .then(if (isActive) Modifier.border(2.dp, CineVaultTheme.colors.accentGold, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (profile.avatarUrl?.isNotBlank() == true) {
                AsyncImage(model = profile.avatarUrl, contentDescription = profile.name, modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else {
                Text(profile.name.take(1).uppercase(), style = CineVaultTheme.typography.sectionTitle.copy(fontSize = 18.sp), color = if (isActive) CineVaultTheme.colors.accentGold else CineVaultTheme.colors.textSecondary)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(profile.name, style = CineVaultTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if (isActive) CineVaultTheme.colors.accentGold else CineVaultTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

// ══════════════════════════════════════════════════════════════════════
// ── Premium Subscription Card (PNG-based) ──
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun MePremiumSection(
    isPremium: Boolean,
    plan: String?,
    daysRemaining: Int?,
    expiresAt: String?,
    onSubscribeClick: () -> Unit,
    onRenewClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "premCardTap",
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = if (isPremium) onRenewClick else onSubscribeClick,
            ),
    ) {
        if (isPremium) {
            // ── Gold Active Card PNG (cropped to remove transparent margins) ──
            Image(
                painter = painterResource(R.drawable.premium_card_active),
                contentDescription = "Premium Active",
                modifier = Modifier.fillMaxWidth().aspectRatio(2.3f),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
            )
            // Format expiry date: "DD MMM, YYYY"
            val formattedExpiry = remember(expiresAt) {
                formatPremiumDate(expiresAt)
            }
            val expiryAnnotated = buildAnnotatedString {
                append("Your ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)) { append("Premium Valid") }
                append(" Till ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formattedExpiry) }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .padding(start = 16.dp, end = 16.dp, bottom = 36.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = expiryAnnotated,
                    fontSize = 12.sp,
                    color = PremTextOnGold,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            // ── Dark Subscribe Card PNG ──
            Image(
                painter = painterResource(R.drawable.premium_card_subscribe),
                contentDescription = "Subscribe to Premium",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

/** Format ISO date or daysRemaining to "DD MMM, YYYY" */
private fun formatPremiumDate(expiresAt: String?): String {
    if (expiresAt == null) return "Active"
    return try {
        val monthAbbr = arrayOf(
            "JAN", "FEB", "MAR", "APR", "MAY", "JUNE",
            "JULY", "AUG", "SEPT", "OCT", "NOV", "DEC",
        )
        // Parse ISO date string (e.g. "2026-04-05T11:35:28.000Z")
        val parsed = java.time.ZonedDateTime.parse(expiresAt)
        val day = String.format("%02d", parsed.dayOfMonth)
        val month = monthAbbr[parsed.monthValue - 1]
        val year = parsed.year
        "$day $month, $year"
    } catch (_: Exception) {
        expiresAt // fallback to raw string
    }
}
