package com.cinevault.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.cinevault.app.R
import com.cinevault.app.data.model.BannerDto
import com.cinevault.app.data.model.HomeSectionDto
import com.cinevault.app.data.model.MovieDto
import com.cinevault.app.ui.components.ContinueWatchingCard
import com.cinevault.app.ui.components.TrendingMovieCard
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.theme.LocalAppDimens
import com.cinevault.app.ui.viewmodel.HomeViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onMovieClick: (String) -> Unit,
    onPlayClick: (String, String?) -> Unit = { id, _ -> onMovieClick(id) },
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    onEarnMoneyClick: () -> Unit = {},
    onSectionClick: ((HomeSectionDto) -> Unit)? = null,
    onAddToWatchlist: ((String) -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentBannerIndex by remember(uiState.selectedTab) { mutableIntStateOf(0) }
    val swipeRefreshState = rememberSwipeRefreshState(uiState.isRefreshing)

    // Auto-rotate banners
    LaunchedEffect(uiState.tabBanners.size, uiState.selectedTab) {
        while (true) {
            delay(3500)
            if (uiState.tabBanners.isNotEmpty()) {
                currentBannerIndex = (currentBannerIndex + 1) % uiState.tabBanners.size
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background)
    ) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refresh() },
            indicator = { state, trigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = trigger,
                    contentColor = CineVaultTheme.colors.accentGold,
                    backgroundColor = CineVaultTheme.colors.surface,
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Search Bar + Bell icon side by side ──
                item {
                    SearchBarWithBell(
                        onSearchClick = onSearchClick,
                        onNotificationsClick = onNotificationsClick,
                        onEarnMoneyClick = onEarnMoneyClick,
                        movieTitles = uiState.homeSections
                            .flatMap { it.items }
                            .map { it.title }
                            .distinct()
                            .take(8)
                    )
                }

                // ── Top Navigation Tabs: Home / Shows / Movies / Anime ──
                item {
                    TopNavTabs(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }

                // ── Banner ──
                item {
                    SquareHeroBanner(
                        banner = if (uiState.tabBanners.isNotEmpty()) uiState.tabBanners[currentBannerIndex.coerceIn(0, (uiState.tabBanners.size - 1).coerceAtLeast(0))] else null,
                        onBannerClick = { contentId -> onMovieClick(contentId) },
                        onPlayClick = { contentId -> onPlayClick(contentId, null) },
                        onAddToWatchlist = { contentId -> onAddToWatchlist?.invoke(contentId) ?: viewModel.addToWatchlist(contentId) },
                        bannerCount = uiState.tabBanners.size,
                        currentIndex = currentBannerIndex.coerceIn(0, (uiState.tabBanners.size - 1).coerceAtLeast(0)),
                    )
                }

                // ── Content based on selected tab ──
                if (uiState.isTabLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = CineVaultTheme.colors.accentGold,
                                strokeWidth = LocalAppDimens.current.strokeWidth
                            )
                        }
                    }
                } else {
                    val sectionsToShow = if (uiState.selectedTab == 0) uiState.homeSections else uiState.tabSections

                    if (sectionsToShow.isEmpty() && uiState.tabBanners.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No content available",
                                    color = CineVaultTheme.colors.textSecondary,
                                    fontSize = LocalAppDimens.current.font16
                                )
                            }
                        }
                    } else {
                        itemsIndexed(sectionsToShow) { _, section ->
                            // Mid Banner: standalone 16:9 clickable banner, no section header
                            if (section.type == "mid_banner" && section.bannerImageUrl != null) {
                                val targetId = section.contentId ?: section.items.firstOrNull()?.id
                                val d = LocalAppDimens.current
                            Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = d.pad16, vertical = d.pad10)
                                        .aspectRatio(18f / 9f)
                                        .clip(RoundedCornerShape(d.radius16))
                                        .background(CineVaultTheme.colors.surface)
                                        .then(
                                            if (targetId != null) Modifier.clickable { onMovieClick(targetId) }
                                            else Modifier
                                        )
                                ) {
                                    AsyncImage(
                                        model = section.bannerImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else {
                            Column {
                                PremiumSectionHeader(
                                    title = section.title,
                                    onArrowClick = if (section.showViewMore) {
                                        { onSectionClick?.invoke(section) }
                                    } else null,
                                    isPremiumOnly = section.isPremiumOnly,
                                )
                                if (section.type == "trending") {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad20),
                                        horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad8),
                                    ) {
                                        itemsIndexed(section.items.take(10)) { index, movie ->
                                            TrendingMovieCard(
                                                movie = movie,
                                                rank = index + 1,
                                                onClick = onMovieClick,
                                            )
                                        }
                                    }
                                } else if (section.type == "upcoming") {
                                    UpcomingSection(
                                        movies = section.items,
                                        onMovieClick = onMovieClick,
                                        onAddToList = { movieId -> onAddToWatchlist?.invoke(movieId) ?: viewModel.addToWatchlist(movieId) },
                                        onRemoveFromList = { movieId -> viewModel.removeFromWatchlist(movieId) },
                                    )
                                } else if (section.type == "large_card" || section.cardSize == "large") {
                                    HorizontalMovieRow(
                                        movies = section.items,
                                        onMovieClick = onMovieClick,
                                        cardWidth = LocalAppDimens.current.cardLargeW,
                                        cardSpacing = LocalAppDimens.current.pad14,
                                    )
                                } else if (section.cardSize == "medium") {
                                    HorizontalMovieRow(
                                        movies = section.items,
                                        onMovieClick = onMovieClick,
                                        cardWidth = LocalAppDimens.current.cardMediumW,
                                        cardSpacing = LocalAppDimens.current.pad12,
                                    )
                                } else {
                                    HorizontalMovieRow(
                                        movies = section.items,
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }
                            } // end else (non-mid_banner)
                        }
                    }
                }

                // Bottom spacer for nav bar
                item {
                    Spacer(modifier = Modifier.height(LocalAppDimens.current.bottomNavSpacer))
                }
            }
        }

        // Loading
        if (uiState.isLoading && uiState.homeSections.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = CineVaultTheme.colors.accentGold,
                strokeWidth = LocalAppDimens.current.strokeWidth
            )
        }

        // Error State
        if (!uiState.isLoading && uiState.homeSections.isEmpty() && uiState.tabSections.isEmpty() && uiState.error != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(LocalAppDimens.current.pad32),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad16)
            ) {
                Text(
                    text = "Connection Failed",
                    fontSize = LocalAppDimens.current.font20,
                    fontWeight = FontWeight.Bold,
                    color = CineVaultTheme.colors.textPrimary
                )
                Text(
                    text = "Unable to connect to server.\nPlease check your network and try again.",
                    fontSize = LocalAppDimens.current.font14,
                    color = CineVaultTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { viewModel.refresh() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CineVaultTheme.colors.accentGold
                    ),
                    shape = RoundedCornerShape(LocalAppDimens.current.radius8)
                ) {
                    Text("Retry", color = CineVaultTheme.colors.background)
                }
            }
        }
        // ── Floating Continue Watching Popup ──
        val lastWatched = uiState.continueWatching.firstOrNull()
        if (uiState.showContinuePopup && lastWatched != null) {
            var popupVisible by remember { mutableStateOf(true) }

            // Auto-hide after 10 seconds
            LaunchedEffect(Unit) {
                delay(10000)
                popupVisible = false
                viewModel.dismissContinuePopup()
            }

            AnimatedVisibility(
                visible = popupVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LocalAppDimens.current.pad16),
                    shape = RoundedCornerShape(LocalAppDimens.current.radius16),
                    color = CineVaultTheme.colors.surfaceElevated,
                    shadowElevation = 12.dp,
                    tonalElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(LocalAppDimens.current.pad12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Thumbnail
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(LocalAppDimens.current.radius10))
                                .background(CineVaultTheme.colors.surface)
                        ) {
                            AsyncImage(
                                model = lastWatched.thumbnailUrl,
                                contentDescription = lastWatched.contentTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }

                        Spacer(Modifier.width(LocalAppDimens.current.pad12))

                        // Title
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Continue Watching",
                                fontSize = LocalAppDimens.current.font10,
                                fontWeight = FontWeight.Medium,
                                color = CineVaultTheme.colors.textSecondary,
                            )
                            Text(
                                lastWatched.contentTitle ?: "Unknown",
                                fontSize = LocalAppDimens.current.font14,
                                fontWeight = FontWeight.Bold,
                                color = CineVaultTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(Modifier.width(LocalAppDimens.current.pad8))

                        // Continue button
                        Button(
                            onClick = {
                                if (lastWatched.contentType == "episode" && lastWatched.seriesId != null) {
                                    onPlayClick(lastWatched.seriesId!!, lastWatched.contentId)
                                } else {
                                    onPlayClick(lastWatched.contentId, null)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CineVaultTheme.colors.accentGold,
                            ),
                            shape = RoundedCornerShape(LocalAppDimens.current.radius10),
                            contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad14, vertical = LocalAppDimens.current.pad6),
                        ) {
                            Text(
                                "Continue \u25B6",
                                fontSize = LocalAppDimens.current.font12,
                                fontWeight = FontWeight.Bold,
                                color = CineVaultTheme.colors.background,
                            )
                        }

                        Spacer(Modifier.width(LocalAppDimens.current.pad4))

                        // X dismiss button
                        IconButton(
                            onClick = {
                                popupVisible = false
                                viewModel.dismissContinuePopup()
                            },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = CineVaultTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SEARCH BAR + BELL ICON (side by side)
// ═══════════════════════════════════════════════════════════════

@Composable
fun SearchBarWithBell(
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onEarnMoneyClick: () -> Unit = {},
    movieTitles: List<String>,
) {
    val placeholders = if (movieTitles.isNotEmpty()) movieTitles
    else listOf("Search movies, shows...", "Action movies", "Thriller series", "Comedy")

    var currentPlaceholderIndex by remember { mutableIntStateOf(0) }
    val infiniteTransition = rememberInfiniteTransition(label = "search")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0
                1f at 500
                1f at 2500
                0f at 3000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "searchAlpha"
    )

    LaunchedEffect(placeholders.size) {
        currentPlaceholderIndex = 0
        while (true) {
            delay(3000)
            if (placeholders.size > 1) {
                currentPlaceholderIndex = (currentPlaceholderIndex + 1) % placeholders.size
            }
        }
    }

    val safeIndex = currentPlaceholderIndex.coerceIn(0, (placeholders.size - 1).coerceAtLeast(0))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.padTiny),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad10)
    ) {
        // Search bar (takes remaining space)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(LocalAppDimens.current.radius22))
                .background(CineVaultTheme.colors.surface)
                .border(
                    width = 1.dp,
                    color = CineVaultTheme.colors.borderSubtle,
                    shape = RoundedCornerShape(LocalAppDimens.current.radius22)
                )
                .clickable(onClick = onSearchClick),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = LocalAppDimens.current.pad14),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad10)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = CineVaultTheme.colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = placeholders[safeIndex],
                    color = CineVaultTheme.colors.textMuted.copy(alpha = alpha),
                    fontSize = LocalAppDimens.current.font14,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bell icon
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = CineVaultTheme.colors.surface,
            onClick = onNotificationsClick,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = CineVaultTheme.colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Earn Money animated button
        val earnPulse = rememberInfiniteTransition(label = "earn")
        val earnScale by earnPulse.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "earnScale",
        )
        Surface(
            modifier = Modifier
                .height(44.dp)
                .graphicsLayer { scaleX = earnScale; scaleY = earnScale },
            shape = RoundedCornerShape(LocalAppDimens.current.radius22),
            color = Color.Transparent,
            onClick = onEarnMoneyClick,
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFC5A44E),
                                Color(0xFFF2D078),
                                Color(0xFFC5A44E),
                            ),
                        ),
                        RoundedCornerShape(LocalAppDimens.current.radius22),
                    )
                    .padding(horizontal = LocalAppDimens.current.pad12),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\uD83E\uDE99", fontSize = LocalAppDimens.current.font14)
                    Spacer(Modifier.width(LocalAppDimens.current.pad4))
                    Text(
                        "Earn",
                        fontSize = LocalAppDimens.current.font12,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A1200),
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TOP NAVIGATION TABS - Home / Shows / Movies / Anime
// ═══════════════════════════════════════════════════════════════

