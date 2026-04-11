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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cinevault.app.R
import com.cinevault.app.data.model.MovieDto
import com.cinevault.app.data.model.PremiumOfferDto
import com.cinevault.app.data.model.ProfileDto
import com.cinevault.app.data.model.WatchProgressDto
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.EarnMoneyViewModel
import com.cinevault.app.ui.viewmodel.PremiumViewModel
import com.cinevault.app.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Premium text color – dark brown for strong contrast on gold card
private val PremTextOnGold = Color(0xFF4A3000)

@Composable
fun MeScreen(
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onNavigateToEarnMoney: () -> Unit = {},
    onMovieClick: (String) -> Unit = {},
    onAddMoreWatchlist: () -> Unit = {},
    onAddMoreCollection: () -> Unit = {},
    /** Navigate to content from history. For movies: episodeId = null. For episodes: episodeId = episode id, contentId = seriesId. */
    onHistoryItemClick: (contentId: String, episodeId: String?) -> Unit = { _, _ -> },
    profileViewModel: ProfileViewModel = hiltViewModel(),
    premiumViewModel: PremiumViewModel = hiltViewModel(),
    earnMoneyViewModel: EarnMoneyViewModel = hiltViewModel(),
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val premiumState by premiumViewModel.uiState.collectAsState()
    val walletState by earnMoneyViewModel.uiState.collectAsState()

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
        MePremiumSection(
            isPremium = premiumState.isPremium,
            plan = premiumState.plan,
            daysRemaining = premiumState.daysRemaining,
            expiresAt = premiumState.expiresAt,
            offers = premiumState.offers,
            onSubscribeClick = onNavigateToPremium,
            onRenewClick = onNavigateToPremium,
        )

        // ── Earn Money Card ──
        MeEarnMoneyCard(
            balance = walletState.balance,
            withdrawThreshold = walletState.withdrawThreshold,
            canWithdraw = walletState.canWithdraw,
            onWithdrawClick = onNavigateToEarnMoney,
            onCardClick = onNavigateToEarnMoney,
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
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(2.dp))
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

        // ── My Watchlist Section ──
        MeSectionHeaderWithAddMore(
            icon = Icons.Outlined.BookmarkBorder,
            title = "My Watchlist",
            onAddMore = onAddMoreWatchlist,
        )
        Spacer(Modifier.height(12.dp))
        MeWatchlistSection(
            userMovies = uiState.watchlist,
            randomPool = uiState.randomContentPool,
            onMovieClick = onMovieClick,
            onAddMore = onAddMoreWatchlist,
        )
        Spacer(Modifier.height(28.dp))

        // ── My Thematic Collection Section ──
        MeSectionHeaderWithAddMore(
            icon = Icons.Filled.Star,
            title = "My Thematic Collection",
            onAddMore = onAddMoreCollection,
        )
        Spacer(Modifier.height(12.dp))
        MeThematicCollectionSection(
            userMovies = uiState.thematicCollection,
            randomPool = uiState.randomContentPool,
            onMovieClick = onMovieClick,
            onAddMore = onAddMoreCollection,
        )
        Spacer(Modifier.height(28.dp))

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
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
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
// ── Premium Subscription Card (Dynamic – offer from Admin Panel) ──
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun MePremiumSection(
    isPremium: Boolean,
    plan: String?,
    daysRemaining: Int?,
    expiresAt: String?,
    offers: List<PremiumOfferDto> = emptyList(),
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
            .padding(horizontal = 10.dp)
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
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("Your ") }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)) { append("Premium Valid") }
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(" Till ") }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formattedExpiry) }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .padding(start = 16.dp, end = 16.dp, bottom = 50.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = expiryAnnotated,
                    fontSize = 13.sp,
                    color = PremTextOnGold,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            // ── Dynamic Subscribe Card ──
            val offer = offers.firstOrNull()
            DynamicPremiumSubscribeCard(
                offer = offer,
                onClick = onSubscribeClick,
            )
        }
    }
}

