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
    val d = LocalTvDimens.current
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
                Text("Loading...", color = TvTextMuted, fontSize = d.fontXL)
            }
        } else if (movie != null) {
            TvLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = d.padSection),
            ) {
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

                if (!movie.synopsis.isNullOrBlank()) {
                    item {
                        Text(
                            text = movie.synopsis,
                            fontSize = d.fontBody,
                            color = TvOnSurfaceVariant,
                            lineHeight = d.lineHeightLarge,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = d.screenPadH, vertical = d.padMedium),
                        )
                    }
                }

                if (!movie.cast.isNullOrEmpty()) {
                    item {
                        Text(
                            text = "Cast: ${movie.cast.take(5).joinToString(", ") { it.name }}",
                            fontSize = d.fontBody,
                            color = TvTextMuted,
                            modifier = Modifier.padding(horizontal = d.screenPadH, vertical = d.padTiny),
                        )
                    }
                }

                if (state.seasons.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(d.padLarge))
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

                if (state.related.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(d.padXL))
                        Text(
                            text = "You May Also Like",
                            fontSize = d.fontLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TvOnSurface,
                            modifier = Modifier.padding(horizontal = d.screenPadH, vertical = d.padSmall),
                        )
                        TvLazyRow(
                            contentPadding = PaddingValues(horizontal = d.screenPadH),
                            horizontalArrangement = Arrangement.spacedBy(d.padLarge),
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
                Text("Failed to load content", color = TvTextMuted, fontSize = d.fontLarge)
            }
        }

        if (showPremiumPopup) {
            PremiumPopup(onDismiss = { showPremiumPopup = false })
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PremiumPopup(onDismiss: () -> Unit) {
    val d = LocalTvDimens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(d.premiumPopupFraction)
                .clip(RoundedCornerShape(d.padLarge))
                .background(TvSurface)
                .padding(d.padXXL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "\uD83D\uDC51", fontSize = d.fontDisplay)
            Spacer(modifier = Modifier.height(d.padMedium))
            Text(
                text = "Premium Content",
                fontSize = d.fontXXL,
                fontWeight = FontWeight.Bold,
                color = TvPrimary,
            )
            Spacer(modifier = Modifier.height(d.padSmall))
            Text(
                text = "This content requires a Premium subscription.\nUpgrade from the mobile app to watch.",
                fontSize = d.fontBody,
                color = TvOnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = d.lineHeightBody,
            )
            Spacer(modifier = Modifier.height(d.padXL))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.colors(
                    containerColor = TvPrimary,
                    contentColor = TvOnPrimary,
                ),
            ) {
                Text(
                    text = "OK",
                    fontSize = d.fontMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = d.padXXL, vertical = d.padTiny),
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
    val d = LocalTvDimens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(d.heroHeight),
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
                .padding(start = d.screenPadH, bottom = d.padXXL, end = d.screenPadH),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(d.padSmall)) {
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

            Spacer(modifier = Modifier.height(d.padMedium))

            Text(
                text = movie.title,
                fontSize = d.fontHero,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            movie.languageLabel?.let { lang ->
                Spacer(modifier = Modifier.height(d.padTiny))
                Text(text = lang, fontSize = d.fontSmall, color = TvPrimary, fontWeight = FontWeight.Medium)
            }

            if (movie.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(d.padSmall))
                Text(
                    text = movie.genres.joinToString(" \u2022 "),
                    fontSize = d.fontBody,
                    color = TvOnSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(d.padXL))

            Row(horizontalArrangement = Arrangement.spacedBy(d.padMedium)) {
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
                        fontSize = d.fontMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = d.padLarge, vertical = d.padTiny),
                    )
                }

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
                        fontSize = d.fontBody,
                        modifier = Modifier.padding(horizontal = d.padMedium, vertical = d.padTiny),
                    )
                }

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
                        fontSize = d.fontBody,
                        modifier = Modifier.padding(horizontal = d.padMedium, vertical = d.padTiny),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InfoChip(text: String, highlight: Boolean = false) {
    val d = LocalTvDimens.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(d.padTiny))
            .background(
                if (highlight) TvPrimary.copy(alpha = 0.2f)
                else TvSurfaceVariant.copy(alpha = 0.8f)
            )
            .padding(horizontal = d.padSmall, vertical = d.padTiny),
    ) {
        Text(
            text = text,
            fontSize = d.fontSmall,
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
    val d = LocalTvDimens.current
    TvLazyRow(
        contentPadding = PaddingValues(horizontal = d.screenPadH),
        horizontalArrangement = Arrangement.spacedBy(d.padMedium),
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
                    fontSize = d.fontBody,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = d.padXL, vertical = d.padMedium),
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
    val d = LocalTvDimens.current

    Column(modifier = Modifier.padding(top = d.padMedium)) {
        Text(
            text = "Episodes",
            fontSize = d.fontLarge,
            fontWeight = FontWeight.SemiBold,
            color = TvOnSurface,
            modifier = Modifier.padding(horizontal = d.screenPadH, vertical = d.padSmall),
        )

        TvLazyRow(
            contentPadding = PaddingValues(horizontal = d.screenPadH),
            horizontalArrangement = Arrangement.spacedBy(d.padMedium),
        ) {
            items(episodes, key = { it.id }) { episode ->
                Surface(
                    onClick = { onEpisodeClick(episode) },
                    modifier = Modifier.width(d.episodeCardW).height(d.episodeCardH),
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
                            .padding(horizontal = d.padLarge),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "E${episode.episodeNumber}",
                            fontSize = d.fontLarge,
                            fontWeight = FontWeight.Bold,
                            color = TvPrimary,
                        )
                        Spacer(modifier = Modifier.width(d.padMedium))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = episode.title.ifBlank { "Episode ${episode.episodeNumber}" },
                                fontSize = d.fontBody,
                                color = TvOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            episode.duration?.let {
                                Text(text = "${it}m", fontSize = d.fontSmall, color = TvTextMuted)
                            }
                        }
                        if (episode.isPremium) {
                            Text("\uD83D\uDC51", fontSize = d.fontMedium)
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
    val d = LocalTvDimens.current
    Surface(
        onClick = onClick,
        modifier = Modifier.width(d.relatedCardW).height(d.relatedCardH),
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
                    .fillMaxHeight(0.3f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            Text(
                text = movie.title,
                fontSize = d.fontSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(d.padSmall),
            )
            if (movie.isEffectivelyPremium) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(d.padTiny)
                        .clip(RoundedCornerShape(d.padTiny))
                        .background(TvPrimary)
                        .padding(horizontal = d.padTiny, vertical = d.padTiny / 2),
                ) {
                    Text("\uD83D\uDC51", fontSize = d.fontXSmall)
                }
            }
        }
    }
}