@Composable
fun TopNavTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val tabs = listOf("Home", "Shows", "Movie", "Anime")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalAppDimens.current.pad16)
            .padding(bottom = LocalAppDimens.current.pad4),
        horizontalArrangement = Arrangement.Center,
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedTab
            val animatedScale by animateFloatAsState(
                targetValue = if (isSelected) 1.18f else 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
                label = "tabScale$index"
            )
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.6f,
                animationSpec = tween(250),
                label = "tabAlpha$index"
            )
            val underlineWidth by animateDpAsState(
                targetValue = if (isSelected) 28.dp else 0.dp,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                label = "underline$index"
            )

            Column(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onTabSelected(index) }
                    .padding(horizontal = LocalAppDimens.current.pad14, vertical = LocalAppDimens.current.pad6)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    fontSize = LocalAppDimens.current.font22,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isSelected) CineVaultTheme.colors.textPrimary.copy(alpha = animatedAlpha)
                    else CineVaultTheme.colors.textSecondary.copy(alpha = animatedAlpha),
                )
                Spacer(modifier = Modifier.height(LocalAppDimens.current.pad4))
                Box(
                    modifier = Modifier
                        .width(underlineWidth)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isSelected) CineVaultTheme.colors.accentGold
                            else Color.Transparent
                        )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HERO BANNER - cinematic style with title, info & action buttons