@Composable
private fun DynamicPremiumSubscribeCard(
    offer: PremiumOfferDto?,
    onClick: () -> Unit,
) {
    // ── PNG-based banner: use the provided premium_card_subscribe_1.png ──
    Box(modifier = Modifier.fillMaxWidth()) {
        // Full-width PNG banner as background
        Image(
            painter = painterResource(R.drawable.premium_card_subscribe_1),
            contentDescription = "Premium Banner",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
        // Dynamic text overlay in the upper-left empty area of the PNG
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.58f)
                .padding(start = 20.dp, top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (offer?.bannerText?.isNotBlank() == true) {
                Text(
                    text = offer.bannerText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.90f),
                    lineHeight = 18.sp,
                    maxLines = 2,
                )
            }
            if (offer != null) {
                val durationLabel = when (offer.durationMonths) {
                    1 -> "/ month"
                    3 -> "/ 3 months"
                    6 -> "/ 6 months"
                    12 -> "/ year"
                    else -> "/ ${offer.durationMonths} months"
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "₹${offer.discountPrice}",
                        fontSize = 33.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF3E5AB),
                        lineHeight = 37.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = durationLabel,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
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

// ── Earn Money Card for Me Tab ──
@Composable
private fun MeEarnMoneyCard(
    balance: Int,
    withdrawThreshold: Int,
    canWithdraw: Boolean,
    onWithdrawClick: () -> Unit,
    onCardClick: () -> Unit,
) {
    // ── Coin 360° rotation (2s, infinite, smooth) ──
    val infiniteTransition = rememberInfiniteTransition(label = "earnCoin")
    val coinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "coinRotate",
    )

    // ── Counting animation on ₹100 target, replays every Me section visit ──
    var animTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { animTrigger++ }
    val animatedThreshold by animateFloatAsState(
        targetValue = if (animTrigger > 0) withdrawThreshold.toFloat() else 0f,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "thresholdCount",
    )

    // ── Withdraw button breathing / glow / pulse animation ──
    val btnPulse = rememberInfiniteTransition(label = "wdPulse")
    val btnScale by btnPulse.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wdScale",
    )
    val btnGlow by btnPulse.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wdGlow",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onCardClick),
    ) {
        // Card background: gold border + dark blue inner, sharp rounded edges
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFAB00), Color(0xFFFFA000))),
                    shape = RoundedCornerShape(10.dp),
                )
                .background(
                    Brush.linearGradient(listOf(Color(0xFF1A1A3E), Color(0xFF252560), Color(0xFF1E1E4A))),
                    RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // ── Rotating Coin ──
                Image(
                    painter = painterResource(R.drawable.ic_earn_coin),
                    contentDescription = "Earn Coin",
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer {
                            rotationY = coinRotation
                            cameraDistance = 12f * density
                        },
                    contentScale = ContentScale.Fit,
                )

                Spacer(Modifier.width(10.dp))

                // ── Text Section ──
                Column(modifier = Modifier.weight(1f)) {
                    // "You've earned ₹80" – normal weight, no counting animation
                    Text(
                        text = buildAnnotatedString {
                            append("You've earned ")
                            withStyle(SpanStyle(color = Color(0xFFFFD700))) {
                                append("₹${balance}.00")
                            }
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    if (!canWithdraw) {
                        // "Keep going to cash out ₹100" – bold, larger, highlighted, ₹100 animates
                        Text(
                            text = buildAnnotatedString {
                                append("Keep going to cash out ")
                                withStyle(SpanStyle(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD700),
                                    fontSize = 16.sp,
                                )) {
                                    append("₹${animatedThreshold.toInt()}")
                                }
                                append("!")
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE0E0E0),
                        )
                    } else {
                        Text(
                            text = "Ready to withdraw! \uD83C\uDF89",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                // ── Withdraw Button (orange gradient + yellow border, always colored) ──
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = btnScale
                            scaleY = btnScale
                            alpha = btnGlow
                        }
                        .border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFC107), Color(0xFFFFB300))),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFFFFB74D), Color(0xFFFF9800), Color(0xFFEF6C00))),
                            RoundedCornerShape(8.dp),
                        )
                        .clickable(onClick = onWithdrawClick)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Withdraw",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ── Section Header with Add More button ──
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MeSectionHeaderWithAddMore(icon: ImageVector, title: String, onAddMore: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = CineVaultTheme.colors.accentGold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = CineVaultTheme.typography.sectionTitle,
            color = CineVaultTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAddMore) {
            Text("+ Add More", style = CineVaultTheme.typography.label, color = CineVaultTheme.colors.accentGold)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = CineVaultTheme.colors.accentGold, modifier = Modifier.size(16.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ── My Watchlist Section (smart auto-loop or static) ──
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MeWatchlistSection(
    userMovies: List<MovieDto>,
    randomPool: List<MovieDto>,
    onMovieClick: (String) -> Unit,
    onAddMore: () -> Unit,
) {
    if (userMovies.isNotEmpty()) {
        // ── User has content → static horizontal scroll, NO auto-loop ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(userMovies) { movie ->
                MeWatchlistCard(movie = movie, onClick = { onMovieClick(movie.id) })
            }
        }
    } else if (randomPool.size >= 2) {
        // ── Empty → Auto-looping random 2 cards with fade animation ──
        MeAutoLoopWatchlist(
            pool = randomPool,
            onMovieClick = onMovieClick,
            onAddMore = onAddMore,
        )
    } else {
        // ── No pool yet (loading) → placeholder ──
        MeEmptyCarouselPlaceholder(onAddMore = onAddMore)
    }
}

