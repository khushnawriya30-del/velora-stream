package com.cinevault.tv.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.cinevault.tv.data.model.EpisodeDto
import com.cinevault.tv.data.model.MovieDto
import com.cinevault.tv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailScreen(
    onPlayClick: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val movie = state.movie

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
            .onPreviewKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyUp) {
                    onBack()
                    true
                } else false
            },
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = TvDimText, fontSize = 20.sp)
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
                        onPlayClick = { onPlayClick(movie.id) },
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
                            color = TvDimText,
                            modifier = Modifier.padding(horizontal = 48.dp, vertical = 4.dp),
                        )
                    }
                }

                // Seasons & Episodes (for series)
                if (state.seasons.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SeasonSelector(
                            seasons = state.seasons.map { "Season ${it.seasonNumber}" },
                            selectedIndex = state.selectedSeasonIndex,
                            onSelect = { viewModel.selectSeason(it) },
                        )
                    }

                    item {
                        EpisodesRow(
                            episodes = state.episodes,
                            movieId = movie.id,
                            onEpisodeClick = { onPlayClick(movie.id) },
                        )
                    }
                }

                // Related content
                if (state.related.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "You May Also Like",
                            fontSize = 20.sp,
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
                Text("Failed to load content", color = TvDimText, fontSize = 18.sp)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroSection(movie: MovieDto, onPlayClick: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
    ) {
        // Backdrop image
        AsyncImage(
            model = movie.bannerUrl ?: movie.posterUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Gradient overlay
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
                        colors = listOf(
                            TvBackground.copy(alpha = 0.85f),
                            Color.Transparent,
                        ),
                        startX = 0f,
                        endX = 800f,
                    )
                )
        )

        // Content overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 32.dp, end = 48.dp),
        ) {
            // Content type badge
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(text = movie.contentType.replaceFirstChar { it.uppercase() })
                movie.contentRating?.let { InfoChip(text = it) }
                movie.releaseYear?.let { InfoChip(text = it.toString()) }
                movie.duration?.let {
                    val hours = it / 60
                    val mins = it % 60
                    InfoChip(text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m")
                }
                if (movie.rating != null && movie.rating > 0) {
                    InfoChip(text = "⭐ ${String.format("%.1f", movie.rating)}")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = movie.title,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (!movie.genres.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = movie.genres.joinToString(" • "),
                    fontSize = 14.sp,
                    color = TvOnSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.colors(
                        containerColor = TvPrimary,
                        contentColor = TvOnPrimary,
                        focusedContainerColor = TvPrimaryVariant,
                    ),
                ) {
                    Text(
                        text = if (movie.contentType == "series") "▶  Play S1:E1" else "▶  Play Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.colors(
                        containerColor = TvSurfaceVariant,
                        contentColor = TvOnSurface,
                        focusedContainerColor = TvBorder,
                    ),
                ) {
                    Text(
                        text = "← Back",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TvSurfaceVariant.copy(alpha = 0.8f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text = text, fontSize = 12.sp, color = TvOnSurfaceVariant)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SeasonSelector(
    seasons: List<String>,
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
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isSelected) TvPrimary else TvSurfaceVariant,
                    focusedContainerColor = TvPrimary,
                    contentColor = if (isSelected) TvOnPrimary else TvOnSurfaceVariant,
                    focusedContentColor = TvOnPrimary,
                ),
            ) {
                Text(
                    text = seasons[index],
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
    movieId: String,
    onEpisodeClick: (String) -> Unit,
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
                    onClick = { onEpisodeClick(episode.id) },
                    modifier = Modifier.width(200.dp).height(64.dp),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = TvSurfaceVariant,
                        focusedContainerColor = TvCardFocused,
                    ),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(
                            border = BorderStroke(width = 2.dp, color = TvPrimary),
                            shape = RoundedCornerShape(8.dp),
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
                        Column {
                            Text(
                                text = "Episode ${episode.episodeNumber}",
                                fontSize = 14.sp,
                                color = TvOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            episode.duration?.let {
                                Text(
                                    text = "${it}m",
                                    fontSize = 12.sp,
                                    color = TvDimText,
                                )
                            }
                        }
                        if (episode.isPremium) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text("👑", fontSize = 16.sp)
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
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
        }
    }
}
