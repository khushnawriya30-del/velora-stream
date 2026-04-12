package com.cinevault.tv.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.cinevault.tv.data.model.MovieDto
import com.cinevault.tv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    isPremium: Boolean = false,
    onMovieClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }
    val d = LocalTvDimens.current

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(horizontal = d.screenPadH, vertical = d.screenPadV)
            .onPreviewKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyUp) {
                    onBack()
                    true
                } else false
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.colors(
                    containerColor = TvSurfaceVariant,
                    contentColor = TvOnSurface,
                    focusedContainerColor = TvPrimary,
                ),
            ) {
                Text("\u2190 Back", fontSize = d.fontBody)
            }

            Spacer(modifier = Modifier.width(d.padLarge))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(d.searchBarH)
                    .clip(RoundedCornerShape(d.padSmall))
                    .background(TvSurfaceVariant)
                    .padding(horizontal = d.padLarge),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (state.query.isEmpty()) {
                    Text("Search movies, series, anime...", color = TvTextMuted, fontSize = d.fontMedium)
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    textStyle = TextStyle(color = TvOnSurface, fontSize = d.fontMedium),
                    singleLine = true,
                    cursorBrush = SolidColor(TvPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester),
                )
            }
        }

        Spacer(modifier = Modifier.height(d.padLarge))

        FilterSection(
            label = "Type",
            options = viewModel.contentTypes,
            selected = state.selectedContentType ?: "All",
            onSelect = { viewModel.onContentTypeChange(it) },
        )

        Spacer(modifier = Modifier.height(d.padSmall))

        FilterSection(
            label = "Genre",
            options = viewModel.genres,
            selected = state.selectedGenre ?: "All",
            onSelect = { viewModel.onGenreChange(it) },
        )

        Spacer(modifier = Modifier.height(d.padSmall))

        FilterSection(
            label = "Language",
            options = viewModel.languages,
            selected = state.selectedLanguage ?: "All",
            onSelect = { viewModel.onLanguageChange(it) },
        )

        Spacer(modifier = Modifier.height(d.padLarge))

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Searching...", color = TvTextMuted, fontSize = d.fontLarge)
            }
        } else if (state.results.isEmpty() && state.hasSearched) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results found", color = TvTextMuted, fontSize = d.fontLarge)
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Adaptive(d.searchGridMinW),
                horizontalArrangement = Arrangement.spacedBy(d.padLarge),
                verticalArrangement = Arrangement.spacedBy(d.padLarge),
                contentPadding = PaddingValues(bottom = d.padXXL),
            ) {
                items(state.results, key = { it.id }) { movie ->
                    SearchResultCard(movie = movie, onClick = { onMovieClick(movie.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterSection(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val d = LocalTvDimens.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            fontSize = d.fontBody,
            color = TvTextMuted,
            modifier = Modifier.width(d.filterLabelW),
        )
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(d.padSmall),
        ) {
            items(options) { option ->
                val isSelected = option == selected
                Surface(
                    onClick = { onSelect(option) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(d.padSmall)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) TvPrimary else TvSurfaceVariant,
                        focusedContainerColor = TvPrimary,
                        contentColor = if (isSelected) TvOnPrimary else TvOnSurfaceVariant,
                        focusedContentColor = TvOnPrimary,
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                ) {
                    Text(
                        text = option,
                        fontSize = d.fontSmall,
                        modifier = Modifier.padding(horizontal = d.padMedium, vertical = d.padSmall),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchResultCard(movie: MovieDto, onClick: () -> Unit) {
    val d = LocalTvDimens.current
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(d.searchCardW)
            .height(d.searchCardH),
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
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(d.padSmall),
            ) {
                Text(
                    text = movie.title,
                    fontSize = d.fontSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = movie.contentType.replaceFirstChar { it.uppercase() },
                    fontSize = d.fontXSmall,
                    color = TvTextMuted,
                )
            }
            if (movie.isEffectivelyPremium) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(d.padSmall)
                        .clip(RoundedCornerShape(d.padTiny))
                        .background(TvPremiumBadge)
                        .padding(horizontal = d.padSmall, vertical = d.padTiny),
                ) {
                    Text("PREMIUM", fontSize = d.fontTiny, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}