@Composable
private fun MeAutoLoopWatchlist(
    pool: List<MovieDto>,
    onMovieClick: (String) -> Unit,
    onAddMore: () -> Unit,
) {
    // Two slots that randomly swap content
    var slot1 by remember { mutableStateOf(pool.random()) }
    var slot2 by remember { mutableStateOf(pool.filter { it.id != slot1.id }.randomOrNull() ?: pool.random()) }
    var alpha1 by remember { mutableFloatStateOf(1f) }
    var alpha2 by remember { mutableFloatStateOf(1f) }

    val animatedAlpha1 by animateFloatAsState(
        targetValue = alpha1,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "watchlistAlpha1",
    )
    val animatedAlpha2 by animateFloatAsState(
        targetValue = alpha2,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "watchlistAlpha2",
    )

    // Auto loop: swap one card every 1.5s, alternating slots
    LaunchedEffect(pool) {
        if (pool.size < 2) return@LaunchedEffect
        var swapSlot1Next = true
        while (isActive) {
            delay(2500L)
            val usedIds = setOf(slot1.id, slot2.id)
            val candidates = pool.filter { it.id !in usedIds }.ifEmpty { pool }
            val newMovie = candidates.random()
            if (swapSlot1Next) {
                alpha1 = 0f
                delay(400L)
                slot1 = newMovie
                alpha1 = 1f
            } else {
                alpha2 = 0f
                delay(400L)
                slot2 = newMovie
                alpha2 = 1f
            }
            swapSlot1Next = !swapSlot1Next
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = animatedAlpha1 },
        ) {
            MeWatchlistCard(movie = slot1, onClick = { onMovieClick(slot1.id) })
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = animatedAlpha2 },
        ) {
            MeWatchlistCard(movie = slot2, onClick = { onMovieClick(slot2.id) })
        }
    }
}

