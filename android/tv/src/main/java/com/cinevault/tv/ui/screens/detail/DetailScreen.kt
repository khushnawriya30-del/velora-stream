package com.cinevault.tv.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.cinevault.tv.data.model.EpisodeDto
import com.cinevault.tv.data.model.MovieDto
import com.cinevault.tv.data.model.SeasonDto
import com.cinevault.tv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailScreen(
    isPremium: Boolean,
    onPlayClick: (movieId: String, episodeId: String?, seasonId: String?) -> Unit,
    onMovieClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val movie = state.movie
    var showPremiumPopup by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
            .onPreviewKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyUp) {
                    if (showPremiumPopup) { showPremiumPopup = false; true }
                    else { onBack(); true }
                } else false
            },
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = TvTextMuted, fontSize = 20.sp)
            }
        } else if (movie != null) {
            TvLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp),
            ) {
                // Hero section with backdrop
                item {
                    HeroSection(
                        movie = movie,
                        isPremium = isPremium,
                        isInWatchlist = state.isInWatchlist,
                        onPlayClick = {
                            if (!isPremium && movie.isEffectivelyPremium) {
                                showPremiumPopup = true
                            } else {
                                onPlayClick(movie.id, null, null)
                            }
                        },
                        onWatchlistClick = { viewModel.toggleWatchlist() },
                        onBack = onBack,
                    )
                }

                // Synopsis
                if (!movie.synopsis.isNullOrBlank()) {
                    item {
                        Text(
                            text = movie.synopsis,
                            fontSize = 15.sp,
                            color = TvOnSurfaceVariant,
                            lineHeight = 22.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 48.dp, vertical = 12.dp),
                        )
                    }
                }

                // Cast
                if (!movie.cast.isNullOrEmpty()) {
                    item {
                        Text(
                            text = "Cast: ${movie.cast.take(5).joinToString(", ") { it.name }}",
                            fontSize = 14.sp,
                            color = TvTextMuted,
                            modifier = Modifier.padding(horizontal = 48.dp, vertical = 4.dp),
                        )
                    }
                }

                // Seasons & Episodes (for series/anime)
                if (state.seasons.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SeasonSelector(
                            seasons = state.seasons,
                            selectedIndex = state.selectedSeasonIndex,
                            onSelect = { viewModel.selectSeason(it) },
                        )
                    }

                    item {
                        EpisodesRow(
                            episodes = state.episodes,
                            isPremium = isPremium,
                            onEpisodeClick = { episode ->
                                val seasonId = state.seasons.getOrNull(state.selectedSeasonIndex)?.id
                                if (!isPremium && episode.isPremium) {
                                    showPremiumPopup = true
                                } else {
                                    onPlayClick(movie.id, episode.id, seasonId)
                                }
                            },
                        )
                    }
                }

                // Related content
                if (state.related.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "You May Also Like",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TvOnSurface,
                            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
                        )
                        TvLazyRow(
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(state.related, key = { it.id }) { relatedMovie ->
                                RelatedCard(movie = relatedMovie, onClick = { onMovieClick(relatedMovie.id) })
                            }
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Failed to load content", color = TvTextMuted, fontSize = 18.sp)
            }
        }

        // Premium popup overlay
        if (showPremiumPopup) {
            PremiumPopup(onDismiss = { showPremiumPopup = false })
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PremiumPopup(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .clip(RoundedCornerShape(16.dp))
                .background(TvSurface)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "\uD83D\uDC51", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Premium Content",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TvPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This content requires a Premium subscription.\nUpgrade from the mobile app to watch.",
                fontSize = 14.sp,
                color = TvOnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.colors(
                    containerColor = TvPrimary,
                    contentColor = TvOnPrimary,
                ),
            ) {
                Text(
                    text = "OK",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroSection(
    movie: MovieDto,
    isPremium: Boolean,
    isInWatchlist: Boolean,
    onPlayClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
    ) {
        AsyncImage(
            model = movie.bannerUrl ?: movie.posterUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            TvBackground.copy(alpha = 0.3f),
                            TvBackground.copy(alpha = 0.7f),
                            TvBackground,
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(TvBackground.copy(alpha = 0.85f), Color.Transparent),
                        startX = 0f,
                        endX = 800f,
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 32.dp, end = 48.dp),
        ) {
            // Info chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(text = movie.contentType.replaceFirstChar { it.uppercase() })
                movie.contentRating?.let { InfoChip(text = it) }
                movie.releaseYear?.let { InfoChip(text = it.toString()) }
                movie.duration?.let {
                    val hours = it / 60; val mins = it % 60
                    InfoChip(text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m")
                }
                if (movie.averageRating > 0) {
                    InfoChip(text = "\u2B50 ${String.format("%.1f", movie.averageRating)}", highlight = true)
                }
                movie.videoQuality?.let {
                    InfoChip(text = it, highlight = true)
                }
                if (movie.isEffectivelyPremium) {
                    InfoChip(text = "\uD83D\uDC51 PREMIUM", highlight = true)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = movie.title,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.5.sp,
            )

            movie.languageLabel?.let { lang ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = lang, fontSize = 12.sp, color = TvPrimary, fontWeight = FontWeight.Medium)
            }

            if (movie.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = movie.genres.joinToString(" \u2022 "),
                    fontSize = 14.sp,
                    color = TvOnSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Play button
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.colors(
                        containerColor = TvPrimary,
                        contentColor = TvOnPrimary,
                        focusedContainerColor = TvPrimaryVariant,
                    ),
                ) {
                    Text(
                        text = if (movie.contentType == "series" || movie.contentType == "anime")
                            "\u25B6  Play S1:E1" else "\u25B6  Play Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                // Watchlist button
                Button(
                    onClick = onWatchlistClick,
                    colors = ButtonDefaults.colors(
                        containerColor = if (isInWatchlist) TvPrimary.copy(alpha = 0.2f) else TvSurfaceVariant,
                        contentColor = if (isInWatchlist) TvPrimary else TvOnSurface,
                        focusedContainerColor = TvPrimary.copy(alpha = 0.3f),
                    ),
                ) {
                    Text(
                        text = if (isInWatchlist) "\u2713  In Watchlist" else "+  Watchlist",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }

                // Back button
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.colors(
                        containerColor = TvSurfaceVariant,
                        contentColor = TvOnSurface,
                        focusedContainerColor = TvBorderSubtle,
                    ),
                ) {
                    Text(
                        text = "\u2190 Back",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InfoChip(text: String, highlight: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (highlight) TvPrimary.copy(alpha = 0.2f)
                else TvSurfaceVariant.copy(alpha = 0.8f)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (highlight) TvPrimary else TvOnSurfaceVariant,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SeasonSelector(
    seasons: List<SeasonDto>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    TvLazyRow(
        contentPadding = PaddingValues(horizontal = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(seasons.size) { index ->
            val isSelected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                shape = ClickableSurfaceDefaults.shape(ChipShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isSelected) TvPrimary else TvSurfaceVariant,
                    focusedContainerColor = TvPrimary,
                    contentColor = if (isSelected) TvOnPrimary else TvOnSurfaceVariant,
                    focusedContentColor = TvOnPrimary,
                ),
            ) {
                Text(
                    text = "Season ${seasons[index].seasonNumber}",
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodesRow(
    episodes: List<EpisodeDto>,
    isPremium: Boolean,
    onEpisodeClick: (EpisodeDto) -> Unit,
) {
    if (episodes.isEmpty()) return

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = "Episodes",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TvOnSurface,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
        )

        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(episodes, key = { it.id }) { episode ->
                Surface(
                    onClick = { onEpisodeClick(episode) },
                    modifier = Modifier.width(220.dp).height(72.dp),
                    shape = ClickableSurfaceDefaults.shape(ChipShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = TvSurfaceVariant,
                        focusedContainerColor = TvCardFocused,
                    ),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(
                            border = BorderStroke(width = 2.dp, color = TvPrimary),
                            shape = ChipShape,
                        ),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "E${episode.episodeNumber}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvPrimary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = episode.title.ifBlank { "Episode ${episode.episodeNumber}" },
                                fontSize = 14.sp,
                                color = TvOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            episode.duration?.let {
                                Text(text = "${it}m", fontSize = 12.sp, color = TvTextMuted)
                            }
                        }
                        if (episode.isPremium) {
                            Text("\uD83D\uDC51", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RelatedCard(movie: MovieDto, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(140.dp).height(210.dp),
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            Text(
                text = movie.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
            if (movie.isEffectivelyPremium) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TvPrimary)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text("\uD83D\uDC51", fontSize = 10.sp)
                }
            }
        }
    }
}