// ═══════════════════════════════════════════════════════════════

@Composable
fun SquareHeroBanner(
    banner: BannerDto?,
    onBannerClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    onAddToWatchlist: (String) -> Unit,
    bannerCount: Int,
    currentIndex: Int,
) {
    if (banner == null) return

    val movieId = banner.contentIdString ?: banner.id

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LocalAppDimens.current.padTiny)
    ) {
        // Banner container — wider aspect ratio for cinematic feel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .background(CineVaultTheme.colors.surface)
                .clickable { onBannerClick(movieId) },
        ) {
            // Banner image
            AsyncImage(
                model = banner.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // Top fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                CineVaultTheme.colors.background,
                                CineVaultTheme.colors.background.copy(alpha = 0.3f),
                                Color.Transparent,
                            )
                        )
                    )
            )

            // Bottom gradient — taller for text readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.50f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                CineVaultTheme.colors.background.copy(alpha = 0.6f),
                                CineVaultTheme.colors.background.copy(alpha = 0.9f),
                                CineVaultTheme.colors.background,
                            )
                        )
                    )
            )

            // Left fade
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                CineVaultTheme.colors.background.copy(alpha = 0.6f),
                                Color.Transparent,
                            )
                        )
                    )
            )

            // Right fade
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                CineVaultTheme.colors.background.copy(alpha = 0.6f),
                            )
                        )
                    )
            )

            // ── Bottom content: Title + Info + Buttons ──
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = LocalAppDimens.current.pad20)
                    .padding(bottom = LocalAppDimens.current.pad32),
            ) {
                // Title — always show as text
                if (banner.title != null) {
                    Text(
                        text = banner.title.uppercase(),
                        fontSize = LocalAppDimens.current.font26,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(LocalAppDimens.current.pad6))
                }

                // Movie info line: ⭐ 8.5 · 2020 · TV SERIES
                val infoParts = mutableListOf<String>()
                banner.starRating?.let { infoParts.add("%.1f".format(it)) }
                banner.releaseYear?.let { infoParts.add(it.toString()) }
                banner.contentType?.let {
                    infoParts.add(it.replace("_", " ").uppercase())
                }
                if (infoParts.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = CineVaultTheme.colors.accentGold,
                        )
                        Spacer(modifier = Modifier.width(LocalAppDimens.current.pad4))
                        Text(
                            text = infoParts.joinToString("  •  "),
                            fontSize = LocalAppDimens.current.font12,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                    Spacer(modifier = Modifier.height(LocalAppDimens.current.pad6))
                }

                // Subtitle / tagline
                val infoText = banner.subtitle ?: banner.tagline
                if (infoText != null) {
                    Text(
                        text = infoText,
                        fontSize = LocalAppDimens.current.font13,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = LocalAppDimens.current.lineHeight18,
                    )
                    Spacer(modifier = Modifier.height(LocalAppDimens.current.pad6))
                }

                // Genre tags
                if (!banner.genreTags.isNullOrEmpty()) {
                    Text(
                        text = banner.genreTags.joinToString("  •  "),
                        fontSize = LocalAppDimens.current.font11,
                        fontWeight = FontWeight.Medium,
                        color = CineVaultTheme.colors.accentGold.copy(alpha = 0.9f),
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(LocalAppDimens.current.pad14))
                } else {
                    Spacer(modifier = Modifier.height(LocalAppDimens.current.pad10))
                }

                // ── Action buttons ──
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad10),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Play button
                    Button(
                        onClick = { onPlayClick(movieId) },
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(LocalAppDimens.current.radius6),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad20),
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(22.dp),
                            tint = Color.Black,
                        )
                        Spacer(modifier = Modifier.width(LocalAppDimens.current.pad4))
                        Text(
                            "Play",
                            fontSize = LocalAppDimens.current.font15,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                    }

                    // My List button
                    OutlinedButton(
                        onClick = { onAddToWatchlist(movieId) },
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(LocalAppDimens.current.radius6),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                        ),
                        contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad16),
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "My List",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White,
                        )
                        Spacer(modifier = Modifier.width(LocalAppDimens.current.pad4))
                        Text(
                            "My List",
                            fontSize = LocalAppDimens.current.font14,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }

            // ── Page indicator dots ──
            if (bannerCount > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = LocalAppDimens.current.pad8),
                    horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad6)
                ) {
                    repeat(bannerCount.coerceAtMost(5)) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentIndex) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentIndex) CineVaultTheme.colors.accentGold
                                    else CineVaultTheme.colors.textMuted.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// FILTERED CONTENT GRID - for Shows/Movies/Anime tabs