@Composable
private fun MeWatchlistCard(movie: MovieDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CineVaultTheme.colors.surface),
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // Quality badge
            movie.videoQuality?.let { quality ->
                if (quality.isNotBlank()) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = CineVaultTheme.colors.accentGold.copy(alpha = 0.9f),
                    ) {
                        Text(
                            text = quality.uppercase(),
                            style = CineVaultTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            // Rating badge
            if ((movie.rating ?: 0.0) > 0.0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xCC000000),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = CineVaultTheme.colors.accentGold, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(String.format("%.1f", movie.rating), style = CineVaultTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = movie.title,
            style = CineVaultTheme.typography.labelSmall,
            color = CineVaultTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// ── My Thematic Collection Section (stacked + smart auto-loop) ──
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MeThematicCollectionSection(
    userMovies: List<MovieDto>,
    randomPool: List<MovieDto>,
    onMovieClick: (String) -> Unit,
    onAddMore: () -> Unit,
) {
    if (userMovies.isNotEmpty()) {
        // ── User has content → static stacked cards, NO auto-loop ──
        MeStaticStackedCarousel(movies = userMovies, onMovieClick = onMovieClick)
    } else if (randomPool.size >= 2) {
        // ── Empty → Auto-looping random 2 stacked cards ──
        MeAutoLoopStackedCollection(
            pool = randomPool,
            onMovieClick = onMovieClick,
            onAddMore = onAddMore,
        )
    } else {
        MeEmptyCarouselPlaceholder(onAddMore = onAddMore)
    }
}

@Composable
private fun MeStaticStackedCarousel(
    movies: List<MovieDto>,
    onMovieClick: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(movies) { movie ->
            MeStackedCardWithDepth(movie = movie, onClick = { onMovieClick(movie.id) })
        }
    }
}

@Composable
private fun MeAutoLoopStackedCollection(
    pool: List<MovieDto>,
    onMovieClick: (String) -> Unit,
    onAddMore: () -> Unit,
) {
    // Two visible front cards + ghost "behind" cards for depth
    var slot1 by remember { mutableStateOf(pool.random()) }
    var slot2 by remember { mutableStateOf(pool.filter { it.id != slot1.id }.randomOrNull() ?: pool.random()) }
    var bg1 by remember { mutableStateOf(pool.filter { it.id != slot1.id && it.id != slot2.id }.randomOrNull() ?: pool.random()) }
    var bg2 by remember { mutableStateOf(pool.filter { it.id !in setOf(slot1.id, slot2.id, bg1.id) }.randomOrNull() ?: pool.random()) }
    var alpha1 by remember { mutableFloatStateOf(1f) }
    var alpha2 by remember { mutableFloatStateOf(1f) }

    val animatedAlpha1 by animateFloatAsState(
        targetValue = alpha1,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "collAlpha1",
    )
    val animatedAlpha2 by animateFloatAsState(
        targetValue = alpha2,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "collAlpha2",
    )

    // Auto loop: swap cards every ~1.8s alternating
    LaunchedEffect(pool) {
        if (pool.size < 2) return@LaunchedEffect
        var swapSlot1Next = true
        while (isActive) {
            delay(3500L)
            val usedIds = setOf(slot1.id, slot2.id, bg1.id, bg2.id)
            val candidates = pool.filter { it.id !in usedIds }.ifEmpty { pool }
            val newMovie = candidates.random()
            if (swapSlot1Next) {
                alpha1 = 0f
                delay(450L)
                bg1 = slot1 // old front becomes background
                slot1 = newMovie
                alpha1 = 1f
            } else {
                alpha2 = 0f
                delay(450L)
                bg2 = slot2
                slot2 = newMovie
                alpha2 = 1f
            }
            swapSlot1Next = !swapSlot1Next
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Slot 1: stacked with depth
        Box(modifier = Modifier.weight(1f)) {
            MeStackedSlot(
                frontMovie = slot1,
                bgMovie = bg1,
                frontAlpha = animatedAlpha1,
                onClick = { onMovieClick(slot1.id) },
            )
        }
        // Slot 2: stacked with depth
        Box(modifier = Modifier.weight(1f)) {
            MeStackedSlot(
                frontMovie = slot2,
                bgMovie = bg2,
                frontAlpha = animatedAlpha2,
                onClick = { onMovieClick(slot2.id) },
            )
        }
    }
}

/** A single stacked card slot: background card behind + front card with animated alpha */
@Composable
private fun MeStackedSlot(
    frontMovie: MovieDto,
    bgMovie: MovieDto,
    frontAlpha: Float,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
    ) {
        // ── Background card (behind, scaled down, faded) ──
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(195.dp)
                .align(Alignment.TopCenter)
                .zIndex(0f)
                .graphicsLayer {
                    scaleX = 0.92f
                    scaleY = 0.92f
                    translationY = -8f
                    alpha = 0.35f
                }
                .clip(RoundedCornerShape(14.dp))
                .background(CineVaultTheme.colors.surface),
        ) {
            AsyncImage(
                model = bgMovie.posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // ── Front card ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
                .align(Alignment.BottomCenter)
                .zIndex(1f)
                .graphicsLayer { alpha = frontAlpha },
        ) {
            MeStackedCardWithDepth(movie = frontMovie, onClick = onClick)
        }
    }
}

@Composable
private fun MeStackedCardWithDepth(movie: MovieDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CineVaultTheme.colors.surface)
                .border(
                    width = 1.dp,
                    color = CineVaultTheme.colors.border.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(14.dp),
                ),
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000)),
                        ),
                    ),
            )
            // Title overlay
            Text(
                text = movie.title,
                style = CineVaultTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
            // Category tag
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                shape = RoundedCornerShape(4.dp),
                color = CineVaultTheme.colors.accentGold.copy(alpha = 0.85f),
            ) {
                Text(
                    text = movie.contentType.replaceFirstChar { it.uppercase() },
                    style = CineVaultTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ── Empty State Placeholder ──
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MeEmptyCarouselPlaceholder(onAddMore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(185.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CineVaultTheme.colors.surface.copy(alpha = 0.5f))
                .border(1.dp, CineVaultTheme.colors.border.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable(onClick = onAddMore),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add More",
                    tint = CineVaultTheme.colors.textSecondary,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "+ Add More",
                    style = CineVaultTheme.typography.labelSmall,
                    color = CineVaultTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
