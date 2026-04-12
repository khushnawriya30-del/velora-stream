package com.cinevault.tv.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.cinevault.tv.data.model.BannerDto
import com.cinevault.tv.data.model.HomeSectionDto
import com.cinevault.tv.data.model.MovieDto
import com.cinevault.tv.data.model.WatchProgressDto
import com.cinevault.tv.ui.theme.*
import kotlinx.coroutines.delay

private val tabs = listOf("Home", "Shows", "Movies", "Anime")
private val tabKeys = listOf("home", "shows", "movies", "anime")

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    initialTab: String = "home",
    isPremium: Boolean,
    onMovieClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onLogout: () -> Unit,
    onTabChange: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(tabKeys.indexOf(initialTab).coerceAtLeast(0)) }

    LaunchedEffect(initialTab) {
        val idx = tabKeys.indexOf(initialTab).coerceAtLeast(0)
        selectedTabIndex = idx
        viewModel.loadContent(tabKeys[idx])
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground),
    ) {
        TvLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            // Top bar with tabs
            item {
                TopBar(
                    selectedTab = selectedTabIndex,
                    onTabSelected = { idx ->
                        selectedTabIndex = idx
                        viewModel.loadContent(tabKeys[idx])
                        onTabChange(tabKeys[idx])
                    },
                    onSearchClick = onSearchClick,
                    onLogout = onLogout,
                )
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading...", color = TvOnSurfaceVariant, fontSize = 18.sp)
                    }
                }
            } else {
                // Hero banner carousel
                if (state.banners.isNotEmpty()) {
                    item {
                        HeroBannerCarousel(
                            banners = state.banners,
                            onBannerClick = { banner ->
                                banner.contentIdString?.let { onMovieClick(it) }
                            }
                        )
                    }
                }

                // Continue watching row
                if (state.continueWatching.isNotEmpty()) {
                    item {
                        ContinueWatchingRow(
                            items = state.continueWatching,
                            onItemClick = { item -> onMovieClick(item.contentId) },
                        )
                    }
                }

                // Trending row
                if (state.trending.isNotEmpty()) {
                    item {
                        ContentRow(
                            title = "\uD83D\uDD25 Trending Now",
                            movies = state.trending,
                            showTrendingNumbers = true,
                            onMovieClick = onMovieClick,
                        )
                    }
                }

                // Dynamic home sections
                items(state.sections.size) { index ->
                    val section = state.sections[index]
                    if (section.type == "mid_banner" && section.bannerImageUrl != null) {
                        MidBannerRow(section = section, onMovieClick = onMovieClick)
                    } else {
                        ContentRow(
                            title = section.title,
                            movies = section.items,
                            isLargeCard = section.type == "large_card" || section.cardSize == "large",
                            onMovieClick = onMovieClick,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onLogout: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // App name
        Text(
            text = "VELORA",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TvPrimary,
            letterSpacing = 3.sp,
        )

        // Nav tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedTab
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = tween(200),
                    label = "tab_scale"
                )

                Surface(
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.scale(scale),
                    shape = ClickableSurfaceDefaults.shape(PillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) TvPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        focusedContainerColor = TvPrimary.copy(alpha = 0.25f),
                        contentColor = if (isSelected) TvPrimary else TvOnSurfaceVariant,
                        focusedContentColor = TvPrimary,
                    ),
                    border = ClickableSurfaceDefaults.border(
                        border = Border(
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 0.dp,
                                if (isSelected) TvPrimary else Color.Transparent
                            ),
                            shape = PillShape,
                        ),
                        focusedBorder = Border(
                            border = BorderStroke(1.5.dp, TvPrimary),
                            shape = PillShape,
                        ),
                    ),
                ) {
                    Text(
                        text = tab,
                        fontSize = if (isSelected) 16.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        // Right side actions
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                onClick = onSearchClick,
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = TvSurfaceVariant,
                    focusedContainerColor = TvPrimary,
                    contentColor = TvOnSurface,
                    focusedContentColor = TvOnPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.padding(12.dp).size(24.dp),
                )
            }

            Surface(
                onClick = onLogout,
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = TvSurfaceVariant,
                    focusedContainerColor = TvError,
                    contentColor = TvOnSurface,
                    focusedContentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Account / Logout",
                    modifier = Modifier.padding(12.dp).size(24.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroBannerCarousel(
    banners: List<BannerDto>,
    onBannerClick: (BannerDto) -> Unit,
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(banners.size) {
        while (true) {
            delay(6000)
            if (banners.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % banners.size
            }
        }
    }

    val banner = banners.getOrNull(currentIndex) ?: return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .padding(horizontal = 48.dp, vertical = 8.dp),
    ) {
        Surface(
            onClick = { onBannerClick(banner) },
            modifier = Modifier.fillMaxSize(),
            shape = ClickableSurfaceDefaults.shape(BannerShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = TvSurface,
                focusedContainerColor = TvSurface,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(width = 3.dp, color = TvPrimary),
                    shape = BannerShape,
                ),
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = banner.imageUrl,
                    contentDescription = banner.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Left gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    TvBackground.copy(alpha = 0.9f),
                                    TvBackground.copy(alpha = 0.4f),
                                    Color.Transparent,
                                )
                            )
                        )
                )

                // Bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, TvBackground.copy(alpha = 0.9f))
                            )
                        )
                )

                // Content overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(32.dp)
                        .fillMaxWidth(0.5f),
                ) {
                    banner.title?.let { title ->
                        Text(
                            text = title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            letterSpacing = 1.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Meta info row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        banner.starRating?.let { rating ->
                            Text(
                                text = "\u2B50 ${String.format("%.1f", rating)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TvRatingGold,
                            )
                        }
                        banner.releaseYear?.let { year ->
                            Text(
                                text = year.toString(),
                                fontSize = 14.sp,
                                color = TvOnSurfaceVariant,
                            )
                        }
                        banner.contentType?.let { type ->
                            Text(
                                text = type.uppercase().replace("_", " "),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TvOnSurfaceVariant,
                                letterSpacing = 0.8.sp,
                            )
                        }
                    }

                    // Genre tags
                    banner.genreTags?.take(3)?.let { tags ->
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = tags.joinToString(" \u2022 ") { it.uppercase() },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TvPrimary,
                                letterSpacing = 0.8.sp,
                            )
                        }
                    }
                }

                // Page indicators
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    banners.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (index == currentIndex) 24.dp else 8.dp,
                                    height = 6.dp,
                                )
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (index == currentIndex) TvPrimary else TvTextMuted.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ContinueWatchingRow(
    items: List<WatchProgressDto>,
    onItemClick: (WatchProgressDto) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = "\u25B6\uFE0F Continue Watching",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TvOnSurface,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
        )
        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(items, key = { "${it.contentId}_${it.id}" }) { item ->
                Surface(
                    onClick = { onItemClick(item) },
                    modifier = Modifier
                        .width(200.dp)
                        .height(130.dp),
                    shape = ClickableSurfaceDefaults.shape(CardShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = TvSurface,
                        focusedContainerColor = TvCardFocused,
                    ),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, TvBorderFocused),
                            shape = CardShape,
                        ),
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = item.contentTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = item.contentTitle ?: "Untitled",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            item.episodeTitle?.let {
                                Text(
                                    text = it,
                                    fontSize = 10.sp,
                                    color = TvOnSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                        // Progress bar
                        val progress = if (item.totalDuration > 0)
                            item.currentTime.toFloat() / item.totalDuration else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter)
                                .background(TvBorderSubtle)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(TvPrimary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MidBannerRow(section: HomeSectionDto, onMovieClick: (String) -> Unit) {
    val contentId = section.contentId ?: return
    val imageUrl = section.bannerImageUrl ?: return

    Box(modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp)) {
        Surface(
            onClick = { onMovieClick(contentId) },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = ClickableSurfaceDefaults.shape(BannerShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = TvSurface,
                focusedContainerColor = TvSurface,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, TvPrimary),
                    shape = BannerShape,
                ),
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = section.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(TvBackground.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                )
                Text(
                    text = section.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 32.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ContentRow(
    title: String,
    movies: List<MovieDto>,
    isLargeCard: Boolean = false,
    showTrendingNumbers: Boolean = false,
    onMovieClick: (String) -> Unit,
) {
    if (movies.isEmpty()) return

    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TvOnSurface,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
            letterSpacing = 0.5.sp,
        )

        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(movies.size) { index ->
                val movie = movies[index]
                if (showTrendingNumbers) {
                    TrendingCard(
                        movie = movie,
                        rank = index + 1,
                        onClick = { onMovieClick(movie.id) },
                    )
                } else {
                    MovieCard(
                        movie = movie,
                        isLarge = isLargeCard,
                        onClick = { onMovieClick(movie.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MovieCard(movie: MovieDto, isLarge: Boolean = false, onClick: () -> Unit) {
    val cardWidth = if (isLarge) 180.dp else 140.dp
    val cardHeight = if (isLarge) 270.dp else 210.dp

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight),
        shape = ClickableSurfaceDefaults.shape(CardShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvSurface,
            focusedContainerColor = TvCardFocused,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvBorderFocused),
                shape = CardShape,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                    .fillMaxHeight(0.4f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            )

            // Language badge - top right
            movie.languageLabel?.let { lang ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(text = lang, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }

            // Video quality badge - bottom left
            movie.videoQuality?.let { quality ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 6.dp, bottom = 28.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(TvPrimary.copy(alpha = 0.8f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(text = quality, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            // Rating - bottom right
            val rating = movie.averageRating
            if (rating > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "\u2B50 ${String.format("%.1f", rating)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvRatingGold,
                    )
                }
            }

            // Premium badge - top left
            if (movie.isEffectivelyPremium) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(TvPrimary, TvPrimaryVariant)
                            )
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(text = "\uD83D\uDC51", fontSize = 12.sp)
                }
            }

            // Title
            Text(
                text = movie.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TrendingCard(movie: MovieDto, rank: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Large rank number
        Text(
            text = rank.toString(),
            fontSize = 60.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TvPrimary.copy(alpha = 0.3f),
            modifier = Modifier.width(50.dp),
        )

        Surface(
            onClick = onClick,
            modifier = Modifier
                .width(120.dp)
                .height(180.dp),
            shape = ClickableSurfaceDefaults.shape(CardShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = TvSurface,
                focusedContainerColor = TvCardFocused,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, TvBorderFocused),
                    shape = CardShape,
                ),
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.35f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                )
                Text(
                    text = movie.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                )
            }
        }
    }
}