// ═══════════════════════════════════════════════════════════════

@Composable
fun FilteredContentGrid(
    movies: List<MovieDto>,
    onMovieClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalAppDimens.current.pad16)
            .padding(top = LocalAppDimens.current.pad8),
        verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad12)
    ) {
        movies.chunked(3).forEach { rowMovies ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad10)
            ) {
                rowMovies.forEach { movie ->
                    Box(modifier = Modifier.weight(1f)) {
                        PremiumMovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie.id) },
                        )
                    }
                }
                // Fill empty slots
                repeat(3 - rowMovies.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM SECTION HEADER - with underline & arrow
// ═══════════════════════════════════════════════════════════════

@Composable
fun PremiumSectionHeader(
    title: String,
    onArrowClick: (() -> Unit)?,
    isPremiumOnly: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalAppDimens.current.pad20)
            .padding(top = LocalAppDimens.current.pad20, bottom = LocalAppDimens.current.pad10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Title with underline
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPremiumOnly) {
                    Image(
                        painter = painterResource(R.drawable.premium_badge_small),
                        contentDescription = "Premium",
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(LocalAppDimens.current.pad6))
                }
                Text(
                    text = title,
                    fontSize = LocalAppDimens.current.font18,
                    fontWeight = FontWeight.Bold,
                    color = if (isPremiumOnly) Color(0xFFFFD700) else CineVaultTheme.colors.textPrimary,
                    letterSpacing = 0.5.sp,
                )
            }
            Spacer(modifier = Modifier.height(LocalAppDimens.current.pad4))
            // Accent underline
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CineVaultTheme.colors.accentGold)
            )
        }

        // Arrow button
        if (onArrowClick != null) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = CineVaultTheme.colors.surface,
                onClick = onArrowClick,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "View all",
                        tint = CineVaultTheme.colors.accentGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HORIZONTAL MOVIE ROW
