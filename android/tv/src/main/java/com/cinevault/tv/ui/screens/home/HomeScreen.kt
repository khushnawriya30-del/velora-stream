package com.cinevault.tv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
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
import com.cinevault.tv.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onMovieClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground),
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = TvDimText, fontSize = 20.sp)
            }
        } else {
            TvLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // Top bar
                item {
                    TopBar(onSearchClick = onSearchClick, onLogout = onLogout)
                }

                // Hero banner carousel
                if (state.banners.isNotEmpty()) {
                    item {
                        HeroBannerCarousel(
                            banners = state.banners,
                            onBannerClick = { bannerId ->
                                val banner = state.banners.find { it.id == bannerId }
                                banner?.movieId?.let { onMovieClick(it) }
                            }
                        )
                    }
                }

                // Trending row
                if (state.trending.isNotEmpty()) {
                    item {
                        ContentRow(
                            title = "🔥 Trending Now",
                            movies = state.trending,
                            onMovieClick = onMovieClick,
                        )
                    }
                }

                // Dynamic home sections
                items(state.sections) { section ->
                    ContentRow(
                        title = section.title,
                        movies = section.movies,
                        onMovieClick = onMovieClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopBar(onSearchClick: () -> Unit, onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "CineVault",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TvPrimary,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onSearchClick,
                colors = ButtonDefaults.colors(
                    containerColor = TvSurfaceVariant,
                    contentColor = TvOnSurface,
                    focusedContainerColor = TvPrimary,
                    focusedContentColor = TvOnPrimary,
                ),
            ) {
                Text("🔍 Search", fontSize = 14.sp)
            }

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.colors(
                    containerColor = TvSurfaceVariant,
                    contentColor = TvOnSurface,
                    focusedContainerColor = TvPrimary,
                    focusedContentColor = TvOnPrimary,
                ),
            ) {
                Text("Logout", fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroBannerCarousel(
    banners: List<BannerDto>,
    onBannerClick: (String) -> Unit,
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-scroll
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
            onClick = { onBannerClick(banner.id) },
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

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    TvBackground.copy(alpha = 0.8f),
                                    Color.Transparent,
                                )
                            )
                        )
                )

                // Banner title
                if (banner.title != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(32.dp),
                    ) {
                        Text(
                            text = banner.title ?: "",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Page indicators
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    banners.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (index == currentIndex) 24.dp else 8.dp,
                                    height = 8.dp,
                                )
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (index == currentIndex) TvPrimary else TvDimText.copy(alpha = 0.5f)
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
private fun ContentRow(
    title: String,
    movies: List<MovieDto>,
    onMovieClick: (String) -> Unit,
) {
    if (movies.isEmpty()) return

    Column(
        modifier = Modifier.padding(top = 24.dp),
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TvOnSurface,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
        )

        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(movies, key = { it.id }) { movie ->
                MovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MovieCard(movie: MovieDto, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(240.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(CardShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvSurface,
            focusedContainerColor = TvCardFocused,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = 2.dp, color = TvBorderFocused),
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

            // Gradient at bottom for title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Title
            Text(
                text = movie.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )

            // Premium badge
            if (movie.isPremium == true) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TvGold)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "PREMIUM",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                }
            }

            // Rating badge
            if (movie.rating != null && movie.rating > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "⭐ ${String.format("%.1f", movie.rating)}",
                        fontSize = 11.sp,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
