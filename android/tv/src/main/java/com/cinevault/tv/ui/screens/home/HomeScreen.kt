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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val d = LocalTvDimens.current
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
            contentPadding = PaddingValues(bottom = d.padSection),
        ) {
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
                            .height(d.bannerHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading...", color = TvOnSurfaceVariant, fontSize = d.fontLarge)
                    }
                }
            } else {
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

                if (state.continueWatching.isNotEmpty()) {
                    item {
                        ContinueWatchingRow(
                            items = state.continueWatching,
                            onItemClick = { item -> onMovieClick(item.contentId) },
                        )
                    }
                }

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
    val d = LocalTvDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = d.screenPadH, vertical = d.padLarge),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "VELORA",
            fontSize = d.fontTitle,
            fontWeight = FontWeight.ExtraBold,
            color = TvPrimary,
            letterSpacing = d.fontSmall * 0.25f,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(d.padSmall),
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
                        fontSize = if (isSelected) d.fontMedium else d.fontBody,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = d.padLarge, vertical = d.padSmall),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(d.padMedium)) {
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
                    modifier = Modifier.padding(d.padMedium).size(d.iconMedium),
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
                    modifier = Modifier.padding(d.padMedium).size(d.iconMedium),
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
    val d = LocalTvDimens.current
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
            .height(d.bannerHeight)
            .padding(horizontal = d.screenPadH, vertical = d.padSmall),
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

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(d.padXXL)
                        .fillMaxWidth(0.5f),
                ) {
                    banner.title?.let { title ->
                        Text(
                            text = title,
                            fontSize = d.fontTitle,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(d.padSmall))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(d.padMedium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        banner.starRating?.let { rating ->
                            Text(
                                text = "\u2B50 ${String.format("%.1f", rating)}",
                                fontSize = d.fontBody,
                                fontWeight = FontWeight.Medium,
                                color = TvRatingGold,
                            )
                        }
                        banner.releaseYear?.let { year ->
                            Text(text = year.toString(), fontSize = d.fontBody, color = TvOnSurfaceVariant)
                        }
                        banner.contentType?.let { type ->
                            Text(
                                text = type.uppercase().replace("_", " "),
                                fontSize = d.fontSmall,
                                fontWeight = FontWeight.Medium,
                                color = TvOnSurfaceVariant,
                            )
                        }
                    }

                    banner.genreTags?.take(3)?.let { tags ->
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(d.padSmall))
                            Text(
                                text = tags.joinToString(" \u2022 ") { it.uppercase() },
                                fontSize = d.fontSmall,
                                fontWeight = FontWeight.Medium,
                                color = TvPrimary,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = d.padLarge),
                    horizontalArrangement = Arrangement.spacedBy(d.padSmall),
                ) {
                    banners.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (index == currentIndex) d.padXL else d.padSmall,
                                    height = d.padSmall * 0.75f,
                                )
                                .clip(RoundedCornerShape(d.padTiny))
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
    val d = LocalTvDimens.current
    Column(modifier = Modifier.padding(top = d.padXL)) {
        Text(
            text = "\u25B6\uFE0F Continue Watching",
            fontSize = d.fontLarge,
            fontWeight = FontWeight.SemiBold,
            color = TvOnSurface,
            modifier = Modifier.padding(horizontal = d.screenPadH, vertical = d.padSmall),
        )
        TvLazyRow(
            contentPadding = PaddingValues(horizontal = d.screenPadH),
            horizontalArrangement = Arrangement.spacedBy(d.padLarge),
        ) {
            items(items, key = { "${it.contentId}_${it.id}" }) { item ->
                Surface(
                    onClick = { onItemClick(item) },
                    modifier = Modifier
                        .width(d.continueCardW)
                        .height(d.continueCardH),
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
                                .fillMaxHeight(0.45f)
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
                                .padding(d.padSmall)
                        ) {
                            Text(
                                text = item.contentTitle ?: "Untitled",
                                fontSize = d.fontSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            item.episodeTitle?.let {
                                Text(
                                    text = it,
                                    fontSize = d.fontXSmall,
                                    color = TvOnSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                        val progress = if (item.totalDuration > 0)
                            item.currentTime.toFloat() / item.totalDuration else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(d.padTiny)
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
    val d = LocalTvDimens.current
    val contentId = section.contentId ?: return
    val imageUrl = section.bannerImageUrl ?: return

    Box(modifier = Modifier.padding(horizontal = d.screenPadH, vertical = d.padLarge)) {
        Surface(
            onClick = { onMovieClick(contentId) },
            modifier = Modifier
                .fillMaxWidth()
                .height(d.midBannerHeight),
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
                    fontSize = d.fontXXL,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = d.padXXL),
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
    val d = LocalTvDimens.current

    Column(modifier = Modifier.padding(top = d.padXL)) {
        Text(
            text = title,
            fontSize = d.fontLarge,
            fontWeight = FontWeight.SemiBold,
            color = TvOnSurface,
            modifier = Modifier.padding(horizontal = d.screenPadH, vertical = d.padSmall),
        )

        TvLazyRow(
            contentPadding = PaddingValues(horizontal = d.screenPadH),
            horizontalArrangement = Arrangement.spacedBy(d.padLarge),
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
    val d = LocalTvDimens.current
    val cardWidth = if (isLarge) d.movieCardLargeW else d.movieCardW
    val cardHeight = if (isLarge) d.movieCardLargeH else d.movieCardH

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

            movie.languageLabel?.let { lang ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(d.padSmall)
                        .clip(RoundedCornerShape(d.padTiny))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = d.padTiny, vertical = d.padTiny / 2),
                ) {
                    Text(text = lang, fontSize = d.fontTiny, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }

            movie.videoQuality?.let { quality ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = d.padSmall, bottom = d.padXXL)
                        .clip(RoundedCornerShape(d.padTiny))
                        .background(TvPrimary.copy(alpha = 0.8f))
                        .padding(horizontal = d.padTiny, vertical = d.padTiny / 2),
                ) {
                    Text(text = quality, fontSize = d.fontTiny, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            val rating = movie.averageRating
            if (rating > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = d.padSmall, bottom = d.padXXL)
                        .clip(RoundedCornerShape(d.padTiny))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = d.padTiny, vertical = d.padTiny / 2),
                ) {
                    Text(
                        text = "\u2B50 ${String.format("%.1f", rating)}",
                        fontSize = d.fontXSmall,
                        fontWeight = FontWeight.Bold,
                        color = TvRatingGold,
                    )
                }
            }

            if (movie.isEffectivelyPremium) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(d.padSmall)
                        .clip(RoundedCornerShape(d.padTiny))
                        .background(
                            Brush.horizontalGradient(listOf(TvPrimary, TvPrimaryVariant))
                        )
                        .padding(horizontal = d.padSmall, vertical = d.padTiny),
                ) {
                    Text(text = "\uD83D\uDC51", fontSize = d.fontSmall)
                }
            }

            Text(
                text = movie.title,
                fontSize = d.fontSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(d.padSmall),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TrendingCard(movie: MovieDto, rank: Int, onClick: () -> Unit) {
    val d = LocalTvDimens.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = rank.toString(),
            fontSize = d.fontTrending,
            fontWeight = FontWeight.ExtraBold,
            color = TvPrimary.copy(alpha = 0.3f),
            modifier = Modifier.width(d.trendingRankW),
        )

        Surface(
            onClick = onClick,
            modifier = Modifier
                .width(d.trendingCardW)
                .height(d.trendingCardH),
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
                    fontSize = d.fontXSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(d.padSmall),
                )
            }
        }
    }
}