// ═══════════════════════════════════════════════════════════════

@Composable
fun HorizontalMovieRow(
    movies: List<MovieDto>,
    onMovieClick: (String) -> Unit,
    cardWidth: Dp = 115.dp,
    cardSpacing: Dp = 12.dp,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad20),
        horizontalArrangement = Arrangement.spacedBy(cardSpacing)
    ) {
        items(movies) { movie ->
            PremiumMovieCard(
                movie = movie,
                onClick = { onMovieClick(movie.id) },
                cardWidth = cardWidth,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM MOVIE CARD
// ═══════════════════════════════════════════════════════════════

@Composable
fun PremiumMovieCard(
    movie: MovieDto,
    onClick: () -> Unit,
    cardWidth: Dp = 115.dp,
) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(LocalAppDimens.current.radius10))
                .background(CineVaultTheme.colors.surface)
        ) {
            AsyncImage(
                model = movie.posterUrl ?: movie.bannerUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                            )
                        )
                    )
            )

            // Language label — top-right
            val langLabel = movie.languageLabel
            if (!langLabel.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(LocalAppDimens.current.pad6),
                    shape = RoundedCornerShape(LocalAppDimens.current.radius4),
                    color = Color.Black.copy(alpha = 0.6f),
                ) {
                    Text(
                        langLabel,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = LocalAppDimens.current.padTiny),
                        fontSize = LocalAppDimens.current.font10,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 0.3.sp,
                    )
                }
            }

            // Video quality — bottom-left
            if (!movie.videoQuality.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(LocalAppDimens.current.pad6),
                    shape = RoundedCornerShape(LocalAppDimens.current.radius4),
                    color = Color.White.copy(alpha = 0.2f),
                ) {
                    Text(
                        movie.videoQuality!!,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = LocalAppDimens.current.padTiny),
                        fontSize = LocalAppDimens.current.font8,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }

            // Star rating — bottom-right
            val displayRating = movie.starRating ?: movie.rating
            if (displayRating != null && displayRating > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(LocalAppDimens.current.pad6),
                    shape = RoundedCornerShape(LocalAppDimens.current.radius4),
                    color = CineVaultTheme.colors.background.copy(alpha = 0.85f),
                ) {
                    Text(
                        String.format("%.1f", displayRating),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        fontSize = LocalAppDimens.current.font11,
                        fontWeight = FontWeight.Bold,
                        color = CineVaultTheme.colors.ratingGold,
                    )
                }
            }

            // Premium badge — top-left
            if (movie.isEffectivelyPremium) {
                Image(
                    painter = painterResource(R.drawable.premium_badge_small),
                    contentDescription = "Premium",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(LocalAppDimens.current.pad4)
                        .size(36.dp),
                )
            }
        }
        Spacer(Modifier.height(LocalAppDimens.current.pad8))
        Text(
            movie.title,
            fontSize = LocalAppDimens.current.font13,
            fontWeight = FontWeight.SemiBold,
            color = CineVaultTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// UPCOMING SECTION - each card has its own date header + animated Add to List
// ═══════════════════════════════════════════════════════════════

@Composable
fun UpcomingSection(
    movies: List<MovieDto>,
    onMovieClick: (String) -> Unit,
    onAddToList: (String) -> Unit,
    onRemoveFromList: (String) -> Unit,
) {
    if (movies.isEmpty()) return

    // ── Local filter state ──
    var selectedPlatform by remember { mutableStateOf<String?>(null) }
    var selectedContentType by remember { mutableStateOf<String?>(null) }

    // Derive available platforms from the movie list
    val availablePlatforms = remember(movies) {
        movies.mapNotNull { it.platformOrigin?.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    // Content types present in the list
    val contentTypeEntries = listOf(
        null to "All",
        "movie" to "Movies",
        "web_series" to "TV Shows",
        "anime" to "Anime",
    )

    // Apply filters
    val filtered = remember(movies, selectedPlatform, selectedContentType) {
        movies
            .filter { selectedPlatform == null || it.platformOrigin?.trim() == selectedPlatform }
            .filter { selectedContentType == null || it.contentType == selectedContentType }
            .sortedBy { it.releaseDate ?: "9999" }
    }

    Column {
        // ── Content Type Chips ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad16),
            horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad8),
            modifier = Modifier.padding(bottom = LocalAppDimens.current.pad6),
        ) {
            items(contentTypeEntries, key = { it.first ?: "all" }) { (type, label) ->
                val selected = selectedContentType == type
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(LocalAppDimens.current.radius8))
                        .background(
                            if (selected) CineVaultTheme.colors.accentGold.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) CineVaultTheme.colors.accentGold.copy(alpha = 0.6f)
                            else Color(0xFF3A3A3A),
                            shape = RoundedCornerShape(LocalAppDimens.current.radius8),
                        )
                        .clickable { selectedContentType = if (selected && type != null) null else type }
                        .padding(horizontal = LocalAppDimens.current.pad12),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = LocalAppDimens.current.font11,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) CineVaultTheme.colors.accentGold
                        else CineVaultTheme.colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }

        // ── OTT Platform Chips (only if platforms are present in data) ──
        if (availablePlatforms.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad16),
                horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad8),
                modifier = Modifier.padding(bottom = LocalAppDimens.current.pad8),
            ) {
                // "All" chip
                item {
                    val selected = selectedPlatform == null
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(LocalAppDimens.current.radius8))
                            .background(
                                if (selected) CineVaultTheme.colors.accentGold.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) CineVaultTheme.colors.accentGold.copy(alpha = 0.6f)
                                else Color(0xFF3A3A3A),
                                shape = RoundedCornerShape(LocalAppDimens.current.radius8),
                            )
                            .clickable { selectedPlatform = null }
                            .padding(horizontal = LocalAppDimens.current.pad12),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "All OTT",
                            fontSize = LocalAppDimens.current.font11,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) CineVaultTheme.colors.accentGold
                            else CineVaultTheme.colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                }
                items(availablePlatforms, key = { it }) { platform ->
                    val selected = selectedPlatform == platform
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(LocalAppDimens.current.radius8))
                            .background(
                                if (selected) Color(0xFF3A3A3A).copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) CineVaultTheme.colors.accentGold.copy(alpha = 0.7f)
                                else Color(0xFF3A3A3A),
                                shape = RoundedCornerShape(LocalAppDimens.current.radius8),
                            )
                            .clickable { selectedPlatform = if (selected) null else platform }
                            .padding(horizontal = LocalAppDimens.current.pad12),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = platform,
                            fontSize = LocalAppDimens.current.font11,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) CineVaultTheme.colors.accentGold
                            else CineVaultTheme.colors.textPrimary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // ── Filtered empty state ──
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalAppDimens.current.pad20, vertical = LocalAppDimens.current.pad16),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No upcoming titles for selected filters",
                    fontSize = LocalAppDimens.current.font12,
                    color = CineVaultTheme.colors.textSecondary,
                )
            }
        } else {
            // ── Content Row ──
            LazyRow(
                contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad20),
                horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad12),
            ) {
                items(filtered) { movie ->
                    UpcomingMovieCard(
                        movie = movie,
                        onClick = { onMovieClick(movie.id) },
                        onAddToList = { onAddToList(movie.id) },
                        onRemoveFromList = { onRemoveFromList(movie.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun UpcomingMovieCard(
    movie: MovieDto,
    onClick: () -> Unit,
    onAddToList: () -> Unit,
    onRemoveFromList: () -> Unit,
) {
    // Track added state for animation (toggle)
    var isAdded by remember { mutableStateOf(false) }

    // Format date label
    val dateLabel = remember(movie.releaseDate) {
        movie.releaseDate?.let {
            try {
                val parts = it.take(10).split("-")
                if (parts.size >= 3) {
                    val monthNum = parts[1].toIntOrNull() ?: 0
                    val day = parts[2].toIntOrNull() ?: 0
                    val month = when (monthNum) {
                        1 -> "JAN"; 2 -> "FEB"; 3 -> "MAR"; 4 -> "APR"
                        5 -> "MAY"; 6 -> "JUN"; 7 -> "JUL"; 8 -> "AUG"
                        9 -> "SEP"; 10 -> "OCT"; 11 -> "NOV"; 12 -> "DEC"
                        else -> "???"
                    }
                    "$month . ${String.format("%02d", day)}"
                } else "TBA"
            } catch (_: Exception) { "TBA" }
        } ?: "TBA"
    }

    Column(
        modifier = Modifier.width(115.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Date header with dashed line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = LocalAppDimens.current.pad6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dateLabel,
                fontSize = LocalAppDimens.current.font11,
                fontWeight = FontWeight.Bold,
                color = CineVaultTheme.colors.textSecondary.copy(alpha = 0.6f),
                letterSpacing = 0.8.sp,
                maxLines = 1,
            )
            Spacer(Modifier.width(LocalAppDimens.current.pad4))
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
            ) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.35f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.5f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(6f, 4f), 0f
                    )
                )
            }
        }

        // Poster
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(LocalAppDimens.current.radius10))
                .background(CineVaultTheme.colors.surface)
                .clickable(onClick = onClick)
        ) {
            AsyncImage(
                model = movie.posterUrl ?: movie.bannerUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(Modifier.height(LocalAppDimens.current.pad6))

        // Title
        Text(
            movie.title,
            fontSize = LocalAppDimens.current.font12,
            fontWeight = FontWeight.SemiBold,
            color = CineVaultTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(LocalAppDimens.current.pad4))

        // + List / ✓ Added button with animation
        val buttonColor by animateColorAsState(
            targetValue = if (isAdded) CineVaultTheme.colors.accentGold.copy(alpha = 0.15f) else Color.Transparent,
            animationSpec = tween(250),
            label = "addedBg"
        )
        val borderColor by animateColorAsState(
            targetValue = if (isAdded) CineVaultTheme.colors.accentGold else CineVaultTheme.colors.accentGold.copy(alpha = 0.6f),
            animationSpec = tween(250),
            label = "addedBorder"
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            shape = RoundedCornerShape(LocalAppDimens.current.radius16),
            color = buttonColor,
            border = BorderStroke(1.dp, borderColor),
            onClick = {
                isAdded = !isAdded
                if (isAdded) {
                    onAddToList()
                } else {
                    onRemoveFromList()
                }
            },
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(
                    targetState = isAdded,
                    transitionSpec = {
                        (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.7f))
                            .togetherWith(fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.7f))
                    },
                    label = "addIcon"
                ) { added ->
                    if (added) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "✓",
                                fontSize = LocalAppDimens.current.font12,
                                fontWeight = FontWeight.Bold,
                                color = CineVaultTheme.colors.accentGold,
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "Added",
                                fontSize = LocalAppDimens.current.font10,
                                fontWeight = FontWeight.SemiBold,
                                color = CineVaultTheme.colors.accentGold,
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "+",
                                fontSize = LocalAppDimens.current.font13,
                                fontWeight = FontWeight.Bold,
                                color = CineVaultTheme.colors.accentGold,
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "List",
                                fontSize = LocalAppDimens.current.font11,
                                fontWeight = FontWeight.Medium,
                                color = CineVaultTheme.colors.accentGold,
                            )
                        }
                    }
                }
            }
        }
    }
}
